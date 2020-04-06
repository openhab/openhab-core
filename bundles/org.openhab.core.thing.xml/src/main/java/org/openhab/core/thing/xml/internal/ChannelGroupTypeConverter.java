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
import java.util.Map;

import org.openhab.core.config.xml.util.ConverterAttributeMapValidator;
import org.openhab.core.config.xml.util.NodeIterator;
import org.openhab.core.thing.type.ChannelGroupTypeUID;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link ChannelGroupTypeConverter} is a concrete implementation of the {@code XStream} {@link Converter} interface
 * used to convert channel group type information within an
 * XML document into a {@link ChannelGroupTypeXmlResult} object.
 * <p>
 * This converter converts {@code channel-group-type} XML tags. It uses the {@link AbstractDescriptionTypeConverter}
 * which offers base functionality for each type definition.
 *
 * @author Michael Grammling - Initial contribution
 * @author Chris Jackson - Modified to support channel properties
 * @author Christoph Weitkamp - Removed "advanced" attribute
 */
public class ChannelGroupTypeConverter extends AbstractDescriptionTypeConverter<ChannelGroupTypeXmlResult> {

    public ChannelGroupTypeConverter() {
        super(ChannelGroupTypeXmlResult.class, "channel-group-type");

        super.attributeMapValidator = new ConverterAttributeMapValidator(new String[][] { { "id", "true" } });
    }

    @SuppressWarnings("unchecked")
    protected List<ChannelXmlResult> readChannelTypeDefinitions(NodeIterator nodeIterator) throws ConversionException {
        return (List<ChannelXmlResult>) nodeIterator.nextList("channels", false);
    }

    @Override
    protected ChannelGroupTypeXmlResult unmarshalType(HierarchicalStreamReader reader, UnmarshallingContext context,
            Map<String, String> attributes, NodeIterator nodeIterator) throws ConversionException {
        ChannelGroupTypeUID channelGroupTypeUID = new ChannelGroupTypeUID(super.getUID(attributes, context));

        String label = super.readLabel(nodeIterator);
        String description = super.readDescription(nodeIterator);
        String category = readCategory(nodeIterator);
        List<ChannelXmlResult> channelTypeDefinitions = readChannelTypeDefinitions(nodeIterator);

        ChannelGroupTypeXmlResult groupChannelType = new ChannelGroupTypeXmlResult(channelGroupTypeUID, label,
                description, category, channelTypeDefinitions);

        return groupChannelType;
    }

    private String readCategory(NodeIterator nodeIterator) {
        Object category = nodeIterator.nextValue("category", false);
        return category == null ? null : category.toString();
    }

}
