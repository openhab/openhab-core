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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.type.ChannelKind;

/**
 * This is a data transfer object that is used to serialize channels.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Chris Jackson - Added properties and configuration
 * @author Kai Kreuzer - Added default tags
 */
public class ChannelDTO {

    public String uid;
    public String id;
    public String channelTypeUID;
    public String itemType;
    public String kind;
    public String label;
    public String description;
    public Set<String> defaultTags;
    public Map<String, String> properties;
    public Map<String, Object> configuration;

    public ChannelDTO() {
    }

    public ChannelDTO(ChannelUID uid, String channelTypeUID, String itemType, ChannelKind kind, String label,
            String description, Map<String, String> properties, Configuration configuration, Set<String> defaultTags) {
        this.uid = uid.toString();
        this.id = uid.getId();
        this.channelTypeUID = channelTypeUID;
        this.itemType = itemType;
        this.label = label;
        this.description = description;
        this.properties = properties;
        this.configuration = toMap(configuration);
        this.defaultTags = new HashSet<>(defaultTags);
        this.kind = kind.toString();
    }

    private Map<String, Object> toMap(Configuration configuration) {
        if (configuration == null) {
            return null;
        }

        Map<String, Object> configurationMap = new HashMap<>(configuration.keySet().size());
        for (String key : configuration.keySet()) {
            configurationMap.put(key, configuration.get(key));
        }
        return configurationMap;
    }
}
