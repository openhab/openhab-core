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

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openhab.core.automation.Condition;
import org.openhab.core.automation.handler.BaseConditionModuleHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a ConditionHandler implementation, which checks the current day of the week against a specified list.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class DayOfWeekConditionHandler extends BaseConditionModuleHandler {

    public static final String MODULE_TYPE_ID = "timer.DayOfWeekCondition";
    public static final String MODULE_CONTEXT_NAME = "MODULE";

    public static final String CFG_DAYS = "days";

    private final Logger logger = LoggerFactory.getLogger(DayOfWeekConditionHandler.class);

    private final Set<DayOfWeek> days;

    @SuppressWarnings("unchecked")
    public DayOfWeekConditionHandler(Condition module) {
        super(module);
        try {
            days = new HashSet<>();
            for (String day : (Iterable<String>) module.getConfiguration().get(CFG_DAYS)) {
                switch (day.toUpperCase()) {
                    case "SUN":
                        days.add(DayOfWeek.SUNDAY);
                        break;
                    case "MON":
                        days.add(DayOfWeek.MONDAY);
                        break;
                    case "TUE":
                        days.add(DayOfWeek.TUESDAY);
                        break;
                    case "WED":
                        days.add(DayOfWeek.WEDNESDAY);
                        break;
                    case "THU":
                        days.add(DayOfWeek.THURSDAY);
                        break;
                    case "FRI":
                        days.add(DayOfWeek.FRIDAY);
                        break;
                    case "SAT":
                        days.add(DayOfWeek.SATURDAY);
                        break;
                    default:
                        logger.warn("Ignoring illegal weekday '{}'", day);
                        break;
                }
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("'days' parameter must be an array of strings.");
        }
    }

    @Override
    public boolean isSatisfied(Map<String, Object> context) {
        DayOfWeek dow = ZonedDateTime.now().getDayOfWeek();
        return days.contains(dow);
    }
}
