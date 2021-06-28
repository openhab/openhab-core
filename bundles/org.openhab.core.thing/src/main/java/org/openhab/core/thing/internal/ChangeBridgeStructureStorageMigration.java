/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.storage.StorageMigration;
import org.openhab.core.thing.dto.LegacyThingDTO;
import org.openhab.core.thing.dto.LegacyThingDTOMapper;
import org.openhab.core.thing.dto.ThingDTO;

/**
 * Before OpenHAB 3.1, objects of type ThingImpl were persisted in the JSON storage.
 * This migration converts any old BridgeImpl in the storage to the new ThingDTO structure.
 *
 * @author Simon Lamon - Initial contribution
 */
@Deprecated
@NonNullByDefault
public class ChangeBridgeStructureStorageMigration extends StorageMigration {

    public ChangeBridgeStructureStorageMigration() {
        super(BridgeImpl.class.getTypeName(), LegacyThingDTO.class, ThingDTO.class.getTypeName(), ThingDTO.class);
    }

    @Override
    public Object migrate(Object in) {
        LegacyThingDTO legacyThing = (LegacyThingDTO) in;
        return LegacyThingDTOMapper.map(legacyThing, true);
    }
}
