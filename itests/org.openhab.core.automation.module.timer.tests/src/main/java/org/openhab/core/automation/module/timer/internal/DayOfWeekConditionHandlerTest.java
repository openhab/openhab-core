/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.internal.module.factory.CoreModuleHandlerFactory;
import org.openhab.core.automation.internal.module.handler.DayOfWeekConditionHandler;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.service.StartLevelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests the dayOfWeek Condition.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Dominik Schlierf - added extension of BasicConditionHandlerTest
 */
@NonNullByDefault
public class DayOfWeekConditionHandlerTest extends BasicConditionHandlerTest {

    private final Logger logger = LoggerFactory.getLogger(DayOfWeekConditionHandlerTest.class);
    private SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.ENGLISH);
    private String dayOfWeek = sdf.format(Date.from(ZonedDateTime.now().toInstant())).toUpperCase();

    public DayOfWeekConditionHandlerTest() {
        logger.info("Today is {}", dayOfWeek);
    }

    @BeforeEach
    public void before() {
        EventPublisher eventPublisher = Objects.requireNonNull(getService(EventPublisher.class));
        ItemRegistry itemRegistry = Objects.requireNonNull(getService(ItemRegistry.class));
        CoreModuleHandlerFactory coreModuleHandlerFactory = new CoreModuleHandlerFactory(getBundleContext(),
                eventPublisher, itemRegistry, mock(TimeZoneProvider.class), mock(StartLevelService.class));
        mock(CoreModuleHandlerFactory.class);
        registerService(coreModuleHandlerFactory);
    }

    @Test
    public void assertThatConditionWorks() {
        Configuration conditionConfiguration = new Configuration(
                Map.of("days", List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")));
        Condition condition = ModuleBuilder.createCondition().withId("id")
                .withTypeUID(DayOfWeekConditionHandler.MODULE_TYPE_ID).withConfiguration(conditionConfiguration)
                .build();
        DayOfWeekConditionHandler handler = new DayOfWeekConditionHandler(condition);

        assertThat(handler.isSatisfied(Map.of()), is(true));

        condition = ModuleBuilder.createCondition(condition)
                .withConfiguration(new Configuration(Map.of("days", List.of()))).build();
        handler = new DayOfWeekConditionHandler(condition);
        assertThat(handler.isSatisfied(Map.of()), is(false));

        condition = ModuleBuilder.createCondition(condition)
                .withConfiguration(new Configuration(Map.of("days", List.of(dayOfWeek)))).build();
        handler = new DayOfWeekConditionHandler(condition);
        assertThat(handler.isSatisfied(Map.of()), is(true));
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
        Configuration conditionConfig = new Configuration(Map.of("days", dayOfWeek));
        return ModuleBuilder.createCondition().withId("MyDOWCondition")
                .withTypeUID(DayOfWeekConditionHandler.MODULE_TYPE_ID).withConfiguration(conditionConfig).build();
    }

    @Override
    protected Configuration getFailingConfiguration() {
        return new Configuration(Map.of("days", List.of()));
    }
}
