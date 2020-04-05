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
package org.openhab.core.ui.tiles;

/**
 * The dashboard tile for external services.
 *
 * @author Pauli Anttila - Initial contribution
 * @author Yannick Schaus - moved into core, remove references to dashboard
 */
public class ExternalServiceTile implements Tile {
    private String name;
    private String url;
    private String overlay;
    private String imageUrl;

    private ExternalServiceTile(TileBuilder builder) {
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
        final int maxlen = 100;

        String limitedImageUrl = imageUrl;
        if (limitedImageUrl != null && limitedImageUrl.length() > maxlen) {
            limitedImageUrl = imageUrl.substring(0, maxlen) + "...";
        }

        return "[name=" + name + ", url=" + url + ", overlay=" + overlay + ", imageUrl=" + limitedImageUrl + "]";
    }

    public static class TileBuilder {

        private String name;
        private String url;
        private String overlay;
        private String imageUrl;

        public TileBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public TileBuilder withUrl(String url) {
            this.url = url;
            return this;
        }

        public TileBuilder withOverlay(String overlay) {
            this.overlay = overlay;
            return this;
        }

        public TileBuilder withImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public ExternalServiceTile build() {
            return new ExternalServiceTile(this);
        }
    }
}
