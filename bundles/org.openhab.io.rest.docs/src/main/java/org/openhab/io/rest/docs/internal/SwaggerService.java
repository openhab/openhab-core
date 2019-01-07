/**
 * Copyright (c) 2015-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.rest.docs.internal;

import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service registers the Swagger UI as a web resource on the HTTP service.
 *
 * @author Kai Kreuzer
 *
 */
public class SwaggerService {

    private static final String ALIAS = "/doc";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private HttpService httpService;

    protected void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    protected void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }

    protected void activate() {
        try {
            httpService.registerResources(ALIAS, "swagger", httpService.createDefaultHttpContext());
        } catch (NamespaceException e) {
            logger.error("Could not start up REST documentation service: {}", e.getMessage());
        }
    }

    protected void deactivate() {
        httpService.unregister(ALIAS);
    }

}
