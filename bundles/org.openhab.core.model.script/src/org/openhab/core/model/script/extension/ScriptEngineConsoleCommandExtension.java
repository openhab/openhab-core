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
package org.openhab.core.model.script.extension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.model.script.engine.Script;
import org.openhab.core.model.script.engine.ScriptEngine;
import org.openhab.core.model.script.engine.ScriptExecutionException;
import org.openhab.core.model.script.engine.ScriptParsingException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This class provides the script engine as a console command
 *
 * @author Oliver Libutzki - Initial contribution
 */
@Component(service = ConsoleCommandExtension.class)
public class ScriptEngineConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private ScriptEngine scriptEngine;

    public ScriptEngineConsoleCommandExtension() {
        super("script", "Execute scripts");
    }

    @Override
    public void execute(String[] args, Console console) {
        if (scriptEngine != null) {
            String scriptString = Arrays.stream(args).collect(Collectors.joining(" "));
            Script script;
            try {
                script = scriptEngine.newScriptFromString(scriptString);
                Object result = script.execute();

                if (result != null) {
                    console.println(result.toString());
                } else {
                    console.println("OK");
                }
            } catch (ScriptParsingException e) {
                console.println(e.getMessage());
            } catch (ScriptExecutionException e) {
                console.println(e.getMessage());
            }
        } else {
            console.println("Script engine is not available.");
        }
    }

    @Override
    public List<String> getUsages() {
        return Collections.singletonList(buildCommandUsage("<script to execute>", "Executes a script"));
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    public void setScriptEngine(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    public void unsetScriptEngine(ScriptEngine scriptEngine) {
        this.scriptEngine = null;
    }
}
