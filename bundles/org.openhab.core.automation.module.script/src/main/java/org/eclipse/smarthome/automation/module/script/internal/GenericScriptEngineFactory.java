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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.eclipse.smarthome.automation.module.script.ScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Simon Merschjohann - Initial contribution
 */
public class GenericScriptEngineFactory implements ScriptEngineFactory {
    private ScriptEngineManager engineManager = new ScriptEngineManager();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public GenericScriptEngineFactory() {
        for (javax.script.ScriptEngineFactory f : engineManager.getEngineFactories()) {
            logger.info("Activated scripting support for {}", f.getLanguageName());
            logger.debug(
                    "Activated scripting support with engine {}({}) for {}({}) with mimetypes {} and file extensions {}",
                    f.getEngineName(), f.getEngineVersion(), f.getLanguageName(), f.getLanguageVersion(),
                    f.getMimeTypes(), f.getExtensions());
        }
    }

    @Override
    public List<String> getLanguages() {
        ArrayList<String> languages = new ArrayList<>();

        for (javax.script.ScriptEngineFactory f : engineManager.getEngineFactories()) {
            languages.addAll(f.getExtensions());
        }

        return languages;
    }

    @Override
    public void scopeValues(ScriptEngine scriptEngine, Map<String, Object> scopeValues) {
        for (Entry<String, Object> entry : scopeValues.entrySet()) {
            scriptEngine.put(entry.getKey(), entry.getValue());
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
        for (javax.script.ScriptEngineFactory f : engineManager.getEngineFactories()) {
            if (f.getExtensions().contains(fileExtension)) {
                return true;
            }
        }

        return false;
    }

}
