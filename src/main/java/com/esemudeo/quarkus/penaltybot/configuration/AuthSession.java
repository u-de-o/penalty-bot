package com.esemudeo.quarkus.penaltybot.configuration;

import com.vaadin.flow.server.VaadinSession;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.UUID;

/**
 * Provides access to the authenticated Discord identity for the current Vaadin session.
 * Backed by {@link VaadinSession} – only callable from Vaadin UI threads.
 * Populated by {@link com.esemudeo.quarkus.penaltybot.configuration.GuildSelectionView} after the
 * one-time handoff token has been validated.
 *
 * Each authentication generates a unique session nonce. Only the tab holding the current
 * nonce is considered active — older tabs are stale and blocked from further operations.
 */
@ApplicationScoped
public class AuthSession {

    private static final String KEY_USER_ID = "auth.userId";
    private static final String KEY_USER_NAME = "auth.userName";
    private static final String KEY_GUILD_ID = "auth.guildId";
    private static final String KEY_ACTIVE_NONCE = "auth.activeNonce";

    public boolean isNotAuthenticated() {
        return getUserId() == null || getGuildId() == null;
    }

    public boolean isUserAuthenticated() {
        return getUserId() != null;
    }

    public Long getUserId() {
        return (Long) VaadinSession.getCurrent().getAttribute(KEY_USER_ID);
    }

    public String getUserName() {
        return (String) VaadinSession.getCurrent().getAttribute(KEY_USER_NAME);
    }

    public void setUserName(String userName) {
        VaadinSession.getCurrent().setAttribute(KEY_USER_NAME, userName);
    }

    /** Clears the authenticated identity by invalidating the underlying HTTP session. */
    public void invalidateSession() {
        VaadinSession.getCurrent().getSession().invalidate();
    }

    public Long getGuildId() {
        return (Long) VaadinSession.getCurrent().getAttribute(KEY_GUILD_ID);
    }

    public void setUserId(long userId) {
        VaadinSession.getCurrent().setAttribute(KEY_USER_ID, userId);
    }

    public void setGuildId(long guildId) {
        VaadinSession.getCurrent().setAttribute(KEY_GUILD_ID, guildId);
    }

    public String rotateNonce() {
        String nonce = UUID.randomUUID().toString();
        VaadinSession.getCurrent().setAttribute(KEY_ACTIVE_NONCE, nonce);
        return nonce;
    }

    public String getActiveNonce() {
        return (String) VaadinSession.getCurrent().getAttribute(KEY_ACTIVE_NONCE);
    }

    public boolean isActiveNonce(String nonce) {
        String activeNonce = (String) VaadinSession.getCurrent().getAttribute(KEY_ACTIVE_NONCE);
        return Objects.equals(activeNonce, nonce);
    }
}
