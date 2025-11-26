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
public class MediaCollectionSource extends MediaCollection {

    public MediaCollectionSource() {

    }

    public MediaCollectionSource(String key, String sourceName) {
        super(key, sourceName);
    }

    public MediaCollectionSource(String key, String sourceName, String artUri) {
        super(key, sourceName);

        this.artUri = artUri;
    }

}
