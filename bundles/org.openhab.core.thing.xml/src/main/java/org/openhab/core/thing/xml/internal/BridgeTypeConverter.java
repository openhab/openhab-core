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

import java.util.Map;

import org.openhab.core.config.xml.util.NodeIterator;
import org.openhab.core.thing.ThingTypeUID;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link BridgeTypeConverter} is a concrete implementation of the {@code XStream} {@link Converter} interface used
 * to convert bridge type information within an XML document
 * into a {@link BridgeTypeXmlResult} object.
 * <p>
 * This converter converts {@code bridge-type} XML tags. It uses the {@link ThingTypeConverter} since both contain the
 * same content.
 *
 * @author Michael Grammling - Initial contribution
 * @author Thomas Höfer - Added thing and thing type properties
 * @author Andre Fuechsel - Added representationProperty
 */
public class BridgeTypeConverter extends ThingTypeConverter {

    public BridgeTypeConverter() {
        super(BridgeTypeXmlResult.class, "thing-type");
    }

    @Override
    protected BridgeTypeXmlResult unmarshalType(HierarchicalStreamReader reader, UnmarshallingContext context,
            Map<String, String> attributes, NodeIterator nodeIterator) throws ConversionException {
        BridgeTypeXmlResult bridgeTypeXmlResult = new BridgeTypeXmlResult(new ThingTypeUID(getUID(attributes, context)),
                readSupportedBridgeTypeUIDs(nodeIterator, context), readLabel(nodeIterator),
                readDescription(nodeIterator), readCategory(nodeIterator), getListed(attributes),
                getExtensibleChannelTypeIds(attributes), getChannelTypeReferenceObjects(nodeIterator),
                getProperties(nodeIterator), getRepresentationProperty(nodeIterator),
                getConfigDescriptionObjects(nodeIterator));

        return bridgeTypeXmlResult;
    }
}
