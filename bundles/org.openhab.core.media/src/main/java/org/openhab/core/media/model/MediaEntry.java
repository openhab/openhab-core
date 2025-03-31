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
public abstract class MediaEntry {
    private Map<String, MediaEntry> childs;
    private @Nullable MediaEntry parent;
    private @Nullable MediaRegistry registry;

    public MediaEntry() {
        childs = new HashMap<String, MediaEntry>();
    }

    public void addChild(String key, MediaEntry mediaEntry) {
        mediaEntry.parent = this;
        registry = getMediaRegistry();
        registry.addPath(mediaEntry.getPath(), mediaEntry);
        childs.put(key, mediaEntry);
    }

    public Map<String, MediaEntry> getChilds() {
        return childs;
    }

    public String getName() {
        return "Root";
    }

    public String getKey() {
        return "Root";
    }

    public @Nullable MediaEntry getParent() {
        return parent;
    }

    public @Nullable MediaRegistry getMediaRegistry() {
        MediaEntry entry = this;
        if (entry instanceof MediaRegistry) {
            return (MediaRegistry) this;
        }

        while ((entry = entry.getParent()) != null) {
            if (entry instanceof MediaRegistry) {
                return (MediaRegistry) entry;
            }
        }
        return null;
    }

    public String getPath() {
        if (parent == null) {
            return "/Root";
        } else {
            return parent.getPath() + "/" + getKey();
        }
    }

}
