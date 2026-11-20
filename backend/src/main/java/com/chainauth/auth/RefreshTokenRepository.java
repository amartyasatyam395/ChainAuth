package com.chainauth.auth;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface RefreshTokenRepository extends MongoRepository<RefreshToken,String> { Optional<RefreshToken> findByTokenHash(String hash); List<RefreshToken> findByFamilyId(String familyId); List<RefreshToken> findByUserIdAndRevokedAtIsNull(String userId); }
