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
package org.eclipse.smarthome.automation.internal.sample.json.internal.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.BaseModuleHandlerFactory;
import org.eclipse.smarthome.automation.handler.ModuleHandler;
import org.eclipse.smarthome.automation.handler.TriggerHandler;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module Handler Factory Sample implementation
 *
 * @author Vasil Ilchev - Initial Contribution
 * @author Kai Kreuzer - refactored and simplified customized module handling
 */
public class SampleHandlerFactory extends BaseModuleHandlerFactory {
    public static final String SUPPORTED_TRIGGER = "SampleTrigger";
    public static final String SUPPORTED_CONDITION = "SampleCondition";
    public static final String SUPPORTED_ACTION = "SampleAction";
    public static final String MODULE_HANDLER_FACTORY_NAME = "[SampleHandlerFactory]";
    private final Logger logger = LoggerFactory.getLogger(SampleHandlerFactory.class);
    private static final Collection<String> TYPES;
    private final List<TriggerHandler> createdTriggerHandler = new ArrayList<TriggerHandler>(10);

    static {
        List<String> temp = new ArrayList<String>();
        temp.add(SUPPORTED_TRIGGER);
        temp.add(SUPPORTED_CONDITION);
        temp.add(SUPPORTED_ACTION);
        TYPES = Collections.unmodifiableCollection(temp);
    }

    @Override
    @Deactivate
    public void deactivate() {
        createdTriggerHandler.clear();
        super.deactivate();
    }

    @Override
    public Collection<String> getTypes() {
        return TYPES;
    }

    /**
     * Retrieves created TriggerHandlers from this HandlerFactory.
     *
     * @return list of created TriggerHandlers
     */
    public List<TriggerHandler> getCreatedTriggerHandler() {
        return createdTriggerHandler;
    }

    @Override
    protected ModuleHandler internalCreate(Module module, String ruleUID) {
        ModuleHandler moduleHandler = null;
        if (SUPPORTED_TRIGGER.equals(module.getTypeUID())) {
            moduleHandler = new SampleTriggerHandler((Trigger) module, ruleUID);
            createdTriggerHandler.add((TriggerHandler) moduleHandler);
        } else if (SUPPORTED_CONDITION.equals(module.getTypeUID())) {
            moduleHandler = new SampleConditionHandler((Condition) module);
        } else if (SUPPORTED_ACTION.equals(module.getTypeUID())) {
            moduleHandler = new SampleActionHandler((Action) module);
        } else {
            logger.error(MODULE_HANDLER_FACTORY_NAME + "Not supported moduleHandler: {}", module.getTypeUID());
        }
        return moduleHandler;
    }

    @Override
    public void ungetHandler(Module module, String ruleUID, ModuleHandler hdlr) {
        createdTriggerHandler.remove(hdlr);
        super.ungetHandler(module, ruleUID, hdlr);
    }

}
