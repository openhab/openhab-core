/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.voice.internal.text.interpreter.llm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.voice.text.interpreter.llm.LLMToolException;

/**
 * Test class for {@link DateTimeLLMTool}.
 *
 * @author Florian Hotze - Initial contribution
 */
public class DateTimeLLMToolTest {
    private final TimeZoneProvider timeZoneProvider = mock(TimeZoneProvider.class);

    private DateTimeLLMTool tool;

    @BeforeEach
    public void setUp() {
        when(timeZoneProvider.getTimeZone()).thenReturn(ZoneId.of("UTC"));

        tool = new DateTimeLLMTool(timeZoneProvider);
    }

    @Test
    public void testGetUID() {
        assertNotNull(tool.getUID());
    }

    @Test
    public void testGetLabel() {
        assertNotNull(tool.getLabel(Locale.ENGLISH));
    }

    @Test
    public void testGetDescription() {
        assertNotNull(tool.getDescription(Locale.ENGLISH));
    }

    @Test
    public void testCall() throws LLMToolException {
        String result = tool.call(Map.of(), Locale.ENGLISH);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
