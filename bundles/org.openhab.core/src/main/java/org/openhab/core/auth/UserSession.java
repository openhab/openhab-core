package org.openhab.core.auth;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class UserSession {
    String sessionId;
    String refreshToken;
    Date createdTime;
    Date lastRefreshTime;
    String clientId;
    String redirectUri;
    String scope;
    boolean sessionCookie;

    public UserSession(String sessionId, String refreshToken, String clientId, String redirectUri, String scope) {
        super();
        this.sessionId = sessionId;
        this.refreshToken = refreshToken;
        this.createdTime = new Date();
        this.lastRefreshTime = new Date();
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.scope = scope;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public Date getLastRefreshTime() {
        return lastRefreshTime;
    }

    public void setLastRefreshTime(Date lastRefreshTime) {
        this.lastRefreshTime = lastRefreshTime;
    }

    public String getScope() {
        return scope;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public boolean hasSessionCookie() {
        return sessionCookie;
    }

    public void setSessionCookie(boolean sessionCookie) {
        this.sessionCookie = sessionCookie;
    }
}
