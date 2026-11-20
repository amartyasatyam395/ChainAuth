package com.chainauth.user;
import com.chainauth.auth.AuthService; import com.chainauth.user.dto.*; import jakarta.validation.Valid; import org.springframework.http.ResponseEntity; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.util.Map;
@RestController @RequestMapping("/api") public class UserController {
 private final UserService service; private final AuthService auth; public UserController(UserService service,AuthService auth){this.service=service;this.auth=auth;}
 @GetMapping("/auth/me") public Object authMe(Authentication a){return auth.view(service.get(a.getName()));}
 @GetMapping("/users/me") public Object me(Authentication a){return auth.view(service.get(a.getName()));}
 @PutMapping("/users/me") public Object update(Authentication a,@Valid @RequestBody ProfileUpdateRequest req){return auth.view(service.update(a.getName(),req));}
 @PostMapping("/users/me/change-password") public ResponseEntity<?> changePassword(Authentication a,@Valid @RequestBody ChangePasswordRequest req){service.changePassword(a.getName(),req);return ResponseEntity.ok(Map.of("message","Password changed. Please sign in again."));}
 @GetMapping("/users/me/security") public Object security(Authentication a){User u=service.get(a.getName());return Map.of("roles",u.getRoles(),"accountEnabled",u.isEnabled(),"createdAt",u.getCreatedAt());}
}
