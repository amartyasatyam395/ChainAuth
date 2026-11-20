package com.chainauth.config;

import com.chainauth.audit.AuditCaptureFilter;
import com.chainauth.security.AuthenticationRateLimitFilter;
import com.chainauth.security.JwtAuthenticationFilter;
import jakarta.servlet.Filter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.*;
import java.util.List;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
            AuthenticationRateLimitFilter rateLimit, AuditCaptureFilter auditFilter) throws Exception {
        CookieCsrfTokenRepository csrf=CookieCsrfTokenRepository.withHttpOnlyFalse(); csrf.setCookiePath("/");
        return http.cors(c->{}).csrf(c->c.csrfTokenRepository(csrf))
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h->h.frameOptions(f->f.deny()).contentTypeOptions(c->{}).referrerPolicy(r->r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)).permissionsPolicy(p->p.policy("camera=(), microphone=(), geolocation=()")))
            .authorizeHttpRequests(a->a.requestMatchers("/api/health/**","/actuator/health","/api/auth/csrf","/api/auth/register","/api/auth/login","/api/auth/wallet/nonce","/api/auth/wallet/verify").permitAll()
                .requestMatchers(HttpMethod.OPTIONS,"/**").permitAll().requestMatchers("/api/auth/refresh","/api/auth/logout").permitAll()
                .requestMatchers("/api/users/**","/api/auth/me","/api/admin/**","/api/auth/wallet/link/verify").authenticated().anyRequest().denyAll())
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->res.sendError(401)).accessDeniedHandler((req,res,ex)->res.sendError(403)))
            .addFilterBefore(jwtFilter,UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimit,JwtAuthenticationFilter.class)
            .addFilterAfter(auditFilter,JwtAuthenticationFilter.class).build();
    }
    @Bean FilterRegistrationBean<Filter> disableRateLimitServletRegistration(AuthenticationRateLimitFilter f){var b=new FilterRegistrationBean<Filter>(f);b.setEnabled(false);return b;}
    @Bean FilterRegistrationBean<Filter> disableAuditServletRegistration(AuditCaptureFilter f){var b=new FilterRegistrationBean<Filter>(f);b.setEnabled(false);return b;}
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration c)throws Exception{return c.getAuthenticationManager();}
    @Bean CorsConfigurationSource corsConfigurationSource(@org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origin}") String origin){
        CorsConfiguration c=new CorsConfiguration();c.setAllowedOrigins(List.of(origin));c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Content-Type","Authorization","X-XSRF-TOKEN","Idempotency-Key"));c.setExposedHeaders(List.of("Set-Cookie","Retry-After"));c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;
    }
}
