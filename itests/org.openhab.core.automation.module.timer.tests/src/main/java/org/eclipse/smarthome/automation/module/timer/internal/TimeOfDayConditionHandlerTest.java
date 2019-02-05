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
package org.eclipse.smarthome.automation.module.timer.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.core.util.ModuleBuilder;
import org.eclipse.smarthome.automation.module.timer.handler.DayOfWeekConditionHandler;
import org.eclipse.smarthome.automation.module.timer.handler.TimeOfDayConditionHandler;
import org.eclipse.smarthome.automation.type.ModuleTypeRegistry;
import org.eclipse.smarthome.config.core.Configuration;
import org.junit.Test;

/**
 * This tests the TimeOfDay Condition.
 *
 * @author Dominik Schlierf - initial contribution
 *
 */
public class TimeOfDayConditionHandlerTest extends BasicConditionHandlerTest {

    public TimeOfDayConditionHandlerTest() {
    }

    /**
     * This checks if the condition on its own works properly.
     */
    @Test
    public void assertThatConditionWorks() {
        LocalTime currentTime = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalTime beforeCurrentTime = currentTime.minus(Duration.ofMinutes(2));
        LocalTime afterCurrentTime = currentTime.plus(Duration.ofMinutes(2));

        // Time is between start and end time -> should return true.
        TimeOfDayConditionHandler handler = getTimeOfDayConditionHandler(beforeCurrentTime.toString(),
                afterCurrentTime.toString());
        assertThat(handler.isSatisfied(null), is(true));

        // Time is equal to start time -> should return true
        handler = getTimeOfDayConditionHandler(currentTime.toString(), afterCurrentTime.toString());
        assertThat(handler.isSatisfied(null), is(true));

        // Time is equal to end time -> should return false
        handler = getTimeOfDayConditionHandler(beforeCurrentTime.toString(), currentTime.toString());
        assertThat(handler.isSatisfied(null), is(false));

        // Start value is in the future & end value is in the past
        // -> should return false
        handler = getTimeOfDayConditionHandler(afterCurrentTime.toString(), beforeCurrentTime.toString());
        assertThat(handler.isSatisfied(null), is(false));

        // Start & end time are in the future & start time is after the end time
        // -> should return true
        handler = getTimeOfDayConditionHandler(afterCurrentTime.plus(Duration.ofMinutes(2)).toString(),
                afterCurrentTime.toString());
        assertThat(handler.isSatisfied(null), is(true));
    }

    private TimeOfDayConditionHandler getTimeOfDayConditionHandler(String startTime, String endTime) {
        TimeOfDayConditionHandler handler = new TimeOfDayConditionHandler(getTimeCondition(startTime, endTime));
        return handler;
    }

    private Condition getTimeCondition(String startTime, String endTime) {
        Configuration timeConfig = getTimeConfiguration(startTime, endTime);
        Condition condition = ModuleBuilder.createCondition().withId("testTimeOfDayCondition")
                .withTypeUID(TimeOfDayConditionHandler.MODULE_TYPE_ID).withConfiguration(timeConfig).build();
        return condition;
    }

    private Configuration getTimeConfiguration(String startTime, String endTime) {
        Map<String, Object> timeMap = new HashMap<>();
        timeMap.put("startTime", startTime);
        timeMap.put("endTime", endTime);
        Configuration timeConfig = new Configuration(timeMap);
        return timeConfig;
    }

    @Override
    public Condition getPassingCondition() {
        LocalTime currentTime = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalTime beforeCurrentTime = currentTime.minus(Duration.ofMinutes(2));
        LocalTime afterCurrentTime = currentTime.plus(Duration.ofMinutes(2));
        return getTimeCondition(beforeCurrentTime.toString(), afterCurrentTime.toString());
    }

    @Override
    public Configuration getFailingConfiguration() {
        LocalTime currentTime = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalTime beforeCurrentTime = currentTime.minus(Duration.ofMinutes(2));
        LocalTime afterCurrentTime = currentTime.plus(Duration.ofMinutes(2));
        return getTimeConfiguration(afterCurrentTime.toString(), beforeCurrentTime.toString());
    }

    @SuppressWarnings("null")
    @Test
    public void checkIfModuleTypeIsRegistered() {
        ModuleTypeRegistry mtr = getService(ModuleTypeRegistry.class);
        waitForAssert(() -> {
            assertThat(mtr.get(DayOfWeekConditionHandler.MODULE_TYPE_ID), is(notNullValue()));
        }, 3000, 100);
    }
}