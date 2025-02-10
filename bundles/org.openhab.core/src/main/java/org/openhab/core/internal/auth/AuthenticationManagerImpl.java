/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.internal.auth;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.AuthenticationManager;
import org.openhab.core.auth.AuthenticationProvider;
import org.openhab.core.auth.Credentials;
import org.openhab.core.auth.UnsupportedCredentialsException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of authentication manager.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
@Component
@NonNullByDefault
public class AuthenticationManagerImpl implements AuthenticationManager {

    private final Logger logger = LoggerFactory.getLogger(AuthenticationManagerImpl.class);

    private final List<AuthenticationProvider> providers = new CopyOnWriteArrayList<>();

    @Override
    public Authentication authenticate(Credentials credentials) throws AuthenticationException {
        boolean unmatched = true;
        Class<? extends Credentials> credentialsClazz = credentials.getClass();
        for (AuthenticationProvider provider : providers) {
            if (provider.supports(credentialsClazz)) {
                unmatched = false;
                try {
                    Authentication authentication = provider.authenticate(credentials);
                    return authentication;
                } catch (AuthenticationException e) {
                    logger.info("Failed to authenticate credentials {} with provider {}", credentialsClazz, provider);
                }
            }
        }

        if (unmatched) {
            throw new UnsupportedCredentialsException("Unsupported credentials specified " + credentialsClazz);
        }
        throw new AuthenticationException("Could not authenticate credentials " + credentialsClazz);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addAuthenticationProvider(AuthenticationProvider provider) {
        providers.add(provider);
    }

    public void removeAuthenticationProvider(AuthenticationProvider provider) {
        providers.remove(provider);
    }
}
