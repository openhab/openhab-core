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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.media.MediaService;

/**
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public class MediaCollection extends MediaEntry {
    private Map<String, MediaEntry> childs;
    public String artUri = "/static/playlist.png";

    public MediaCollection(String key, String name) {
        super(key, name);

        if (name.indexOf("Artistes") >= 0) {
            artUri = "/static/Artists.png";
        } else if (name.indexOf("Albums") >= 0) {
            artUri = "/static/Albums.png";
        } else if (name.indexOf("Dossiers") >= 0) {
            artUri = "/static/Folder.png";
        }

        childs = new HashMap<String, MediaEntry>();
    }

    public MediaCollection(String key, String name, String artUri) {
        super(key, name);

        this.artUri = artUri;
        childs = new HashMap<String, MediaEntry>();
    }

    public Map<String, MediaEntry> getChilds() {
        return childs;
    }

    @Override
    public void print() {
        super.print();

        for (MediaEntry child : childs.values()) {
            child.print();
        }
    }

    @Override
    public void addChild(String key, MediaEntry childEntry) {
        childs.put(key, childEntry);
    }

    public String getArtUri() {
        return artUri;
    }

    public String getExternalArtUri() {
        MediaService mediaService = this.getMediaRegistry().getMediaService();
        String result = mediaService.handleImageProxy(artUri);
        return result;
    }

    public void setArtUri(String artUri) {
        this.artUri = artUri;
    }

}
