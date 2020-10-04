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
package org.openhab.core.binding.xml.internal;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterGroup;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.xml.ConfigDescriptionConverter;
import org.openhab.core.config.xml.ConfigDescriptionParameterConverter;
import org.openhab.core.config.xml.ConfigDescriptionParameterGroupConverter;
import org.openhab.core.config.xml.FilterCriteriaConverter;
import org.openhab.core.config.xml.util.NodeAttributes;
import org.openhab.core.config.xml.util.NodeAttributesConverter;
import org.openhab.core.config.xml.util.NodeList;
import org.openhab.core.config.xml.util.NodeValue;
import org.openhab.core.config.xml.util.NodeValueConverter;
import org.openhab.core.config.xml.util.XmlDocumentReader;

import com.thoughtworks.xstream.XStream;

/**
 * The {@link BindingInfoReader} reads XML documents, which contain the {@code binding} XML tag,
 * and converts them to {@link BindingInfoXmlResult} objects.
 * <p>
 * This reader uses {@code XStream} and {@code StAX} to parse and convert the XML document.
 *
 * @author Michael Grammling - Initial contribution
 * @author Alex Tugarev - Extended by options and filter criteria
 * @author Chris Jackson - Add parameter groups
 */
@NonNullByDefault
public class BindingInfoReader extends XmlDocumentReader<BindingInfoXmlResult> {

    /**
     * The default constructor of this class.
     */
    public BindingInfoReader() {
        super.setClassLoader(BindingInfoReader.class.getClassLoader());
    }

    @Override
    protected void registerConverters(XStream xstream) {
        xstream.registerConverter(new NodeAttributesConverter());
        xstream.registerConverter(new NodeValueConverter());
        xstream.registerConverter(new BindingInfoConverter());
        xstream.registerConverter(new ConfigDescriptionConverter());
        xstream.registerConverter(new ConfigDescriptionParameterConverter());
        xstream.registerConverter(new ConfigDescriptionParameterGroupConverter());
        xstream.registerConverter(new FilterCriteriaConverter());
    }

    @Override
    protected void registerAliases(XStream xstream) {
        xstream.alias("binding", BindingInfoXmlResult.class);
        xstream.alias("name", NodeValue.class);
        xstream.alias("description", NodeValue.class);
        xstream.alias("author", NodeValue.class);
        xstream.alias("service-id", NodeValue.class);
        xstream.alias("config-description", ConfigDescription.class);
        xstream.alias("config-description-ref", NodeAttributes.class);
        xstream.alias("parameter", ConfigDescriptionParameter.class);
        xstream.alias("parameter-group", ConfigDescriptionParameterGroup.class);
        xstream.alias("options", NodeList.class);
        xstream.alias("option", NodeValue.class);
        xstream.alias("filter", List.class);
        xstream.alias("criteria", FilterCriteria.class);
    }
}
