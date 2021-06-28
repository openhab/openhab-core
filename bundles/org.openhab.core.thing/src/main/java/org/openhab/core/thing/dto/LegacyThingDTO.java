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
package org.openhab.core.thing.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

/**
 * Before OpenHAB 3.1, objects of type ThingImpl were persisted in the JSON storage.
 * This class is a copy of ThingImpl to load old entries from the storage for conversion.
 *
 * @author Simon Lamon - Initial contribution
 */
@Deprecated
@NonNullByDefault
public class LegacyThingDTO {

    private @Nullable String label;

    private @Nullable ThingUID bridgeUID;

    private List<LegacyChannelDTO> channels = new ArrayList<>();

    private Configuration configuration = new Configuration();

    private Map<String, String> properties = new HashMap<>();

    private @NonNullByDefault({}) ThingUID uid;

    private @NonNullByDefault({}) ThingTypeUID thingTypeUID;

    private @Nullable String location;

    public LegacyThingDTO() {
    }

    public @Nullable String getLabel() {
        return label;
    }

    public @Nullable ThingUID getBridgeUID() {
        return bridgeUID;
    }

    public List<LegacyChannelDTO> getChannels() {
        return channels;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ThingUID getUID() {
        return uid;
    }

    public ThingTypeUID getThingTypeUID() {
        return thingTypeUID;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public @Nullable String getLocation() {
        return location;
    }
}
