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
package org.eclipse.smarthome.automation.module.script.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.eclipse.smarthome.automation.module.script.ScriptEngineFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Simon Merschjohann - Initial contribution
 */
@Component(service = ScriptEngineFactory.class)
public class NashornScriptEngineFactory implements ScriptEngineFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ScriptEngineManager engineManager = new ScriptEngineManager();

    @Override
    public List<String> getLanguages() {
        return Arrays.asList("js", "javascript", "application/javascript");
    }

    @Override
    public void scopeValues(ScriptEngine engine, Map<String, Object> scopeValues) {
        Set<String> expressions = new HashSet<String>();

        for (Entry<String, Object> entry : scopeValues.entrySet()) {
            engine.put(entry.getKey(), entry.getValue());

            if (entry.getValue() instanceof Class) {
                expressions.add(String.format("%s = %s.static;", entry.getKey(), entry.getKey()));
            }
        }
        String scriptToEval = String.join("\n", expressions);
        try {
            engine.eval(scriptToEval);
        } catch (ScriptException e) {
            logger.error("ScriptException while importing scope: {}", e.getMessage());
        }
    }

    @Override
    public ScriptEngine createScriptEngine(String fileExtension) {
        ScriptEngine engine = engineManager.getEngineByExtension(fileExtension);

        if (engine == null) {
            engine = engineManager.getEngineByName(fileExtension);
        }

        if (engine == null) {
            engine = engineManager.getEngineByMimeType(fileExtension);
        }

        return engine;
    }

    @Override
    public boolean isSupported(String fileExtension) {
        return getLanguages().contains(fileExtension);
    }

}
