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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.smarthome.config.xml.util.ConverterAttributeMapValidator;
import org.eclipse.smarthome.config.xml.util.GenericUnmarshaller;
import org.eclipse.smarthome.config.xml.util.NodeIterator;
import org.eclipse.smarthome.config.xml.util.NodeList;
import org.eclipse.smarthome.config.xml.util.NodeValue;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateOption;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link StateDescriptionConverter} is a concrete implementation of the {@code XStream} {@link Converter} interface
 * used to convert a state description within an XML document
 * into a {@link StateDescription} object.
 * <p>
 * This converter converts {@code state} XML tags.
 *
 * @author Michael Grammling - Initial Contribution
 */
public class StateDescriptionConverter extends GenericUnmarshaller<StateDescription> {

    protected ConverterAttributeMapValidator attributeMapValidator;

    public StateDescriptionConverter() {
        super(StateDescription.class);

        this.attributeMapValidator = new ConverterAttributeMapValidator(new String[][] { { "min", "false" },
                { "max", "false" }, { "step", "false" }, { "pattern", "false" }, { "readOnly", "false" } });
    }

    private BigDecimal toBigDecimal(Map<String, String> attributes, String attribute, BigDecimal defaultValue)
            throws ConversionException {
        String attrValueText = attributes.get(attribute);

        if (attrValueText != null) {
            try {
                return new BigDecimal(attrValueText);
            } catch (NumberFormatException nfe) {
                throw new ConversionException(
                        "The attribute '" + attribute + "' has not a valid decimal number format!", nfe);
            }
        }

        return defaultValue;
    }

    private boolean toBoolean(Map<String, String> attributes, String attribute, Boolean defaultValue) {
        String attrValueText = attributes.get(attribute);

        if (attrValueText != null) {
            return Boolean.valueOf(attrValueText);
        }

        return defaultValue;
    }

    private List<StateOption> toListOfChannelState(NodeList nodeList) throws ConversionException {
        if ("options".equals(nodeList.getNodeName())) {
            List<StateOption> stateOptions = new ArrayList<>();

            for (Object nodeObject : nodeList.getList()) {
                stateOptions.add(toChannelStateOption((NodeValue) nodeObject));
            }

            return stateOptions;
        }

        throw new ConversionException("Unknown type '" + nodeList.getNodeName() + "'!");
    }

    private StateOption toChannelStateOption(NodeValue nodeValue) throws ConversionException {
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

            return new StateOption(value, label);
        }

        throw new ConversionException("Unknown type in the list of 'options'!");
    }

    @Override
    public final Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Map<String, String> attributes = this.attributeMapValidator.readValidatedAttributes(reader);

        BigDecimal minimum = toBigDecimal(attributes, "min", null);
        BigDecimal maximum = toBigDecimal(attributes, "max", null);
        BigDecimal step = toBigDecimal(attributes, "step", null);
        String pattern = attributes.get("pattern");
        boolean readOnly = toBoolean(attributes, "readOnly", false);

        List<StateOption> channelOptions = null;

        NodeList nodes = (NodeList) context.convertAnother(context, NodeList.class);
        NodeIterator nodeIterator = new NodeIterator(nodes.getList());

        NodeList optionNodes = (NodeList) nodeIterator.next();
        if (optionNodes != null) {
            channelOptions = toListOfChannelState(optionNodes);
        }

        nodeIterator.assertEndOfType();

        StateDescription stateDescription = new StateDescription(minimum, maximum, step, pattern, readOnly,
                channelOptions);

        return stateDescription;
    }

}
