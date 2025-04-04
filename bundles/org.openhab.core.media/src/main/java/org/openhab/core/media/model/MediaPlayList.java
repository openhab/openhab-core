/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.media.model;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public class MediaPlayList extends MediaCollection {
    private String artUri = "";

    public MediaPlayList(String key, String playListName) {
        super(key, playListName);
    }

    public void setArtUri(String artUri) {
        this.artUri = artUri;
    }

    public String getArtUri() {
        return this.artUri;
    }

}
