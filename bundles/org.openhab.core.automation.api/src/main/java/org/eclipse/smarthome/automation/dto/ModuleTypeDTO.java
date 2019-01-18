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
package org.eclipse.smarthome.automation.dto;

import java.util.List;
import java.util.Set;

import org.eclipse.smarthome.automation.Visibility;
import org.eclipse.smarthome.config.core.dto.ConfigDescriptionParameterDTO;

/**
 * This is a data transfer object that is used to serialize the respective class.
 *
 * @author Markus Rathgeb - Initial contribution and API
 */
public class ModuleTypeDTO {

    public String uid;
    public Visibility visibility;
    public Set<String> tags;
    public String label;
    public String description;
    public List<ConfigDescriptionParameterDTO> configDescriptions;

}
