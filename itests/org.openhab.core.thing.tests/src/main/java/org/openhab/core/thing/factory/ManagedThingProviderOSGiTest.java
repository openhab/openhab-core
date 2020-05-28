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
package org.openhab.core.thing.factory;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.test.AsyncResultWrapper;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingManager;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.internal.BridgeImpl;
import org.openhab.core.thing.internal.ThingImpl;
import org.openhab.core.thing.type.BridgeType;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.types.Command;

/**
 * Tests for {@link ManagedThingProvider}.
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@NonNullByDefault
public class ManagedThingProviderOSGiTest extends JavaOSGiTest {

    private static final String BINDIND_ID = "testBinding";
    private static final String THING_TYPE_ID = "testThingType";
    private static final String BRIDGE_TYPE_ID = "testBridgeType";
    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID(BINDIND_ID, THING_TYPE_ID);
    private static final ChannelUID CHANNEL1_TYPE_UID = new ChannelUID("testBinding:testThingType:testThing1:ch1");
    private static final ChannelUID CHANNEL2_TYPE_UID = new ChannelUID("testBinding:testThingType:testThing1:ch2");
    private static final String THING1_ID = "testThing1";
    private static final String THING2_ID = "testThing2";

    private final Map<ThingTypeUID, ThingType> thingTypes = new HashMap<>();
    private final Map<ThingTypeUID, BridgeType> bridgeTypes = new HashMap<>();

    private @NonNullByDefault({}) ManagedThingProvider managedThingProvider;
    private @NonNullByDefault({}) ProviderChangeListener<@NonNull Thing> thingChangeListener;
    private @NonNullByDefault({}) ThingManager thingManager;

    @Before
    public void setup() {
        ThingTypeProvider thingTypeProvider = mock(ThingTypeProvider.class);
        when(thingTypeProvider.getThingType(any(), any())).thenAnswer(invocation -> {
            ThingType thingType = thingTypes.get(invocation.getArgument(0));
            if (thingType == null) {
                BridgeType bridgeType = bridgeTypes.get(invocation.getArgument(0));
                return bridgeType;
            }
            return thingType;
        });
        registerService(thingTypeProvider);

        registerVolatileStorageService();
        managedThingProvider = getService(ManagedThingProvider.class);
        assertThat(managedThingProvider, is(notNullValue()));
        thingManager = getService(ThingManager.class);
        assertThat(thingManager, is(notNullValue()));
        unregisterCurrentThingsChangeListener();
    }

    @After
    public void teardown() {
        unregisterCurrentThingsChangeListener();
        managedThingProvider.getAll().forEach(t -> managedThingProvider.remove(t.getUID()));
    }

    private void registerThingsChangeListener(ProviderChangeListener<Thing> thingChangeListener) {
        unregisterCurrentThingsChangeListener();
        this.thingChangeListener = thingChangeListener;
        managedThingProvider.addProviderChangeListener(this.thingChangeListener);
    }

    private void unregisterCurrentThingsChangeListener() {
        if (this.thingChangeListener != null) {
            managedThingProvider.removeProviderChangeListener(this.thingChangeListener);
        }
    }

    @Test
    public void assertThatAddedThingIsReturnedByGetThings() {
        Thing thing1 = ThingBuilder.create(THING_TYPE_UID, THING1_ID).build();
        managedThingProvider.add(thing1);
        Collection<Thing> things = managedThingProvider.getAll();
        assertThat(things.size(), is(1));
        assertThat(things.iterator().next(), is(thing1));
        Thing thing2 = ThingBuilder.create(THING_TYPE_UID, THING2_ID).build();
        managedThingProvider.add(thing2);
        things = managedThingProvider.getAll();
        // Check for exact size and if the collection contains every element.
        // So, the order of the elements is ignored.
        assertThat(things.size(), is(2));
        assertTrue(things.contains(thing1));
        assertTrue(things.contains(thing2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void assertThatTwiceAddedThingThrowsException() {
        Thing thing1 = ThingBuilder.create(THING_TYPE_UID, THING1_ID).build();
        Thing thing2 = ThingBuilder.create(THING_TYPE_UID, THING1_ID).build();
        managedThingProvider.add(thing1);
        managedThingProvider.add(thing2);
    }

    @Test
    public void assertThatRemovedThingIsNotReturnedByGetThings() {
        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING1_ID).build();
        managedThingProvider.add(thing);
        managedThingProvider.remove(thing.getUID());
        Collection<Thing> things = managedThingProvider.getAll();
        assertThat(things.size(), is(0));
    }

    @Test
    public void assertThatThingsChangeListenerIsNotifiedAboutAddedThing() {
        AsyncResultWrapper<Provider<Thing>> thingProviderWrapper = new AsyncResultWrapper<>();
        AsyncResultWrapper<Thing> thingWrapper = new AsyncResultWrapper<>();

        registerThingsChangeListener(new ProviderChangeListener<Thing>() {
            @Override
            public void added(Provider<Thing> provider, Thing thing) {
                thingProviderWrapper.set(provider);
                thingWrapper.set(thing);
            }

            @Override
            public void removed(Provider<Thing> provider, Thing thing) {
            }

            @Override
            public void updated(Provider<Thing> provider, Thing oldThing, Thing thing) {
            }
        });

        Thing thing1 = ThingBuilder.create(THING_TYPE_UID, THING1_ID).build();
        managedThingProvider.add(thing1);

        waitForAssert(() -> assertTrue(thingProviderWrapper.isSet()));
        waitForAssert(() -> assertTrue(thingWrapper.isSet()));

        assertThat(thingProviderWrapper.getWrappedObject(), is(managedThingProvider));
        assertThat(thingWrapper.getWrappedObject(), is(thing1));
    }

    @Test
    public void assertThatThingsChangeListenerIsNotifiedAboutUpdatedThing() {
        AsyncResultWrapper<Provider<Thing>> updatedThingProviderWrapper = new AsyncResultWrapper<>();
        AsyncResultWrapper<Thing> oldThingWrapper = new AsyncResultWrapper<>();
        AsyncResultWrapper<Thing> updatedThingWrapper = new AsyncResultWrapper<>();

        Thing thing1 = ThingBuilder.create(THING_TYPE_UID, THING1_ID).build();
        managedThingProvider.add(thing1);

        registerThingsChangeListener(new ProviderChangeListener<Thing>() {
            @Override
            public void added(Provider<Thing> provider, Thing thing) {
            }

            @Override
            public void removed(Provider<Thing> provider, Thing thing) {
            }

            @Override
            public void updated(Provider<Thing> provider, Thing oldThing, Thing thing) {
                updatedThingProviderWrapper.set(provider);
                oldThingWrapper.set(oldThing);
                updatedThingWrapper.set(thing);
            }
        });

        Thing thing2 = ThingBuilder.create(THING_TYPE_UID, THING1_ID).build();
        managedThingProvider.update(thing2);

        waitForAssert(() -> assertTrue(updatedThingProviderWrapper.isSet()));
        waitForAssert(() -> assertTrue(oldThingWrapper.isSet()));
        waitForAssert(() -> assertTrue(updatedThingWrapper.isSet()));

        assertThat(updatedThingProviderWrapper.getWrappedObject(), is(managedThingProvider));
        assertThat(oldThingWrapper.getWrappedObject(), is(thing1));
        assertThat(updatedThingWrapper.getWrappedObject(), is(thing2));
    }

    @Test
    public void assertSimpleThingIsStoredAndRetrievedAsWell() {
        assertThat(managedThingProvider.getAll().size(), is(0));

        ThingType thingType = ThingTypeBuilder.instance(BINDIND_ID, THING_TYPE_ID, "label").build();
        thingTypes.put(thingType.getUID(), thingType);

        Thing thing = new ThingImpl(thingType.getUID(), new ThingUID(thingType.getUID(), THING1_ID));
        ThingUID thingUID = thing.getUID();
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        managedThingProvider.add(thing);

        Collection<Thing> things = managedThingProvider.getAll();
        assertThat(things.size(), is(1));

        Thing result = managedThingProvider.remove(thingUID);

        assertThat(thing, is(instanceOf(Thing.class)));
        assertThat(result.getUID(), is(equalTo(thingUID)));
        assertThat(result.getThingTypeUID(), is(equalTo(thingTypeUID)));
        assertThat(result.getConfiguration(), is(not(nullValue())));
        assertThat(result.getProperties(), is(not(nullValue())));

        assertThat(managedThingProvider.getAll().size(), is(0));
    }

    @Test
    public void assertSimpleBridgeIsStoredAndRetrievedAsWell() {
        assertThat(managedThingProvider.getAll().size(), is(0));

        BridgeType thingType = ThingTypeBuilder.instance(BINDIND_ID, THING_TYPE_ID, "label").buildBridge();
        bridgeTypes.put(thingType.getUID(), thingType);

        Thing thing = new BridgeImpl(thingType.getUID(), new ThingUID(thingType.getUID(), THING2_ID));
        ThingUID thingUID = thing.getUID();
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        managedThingProvider.add(thing);

        Collection<Thing> things = managedThingProvider.getAll();
        assertThat(things.size(), is(1));

        Thing result = managedThingProvider.remove(thingUID);

        assertThat(thing, is(instanceOf(Bridge.class)));
        assertThat(result.getUID(), is(equalTo(thingUID)));
        assertThat(result.getThingTypeUID(), is(equalTo(thingTypeUID)));
        assertThat(result.getConfiguration(), is(not(nullValue())));
        assertThat(result.getProperties(), is(not(nullValue())));

        assertThat(managedThingProvider.getAll().size(), is(0));
    }

    @Test
    public void assertThingWithBridgeIsStoredAndRetrievedAsWell() {
        assertThat(managedThingProvider.getAll().size(), is(0));

        ThingType thingType = ThingTypeBuilder.instance(BINDIND_ID, THING_TYPE_ID, "label").build();
        thingTypes.put(thingType.getUID(), thingType);

        BridgeType bridgeType = ThingTypeBuilder.instance(BINDIND_ID, BRIDGE_TYPE_ID, "label").buildBridge();
        bridgeTypes.put(thingType.getUID(), bridgeType);

        Thing thing = new ThingImpl(thingType.getUID(), new ThingUID(thingType.getUID(), THING1_ID));
        thing.setBridgeUID(new ThingUID(bridgeType.getUID(), THING2_ID));
        ThingUID bridgeUID = thing.getBridgeUID();
        ThingUID thingUID = thing.getUID();
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        managedThingProvider.add(thing);

        Collection<Thing> things = managedThingProvider.getAll();
        assertThat(things.size(), is(1));

        Thing result = managedThingProvider.remove(thingUID);

        assertThat(thing, is(instanceOf(Thing.class)));
        assertThat(result.getUID(), is(equalTo(thingUID)));
        assertThat(result.getBridgeUID(), is(equalTo(bridgeUID)));
        assertThat(result.getThingTypeUID(), is(equalTo(thingTypeUID)));
        assertThat(result.getConfiguration(), is(not(nullValue())));
        assertThat(result.getProperties(), is(not(nullValue())));

        assertThat(managedThingProvider.getAll().size(), is(0));
    }

    @Test
    public void assertThingWithChannelsIsStoredAndRetrievedAsWell() {
        assertThat(managedThingProvider.getAll().size(), is(0));

        ThingType thingType = ThingTypeBuilder.instance(BINDIND_ID, THING_TYPE_ID, "label")
                .withSupportedBridgeTypeUIDs(emptyList()).build();
        thingTypes.put(thingType.getUID(), thingType);

        Configuration configuration = new Configuration();
        Thing thing = ThingFactory.createThing(thingType, new ThingUID(thingType.getUID(), THING1_ID), configuration,
                null);
        Channel channel1 = ChannelBuilder.create(CHANNEL1_TYPE_UID, "Color")
                .withDefaultTags(new HashSet<>(Arrays.asList("tag1", "tag2"))).build();
        Channel channel2 = ChannelBuilder.create(CHANNEL2_TYPE_UID, "Dimmer")
                .withDefaultTags(new HashSet<>(Arrays.asList("tag3"))).build();
        ((ThingImpl) thing).setChannels(Arrays.asList(channel1, channel2));
        ThingUID thingUID = thing.getUID();
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        managedThingProvider.add(thing);

        Collection<Thing> things = managedThingProvider.getAll();
        assertThat(things.size(), is(1));

        Thing result = managedThingProvider.remove(thingUID);

        assertThat(result.getChannels().size(), is(2));
        assertThat(result.getChannels().get(0).getUID(), is(equalTo(CHANNEL1_TYPE_UID)));
        assertThat(result.getChannels().get(0).getAcceptedItemType(), is(equalTo("Color")));
        assertThat(result.getChannels().get(0).getDefaultTags().contains("tag1"), is(true));
        assertThat(result.getChannels().get(0).getDefaultTags().contains("tag2"), is(true));
        assertThat(result.getChannels().get(0).getDefaultTags().contains("tag3"), is(false));
        assertThat(result.getChannels().get(1).getDefaultTags().contains("tag1"), is(false));
        assertThat(result.getChannels().get(1).getDefaultTags().contains("tag2"), is(false));
        assertThat(result.getChannels().get(1).getDefaultTags().contains("tag3"), is(true));
        assertThat(thing, is(instanceOf(Thing.class)));
        assertThat(result.getUID(), is(equalTo(thingUID)));
        assertThat(result.getThingTypeUID(), is(equalTo(thingTypeUID)));
        assertThat(result.getConfiguration(), is(not(nullValue())));
        assertThat(result.getProperties(), is(not(nullValue())));

        assertThat(managedThingProvider.getAll().size(), is(0));
    }

    @Test
    public void assertThingIsStoredAndRetrievedAsWell() throws Exception {
        assertThat(managedThingProvider.getAll().size(), is(0));

        ThingType thingType = ThingTypeBuilder.instance(BINDIND_ID, THING_TYPE_ID, "label").build();
        thingTypes.put(thingType.getUID(), thingType);

        Configuration configuration = new Configuration();
        configuration.put("p1", true);
        configuration.put("p2", new BigDecimal("5"));
        configuration.put("p3", new BigDecimal("2.3"));
        configuration.put("p4", null);
        configuration.put("p5", Arrays.asList(new BigDecimal("2.3"), new BigDecimal("2.4"), new BigDecimal("2.5")));
        Map<String, String> properties = new HashMap<>();
        properties.put("key1", "value1");
        properties.put("key2", "value2");
        Thing thing = ThingFactory.createThing(thingType, new ThingUID(thingType.getUID(), THING1_ID), configuration,
                null);
        ((ThingImpl) thing).setProperties(properties);
        ThingUID thingUID = thing.getUID();
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        managedThingProvider.add(thing);

        thing.setLocation("location");

        managedThingProvider.update(thing);

        Collection<Thing> things = managedThingProvider.getAll();
        assertThat(things.size(), is(1));

        Thing result = managedThingProvider.remove(thingUID);

        assertThat(result.getLocation(), is("location"));
        assertThat(result.getProperties().size(), is(2));
        assertThat(result.getProperties().get("key1"), is("value1"));
        assertThat(result.getProperties().get("key2"), is("value2"));
        assertThat(result.getConfiguration(), is(not(nullValue())));
        assertThat(result.getConfiguration().get("p1"), is(equalTo(true)));
        assertThat(((BigDecimal) result.getConfiguration().get("p2")).compareTo(new BigDecimal("5")), is(0));
        assertThat(((BigDecimal) result.getConfiguration().get("p3")).compareTo(new BigDecimal("2.3")), is(0));
        assertThat(result.getConfiguration().get("p4"), is(nullValue()));
        assertThat(result.getConfiguration().get("p5"), is(instanceOf(List.class)));
        assertThat(((List<?>) result.getConfiguration().get("p5")).size(), is(3));
        assertThat(thing, is(instanceOf(Thing.class)));
        assertThat(result.getUID(), is(equalTo(thingUID)));
        assertThat(result.getThingTypeUID(), is(equalTo(thingTypeUID)));
        assertThat(result.getConfiguration(), is(not(nullValue())));
        assertThat(result.getProperties(), is(not(nullValue())));

        assertThat(managedThingProvider.getAll().size(), is(0));
    }

    class SimpleThingHandler extends BaseThingHandler {

        SimpleThingHandler(Thing thing) {
            super(thing);
        }

        @Override
        public void handleCommand(ChannelUID channelUID, Command command) {
        }

        @Override
        public void initialize() {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    class SimpleThingHandlerFactory extends BaseThingHandlerFactory {
        private final Set<ThingHandler> handlers = new HashSet<>();

        @Override
        public boolean supportsThingType(ThingTypeUID thingTypeUID) {
            return true;
        }

        @Override
        protected @Nullable ThingHandler createHandler(Thing thing) {
            ThingHandler handler = new SimpleThingHandler(thing);
            handlers.add(handler);
            return handler;
        }

        public Set<ThingHandler> getHandlers() {
            return handlers;
        }
    }

    @Test
    public void assertCorrectThingHandlerAfterRetrieval() {
        assertThat(managedThingProvider.getAll().size(), is(0));

        ThingType thingType = ThingTypeBuilder.instance(BINDIND_ID, THING_TYPE_ID, "label").build();
        thingTypes.put(thingType.getUID(), thingType);

        Configuration configuration = new Configuration();
        Thing thing = ThingFactory.createThing(thingType, new ThingUID(thingType.getUID(), THING1_ID), configuration,
                null);
        ThingUID thingUID = thing.getUID();

        SimpleThingHandlerFactory thingHandlerFactory = new SimpleThingHandlerFactory();
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());

        managedThingProvider.add(thing);

        Collection<Thing> things = managedThingProvider.getAll();
        assertThat(things.size(), is(1));

        Thing result = managedThingProvider.get(thingUID);

        waitForAssert(() -> assertThat(result.getHandler(), is(not(nullValue()))));
        waitForAssert(() -> assertThat(result.getStatus(), is(ThingStatus.ONLINE)));

        thingManager.setEnabled(thing.getUID(), false);

        waitForAssert(() -> assertThat(result.getHandler(), is(nullValue())));
        waitForAssert(() -> assertThat(result.getStatusInfo().getStatusDetail(), is(ThingStatusDetail.DISABLED)));

        thingManager.setEnabled(thing.getUID(), true);

        waitForAssert(() -> assertThat(result.getHandler(), is(not(nullValue()))));
        waitForAssert(() -> assertThat(result.getStatus(), is(ThingStatus.ONLINE)));

        thing.setHandler(null);

        Thing updatedResult = managedThingProvider.get(thingUID);
        waitForAssert(() -> assertThat(updatedResult.getHandler(), is(nullValue())));

        managedThingProvider.remove(thingUID);

        assertThat(managedThingProvider.getAll().size(), is(0));
    }
}
