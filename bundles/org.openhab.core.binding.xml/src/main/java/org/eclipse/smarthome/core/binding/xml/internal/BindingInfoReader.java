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
package org.eclipse.smarthome.core.binding.xml.internal;

import java.util.List;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterGroup;
import org.eclipse.smarthome.config.core.FilterCriteria;
import org.eclipse.smarthome.config.xml.ConfigDescriptionConverter;
import org.eclipse.smarthome.config.xml.ConfigDescriptionParameterConverter;
import org.eclipse.smarthome.config.xml.ConfigDescriptionParameterGroupConverter;
import org.eclipse.smarthome.config.xml.FilterCriteriaConverter;
import org.eclipse.smarthome.config.xml.util.NodeAttributes;
import org.eclipse.smarthome.config.xml.util.NodeAttributesConverter;
import org.eclipse.smarthome.config.xml.util.NodeList;
import org.eclipse.smarthome.config.xml.util.NodeValue;
import org.eclipse.smarthome.config.xml.util.NodeValueConverter;
import org.eclipse.smarthome.config.xml.util.XmlDocumentReader;

import com.thoughtworks.xstream.XStream;

/**
 * The {@link BindingInfoReader} reads XML documents, which contain the {@code binding} XML tag,
 * and converts them to {@link BindingInfoXmlResult} objects.
 * <p>
 * This reader uses {@code XStream} and {@code StAX} to parse and convert the XML document.
 *
 * @author Michael Grammling - Initial Contribution
 * @author Alex Tugarev - Extended by options and filter criteria
 * @author Chris Jackson - Add parameter groups
 */
public class BindingInfoReader extends XmlDocumentReader<BindingInfoXmlResult> {

    /**
     * The default constructor of this class.
     */
    public BindingInfoReader() {
        super.setClassLoader(BindingInfoReader.class.getClassLoader());
    }

    @Override
    public void registerConverters(XStream xstream) {
        xstream.registerConverter(new NodeAttributesConverter());
        xstream.registerConverter(new NodeValueConverter());
        xstream.registerConverter(new BindingInfoConverter());
        xstream.registerConverter(new ConfigDescriptionConverter());
        xstream.registerConverter(new ConfigDescriptionParameterConverter());
        xstream.registerConverter(new ConfigDescriptionParameterGroupConverter());
        xstream.registerConverter(new FilterCriteriaConverter());
    }

    @Override
    public void registerAliases(XStream xstream) {
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
