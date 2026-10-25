/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterGroup;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.core.xml.ConfigDescriptionConverter;
import org.openhab.core.config.core.xml.ConfigDescriptionParameterConverter;
import org.openhab.core.config.core.xml.ConfigDescriptionParameterGroupConverter;
import org.openhab.core.config.core.xml.FilterCriteriaConverter;
import org.openhab.core.config.core.xml.util.NodeAttributes;
import org.openhab.core.config.core.xml.util.NodeAttributesConverter;
import org.openhab.core.config.core.xml.util.NodeList;
import org.openhab.core.config.core.xml.util.NodeListConverter;
import org.openhab.core.config.core.xml.util.NodeValue;
import org.openhab.core.config.core.xml.util.NodeValueConverter;
import org.openhab.core.config.core.xml.util.XmlDocumentReader;
import org.openhab.core.config.core.xml.util.XmlMapping;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.EventDescription;
import org.openhab.core.types.StateDescription;

/**
 * The {@link ThingDescriptionReader} reads XML documents, which contain the {@code thing-description} XML tag, and
 * converts them to {@link ThingDescriptionList} objects consisting of {@link ThingTypeXmlResult},
 * {@link BridgeTypeXmlResult} and {@link ChannelTypeXmlResult} objects.
 * <p>
 * This reader uses {@code StAX} to parse and convert the XML document.
 *
 * @author Michael Grammling - Initial contribution
 * @author Alex Tugarev - Extended by options and filter criteria
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Chris Jackson - Added parameter groups and channel properties
 * @author Moritz Kammerer - Added triggers
 * @author Andrew Fiddian-Green - Added equipment property
 */
@NonNullByDefault
public class ThingDescriptionReader extends XmlDocumentReader<List<?>> {

    @Override
    public void registerConverters(XmlMapping mapping) {
        mapping.registerConverter(new NodeAttributesConverter());
        mapping.registerConverter(new NodeValueConverter());
        mapping.registerConverter(new NodeListConverter());
        mapping.registerConverter(new ThingDescriptionConverter());
        mapping.registerConverter(new ThingTypeConverter());
        mapping.registerConverter(new BridgeTypeConverter());
        mapping.registerConverter(new ChannelConverter());
        mapping.registerConverter(new ChannelTypeConverter());
        mapping.registerConverter(new ChannelGroupTypeConverter());
        mapping.registerConverter(new StateDescriptionConverter());
        mapping.registerConverter(new CommandDescriptionConverter());
        mapping.registerConverter(new EventDescriptionConverter());
        mapping.registerConverter(new ConfigDescriptionConverter());
        mapping.registerConverter(new ConfigDescriptionParameterConverter());
        mapping.registerConverter(new ConfigDescriptionParameterGroupConverter());
        mapping.registerConverter(new FilterCriteriaConverter());
    }

    @Override
    public void registerAliases(XmlMapping mapping) {
        mapping.alias("thing-descriptions", ThingDescriptionList.class);
        mapping.alias("thing-type", ThingTypeXmlResult.class);
        mapping.alias("bridge-type", BridgeTypeXmlResult.class);
        mapping.alias("channel-type", ChannelTypeXmlResult.class);
        mapping.alias("channel-group-type", ChannelGroupTypeXmlResult.class);
        mapping.alias("supported-bridge-type-refs", NodeList.class);
        mapping.alias("bridge-type-ref", NodeAttributes.class);
        mapping.alias("item-type", NodeValue.class);
        mapping.alias("dimension", NodeValue.class);
        mapping.alias("kind", NodeValue.class);
        mapping.alias("label", NodeValue.class);
        mapping.alias("description", NodeValue.class);
        mapping.alias("channels", NodeList.class);
        mapping.alias("channel", ChannelXmlResult.class);
        mapping.alias("channel-groups", NodeList.class);
        mapping.alias("channel-group", ChannelXmlResult.class);
        mapping.alias("category", NodeValue.class);
        mapping.alias("tags", NodeList.class);
        mapping.alias("tag", NodeValue.class);
        mapping.alias("state", StateDescription.class);
        mapping.alias("command", CommandDescription.class);
        mapping.alias("event", EventDescription.class);
        mapping.alias("options", NodeList.class);
        mapping.alias("option", NodeValue.class);
        mapping.alias("config-descriptions", NodeList.class);
        mapping.alias("config-description", ConfigDescription.class);
        mapping.alias("config-description-ref", NodeAttributes.class);
        mapping.alias("parameter", ConfigDescriptionParameter.class);
        mapping.alias("parameter-group", ConfigDescriptionParameterGroup.class);
        mapping.alias("filter", List.class);
        mapping.alias("criteria", FilterCriteria.class);
        mapping.alias("properties", NodeList.class);
        mapping.alias("property", NodeValue.class);
        mapping.alias("representation-property", NodeValue.class);
        mapping.alias("command-options", NodeList.class);
        mapping.alias("autoUpdatePolicy", NodeValue.class);
        mapping.alias("semantic-equipment-tag", NodeValue.class);
    }
}
