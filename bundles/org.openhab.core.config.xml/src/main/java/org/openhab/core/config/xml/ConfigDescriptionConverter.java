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
package org.openhab.core.config.xml;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterGroup;
import org.openhab.core.config.xml.util.ConverterAssertion;
import org.openhab.core.config.xml.util.ConverterAttributeMapValidator;
import org.openhab.core.config.xml.util.GenericUnmarshaller;
import org.openhab.core.config.xml.util.NodeIterator;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link ConfigDescriptionConverter} is a concrete implementation of the {@code XStream} {@link Converter}
 * interface used to convert config
 * description information within an XML document into a {@link ConfigDescription} object.
 * <p>
 * This converter converts {@code config-description} XML tags.
 *
 * @author Michael Grammling - Initial contribution
 * @author Chris Jackson - Added configuration groups
 */
public class ConfigDescriptionConverter extends GenericUnmarshaller<ConfigDescription> {

    private ConverterAttributeMapValidator attributeMapValidator;

    public ConfigDescriptionConverter() {
        super(ConfigDescription.class);

        this.attributeMapValidator = new ConverterAttributeMapValidator(new String[][] { { "uri", "false" } });
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        ConfigDescription configDescription = null;

        // read attributes
        Map<String, String> attributes = this.attributeMapValidator.readValidatedAttributes(reader);
        String uriText = attributes.get("uri");
        if (uriText == null) {
            // the URI could be overridden by a context field if it could be
            // automatically extracted
            uriText = (String) context.get("config-description.uri");
        }

        URI uri = null;
        if (uriText == null) {
            throw new ConversionException("No URI provided");
        }

        try {
            uri = new URI(uriText);
        } catch (URISyntaxException ex) {
            throw new ConversionException(
                    "The URI '" + uriText + "' in node '" + reader.getNodeName() + "' is invalid!", ex);
        }

        // create the lists to hold parameters and groups
        List<ConfigDescriptionParameter> configDescriptionParams = new ArrayList<>();
        List<ConfigDescriptionParameterGroup> configDescriptionGroups = new ArrayList<>();

        // read values
        List<?> nodes = (List<?>) context.convertAnother(context, List.class);
        NodeIterator nodeIterator = new NodeIterator(nodes);

        // iterate through the nodes, putting the different types into their
        // respective arrays
        while (nodeIterator.hasNext()) {
            Object node = nodeIterator.next();
            if (node instanceof ConfigDescriptionParameter) {
                configDescriptionParams.add((ConfigDescriptionParameter) node);
            }
            if (node instanceof ConfigDescriptionParameterGroup) {
                configDescriptionGroups.add((ConfigDescriptionParameterGroup) node);
            }
        }

        ConverterAssertion.assertEndOfType(reader);

        return ConfigDescriptionBuilder.create(uri).withParameters(configDescriptionParams)
                .withParameterGroups(configDescriptionGroups).build();
    }
}
