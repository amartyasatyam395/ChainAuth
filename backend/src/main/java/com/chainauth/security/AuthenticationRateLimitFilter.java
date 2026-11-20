package com.chainauth.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String,Window> windows=new ConcurrentHashMap<>();
    private final int limit; private final long windowMs;
    public AuthenticationRateLimitFilter(@Value("${app.security.rate-limit-per-minute:10}") int limit,
                                          @Value("${app.security.rate-limit-window-ms:60000}") long windowMs){this.limit=Math.max(1,limit);this.windowMs=Math.max(1000,windowMs);}
    @Override protected boolean shouldNotFilter(HttpServletRequest r){String p=r.getRequestURI();return !(p.equals("/api/auth/login")||p.equals("/api/auth/register")||p.equals("/api/auth/wallet/nonce")||p.equals("/api/auth/wallet/verify")||p.equals("/api/auth/wallet/link/verify")||p.equals("/api/auth/refresh"));}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
        long now=System.currentTimeMillis();String key=(req.getRemoteAddr()==null?"unknown":req.getRemoteAddr())+":"+req.getRequestURI();
        Window w=windows.compute(key,(k,old)->old==null||now-old.startedAt>=windowMs?new Window(now,new AtomicInteger(0)):old);
        if(w.count.incrementAndGet()>limit){res.setStatus(429);res.setHeader("Retry-After",Long.toString(Math.max(1,(w.startedAt+windowMs-now+999)/1000)));res.setContentType("application/json");res.getWriter().write("{\"status\":429,\"error\":\"Too many authentication requests. Try again later.\"}");return;}
        if(windows.size()>10000)windows.entrySet().removeIf(e->now-e.getValue().startedAt>windowMs*2);
        chain.doFilter(req,res);
    }
    private record Window(long startedAt,AtomicInteger count){}
}
