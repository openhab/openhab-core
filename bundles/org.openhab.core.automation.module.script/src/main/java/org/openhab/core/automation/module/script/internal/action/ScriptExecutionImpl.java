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
package org.openhab.core.automation.module.script.internal.action;

import java.time.ZonedDateTime;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
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
    private final RuleManager ruleManager;
    private final RuleRegistry ruleRegistry;

    @Activate
    public ScriptExecutionImpl(@Reference RuleRegistry ruleRegistry, @Reference RuleManager ruleManager,
            @Reference Scheduler scheduler) {
        this.ruleRegistry = ruleRegistry;
        this.scheduler = scheduler;
        this.ruleManager = ruleManager;
    }

    @Override
    public Timer createTimer(ZonedDateTime instant, Runnable runnable) {
        return createTimer(null, instant, runnable);
    }

    @Override
    public Timer createTimer(@Nullable String identifier, ZonedDateTime instant, Runnable runnable) {
        return new TimerImpl(scheduler, instant, runnable::run, identifier);
    }

    @Override
    public Timer createTimerWithArgument(ZonedDateTime instant, Object arg1, Consumer<Object> consumer) {
        return createTimerWithArgument(null, instant, arg1, consumer);
    }

    @Override
    public Timer createTimerWithArgument(@Nullable String identifier, ZonedDateTime instant, Object arg1,
            Consumer<Object> consumer) {
        return new TimerImpl(scheduler, instant, () -> consumer.accept(arg1), identifier);
    }
}
