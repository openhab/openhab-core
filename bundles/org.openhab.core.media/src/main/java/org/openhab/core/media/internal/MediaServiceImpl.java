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
package org.openhab.core.media.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.media.MediaDevice;
import org.openhab.core.media.MediaListenner;
import org.openhab.core.media.MediaService;
import org.openhab.core.media.model.MediaRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Implementation of
 *
 * @author Laurent Arnal - Initial contribution
 *
 */
@NonNullByDefault
@Component(immediate = true)
public class MediaServiceImpl implements MediaService {
    private Map<String, MediaListenner> mediaListenner = new HashMap<String, MediaListenner>();
    private Map<String, MediaDevice> mediaDevices = new HashMap<String, MediaDevice>();

    public @Nullable MediaListenner listenner = null;
    public MediaRegistry registry = new MediaRegistry(this);

    private Map<String, String> proxyRegistry = new HashMap<String, String>();
    private String baseUri = "none";

    @Activate
    public MediaServiceImpl() {
        this.addProxySource("lyrionUpnp1", "http://192.168.254.1:9000/");
        this.addProxySource("lyrionUpnp2", "http://192.168.0.1:9000/");
        this.addProxySource("emby", "http://192.168.254.1:8096/");

    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    @Override
    public String handleImageProxy(String uri) {
        String result = uri;
        for (String key : proxyRegistry.keySet()) {
            String proxyUri = proxyRegistry.get(key);
            if (proxyUri != null) {
                result = result.replace(proxyUri, baseUri + "/proxy/" + key + "/");
            }
        }
        return result;
    }

    @Override
    public @Nullable String getProxy(String key) {
        if (!proxyRegistry.containsKey(key)) {
            return null;
        }

        return proxyRegistry.get(key);
    }

    public void addProxySource(String source, String uri) {
        proxyRegistry.put(source, uri);
    }

    @Override
    public MediaRegistry getMediaRegistry() {
        return registry;
    }

    @Override
    public void addMediaListenner(String key, MediaListenner listenner) {
        mediaListenner.put(key, listenner);
    }

    @Override
    public @Nullable MediaListenner getMediaListenner(String key) {
        // TODO Auto-generated method stub
        if (mediaListenner.containsKey(key)) {
            return mediaListenner.get(key);
        }

        return null;
    }

    @Override
    public void registerDevice(MediaDevice device) {
        mediaDevices.put(device.getId(), device);
    }

    @Override
    public Map<String, MediaDevice> getMediaDevices() {
        return mediaDevices;
    }

    public Map<String, MediaListenner> getMediaListenners() {
        return mediaListenner;
    }

}
