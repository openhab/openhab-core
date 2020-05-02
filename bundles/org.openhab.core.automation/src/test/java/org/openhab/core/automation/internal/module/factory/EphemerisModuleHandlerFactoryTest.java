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
package org.openhab.core.automation.internal.module.factory;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.Before;
import org.junit.Test;
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

    @Before
    public void setUp() {
        factory = new EphemerisModuleHandlerFactory(mock(EphemerisManager.class));

        moduleMock = mock(Condition.class);
        when(moduleMock.getId()).thenReturn("My id");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFactoryFailsCreatingModuleHandlerForDaysetCondition() {
        when(moduleMock.getTypeUID()).thenReturn(EphemerisConditionHandler.DAYSET_MODULE_TYPE_ID);

        when(moduleMock.getConfiguration()).thenReturn(new Configuration());
        ModuleHandler handler = factory.internalCreate(moduleMock, "My first rule");
        assertThat(handler, is(notNullValue()));
        assertThat(handler, instanceOf(EphemerisConditionHandler.class));
    }

    @Test
    public void testFactoryCreatesModuleHandlerForDaysetCondition() {
        when(moduleMock.getTypeUID()).thenReturn(EphemerisConditionHandler.DAYSET_MODULE_TYPE_ID);

        when(moduleMock.getConfiguration()).thenReturn(new Configuration(Collections.singletonMap("dayset", "school")));
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

        when(moduleMock.getConfiguration()).thenReturn(new Configuration(Collections.singletonMap("offset", 5)));
        handler = factory.internalCreate(moduleMock, "My second rule");
        assertThat(handler, is(notNullValue()));
        assertThat(handler, instanceOf(EphemerisConditionHandler.class));
    }
}
