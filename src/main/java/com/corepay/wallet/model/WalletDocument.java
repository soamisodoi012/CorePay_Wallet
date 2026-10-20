package com.corepay.wallet.model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Document(collection = "wallets")
public class WalletDocument {
    @Id
    private String walletId;
    private String ownerId;
    private double balance;
    private String currencyCode;
    private Instant createdAt;
    private Instant updatedAt;
}