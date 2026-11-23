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
package org.openhab.core.io.rest.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.dto.ConfigDescriptionDTO;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterDTO;
import org.openhab.core.ui.components.RootUIComponent;

/**
 * A {@link RootUIComponent} enriched with runtime-computed information for REST API responses.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
public class EnrichedRootUIComponent extends RootUIComponent {

    /**
     * Whether the component can be modified or deleted through the REST API.
     * Components loaded from YAML configuration files are not editable.
     */
    public boolean editable;

    public EnrichedRootUIComponent(RootUIComponent component, boolean editable) {
        super(component.getUID(), component.getType());
        setConfig(component.getConfig());
        setSlots(component.getSlots());
        if (component.getTags() != null) {
            addTags(component.getTags());
        }
        Date timestamp = component.getTimestamp();
        if (timestamp != null) {
            setTimestamp(timestamp);
        }
        setProps(addDefaultValues(component.getProps()));
        this.editable = editable;
    }

    /**
     * Provides the list of default values for parameters with <code>multiple=true</code> that only define the
     * comma separated <code>default</code> property, so that clients get the same result for both notations.
     * The original {@link ConfigDescriptionDTO} is not modified.
     */
    private static ConfigDescriptionDTO addDefaultValues(ConfigDescriptionDTO props) {
        if (props == null || props.parameters == null) {
            return props;
        }
        List<ConfigDescriptionParameterDTO> parameters = new ArrayList<>(props.parameters.size());
        for (ConfigDescriptionParameterDTO parameter : props.parameters) {
            if (parameter.defaultValues == null && parameter.defaultValue != null && parameter.type != null
                    && Boolean.TRUE.equals(parameter.multiple)) {
                ConfigDescriptionParameter cdp = ConfigDescriptionParameterBuilder
                        .create(parameter.name, parameter.type).withMultiple(true).withDefault(parameter.defaultValue)
                        .withMultipleLimit(parameter.multipleLimit).build();
                if (ConfigUtil.getDefaultValueAsCorrectType(cdp) instanceof List<?> list) {
                    parameter = copyWithDefaultValues(parameter, list);
                }
            }
            parameters.add(parameter);
        }
        return new ConfigDescriptionDTO(props.uri, parameters, props.parameterGroups);
    }

    @SuppressWarnings("unchecked")
    private static ConfigDescriptionParameterDTO copyWithDefaultValues(ConfigDescriptionParameterDTO p,
            List<?> values) {
        ConfigDescriptionParameterDTO c = new ConfigDescriptionParameterDTO(p.name, p.type, p.min, p.max, p.stepsize,
                p.pattern, p.required, p.readOnly, p.multiple, p.context, p.defaultValue, p.label, p.description,
                p.options, p.filterCriteria, p.groupName, p.advanced, p.limitToOptions, p.multipleLimit, p.unit,
                p.unitLabel, p.verify);
        c.defaultValues = (Collection<String>) values;
        return c;
    }
}
