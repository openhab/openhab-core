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
import org.osgi.service.component.annotations.Component;

/**
 * The dashboard tile for Home Builder
 *
 * @author Kuba Wolanin - Initial contribution
 *
 */
@Component
public class HomeBuilderDashboardTile implements DashboardTile {

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
