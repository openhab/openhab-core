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
package org.openhab.core.thing.link.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This is an abstract class for link data transfer object that is used to serialize links.
 *
 * @author Dennis Nobel - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractLinkDTO {

    public String itemName;

    /**
     * Default constructor for deserialization e.g. by Gson.
     */
    protected AbstractLinkDTO() {
        this("");
    }

    protected AbstractLinkDTO(String itemName) {
        this.itemName = itemName;
    }
}
