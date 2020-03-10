package org.openhab.core.auth;

import java.security.Principal;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;

@NonNullByDefault
public interface User extends Principal, Identifiable<String> {

    public Set<String> getRoles();
}
