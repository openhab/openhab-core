/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.Credentials;
import org.openhab.core.auth.UserRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link LoginModule} delegates the authentication to a {@link UserRegistry}
 *
 * @author Yannick Schaus - initial contribution
 */
public class ManagedUserLoginModule implements LoginModule {

    private final Logger logger = LoggerFactory.getLogger(ManagedUserLoginModule.class);

    private UserRegistry userRegistry;

    private Subject subject;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {
        this.subject = subject;
    }

    @Override
    public boolean login() throws LoginException {
        try {
            // try to get the UserRegistry instance
            BundleContext bundleContext = FrameworkUtil.getBundle(UserRegistry.class).getBundleContext();
            ServiceReference<UserRegistry> serviceReference = bundleContext.getServiceReference(UserRegistry.class);

            userRegistry = bundleContext.getService(serviceReference);
        } catch (Exception e) {
            logger.error("Cannot initialize the ManagedLoginModule", e);
            throw new LoginException("Authorization failed");
        }

        try {
            Credentials credentials = (Credentials) this.subject.getPrivateCredentials().iterator().next();
            userRegistry.authenticate(credentials);
            return true;
        } catch (AuthenticationException e) {
            throw new LoginException(e.getMessage());
        }
    }

    @Override
    public boolean commit() throws LoginException {
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return false;
    }

    @Override
    public boolean logout() throws LoginException {
        return false;
    }
}
