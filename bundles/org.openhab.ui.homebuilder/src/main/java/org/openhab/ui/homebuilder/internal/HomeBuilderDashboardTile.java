/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.ui.homebuilder.internal;

import org.openhab.ui.dashboard.DashboardTile;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The dashboard tile and resource registering for Home Builder
 *
 * @author Kuba Wolanin - Initial contribution
 *
 */
public class HomeBuilderDashboardTile implements DashboardTile {

    public static final String HOMEBUILDER_ALIAS = "/homebuilder";

    private final Logger logger = LoggerFactory.getLogger(HomeBuilderDashboardTile.class);

    protected HttpService httpService;

    protected void activate() {
        try {
            httpService.registerResources(HOMEBUILDER_ALIAS, "web", null);
            logger.info("Started Home Builder at {}", HOMEBUILDER_ALIAS);
        } catch (NamespaceException e) {
            logger.error("Error during Home Builder startup: {}", e.getMessage());
        }
    }

    protected void deactivate() {
        httpService.unregister(HOMEBUILDER_ALIAS);
        logger.info("Stopped Home Builder");
    }

    protected void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    protected void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }

    @Override
    public String getName() {
        return "Home Builder";
    }

    @Override
    public String getUrl() {
        return "../homebuilder/index.html";
    }

    @Override
    public String getOverlay() {
        return null;
    }

    @Override
    public String getImageUrl() {
        return "../homebuilder/tile.png";
    }

}
