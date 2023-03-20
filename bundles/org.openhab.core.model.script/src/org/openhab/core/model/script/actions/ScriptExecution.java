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
package org.openhab.core.model.script.actions;

import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.lib.Procedures;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.script.ScriptServiceUtil;
import org.openhab.core.model.script.engine.Script;
import org.openhab.core.model.script.engine.ScriptEngine;
import org.openhab.core.model.script.engine.ScriptExecutionException;
import org.openhab.core.model.script.engine.action.ActionDoc;
import org.openhab.core.model.script.internal.engine.action.ScriptExecutionActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ScriptExecution} is a wrapper for the ScriptExecution actions
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ScriptExecution {

    private static final Logger logger = LoggerFactory.getLogger(ScriptExecution.class);

    /**
     * Calls a script which must be located in the configurations/scripts folder.
     *
     * @param scriptName the name of the script (if the name does not end with
     *            the .script file extension it is added)
     *
     * @return the return value of the script
     * @throws ScriptExecutionException if an error occurs during the execution
     */
    @ActionDoc(text = "call a script file")
    public static Object callScript(String scriptName) throws ScriptExecutionException {
        ModelRepository repo = ScriptServiceUtil.getModelRepository();
        if (repo != null) {
            String scriptNameWithExt = scriptName;
            if (!scriptName.endsWith(Script.SCRIPT_FILEEXT)) {
                scriptNameWithExt = scriptName + "." + Script.SCRIPT_FILEEXT;
            }
            XExpression expr = (XExpression) repo.getModel(scriptNameWithExt);
            if (expr != null) {
                ScriptEngine scriptEngine = ScriptServiceUtil.getScriptEngine();
                if (scriptEngine != null) {
                    Script script = scriptEngine.newScriptFromXExpression(expr);
                    return script.execute();
                } else {
                    throw new ScriptExecutionException("Script engine is not available.");
                }
            } else {
                throw new ScriptExecutionException("Script '" + scriptName + "' cannot be found.");
            }
        } else {
            throw new ScriptExecutionException("Model repository is not available.");
        }
    }

    @ActionDoc(text = "create a timer")
    public static Timer createTimer(ZonedDateTime zonedDateTime, Procedures.Procedure0 closure) {
        return new Timer(ScriptExecutionActionService.getScriptExecution().createTimer(zonedDateTime, closure::apply));
    }

    @ActionDoc(text = "create an identifiable timer ")
    public static Timer createTimer(@Nullable String identifier, ZonedDateTime zonedDateTime,
            Procedures.Procedure0 closure) {
        return new Timer(ScriptExecutionActionService.getScriptExecution().createTimer(identifier, zonedDateTime,
                closure::apply));
    }

    @ActionDoc(text = "create a timer with argument")
    public static Timer createTimerWithArgument(ZonedDateTime zonedDateTime, Object arg1,
            Procedures.Procedure1 closure) {
        return new Timer(ScriptExecutionActionService.getScriptExecution().createTimerWithArgument(zonedDateTime, arg1,
                closure::apply));
    }

    @ActionDoc(text = "create an identifiable timer with argument")
    public static Timer createTimerWithArgument(@Nullable String identifier, ZonedDateTime zonedDateTime, Object arg1,
            Procedures.Procedure1 closure) {
        return new Timer(ScriptExecutionActionService.getScriptExecution().createTimerWithArgument(identifier,
                zonedDateTime, arg1, closure::apply));
    }
}
