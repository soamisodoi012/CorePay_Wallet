package com.corepay.wallet.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "customers")
public class CustomerDocument {
    @Id
    private String id;
    private String customerId; // e.g., cust_abc12345
    private String fullName;
    private String email;
    private String phoneNumber;
    private String kycStatus;   // e.g., "PENDING", "VERIFIED"
    private Instant createdAt;
}
