/**
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.eclipse.smarthome.automation.module.script.ScriptEngineFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Scott Rushworth - Initial contribution
 */
@Component(service = ScriptEngineFactory.class)
public class GroovyScriptEngineFactory implements ScriptEngineFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ScriptEngineManager engineManager = new ScriptEngineManager();

    @Override
    public List<String> getLanguages() {
        return Arrays.asList("groovy", "application/groovy");
    }

    @Override
    public void scopeValues(ScriptEngine scriptEngine, Map<String, Object> scopeValues) {
        for (Entry<String, Object> entry : scopeValues.entrySet()) {
            scriptEngine.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public ScriptEngine createScriptEngine(String fileExtension) {
        if (!getLanguages().contains(fileExtension)) {
            logger.error("Invalid fileExtension: {}", fileExtension);
            return null;
        }

        return engineManager.getEngineByName("groovy");
    }

    @Override
    public boolean isSupported(String fileExtension) {
        return getLanguages().contains(fileExtension);
    }
}
