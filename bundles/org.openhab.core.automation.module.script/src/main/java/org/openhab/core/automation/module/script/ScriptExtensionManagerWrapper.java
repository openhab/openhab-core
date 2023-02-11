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
package org.openhab.core.automation.module.script;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.internal.ScriptExtensionManager;

/**
 *
 * @author Simon Merschjohann - Initial contribution
 */
@NonNullByDefault
public interface ScriptExtensionManagerWrapper {
    List<String> getTypes();

    List<String> getPresets();

    @Nullable
    Object get(String type);

    String getScriptIdentifier();

    List<String> getDefaultPresets();

    /**
     * Imports a collection of named host objects/classes into a script engine instance. Sets of objects are provided
     * under their object name, and categorized by preset name. This method will import all named objects for a specific
     * preset name.
     *
     * @param preset the name of the preset to import
     * @return a map of host object names to objects
     * @see ScriptExtensionManager
     */
    Map<String, Object> importPreset(String preset);
}
