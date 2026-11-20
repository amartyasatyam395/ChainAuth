package com.chainauth.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component @Profile("prod")
public class ProductionSecurityGuard {
    @Value("${app.security.jwt-secret}") private String jwtSecret;
    @Value("${app.security.secure-cookie:false}") private boolean secureCookie;
    @Value("${app.security.audit-hash-secret}") private String auditHashSecret;
    @Value("${app.cors.allowed-origin}") private String allowedOrigin;
    @PostConstruct void validate(){
        if(jwtSecret==null||jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<32||jwtSecret.contains("local-development"))throw new IllegalStateException("Production requires a unique JWT_SECRET of at least 32 bytes");
        if(auditHashSecret==null||auditHashSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<32||auditHashSecret.contains("local-audit-hash")||auditHashSecret.equals(jwtSecret))throw new IllegalStateException("Production requires a unique AUDIT_HASH_SECRET of at least 32 bytes");
        if(!secureCookie)throw new IllegalStateException("Production requires SECURE_COOKIE=true");
        if(allowedOrigin==null||!allowedOrigin.startsWith("https://"))throw new IllegalStateException("Production FRONTEND_ORIGIN must use HTTPS");
    }
}
