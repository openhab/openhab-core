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
public class MediaAlbum extends MediaCollection {

    private String artUri = "";
    private String artist = "";
    private String genre = "";

    public MediaAlbum(String key, String albumName) {
        super(key, albumName);

    }

    public void setArtUri(String artUri) {
        this.artUri = artUri;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getGenre() {
        return this.genre;
    }

    public String getArtist() {
        return this.artist;
    }

    public String getArtUri() {
        return this.artUri;
    }

}
