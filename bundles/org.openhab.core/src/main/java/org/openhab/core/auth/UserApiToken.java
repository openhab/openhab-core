package org.openhab.core.auth;

import java.util.Date;

public class UserApiToken {
    String name;
    String apiToken;
    Date createdTime;
    String scope;

    public UserApiToken(String name, String apiToken, String scope) {
        super();
        this.name = name;
        this.apiToken = apiToken;
        this.createdTime = new Date();
        this.scope = scope;
    }

    public String getApiToken() {
        return apiToken;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public String getScope() {
        return scope;
    }
}
