package com.coffeeshop.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Minimal client for the Supabase Storage REST API. Render's disk is wiped on every deploy,
 * so product images live in a public Supabase bucket instead.
 */
@Component
public class SupabaseStorageClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final String bucket;

    public SupabaseStorageClient(RestClient.Builder restClientBuilder,
                                 @Value("${app.storage.supabase-url:}") String supabaseUrl,
                                 @Value("${app.storage.supabase-key:}") String serviceKey,
                                 @Value("${app.storage.bucket:products}") String bucket) {
        this.baseUrl = supabaseUrl.replaceAll("/+$", "");
        this.bucket = bucket;
        RestClient.Builder builder = restClientBuilder
                .baseUrl(this.baseUrl + "/storage/v1")
                .defaultHeader("apikey", serviceKey);
        // Legacy service_role keys are JWTs and also go in Authorization. New secret keys
        // (sb_secret_...) are not JWTs: sending them as a Bearer token is rejected.
        if (!serviceKey.startsWith("sb_")) {
            builder.defaultHeader("Authorization", "Bearer " + serviceKey);
        }
        this.restClient = builder.build();
        if (!this.baseUrl.isBlank() && serviceKey.isBlank()) {
            throw new IllegalStateException("SUPABASE_SERVICE_KEY must be set when SUPABASE_URL is configured");
        }
    }

    public boolean isEnabled() {
        return !baseUrl.isBlank();
    }

    /** Uploads the object and returns its public URL. */
    public String upload(String objectName, byte[] content, String contentType) {
        restClient.post()
                .uri("/object/{bucket}/{name}", bucket, objectName)
                .contentType(MediaType.parseMediaType(contentType))
                .header("x-upsert", "true")
                .body(content)
                .retrieve()
                .toBodilessEntity();
        return publicUrlPrefix() + objectName;
    }

    public void delete(String objectName) {
        restClient.delete()
                .uri("/object/{bucket}/{name}", bucket, objectName)
                .retrieve()
                .toBodilessEntity();
    }

    public String publicUrlPrefix() {
        return baseUrl + "/storage/v1/object/public/" + bucket + "/";
    }
}
