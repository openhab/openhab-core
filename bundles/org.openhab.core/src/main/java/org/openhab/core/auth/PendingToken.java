package org.openhab.core.auth;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class PendingToken {
    private String authorizationCode;
    private String clientId;
    private String scope;

    public PendingToken(String authorizationCode, String clientId, String scope) {
        super();
        this.authorizationCode = authorizationCode;
        this.clientId = clientId;
        this.scope = scope;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public String getClientId() {
        return clientId;
    }

    public String getScope() {
        return scope;
    }
}
