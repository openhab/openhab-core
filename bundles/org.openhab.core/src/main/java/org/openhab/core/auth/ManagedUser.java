package org.openhab.core.auth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class ManagedUser implements User {

    private String name;
    private String passwordHash;
    private String passwordSalt;
    private Set<String> roles = new HashSet<>();
    private @Nullable PendingToken pendingToken = null;
    private List<UserSession> sessions = new ArrayList<>();
    private List<UserApiToken> apiTokens = new ArrayList<>();

    protected void setName(String name) {
        this.name = name;
    }

    public ManagedUser(String name, String passwordSalt, String passwordHash) {
        super();
        this.name = name;
        this.passwordSalt = passwordSalt;
        this.passwordHash = passwordHash;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUID() {
        return name;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public @Nullable PendingToken getPendingToken() {
        return pendingToken;
    }

    public void setPendingToken(@Nullable PendingToken pendingToken) {
        this.pendingToken = pendingToken;
    }

    public List<UserSession> getSessions() {
        return sessions;
    }

    public void setSessions(List<UserSession> sessions) {
        this.sessions = sessions;
    }

    public List<UserApiToken> getApiTokens() {
        return apiTokens;
    }

    public void setApiTokens(List<UserApiToken> apiTokens) {
        this.apiTokens = apiTokens;
    }

}
