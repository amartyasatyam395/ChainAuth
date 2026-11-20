package com.chainauth.auth;
import com.chainauth.auth.dto.*; import com.chainauth.user.User; import jakarta.servlet.http.*; import jakarta.validation.Valid; import org.springframework.beans.factory.annotation.Value; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.time.Duration; import java.util.Map;
@RestController @RequestMapping("/api/auth") public class AuthController {
 private final AuthService service; private final long refreshDays; private final boolean secureCookie;
 public AuthController(AuthService service,@Value("${app.security.refresh-token-days:7}") long days,@Value("${app.security.secure-cookie:false}") boolean secure){this.service=service;this.refreshDays=days;this.secureCookie=secure;}
 @PostMapping("/register") public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req){User u=service.register(req);return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message","Account created","user",service.view(u)));}
 @PostMapping("/login") public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req,HttpServletResponse res){var a=service.login(req);setCookie(res,a.refreshToken());return ResponseEntity.ok(a.response());}
 @PostMapping("/refresh") public ResponseEntity<AuthResponse> refresh(@CookieValue(name="chainauth_refresh",required=false) String raw,HttpServletResponse res){var a=service.refresh(raw);setCookie(res,a.refreshToken());return ResponseEntity.ok(a.response());}
 @PostMapping("/logout") public ResponseEntity<?> logout(@CookieValue(name="chainauth_refresh",required=false) String raw,HttpServletResponse res){service.logout(raw);clearCookie(res);return ResponseEntity.ok(Map.of("message","Logged out"));}
 private void setCookie(HttpServletResponse res,String token){ResponseCookie cookie=ResponseCookie.from("chainauth_refresh",token).httpOnly(true).secure(secureCookie).sameSite("Strict").path("/api/auth").maxAge(Duration.ofDays(refreshDays)).build();res.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());}
 private void clearCookie(HttpServletResponse res){ResponseCookie cookie=ResponseCookie.from("chainauth_refresh","").httpOnly(true).secure(secureCookie).sameSite("Strict").path("/api/auth").maxAge(Duration.ZERO).build();res.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());}
}
