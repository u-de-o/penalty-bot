package com.esemudeo.quarkus.penaltybot.configuration.auth.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import com.esemudeo.quarkus.penaltybot.configuration.auth.model.AuthFlowToken;

import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class AuthFlowTokenRepository {

	public Optional<AuthFlowToken> findValidByToken(String token) {
		return AuthFlowToken.find("token = ?1 and used = false and expiresAt > ?2", token, Instant.now())
				.firstResultOptional();
	}

	public void markAsUsed(String token) {
		AuthFlowToken.update("used = true where token = ?1", token);
	}

	public void save(AuthFlowToken authFlowToken) {
		authFlowToken.persist();
	}
}
