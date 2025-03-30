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
import org.eclipse.jdt.annotation.Nullable;

/**
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public class MediaRegistry extends MediaEntry {
    private Map<String, MediaEntry> entries;

    public MediaRegistry() {
        entries = new HashMap<String, MediaEntry>();
    }

    public void addPath(String key, MediaEntry mediaEntry) {
        entries.put(mediaEntry.getPath(), mediaEntry);
    }

    public @Nullable MediaEntry getChildForPath(String path) {
        if (path.equals("/Root")) {
            return this;
        } else {
            return entries.get(path);
        }
    }
}
