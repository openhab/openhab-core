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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.core.util.ModuleBuilder;
import org.eclipse.smarthome.automation.module.timer.handler.DayOfWeekConditionHandler;
import org.eclipse.smarthome.automation.type.ModuleTypeRegistry;
import org.eclipse.smarthome.config.core.Configuration;
import org.junit.Test;

/**
 * This tests the dayOfWeek Condition.
 *
 * @author Dominik Schlierf - added extension of BasicConditionHandlerTest
 * @author Kai Kreuzer - initial contribution
 *
 */
public class DayOfWeekConditionHandlerTest extends BasicConditionHandlerTest {
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.ENGLISH);
    String dayOfWeek = sdf.format(cal.getTime()).toUpperCase();

    public DayOfWeekConditionHandlerTest() {
        logger.info("Today is {}", dayOfWeek);
    }

    @Test
    public void assertThatConditionWorks() {
        Configuration conditionConfiguration = new Configuration(Collections.singletonMap("days",
                Arrays.asList(new String[] { "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN" })));
        Condition condition = ModuleBuilder.createCondition().withId("id")
                .withTypeUID(DayOfWeekConditionHandler.MODULE_TYPE_ID).withConfiguration(conditionConfiguration)
                .build();
        DayOfWeekConditionHandler handler = new DayOfWeekConditionHandler(condition);

        assertThat(handler.isSatisfied(null), is(true));

        condition = ModuleBuilder.createCondition(condition)
                .withConfiguration(new Configuration(Collections.singletonMap("days", Collections.emptyList())))
                .build();
        handler = new DayOfWeekConditionHandler(condition);
        assertThat(handler.isSatisfied(null), is(false));

        condition = ModuleBuilder.createCondition(condition)
                .withConfiguration(
                        new Configuration(Collections.singletonMap("days", Collections.singletonList(dayOfWeek))))
                .build();
        handler = new DayOfWeekConditionHandler(condition);
        assertThat(handler.isSatisfied(null), is(true));
    }

    @Test
    public void checkIfModuleTypeIsRegistered() {
        ModuleTypeRegistry mtr = getService(ModuleTypeRegistry.class);
        waitForAssert(() -> {
            assertThat(mtr.get(DayOfWeekConditionHandler.MODULE_TYPE_ID), is(notNullValue()));
        }, 3000, 100);
    }

    @Override
    protected Condition getPassingCondition() {
        Configuration conditionConfig = new Configuration(Collections.singletonMap("days", dayOfWeek));
        return ModuleBuilder.createCondition().withId("MyDOWCondition")
                .withTypeUID(DayOfWeekConditionHandler.MODULE_TYPE_ID).withConfiguration(conditionConfig).build();
    }

    @Override
    protected Configuration getFailingConfiguration() {
        return new Configuration(Collections.singletonMap("days", Collections.emptyList()));
    }
}
