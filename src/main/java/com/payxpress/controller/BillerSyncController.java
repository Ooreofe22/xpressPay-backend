package com.payxpress.controller;

import com.payxpress.service.BillerSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * TODO: lock this behind an admin role once role-based auth exists. For now it
 * only requires a valid JWT (any logged-in user), which is fine for internal
 * testing but not for production — anyone with an account could re-trigger a
 * full catalog resync.
 */
@RestController
@RequestMapping("/admin/billers")
public class BillerSyncController {

    private final BillerSyncService billerSyncService;

    public BillerSyncController(BillerSyncService billerSyncService) {
        this.billerSyncService = billerSyncService;
    }

    @PostMapping("/sync")
    public Map<String, String> sync() {
        billerSyncService.syncAllBillers();
        return Map.of("message", "Biller catalog sync complete");
    }
}
