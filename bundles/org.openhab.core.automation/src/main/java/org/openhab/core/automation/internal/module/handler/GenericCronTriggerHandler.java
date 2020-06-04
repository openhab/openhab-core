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
 * based on a cron expression. The cron expression can be set with the
 * configuration.
 *
 * @author Christoph Knauf - Initial contribution
 * @author Yordan Mihaylov - Remove Quarz lib dependency
 */
public class GenericCronTriggerHandler extends BaseTriggerModuleHandler implements SchedulerRunnable {

    private final Logger logger = LoggerFactory.getLogger(GenericCronTriggerHandler.class);

    public static final String MODULE_TYPE_ID = "timer.GenericCronTrigger";
    public static final String CALLBACK_CONTEXT_NAME = "CALLBACK";
    public static final String MODULE_CONTEXT_NAME = "MODULE";

    private static final String CFG_CRON_EXPRESSION = "cronExpression";
    private final CronScheduler scheduler;
    private final String expression;
    private ScheduledCompletableFuture<?> schedule;

    public GenericCronTriggerHandler(Trigger module, CronScheduler scheduler) {
        super(module);
        this.scheduler = scheduler;
        this.expression = (String) module.getConfiguration().get(CFG_CRON_EXPRESSION);
    }

    @Override
    public synchronized void setCallback(ModuleHandlerCallback callback) {
        super.setCallback(callback);
        scheduleJob();
    }

    private void scheduleJob() {
        schedule = scheduler.schedule(this, expression);
        logger.debug("Scheduled cron job '{}' for trigger '{}'.", module.getConfiguration().get(CFG_CRON_EXPRESSION),
                module.getId());
    }

    @Override
    public synchronized void dispose() {
        super.dispose();
        if (schedule != null) {
            schedule.cancel(true);
            logger.debug("cancelled job for trigger '{}'.", module.getId());
        }
    }

    @Override
    public void run() {
        if (callback != null) {
            ((TriggerHandlerCallback) callback).triggered(module, null);
        } else {
            logger.debug("Tried to trigger, but callback isn't available!");
        }
    }
}
