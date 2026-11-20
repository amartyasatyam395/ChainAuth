package com.chainauth.blockchain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document("identity_sync_jobs")
@CompoundIndex(name="sync_status_retry_idx", def="{'status':1,'nextAttemptAt':1}")
public class IdentitySyncJob {
 @Id private String id;
 @Indexed(unique=true) private String idempotencyKey;
 private String walletAddress; private String userId; private IdentityRole role;
 private String operation; private SyncStatus status=SyncStatus.QUEUED;
 private String transactionHash; private String submittedBlockHash; private Long submittedBlock;
 private Integer confirmations=0; private Integer attempts=0; private String lastError;
 private Instant createdAt=Instant.now(); private Instant updatedAt=Instant.now(); private Instant nextAttemptAt=Instant.now(); private Instant submittedAt;
 public String getId(){return id;} public void setId(String v){id=v;}
 public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;}
 public String getWalletAddress(){return walletAddress;} public void setWalletAddress(String v){walletAddress=v;}
 public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
 public IdentityRole getRole(){return role;} public void setRole(IdentityRole v){role=v;}
 public String getOperation(){return operation;} public void setOperation(String v){operation=v;}
 public SyncStatus getStatus(){return status;} public void setStatus(SyncStatus v){status=v;}
 public String getTransactionHash(){return transactionHash;} public void setTransactionHash(String v){transactionHash=v;}
 public String getSubmittedBlockHash(){return submittedBlockHash;} public void setSubmittedBlockHash(String v){submittedBlockHash=v;}
 public Long getSubmittedBlock(){return submittedBlock;} public void setSubmittedBlock(Long v){submittedBlock=v;}
 public Integer getConfirmations(){return confirmations;} public void setConfirmations(Integer v){confirmations=v;}
 public Integer getAttempts(){return attempts;} public void setAttempts(Integer v){attempts=v;}
 public String getLastError(){return lastError;} public void setLastError(String v){lastError=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
 public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
 public Instant getNextAttemptAt(){return nextAttemptAt;} public void setNextAttemptAt(Instant v){nextAttemptAt=v;}
 public Instant getSubmittedAt(){return submittedAt;} public void setSubmittedAt(Instant v){submittedAt=v;}
}
