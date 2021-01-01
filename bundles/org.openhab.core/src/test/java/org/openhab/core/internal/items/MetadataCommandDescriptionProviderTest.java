/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.items.ManagedMetadataProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandOption;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * @author Yannick Schaus - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
public class MetadataCommandDescriptionProviderTest {

    private static final String ITEM_NAME = "itemName";

    @SuppressWarnings("rawtypes")
    private @Mock ServiceReference managedProviderRef;
    private @Mock BundleContext bundleContext;
    private @Mock ManagedMetadataProvider managedProvider;

    private @Mock MetadataRegistryImpl metadataRegistry;
    private MetadataCommandDescriptionProvider commandDescriptionProvider;

    private ServiceListener providerTracker;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        when(bundleContext.getService(same(managedProviderRef))).thenReturn(managedProvider);

        metadataRegistry = new MetadataRegistryImpl();
        metadataRegistry.setManagedProvider(managedProvider);
        metadataRegistry.activate(bundleContext);
        metadataRegistry.waitForCompletedAsyncActivationTasks();

        ArgumentCaptor<ServiceListener> captor = ArgumentCaptor.forClass(ServiceListener.class);
        verify(bundleContext).addServiceListener(captor.capture(), any());
        providerTracker = captor.getValue();
        providerTracker.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, managedProviderRef));

        commandDescriptionProvider = new MetadataCommandDescriptionProvider(metadataRegistry, new HashMap<>());
    }

    @Test
    public void testEmpty() throws Exception {
        CommandDescription commandDescription = commandDescriptionProvider.getCommandDescription(ITEM_NAME, null);
        assertNull(commandDescription);
    }

    @Test
    public void testEmptyConfig() throws Exception {
        MetadataKey metadataKey = new MetadataKey("commandDescription", ITEM_NAME);
        // Map<String, Object> metadataConfig = new HashMap<>();
        Metadata metadata = new Metadata(metadataKey, "N/A", null);

        metadataRegistry.added(managedProvider, metadata);
        CommandDescription commandDescription = commandDescriptionProvider.getCommandDescription(ITEM_NAME, null);
        assertNull(commandDescription);
    }

    @Test
    public void testOptions() throws Exception {
        MetadataKey metadataKey = new MetadataKey("commandDescription", ITEM_NAME);
        Map<String, Object> metadataConfig = new HashMap<>();
        metadataConfig.put("options", "OPTION1,OPTION2 , 3 =Option 3  ");
        Metadata metadata = new Metadata(metadataKey, "N/A", metadataConfig);

        metadataRegistry.added(managedProvider, metadata);
        CommandDescription commandDescription = commandDescriptionProvider.getCommandDescription(ITEM_NAME, null);
        assertNotNull(commandDescription);
        assertNotNull(commandDescription.getCommandOptions());
        assertEquals(3, commandDescription.getCommandOptions().size());

        Iterator<CommandOption> it = commandDescription.getCommandOptions().iterator();
        CommandOption commandOption = it.next();
        assertEquals("OPTION1", commandOption.getCommand());
        assertNull(commandOption.getLabel());
        commandOption = it.next();
        assertEquals("OPTION2", commandOption.getCommand());
        assertNull(commandOption.getLabel());
        commandOption = it.next();
        assertEquals("3", commandOption.getCommand());
        assertEquals("Option 3", commandOption.getLabel());
    }
}
