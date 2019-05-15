/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptEngine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an abstract class for implementing {@link ScriptEngineFactory}s.
 *
 * @author Scott Rushworth - initial contribution
 */
@NonNullByDefault
public abstract class AbstractScriptEngineFactory implements ScriptEngineFactory {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public List<String> getScriptTypes() {
        List<String> scriptTypes = new ArrayList<>();

        for (javax.script.ScriptEngineFactory f : engineManager.getEngineFactories()) {
            scriptTypes.addAll(f.getExtensions());
            scriptTypes.addAll(f.getMimeTypes());
        }
        return Collections.unmodifiableList(scriptTypes);
    }

    @Override
    public void scopeValues(ScriptEngine scriptEngine, Map<String, Object> scopeValues) {
        for (Entry<String, Object> entry : scopeValues.entrySet()) {
            scriptEngine.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public @Nullable ScriptEngine createScriptEngine(String scriptType) {
        ScriptEngine scriptEngine = engineManager.getEngineByExtension(scriptType);
        if (scriptEngine == null) {
            scriptEngine = engineManager.getEngineByMimeType(scriptType);
        }
        if (scriptEngine == null) {
            scriptEngine = engineManager.getEngineByName(scriptType);
        }
        return scriptEngine;
    }

}
