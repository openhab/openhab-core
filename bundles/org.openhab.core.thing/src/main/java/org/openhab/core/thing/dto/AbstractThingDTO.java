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
package org.openhab.core.thing.dto;

import java.util.Map;

/**
 * This is a data transfer object that is used to serialize things.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Stefan Bußweiler - Added new thing status handling
 * @author Simon Kaufmann - Added label
 * @author Wouter Born - Let (Enriched)ThingDTO extend AbstractThingDTO so both can define their own "channels" type
 */
public abstract class AbstractThingDTO {

    public String label;
    public String bridgeUID;
    public Map<String, Object> configuration;
    public Map<String, String> properties;
    public String UID;
    public String thingTypeUID;
    public String location;

    public AbstractThingDTO() {
    }

    protected AbstractThingDTO(String thingTypeUID, String UID, String label, String bridgeUID,
            Map<String, Object> configuration, Map<String, String> properties, String location) {
        this.thingTypeUID = thingTypeUID;
        this.UID = UID;
        this.label = label;
        this.bridgeUID = bridgeUID;
        this.configuration = configuration;
        this.properties = properties;
        this.location = location;
    }
}
