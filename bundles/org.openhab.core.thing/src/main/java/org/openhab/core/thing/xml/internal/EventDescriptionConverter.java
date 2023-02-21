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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.xml.util.GenericUnmarshaller;
import org.openhab.core.config.core.xml.util.NodeIterator;
import org.openhab.core.config.core.xml.util.NodeList;
import org.openhab.core.config.core.xml.util.NodeValue;
import org.openhab.core.types.EventDescription;
import org.openhab.core.types.EventOption;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link EventDescriptionConverter} is a concrete implementation of the {@code XStream}
 * {@link com.thoughtworks.xstream.converters.Converter} interface
 * used to convert a event description within an XML document into an {@link EventDescription} object.
 * <p>
 * This converter converts {@code state} XML tags.
 *
 * @author Moritz Kammerer - Initial contribution
 */
@NonNullByDefault
public class EventDescriptionConverter extends GenericUnmarshaller<EventDescription> {

    public EventDescriptionConverter() {
        super(EventDescription.class);
    }

    private List<EventOption> toListOfEventOptions(NodeList nodeList) throws ConversionException {
        if ("options".equals(nodeList.getNodeName())) {
            List<EventOption> eventOptions = new ArrayList<>();

            for (Object nodeObject : nodeList.getList()) {
                eventOptions.add(toEventOption((NodeValue) nodeObject));
            }

            return eventOptions;
        }

        throw new ConversionException("Unknown type '" + nodeList.getNodeName() + "'!");
    }

    private EventOption toEventOption(NodeValue nodeValue) throws ConversionException {
        if ("option".equals(nodeValue.getNodeName())) {
            String value;
            String label;

            Map<String, String> attributes = nodeValue.getAttributes();
            if ((attributes != null) && (attributes.containsKey("value"))) {
                value = attributes.get("value");
            } else {
                throw new ConversionException("The node 'option' requires the attribute 'value'!");
            }

            label = (String) nodeValue.getValue();

            return new EventOption(value, label);
        }

        throw new ConversionException("Unknown type in the list of 'options'!");
    }

    @Override
    public final @Nullable Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        List<EventOption> eventOptions = null;

        NodeList nodes = (NodeList) context.convertAnother(context, NodeList.class);
        NodeIterator nodeIterator = new NodeIterator(nodes.getList());

        NodeList optionNodes = (NodeList) nodeIterator.next();
        if (optionNodes != null) {
            eventOptions = toListOfEventOptions(optionNodes);
        }

        nodeIterator.assertEndOfType();

        return new EventDescription(eventOptions);
    }
}
