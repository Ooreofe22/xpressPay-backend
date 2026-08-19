package com.payxpress.biller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Gated by the same X-Admin-Key shared secret as AdminController.
 * NOTE: everything under /admin/** is permitAll() at the Spring Security layer
 * (see SecurityConfig) — this header check is the *only* thing protecting this
 * endpoint, so it must stay in sync with AdminController's checkKey(). This was
 * previously unguarded (the old TODO here claiming JWT protection was stale —
 * /admin/** bypasses the JWT filter's authorization entirely).
 */
@RestController
@RequestMapping("/admin/billers")
public class BillerSyncController {

    private final BillerSyncService billerSyncService;

    @Value("${admin.api-key:changeme-admin-key}")
    private String adminApiKey;

    public BillerSyncController(BillerSyncService billerSyncService) {
        this.billerSyncService = billerSyncService;
    }

    @PostMapping("/sync")
    public ResponseEntity<?> sync(@RequestHeader("X-Admin-Key") String key) {
        if (!adminApiKey.equals(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid admin key"));
        }
        billerSyncService.syncAllBillers();
        return ResponseEntity.ok(Map.of("message", "Biller catalog sync complete"));
    }
}
