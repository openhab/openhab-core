package org.openhab.core.io.rest.auth.internal;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.GenericUser;

@NonNullByDefault
public class JwtSecurityContext implements SecurityContext {

    Authentication authentication;

    public JwtSecurityContext(Authentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public Principal getUserPrincipal() {
        return new GenericUser(authentication.getUsername());
    }

    @Override
    public boolean isUserInRole(@Nullable String role) {
        return authentication.getRoles().contains(role);
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public String getAuthenticationScheme() {
        return "JWT";
    }
}
