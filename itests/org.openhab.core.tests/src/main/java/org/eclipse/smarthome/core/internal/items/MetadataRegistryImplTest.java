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
package org.eclipse.smarthome.core.internal.items;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ManagedMetadataProvider;
import org.eclipse.smarthome.core.items.Metadata;
import org.eclipse.smarthome.core.items.MetadataKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * @author Simon Kaufmann - initial contribution and API
 */
public class MetadataRegistryImplTest {

    private static final String ITEM_NAME = "itemName";

    @SuppressWarnings("rawtypes")
    private @Mock ServiceReference managedProviderRef;
    private @Mock BundleContext bundleContext;
    private @Mock ManagedMetadataProvider managedProvider;
    private @Mock Item item;

    private MetadataRegistryImpl registry;
    private ServiceListener providerTracker;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        initMocks(this);

        when(bundleContext.getService(same(managedProviderRef))).thenReturn(managedProvider);

        when(item.getName()).thenReturn(ITEM_NAME);

        registry = new MetadataRegistryImpl();

        registry.setManagedProvider(managedProvider);
        registry.activate(bundleContext);

        ArgumentCaptor<ServiceListener> captor = ArgumentCaptor.forClass(ServiceListener.class);
        verify(bundleContext).addServiceListener(captor.capture(), any());
        providerTracker = captor.getValue();
        providerTracker.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, managedProviderRef));

    }

    @Test
    public void testGet_empty() throws Exception {
        MetadataKey key = new MetadataKey("namespace", "itemName");

        Metadata res = registry.get(key);
        assertNull(res);
    }

    @Test
    public void testGet() throws Exception {
        MetadataKey key = new MetadataKey("namespace", "itemName");
        registry.added(managedProvider, new Metadata(key, "value", Collections.emptyMap()));
        registry.added(managedProvider,
                new Metadata(new MetadataKey("other", "itemName"), "other", Collections.emptyMap()));
        registry.added(managedProvider,
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

        verify(managedProvider).removeItemMetadata(captor.capture());
        assertEquals("itemName", captor.getValue());
    }

}
