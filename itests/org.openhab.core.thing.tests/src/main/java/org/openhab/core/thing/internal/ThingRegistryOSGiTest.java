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
package org.openhab.core.thing.internal;

import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingProvider;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.events.ThingAddedEvent;
import org.openhab.core.thing.events.ThingRemovedEvent;
import org.openhab.core.thing.events.ThingUpdatedEvent;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.types.Command;
import org.osgi.framework.ServiceRegistration;

/**
 * {@link ThingRegistryOSGiTest} tests the {@link ThingRegistry}.
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Kai Kreuzer - Moved createThing test from managed provider
 */
@NonNullByDefault
public class ThingRegistryOSGiTest extends JavaOSGiTest {

    private @NonNullByDefault({}) ManagedThingProvider managedThingProvider;
    private @NonNullByDefault({}) ServiceRegistration<?> thingHandlerFactoryServiceReg;

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("binding:type");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "id");
    private static final Thing THING = ThingBuilder.create(THING_TYPE_UID, THING_UID).build();
    private static final String THING1_ID = "testThing1";
    private static final String THING2_ID = "testThing2";

    private @Nullable Map<String, Object> changedParameters = null;
    private @Nullable Event receivedEvent = null;

    @BeforeEach
    public void setUp() {
        registerVolatileStorageService();
        managedThingProvider = getService(ManagedThingProvider.class);
        registerThingTypeProvider();
        unregisterCurrentThingHandlerFactory();
    }

    @AfterEach
    public void teardown() {
        unregisterCurrentThingHandlerFactory();
        managedThingProvider.getAll().stream().forEach(it -> managedThingProvider.remove(it.getUID()));
    }

    @Test
    public void assertThatThingRegistryEventSubscribersReceiveEventsAboutThingChanges() {
        EventSubscriber thingRegistryEventSubscriber = new EventSubscriber() {

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Set.of(ThingAddedEvent.TYPE, ThingRemovedEvent.TYPE, ThingUpdatedEvent.TYPE);
            }

            @Override
            public void receive(Event event) {
                receivedEvent = event;
            }
        };
        registerService(thingRegistryEventSubscriber);

        // add new thing
        managedThingProvider.add(THING);
        waitForAssert(() -> {
            assertThat(receivedEvent, notNullValue());
        });
        assertThat(receivedEvent, is(instanceOf(ThingAddedEvent.class)));
        receivedEvent = null;

        // update thing
        Thing updatedThing = ThingBuilder.create(THING_TYPE_UID, THING_UID).build();
        managedThingProvider.update(updatedThing);
        waitForAssert(() -> {
            assertThat(receivedEvent, notNullValue());
        });
        assertThat(receivedEvent, is(instanceOf(ThingUpdatedEvent.class)));
        receivedEvent = null;

        // remove thing
        managedThingProvider.remove(THING.getUID());
        waitForAssert(() -> {
            assertThat(receivedEvent, notNullValue());
        });
        assertThat(receivedEvent, is(instanceOf(ThingRemovedEvent.class)));
        receivedEvent = null;
    }

    @Test
    public void assertThatThingRegistryDelegatesConfigUpdateToThingHandler() {
        ThingUID thingUID = new ThingUID("binding:type:thing");
        Thing thing = ThingBuilder.create(THING_TYPE_UID, thingUID).build();
        ThingHandler thingHandler = new BaseThingHandler(thing) {

            @Override
            public void handleCommand(ChannelUID channelUID, Command command) {
            }

            @Override
            public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
                changedParameters = configurationParameters;
            }

            @Override
            public void initialize() {
                updateStatus(ThingStatus.ONLINE);
            }
        };

        thing.setHandler(thingHandler);

        ThingProvider thingProvider = new ThingProvider() {

            @Override
            public void addProviderChangeListener(ProviderChangeListener<Thing> listener) {
            }

            @Override
            public Collection<Thing> getAll() {
                return Set.of(thing);
            }

            @Override
            public void removeProviderChangeListener(ProviderChangeListener<Thing> listener) {
            }
        };
        registerService(thingProvider);

        ThingRegistry thingRegistry = getService(ThingRegistry.class);

        Map<String, Object> parameters = Map.ofEntries(entry("param1", "value1"), entry("param2", 1));

        thingRegistry.updateConfiguration(thingUID, parameters);

        assertThat(changedParameters.entrySet(), is(equalTo(parameters.entrySet())));
    }

    @Test
    public void assertThatThingRegistryThrowsExceptionForConfigUpdateOfNonExistingThing() {
        ThingRegistry thingRegistry = getService(ThingRegistry.class);
        ThingUID thingUID = new ThingUID(THING_TYPE_UID, "thing");
        Map<String, Object> parameters = Map.of("param", "value1");
        assertThrows(IllegalArgumentException.class, () -> thingRegistry.updateConfiguration(thingUID, parameters));
    }

    @Test
    public void assertThatCreateThingDelegatesToRegisteredThingHandlerFactory() {
        ThingTypeUID expectedThingTypeUID = THING_TYPE_UID;
        ThingUID expectedBridgeUID = new ThingUID(THING_TYPE_UID, THING2_ID);
        ThingUID expectedThingUID = new ThingUID(THING_TYPE_UID, expectedBridgeUID, THING1_ID);
        String expectedLabel = "Test Thing";
        Configuration expectedConfiguration = new Configuration();

        AtomicReference<Thing> thingResultWrapper = new AtomicReference<>();

        ThingRegistry thingRegistry = getService(ThingRegistry.class);

        ThingHandlerFactory thingHandlerFactory = new BaseThingHandlerFactory() {

            @Override
            public boolean supportsThingType(ThingTypeUID thingTypeUID) {
                return true;
            }

            @Override
            protected @Nullable ThingHandler createHandler(Thing thing) {
                return null;
            }

            @Override
            public @Nullable Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration,
                    @Nullable ThingUID thingUID, @Nullable ThingUID bridgeUID) {
                assertThat(thingTypeUID, is(expectedThingTypeUID));
                assertThat(configuration, is(expectedConfiguration));
                assertThat(thingUID, is(expectedThingUID));
                assertThat(bridgeUID, is(expectedBridgeUID));
                Thing thing = ThingBuilder.create(thingTypeUID, thingUID.getId()).withBridge(bridgeUID).build();
                thingResultWrapper.set(thing);
                return thing;
            }
        };

        registerThingHandlerFactory(thingHandlerFactory);

        Thing thing = thingRegistry.createThingOfType(expectedThingTypeUID, expectedThingUID, expectedBridgeUID,
                expectedLabel, expectedConfiguration);
        waitForAssert(() -> {
            assertTrue(thingResultWrapper.get() != null);
        });

        assertThat(thing, is(notNullValue()));
        if (thing != null) {
            assertThat(thing, is(thingResultWrapper.get()));
        }
    }

    private void registerThingHandlerFactory(ThingHandlerFactory thingHandlerFactory) {
        unregisterCurrentThingHandlerFactory();
        thingHandlerFactoryServiceReg = registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());
    }

    private void unregisterCurrentThingHandlerFactory() {
        if (thingHandlerFactoryServiceReg != null) {
            unregisterService(thingHandlerFactoryServiceReg);
            thingHandlerFactoryServiceReg = null;
        }
    }

    private void registerThingTypeProvider() {
        ThingType thingType = ThingTypeBuilder.instance(THING_TYPE_UID, "label").build();

        ThingTypeProvider thingTypeProvider = mock(ThingTypeProvider.class);
        when(thingTypeProvider.getThingType(any(ThingTypeUID.class), nullable(Locale.class))).thenReturn(thingType);
        registerService(thingTypeProvider);

        ThingTypeRegistry thingTypeRegistry = mock(ThingTypeRegistry.class);
        when(thingTypeRegistry.getThingType(any(ThingTypeUID.class))).thenReturn(thingType);
        registerService(thingTypeRegistry);
    }
}
