/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.internal.auth;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.smarthome.core.auth.Authentication;
import org.eclipse.smarthome.core.auth.AuthenticationException;
import org.eclipse.smarthome.core.auth.AuthenticationManager;
import org.eclipse.smarthome.core.auth.AuthenticationProvider;
import org.eclipse.smarthome.core.auth.Credentials;
import org.eclipse.smarthome.core.auth.UnsupportedCredentialsException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of authentication manager.
 *
 * @author Łukasz Dywicki - Initial contribution and API
 *
 */
@Component
public class AuthenticationManagerImpl implements AuthenticationManager {

    private final Logger logger = LoggerFactory.getLogger(AuthenticationManagerImpl.class);

    private final List<AuthenticationProvider> providers = new CopyOnWriteArrayList<>();

    @Override
    public Authentication authenticate(Credentials credentials) throws AuthenticationException {
        boolean unmatched = true;
        for (AuthenticationProvider provider : providers) {
            if (provider.supports(credentials.getClass())) {
                unmatched = false;
                try {
                    Authentication authentication = provider.authenticate(credentials);
                    if (authentication != null) {
                        return authentication;
                    }
                } catch (AuthenticationException e) {
                    logger.info("Failed to authenticate credentials {} with provider {}", credentials.getClass(),
                            provider, e);
                }
            }
        }

        if (unmatched) {
            throw new UnsupportedCredentialsException("Unsupported credentials specified " + credentials.getClass());
        }
        throw new AuthenticationException("Could not authenticate credentials " + credentials.getClass());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addAuthenticationProvider(AuthenticationProvider provider) {
        providers.add(provider);
    }

    public void removeAuthenticationProvider(AuthenticationProvider provider) {
        providers.remove(provider);
    }
}
