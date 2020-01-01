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
package org.openhab.core.config.discovery.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

/**
 * The {@link DiscoveryResultDTOMapper} is an utility class to map discovery results into discovery result transfer
 * objects.
 *
 * @author Stefan Bussweiler - Initial contribution
 */
@NonNullByDefault
public class DiscoveryResultDTOMapper {

    /**
     * Maps discovery result into discovery result data transfer object.
     *
     * @param discoveryResult the discovery result
     * @return the discovery result data transfer object
     */
    public static DiscoveryResultDTO map(DiscoveryResult discoveryResult) {
        ThingUID thingUID = discoveryResult.getThingUID();
        ThingUID bridgeUID = discoveryResult.getBridgeUID();

        return new DiscoveryResultDTO(thingUID.toString(), bridgeUID != null ? bridgeUID.toString() : null,
                discoveryResult.getThingTypeUID().toString(), discoveryResult.getLabel(), discoveryResult.getFlag(),
                discoveryResult.getProperties(), discoveryResult.getRepresentationProperty());
    }

    /**
     * Maps discovery result data transfer object into discovery result.
     *
     * @param discoveryResultDTO the discovery result data transfer object
     * @return the discovery result
     */
    public static DiscoveryResult map(DiscoveryResultDTO discoveryResultDTO) {
        final ThingUID thingUID = new ThingUID(discoveryResultDTO.thingUID);

        final String dtoThingTypeUID = discoveryResultDTO.thingTypeUID;
        final ThingTypeUID thingTypeUID = dtoThingTypeUID != null ? new ThingTypeUID(dtoThingTypeUID) : null;

        final String dtoBridgeUID = discoveryResultDTO.bridgeUID;
        final ThingUID bridgeUID = dtoBridgeUID != null ? new ThingUID(dtoBridgeUID) : null;

        return DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID).withBridge(bridgeUID)
                .withLabel(discoveryResultDTO.label)
                .withRepresentationProperty(discoveryResultDTO.representationProperty)
                .withProperties(discoveryResultDTO.properties).build();
    }
}
