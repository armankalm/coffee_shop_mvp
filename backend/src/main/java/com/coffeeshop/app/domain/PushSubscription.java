package com.coffeeshop.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** A browser's Web Push subscription: where to send and the keys to encrypt for it. */
@Entity
@Table(name = "push_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 1000)
    private String endpoint;

    /** Browser's P-256 public key, base64url (uncompressed point). */
    @Column(nullable = false, length = 200)
    private String p256dh;

    /** Browser's auth secret, base64url. */
    @Column(nullable = false, length = 100)
    private String auth;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
