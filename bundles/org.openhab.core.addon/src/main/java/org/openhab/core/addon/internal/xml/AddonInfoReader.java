/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonMatchProperty;
import org.openhab.core.addon.AddonParameter;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterGroup;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.core.xml.ConfigDescriptionConverter;
import org.openhab.core.config.core.xml.ConfigDescriptionParameterConverter;
import org.openhab.core.config.core.xml.ConfigDescriptionParameterGroupConverter;
import org.openhab.core.config.core.xml.FilterCriteriaConverter;
import org.openhab.core.config.core.xml.util.NodeAttributes;
import org.openhab.core.config.core.xml.util.NodeAttributesConverter;
import org.openhab.core.config.core.xml.util.NodeList;
import org.openhab.core.config.core.xml.util.NodeListConverter;
import org.openhab.core.config.core.xml.util.NodeValue;
import org.openhab.core.config.core.xml.util.NodeValueConverter;
import org.openhab.core.config.core.xml.util.XmlDocumentReader;
import org.openhab.core.config.core.xml.util.XmlMapping;

/**
 * The {@link AddonInfoReader} reads XML documents, which contain the {@code addon} XML tag,
 * and converts them to {@link AddonInfoXmlResult} objects.
 * <p>
 * This reader uses {@code StAX} to parse and convert the XML document.
 *
 * @author Michael Grammling - Initial contribution
 * @author Alex Tugarev - Extended by options and filter criteria
 * @author Chris Jackson - Add parameter groups
 * @author Jan N. Klug - Refactored to cover all add-ons
 */
@NonNullByDefault
public class AddonInfoReader extends XmlDocumentReader<AddonInfoXmlResult> {

    @Override
    protected void registerConverters(XmlMapping mapping) {
        mapping.registerConverter(new NodeAttributesConverter());
        mapping.registerConverter(new NodeListConverter());
        mapping.registerConverter(new NodeValueConverter());
        mapping.registerConverter(new AddonInfoConverter());
        mapping.registerConverter(new ConfigDescriptionConverter());
        mapping.registerConverter(new ConfigDescriptionParameterConverter());
        mapping.registerConverter(new ConfigDescriptionParameterGroupConverter());
        mapping.registerConverter(new FilterCriteriaConverter());
        mapping.registerConverter(new AddonDiscoveryMethodConverter());
        mapping.registerConverter(new AddonParameterConverter());
        mapping.registerConverter(new AddonMatchPropertyConverter());
    }

    @Override
    protected void registerAliases(XmlMapping mapping) {
        mapping.alias("addon", AddonInfoXmlResult.class);
        mapping.alias("name", NodeValue.class);
        mapping.alias("description", NodeValue.class);
        mapping.alias("type", NodeValue.class);
        mapping.alias("keywords", NodeValue.class);
        mapping.alias("connection", NodeValue.class);
        mapping.alias("countries", NodeValue.class);
        mapping.alias("config-description", ConfigDescription.class);
        mapping.alias("config-description-ref", NodeAttributes.class);
        mapping.alias("parameter", ConfigDescriptionParameter.class);
        mapping.alias("parameter-group", ConfigDescriptionParameterGroup.class);
        mapping.alias("options", NodeList.class);
        mapping.alias("option", NodeValue.class);
        mapping.alias("filter", List.class);
        mapping.alias("criteria", FilterCriteria.class);
        mapping.alias("service-id", NodeValue.class);
        mapping.alias("discovery-methods", NodeList.class);
        mapping.alias("discovery-method", AddonDiscoveryMethod.class);
        mapping.alias("service-type", NodeValue.class);
        mapping.alias("discovery-parameters", NodeList.class);
        mapping.alias("discovery-parameter", AddonParameter.class);
        mapping.alias("match-properties", NodeList.class);
        mapping.alias("match-property", AddonMatchProperty.class);
        mapping.alias("value", NodeValue.class);
        mapping.alias("regex", NodeValue.class);
    }
}
