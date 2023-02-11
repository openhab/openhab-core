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
package org.openhab.core.config.discovery.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryServiceRegistry;
import org.openhab.core.config.discovery.inbox.events.InboxAddedEvent;
import org.openhab.core.config.discovery.inbox.events.InboxUpdatedEvent;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.thing.type.ThingTypeRegistry;

/**
 * @author Simon Kaufmann - Initial contribution
 * @author Laurent Garnier - Added tests testApproveWithThingId and testApproveWithInvalidThingId
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class PersistentInboxTest {

    private static final String THING_OTHER_ID = "other";

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("test", "test");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "test");
    private static final ThingUID THING_OTHER_UID = new ThingUID(THING_TYPE_UID, THING_OTHER_ID);

    private @NonNullByDefault({}) PersistentInbox inbox;
    private @Nullable Thing lastAddedThing;

    private @Mock @NonNullByDefault({}) ThingRegistry thingRegistryMock;
    private @Mock @NonNullByDefault({}) StorageService storageServiceMock;
    private @Mock @NonNullByDefault({}) Storage<Object> storageMock;
    private @Mock @NonNullByDefault({}) ManagedThingProvider thingProviderMock;
    private @Mock @NonNullByDefault({}) ThingTypeRegistry thingTypeRegistryMock;
    private @Mock @NonNullByDefault({}) ConfigDescriptionRegistry configDescriptionRegistryMock;
    private @Mock @NonNullByDefault({}) ThingHandlerFactory thingHandlerFactoryMock;
    private @Mock @NonNullByDefault({}) ThingType thingTypeMock;

    @BeforeEach
    public void setup() {
        when(thingTypeMock.getConfigDescriptionURI()).thenReturn(null);
        when(thingTypeRegistryMock.getThingType(any())).thenReturn(thingTypeMock);
        when(storageServiceMock.getStorage(any(String.class), any(ClassLoader.class))).thenReturn(storageMock);
        doAnswer(invocation -> lastAddedThing = (Thing) invocation.getArguments()[0]).when(thingRegistryMock)
                .add(any(Thing.class));
        when(thingHandlerFactoryMock.supportsThingType(eq(THING_TYPE_UID))).thenReturn(true);
        when(thingHandlerFactoryMock.createThing(eq(THING_TYPE_UID), any(Configuration.class), eq(THING_UID), any()))
                .then(invocation -> ThingBuilder.create(THING_TYPE_UID, "test")
                        .withConfiguration((Configuration) invocation.getArguments()[1]).build());
        when(thingHandlerFactoryMock
                .createThing(eq(THING_TYPE_UID), any(Configuration.class), eq(THING_OTHER_UID), any()))
                        .then(invocation -> ThingBuilder.create(THING_TYPE_UID, THING_OTHER_ID)
                                .withConfiguration((Configuration) invocation.getArguments()[1]).build());
        inbox = new PersistentInbox(storageServiceMock, mock(DiscoveryServiceRegistry.class), thingRegistryMock,
                thingProviderMock, thingTypeRegistryMock, configDescriptionRegistryMock);
        inbox.addThingHandlerFactory(thingHandlerFactoryMock);
    }

    @Test
    public void testConfigUpdateNormalizationWithConfigDescription() throws URISyntaxException {
        Map<String, Object> props = new HashMap<>();
        props.put("foo", "1");
        Configuration config = new Configuration(props);
        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID).withConfiguration(config).build();
        configureConfigDescriptionRegistryMock("foo", Type.TEXT);
        when(thingRegistryMock.get(eq(THING_UID))).thenReturn(thing);
        when(thingProviderMock.get(eq(THING_UID))).thenReturn(thing);

        assertTrue(thing.getConfiguration().get("foo") instanceof String);

        inbox.activate();
        inbox.add(DiscoveryResultBuilder.create(THING_UID).withProperty("foo", 3).build());

        assertTrue(thing.getConfiguration().get("foo") instanceof String);
        // thing updated if managed
        assertEquals("3", thing.getConfiguration().get("foo"));
    }

    @Test
    public void testConfigUpdateNormalizationWithConfigDescriptionUnanagedThing() {
        Configuration config = new Configuration(Map.of("foo", "1"));
        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID).withConfiguration(config).build();
        configureConfigDescriptionRegistryMock("foo", Type.TEXT);
        when(thingRegistryMock.get(eq(THING_UID))).thenReturn(thing);

        assertTrue(thing.getConfiguration().get("foo") instanceof String);

        inbox.activate();
        inbox.add(DiscoveryResultBuilder.create(THING_UID).withProperty("foo", 3).build());

        assertTrue(thing.getConfiguration().get("foo") instanceof String);
        // thing not updated if unmanaged
        assertEquals("1", thing.getConfiguration().get("foo"));
    }

    @Test
    public void testApproveNormalization() {
        DiscoveryResult result = DiscoveryResultBuilder.create(THING_UID).withProperty("foo", 3).build();
        configureConfigDescriptionRegistryMock("foo", Type.TEXT);
        when(storageMock.getValues()).thenReturn(List.of(result));

        inbox.activate();
        inbox.approve(THING_UID, "Test", null);

        assertEquals(THING_UID, lastAddedThing.getUID());
        assertTrue(lastAddedThing.getConfiguration().get("foo") instanceof String);
        assertEquals("3", lastAddedThing.getConfiguration().get("foo"));
    }

    @Test
    public void testApproveWithThingId() {
        DiscoveryResult result = DiscoveryResultBuilder.create(THING_UID).withProperty("foo", 3).build();
        configureConfigDescriptionRegistryMock("foo", Type.TEXT);
        when(storageMock.getValues()).thenReturn(List.of(result));

        inbox.activate();
        inbox.approve(THING_UID, "Test", THING_OTHER_ID);

        assertEquals(THING_OTHER_UID, lastAddedThing.getUID());
        assertTrue(lastAddedThing.getConfiguration().get("foo") instanceof String);
        assertEquals("3", lastAddedThing.getConfiguration().get("foo"));
    }

    @Test
    public void testApproveWithInvalidThingId() {
        DiscoveryResult result = DiscoveryResultBuilder.create(THING_UID).withProperty("foo", 3).build();
        configureConfigDescriptionRegistryMock("foo", Type.TEXT);
        when(storageMock.getValues()).thenReturn(List.of(result));

        inbox.activate();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            inbox.approve(THING_UID, "Test", "invalid:id");
        });
        assertEquals("New Thing ID invalid:id must not contain multiple segments", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> {
            inbox.approve(THING_UID, "Test", "invalid$id");
        });
        assertEquals("Invalid thing UID test:test:invalid$id", exception.getMessage());
    }

    @Test
    public void testEmittedAddedResultIsReadFromStorage() {
        DiscoveryResult result = DiscoveryResultBuilder.create(THING_UID).withProperty("foo", 3).build();

        EventPublisher eventPublisher = mock(EventPublisher.class);
        inbox.setEventPublisher(eventPublisher);

        when(storageMock.get(THING_UID.toString())) //
                .thenReturn(null) //
                .thenReturn(DiscoveryResultBuilder.create(THING_UID).withProperty("foo", "bar").build());

        inbox.activate();
        inbox.add(result);

        // 1st call checks existence of the result in the storage (returns null)
        // 2nd call retrieves the stored instance before the event gets emitted
        // (modified due to storage mock configuration)
        verify(storageMock, times(2)).get(THING_UID.toString());

        ArgumentCaptor<InboxAddedEvent> eventCaptor = ArgumentCaptor.forClass(InboxAddedEvent.class);
        verify(eventPublisher).post(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getDiscoveryResult().properties, hasEntry("foo", "bar"));
    }

    @Test
    public void testEmittedUpdatedResultIsReadFromStorage() {
        DiscoveryResult result = DiscoveryResultBuilder.create(THING_UID).withProperty("foo", 3).build();

        EventPublisher eventPublisher = mock(EventPublisher.class);
        inbox.setEventPublisher(eventPublisher);

        when(storageMock.get(THING_UID.toString())) //
                .thenReturn(result) //
                .thenReturn(DiscoveryResultBuilder.create(THING_UID).withProperty("foo", "bar").build());

        inbox.activate();
        inbox.add(result);

        // 1st call checks existence of the result in the storage (returns the original result)
        // 2nd call retrieves the stored instance before the event gets emitted
        // (modified due to storage mock configuration)
        verify(storageMock, times(2)).get(THING_UID.toString());

        ArgumentCaptor<InboxUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(InboxUpdatedEvent.class);
        verify(eventPublisher).post(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getDiscoveryResult().properties, hasEntry("foo", "bar"));
    }

    private void configureConfigDescriptionRegistryMock(String paramName, Type type) {
        URI configDescriptionURI = URI.create("thing-type:test:test");
        ThingType thingType = ThingTypeBuilder.instance(THING_TYPE_UID, "Test")
                .withConfigDescriptionURI(configDescriptionURI).build();
        ConfigDescription configDesc = ConfigDescriptionBuilder.create(configDescriptionURI)
                .withParameter(ConfigDescriptionParameterBuilder.create(paramName, type).build()).build();

        when(thingTypeRegistryMock.getThingType(THING_TYPE_UID)).thenReturn(thingType);
        when(configDescriptionRegistryMock.getConfigDescription(eq(configDescriptionURI))).thenReturn(configDesc);
    }
}
