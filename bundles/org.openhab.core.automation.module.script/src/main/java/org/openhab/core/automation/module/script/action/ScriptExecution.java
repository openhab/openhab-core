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
package org.openhab.core.automation.module.script.action;

import java.time.ZonedDateTime;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ScriptExecution} allows creating timers for asynchronous script execution
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface ScriptExecution {

    /**
     * Schedules a block of code for later execution.
     *
     * @param zonedDateTime the point in time when the code should be executed
     * @param closure the code block to execute
     * @return a handle to the created timer, so that it can be canceled or rescheduled
     */
    Timer createTimer(ZonedDateTime zonedDateTime, Runnable closure);

    /**
     * Schedules a block of code for later execution.
     *
     * @param identifier an optional identifier
     * @param zonedDateTime the point in time when the code should be executed
     * @param closure the code block to execute
     * @return a handle to the created timer, so that it can be canceled or rescheduled
     */
    Timer createTimer(@Nullable String identifier, ZonedDateTime zonedDateTime, Runnable closure);

    /**
     * Schedules a block of code (with argument) for later execution
     *
     * @param zonedDateTime the point in time when the code should be executed
     * @param arg1 the argument to pass to the code block
     * @param closure the code block to execute
     * @return a handle to the created timer, so that it can be canceled or rescheduled
     */
    Timer createTimerWithArgument(ZonedDateTime zonedDateTime, Object arg1, Consumer<Object> closure);

    /**
     * Schedules a block of code (with argument) for later execution
     *
     * @param identifier an optional identifier
     * @param zonedDateTime the point in time when the code should be executed
     * @param arg1 the argument to pass to the code block
     * @param closure the code block to execute
     * @return a handle to the created timer, so that it can be canceled or rescheduled
     */
    Timer createTimerWithArgument(@Nullable String identifier, ZonedDateTime zonedDateTime, Object arg1,
            Consumer<Object> closure);
}
