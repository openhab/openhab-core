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

import java.time.ZonedDateTime;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.handler.BaseModuleHandler;
import org.openhab.core.automation.handler.ConditionHandler;
import org.openhab.core.automation.internal.module.config.EphemerisConditionConfig;
import org.openhab.core.ephemeris.EphemerisManager;

/**
 * ConditionHandler implementation for Ephemeris based conditions.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class EphemerisConditionHandler extends BaseModuleHandler<Condition> implements ConditionHandler {

    public static final String HOLIDAY_MODULE_TYPE_ID = "ephemeris.HolidayCondition";
    public static final String WEEKEND_MODULE_TYPE_ID = "ephemeris.WeekendCondition";
    public static final String WEEKDAY_MODULE_TYPE_ID = "ephemeris.WeekdayCondition";
    public static final String DAYSET_MODULE_TYPE_ID = "ephemeris.DaysetCondition";

    private final EphemerisManager ephemerisManager;
    private final @Nullable String dayset;
    private final ZonedDateTime target;

    public EphemerisConditionHandler(Condition condition, EphemerisManager ephemerisManager) {
        super(condition);
        this.ephemerisManager = ephemerisManager;

        EphemerisConditionConfig config = getConfigAs(EphemerisConditionConfig.class);
        dayset = DAYSET_MODULE_TYPE_ID.equals(module.getTypeUID())
                ? getValidStringConfigParameter(config.dayset, module.getId())
                : null;
        target = ZonedDateTime.now().plusDays(config.offset);
    }

    private static String getValidStringConfigParameter(@Nullable String value, String moduleId) {
        if (value != null && !value.trim().isEmpty()) {
            return value;
        } else {
            throw new IllegalArgumentException(String
                    .format("Config parameter 'dayset' is missing in the configuration of module '%s'.", moduleId));
        }
    }

    @Override
    public boolean isSatisfied(Map<String, Object> inputs) {
        switch (module.getTypeUID()) {
            case HOLIDAY_MODULE_TYPE_ID:
                return ephemerisManager.isBankHoliday(target);
            case WEEKEND_MODULE_TYPE_ID:
                return ephemerisManager.isWeekend(target);
            case WEEKDAY_MODULE_TYPE_ID:
                return !ephemerisManager.isWeekend(target);
            case DAYSET_MODULE_TYPE_ID:
                final String dayset = this.dayset;
                if (dayset != null) {
                    return ephemerisManager.isInDayset(dayset, target);
                }
                break;
        }
        // If none of these conditions apply false is returned.
        return false;
    }
}
