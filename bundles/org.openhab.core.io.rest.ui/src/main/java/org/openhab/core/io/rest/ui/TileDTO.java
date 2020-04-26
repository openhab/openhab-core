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
package org.openhab.core.io.rest.ui;

/**
 * This is an data transfer object for a UI tile.
 *
 * @author Yannick Schaus - Initial contribution
 */
public class TileDTO {

    public String name;
    public String url;
    public String overlay;
    public String imageUrl;

    public TileDTO(String name, String url, String overlay, String imageUrl) {
        super();
        this.name = name;
        this.url = url;
        this.overlay = overlay;
        this.imageUrl = imageUrl;
    }
}
