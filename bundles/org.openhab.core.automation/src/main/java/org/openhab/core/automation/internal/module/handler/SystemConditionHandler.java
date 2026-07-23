/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.handler.BaseConditionModuleHandler;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.service.StartLevelService;

/**
 * This is a ModuleHandler implementation for system state conditions.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class SystemConditionHandler extends BaseConditionModuleHandler {

    public static final String STARTLEVEL_MODULE_TYPE_ID = "core.SystemStartlevelCondition";
    public static final String CFG_MIN_STARTLEVEL = "minStartlevel";

    /**
     * The minimum startlevel.
     */
    private final int minStartlevel;
    private final StartLevelService startLevelService;

    public SystemConditionHandler(Condition condition, StartLevelService startLevelService) {
        super(condition);
        this.startLevelService = startLevelService;
        Configuration configuration = module.getConfiguration();
        this.minStartlevel = ((Number) configuration.get(CFG_MIN_STARTLEVEL)).intValue();
    }

    @Override
    public boolean isSatisfied(Map<String, Object> inputs) {
        return startLevelService.getStartLevel() >= minStartlevel;
    }
}
