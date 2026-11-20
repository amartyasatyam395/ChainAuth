package com.chainauth.user;
import com.chainauth.auth.TokenService; import com.chainauth.user.dto.*; import org.springframework.http.HttpStatus; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service; import org.springframework.web.server.ResponseStatusException; import java.time.Instant;
@Service public class UserService {
 private final UserRepository users; private final PasswordEncoder encoder; private final TokenService tokens;
 public UserService(UserRepository users,PasswordEncoder encoder,TokenService tokens){this.users=users;this.encoder=encoder;this.tokens=tokens;}
 public User get(String email){return users.findByEmail(email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));}
 public User update(String email,ProfileUpdateRequest req){User u=get(email);u.setName(req.name().trim());u.setUpdatedAt(Instant.now());return users.save(u);}
 public void changePassword(String email,ChangePasswordRequest req){if(req.newPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"New password must be at most 72 UTF-8 bytes");User u=get(email);if(!encoder.matches(req.currentPassword(),u.getPasswordHash()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Current password is incorrect");if(encoder.matches(req.newPassword(),u.getPasswordHash()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"New password must differ from current password");u.setPasswordHash(encoder.encode(req.newPassword()));u.setTokenVersion(u.getTokenVersion()+1);u.setUpdatedAt(Instant.now());users.save(u);tokens.revokeUser(u.getId());}
}
