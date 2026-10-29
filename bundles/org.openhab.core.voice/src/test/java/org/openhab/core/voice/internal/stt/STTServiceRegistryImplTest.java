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
package org.openhab.core.voice.internal.stt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.voice.stt.STTService;

/**
 * Test class for {@link STTServiceRegistryImpl}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class STTServiceRegistryImplTest {

    private final STTService stt1 = mock(STTService.class);
    private final STTService stt2 = mock(STTService.class);

    private @NonNullByDefault({}) STTServiceRegistryImpl registry;

    @BeforeEach
    public void setUp() {
        registry = new STTServiceRegistryImpl();
        when(stt1.getUID()).thenReturn("stt1");
        when(stt1.getId()).thenReturn("stt1");
        when(stt2.getUID()).thenReturn("stt2");
        when(stt2.getId()).thenReturn("stt2");
    }

    @AfterEach
    public void tearDown() {
        clearInvocations(stt1, stt2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddAndRemoveSTTService() {
        RegistryChangeListener<STTService> listener = mock(RegistryChangeListener.class);
        registry.addRegistryChangeListener(listener);

        registry.addSTTService(stt1);
        assertEquals(stt1, registry.get("stt1"));
        verify(listener).added(stt1);

        registry.removeSTTService(stt1);
        assertNull(registry.get("stt1"));
        verify(listener).removed(stt1);
    }

    @Test
    public void testGetAllAndStream() {
        registry.addSTTService(stt1);
        registry.addSTTService(stt2);

        Collection<STTService> all = registry.getAll();
        assertEquals(2, all.size());
        assertTrue(all.contains(stt1));
        assertTrue(all.contains(stt2));

        assertEquals(2, registry.stream().count());
    }

    @Test
    public void testUnsupportedOperations() {
        assertThrows(UnsupportedOperationException.class, () -> registry.add(stt1));
        assertThrows(UnsupportedOperationException.class, () -> registry.update(stt1));
        assertThrows(UnsupportedOperationException.class, () -> registry.remove("stt1"));
    }
}
