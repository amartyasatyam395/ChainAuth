package com.chainauth.blockchain;

import org.springframework.beans.factory.annotation.Value; import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component;
import java.time.Instant; import java.math.BigInteger;

@Component public class IdentitySyncWorker {
 private final IdentitySyncJobRepository jobs; private final BlockchainService chain; private final int requiredConfirmations; private final int maxAttempts; private final int txTimeoutSeconds;
 public IdentitySyncWorker(IdentitySyncJobRepository jobs, BlockchainService chain, @Value("${app.blockchain.confirmations:3}") int confirmations, @Value("${app.blockchain.max-sync-attempts:5}") int maxAttempts, @Value("${app.blockchain.transaction-timeout-seconds:600}") int txTimeoutSeconds){this.jobs=jobs;this.chain=chain;this.requiredConfirmations=Math.max(1,confirmations);this.maxAttempts=Math.max(1,maxAttempts);this.txTimeoutSeconds=Math.max(60,txTimeoutSeconds);}
 @Scheduled(fixedDelayString="${app.blockchain.sync-poll-ms:5000}") public void process(){
  for(IdentitySyncJob job:jobs.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(SyncStatus.QUEUED,Instant.now())) submit(job);
  for(IdentitySyncJob job:jobs.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(SyncStatus.SUBMITTED,Instant.now())) confirm(job);
 }
 private void submit(IdentitySyncJob j){try{j.setAttempts(j.getAttempts()+1);String hash=switch(j.getOperation()){
  case "REGISTER" -> chain.submitIdentityOperation("registerIdentity",j.getWalletAddress(),j.getRole());
  case "ASSIGN_ROLE" -> chain.submitIdentityOperation("assignRole",j.getWalletAddress(),j.getRole());
  case "DEACTIVATE" -> chain.submitIdentityOperation("deactivateIdentity",j.getWalletAddress(),null);
  case "REACTIVATE" -> chain.submitIdentityOperation("reactivateIdentity",j.getWalletAddress(),null);
  default -> throw new IllegalArgumentException("Unsupported sync operation: "+j.getOperation());};j.setTransactionHash(hash);j.setSubmittedAt(Instant.now());j.setStatus(SyncStatus.SUBMITTED);j.setNextAttemptAt(Instant.now().plusSeconds(5));j.setLastError(null);}catch(Exception ex){j.setLastError(safe(ex));if(j.getAttempts()>=maxAttempts)j.setStatus(SyncStatus.FAILED);else j.setNextAttemptAt(Instant.now().plusSeconds(Math.min(300,5L<<Math.min(j.getAttempts(),6))));}j.setUpdatedAt(Instant.now());jobs.save(j);}
 private void confirm(IdentitySyncJob j){try{var receipt=chain.findReceipt(j.getTransactionHash());if(receipt==null){if(j.getSubmittedBlock()!=null&&j.getSubmittedBlockHash()!=null){String canonical=chain.getBlockHash(j.getSubmittedBlock());if(!canonical.equalsIgnoreCase(j.getSubmittedBlockHash())){j.setStatus(SyncStatus.REORGED);j.setLastError("Transaction receipt disappeared and its previously observed block was reorganized");}else if(j.getSubmittedAt()!=null&&Instant.now().isAfter(j.getSubmittedAt().plusSeconds(txTimeoutSeconds))){j.setStatus(SyncStatus.FAILED);j.setLastError("Transaction receipt was not found before the configured timeout; manual retry required");}else j.setNextAttemptAt(Instant.now().plusSeconds(10));}else if(j.getSubmittedAt()!=null&&Instant.now().isAfter(j.getSubmittedAt().plusSeconds(txTimeoutSeconds))){j.setStatus(SyncStatus.FAILED);j.setLastError("Transaction receipt was not found before the configured timeout; manual retry required");}else j.setNextAttemptAt(Instant.now().plusSeconds(10));}else if(!receipt.isStatusOK()){j.setStatus(SyncStatus.FAILED);j.setLastError("Blockchain transaction reverted");}else{long block=receipt.getBlockNumber().longValueExact();String canonical=chain.getBlockHash(block);if(j.getSubmittedBlockHash()!=null&&!j.getSubmittedBlockHash().equalsIgnoreCase(canonical)){j.setStatus(SyncStatus.REORGED);j.setLastError("Receipt block was reorganized; manual reconciliation required");}else{j.setSubmittedBlock(block);j.setSubmittedBlockHash(canonical);long latest=chain.getLatestBlock().longValueExact();int conf=(int)Math.max(0,Math.min(Integer.MAX_VALUE,latest-block+1));j.setConfirmations(conf);if(conf>=requiredConfirmations){var identity=chain.getIdentity(j.getWalletAddress());boolean matches=switch(j.getOperation()){
  case "REGISTER", "REACTIVATE" -> identity.registered()&&identity.active()&&(j.getRole()==null||identity.role()==j.getRole());
  case "ASSIGN_ROLE" -> identity.registered()&&identity.active()&&identity.role()==j.getRole();
  case "DEACTIVATE" -> identity.registered()&&!identity.active();
  default -> false;};
  if(matches){j.setStatus(SyncStatus.CONFIRMED);j.setLastError(null);}else{j.setLastError("Receipt confirmed but contract state does not match requested identity");j.setNextAttemptAt(Instant.now().plusSeconds(15));}}else j.setNextAttemptAt(Instant.now().plusSeconds(5));}}}catch(Exception ex){j.setLastError(safe(ex));j.setNextAttemptAt(Instant.now().plusSeconds(15));}j.setUpdatedAt(Instant.now());jobs.save(j);}
 private String safe(Exception e){String m=e.getMessage();if(m==null)m=e.getClass().getSimpleName();return m.length()>300?m.substring(0,300):m;}
}
