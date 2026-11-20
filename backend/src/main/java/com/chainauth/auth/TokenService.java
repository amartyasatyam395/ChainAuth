package com.chainauth.auth;

import com.chainauth.user.User; import org.springframework.beans.factory.annotation.Value; import org.springframework.data.mongodb.core.MongoTemplate; import org.springframework.data.mongodb.core.query.*; import org.springframework.stereotype.Service; import java.security.SecureRandom; import java.time.Instant; import java.util.*; import java.util.Base64; import java.security.MessageDigest; import java.nio.charset.StandardCharsets;

@Service public class TokenService {
 private final RefreshTokenRepository repo; private final MongoTemplate mongo; private final long ttlSeconds; private final SecureRandom random=new SecureRandom();
 public TokenService(RefreshTokenRepository repo,MongoTemplate mongo,@Value("${app.security.refresh-token-days:7}") long days){this.repo=repo;this.mongo=mongo;this.ttlSeconds=days*86400;}
 public record Issued(String raw,String familyId){}
 private String randomToken(){byte[] b=new byte[64];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 public String hash(String raw){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 public Issued issue(String userId,String family){String raw=randomToken();RefreshToken t=new RefreshToken();t.setTokenHash(hash(raw));t.setUserId(userId);t.setFamilyId(family==null?UUID.randomUUID().toString():family);t.setCreatedAt(Instant.now());t.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));repo.save(t);return new Issued(raw,t.getFamilyId());}
 public Optional<RefreshToken> consume(String raw){String h=hash(raw); Query q=new Query(Criteria.where("tokenHash").is(h).and("usedAt").is(null).and("revokedAt").is(null).and("expiresAt").gt(Instant.now())); Update u=new Update().set("usedAt",Instant.now()); RefreshToken consumed=mongo.findAndModify(q,u,FindAndModifyOptions.options().returnNew(true),RefreshToken.class);
  if(consumed!=null)return Optional.of(consumed); Optional<RefreshToken> existing=repo.findByTokenHash(h); if(existing.isPresent()&&existing.get().getUsedAt()!=null)revokeFamily(existing.get().getFamilyId()); return Optional.empty();}
 public void replace(String oldHash,String newHash){repo.findByTokenHash(oldHash).ifPresent(t->{t.setReplacedByHash(newHash);repo.save(t);});}
 public void revokeFamily(String family){repo.findByFamilyId(family).forEach(t->{t.setRevokedAt(Instant.now());repo.save(t);});}
 public void revokeUser(String userId){repo.findByUserIdAndRevokedAtIsNull(userId).forEach(t->{t.setRevokedAt(Instant.now());repo.save(t);});}
 public void revokeRaw(String raw){repo.findByTokenHash(hash(raw)).ifPresent(t->{t.setRevokedAt(Instant.now());repo.save(t);});}
 public long getTtlSeconds(){return ttlSeconds;}
}
