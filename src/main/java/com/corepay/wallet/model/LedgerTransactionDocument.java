package com.corepay.wallet.model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Document(collection = "ledger_transactions")
public class LedgerTransactionDocument {
    @Id
    private String transactionId;
    @Indexed(unique = true)
    private String idempotencyKey;
    private String sourceWalletId;
    private String targetWalletId;
    private double amount;
    private String status; // SUCCESS, FAILED
    private Instant timestamp;
}
