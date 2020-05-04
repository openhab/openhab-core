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

import org.openhab.core.config.core.ConfigDescriptionParameterGroup;
import org.openhab.core.config.core.ConfigDescriptionParameterGroupBuilder;
import org.openhab.core.config.xml.util.ConverterValueMap;
import org.openhab.core.config.xml.util.GenericUnmarshaller;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link ConfigDescriptionParameterGroupConverter} creates a {@link ConfigDescriptionParameterGroup} instance from
 * a {@code option} XML
 * node.
 *
 * @author Chris Jackson - Initial contribution
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

        return ConfigDescriptionParameterGroupBuilder.create(name) //
                .withContext(context) //
                .withAdvanced(advanced) //
                .withLabel(label) //
                .withDescription(description) //
                .build();
    }
}
