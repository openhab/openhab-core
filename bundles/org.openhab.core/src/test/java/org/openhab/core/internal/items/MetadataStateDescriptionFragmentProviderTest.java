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
package org.openhab.core.internal.items;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.openhab.core.items.Item;
import org.openhab.core.items.ManagedMetadataProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateOption;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * @author Yannick Schaus - Initial contribution
 */
public class MetadataStateDescriptionFragmentProviderTest {

    private static final String ITEM_NAME = "itemName";

    @SuppressWarnings("rawtypes")
    private @Mock ServiceReference managedProviderRef;
    private @Mock BundleContext bundleContext;
    private @Mock ManagedMetadataProvider managedProvider;
    private @Mock Item item;

    private @Mock MetadataRegistryImpl metadataRegistry;
    private MetadataStateDescriptionFragmentProvider stateDescriptionFragmentProvider;

    private ServiceListener providerTracker;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        initMocks(this);

        when(bundleContext.getService(same(managedProviderRef))).thenReturn(managedProvider);

        when(item.getName()).thenReturn(ITEM_NAME);

        metadataRegistry = new MetadataRegistryImpl();

        metadataRegistry.setManagedProvider(managedProvider);
        metadataRegistry.activate(bundleContext);

        ArgumentCaptor<ServiceListener> captor = ArgumentCaptor.forClass(ServiceListener.class);
        verify(bundleContext).addServiceListener(captor.capture(), any());
        providerTracker = captor.getValue();
        providerTracker.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, managedProviderRef));

        stateDescriptionFragmentProvider = new MetadataStateDescriptionFragmentProvider(metadataRegistry,
                new HashMap<>());
    }

    @Test
    public void testEmpty() throws Exception {
        StateDescriptionFragment stateDescriptionFragment = stateDescriptionFragmentProvider
                .getStateDescriptionFragment(ITEM_NAME, null);
        assertNull(stateDescriptionFragment);
    }

    @SuppressWarnings("null")
    @Test
    public void testFragment() throws Exception {
        MetadataKey metadataKey = new MetadataKey("stateDescription", ITEM_NAME);
        Map<String, Object> metadataConfig = new HashMap<>();
        metadataConfig.put("pattern", "%.1f %unit%");
        metadataConfig.put("min", 18.5);
        metadataConfig.put("max", "34");
        metadataConfig.put("step", 3);
        metadataConfig.put("readOnly", "true");
        metadataConfig.put("options", "OPTION1,OPTION2 , 3 =Option 3 ");
        Metadata metadata = new Metadata(metadataKey, "N/A", metadataConfig);
        metadataRegistry.added(managedProvider, metadata);

        StateDescriptionFragment stateDescriptionFragment = stateDescriptionFragmentProvider
                .getStateDescriptionFragment(ITEM_NAME, null);
        assertNotNull(stateDescriptionFragment);
        assertEquals("%.1f %unit%", stateDescriptionFragment.getPattern());
        assertEquals(new BigDecimal(18.5), stateDescriptionFragment.getMinimum());
        assertEquals(new BigDecimal(34), stateDescriptionFragment.getMaximum());
        assertEquals(new BigDecimal(3), stateDescriptionFragment.getStep());
        assertEquals(true, stateDescriptionFragment.isReadOnly());
        assertNotNull(stateDescriptionFragment.getOptions());
        Iterator<StateOption> it = stateDescriptionFragment.getOptions().iterator();
        StateOption stateOption = it.next();
        assertEquals("OPTION1", stateOption.getValue());
        assertEquals(null, stateOption.getLabel());
        stateOption = it.next();
        assertEquals("OPTION2", stateOption.getValue());
        assertEquals(null, stateOption.getLabel());
        stateOption = it.next();
        assertEquals("3", stateOption.getValue());
        assertEquals("Option 3", stateOption.getLabel());
    }
}
