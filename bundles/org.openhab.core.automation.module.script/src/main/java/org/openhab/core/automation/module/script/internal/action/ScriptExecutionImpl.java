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
package org.openhab.core.automation.module.script.internal.action;

import java.time.ZonedDateTime;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.action.ScriptExecution;
import org.openhab.core.automation.module.script.action.Timer;
import org.openhab.core.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The static methods of this class are made available as functions in the scripts.
 * This allows a script to call another script, which is available as a file.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(immediate = true, service = ScriptExecution.class)
@NonNullByDefault
public class ScriptExecutionImpl implements ScriptExecution {

    private final Scheduler scheduler;

    @Activate
    public ScriptExecutionImpl(@Reference Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Timer createTimer(ZonedDateTime zonedDateTime, Runnable runnable) {
        return createTimer(null, zonedDateTime, runnable);
    }

    @Override
    public Timer createTimer(@Nullable String identifier, ZonedDateTime zonedDateTime, Runnable runnable) {
        return new TimerImpl(scheduler, zonedDateTime, runnable::run, identifier);
    }

    @Override
    public Timer createTimerWithArgument(ZonedDateTime zonedDateTime, Object arg1, Consumer<Object> consumer) {
        return createTimerWithArgument(null, zonedDateTime, arg1, consumer);
    }

    @Override
    public Timer createTimerWithArgument(@Nullable String identifier, ZonedDateTime zonedDateTime, Object arg1,
            Consumer<Object> consumer) {
        return new TimerImpl(scheduler, zonedDateTime, () -> consumer.accept(arg1), identifier);
    }
}
