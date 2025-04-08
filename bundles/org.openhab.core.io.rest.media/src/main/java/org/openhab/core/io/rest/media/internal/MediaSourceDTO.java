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
package org.openhab.core.io.rest.media.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A DTO that is used on the REST API to provide infos about {@link AudioSource} to UIs.
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public class MediaSourceDTO {
    public String id;
    public String path;
    public @Nullable String artUri;
    public @Nullable String label;

    public MediaSourceDTO(String id, String path, @Nullable String label) {
        this.id = id;
        this.path = path;
        this.label = label;
    }

    public void setArtUri(String artUri) {
        this.artUri = artUri;
    }
}
