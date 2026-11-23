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
package org.openhab.core.audio.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.common.registry.RegistryChangeListener;

/**
 * Test class for {@link AudioSinkRegistryImpl}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class AudioSinkRegistryImplTest {

    private final AudioSink sink1 = mock(AudioSink.class);
    private final AudioSink sink2 = mock(AudioSink.class);

    private @NonNullByDefault({}) AudioSinkRegistryImpl registry;

    @BeforeEach
    public void setUp() {
        registry = new AudioSinkRegistryImpl();
        when(sink1.getUID()).thenReturn("sink1");
        when(sink1.getId()).thenReturn("sink1");
        when(sink2.getUID()).thenReturn("sink2");
        when(sink2.getId()).thenReturn("sink2");
    }

    @AfterEach
    public void tearDown() {
        clearInvocations(sink1, sink2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddAndRemoveAudioSink() {
        RegistryChangeListener<AudioSink> listener = mock(RegistryChangeListener.class);
        registry.addRegistryChangeListener(listener);

        registry.addAudioSink(sink1);
        assertEquals(sink1, registry.get("sink1"));
        verify(listener).added(sink1);

        registry.removeAudioSink(sink1);
        assertNull(registry.get("sink1"));
        verify(listener).removed(sink1);
    }

    @Test
    public void testGetAllAndStream() {
        registry.addAudioSink(sink1);
        registry.addAudioSink(sink2);

        Collection<AudioSink> all = registry.getAll();
        assertEquals(2, all.size());
        assertTrue(all.contains(sink1));
        assertTrue(all.contains(sink2));

        assertEquals(2, registry.stream().count());
    }

    @Test
    public void testUnsupportedOperations() {
        assertThrows(UnsupportedOperationException.class, () -> registry.add(sink1));
        assertThrows(UnsupportedOperationException.class, () -> registry.update(sink1));
        assertThrows(UnsupportedOperationException.class, () -> registry.remove("sink1"));
    }
}
