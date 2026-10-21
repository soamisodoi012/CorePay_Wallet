package com.corepay.wallet.controller;

import com.corepay.wallet.dto.CreateWalletRequestDto;
import com.corepay.wallet.grpc.CreateWalletRequest;
import com.corepay.wallet.grpc.WalletResponse;
import com.corepay.wallet.grpc.WalletServiceGrpc;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletBffController {

    @GrpcClient("wallet-service")
    private WalletServiceGrpc.WalletServiceBlockingStub walletClient;

    @PostMapping
    public ResponseEntity<?> createWallet(@RequestBody CreateWalletRequestDto dto) {
        try {
            CreateWalletRequest grpcRequest = CreateWalletRequest.newBuilder()
                    .setCustomerId(dto.getCustomerId())     // Use Lombok getter
                    .setCurrencyCode(dto.getCurrencyCode()) // Use Lombok getter
                    .build();

            WalletResponse grpcResponse = walletClient.createWallet(grpcRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(grpcResponse);

        } catch (StatusRuntimeException e) {
            HttpStatus status = switch (e.getStatus().getCode()) {
                case NOT_FOUND -> HttpStatus.NOT_FOUND;
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
            return ResponseEntity.status(status).body(e.getStatus().getDescription());
        }
    }
}