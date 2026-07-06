package com.esemudeo.quarkus.penaltybot.configuration.auth.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One-time token used to bridge the JAX-RS OAuth endpoints and the Vaadin session.
 * Serves two purposes distinguished by whether {@code userId} is set:
 * <ul>
 *     <li>CSRF {@code state} (userId null): created at {@code /login}, echoed by Discord,
 *     validated and consumed at {@code /oauth/callback}.</li>
 *     <li>Handoff (userId set): created at {@code /oauth/callback}, consumed by the
 *     guild-selection view to populate the authenticated user into the Vaadin session.</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "auth_flow_token")
public class AuthFlowToken extends PanacheEntity {
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "token", nullable = false, unique = true)
    private String token;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "used", nullable = false)
    @Setter
    private boolean used;
}
