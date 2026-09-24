package com.coffeeshop.app.dto.push;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Mirrors the browser's PushSubscription.toJSON(): {endpoint, keys: {p256dh, auth}}. */
public class PushSubscriptionRequest {

    @NotBlank
    @Size(max = 1000)
    private String endpoint;

    @Valid
    private Keys keys;

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public Keys getKeys() { return keys; }
    public void setKeys(Keys keys) { this.keys = keys; }

    public static class Keys {
        @NotNull
        @Size(max = 200)
        private String p256dh;

        @NotNull
        @Size(max = 100)
        private String auth;

        public String getP256dh() { return p256dh; }
        public void setP256dh(String p256dh) { this.p256dh = p256dh; }

        public String getAuth() { return auth; }
        public void setAuth(String auth) { this.auth = auth; }
    }
}
