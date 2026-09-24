package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.push.PushSubscriptionRequest;
import com.coffeeshop.app.service.push.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushNotificationService pushService;

    public PushController(PushNotificationService pushService) {
        this.pushService = pushService;
    }

    /** Whether push is configured on the server and the VAPID key browsers subscribe with. */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
                "enabled", pushService.isEnabled(),
                "publicKey", pushService.isEnabled() ? pushService.getPublicKey() : ""));
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<Void> subscribe(Authentication authentication,
                                          @Valid @RequestBody PushSubscriptionRequest request) {
        if (request.getKeys() == null) {
            throw new IllegalArgumentException("Push subscription keys are required");
        }
        pushService.subscribe(authentication.getName(), request.getEndpoint(),
                request.getKeys().getP256dh(), request.getKeys().getAuth());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/subscriptions")
    public ResponseEntity<Void> unsubscribe(Authentication authentication,
                                            @RequestBody PushSubscriptionRequest request) {
        pushService.unsubscribe(authentication.getName(), request.getEndpoint());
        return ResponseEntity.noContent().build();
    }
}
