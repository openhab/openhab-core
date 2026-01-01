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
import org.openhab.core.media.BaseDto;

/**
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public class MediaTrack extends MediaEntry {
    private String artUri = "/static/Artists.png";
    private String artist = "";

    public MediaTrack() {

    }

    public MediaTrack(String key, String trackName) {
        super(key, trackName);
    }

    public void setArtUri(String artUri) {
        this.artUri = artUri;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getArtUri() {
        return this.artUri;
    }

    public String getArtist() {
        return this.artist;
    }

    @Override
    public void initFrom(BaseDto dto) {
        this.artUri = dto.getArtwork();
    }

}
