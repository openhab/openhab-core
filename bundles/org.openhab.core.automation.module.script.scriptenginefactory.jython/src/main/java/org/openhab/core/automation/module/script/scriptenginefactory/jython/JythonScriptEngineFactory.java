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
package org.openhab.core.automation.module.script.scriptenginefactory.jython;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.AbstractScriptEngineFactory;
import org.openhab.core.automation.module.script.ScriptEngineFactory;
import org.osgi.service.component.annotations.Component;

/**
 * An implementation of {@link ScriptEngineFactory} for Jython.
 *
 * @author Scott Rushworth - Initial contribution
 */
@NonNullByDefault
@Component(service = org.openhab.core.automation.module.script.ScriptEngineFactory.class)
public class JythonScriptEngineFactory extends AbstractScriptEngineFactory {

    private static final String SCRIPT_TYPE = "py";
    private static javax.script.ScriptEngineManager ENGINE_MANAGER = new javax.script.ScriptEngineManager();

    public JythonScriptEngineFactory() {
        String home = JythonScriptEngineFactory.class.getProtectionDomain().getCodeSource().getLocation().toString()
                .replace("file:", "");
        String openhabConf = System.getenv("OPENHAB_CONF");
        StringBuilder newPythonPath = new StringBuilder();
        String previousPythonPath = System.getenv("python.path");
        if (previousPythonPath != null) {
            newPythonPath.append(previousPythonPath).append(File.pathSeparator);
        }
        newPythonPath.append(openhabConf).append(File.separator).append("automation").append(File.separator)
                .append("lib").append(File.separator).append("python");

        System.setProperty("python.home", home);
        System.setProperty("python.path", newPythonPath.toString());
        System.setProperty("python.cachedir", openhabConf);
        logger.trace("python.home [{}], python.path [{}]", System.getProperty("python.home"),
                System.getProperty("python.path"));
    }

    @Override
    public List<String> getScriptTypes() {
        List<String> scriptTypes = new ArrayList<>();

        for (javax.script.ScriptEngineFactory factory : ENGINE_MANAGER.getEngineFactories()) {
            List<String> extensions = factory.getExtensions();

            if (extensions.contains(SCRIPT_TYPE)) {
                scriptTypes.addAll(extensions);
                scriptTypes.addAll(factory.getMimeTypes());
            }
        }
        return Collections.unmodifiableList(scriptTypes);
    }

    @Override
    public @Nullable ScriptEngine createScriptEngine(String scriptType) {
        ScriptEngine scriptEngine = ENGINE_MANAGER.getEngineByExtension(scriptType);
        if (scriptEngine == null) {
            scriptEngine = ENGINE_MANAGER.getEngineByMimeType(scriptType);
        }
        if (scriptEngine == null) {
            scriptEngine = ENGINE_MANAGER.getEngineByName(scriptType);
        }
        return scriptEngine;
    }
}
