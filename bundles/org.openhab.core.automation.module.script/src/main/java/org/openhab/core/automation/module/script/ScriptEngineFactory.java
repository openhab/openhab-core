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

import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.internal.provider.ScriptModuleTypeProvider;

/**
 * This class is used by the ScriptEngineManager to load ScriptEngines. This is meant as a way to allow other OSGi
 * bundles to provide custom script-languages with special needs, e.g. Nashorn, Jython, Groovy, etc.
 *
 * @author Simon Merschjohann - Initial contribution
 * @author Scott Rushworth - added/changed methods and parameters when implementing {@link ScriptModuleTypeProvider}
 * @author Jonathan Gilbert - added context keys
 */
@NonNullByDefault
public interface ScriptEngineFactory {

    static final ScriptEngineManager ENGINE_MANAGER = new ScriptEngineManager();

    /**
     * Key to access engine identifier in script context.
     */
    String CONTEXT_KEY_ENGINE_IDENTIFIER = "oh.engine-identifier";

    /**
     * This method returns a list of file extensions and MimeTypes that are supported by the ScriptEngine, e.g. py,
     * application/python, js, application/javascript, etc.
     *
     * @return List of supported script types
     */
    List<String> getScriptTypes();

    /**
     * This method "scopes" new values into the given ScriptEngine.
     *
     * @param scriptEngine
     * @param scopeValues
     */
    void scopeValues(ScriptEngine scriptEngine, Map<String, Object> scopeValues);

    /**
     * This method creates a new ScriptEngine based on the supplied file extension or MimeType.
     *
     * @param scriptType a file extension (script) or MimeType (ScriptAction or ScriptCondition)
     * @return ScriptEngine or null
     */
    @Nullable
    ScriptEngine createScriptEngine(String scriptType);
}
