package org.openhab.core.auth;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Registry;

@NonNullByDefault
public interface UserRegistry extends Registry<User, String>, AuthenticationProvider {

    public User register(String username, String password, Set<String> roles);
}
