package com.chainauth.audit;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/admin/audit") @PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {
    private final AuditService audit;
    public AdminAuditController(AuditService audit){this.audit=audit;}
    @GetMapping public Map<String,Object> recent(@RequestParam(defaultValue="50") int limit){return Map.of("events",audit.recent(limit),"chainValid",audit.verify(5000));}
    @GetMapping("/verify") public Map<String,Object> verify(){return Map.of("valid",audit.verify(5000),"scope","latest 5000 events");}
}
