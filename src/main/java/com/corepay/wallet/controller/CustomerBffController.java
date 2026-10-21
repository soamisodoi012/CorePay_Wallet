package com.corepay.wallet.controller;

import com.corepay.wallet.grpc.CreateCustomerRequest;
import com.corepay.wallet.grpc.CustomerResponse;
import com.corepay.wallet.grpc.WalletServiceGrpc;
import com.corepay.wallet.model.CustomerDocument;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient; // <--- Added missing import
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerBffController {

    @GrpcClient("wallet-service")
    private WalletServiceGrpc.WalletServiceBlockingStub walletClient;

    @PostMapping
    public ResponseEntity<?> createCustomer(@RequestBody CustomerDocument dto) {
        try {
            // 1. Map JSON DTO to Protobuf Request (using standard Lombok getters if using @Data)
            CreateCustomerRequest grpcRequest = CreateCustomerRequest.newBuilder()
                    .setFullName(dto.getFullName())
                    .setEmail(dto.getEmail())
                    .setPhoneNumber(dto.getPhoneNumber())
                    .build();

            // 2. Call internal gRPC service synchronously
            CustomerResponse grpcResponse = walletClient.createCustomer(grpcRequest);


            // 3. Return JSON response to React / React Native app
            return ResponseEntity.status(HttpStatus.CREATED).body(grpcResponse);

        } catch (StatusRuntimeException e) {
            HttpStatus status = switch (e.getStatus().getCode()) {
                case ALREADY_EXISTS -> HttpStatus.CONFLICT;
                case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
            return ResponseEntity.status(status).body(e.getStatus().getDescription());
        }
    }
}