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

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openhab.core.config.xml.util.ConverterAttributeMapValidator;
import org.openhab.core.config.xml.util.NodeIterator;
import org.openhab.core.config.xml.util.NodeList;
import org.openhab.core.config.xml.util.NodeValue;
import org.openhab.core.thing.ThingTypeUID;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link ThingTypeConverter} is a concrete implementation of the {@code XStream} {@link Converter} interface used
 * to convert {@code Thing} type information within
 * an XML document into a {@link ThingTypeXmlResult} object.
 * <p>
 * This converter converts {@code thing-type} XML tags. It uses the {@link AbstractDescriptionTypeConverter} which
 * offers base functionality for each type definition.
 *
 * @author Michael Grammling - Initial contribution
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Chris Jackson - Added channel properties
 * @author Andre Fuechsel - Added representationProperty
 */
public class ThingTypeConverter extends AbstractDescriptionTypeConverter<ThingTypeXmlResult> {

    public ThingTypeConverter() {
        this(ThingTypeXmlResult.class, "thing-type");
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param clazz the class of the result object (must not be null)
     * @param type the name of the type (e.g. "bridge-type")
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected ThingTypeConverter(Class clazz, String type) {
        super(clazz, type);
        this.attributeMapValidator = new ConverterAttributeMapValidator(
                new String[][] { { "id", "true" }, { "listed", "false" }, { "extensible", "false" } });
    }

    protected List<String> readSupportedBridgeTypeUIDs(NodeIterator nodeIterator, UnmarshallingContext context) {
        Object nextNode = nodeIterator.next("supported-bridge-type-refs", false);

        if (nextNode != null) {
            String bindingID = (String) context.get("thing-descriptions.bindingId");

            String uidFormat = String.format("%s:%s", bindingID, "%s");

            return ((NodeList) nextNode).getAttributes("bridge-type-ref", "id", uidFormat);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    protected List<ChannelXmlResult>[] getChannelTypeReferenceObjects(NodeIterator nodeIterator)
            throws ConversionException {
        List<ChannelXmlResult> channelTypeReferences = null;
        List<ChannelXmlResult> channelGroupTypeReferences = null;

        channelTypeReferences = (List<ChannelXmlResult>) nodeIterator.nextList("channels", false);
        if (channelTypeReferences == null) {
            channelGroupTypeReferences = (List<ChannelXmlResult>) nodeIterator.nextList("channel-groups", false);
        }

        return new List[] { channelTypeReferences, channelGroupTypeReferences };
    }

    @SuppressWarnings("unchecked")
    protected List<NodeValue> getProperties(NodeIterator nodeIterator) {
        return (List<NodeValue>) nodeIterator.nextList("properties", false);
    }

    @Override
    protected ThingTypeXmlResult unmarshalType(HierarchicalStreamReader reader, UnmarshallingContext context,
            Map<String, String> attributes, NodeIterator nodeIterator) throws ConversionException {
        ThingTypeXmlResult thingTypeXmlResult = new ThingTypeXmlResult(
                new ThingTypeUID(super.getUID(attributes, context)), readSupportedBridgeTypeUIDs(nodeIterator, context),
                super.readLabel(nodeIterator), super.readDescription(nodeIterator), readCategory(nodeIterator),
                getListed(attributes), getExtensibleChannelTypeIds(attributes),
                getChannelTypeReferenceObjects(nodeIterator), getProperties(nodeIterator),
                getRepresentationProperty(nodeIterator), super.getConfigDescriptionObjects(nodeIterator));

        return thingTypeXmlResult;
    }

    protected List<String> getExtensibleChannelTypeIds(Map<String, String> attributes) {
        String extensible = attributes.get("extensible");
        if (extensible == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(extensible.split(",")).map(String::trim).collect(toList());
    }

    protected String readCategory(NodeIterator nodeIterator) {
        Object category = nodeIterator.nextValue("category", false);
        if (category != null) {
            return category.toString();
        } else {
            return null;
        }
    }

    protected boolean getListed(Map<String, String> attributes) {
        String listedFlag = attributes.get("listed");
        if (listedFlag != null) {
            return Boolean.parseBoolean(listedFlag);
        }
        return true;
    }

    protected String getRepresentationProperty(NodeIterator nodeIterator) {
        return (String) nodeIterator.nextValue("representation-property", false);
    }

}
