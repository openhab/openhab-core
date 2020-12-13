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
package org.openhab.core.automation.internal.module.factory;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.handler.BaseModuleHandlerFactory;
import org.openhab.core.automation.handler.ModuleHandler;
import org.openhab.core.automation.handler.ModuleHandlerFactory;
import org.openhab.core.automation.internal.module.handler.EphemerisConditionHandler;
import org.openhab.core.ephemeris.EphemerisManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This HandlerFactory creates ModuleHandlers to control items within the RuleManager. It contains basic Ephemeris
 * Conditions.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@Component
@NonNullByDefault
public class EphemerisModuleHandlerFactory extends BaseModuleHandlerFactory implements ModuleHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(EphemerisModuleHandlerFactory.class);

    private static final Collection<String> TYPES = List.of(EphemerisConditionHandler.HOLIDAY_MODULE_TYPE_ID,
            EphemerisConditionHandler.WEEKEND_MODULE_TYPE_ID, EphemerisConditionHandler.WEEKDAY_MODULE_TYPE_ID,
            EphemerisConditionHandler.DAYSET_MODULE_TYPE_ID);

    private final EphemerisManager ephemerisManager;

    @Activate
    public EphemerisModuleHandlerFactory(final @Reference EphemerisManager ephemerisManager) {
        this.ephemerisManager = ephemerisManager;
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public Collection<String> getTypes() {
        return TYPES;
    }

    @Override
    protected @Nullable ModuleHandler internalCreate(final Module module, final String ruleUID) {
        final String moduleTypeUID = module.getTypeUID();
        logger.trace("create {} -> {} : {}", module.getId(), moduleTypeUID, ruleUID);

        if (module instanceof Condition) {
            switch (moduleTypeUID) {
                case EphemerisConditionHandler.HOLIDAY_MODULE_TYPE_ID:
                case EphemerisConditionHandler.WEEKEND_MODULE_TYPE_ID:
                case EphemerisConditionHandler.WEEKDAY_MODULE_TYPE_ID:
                case EphemerisConditionHandler.DAYSET_MODULE_TYPE_ID:
                    return new EphemerisConditionHandler((Condition) module, ephemerisManager);
            }
        }

        logger.error("The ModuleHandler is not supported: {}", moduleTypeUID);
        return null;
    }
}
