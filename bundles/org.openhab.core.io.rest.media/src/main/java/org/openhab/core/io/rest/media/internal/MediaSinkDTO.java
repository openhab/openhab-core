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

/**
 * A DTO that is used on the REST API to provide infos about {@link AudioSource} to UIs.
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public class MediaSinkDTO {
    private String id;
    private String name;
    private String type;
    private String binding;
    private String playerItemName;

    public MediaSinkDTO(String id, String name, String type, String binding) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.binding = binding;
        this.playerItemName = "";
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public String getBinding() {
        return this.binding;
    }

    public String getPlayerItemName() {
        return this.playerItemName;
    }

    public void setPlayerItemName(String playerItemName) {
        this.playerItemName = playerItemName;
    }
}
