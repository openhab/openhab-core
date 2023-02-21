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
package org.openhab.core.thing.xml.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.xml.util.NodeValue;
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
@NonNullByDefault
public class ChannelXmlResult {

    private final String id;
    private final String typeId;
    private final @Nullable String label;
    private final @Nullable String description;
    private final @Nullable List<NodeValue> properties;
    private final @Nullable AutoUpdatePolicy autoUpdatePolicy;

    /**
     * Constructs a new {@link ChannelXmlResult}
     *
     * @param id the channel id
     * @param typeId the channel type id
     * @param label the channel label
     * @param description the channel description
     * @param properties a {@link List} of channel properties
     */
    public ChannelXmlResult(String id, String typeId, @Nullable String label, @Nullable String description,
            @Nullable List<NodeValue> properties, @Nullable AutoUpdatePolicy autoUpdatePolicy) {
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
        return id;
    }

    /**
     * Retrieves the type ID for this channel
     *
     * @return type ID
     */
    public String getTypeId() {
        return typeId;
    }

    /**
     * Retrieves the properties for this channel
     *
     * @return properties list
     */
    public List<NodeValue> getProperties() {
        return Objects.requireNonNullElse(properties, List.of());
    }

    /**
     * Get the label for this channel
     *
     * @return the channel label
     */
    public @Nullable String getLabel() {
        return label;
    }

    /**
     * Get the description for this channel
     *
     * @return the channel description
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * Get the auto update policy for this channel.
     *
     * @return the auto update policy
     */
    public @Nullable AutoUpdatePolicy getAutoUpdatePolicy() {
        return autoUpdatePolicy;
    }

    @Override
    public String toString() {
        return "ChannelXmlResult [id=" + id + ", typeId=" + typeId + ", properties=" + properties + "]";
    }

    protected ChannelDefinition toChannelDefinition(String bindingId) throws ConversionException {
        String typeUID = getTypeUID(bindingId, typeId);

        // Convert the channel properties into a map
        Map<String, String> propertiesMap = new HashMap<>();
        for (NodeValue property : getProperties()) {
            Map<String, String> attributes = property.getAttributes();
            if (attributes != null) {
                String name = attributes.get("name");
                String value = (String) property.getValue();
                if (name != null && value != null) {
                    propertiesMap.put(name, value);
                }
            }
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
