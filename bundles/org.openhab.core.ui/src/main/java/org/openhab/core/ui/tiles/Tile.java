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
 * A tile can be registered by an UI as a service in order to appear on the main openHAB UI.
 *
 * @author Kai Kreuzer - initial contribution
 * @author Yannick Schaus - refactored into core, remove references to dashboard
 *
 */
public interface Tile {

    /**
     * The name that should appear on the tile
     *
     * @return name of the tile
     */
    String getName();

    /**
     * The url to point to (if it is a local UI, it should be a relative path starting with "../")
     *
     * @return the url
     */
    String getUrl();

    /**
     * The url to point to for the tile.
     * (if it is a local UI, it should be a relative path starting with "../")
     *
     * @return the tile url
     */
    String getImageUrl();

    /**
     * An HTML5 overlay icon to use for the tile, e.g. "html5", "android" or "apple".
     *
     * @return the overlay to use
     */
    String getOverlay();
}
