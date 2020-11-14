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
package org.openhab.core.automation.module.script.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.AbstractScriptEngineFactory;
import org.openhab.core.automation.module.script.ScriptEngineFactory;
import org.osgi.service.component.annotations.Component;

/**
 * An implementation of {@link ScriptEngineFactory} with customizations for Nashorn ScriptEngines.
 *
 * @author Simon Merschjohann - Initial contribution
 * @author Scott Rushworth - removed default methods provided by ScriptEngineFactory
 * @author Yannick Schaus - create script engines with the bundle's class loader as "app" class loader
 */
@NonNullByDefault
@Component(service = ScriptEngineFactory.class)
public class NashornScriptEngineFactory extends AbstractScriptEngineFactory {

    private static final String SCRIPT_TYPE = "js";

    @Override
    public List<String> getScriptTypes() {
        List<String> scriptTypes = new ArrayList<>();

        for (javax.script.ScriptEngineFactory f : ENGINE_MANAGER.getEngineFactories()) {
            List<String> extensions = f.getExtensions();

            if (extensions.contains(SCRIPT_TYPE)) {
                scriptTypes.addAll(extensions);
                scriptTypes.addAll(f.getMimeTypes());
            }
        }
        return Collections.unmodifiableList(scriptTypes);
    }

    @Override
    public void scopeValues(ScriptEngine scriptEngine, Map<String, Object> scopeValues) {
        Set<String> expressions = new HashSet<>();

        for (Entry<String, Object> entry : scopeValues.entrySet()) {
            scriptEngine.put(entry.getKey(), entry.getValue());
            if (entry.getValue() instanceof Class) {
                expressions.add(String.format("%s = %<s.static;", entry.getKey()));
            }
        }
        String scriptToEval = String.join("\n", expressions);
        try {
            scriptEngine.eval(scriptToEval);
        } catch (ScriptException ex) {
            logger.error("ScriptException while importing scope: {}", ex.getMessage());
        }
    }

    @Override
    public @Nullable ScriptEngine createScriptEngine(String scriptType) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(NashornScriptEngineFactory.class.getClassLoader());
        ScriptEngine scriptEngine = super.createScriptEngine(scriptType);
        Thread.currentThread().setContextClassLoader(originalClassLoader);
        return scriptEngine;
    }
}
