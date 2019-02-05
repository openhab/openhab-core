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
package org.eclipse.smarthome.config.discovery.internal;

import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;
import org.eclipse.smarthome.config.core.ConfigDescriptionRegistry;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.inbox.events.InboxAddedEvent;
import org.eclipse.smarthome.config.discovery.inbox.events.InboxUpdatedEvent;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.storage.Storage;
import org.eclipse.smarthome.core.storage.StorageService;
import org.eclipse.smarthome.core.thing.ManagedThingProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeBuilder;
import org.eclipse.smarthome.core.thing.type.ThingTypeRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

/**
 * @author Simon Kaufmann - initial contribution and API
 */
public class PersistentInboxTest {

    private PersistentInbox inbox;

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("test", "test");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "test");

    @Mock
    private ThingRegistry thingRegistry;
    private Thing lastAddedThing = null;

    @Mock
    private StorageService storageService;

    @Mock
    private Storage<Object> storage;

    @Mock
    private ManagedThingProvider thingProvider;

    @Mock
    private ThingTypeRegistry thingTypeRegistry;

    @Mock
    private ConfigDescriptionRegistry configDescriptionRegistry;

    @Mock
    private ThingHandlerFactory thingHandlerFactory;

    @Before
    public void setup() {
        initMocks(this);
        when(storageService.getStorage(any(String.class), any(ClassLoader.class))).thenReturn(storage);
        doAnswer(invocation -> lastAddedThing = (Thing) invocation.getArguments()[0]).when(thingRegistry)
                .add(any(Thing.class));
        when(thingHandlerFactory.supportsThingType(eq(THING_TYPE_UID))).thenReturn(true);
        when(thingHandlerFactory.createThing(eq(THING_TYPE_UID), any(Configuration.class), eq(THING_UID), any()))
                .then(invocation -> ThingBuilder.create(THING_TYPE_UID, "test")
                        .withConfiguration((Configuration) invocation.getArguments()[1]).build());

        inbox = new PersistentInbox();
        inbox.setThingRegistry(thingRegistry);
        inbox.setStorageService(storageService);
        inbox.setManagedThingProvider(thingProvider);
        inbox.setConfigDescriptionRegistry(configDescriptionRegistry);
        inbox.setThingTypeRegistry(thingTypeRegistry);
        inbox.addThingHandlerFactory(thingHandlerFactory);
    }

    @Test
    public void testConfigUpdateNormalization_guessType() {
        Map<String, Object> props = new HashMap<>();
        props.put("foo", 1);
        Configuration config = new Configuration(props);
        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID).withConfiguration(config).build();

        when(thingRegistry.get(eq(THING_UID))).thenReturn(thing);

        assertTrue(thing.getConfiguration().get("foo") instanceof BigDecimal);
        inbox.add(DiscoveryResultBuilder.create(THING_UID).withProperty("foo", 3).build());
        assertTrue(thing.getConfiguration().get("foo") instanceof BigDecimal);
        assertEquals(new BigDecimal(3), thing.getConfiguration().get("foo"));
    }

    @Test
    public void testConfigUpdateNormalization_withConfigDescription() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("foo", "1");
        Configuration config = new Configuration(props);
        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID).withConfiguration(config).build();
        configureConfigDescriptionRegistryMock("foo", Type.TEXT);
        when(thingRegistry.get(eq(THING_UID))).thenReturn(thing);

        assertTrue(thing.getConfiguration().get("foo") instanceof String);
        inbox.add(DiscoveryResultBuilder.create(THING_UID).withProperty("foo", 3).build());
        assertTrue(thing.getConfiguration().get("foo") instanceof String);
        assertEquals("3", thing.getConfiguration().get("foo"));
    }

    @Test
    public void testApproveNormalization() throws Exception {
        DiscoveryResult result = DiscoveryResultBuilder.create(THING_UID).withProperty("foo", 3).build();
        configureConfigDescriptionRegistryMock("foo", Type.TEXT);
        when(storage.getValues()).thenReturn(Collections.singletonList(result));

        inbox.approve(THING_UID, "Test");

        assertTrue(lastAddedThing.getConfiguration().get("foo") instanceof String);
        assertEquals("3", lastAddedThing.getConfiguration().get("foo"));
    }

    @Test
    public void testEmittedAddedResultIsReadFromStorage() {
        DiscoveryResult result = DiscoveryResultBuilder.create(THING_UID).withProperty("foo", 3).build();

        EventPublisher eventPublisher = mock(EventPublisher.class);
        inbox.setEventPublisher(eventPublisher);

        when(storage.get(THING_UID.toString())) //
                .thenReturn(null) //
                .thenReturn(DiscoveryResultBuilder.create(THING_UID).withProperty("foo", "bar").build());

        inbox.add(result);

        // 1st call checks existence of the result in the storage (returns null)
        // 2nd call retrieves the stored instance before the event gets emitted
        // (modified due to storage mock configuration)
        verify(storage, times(2)).get(THING_UID.toString());

        ArgumentCaptor<InboxAddedEvent> eventCaptor = ArgumentCaptor.forClass(InboxAddedEvent.class);
        verify(eventPublisher).post(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getDiscoveryResult().properties, hasEntry("foo", "bar"));
    }

    @Test
    public void testEmittedUpdatedResultIsReadFromStorage() {
        DiscoveryResult result = DiscoveryResultBuilder.create(THING_UID).withProperty("foo", 3).build();

        EventPublisher eventPublisher = mock(EventPublisher.class);
        inbox.setEventPublisher(eventPublisher);

        when(storage.get(THING_UID.toString())) //
                .thenReturn(result) //
                .thenReturn(DiscoveryResultBuilder.create(THING_UID).withProperty("foo", "bar").build());

        inbox.add(result);

        // 1st call checks existence of the result in the storage (returns the original result)
        // 2nd call retrieves the stored instance before the event gets emitted
        // (modified due to storage mock configuration)
        verify(storage, times(2)).get(THING_UID.toString());

        ArgumentCaptor<InboxUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(InboxUpdatedEvent.class);
        verify(eventPublisher).post(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getDiscoveryResult().properties, hasEntry("foo", "bar"));
    }

    private void configureConfigDescriptionRegistryMock(String paramName, Type type) throws URISyntaxException {
        URI configDescriptionURI = new URI("thing-type:test:test");
        ThingType thingType = ThingTypeBuilder.instance(THING_TYPE_UID, "Test")
                .withConfigDescriptionURI(configDescriptionURI).build();
        ConfigDescriptionParameter param = ConfigDescriptionParameterBuilder.create(paramName, type).build();
        ConfigDescription configDesc = new ConfigDescription(configDescriptionURI, Collections.singletonList(param));

        when(thingTypeRegistry.getThingType(THING_TYPE_UID)).thenReturn(thingType);
        when(configDescriptionRegistry.getConfigDescription(eq(configDescriptionURI))).thenReturn(configDesc);
    }

}
