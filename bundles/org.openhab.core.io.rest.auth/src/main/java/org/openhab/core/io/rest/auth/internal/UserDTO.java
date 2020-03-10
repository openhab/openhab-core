package org.openhab.core.io.rest.auth.internal;

import java.util.Collection;

import org.openhab.core.auth.User;

public class UserDTO {
    String name;
    Collection<String> roles;

    public UserDTO(User user) {
        super();
        this.name = user.getName();
        this.roles = user.getRoles();
    }
}
