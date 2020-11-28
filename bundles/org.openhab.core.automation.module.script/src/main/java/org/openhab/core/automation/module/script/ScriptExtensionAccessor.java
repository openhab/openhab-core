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
package org.openhab.core.automation.module.script;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Accessor allowing script engines to lookup presets.
 *
 * @author Jonathan Gilbert - Initial contribution
 */
@NonNullByDefault
public interface ScriptExtensionAccessor {

    /**
     * Access the default presets for a script engine
     *
     * @param scriptIdentifier the identifier for the script engine
     * @return map of preset objects
     */
    Map<String, Object> findDefaultPresets(String scriptIdentifier);

    /**
     * Access specific presets for a script engine
     *
     * @param preset the name of the preset
     * @param scriptIdentifier the identifier for the script engine
     * @return map of preset objects
     */
    Map<String, Object> findPreset(String preset, String scriptIdentifier);
}
