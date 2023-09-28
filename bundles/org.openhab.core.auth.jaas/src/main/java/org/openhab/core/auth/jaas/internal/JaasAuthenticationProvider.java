/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.auth.jaas.internal;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.AuthenticationProvider;
import org.openhab.core.auth.Credentials;
import org.openhab.core.auth.GenericUser;
import org.openhab.core.auth.UserApiTokenCredentials;
import org.openhab.core.auth.UsernamePasswordCredentials;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

/**
 * Implementation of authentication provider which is backed by JAAS realm.
 *
 * Real authentication logic is embedded in login modules implemented by 3rd party, this code is just for bridging it.
 *
 * @author Łukasz Dywicki - Initial contribution
 * @author Kai Kreuzer - Removed ManagedService and used DS configuration instead
 * @author Yannick Schaus - provides a configuration with the ManagedUserLoginModule as a sufficient login module
 */
@NonNullByDefault
@Component(configurationPid = "org.openhab.jaas")
public class JaasAuthenticationProvider implements AuthenticationProvider {
    private static final String DEFAULT_REALM = "openhab";
    static final String API_TOKEN_PREFIX = "oh.";

    private @Nullable String realmName;

    @Override
    public Authentication authenticate(final Credentials credentials) throws AuthenticationException {
        if (realmName == null) { // configuration is not yet ready or set
            realmName = DEFAULT_REALM;
        }

        if (!((credentials instanceof UsernamePasswordCredentials)
                || (credentials instanceof UserApiTokenCredentials))) {
            throw new AuthenticationException("Unsupported credentials passed to provider.");
        }

        Subject subject;

        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        if (credentials instanceof UserApiTokenCredentials) {
            UserApiTokenCredentials userCredentials = (UserApiTokenCredentials) credentials;
            final String token = userCredentials.getApiToken();

            subject = new Subject(false, Collections.emptySet(), Collections.emptySet(), Set.of(userCredentials));
            try {

                Thread.currentThread().setContextClassLoader(ApiTokenLoginModule.class.getClassLoader());
                LoginContext loginContext = new LoginContext(realmName, subject, null,
                        new ApiTokenLoginConfiguration());
                loginContext.login();

                return getAuthentication("", loginContext.getSubject());
            } catch (LoginException e) {
                String message = e.getMessage();
                throw new AuthenticationException(message != null ? message : "An unexpected LoginException occurred");
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }

        } else {
            UsernamePasswordCredentials userCredentials = (UsernamePasswordCredentials) credentials;
            final String name = userCredentials.getUsername();
            final char[] password = userCredentials.getPassword().toCharArray();

            subject = new Subject(false, Collections.emptySet(), Collections.emptySet(), Set.of(userCredentials));
            try {

                Thread.currentThread().setContextClassLoader(ManagedUserLoginModule.class.getClassLoader());
                LoginContext loginContext = new LoginContext(realmName, subject, null,
                        new ManagedUserLoginConfiguration());
                loginContext.login();
                return getAuthentication(name, loginContext.getSubject());
            } catch (LoginException e) {
                String message = e.getMessage();
                throw new AuthenticationException(message != null ? message : "An unexpected LoginException occurred");
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
    }

    private Authentication getAuthentication(String name, Subject subject) throws CredentialException {
        String username = name;

        if (subject.getPrincipals().isEmpty()) {
            throw new CredentialException("Missing logged in user information");
        }

        if (username.isBlank()) {
            username = ((GenericUser) subject.getPrincipals().iterator().next()).getName();
        }
        return new Authentication(username, getRoles(subject.getPrincipals()));
    }

    private String[] getRoles(Set<Principal> principals) throws CredentialException {
        GenericUser user = (GenericUser) principals.iterator().next();
        String[] roles = new String[user.getRoles().size()];

        int i = 0;
        for (String role : user.getRoles()) {
            roles[i++] = role;
        }
        return roles;
    }

    @Activate
    protected void activate(Map<String, Object> properties) {
        modified(properties);
    }

    @Deactivate
    protected void deactivate(Map<String, Object> properties) {
    }

    @Modified
    protected void modified(Map<String, Object> properties) {
        if (properties == null) {
            realmName = DEFAULT_REALM;
            return;
        }

        Object propertyValue = properties.get("realmName");
        if (propertyValue != null) {
            if (propertyValue instanceof String string) {
                realmName = string;
            } else {
                realmName = propertyValue.toString();
            }
        } else {
            // value could be unset, we should reset its value
            realmName = DEFAULT_REALM;
        }
    }

    @Override
    public boolean supports(Class<? extends Credentials> type) {
        return UsernamePasswordCredentials.class.isAssignableFrom(type);
    }
}
