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
package org.openhab.core.automation.internal.module.handler;

import java.text.MessageFormat;

import org.openhab.core.automation.ModuleHandlerCallback;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.BaseTriggerModuleHandler;
import org.openhab.core.automation.handler.TriggerHandlerCallback;
import org.openhab.core.scheduler.CronScheduler;
import org.openhab.core.scheduler.ScheduledCompletableFuture;
import org.openhab.core.scheduler.SchedulerRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an ModuleHandler implementation for Triggers which trigger the rule
 * at a specific time (format 'hh:mm').
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class TimeOfDayTriggerHandler extends BaseTriggerModuleHandler implements SchedulerRunnable {

    private final Logger logger = LoggerFactory.getLogger(TimeOfDayTriggerHandler.class);

    public static final String MODULE_TYPE_ID = "timer.TimeOfDayTrigger";
    public static final String MODULE_CONTEXT_NAME = "MODULE";

    public static final String CFG_TIME = "time";

    private final CronScheduler scheduler;
    private final String expression;
    private ScheduledCompletableFuture<Void> schedule;

    public TimeOfDayTriggerHandler(Trigger module, CronScheduler scheduler) {
        super(module);
        this.scheduler = scheduler;
        String time = module.getConfiguration().get(CFG_TIME).toString();
        try {
            String[] parts = time.split(":");
            expression = MessageFormat.format("0 {1} {0} * * *", Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]));
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            throw new IllegalArgumentException("'time' parameter '" + time + "' is not in valid format 'hh:mm'.", e);
        }
    }

    @Override
    public synchronized void setCallback(ModuleHandlerCallback callback) {
        super.setCallback(callback);
        scheduleJob();
    }

    private void scheduleJob() {
        schedule = scheduler.schedule(this, expression);
        logger.debug("Scheduled job for trigger '{}' at '{}' each day.", module.getId(),
                module.getConfiguration().get(CFG_TIME));
    }

    @Override
    public void run() {
        ((TriggerHandlerCallback) callback).triggered(module, null);
    }

    @Override
    public synchronized void dispose() {
        super.dispose();
        if (schedule != null) {
            schedule.cancel(true);
            logger.debug("cancelled job for trigger '{}'.", module.getId());
        }
    }
}
