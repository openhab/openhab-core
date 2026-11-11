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
import org.openhab.core.audio.AudioSource;
import org.openhab.core.common.registry.RegistryChangeListener;

/**
 * Test class for {@link AudioSourceRegistryImpl}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class AudioSourceRegistryImplTest {

    private final AudioSource source1 = mock(AudioSource.class);
    private final AudioSource source2 = mock(AudioSource.class);

    private @NonNullByDefault({}) AudioSourceRegistryImpl registry;

    @BeforeEach
    public void setUp() {
        registry = new AudioSourceRegistryImpl();
        when(source1.getUID()).thenReturn("source1");
        when(source1.getId()).thenReturn("source1");
        when(source2.getUID()).thenReturn("source2");
        when(source2.getId()).thenReturn("source2");
    }

    @AfterEach
    public void tearDown() {
        clearInvocations(source1, source2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddAndRemoveAudioSource() {
        RegistryChangeListener<AudioSource> listener = mock(RegistryChangeListener.class);
        registry.addRegistryChangeListener(listener);

        registry.addAudioSource(source1);
        assertEquals(source1, registry.get("source1"));
        verify(listener).added(source1);

        registry.removeAudioSource(source1);
        assertNull(registry.get("source1"));
        verify(listener).removed(source1);
    }

    @Test
    public void testGetAllAndStream() {
        registry.addAudioSource(source1);
        registry.addAudioSource(source2);

        Collection<AudioSource> all = registry.getAll();
        assertEquals(2, all.size());
        assertTrue(all.contains(source1));
        assertTrue(all.contains(source2));

        assertEquals(2, registry.stream().count());
    }

    @Test
    public void testUnsupportedOperations() {
        assertThrows(UnsupportedOperationException.class, () -> registry.add(source1));
        assertThrows(UnsupportedOperationException.class, () -> registry.update(source1));
        assertThrows(UnsupportedOperationException.class, () -> registry.remove("source1"));
    }
}
