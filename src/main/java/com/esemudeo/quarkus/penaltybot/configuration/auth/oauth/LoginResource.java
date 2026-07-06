package com.esemudeo.quarkus.penaltybot.configuration.auth.oauth;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import com.esemudeo.quarkus.penaltybot.configuration.auth.model.AuthFlowToken;
import com.esemudeo.quarkus.penaltybot.configuration.auth.repository.AuthFlowTokenRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

/**
 * Entry point of the OAuth login. Generates a one-time CSRF {@code state}, persists it,
 * and redirects the browser to Discord's authorize endpoint (scope {@code identify}).
 */
@Path("/login")
public class LoginResource {

    private static final int STATE_VALIDITY_MINUTES = 5;
    private static final int STATE_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Inject
    AuthFlowTokenRepository authFlowTokenRepository;

    @ConfigProperty(name = "discord.oauth.client-id")
    String clientId;

    @ConfigProperty(name = "discord.oauth.redirect-uri")
    String redirectUri;

    @GET
    public Response login() {
        String state = generateState();
        authFlowTokenRepository.save(AuthFlowToken.builder()
                .token(state)
                .expiresAt(Instant.now().plus(STATE_VALIDITY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build());

        String discordOAuthUrl = "https://discord.com/oauth2/authorize"
                + "?client_id=" + clientId
                + "&response_type=code"
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&scope=identify"
                + "&state=" + state;

        return Response.seeOther(URI.create(discordOAuthUrl)).build();
    }

    private static String generateState() {
        byte[] bytes = new byte[STATE_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
