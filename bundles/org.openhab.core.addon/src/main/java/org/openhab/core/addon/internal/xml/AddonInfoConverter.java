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
package org.openhab.core.addon.internal.xml;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.xml.util.ConverterAttributeMapValidator;
import org.openhab.core.config.core.xml.util.GenericUnmarshaller;
import org.openhab.core.config.core.xml.util.NodeIterator;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link AddonInfoConverter} is a concrete implementation of the {@code XStream} {@link Converter} interface used
 * to convert add-on information within an XML document into a {@link AddonInfoXmlResult} object.
 * This converter converts {@code addon} XML tags.
 *
 * @author Michael Grammling - Initial contribution
 * @author Andre Fuechsel - Made author tag optional
 * @author Jan N. Klug - Refactored to cover all add-ons
 */
@NonNullByDefault
public class AddonInfoConverter extends GenericUnmarshaller<AddonInfoXmlResult> {
    private static final String CONFIG_DESCRIPTION_URI_PLACEHOLDER = "addonInfoConverter:placeHolder";
    private final ConverterAttributeMapValidator attributeMapValidator;

    public AddonInfoConverter() {
        super(AddonInfoXmlResult.class);

        attributeMapValidator = new ConverterAttributeMapValidator(Map.of("id", true, "schemaLocation", false));
    }

    private @Nullable ConfigDescription readConfigDescription(NodeIterator nodeIterator) {
        Object nextNode = nodeIterator.next();

        if (nextNode != null) {
            if (nextNode instanceof ConfigDescription configDescription) {
                return configDescription;
            }

            nodeIterator.revert();
        }

        return null;
    }

    @Override
    public @Nullable Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        // read attributes
        Map<String, String> attributes = attributeMapValidator.readValidatedAttributes(reader);

        String id = requireNonEmpty(attributes.get("id"), "Add-on id attribute is null or empty");

        // set automatically extracted URI for a possible 'config-description' section
        context.put("config-description.uri", CONFIG_DESCRIPTION_URI_PLACEHOLDER);

        // read values
        List<?> nodes = (List<?>) context.convertAnother(context, List.class);
        NodeIterator nodeIterator = new NodeIterator(nodes);

        String type = requireNonEmpty((String) nodeIterator.nextValue("type", true), "Add-on type is null or empty");

        String name = requireNonEmpty((String) nodeIterator.nextValue("name", true),
                "Add-on name attribute is null or empty");
        String description = requireNonEmpty((String) nodeIterator.nextValue("description", true),
                "Add-on description is null or empty");

        AddonInfo.Builder addonInfo = AddonInfo.builder(id, type).withName(name).withDescription(description);
        addonInfo.withConnection((String) nodeIterator.nextValue("connection", false));
        addonInfo.withCountries((String) nodeIterator.nextValue("countries", false));
        addonInfo.withServiceId((String) nodeIterator.nextValue("service-id", false));

        String configDescriptionURI = nodeIterator.nextAttribute("config-description-ref", "uri", false);
        ConfigDescription configDescription = null;
        if (configDescriptionURI == null) {
            configDescription = readConfigDescription(nodeIterator);
            if (configDescription != null) {
                configDescriptionURI = configDescription.getUID().toString();
                // if config description is missing the URI, recreate it with correct URI
                if (CONFIG_DESCRIPTION_URI_PLACEHOLDER.equals(configDescriptionURI)) {
                    configDescriptionURI = type + ":" + id;
                    configDescription = ConfigDescriptionBuilder.create(URI.create(configDescriptionURI))
                            .withParameterGroups(configDescription.getParameterGroups())
                            .withParameters(configDescription.getParameters()).build();
                }
            }
        }

        addonInfo.withConfigDescriptionURI(configDescriptionURI);

        nodeIterator.assertEndOfType();

        // create object
        return new AddonInfoXmlResult(addonInfo.build(), configDescription);
    }
}
