package com.chainauth.security;
import com.chainauth.user.*; import org.springframework.security.core.userdetails.*; import org.springframework.stereotype.Service;
@Service public class CustomUserDetailsService implements UserDetailsService {
 private final UserRepository users; public CustomUserDetailsService(UserRepository users){this.users=users;}
 @Override public UserDetails loadUserByUsername(String email){ User u=users.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("User not found"));
  return org.springframework.security.core.userdetails.User.withUsername(u.getEmail()).password(u.getPasswordHash()).disabled(!u.isEnabled()).authorities(u.getRoles().stream().map(r->"ROLE_"+r.name()).toArray(String[]::new)).build(); }
}
