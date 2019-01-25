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
package org.openhab.core.automation.dto;

import java.util.List;
import java.util.Set;

import org.eclipse.smarthome.config.core.dto.ConfigDescriptionParameterDTO;
import org.openhab.core.automation.Visibility;

/**
 * This is a data transfer object that is used to serialize the rule templates.
 *
 * @author Ana Dimova - Initial contribution
 *
 */
public class RuleTemplateDTO {
    public String label;
    public String uid;
    public Set<String> tags;
    public String description;
    public Visibility visibility;
    public List<ConfigDescriptionParameterDTO> configDescriptions;
    public List<TriggerDTO> triggers;
    public List<ConditionDTO> conditions;
    public List<ActionDTO> actions;
}
