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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ManagedThingProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingProvider;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.events.ThingAddedEvent;
import org.eclipse.smarthome.core.thing.events.ThingRemovedEvent;
import org.eclipse.smarthome.core.thing.events.ThingUpdatedEvent;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;

/**
 * {@link ThingRegistryOSGiTest} tests the {@link ThingRegistry}.
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Kai Kreuzer - Moved createThing test from managed provider
 */
public class ThingRegistryOSGiTest extends JavaOSGiTest {

    ManagedThingProvider managedThingProvider;
    ServiceRegistration<?> thingHandlerFactoryServiceReg;

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("binding:type");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "id");
    private static final Thing THING = ThingBuilder.create(THING_TYPE_UID, THING_UID).build();
    private static final String THING1_ID = "testThing1";
    private static final String THING2_ID = "testThing2";

    Map<@NonNull String, @NonNull Object> changedParameters = null;
    Event receivedEvent = null;

    @Before
    public void setUp() {
        registerVolatileStorageService();
        managedThingProvider = getService(ManagedThingProvider.class);
        unregisterCurrentThingHandlerFactory();
    }

    @After
    public void teardown() {
        unregisterCurrentThingHandlerFactory();
        managedThingProvider.getAll().stream().forEach(it -> managedThingProvider.remove(it.getUID()));
    }

    @Test
    public void assertThatThingRegistryEventSubscribersReceiveEventsAboutThingChanges() {
        EventSubscriber thingRegistryEventSubscriber = new EventSubscriber() {

            @Override
            public Set<String> getSubscribedEventTypes() {
                Set<String> types = new HashSet<>();
                types.add(ThingAddedEvent.TYPE);
                types.add(ThingRemovedEvent.TYPE);
                types.add(ThingUpdatedEvent.TYPE);
                return types;
            }

            @Override
            public EventFilter getEventFilter() {
                return null;
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
            public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
            }

            @Override
            public void handleConfigurationUpdate(
                    @NonNull Map<@NonNull String, @NonNull Object> configurationParameters) {
                changedParameters = configurationParameters;
            }
        };

        thing.setHandler(thingHandler);

        ThingProvider thingProvider = new ThingProvider() {

            @Override
            public void addProviderChangeListener(ProviderChangeListener<@NonNull Thing> listener) {
            }

            @Override
            public Collection<@NonNull Thing> getAll() {
                return Collections.singleton(thing);
            }

            @Override
            public void removeProviderChangeListener(ProviderChangeListener<@NonNull Thing> listener) {
            }

        };
        registerService(thingProvider);

        ThingRegistry thingRegistry = getService(ThingRegistry.class);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param1", "value1");
        parameters.put("param2", 1);

        thingRegistry.updateConfiguration(thingUID, parameters);

        assertThat(changedParameters.entrySet(), is(equalTo(parameters.entrySet())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void assertThatThingRegistryThrowsExceptionForConfigUpdateOfNonExistingThing() {
        ThingRegistry thingRegistry = getService(ThingRegistry.class);
        ThingUID thingUID = new ThingUID(THING_TYPE_UID, "binding:type:thing");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param", "value1");
        thingRegistry.updateConfiguration(thingUID, parameters);
    }

    @Test
    public void assertThatCreateThingDelegatesToRegisteredThingHandlerFactory() {
        ThingTypeUID expectedThingTypeUID = THING_TYPE_UID;
        ThingUID expectedThingUID = new ThingUID(THING_TYPE_UID, THING1_ID);
        Configuration expectedConfiguration = new Configuration();
        ThingUID expectedBridgeUID = new ThingUID(THING_TYPE_UID, THING2_ID);
        String expectedLabel = "Test Thing";

        AtomicReference<Thing> thingResultWrapper = new AtomicReference<Thing>();

        ThingRegistry thingRegistry = getService(ThingRegistry.class);

        ThingHandlerFactory thingHandlerFactory = new BaseThingHandlerFactory() {

            @Override
            public boolean supportsThingType(@NonNull ThingTypeUID thingTypeUID) {
                return true;
            }

            @Override
            protected @Nullable ThingHandler createHandler(@NonNull Thing thing) {
                return null;
            }

            @Override
            public @Nullable Thing createThing(@NonNull ThingTypeUID thingTypeUID, @NonNull Configuration configuration,
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
        assertThat(thing, is(thingResultWrapper.get()));
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

}
