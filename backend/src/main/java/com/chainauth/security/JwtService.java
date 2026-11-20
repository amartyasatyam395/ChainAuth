package com.chainauth.security;

import com.chainauth.user.User; import io.jsonwebtoken.*; import io.jsonwebtoken.security.Keys; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service;
import javax.crypto.SecretKey; import java.nio.charset.StandardCharsets; import java.time.Instant; import java.util.Date;

@Service public class JwtService {
 private final SecretKey key; private final long ttlSeconds;
 public JwtService(@Value("${app.security.jwt-secret}") String secret,@Value("${app.security.access-token-seconds:900}") long ttlSeconds){
  byte[] bytes=secret.getBytes(StandardCharsets.UTF_8); if(bytes.length<32) throw new IllegalStateException("JWT secret must be at least 32 bytes"); this.key=Keys.hmacShaKeyFor(bytes); this.ttlSeconds=ttlSeconds; }
 public String generate(User user){Instant now=Instant.now(); return Jwts.builder().subject(user.getEmail()).claim("email",user.getEmail()).claim("roles",user.getRoles().stream().map(Enum::name).toList()).claim("tokenVersion",user.getTokenVersion()).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(ttlSeconds))).signWith(key).compact();}
 public String subject(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();}
 public long tokenVersion(String token){Object v=Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("tokenVersion");return v instanceof Number n?n.longValue():0L;}
 public long getTtlSeconds(){return ttlSeconds;}
}
