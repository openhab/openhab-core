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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.media.BaseDto;
import org.openhab.core.media.MediaService;

/**
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public class MediaCollection extends MediaEntry {
    private Map<String, MediaEntry> maps;
    private List<MediaEntry> list = new ArrayList<>();
    public String artUri = "/static/playlist.png";

    public MediaCollection() {
        maps = new HashMap<String, MediaEntry>();
        list = new ArrayList<MediaEntry>();
    }

    public void Clear() {
        list.clear();
        maps.clear();
    }

    public MediaCollection(String key, String name) {
        super(key, name);

        if (name.indexOf("Artistes") >= 0) {
            artUri = "/static/Artists.png";
        } else if (name.indexOf("Albums") >= 0) {
            artUri = "/static/Albums.png";
        } else if (name.indexOf("Dossiers") >= 0) {
            artUri = "/static/Folder.png";
        }

        maps = new HashMap<String, MediaEntry>();
        list = new ArrayList<MediaEntry>();
    }

    public MediaCollection(String key, String name, String artUri) {
        super(key, name);

        this.artUri = artUri;
        maps = new HashMap<String, MediaEntry>();
    }

    public Map<String, MediaEntry> getChildsAsMap() {
        return maps;
    }

    public List<MediaEntry> getChildsAsArray() {
        return list;
    }

    @Override
    public void print() {
        super.print();

        for (MediaEntry child : list) {
            child.print();
        }
    }

    @Override
    public void addChild(String key, MediaEntry childEntry) {
        if (!maps.containsKey(key)) {
            maps.put(key, childEntry);
            list.add(childEntry);
        }
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

    @Override
    public void initFrom(BaseDto dto) {
        this.artUri = dto.getArtwork();

    }

}
