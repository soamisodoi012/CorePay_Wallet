package com.corepay.wallet.model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Document(collection = "wallets")
public class WalletDocument {
    @Id
    private String id;
    private String walletId;     // e.g., wal_abc12345
    private String customerId;   // e.g., cust_98765432 (The relational link!)
    private String currencyCode; // e.g., USD, EUR
    private double balance;
    private Instant createdAt;
    private Instant updatedAt;
}