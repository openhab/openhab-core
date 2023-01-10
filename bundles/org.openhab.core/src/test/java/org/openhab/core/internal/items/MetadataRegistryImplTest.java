/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.internal.items;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.items.ManagedMetadataProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.service.ReadyService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * @author Simon Kaufmann - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class MetadataRegistryImplTest {

    @SuppressWarnings("rawtypes")
    private @Mock @NonNullByDefault({}) ServiceReference managedProviderRefMock;
    private @Mock @NonNullByDefault({}) BundleContext bundleContextMock;
    private @Mock @NonNullByDefault({}) ManagedMetadataProvider managedProviderMock;
    private @Mock @NonNullByDefault({}) ReadyService readyServiceMock;

    private @NonNullByDefault({}) MetadataRegistryImpl registry;
    private @NonNullByDefault({}) ServiceListener providerTracker;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        when(bundleContextMock.getService(same(managedProviderRefMock))).thenReturn(managedProviderMock);

        registry = new MetadataRegistryImpl(readyServiceMock);
        registry.setManagedProvider(managedProviderMock);
        registry.activate(bundleContextMock);
        registry.waitForCompletedAsyncActivationTasks();

        ArgumentCaptor<ServiceListener> captor = ArgumentCaptor.forClass(ServiceListener.class);
        verify(bundleContextMock).addServiceListener(captor.capture(), any());
        providerTracker = captor.getValue();
        providerTracker.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, managedProviderRefMock));
    }

    @Test
    public void testGetEmpty() throws Exception {
        MetadataKey key = new MetadataKey("namespace", "itemName");

        Metadata res = registry.get(key);
        assertNull(res);
    }

    @Test
    public void testGet() throws Exception {
        MetadataKey key = new MetadataKey("namespace", "itemName");
        registry.added(managedProviderMock, new Metadata(key, "value", Collections.emptyMap()));
        registry.added(managedProviderMock,
                new Metadata(new MetadataKey("other", "itemName"), "other", Collections.emptyMap()));
        registry.added(managedProviderMock,
                new Metadata(new MetadataKey("namespace", "other"), "other", Collections.emptyMap()));

        Metadata res = registry.get(key);
        assertNotNull(res);
        assertEquals("value", res.getValue());
        assertEquals("namespace", res.getUID().getNamespace());
        assertEquals("itemName", res.getUID().getItemName());
    }

    @Test
    public void testRemoveItemMetadata() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        registry.removeItemMetadata("itemName");

        verify(managedProviderMock).removeItemMetadata(captor.capture());
        assertEquals("itemName", captor.getValue());
    }
}
