package com.chainauth.blockchain;

import com.chainauth.wallet.Wallet; import com.chainauth.wallet.WalletRepository;
import org.springframework.dao.DuplicateKeyException; import org.springframework.stereotype.Service;
import java.time.Instant; import java.util.List; import java.util.Locale; import java.util.UUID;

@Service public class IdentitySyncService {
 private final IdentitySyncJobRepository jobs; private final WalletRepository wallets;
 public IdentitySyncService(IdentitySyncJobRepository jobs, WalletRepository wallets){this.jobs=jobs;this.wallets=wallets;}
 public IdentitySyncJob enqueueRegistration(String rawAddress, IdentityRole role, String requestedKey){return enqueue(rawAddress,"REGISTER",role,requestedKey);}
 public IdentitySyncJob enqueueRoleChange(String rawAddress, IdentityRole role, String requestedKey){return enqueue(rawAddress,"ASSIGN_ROLE",role,requestedKey);}
 public IdentitySyncJob enqueueStatusChange(String rawAddress, boolean active, String requestedKey){return enqueue(rawAddress,active?"REACTIVATE":"DEACTIVATE",null,requestedKey);}
 private IdentitySyncJob enqueue(String rawAddress,String operation,IdentityRole role,String requestedKey){
  String address=normalize(rawAddress); Wallet wallet=wallets.findByAddress(address).orElseThrow(()->new IllegalArgumentException("Wallet must be linked before identity synchronization"));
  String key=(requestedKey==null||requestedKey.isBlank())?UUID.randomUUID().toString():requestedKey.trim();
  if(key.length()>128) throw new IllegalArgumentException("Idempotency key must be at most 128 characters");
  var existing=jobs.findByIdempotencyKey(key);if(existing.isPresent()){IdentitySyncJob prior=existing.get();if(!prior.getWalletAddress().equals(address)||!prior.getOperation().equals(operation)||prior.getRole()!=role)throw new IllegalArgumentException("Idempotency-Key was already used for a different request");return prior;}
  IdentitySyncJob j=new IdentitySyncJob(); j.setIdempotencyKey(key);j.setWalletAddress(address);j.setUserId(wallet.getUserId());j.setRole(role);j.setOperation(operation);
  try{return jobs.save(j);}catch(DuplicateKeyException ex){return jobs.findByIdempotencyKey(key).filter(prior->prior.getWalletAddress().equals(address)&&prior.getOperation().equals(operation)&&prior.getRole()==role).orElseThrow(()->new IllegalArgumentException("Idempotency-Key conflict",ex));}
 }
 public IdentitySyncJob get(String id){return jobs.findById(id).orElseThrow(()->new IllegalArgumentException("Sync job not found"));}
 public List<IdentitySyncJob> list(){return jobs.findTop100ByOrderByCreatedAtDesc();}
 public IdentitySyncJob retry(String id){IdentitySyncJob j=get(id);if(j.getStatus()!=SyncStatus.FAILED&&j.getStatus()!=SyncStatus.REORGED)throw new IllegalArgumentException("Only failed or reorged jobs can be retried");j.setStatus(SyncStatus.QUEUED);j.setTransactionHash(null);j.setSubmittedAt(null);j.setSubmittedBlock(null);j.setSubmittedBlockHash(null);j.setConfirmations(0);j.setAttempts(0);j.setLastError(null);j.setNextAttemptAt(Instant.now());j.setUpdatedAt(Instant.now());return jobs.save(j);}
 static String normalize(String a){if(a==null||!a.matches("(?i)^0x[0-9a-f]{40}$"))throw new IllegalArgumentException("Invalid Ethereum address");return "0x"+a.substring(2).toLowerCase(Locale.ROOT);}
}
