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
package org.openhab.core.auth.oauth2client.test.internal;

import java.util.Map;

import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For testing of AuthorizationCode against a real provider
 *
 * @author Gary Tse - Initial contribution
 */
@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, name = "AuthorizationCodeTestAgent", configurationPid = "AuthorizationCodeTestAgent")
public class AuthorizationCodeTestAgent extends AbstractTestAgent implements TestAgent {

    public static final String CONFIGURATION_PID = "AuthorizationCodeTestAgent";
    private final Logger logger = LoggerFactory.getLogger(AuthorizationCodeTestAgent.class);

    @Override
    @Activate
    public void activate(Map<String, Object> properties) {
        super.activate(properties);
        logger.debug("{} activated", this.getClass().getSimpleName());
    }

    @Override
    @Deactivate
    public void deactivate() {
        super.deactivate();
        logger.debug("{} deactivated", this.getClass().getSimpleName());
    }

    @Reference
    public void setOauthFactory(OAuthFactory oauthFactory) {
        super.oauthFactory = oauthFactory;
    }

    public void unsetOauthFactory(OAuthFactory oauthFactory) {
        if (super.oauthFactory == oauthFactory) {
            if (handle != null) {
                oauthFactory.ungetOAuthService(handle);
            }
            super.oauthFactory = null;
        }
    }
}
