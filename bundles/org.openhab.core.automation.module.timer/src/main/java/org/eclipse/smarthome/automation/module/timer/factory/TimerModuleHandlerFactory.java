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
package org.eclipse.smarthome.automation.module.timer.factory;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.BaseModuleHandlerFactory;
import org.eclipse.smarthome.automation.handler.ModuleHandler;
import org.eclipse.smarthome.automation.handler.ModuleHandlerFactory;
import org.eclipse.smarthome.automation.module.timer.handler.DayOfWeekConditionHandler;
import org.eclipse.smarthome.automation.module.timer.handler.GenericCronTriggerHandler;
import org.eclipse.smarthome.automation.module.timer.handler.TimeOfDayConditionHandler;
import org.eclipse.smarthome.automation.module.timer.handler.TimeOfDayTriggerHandler;
import org.eclipse.smarthome.core.scheduler.CronScheduler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This HandlerFactory creates TimerTriggerHandlers to control items within the
 * RuleManager.
 *
 * @author Christoph Knauf - initial contribution
 * @author Kai Kreuzer - added new module types
 *
 */
@Component(immediate = true, service = ModuleHandlerFactory.class)
public class TimerModuleHandlerFactory extends BaseModuleHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(TimerModuleHandlerFactory.class);

    public static final String THREADPOOLNAME = "ruletimer";
    private static final Collection<String> TYPES = Arrays
            .asList(new String[] { GenericCronTriggerHandler.MODULE_TYPE_ID, TimeOfDayTriggerHandler.MODULE_TYPE_ID,
                    TimeOfDayConditionHandler.MODULE_TYPE_ID, DayOfWeekConditionHandler.MODULE_TYPE_ID });

    private CronScheduler scheduler;

    @Override
    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Reference
    protected void setCronScheduler(CronScheduler scheduler) {
        this.scheduler = scheduler;
    }

    protected void unsetCronScheduler(CronScheduler scheduler) {
        this.scheduler = null;
    }

    @Override
    public Collection<String> getTypes() {
        return TYPES;
    }

    @Override
    protected ModuleHandler internalCreate(Module module, String ruleUID) {
        logger.trace("create {} -> {}", module.getId(), module.getTypeUID());
        String moduleTypeUID = module.getTypeUID();
        if (GenericCronTriggerHandler.MODULE_TYPE_ID.equals(moduleTypeUID) && module instanceof Trigger) {
            return new GenericCronTriggerHandler((Trigger) module, scheduler);
        } else if (TimeOfDayTriggerHandler.MODULE_TYPE_ID.equals(moduleTypeUID) && module instanceof Trigger) {
            return new TimeOfDayTriggerHandler((Trigger) module, scheduler);
        } else if (TimeOfDayConditionHandler.MODULE_TYPE_ID.equals(moduleTypeUID) && module instanceof Condition) {
            return new TimeOfDayConditionHandler((Condition) module);
        } else if (DayOfWeekConditionHandler.MODULE_TYPE_ID.equals(moduleTypeUID) && module instanceof Condition) {
            return new DayOfWeekConditionHandler((Condition) module);
        } else {
            logger.error("The module handler type '{}' is not supported.", moduleTypeUID);
        }
        return null;
    }
}
