package com.corepay.wallet.repo;

import com.corepay.wallet.model.LedgerTransactionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface LedgerRepository extends MongoRepository<LedgerTransactionDocument, String> {
    Optional<LedgerTransactionDocument> findByIdempotencyKey(String idempotencyKey);
}
