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
public class MediaCollection extends MediaEntry {
    private String name;
    private String key;

    public MediaCollection(String key, String name) {
        this.name = name;
        this.key = key;

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getKey() {
        return key;
    }
}
