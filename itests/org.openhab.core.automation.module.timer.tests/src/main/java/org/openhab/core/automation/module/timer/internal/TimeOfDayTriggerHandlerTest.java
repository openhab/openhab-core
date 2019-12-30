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

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.internal.module.handler.TimeOfDayTriggerHandler;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.test.storage.VolatileStorageService;

/**
 * this tests the timeOfDay trigger
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class TimeOfDayTriggerHandlerTest extends JavaOSGiTest {

    private VolatileStorageService volatileStorageService = new VolatileStorageService();
    private RuleRegistry ruleRegistry;

    public TimeOfDayTriggerHandlerTest() {
    }

    @Before
    public void before() {
        registerService(volatileStorageService);
        waitForAssert(() -> {
            ruleRegistry = getService(RuleRegistry.class);
            assertThat(ruleRegistry, is(notNullValue()));
        }, 3000, 100);
    }

    @Test
    public void checkIfModuleTypeIsRegistered() {
        ModuleTypeRegistry mtr = getService(ModuleTypeRegistry.class);
        waitForAssert(() -> {
            assertThat(mtr.get(TimeOfDayTriggerHandler.MODULE_TYPE_ID), is(notNullValue()));
        }, 3000, 100);
    }
}
