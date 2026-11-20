package com.chainauth.user;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.Map;
@RestController @RequestMapping("/api/admin") public class AdminController {
 @GetMapping("/status") @PreAuthorize("hasRole('ADMIN')") public Map<String,String> status(){return Map.of("status","AUTHORIZED","scope","ADMIN");}
}
