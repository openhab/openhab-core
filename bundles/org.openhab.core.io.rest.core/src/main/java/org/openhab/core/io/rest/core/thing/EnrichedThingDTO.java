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
package org.openhab.core.io.rest.core.thing;

import java.util.List;

import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.dto.ChannelDTO;
import org.openhab.core.thing.dto.ThingDTO;
import org.openhab.core.thing.firmware.dto.FirmwareStatusDTO;

/**
 * This is a data transfer object that is used to serialize things with dynamic data like the status.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - Removed links and items
 * @author Chris Jackson - Added 'editable' flag
 */
public class EnrichedThingDTO extends ThingDTO {

    public ThingStatusInfo statusInfo;
    public final FirmwareStatusDTO firmwareStatus;
    public boolean editable;

    /**
     * Creates an enriched thing data transfer object.
     *
     * @param thingDTO the base {@link ThingDTO}
     * @param channels the list of {@link EnrichedChannelDTO} for this thing
     * @param statusInfo {@link ThingStatusInfo} for this thing
     * @param firmwareStatus {@link FirmwareStatusDTO} for this thing
     * @param editable true if this thing can be edited
     */
    EnrichedThingDTO(ThingDTO thingDTO, List<ChannelDTO> channels, ThingStatusInfo statusInfo,
            FirmwareStatusDTO firmwareStatus, boolean editable) {
        super(thingDTO.thingTypeUID, thingDTO.UID, thingDTO.label, thingDTO.bridgeUID, channels, thingDTO.configuration,
                thingDTO.properties, thingDTO.location);
        this.statusInfo = statusInfo;
        this.firmwareStatus = firmwareStatus;
        this.editable = editable;
    }
}
