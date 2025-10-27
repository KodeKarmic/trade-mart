package com.trademart.tradestore.repair.repository;

import com.trademart.tradestore.repair.dto.FailedTrade;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedTradeRepository extends MongoRepository<FailedTrade, String> {
    // Additional query methods can be added here as needed
}
