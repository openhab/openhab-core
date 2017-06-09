/**
 * Copyright (c) 2015-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.rest.docs.internal;

import org.openhab.ui.dashboard.DashboardTile;

/**
 * The dashboard tile for the REST API,
 *
 * @author Kai Kreuzer
 *
 */
public class RESTDashboardTile implements DashboardTile {

    protected void activate() {
    }

    protected void deactivate() {
    }

    @Override
    public String getName() {
        return "REST API";
    }

    @Override
    public String getUrl() {
        return "../doc/index.html";
    }

    @Override
    public String getOverlay() {
        return null;
    }

    @Override
    public String getImageUrl() {
        return "../doc/images/dashboardtile.png";
    }

}
