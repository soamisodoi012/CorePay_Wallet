package com.corepay.wallet.services;



import com.corepay.wallet.grpc.*;
import com.corepay.wallet.model.CustomerDocument;
import com.corepay.wallet.repo.CustomerRepository;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import com.corepay.wallet.interceptor.JwtServerInterceptor;
import com.corepay.wallet.model.LedgerTransactionDocument;
import com.corepay.wallet.model.WalletDocument;
import com.corepay.wallet.repo.LedgerRepository;
import com.corepay.wallet.repo.WalletRepository;
import com.corepay.wallet.stream.BalanceStreamBroadcaster;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class WalletGrpcServiceImpl extends WalletServiceGrpc.WalletServiceImplBase {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;
    private final BalanceStreamBroadcaster streamBroadcaster;
    private final CustomerRepository customerRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public void createWallet(CreateWalletRequest request, StreamObserver<WalletResponse> responseObserver) {

        // 1. Validate that the Customer exists first!
        boolean customerExists = customerRepository.findByCustomerId(request.getCustomerId()).isPresent();
        if (!customerExists) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Customer with ID " + request.getCustomerId() + " does not exist.")
                    .asRuntimeException());
            return;
        }

        // 2. Generate unique Wallet ID
        String walletId = "wal_" + UUID.randomUUID().toString().substring(0, 8);
        Instant now = Instant.now();

        // 3. Save Wallet linked to Customer
        WalletDocument wallet = new WalletDocument();
        wallet.setWalletId(walletId);
        wallet.setCustomerId(request.getCustomerId()); // Set the relationship
        wallet.setCurrencyCode(request.getCurrencyCode());
        wallet.setBalance(0.0); // Starting balance
        wallet.setCreatedAt(now);
        wallet.setUpdatedAt(now);

        walletRepository.save(wallet);

        // 4. Build gRPC Response
        WalletResponse response = WalletResponse.newBuilder()
                .setWalletId(walletId)
                .setCustomerId(wallet.getCustomerId())
                .setCurrencyCode(wallet.getCurrencyCode())
                .setBalance(wallet.getBalance())
                .setCreatedAt(now.toString())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getBalance(GetBalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        WalletDocument wallet = walletRepository.findByWalletId(request.getWalletId())
                .orElseThrow(() -> Status.NOT_FOUND.withDescription("Wallet not found").asRuntimeException());

        BalanceResponse response = BalanceResponse.newBuilder()
                .setWalletId(wallet.getWalletId())
                .setBalance(wallet.getBalance())
                .setCurrencyCode(wallet.getCurrencyCode())
                .setUpdatedAt(Timestamp.newBuilder().setSeconds(wallet.getUpdatedAt().getEpochSecond()).build())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void transferFunds(TransferRequest request, StreamObserver<TransferResponse> responseObserver) {
        // 1. Idempotency Check
        Optional<LedgerTransactionDocument> existingTx = ledgerRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingTx.isPresent()) {
            TransferResponse cachedResponse = TransferResponse.newBuilder()
                    .setTransactionId(existingTx.get().getTransactionId())
                    .setStatus(existingTx.get().getStatus())
                    .setMessage("Duplicate request processed idempotently.")
                    .setTimestamp(Timestamp.newBuilder().setSeconds(existingTx.get().getTimestamp().getEpochSecond()).build())
                    .build();
            responseObserver.onNext(cachedResponse);
            responseObserver.onCompleted();
            return;
        }

        // 2. Fetch Source & Target Wallets
        WalletDocument sourceWallet = walletRepository.findByWalletId(request.getSourceWalletId())
                .orElseThrow(() -> Status.NOT_FOUND.withDescription("Source wallet not found").asRuntimeException());
        System.out.println("sourcewallet:"+sourceWallet);
        WalletDocument targetWallet = walletRepository.findByWalletId(request.getTargetWalletId())
                .orElseThrow(() -> Status.NOT_FOUND.withDescription("Target wallet not found").asRuntimeException());

        // 3. Validation Logic
        if (sourceWallet.getBalance() < request.getAmount()) {
            responseObserver.onError(Status.FAILED_PRECONDITION.withDescription("Insufficient wallet balance").asRuntimeException());
            return;
        }

        // 4. Update Balances Atomically
        sourceWallet.setBalance(sourceWallet.getBalance() - request.getAmount());
        sourceWallet.setUpdatedAt(Instant.now());

        targetWallet.setBalance(targetWallet.getBalance() + request.getAmount());
        targetWallet.setUpdatedAt(Instant.now());

        walletRepository.save(sourceWallet);
        walletRepository.save(targetWallet);

        // 5. Save Ledger Record with Idempotency Key
        String txId = "tx_" + UUID.randomUUID().toString().substring(0, 10);
        LedgerTransactionDocument tx = new LedgerTransactionDocument();
        tx.setTransactionId(txId);
        tx.setIdempotencyKey(request.getIdempotencyKey());
        tx.setSourceWalletId(sourceWallet.getWalletId());
        tx.setTargetWalletId(targetWallet.getWalletId());
        tx.setAmount(request.getAmount());
        tx.setStatus("SUCCESS");
        tx.setTimestamp(Instant.now());
        ledgerRepository.save(tx);

        // 6. Broadcast Real-time Server Stream Update to Listeners
        streamBroadcaster.broadcast(BalanceUpdateEvent.newBuilder()
                .setWalletId(sourceWallet.getWalletId())
                .setCurrentBalance(sourceWallet.getBalance())
                .setTransactionId(txId)
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                .build());

        streamBroadcaster.broadcast(BalanceUpdateEvent.newBuilder()
                .setWalletId(targetWallet.getWalletId())
                .setCurrentBalance(targetWallet.getBalance())
                .setTransactionId(txId)
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                .build());

        // 7. Send Response
        TransferResponse response = TransferResponse.newBuilder()
                .setTransactionId(txId)
                .setStatus("SUCCESS")
                .setMessage("Transfer executed successfully.")
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void streamBalanceUpdates(StreamBalanceRequest request, StreamObserver<BalanceUpdateEvent> responseObserver) {
        // Register client stream connection for real-time publishing
        streamBroadcaster.register(request.getWalletId(), responseObserver);

        // Keep stream open context via lifecycle callback hook if desired,
        // or let the broadcaster manage dispatching events when balances change.
    }
    public void createCustomer(CreateCustomerRequest request, StreamObserver<CustomerResponse> responseObserver) {
        // 1. Check if email already exists
        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription("Customer with this email already exists.")
                    .asRuntimeException());
            return;
        }

        // 2. Generate unique Customer ID
        String customerId = "cust_" + UUID.randomUUID().toString().substring(0, 8);
        Instant now = Instant.now();

        // 3. Create and populate document
        CustomerDocument customer = new CustomerDocument();
        customer.setCustomerId(customerId);
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setKycStatus("VERIFIED"); // Default status for onboarding demo
        customer.setCreatedAt(now);

        customerRepository.save(customer);

        // 4. Build gRPC Response
        CustomerResponse response = CustomerResponse.newBuilder()
                .setCustomerId(customerId)
                .setFullName(customer.getFullName())
                .setEmail(customer.getEmail())
                .setPhoneNumber(customer.getPhoneNumber())
                .setKycStatus(customer.getKycStatus())
                .setCreatedAt(now.toString())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
