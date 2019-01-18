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
package org.eclipse.smarthome.config.xml;

import org.eclipse.smarthome.config.core.ConfigDescriptionParameterGroup;
import org.eclipse.smarthome.config.xml.util.ConverterValueMap;
import org.eclipse.smarthome.config.xml.util.GenericUnmarshaller;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link ConfigDescriptionParameterGroupConverter} creates a {@link ConfigDescriptionParameterGroup} instance from
 * a {@code option} XML
 * node.
 *
 * @author Chris Jackson - Initial Contribution
 */
public class ConfigDescriptionParameterGroupConverter extends GenericUnmarshaller<ConfigDescriptionParameterGroup> {

    public ConfigDescriptionParameterGroupConverter() {
        super(ConfigDescriptionParameterGroup.class);
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext marshallingContext) {
        String name = reader.getAttribute("name");

        // Read values
        ConverterValueMap valueMap = new ConverterValueMap(reader, marshallingContext);

        String context = valueMap.getString("context");
        String description = valueMap.getString("description");
        String label = valueMap.getString("label");
        Boolean advanced = valueMap.getBoolean("advanced", false);

        return new ConfigDescriptionParameterGroup(name, context, advanced, label, description);
    }
}
