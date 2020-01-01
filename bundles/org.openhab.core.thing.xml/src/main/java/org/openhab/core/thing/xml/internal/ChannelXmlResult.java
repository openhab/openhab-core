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
package org.openhab.core.thing.xml.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openhab.core.config.xml.util.NodeValue;
import org.openhab.core.thing.type.AutoUpdatePolicy;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelDefinitionBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;

import com.thoughtworks.xstream.converters.ConversionException;

/**
 * The {@link ChannelXmlResult} is an intermediate XML conversion result object.
 *
 * @author Chris Jackson - Initial contribution
 */
public class ChannelXmlResult {

    private final String id;
    private final String typeId;
    String label;
    String description;
    List<NodeValue> properties;
    private final AutoUpdatePolicy autoUpdatePolicy;

    /**
     * Constructs a new {@link ChannelXmlResult}
     *
     * @param id the channel id
     * @param typeId the channel type id
     * @param label the channel label
     * @param description the channel description
     * @param properties a {@link List} of channel properties
     */
    public ChannelXmlResult(String id, String typeId, String label, String description, List<NodeValue> properties,
            AutoUpdatePolicy autoUpdatePolicy) {
        this.id = id;
        this.typeId = typeId;
        this.label = label;
        this.description = description;
        this.properties = properties;
        this.autoUpdatePolicy = autoUpdatePolicy;
    }

    /**
     * Retrieves the ID for this channel
     *
     * @return channel id
     */
    public String getId() {
        return this.id;
    }

    /**
     * Retrieves the type ID for this channel
     *
     * @return type ID
     */
    public String getTypeId() {
        return this.typeId;
    }

    /**
     * Retrieves the properties for this channel
     *
     * @return properties list (not null)
     */
    public List<NodeValue> getProperties() {
        if (this.properties == null) {
            return new ArrayList<>(0);
        }
        return this.properties;
    }

    /**
     * Get the label for this channel
     *
     * @return the channel label. Can be null
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get the description for this channel
     *
     * @return the channel description. Can be null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the auto update policy for this channel.
     *
     * @return the auto update policy
     */
    public AutoUpdatePolicy getAutoUpdatePolicy() {
        return autoUpdatePolicy;
    }

    @Override
    public String toString() {
        return "ChannelXmlResult [id=" + id + ", typeId=" + typeId + ", properties=" + properties + "]";
    }

    protected ChannelDefinition toChannelDefinition(String bindingId) throws ConversionException {
        String id = getId();
        String typeId = getTypeId();

        String typeUID = getTypeUID(bindingId, typeId);

        // Convert the channel properties into a map
        Map<String, String> propertiesMap = new HashMap<>();
        for (NodeValue property : getProperties()) {
            propertiesMap.put(property.getAttributes().get("name"), (String) property.getValue());
        }

        return new ChannelDefinitionBuilder(id, new ChannelTypeUID(typeUID)).withProperties(propertiesMap)
                .withLabel(getLabel()).withDescription(getDescription()).withAutoUpdatePolicy(getAutoUpdatePolicy())
                .build();
    }

    private String getTypeUID(String bindingId, String typeId) {
        if (typeId.startsWith(XmlHelper.SYSTEM_NAMESPACE_PREFIX)) {
            return XmlHelper.getSystemUID(typeId);
        } else {
            return String.format("%s:%s", bindingId, typeId);
        }
    }

}
