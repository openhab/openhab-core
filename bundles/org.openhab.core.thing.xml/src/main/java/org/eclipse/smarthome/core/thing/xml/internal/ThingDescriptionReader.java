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
package org.eclipse.smarthome.core.thing.xml.internal;

import java.util.List;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterGroup;
import org.eclipse.smarthome.config.core.FilterCriteria;
import org.eclipse.smarthome.config.xml.ConfigDescriptionConverter;
import org.eclipse.smarthome.config.xml.ConfigDescriptionParameterConverter;
import org.eclipse.smarthome.config.xml.ConfigDescriptionParameterGroupConverter;
import org.eclipse.smarthome.config.xml.FilterCriteriaConverter;
import org.eclipse.smarthome.config.xml.util.NodeAttributes;
import org.eclipse.smarthome.config.xml.util.NodeAttributesConverter;
import org.eclipse.smarthome.config.xml.util.NodeList;
import org.eclipse.smarthome.config.xml.util.NodeListConverter;
import org.eclipse.smarthome.config.xml.util.NodeValue;
import org.eclipse.smarthome.config.xml.util.NodeValueConverter;
import org.eclipse.smarthome.config.xml.util.XmlDocumentReader;
import org.eclipse.smarthome.core.types.EventDescription;
import org.eclipse.smarthome.core.types.StateDescription;

import com.thoughtworks.xstream.XStream;

/**
 * The {@link ThingDescriptionReader} reads XML documents, which contain the {@code thing-description} XML tag, and
 * converts them to {@link ThingDescriptionList} objects consisting of {@link ThingTypeXmlResult},
 * {@link BridgeTypeXmlResult} and {@link ChannelTypeXmlResult} objects.
 * <p>
 * This reader uses {@code XStream} and {@code StAX} to parse and convert the XML document.
 *
 * @author Michael Grammling - Initial Contribution
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
        xstream.alias("autoUpdatePolicy", NodeValue.class);
    }

}
