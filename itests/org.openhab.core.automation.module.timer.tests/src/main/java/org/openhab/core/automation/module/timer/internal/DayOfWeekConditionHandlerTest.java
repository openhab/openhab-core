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
package org.openhab.core.automation.module.timer.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import org.junit.Test;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.internal.module.handler.DayOfWeekConditionHandler;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.config.core.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests the dayOfWeek Condition.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Dominik Schlierf - added extension of BasicConditionHandlerTest
 */
public class DayOfWeekConditionHandlerTest extends BasicConditionHandlerTest {

    private final Logger logger = LoggerFactory.getLogger(DayOfWeekConditionHandlerTest.class);
    private SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.ENGLISH);
    private String dayOfWeek = sdf.format(Date.from(ZonedDateTime.now().toInstant())).toUpperCase();

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

        assertThat(handler.isSatisfied(Collections.emptyMap()), is(true));

        condition = ModuleBuilder.createCondition(condition)
                .withConfiguration(new Configuration(Collections.singletonMap("days", Collections.emptyList())))
                .build();
        handler = new DayOfWeekConditionHandler(condition);
        assertThat(handler.isSatisfied(Collections.emptyMap()), is(false));

        condition = ModuleBuilder.createCondition(condition)
                .withConfiguration(
                        new Configuration(Collections.singletonMap("days", Collections.singletonList(dayOfWeek))))
                .build();
        handler = new DayOfWeekConditionHandler(condition);
        assertThat(handler.isSatisfied(Collections.emptyMap()), is(true));
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
