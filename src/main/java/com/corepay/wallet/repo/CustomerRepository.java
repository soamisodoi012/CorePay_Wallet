package com.corepay.wallet.repo;
import com.corepay.wallet.model.CustomerDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface CustomerRepository extends MongoRepository<CustomerDocument, String> {
    Optional<CustomerDocument> findByCustomerId(String customerId);
    Optional<CustomerDocument> findByEmail(String email);
}
