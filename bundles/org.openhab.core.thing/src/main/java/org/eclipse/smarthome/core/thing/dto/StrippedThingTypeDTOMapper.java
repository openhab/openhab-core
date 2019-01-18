/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.thing.dto;

import java.util.Locale;

import org.eclipse.smarthome.core.thing.type.BridgeType;
import org.eclipse.smarthome.core.thing.type.ThingType;

/**
 * The {@link StrippedThingTypeDTOMapper} is an utility class to map things into stripped thing type data transfer
 * objects (DTOs).
 *
 * @author Miki Jankov - Initial contribution
 */
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
