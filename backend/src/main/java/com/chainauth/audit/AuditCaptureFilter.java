package com.chainauth.audit;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

@Component
public class AuditCaptureFilter extends OncePerRequestFilter {
    private final AuditService audit; private final String hashKey;
    public AuditCaptureFilter(AuditService audit, @org.springframework.beans.factory.annotation.Value("${app.security.audit-hash-secret}") String hashKey){this.audit=audit;this.hashKey=hashKey;}
    @Override protected boolean shouldNotFilter(HttpServletRequest r){String m=r.getMethod();return !(m.equals("POST")||m.equals("PUT")||m.equals("PATCH")||m.equals("DELETE"))||r.getRequestURI().startsWith("/actuator/")||r.getRequestURI().equals("/api/auth/csrf");}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
        try{chain.doFilter(req,res);}finally{
            Authentication auth=SecurityContextHolder.getContext().getAuthentication();
            String actor=auth==null||!auth.isAuthenticated()?"anonymous":auth.getName();
            String source=hmac(req.getRemoteAddr()==null?"unknown":req.getRemoteAddr());
            String path=req.getRequestURI();
            audit.record(actor,req.getMethod(),path,res.getStatus(),source);
        }
    }
    private String hmac(String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(hashKey.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Unable to hash audit source",e);}}
}
