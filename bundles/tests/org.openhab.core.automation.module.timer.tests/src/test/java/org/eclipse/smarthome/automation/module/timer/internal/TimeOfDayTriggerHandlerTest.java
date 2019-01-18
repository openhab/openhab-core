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

import org.eclipse.smarthome.automation.RuleRegistry;
import org.eclipse.smarthome.automation.module.timer.handler.TimeOfDayTriggerHandler;
import org.eclipse.smarthome.automation.type.ModuleTypeRegistry;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.eclipse.smarthome.test.storage.VolatileStorageService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this tests the timeOfDay trigger
 *
 * @author Kai Kreuzer - initial contribution
 *
 */
public class TimeOfDayTriggerHandlerTest extends JavaOSGiTest {

    final Logger logger = LoggerFactory.getLogger(RuntimeRuleTest.class);
    VolatileStorageService volatileStorageService = new VolatileStorageService();
    RuleRegistry ruleRegistry;

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
