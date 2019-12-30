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
package org.openhab.core.config.xml.internal;

import java.util.List;

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
import org.openhab.core.config.xml.util.NodeListConverter;
import org.openhab.core.config.xml.util.NodeValue;
import org.openhab.core.config.xml.util.NodeValueConverter;
import org.openhab.core.config.xml.util.XmlDocumentReader;

import com.thoughtworks.xstream.XStream;

/**
 * The {@link ConfigDescriptionReader} reads XML documents, which contain the {@code config-descriptions} XML tag, and
 * converts them to {@link List} objects consisting of {@link ConfigDescription} objects.
 * <p>
 * This reader uses {@code XStream} and {@code StAX} to parse and convert the XML document.
 *
 * @author Michael Grammling - Initial contribution
 * @author Alex Tugarev - Extended for options and filter criteria
 * @author Chris Jackson - Added configuration groups
 */
public class ConfigDescriptionReader extends XmlDocumentReader<List<ConfigDescription>> {

    /**
     * The default constructor of this class.
     */
    public ConfigDescriptionReader() {
        super.setClassLoader(ConfigDescriptionReader.class.getClassLoader());
    }

    @Override
    public void registerConverters(XStream xstream) {
        xstream.registerConverter(new NodeValueConverter());
        xstream.registerConverter(new NodeListConverter());
        xstream.registerConverter(new NodeAttributesConverter());
        xstream.registerConverter(new ConfigDescriptionConverter());
        xstream.registerConverter(new ConfigDescriptionParameterConverter());
        xstream.registerConverter(new ConfigDescriptionParameterGroupConverter());
        xstream.registerConverter(new FilterCriteriaConverter());
    }

    @Override
    public void registerAliases(XStream xstream) {
        xstream.alias("config-descriptions", List.class);
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
