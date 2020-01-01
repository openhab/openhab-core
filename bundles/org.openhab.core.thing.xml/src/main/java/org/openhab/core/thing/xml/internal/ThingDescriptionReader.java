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

import java.util.List;

import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterGroup;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.xml.ConfigDescriptionConverter;
import org.openhab.core.config.xml.ConfigDescriptionParameterConverter;
import org.openhab.core.config.xml.ConfigDescriptionParameterGroupConverter;
import org.openhab.core.config.xml.FilterCriteriaConverter;
import org.openhab.core.config.xml.util.NodeAttributes;
import org.openhab.core.config.xml.util.NodeAttributesConverter;
import org.openhab.core.config.xml.util.NodeList;
import org.openhab.core.config.xml.util.NodeListConverter;
import org.openhab.core.config.xml.util.NodeValue;
import org.openhab.core.config.xml.util.NodeValueConverter;
import org.openhab.core.config.xml.util.XmlDocumentReader;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.EventDescription;
import org.openhab.core.types.StateDescription;

import com.thoughtworks.xstream.XStream;

/**
 * The {@link ThingDescriptionReader} reads XML documents, which contain the {@code thing-description} XML tag, and
 * converts them to {@link ThingDescriptionList} objects consisting of {@link ThingTypeXmlResult},
 * {@link BridgeTypeXmlResult} and {@link ChannelTypeXmlResult} objects.
 * <p>
 * This reader uses {@code XStream} and {@code StAX} to parse and convert the XML document.
 *
 * @author Michael Grammling - Initial contribution
 * @author Alex Tugarev - Extended by options and filter criteria
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Chris Jackson - Added parameter groups and channel properties
 * @author Moritz Kammerer - Added triggers
 */
public class ThingDescriptionReader extends XmlDocumentReader<List<?>> {

    /**
     * The default constructor of this class.
     */
    public ThingDescriptionReader() {
        super.setClassLoader(ThingDescriptionReader.class.getClassLoader());
    }

    @Override
    public void registerConverters(XStream xstream) {
        xstream.registerConverter(new NodeAttributesConverter());
        xstream.registerConverter(new NodeValueConverter());
        xstream.registerConverter(new NodeListConverter());
        xstream.registerConverter(new ThingDescriptionConverter());
        xstream.registerConverter(new ThingTypeConverter());
        xstream.registerConverter(new BridgeTypeConverter());
        xstream.registerConverter(new ChannelConverter());
        xstream.registerConverter(new ChannelTypeConverter());
        xstream.registerConverter(new ChannelGroupTypeConverter());
        xstream.registerConverter(new StateDescriptionConverter());
        xstream.registerConverter(new CommandDescriptionConverter());
        xstream.registerConverter(new EventDescriptionConverter());
        xstream.registerConverter(new ConfigDescriptionConverter());
        xstream.registerConverter(new ConfigDescriptionParameterConverter());
        xstream.registerConverter(new ConfigDescriptionParameterGroupConverter());
        xstream.registerConverter(new FilterCriteriaConverter());
    }

    @Override
    public void registerAliases(XStream xstream) {
        xstream.alias("thing-descriptions", ThingDescriptionList.class);
        xstream.alias("thing-type", ThingTypeXmlResult.class);
        xstream.alias("bridge-type", BridgeTypeXmlResult.class);
        xstream.alias("channel-type", ChannelTypeXmlResult.class);
        xstream.alias("channel-group-type", ChannelGroupTypeXmlResult.class);
        xstream.alias("supported-bridge-type-refs", NodeList.class);
        xstream.alias("bridge-type-ref", NodeAttributes.class);
        xstream.alias("item-type", NodeValue.class);
        xstream.alias("dimension", NodeValue.class);
        xstream.alias("kind", NodeValue.class);
        xstream.alias("label", NodeValue.class);
        xstream.alias("description", NodeValue.class);
        xstream.alias("channels", NodeList.class);
        xstream.alias("channel", ChannelXmlResult.class);
        xstream.alias("channel-groups", NodeList.class);
        xstream.alias("channel-group", ChannelXmlResult.class);
        xstream.alias("category", NodeValue.class);
        xstream.alias("tags", NodeList.class);
        xstream.alias("tag", NodeValue.class);
        xstream.alias("state", StateDescription.class);
        xstream.alias("command", CommandDescription.class);
        xstream.alias("event", EventDescription.class);
        xstream.alias("options", NodeList.class);
        xstream.alias("option", NodeValue.class);
        xstream.alias("config-descriptions", NodeList.class);
        xstream.alias("config-description", ConfigDescription.class);
        xstream.alias("config-description-ref", NodeAttributes.class);
        xstream.alias("parameter", ConfigDescriptionParameter.class);
        xstream.alias("parameter-group", ConfigDescriptionParameterGroup.class);
        xstream.alias("filter", List.class);
        xstream.alias("criteria", FilterCriteria.class);
        xstream.alias("properties", NodeList.class);
        xstream.alias("property", NodeValue.class);
        xstream.alias("representation-property", NodeValue.class);
        xstream.alias("command-options", NodeList.class);
        xstream.alias("autoUpdatePolicy", NodeValue.class);
    }

}
