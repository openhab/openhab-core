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
package org.openhab.core.voice.internal.tts;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.voice.tts.TTSService;

/**
 * Test class for {@link TTSServiceRegistryImpl}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class TTSServiceRegistryImplTest {

    private final TTSService tts1 = mock(TTSService.class);
    private final TTSService tts2 = mock(TTSService.class);

    private @NonNullByDefault({}) TTSServiceRegistryImpl registry;

    @BeforeEach
    public void setUp() {
        registry = new TTSServiceRegistryImpl();
        when(tts1.getUID()).thenReturn("tts1");
        when(tts1.getId()).thenReturn("tts1");
        when(tts2.getUID()).thenReturn("tts2");
        when(tts2.getId()).thenReturn("tts2");
    }

    @AfterEach
    public void tearDown() {
        clearInvocations(tts1, tts2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddAndRemoveTTSService() {
        RegistryChangeListener<TTSService> listener = mock(RegistryChangeListener.class);
        registry.addRegistryChangeListener(listener);

        registry.addTTSService(tts1);
        assertEquals(tts1, registry.get("tts1"));
        verify(listener).added(tts1);

        registry.removeTTSService(tts1);
        assertNull(registry.get("tts1"));
        verify(listener).removed(tts1);
    }

    @Test
    public void testGetAllAndStream() {
        registry.addTTSService(tts1);
        registry.addTTSService(tts2);

        Collection<TTSService> all = registry.getAll();
        assertEquals(2, all.size());
        assertTrue(all.contains(tts1));
        assertTrue(all.contains(tts2));

        assertEquals(2, registry.stream().count());
    }

    @Test
    public void testUnsupportedOperations() {
        assertThrows(UnsupportedOperationException.class, () -> registry.add(tts1));
        assertThrows(UnsupportedOperationException.class, () -> registry.update(tts1));
        assertThrows(UnsupportedOperationException.class, () -> registry.remove("tts1"));
    }
}
