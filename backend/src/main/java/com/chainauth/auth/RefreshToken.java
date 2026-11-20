package com.chainauth.auth;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document("refresh_tokens")
public class RefreshToken {
 @Id private String id; @Indexed(unique=true) private String tokenHash; @Indexed private String userId; @Indexed private String familyId;
 private Instant createdAt; @Indexed(expireAfter="0s") private Instant expiresAt; private Instant usedAt; private Instant revokedAt; private String replacedByHash;
 public String getId(){return id;} public void setId(String v){id=v;} public String getTokenHash(){return tokenHash;} public void setTokenHash(String v){tokenHash=v;}
 public String getUserId(){return userId;} public void setUserId(String v){userId=v;} public String getFamilyId(){return familyId;} public void setFamilyId(String v){familyId=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;} public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant v){expiresAt=v;}
 public Instant getUsedAt(){return usedAt;} public void setUsedAt(Instant v){usedAt=v;} public Instant getRevokedAt(){return revokedAt;} public void setRevokedAt(Instant v){revokedAt=v;}
 public String getReplacedByHash(){return replacedByHash;} public void setReplacedByHash(String v){replacedByHash=v;}
}
