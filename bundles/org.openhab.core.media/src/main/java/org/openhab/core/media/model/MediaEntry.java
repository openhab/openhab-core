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
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public class MediaEntry {
    private final Logger logger = LoggerFactory.getLogger(MediaEntry.class);

    private @Nullable MediaEntry parent;
    private @Nullable MediaRegistry registry;
    private String key;
    private String name;

    public MediaEntry(String key, String name) {
        this.name = name;
        this.key = key;
    }

    public <T extends @Nullable MediaEntry> T registerEntry(String key, MediaAllocator<T> allocator) {
        registry = getMediaRegistry();

        String entryPath = getPath() + "/" + key;
        T result = (T) registry.getEntry(entryPath);
        if (result == null) {
            result = allocator.alloc();
            result.setParent(this);
            if (registry != null) {
                registry.addEntry(result);
            }

            addChild(key, result);
        }

        return result;
    }

    public void addChild(String key, MediaEntry childEntry) {

    }

    public @Nullable MediaSource getMediaSource() {
        if (this instanceof MediaSource) {
            if (parent instanceof MediaSource) {
                return (MediaSource) parent;
            }
            return (MediaSource) this;
        }

        if (parent != null) {
            return parent.getMediaSource();
        }

        return null;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public void setName(String name) {
        this.name = name;
    }

    public @Nullable MediaEntry getParent() {
        return parent;
    }

    public void setParent(MediaEntry parent) {
        this.parent = parent;
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
        if (parent != null) {
            return parent.getPath() + "/" + getKey();

        } else {
            return "/Root";
        }
    }

    public String getSubPath() {
        if (parent != null && !(parent instanceof MediaSource)) {
            return parent.getSubPath() + "/" + getKey();

        } else {
            return "/" + getKey();
        }
    }

    public int getLevel() {
        if (parent != null) {
            return parent.getLevel() + 1;
        } else {
            return 0;
        }
    }

    private String empty = "                                                                       ";

    public void print() {
        int level = getLevel();
        logger.debug(String.format("%s %d - MediaEntry %s - %s", empty.substring(0, level * 4), level, key, name));
    }

}
