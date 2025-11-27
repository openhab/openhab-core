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

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.media.model.MediaEntry;
import org.openhab.core.media.model.MediaRegistry;

/**
 * This is an interface that is
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public interface MediaService {
    MediaRegistry getMediaRegistry();

    Map<String, MediaSink> getMediaSinks();

    void addMediaListenner(String key, MediaListenner mediaListenner);

    Map<String, MediaListenner> getAllMediaListenner();

    @Nullable
    MediaListenner getMediaListenner(String key);

    void addMediaSink(MediaSink mediaSink);

    void removeMediaSink(MediaSink mediaSink);

    @Nullable
    String getProxy(String key);

    String handleImageProxy(String uri);

    void setBaseUri(String baseUri);

    <T extends BaseDto, R extends MediaEntry> void RegisterCollections(MediaEntry parentEntry, List<T> collection,
            Class<R> allocator);

}
