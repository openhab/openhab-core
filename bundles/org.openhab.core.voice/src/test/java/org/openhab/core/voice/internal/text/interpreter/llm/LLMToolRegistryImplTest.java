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
import static org.mockito.Mockito.*;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;

/**
 * Test class for {@link LLMToolRegistryImpl}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class LLMToolRegistryImplTest {

    private LLMTool tool1 = mock(LLMTool.class);
    private LLMTool tool2 = mock(LLMTool.class);

    private @NonNullByDefault({}) LLMToolRegistryImpl registry;

    @BeforeEach
    public void setUp() {
        registry = new LLMToolRegistryImpl();
        when(tool1.getUID()).thenReturn("tool1");
        when(tool2.getUID()).thenReturn("tool2");
    }

    @AfterEach
    public void tearDown() {
        clearInvocations(tool1, tool2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddAndRemoveLLMTool() {
        RegistryChangeListener<LLMTool> listener = mock(RegistryChangeListener.class);
        registry.addRegistryChangeListener(listener);

        registry.addLLMTool(tool1);
        assertEquals(tool1, registry.get("tool1"));
        verify(listener).added(tool1);

        registry.removeLLMTool(tool1);
        assertNull(registry.get("tool1"));
        verify(listener).removed(tool1);
    }

    @Test
    public void getByIdsListReturnsAvailableLLMTools() {
        registry.addLLMTool(tool1);
        registry.addLLMTool(tool2);

        List<LLMTool> result = registry.getByIds(List.of("tool1", "tool2", "tool3"));
        assertEquals(2, result.size());
        assertTrue(result.contains(tool1));
        assertTrue(result.contains(tool2));
    }

    @Test
    public void getByIdsStringReturnsAvailableLLMTools() {
        registry.addLLMTool(tool1);
        registry.addLLMTool(tool2);

        List<LLMTool> result = registry.getByIds("tool1,tool2,tool3");
        assertEquals(2, result.size());
        assertTrue(result.contains(tool1));
        assertTrue(result.contains(tool2));
    }

    @Test
    public void getByIdsStringHandlesBlankStringOrNull() {
        registry.addLLMTool(tool1);
        registry.addLLMTool(tool2);

        assertTrue(registry.getByIds("").isEmpty());
        assertTrue(registry.getByIds((String) null).isEmpty());
    }
}
