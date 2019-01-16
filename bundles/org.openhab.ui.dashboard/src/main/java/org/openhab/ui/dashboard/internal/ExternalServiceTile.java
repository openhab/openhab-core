/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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

    @Override
    public String toString() {
        final int MAXLEN = 100;

        String limitedImageUrl = imageUrl;
        if (limitedImageUrl != null && limitedImageUrl.length() > MAXLEN) {
            limitedImageUrl = imageUrl.substring(0, MAXLEN) + "...";
        }

        return "[name=" + name + ", url=" + url + ", overlay=" + overlay + ", imageUrl=" + limitedImageUrl + "]";
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
