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
package org.openhab.core.voice.internal.dialog.trigger.ks;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.voice.dialog.trigger.ks.KSService;

/**
 * Test class for {@link KSServiceRegistryImpl}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class KSServiceRegistryImplTest {

    private final KSService ks1 = mock(KSService.class);
    private final KSService ks2 = mock(KSService.class);

    private @NonNullByDefault({}) KSServiceRegistryImpl registry;

    @BeforeEach
    public void setUp() {
        registry = new KSServiceRegistryImpl();
        when(ks1.getUID()).thenReturn("ks1");
        when(ks1.getId()).thenReturn("ks1");
        when(ks2.getUID()).thenReturn("ks2");
        when(ks2.getId()).thenReturn("ks2");
    }

    @AfterEach
    public void tearDown() {
        clearInvocations(ks1, ks2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddAndRemoveKSService() {
        RegistryChangeListener<KSService> listener = mock(RegistryChangeListener.class);
        registry.addRegistryChangeListener(listener);

        registry.addKSService(ks1);
        assertEquals(ks1, registry.get("ks1"));
        verify(listener).added(ks1);

        registry.removeKSService(ks1);
        assertNull(registry.get("ks1"));
        verify(listener).removed(ks1);
    }

    @Test
    public void testGetAllAndStream() {
        registry.addKSService(ks1);
        registry.addKSService(ks2);

        Collection<KSService> all = registry.getAll();
        assertEquals(2, all.size());
        assertTrue(all.contains(ks1));
        assertTrue(all.contains(ks2));

        assertEquals(2, registry.stream().count());
    }

    @Test
    public void testUnsupportedOperations() {
        assertThrows(UnsupportedOperationException.class, () -> registry.add(ks1));
        assertThrows(UnsupportedOperationException.class, () -> registry.update(ks1));
        assertThrows(UnsupportedOperationException.class, () -> registry.remove("ks1"));
    }
}
