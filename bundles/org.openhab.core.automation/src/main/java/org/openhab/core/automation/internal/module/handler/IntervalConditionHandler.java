/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.math.BigDecimal;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.handler.BaseConditionModuleHandler;
import org.openhab.core.config.core.Configuration;

/**
 * ConditionHandler implementation for trigger interval limiting.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class IntervalConditionHandler extends BaseConditionModuleHandler {

    public static final String MODULE_TYPE_ID = "timer.IntervalCondition";

    /**
     * Constants for Config-Parameters corresponding to Definition in
     * IntervalConditionHandler.json
     */
    public static final String CFG_MIN_INTERVAL = "minInterval";

    /**
     * The minimum interval stored in nano seconds.
     */
    private long minInterval;

    private @Nullable Long lastAcceptedTime = null;

    public IntervalConditionHandler(Condition condition) {
        super(condition);
        Configuration configuration = module.getConfiguration();
        this.minInterval = ((BigDecimal) configuration.get(CFG_MIN_INTERVAL)).longValue() * 1000000L;
    }

    @Override
    public boolean isSatisfied(Map<String, Object> inputs) {
        long currentTime = System.nanoTime();
        if (lastAcceptedTime == null || currentTime - lastAcceptedTime >= minInterval) {
            lastAcceptedTime = currentTime;
            return true;
        }
        return false;
    }
}
