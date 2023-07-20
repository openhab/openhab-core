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
package org.openhab.core.automation.internal.module.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.TriggerHandlerCallback;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.Event;
import org.openhab.core.events.system.SystemEventFactory;
import org.openhab.core.service.StartLevelService;
import org.osgi.framework.BundleContext;

/**
 * The {@link SystemTriggerHandlerTest} contains tests for the {@link SystemTriggerHandler}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SystemTriggerHandlerTest {
    private static final int CFG_STARTLEVEL = 80;

    private @Mock @NonNullByDefault({}) BundleContext bundleContextMock;
    private @Mock @NonNullByDefault({}) StartLevelService startLevelServiceMock;
    private @Mock @NonNullByDefault({}) TriggerHandlerCallback callbackMock;

    private @Mock @NonNullByDefault({}) Trigger triggerMock;

    @BeforeEach
    public void setup() {
        when(triggerMock.getConfiguration())
                .thenReturn(new Configuration(Map.of(SystemTriggerHandler.CFG_STARTLEVEL, CFG_STARTLEVEL)));
        when(triggerMock.getTypeUID()).thenReturn(SystemTriggerHandler.STARTLEVEL_MODULE_TYPE_ID);
    }

    @Test
    public void testDoesNotTriggerIfStartLevelTooLow() {
        when(startLevelServiceMock.getStartLevel()).thenReturn(0);

        SystemTriggerHandler triggerHandler = new SystemTriggerHandler(triggerMock, bundleContextMock);
        triggerHandler.setCallback(callbackMock);

        verifyNoInteractions(callbackMock);
    }

    @Test
    public void testDoesNotTriggerIfStartLevelEventLower() {
        when(startLevelServiceMock.getStartLevel()).thenReturn(0);

        SystemTriggerHandler triggerHandler = new SystemTriggerHandler(triggerMock, bundleContextMock);
        triggerHandler.setCallback(callbackMock);

        Event event = SystemEventFactory.createStartlevelEvent(70);
        triggerHandler.receive(event);

        verifyNoInteractions(callbackMock);
    }

    @Test
    public void testDoesTriggerIfStartLevelEventHigher() {
        when(startLevelServiceMock.getStartLevel()).thenReturn(0);

        SystemTriggerHandler triggerHandler = new SystemTriggerHandler(triggerMock, bundleContextMock);
        triggerHandler.setCallback(callbackMock);

        Event event = SystemEventFactory.createStartlevelEvent(100);
        triggerHandler.receive(event);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(callbackMock).triggered(eq(triggerMock), captor.capture());

        Map<String, Object> configuration = (Map<String, Object>) captor.getValue();
        assertThat(configuration.get(SystemTriggerHandler.OUT_STARTLEVEL), is(CFG_STARTLEVEL));
    }

    @Test
    public void testDoesNotTriggerAfterEventTrigger() {
        when(startLevelServiceMock.getStartLevel()).thenReturn(0);

        SystemTriggerHandler triggerHandler = new SystemTriggerHandler(triggerMock, bundleContextMock);
        triggerHandler.setCallback(callbackMock);

        Event event = SystemEventFactory.createStartlevelEvent(100);
        triggerHandler.receive(event);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(callbackMock).triggered(eq(triggerMock), captor.capture());

        Map<String, Object> configuration = (Map<String, Object>) captor.getValue();
        assertThat(configuration.get(SystemTriggerHandler.OUT_STARTLEVEL), is(CFG_STARTLEVEL));

        triggerHandler.receive(event);

        verifyNoMoreInteractions(callbackMock);
    }
}
