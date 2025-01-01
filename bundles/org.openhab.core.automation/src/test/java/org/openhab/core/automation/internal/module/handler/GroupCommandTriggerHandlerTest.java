/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
 * Test cases for {@link GroupCommandTriggerHandler}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
class GroupCommandTriggerHandlerTest extends JavaTest {
    private @Mock @NonNullByDefault({}) Trigger moduleMock;
    private @Mock @NonNullByDefault({}) BundleContext contextMock;
    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;

    @Test
    public void testWarningLoggedWhenConfigurationInvalid() {
        when(moduleMock.getConfiguration()).thenReturn(new Configuration());
        when(moduleMock.getId()).thenReturn("triggerId");
        setupInterceptedLogger(GroupCommandTriggerHandler.class, LogLevel.WARN);

        GroupCommandTriggerHandler unused = new GroupCommandTriggerHandler(moduleMock, "ruleId", contextMock,
                itemRegistryMock);

        stopInterceptedLogger(GroupCommandTriggerHandler.class);

        assertLogMessage(GroupCommandTriggerHandler.class, LogLevel.WARN,
                "GroupCommandTrigger triggerId of rule ruleId has no groupName configured and will not work.");
    }

    @Test
    public void testNoWarningLoggedWhenConfigurationValid() {
        when(moduleMock.getConfiguration())
                .thenReturn(new Configuration(Map.of(GroupCommandTriggerHandler.CFG_GROUPNAME, "name")));
        when(moduleMock.getId()).thenReturn("triggerId");
        setupInterceptedLogger(GroupCommandTriggerHandler.class, LogLevel.WARN);

        GroupCommandTriggerHandler unused = new GroupCommandTriggerHandler(moduleMock, "ruleId", contextMock,
                itemRegistryMock);

        stopInterceptedLogger(GroupCommandTriggerHandler.class);

        assertLogMessage(GroupCommandTriggerHandler.class, LogLevel.WARN,
                "Group 'name' needed for rule 'ruleId' is missing. Trigger 'triggerId' will not work.");
    }
}
