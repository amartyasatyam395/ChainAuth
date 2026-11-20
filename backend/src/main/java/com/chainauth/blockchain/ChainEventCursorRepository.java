package com.chainauth.blockchain;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface ChainEventCursorRepository extends MongoRepository<ChainEventCursor,String> {}
