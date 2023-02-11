/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.parser.gson;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.dto.CompositeActionTypeDTO;
import org.openhab.core.automation.dto.CompositeConditionTypeDTO;
import org.openhab.core.automation.dto.CompositeTriggerTypeDTO;

/**
 * This is a helper data structure for GSON that represents the JSON format used when having different module types
 * within a single input stream.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class ModuleTypeParsingContainer {

    public @Nullable List<CompositeTriggerTypeDTO> triggers;

    public @Nullable List<CompositeConditionTypeDTO> conditions;

    public @Nullable List<CompositeActionTypeDTO> actions;
}
