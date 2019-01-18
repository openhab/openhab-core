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

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.handler.BaseModuleHandler;
import org.eclipse.smarthome.automation.handler.ConditionHandler;
import org.eclipse.smarthome.config.core.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConditionHandler implementation for time based conditions.
 *
 * @author Dominik Schlierf - initial contribution
 *
 */
public class TimeOfDayConditionHandler extends BaseModuleHandler<Condition> implements ConditionHandler {

    private final Logger logger = LoggerFactory.getLogger(TimeOfDayConditionHandler.class);

    public static final String MODULE_TYPE_ID = "core.TimeOfDayCondition";

    /**
     * Constants for Config-Parameters corresponding to Definition in
     * TimeOfDayConditionHandler.json
     */
    private static final String START_TIME = "startTime";
    private static final String END_TIME = "endTime";
    /**
     * The start time of the user configured time span.
     */
    private final LocalTime startTime;
    /**
     * The end time of the user configured time span.
     */
    private final LocalTime endTime;

    public TimeOfDayConditionHandler(Condition condition) {
        super(condition);
        Configuration configuration = module.getConfiguration();
        String startTimeConfig = (String) configuration.get(START_TIME);
        String endTimeConfig = (String) configuration.get(END_TIME);
        startTime = startTimeConfig == null ? null : LocalTime.parse(startTimeConfig).truncatedTo(ChronoUnit.MINUTES);
        endTime = endTimeConfig == null ? null : LocalTime.parse(endTimeConfig).truncatedTo(ChronoUnit.MINUTES);
    }

    @Override
    public boolean isSatisfied(Map<String, Object> inputs) {

        if (startTime == null || endTime == null) {
            logger.warn("Time condition with id {} is not well configured: startTime={}  endTime = {}", module.getId(),
                    startTime, endTime);
            return false;
        }

        LocalTime currentTime = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);

        // If the current time equals the start time, the condition is always true.
        if (currentTime.equals(startTime)) {
            logger.debug("Time condition with id {} evaluated, that the current time {} equals the start time: {}",
                    module.getId(), currentTime, startTime);
            return true;
        }
        // If the start time is before the end time, the condition will evaluate as true,
        // if the current time is between the start time and the end time.
        if (startTime.isBefore(endTime)) {
            if (currentTime.isAfter(startTime) && currentTime.isBefore(endTime)) {
                logger.debug("Time condition with id {} evaluated, that {} is between {} and {}.", module.getId(),
                        currentTime, startTime, endTime);
                return true;
            }
        }
        // If the start time is set after the end time, the time values wrap around the midnight mark.
        // So if the start time is 19:00 and the end time is 07:00, the condition will be true from
        // 19:00 to 23:59 and 00:00 to 07:00.
        else if (currentTime.isAfter(LocalTime.MIDNIGHT) && currentTime.isBefore(endTime)
                || currentTime.isAfter(startTime) && currentTime.isBefore(LocalTime.MAX)) {
            logger.debug("Time condition with id {} evaluated, that {} is between {} and {}, or between {} and {}.",
                    module.getId(), currentTime, LocalTime.MIDNIGHT, endTime, startTime,
                    LocalTime.MAX.truncatedTo(ChronoUnit.MINUTES));
            return true;
        }
        // If none of these conditions apply false is returned.
        return false;
    }

}
