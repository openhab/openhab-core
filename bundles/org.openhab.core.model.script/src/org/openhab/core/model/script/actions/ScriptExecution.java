/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.xbase.lib.Procedures;
import org.openhab.core.automation.module.script.action.Timer;
import org.openhab.core.model.script.engine.action.ActionDoc;
import org.openhab.core.model.script.internal.engine.action.ScriptExecutionActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

/**
 * The {@link ScriptExecution} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault public class ScriptExecution {

    private static final Logger logger = LoggerFactory.getLogger(ScriptExecution.class);

    public static Object callScript(String scriptUid) {
        return ScriptExecutionActionService.getScriptExecution().callScript(scriptUid);
    }

    @ActionDoc(text = "create a timer")
    public static Timer createTimer(ZonedDateTime zonedDateTime, Procedures.Procedure0 closure) {
        return ScriptExecutionActionService.getScriptExecution().createTimer(zonedDateTime, closure::apply);
    }

    public static Timer createTimer(@Nullable String identifier,  ZonedDateTime zonedDateTime, Procedures.Procedure0 closure) {
        return ScriptExecutionActionService.getScriptExecution().createTimer(identifier, zonedDateTime, closure::apply);
    }

    public static Timer createTimerWithArgument(ZonedDateTime zonedDateTime, Object arg1, Procedures.Procedure1 closure) {
        return ScriptExecutionActionService.getScriptExecution().createTimerWithArgument(zonedDateTime, arg1, closure::apply);
    }

    public static Timer createTimerWithArgument(@Nullable String identifier,  ZonedDateTime zonedDateTime, Object arg1, Procedures.Procedure1 closure) {
        return ScriptExecutionActionService.getScriptExecution().createTimerWithArgument(identifier, zonedDateTime, arg1, closure::apply);
    }
}
