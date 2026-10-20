package com.corepay.wallet.repo;

import com.corepay.wallet.model.WalletDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface WalletRepository extends MongoRepository<WalletDocument, String> {
    Optional<WalletDocument> findByWalletId(String walletId);
}