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
package org.openhab.core.media;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.media.model.MediaRegistry;

/**
 * This is an interface that is
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public interface MediaService {
    public MediaRegistry getMediaRegistry();

    public Map<String, MediaDevice> getMediaDevices();

    public void addMediaListenner(String key, MediaListenner mediaListenner);

    public @Nullable MediaListenner getMediaListenner(String key);

    public void registerDevice(MediaDevice device);

    public @Nullable String getProxy(String key);

    public String handleImageProxy(String uri);

    public void setBaseUri(String baseUri);
}
