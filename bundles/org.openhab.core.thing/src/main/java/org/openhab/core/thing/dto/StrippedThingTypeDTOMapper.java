/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.thing.dto;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.type.BridgeType;
import org.openhab.core.thing.type.ThingType;

/**
 * The {@link StrippedThingTypeDTOMapper} is an utility class to map things into stripped thing type data transfer
 * objects (DTOs).
 *
 * @author Miki Jankov - Initial contribution
 */
@NonNullByDefault
public class StrippedThingTypeDTOMapper {

    /**
     * Maps thing type into stripped thing type data transfer object.
     *
     * @param thingType the thing type to be mapped
     * @return the stripped thing type DTO object
     */
    public static StrippedThingTypeDTO map(ThingType thingType, Locale locale) {
        return new StrippedThingTypeDTO(thingType.getUID().toString(), thingType.getLabel(), thingType.getDescription(),
                thingType.getCategory(), thingType.isListed(), thingType.getSupportedBridgeTypeUIDs(),
                thingType instanceof BridgeType);
    }
}
