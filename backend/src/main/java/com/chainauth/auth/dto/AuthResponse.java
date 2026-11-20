package com.chainauth.auth.dto;
import java.time.Instant; import java.util.Set;
public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UserView user) {
 public record UserView(String id,String name,String email,Set<String> roles) {}
}
