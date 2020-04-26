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
package org.openhab.core.config.xml.util;

import java.util.Map;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link NodeValueConverter} is a concrete implementation of the {@code XStream} {@link Converter} interface used
 * to convert the value of an XML tag within an XML
 * document into a {@link NodeValue} object.
 *
 * @author Michael Grammling - Initial contribution
 */
public class NodeValueConverter extends GenericUnmarshaller<NodeValue> {

    public NodeValueConverter() {
        super(NodeValue.class);
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Map<String, String> attributes = ConverterAttributeMapValidator.readValidatedAttributes(reader, null);

        return new NodeValue(reader.getNodeName(), attributes, reader.getValue());
    }
}
