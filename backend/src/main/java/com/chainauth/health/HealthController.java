package com.chainauth.health;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final HealthService healthService;
    public HealthController(HealthService healthService) { this.healthService = healthService; }

    @GetMapping
    public Map<String, Object> health() {
        return Map.of("service", "chainauth-api", "status", "UP", "timestamp", Instant.now().toString());
    }

    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> database() {
        Map<String, Object> result = healthService.database();
        return ResponseEntity.status("UP".equals(result.get("status")) ? 200 : 503).body(result);
    }

    @GetMapping("/blockchain")
    public ResponseEntity<Map<String, Object>> blockchain() {
        Map<String, Object> result = healthService.blockchain();
        return ResponseEntity.status("UP".equals(result.get("status")) ? 200 : 503).body(result);
    }
}
