/**
 * Copyright (c) 2015-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.ui.classicui.internal;

import org.openhab.ui.dashboard.DashboardTile;
import org.osgi.service.component.annotations.Component;

/**
 * The dashboard tile for the Classic UI
 *
 * @author Kai Kreuzer
 *
 */
@Component
public class ClassicUIDashboardTile implements DashboardTile {

    @Override
    public String getName() {
        return "Classic UI";
    }

    @Override
    public String getUrl() {
        return "../classicui/app";
    }

    @Override
    public String getOverlay() {
        return null;
    }

    @Override
    public String getImageUrl() {
        return "img/classicui.png";
    }
}
