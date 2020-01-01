/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.thing.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.service.ReadyService;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;

/**
 * @author Simon Kaufmann - Initial contribution
 */
public class ThingManagerImplTest {

    private @Mock BundleResolver mockBundleResolver;
    private @Mock Bundle mockBundle;
    private @Mock ComponentContext mockComponentContext;
    private @Mock ReadyService mockReadyService;
    private @Mock Thing mockThing;

    private @Mock StorageService mockStorageService;
    private @Mock Storage<Object> mockStorage;

    private final ThingRegistryImpl thingRegistry = new ThingRegistryImpl();

    @Before
    public void setup() {
        initMocks(this);
        when(mockBundle.getSymbolicName()).thenReturn("test");
        when(mockBundleResolver.resolveBundle(any())).thenReturn(mockBundle);
        when(mockThing.getUID()).thenReturn(new ThingUID("test", "thing"));
    }

    @Test
    public void testThingHandlerFactoryLifecycle() {
        ThingHandlerFactory mockFactory1 = mock(ThingHandlerFactory.class);
        ThingHandlerFactory mockFactory2 = mock(ThingHandlerFactory.class);

        ThingManagerImpl thingManager = new ThingManagerImpl();
        thingManager.setBundleResolver(mockBundleResolver);
        thingManager.setThingRegistry(thingRegistry);
        thingManager.setReadyService(mockReadyService);
        thingManager.thingAdded(mockThing, null);

        // ensure usage is delayed until activation
        thingManager.addThingHandlerFactory(mockFactory1);
        verify(mockFactory1, times(0)).supportsThingType(any());
        thingManager.activate(mockComponentContext);
        verify(mockFactory1, atLeastOnce()).supportsThingType(any());

        // ensure it is directly used
        thingManager.addThingHandlerFactory(mockFactory2);
        verify(mockFactory2, atLeastOnce()).supportsThingType(any());
    }

    @Test
    public void testCallSetEnabledWithUnknownThingUID() throws Exception {
        ThingUID unknownUID = new ThingUID("someBundle", "someType", "someID");
        ThingManagerImpl thingManager = new ThingManagerImpl();

        when(mockStorageService.getStorage(eq("thing_status_storage"), any(ClassLoader.class))).thenReturn(mockStorage);
        thingManager.setStorageService(mockStorageService);
        thingManager.setEnabled(unknownUID, true);
        verify(mockStorage).remove(eq(unknownUID.getAsString()));

        thingManager.setEnabled(unknownUID, false);
        verify(mockStorage).put(eq(unknownUID.getAsString()), eq(""));
    }

    @Test
    public void testCallIsEnabledWithUnknownThingUIDAndNullStorage() throws Exception {
        ThingUID unknownUID = new ThingUID("someBundle", "someType", "someID");
        ThingManagerImpl thingManager = new ThingManagerImpl();

        when(mockStorageService.getStorage(eq("thing_status_storage"), any(ClassLoader.class))).thenReturn(mockStorage);
        thingManager.setStorageService(mockStorageService);
        assertEquals(thingManager.isEnabled(unknownUID), true);

    }

    @Test
    public void testCallIsEnabledWithUnknownThingUIDAndNonNullStorage() throws Exception {
        ThingUID unknownUID = new ThingUID("someBundle", "someType", "someID");
        ThingManagerImpl thingManager = new ThingManagerImpl();

        when(mockStorage.containsKey(unknownUID.getAsString())).thenReturn(false);
        when(mockStorageService.getStorage(eq("thing_status_storage"), any(ClassLoader.class))).thenReturn(mockStorage);
        thingManager.setStorageService(mockStorageService);
        assertEquals(thingManager.isEnabled(unknownUID), true);

        when(mockStorage.containsKey(unknownUID.getAsString())).thenReturn(true);
        when(mockStorageService.getStorage(eq("thing_status_storage"), any(ClassLoader.class))).thenReturn(mockStorage);
        thingManager.setStorageService(mockStorageService);
        assertEquals(thingManager.isEnabled(unknownUID), false);
    }
}
