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

import java.io.InputStreamReader;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.internal.provider.ScriptModuleTypeProvider;

/**
 * The ScriptEngineManager provides the ability to load and unload scripts.
 *
 * @author Simon Merschjohann - Initial contribution
 * @author Scott Rushworth - changed parameter names when implementing {@link ScriptModuleTypeProvider}
 */
@NonNullByDefault
public interface ScriptEngineManager {

    /**
     * Creates a new ScriptEngine used to execute scripts, ScriptActions or ScriptConditions
     *
     * @param scriptType a file extension (script) or MimeType (ScriptAction or ScriptCondition)
     * @param engineIdentifier the unique identifier for the ScriptEngine (script file path or UUID)
     * @return ScriptEngineContainer or null
     */
    @Nullable
    ScriptEngineContainer createScriptEngine(String scriptType, String engineIdentifier);

    /**
     * Loads a script and initializes its scope variables
     *
     * @param engineIdentifier the unique identifier for the ScriptEngine (script file path or UUID)
     * @param scriptData the content of the script
     * @return <code>true</code> if the script was successfully loaded, <code>false</code> otherwise
     */
    boolean loadScript(String engineIdentifier, InputStreamReader scriptData);

    /**
     * Unloads the ScriptEngine loaded with the engineIdentifier
     *
     * @param engineIdentifier the unique identifier for the ScriptEngine (script file path or UUID)
     */
    void removeEngine(String engineIdentifier);

    /**
     * Checks if the supplied file extension or MimeType is supported by the existing ScriptEngineFactories
     *
     * @param scriptType a file extension (script) or MimeType (ScriptAction or ScriptCondition)
     * @return true, if supported, else false
     */
    boolean isSupported(String scriptType);

    /**
     * Add a listener that is notified when a ScriptEngineFactory is added or removed
     *
     * @param listener an object that implements {@link FactoryChangeListener}
     */
    void addFactoryChangeListener(FactoryChangeListener listener);

    /**
     * Remove a listener that is notified when a ScriptEngineFactory is added or removed
     *
     * @param listener an object that implements {@link FactoryChangeListener}
     */
    void removeFactoryChangeListener(FactoryChangeListener listener);

    interface FactoryChangeListener {

        /**
         * Called by the {@link ScriptEngineManager} when a ScriptEngineFactory is added
         *
         * @param scriptType the script type supported by the newly added factory
         */
        void factoryAdded(String scriptType);

        /**
         * Called by the {@link ScriptEngineManager} when a ScriptEngineFactory is removed
         *
         * @param scriptType the script type supported by the removed factory
         */
        void factoryRemoved(String scriptType);
    }
}
