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
package org.openhab.core.automation.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openhab.core.automation.Visibility;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterDTO;

/**
 * This is a data transfer object that is used to serialize rules.
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class RuleDTO {

    public List<TriggerDTO> triggers;
    public List<ConditionDTO> conditions;
    public List<ActionDTO> actions;
    public Map<String, Object> configuration;
    public List<ConfigDescriptionParameterDTO> configDescriptions;
    public String templateUID;
    public String uid;
    public String name;
    public Set<String> tags;
    public Visibility visibility;
    public String description;

}
