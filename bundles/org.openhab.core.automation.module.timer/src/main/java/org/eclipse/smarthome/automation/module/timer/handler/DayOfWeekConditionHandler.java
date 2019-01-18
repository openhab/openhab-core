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
package org.eclipse.smarthome.automation.module.timer.handler;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.handler.BaseModuleHandler;
import org.eclipse.smarthome.automation.handler.ConditionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a ConditionHandler implementation, which checks the current day of the week against a specified list.
 *
 * @author Kai Kreuzer - Initial Contribution
 *
 */
public class DayOfWeekConditionHandler extends BaseModuleHandler<Condition> implements ConditionHandler {

    private final Logger logger = LoggerFactory.getLogger(DayOfWeekConditionHandler.class);

    public static final String MODULE_TYPE_ID = "timer.DayOfWeekCondition";
    public static final String MODULE_CONTEXT_NAME = "MODULE";

    private static final String CFG_DAYS = "days";

    private final Set<Integer> days;

    @SuppressWarnings("unchecked")
    public DayOfWeekConditionHandler(Condition module) {
        super(module);
        try {
            days = new HashSet<>();
            for (String day : (Iterable<String>) module.getConfiguration().get(CFG_DAYS)) {
                switch (day.toUpperCase()) {
                    case "SUN":
                        days.add(Calendar.SUNDAY);
                        break;
                    case "MON":
                        days.add(Calendar.MONDAY);
                        break;
                    case "TUE":
                        days.add(Calendar.TUESDAY);
                        break;
                    case "WED":
                        days.add(Calendar.WEDNESDAY);
                        break;
                    case "THU":
                        days.add(Calendar.THURSDAY);
                        break;
                    case "FRI":
                        days.add(Calendar.FRIDAY);
                        break;
                    case "SAT":
                        days.add(Calendar.SATURDAY);
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
        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return days.contains(dow);
    }
}
