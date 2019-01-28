/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.thing.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.eclipse.smarthome.core.service.ReadyService;
import org.eclipse.smarthome.core.storage.Storage;
import org.eclipse.smarthome.core.storage.StorageService;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.core.util.BundleResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;

/**
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
public class ThingManagerTest {

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

        when(mockStorageService.getStorage(eq("thing_status_storage"), any(ClassLoader.class))).thenReturn(null);
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
