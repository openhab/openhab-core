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
package org.eclipse.smarthome.automation.sample.moduletype.demo.internal.factory;

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
import org.eclipse.smarthome.automation.handler.ModuleHandlerFactory;
import org.eclipse.smarthome.automation.sample.moduletype.demo.internal.handlers.CompareCondition;
import org.eclipse.smarthome.automation.sample.moduletype.demo.internal.handlers.ConsolePrintAction;
import org.eclipse.smarthome.automation.sample.moduletype.demo.internal.handlers.ConsoleTrigger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a factory for creating {@link ConsoleTrigger}, {@link CompareCondition} and {@link ConsolePrintAction}
 * objects.
 *
 * @author Plamen Peev - Initial contribution
 */
@Component(immediate = true, service = ModuleHandlerFactory.class)
public class HandlerFactory extends BaseModuleHandlerFactory implements ModuleHandlerFactory {

    /**
     * This field contains the name of this factory
     */
    public static final String MODULE_HANDLER_FACTORY_NAME = "[SampleDemoFactory]";

    /**
     * This field contains the types that are supported by this factory.
     */
    private static final Collection<String> TYPES;

    /**
     * For error logging if there is a query for a type that is not supported.
     */
    private static final Logger LOGGER;

    /**
     * This blocks fills the Collection ,which contains the types supported by this factory, with supported types and
     * creates a Logger instance for logging errors occurred during the handler creation.
     */
    static {
        final List<String> temp = new ArrayList<String>();
        temp.add(CompareCondition.UID);
        temp.add(ConsoleTrigger.UID);
        temp.add(ConsolePrintAction.UID);
        TYPES = Collections.unmodifiableCollection(temp);

        LOGGER = LoggerFactory.getLogger(HandlerFactory.class);
    }

    private BundleContext bundleContext;

    /**
     * This method must deliver the correct handler if this factory can create it or log an error otherwise.
     * It recognises the correct type by {@link Module}'s UID.
     */
    @Override
    protected ModuleHandler internalCreate(Module module, String ruleUID) {
        if (CompareCondition.UID.equals(module.getTypeUID())) {
            return new CompareCondition((Condition) module);
        } else if (ConsolePrintAction.UID.equals(module.getTypeUID())) {
            return new ConsolePrintAction((Action) module, ruleUID);
        } else if (ConsoleTrigger.UID.equals(module.getTypeUID())) {
            return new ConsoleTrigger((Trigger) module, bundleContext);
        } else {
            LOGGER.error(MODULE_HANDLER_FACTORY_NAME + "Not supported moduleHandler: {}", module.getTypeUID());
        }

        return null;
    }

    /**
     * Returns a {@link Collection} that contains the UIDs of the module types for which this factory can create
     * handlers.
     */
    @Override
    public Collection<String> getTypes() {
        return TYPES;
    }

    /**
     * This method is called when all of the services required by this factory are available.
     *
     * @param bundleContext - the {@link ComponentContext} of the HandlerFactory component.
     */
    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * This method is called when a service that is required from this factory becomes unavailable.
     */
    @Deactivate
    @Override
    protected void deactivate() {
        super.deactivate();
    }
}
