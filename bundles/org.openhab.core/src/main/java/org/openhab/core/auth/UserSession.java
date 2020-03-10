package org.openhab.core.auth;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class UserSession {
    String refreshToken;
    Date createdTime;
    Date lastRefreshTime;
    String scope;

    public UserSession(String refreshToken, String scope) {
        super();
        this.refreshToken = refreshToken;
        this.createdTime = new Date();
        this.lastRefreshTime = new Date();
        this.scope = scope;
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
}
