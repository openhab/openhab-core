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
package org.openhab.core.voice.internal.text;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.voice.text.HumanLanguageInterpreter;

/**
 * Test class for {@link HumanLanguageInterpreterRegistryImpl}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class HumanLanguageInterpreterRegistryImplTest {

    private final HumanLanguageInterpreter hli1 = mock(HumanLanguageInterpreter.class);
    private final HumanLanguageInterpreter hli2 = mock(HumanLanguageInterpreter.class);

    private @NonNullByDefault({}) HumanLanguageInterpreterRegistryImpl registry;

    @BeforeEach
    public void setUp() {
        registry = new HumanLanguageInterpreterRegistryImpl();
        when(hli1.getUID()).thenReturn("hli1");
        when(hli1.getId()).thenReturn("hli1");
        when(hli2.getUID()).thenReturn("hli2");
        when(hli2.getId()).thenReturn("hli2");
    }

    @AfterEach
    public void tearDown() {
        clearInvocations(hli1, hli2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddAndRemoveHumanLanguageInterpreter() {
        RegistryChangeListener<HumanLanguageInterpreter> listener = mock(RegistryChangeListener.class);
        registry.addRegistryChangeListener(listener);

        registry.addHumanLanguageInterpreter(hli1);
        assertEquals(hli1, registry.get("hli1"));
        verify(listener).added(hli1);

        registry.removeHumanLanguageInterpreter(hli1);
        assertNull(registry.get("hli1"));
        verify(listener).removed(hli1);
    }

    @Test
    public void testGetAllAndStream() {
        registry.addHumanLanguageInterpreter(hli1);
        registry.addHumanLanguageInterpreter(hli2);

        Collection<HumanLanguageInterpreter> all = registry.getAll();
        assertEquals(2, all.size());
        assertTrue(all.contains(hli1));
        assertTrue(all.contains(hli2));

        assertEquals(2, registry.stream().count());
    }

    @Test
    public void getByIdsListReturnsAvailableInterpreters() {
        registry.addHumanLanguageInterpreter(hli1);
        registry.addHumanLanguageInterpreter(hli2);

        List<HumanLanguageInterpreter> result = registry.getByIds(List.of("hli1", "hli2", "hli3"));
        assertEquals(2, result.size());
        assertTrue(result.contains(hli1));
        assertTrue(result.contains(hli2));
    }

    @Test
    public void getByIdsStringReturnsAvailableInterpreters() {
        registry.addHumanLanguageInterpreter(hli1);
        registry.addHumanLanguageInterpreter(hli2);

        List<HumanLanguageInterpreter> result = registry.getByIds("hli1,hli2,hli3");
        assertEquals(2, result.size());
        assertTrue(result.contains(hli1));
        assertTrue(result.contains(hli2));
    }

    @Test
    public void getByIdsStringHandlesBlankStringOrNull() {
        registry.addHumanLanguageInterpreter(hli1);
        registry.addHumanLanguageInterpreter(hli2);

        assertTrue(registry.getByIds("").isEmpty());
        assertTrue(registry.getByIds((String) null).isEmpty());
    }

    @Test
    public void testUnsupportedOperations() {
        assertThrows(UnsupportedOperationException.class, () -> registry.add(hli1));
        assertThrows(UnsupportedOperationException.class, () -> registry.update(hli1));
        assertThrows(UnsupportedOperationException.class, () -> registry.remove("hli1"));
    }
}
