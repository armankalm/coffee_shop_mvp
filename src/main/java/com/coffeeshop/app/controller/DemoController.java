package com.coffeeshop.app.controller;

import com.coffeeshop.app.service.DemoDataService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Profile("demo")
@RestController
@RequestMapping("/api/admin/demo")
@PreAuthorize("hasRole('ADMIN')")
public class DemoController {

    private final DemoDataService demoDataService;

    public DemoController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        demoDataService.reset();
        Map<String, Object> response = Map.of(
                "message", "Demo data reset successfully",
                "stats", demoDataService.stats()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(demoDataService.stats());
    }
}
