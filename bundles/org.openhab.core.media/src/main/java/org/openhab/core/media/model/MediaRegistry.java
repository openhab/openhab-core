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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public class MediaRegistry extends MediaCollection {
    private final Logger logger = LoggerFactory.getLogger(MediaEntry.class);
    private Map<String, MediaEntry> pathToEntry;

    public MediaRegistry() {
        super("Root", "Registry");
        pathToEntry = new HashMap<String, MediaEntry>();
        pathToEntry.put("/Root", this);
    }

    public void addEntry(MediaEntry mediaEntry) {
        pathToEntry.put(mediaEntry.getPath(), mediaEntry);
    }

    public @Nullable MediaEntry getEntry(String path) {
        return pathToEntry.get(path);
    }

    @Override
    public void print() {
        logger.debug("Registry:");
        super.print();

    }
}
