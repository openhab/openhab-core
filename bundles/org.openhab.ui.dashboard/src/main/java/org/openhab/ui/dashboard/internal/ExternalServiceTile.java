/**
 * Copyright (c) 2015-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.ui.dashboard.internal;

import org.openhab.ui.dashboard.DashboardTile;

/**
 * The dashboard tile for external services.
 *
 * @author Pauli Anttila - Initial contribution
 */
public class ExternalServiceTile implements DashboardTile {
    private String name;
    private String url;
    private String overlay;
    private String imageUrl;

    private ExternalServiceTile(DashboardTileBuilder builder) {
        this.name = builder.name;
        this.url = builder.url;
        this.overlay = builder.overlay;
        this.imageUrl = builder.imageUrl;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getOverlay() {
        return overlay;
    }

    @Override
    public String getImageUrl() {
        return imageUrl;
    }

    public static class DashboardTileBuilder {

        private String name;
        private String url;
        private String overlay;
        private String imageUrl;

        public DashboardTileBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public DashboardTileBuilder withUrl(String url) {
            this.url = url;
            return this;
        }

        public DashboardTileBuilder withOverlay(String overlay) {
            this.overlay = overlay;
            return this;
        }

        public DashboardTileBuilder withImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public ExternalServiceTile build() {
            return new ExternalServiceTile(this);
        }
    }
}
