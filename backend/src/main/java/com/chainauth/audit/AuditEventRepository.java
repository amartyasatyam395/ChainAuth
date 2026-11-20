package com.chainauth.audit;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
public interface AuditEventRepository extends MongoRepository<AuditEvent,String> {
    Optional<AuditEvent> findTopByOrderBySequenceDesc();
    Optional<AuditEvent> findBySequence(long sequence);
}
