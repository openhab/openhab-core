/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.thing.internal;

import org.openhab.core.thing.dto.ThingDTO;

/**
 * The {@link ThingStorageEntity} is an entity for Thing storage
 *
 * @author Jan N. Klug - Initial contribution
 */
public class ThingStorageEntity extends ThingDTO {
    public boolean isBridge = false;

    ThingStorageEntity() {
        // do not remove, needed by GSON for deserialization
    }

    public ThingStorageEntity(ThingDTO thingDTO, boolean isBridge) {
        super(thingDTO.thingTypeUID, thingDTO.UID, thingDTO.label, thingDTO.bridgeUID, thingDTO.channels,
                thingDTO.configuration, thingDTO.properties, thingDTO.location);
        this.isBridge = isBridge;
    }
}
