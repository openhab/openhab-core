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
package org.eclipse.smarthome.automation.module.script;

import java.io.InputStreamReader;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 *
 * @author Simon Merschjohann - Initial contribution
 */
@NonNullByDefault
public interface ScriptEngineManager {

    /**
     * Checks if a given fileExtension is supported
     *
     * @param fileExtension
     * @return true if supported
     */
    boolean isSupported(String fileExtension);

    /**
     * Creates a new ScriptEngine based on the given fileExtension
     *
     * @param fileExtension
     * @param scriptIdentifier
     * @return
     */
    @Nullable
    ScriptEngineContainer createScriptEngine(String fileExtension, String scriptIdentifier);

    /**
     * Loads a script and initializes its scope variables
     *
     * @param fileExtension
     * @param scriptIdentifier
     * @param scriptData
     * @return
     */
    void loadScript(String scriptIdentifier, InputStreamReader scriptData);

    /**
     * Unloads the ScriptEngine loaded with the scriptIdentifer
     *
     * @param scriptIdentifier
     */
    void removeEngine(String scriptIdentifier);

}
