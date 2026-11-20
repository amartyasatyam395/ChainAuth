package com.chainauth.blockchain;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface IndexedChainEventRepository extends MongoRepository<IndexedChainEvent,String> { void deleteByBlockNumberGreaterThanEqual(long blockNumber); }
