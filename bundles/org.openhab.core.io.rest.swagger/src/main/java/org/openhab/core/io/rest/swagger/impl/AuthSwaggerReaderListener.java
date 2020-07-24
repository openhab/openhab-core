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
package org.openhab.core.io.rest.swagger.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.Swagger;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.auth.SecuritySchemeDefinition;

/**
 * This class adds a security definition to the Swagger object for the OAuth2 flow.
 *
 * @author Yannick Schaus - initial contribution
 */
@Component
@NonNullByDefault
public class AuthSwaggerReaderListener implements ReaderListener {
    public static final String OAUTH_AUTHORIZE_ENDPOINT = "/auth/authorize";
    public static final String OAUTH_TOKEN_ENDPOINT = "/rest/auth/token";

    private final Logger logger = LoggerFactory.getLogger(AuthSwaggerReaderListener.class);

    @Override
    public void beforeScan(@NonNullByDefault({}) Reader reader, @NonNullByDefault({}) Swagger swagger) {
        logger.debug("Adding securityDefinition to Swagger");
        SecuritySchemeDefinition oauth2Definition = new OAuth2Definition()
                .accessCode(OAUTH_AUTHORIZE_ENDPOINT, OAUTH_TOKEN_ENDPOINT).scope("admin", "Administration operations");
        Map<String, SecuritySchemeDefinition> securityDefinitions = new HashMap<>();
        securityDefinitions.put("oauth2", oauth2Definition);
        swagger.setSecurityDefinitions(securityDefinitions);
    }

    @Override
    public void afterScan(@NonNullByDefault({}) Reader reader, @NonNullByDefault({}) Swagger swagger) {
    }
}
