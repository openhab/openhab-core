/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.module.handler;

import static org.mockito.Mockito.when;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.automation.Trigger;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.test.java.JavaTest;
import org.osgi.framework.BundleContext;

/**
 * Test cases for {@link GroupStateTriggerHandler}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
class GroupStateTriggerHandlerTest extends JavaTest {
    private @Mock @NonNullByDefault({}) Trigger moduleMock;
    private @Mock @NonNullByDefault({}) BundleContext contextMock;
    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;

    @Test
    public void testWarningLoggedWhenConfigurationInvalid() {
        when(moduleMock.getConfiguration()).thenReturn(new Configuration());
        when(moduleMock.getId()).thenReturn("triggerId");
        setupInterceptedLogger(GroupStateTriggerHandler.class, LogLevel.WARN);

        GroupStateTriggerHandler handler = new GroupStateTriggerHandler(moduleMock, "ruleId", contextMock,
                itemRegistryMock);

        stopInterceptedLogger(GroupStateTriggerHandler.class);

        assertLogMessage(GroupStateTriggerHandler.class, LogLevel.WARN,
                "GroupStateTrigger triggerId of rule ruleId has no groupName configured and will not work.");
    }

    @Test
    public void testNoWarningLoggedWhenConfigurationValid() {
        when(moduleMock.getConfiguration())
                .thenReturn(new Configuration(Map.of(GroupStateTriggerHandler.CFG_GROUPNAME, "name")));
        when(moduleMock.getId()).thenReturn("triggerId");
        setupInterceptedLogger(GroupStateTriggerHandler.class, LogLevel.WARN);

        GroupStateTriggerHandler handler = new GroupStateTriggerHandler(moduleMock, "ruleId", contextMock,
                itemRegistryMock);

        stopInterceptedLogger(GroupStateTriggerHandler.class);

        assertLogMessage(GroupStateTriggerHandler.class, LogLevel.WARN,
                "Group 'name' needed for rule 'ruleId' is missing. Trigger 'triggerId' will not work.");
    }
}
