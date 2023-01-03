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
package org.openhab.core.automation.internal.module.factory;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.handler.ModuleHandler;
import org.openhab.core.automation.internal.module.handler.EphemerisConditionHandler;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.ephemeris.EphemerisManager;

/**
 * Basic test cases for {@link EphemerisModuleHandlerFactory}
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class EphemerisModuleHandlerFactoryTest {

    private @NonNullByDefault({}) EphemerisModuleHandlerFactory factory;
    private @NonNullByDefault({}) Module moduleMock;

    @BeforeEach
    public void setUp() {
        factory = new EphemerisModuleHandlerFactory(mock(EphemerisManager.class));

        moduleMock = mock(Condition.class);
        when(moduleMock.getId()).thenReturn("My id");
    }

    @Test
    public void testFactoryFailsCreatingModuleHandlerForDaysetCondition() {
        when(moduleMock.getTypeUID()).thenReturn(EphemerisConditionHandler.DAYSET_MODULE_TYPE_ID);

        when(moduleMock.getConfiguration()).thenReturn(new Configuration());
        assertThrows(IllegalArgumentException.class, () -> factory.internalCreate(moduleMock, "My first rule"));
    }

    @Test
    public void testFactoryCreatesModuleHandlerForDaysetCondition() {
        when(moduleMock.getTypeUID()).thenReturn(EphemerisConditionHandler.DAYSET_MODULE_TYPE_ID);

        when(moduleMock.getConfiguration()).thenReturn(new Configuration(Map.of("dayset", "school")));
        ModuleHandler handler = factory.internalCreate(moduleMock, "My second rule");
        assertThat(handler, is(notNullValue()));
        assertThat(handler, instanceOf(EphemerisConditionHandler.class));
    }

    @Test
    public void testFactoryCreatesModuleHandlerForWeekdayCondition() {
        when(moduleMock.getTypeUID()).thenReturn(EphemerisConditionHandler.WEEKDAY_MODULE_TYPE_ID);

        when(moduleMock.getConfiguration()).thenReturn(new Configuration());
        ModuleHandler handler = factory.internalCreate(moduleMock, "My first rule");
        assertThat(handler, is(notNullValue()));
        assertThat(handler, instanceOf(EphemerisConditionHandler.class));

        when(moduleMock.getConfiguration()).thenReturn(new Configuration(Map.of("offset", 5)));
        handler = factory.internalCreate(moduleMock, "My second rule");
        assertThat(handler, is(notNullValue()));
        assertThat(handler, instanceOf(EphemerisConditionHandler.class));
    }

    @Test
    public void testFactoryCreatesModuleHandlerForHolidayCondition() {
        when(moduleMock.getTypeUID()).thenReturn(EphemerisConditionHandler.HOLIDAY_MODULE_TYPE_ID);

        when(moduleMock.getConfiguration()).thenReturn(new Configuration());
        ModuleHandler handler = factory.internalCreate(moduleMock, "My first rule");
        assertThat(handler, is(notNullValue()));
        assertThat(handler, instanceOf(EphemerisConditionHandler.class));

        when(moduleMock.getConfiguration()).thenReturn(new Configuration(Map.of("offset", 5)));
        handler = factory.internalCreate(moduleMock, "My second rule");
        assertThat(handler, is(notNullValue()));
        assertThat(handler, instanceOf(EphemerisConditionHandler.class));
    }

    @Test
    public void testFactoryCreatesModuleHandlerForNotHolidayCondition() {
        when(moduleMock.getTypeUID()).thenReturn(EphemerisConditionHandler.NOT_HOLIDAY_MODULE_TYPE_ID);

        when(moduleMock.getConfiguration()).thenReturn(new Configuration());
        ModuleHandler handler = factory.internalCreate(moduleMock, "My first rule");
        assertThat(handler, is(notNullValue()));
        assertThat(handler, instanceOf(EphemerisConditionHandler.class));

        when(moduleMock.getConfiguration()).thenReturn(new Configuration(Map.of("offset", 5)));
        handler = factory.internalCreate(moduleMock, "My second rule");
        assertThat(handler, is(notNullValue()));
        assertThat(handler, instanceOf(EphemerisConditionHandler.class));
    }
}
