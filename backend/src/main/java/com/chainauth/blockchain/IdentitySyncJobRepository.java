package com.chainauth.blockchain;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant; import java.util.List; import java.util.Optional;
public interface IdentitySyncJobRepository extends MongoRepository<IdentitySyncJob,String> {
 Optional<IdentitySyncJob> findByIdempotencyKey(String key);
 List<IdentitySyncJob> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(SyncStatus status, Instant now);
 List<IdentitySyncJob> findTop100ByStatusOrderByUpdatedAtDesc(SyncStatus status);
 List<IdentitySyncJob> findTop100ByOrderByCreatedAtDesc();
}
