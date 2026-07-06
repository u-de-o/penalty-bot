package com.esemudeo.quarkus.penaltybot.configuration.auth.oauth;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import com.esemudeo.quarkus.penaltybot.configuration.auth.model.AuthFlowToken;
import com.esemudeo.quarkus.penaltybot.configuration.auth.repository.AuthFlowTokenRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Path("/oauth/callback")
public class OAuthCallbackResource {

    private static final int HANDOFF_VALIDITY_MINUTES = 2;

    @Inject
    Logger log;

    @Inject
    AuthFlowTokenRepository authFlowTokenRepository;

    @RestClient
    DiscordOAuthClient discordOAuthClient;

    @ConfigProperty(name = "discord.oauth.client-id")
    String clientId;

    @ConfigProperty(name = "discord.oauth.client-secret")
    String clientSecret;

    @ConfigProperty(name = "discord.oauth.redirect-uri")
    String redirectUri;

    @GET
    @Transactional
    public Response callback(@QueryParam("code") String code, @QueryParam("state") String state) {
        if (code == null || state == null) {
            log.warn("OAuth callback called without code or state");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        // state is the CSRF token we generated at /login. Discord echoes it back unchanged.
        Optional<AuthFlowToken> stateOpt = authFlowTokenRepository.findValidByToken(state);
        if (stateOpt.isEmpty()) {
            log.warn("OAuth callback: no valid state token found");
            return Response.seeOther(URI.create("/error")).build();
        }
        authFlowTokenRepository.markAsUsed(state);

        DiscordTokenResponse tokenResponse;
        try {
            tokenResponse = discordOAuthClient.exchangeCode(clientId, clientSecret, "authorization_code", code, redirectUri);
        } catch (Exception e) {
            log.error("OAuth callback: Discord token exchange failed", e);
            return Response.seeOther(URI.create("/error")).build();
        }

        DiscordUser discordUser;
        try {
            discordUser = discordOAuthClient.getCurrentUser("Bearer " + tokenResponse.accessToken());
        } catch (Exception e) {
            log.error("OAuth callback: Discord user fetch failed", e);
            return Response.seeOther(URI.create("/error")).build();
        }

        long discordUserId;
        try {
            discordUserId = Long.parseLong(discordUser.id());
        } catch (NumberFormatException e) {
            log.error("OAuth callback: Discord user ID is not a valid long: %s".formatted(discordUser.id()));
            return Response.seeOther(URI.create("/error")).build();
        }

        // Create a short-lived handoff token carrying the authenticated user. The
        // guild-selection view validates it and writes the userId into the VaadinSession.
        String handoffToken = UUID.randomUUID().toString();
        authFlowTokenRepository.save(AuthFlowToken.builder()
                .userId(discordUserId)
                .token(handoffToken)
                .expiresAt(Instant.now().plus(HANDOFF_VALIDITY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build());

        return Response.seeOther(URI.create("/guilds?token=" + handoffToken)).build();
    }
}
