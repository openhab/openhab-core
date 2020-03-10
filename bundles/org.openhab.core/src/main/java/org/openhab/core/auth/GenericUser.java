package org.openhab.core.auth;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class GenericUser implements User {

    public String name;
    public Set<String> roles;

    public GenericUser(String name, Set<String> roles) {
        this.name = name;
        this.roles = roles;
    }

    public GenericUser(String name) {
        this(name, new HashSet<>());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public @NonNull String getUID() {
        return name;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }
}
