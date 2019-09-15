/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.thing.factory;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.registry.Provider;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.DefaultSystemChannelTypeProvider;
import org.eclipse.smarthome.core.thing.ManagedThingProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.internal.SimpleThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelDefinitionBuilder;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.test.AsyncResultWrapper;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

/**
 * Tests for {@link ManagedThingProvider}.
 *
 * @author Oliver Libutzki - Initital contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class ManagedThingProviderOSGiTest extends JavaOSGiTest {

    private final static String BINDIND_ID = "testBinding";
    private final static String THING_TYPE_ID = "testThingType";
    private final static ThingTypeUID THING_TYPE_UID = new ThingTypeUID(BINDIND_ID, THING_TYPE_ID);
    private final static String THING1_ID = "testThing1";
    private final static String THING2_ID = "testThing2";

    private final static String THING_TYPE_ID_2 = "testThingType2";
    private final static ThingTypeUID THING_TYPE_UID_2 = new ThingTypeUID(BINDIND_ID, THING_TYPE_ID_2);
    private final static String CHANNEL_DEF_1_ID = "channel1";
    private final static String CHANNEL_DEF_2_ID = "channel2";
    private final static ChannelDefinition CHANNEL_DEF_1 = new ChannelDefinitionBuilder(CHANNEL_DEF_1_ID,
            DefaultSystemChannelTypeProvider.SYSTEM_BRIGHTNESS.getUID()).build();
    private final static ChannelDefinition CHANNEL_DEF_2 = new ChannelDefinitionBuilder(CHANNEL_DEF_2_ID,
            DefaultSystemChannelTypeProvider.SYSTEM_BRIGHTNESS.getUID()).build();
    private final static ThingType THING_TYPE_2 = ThingTypeBuilder.instance(THING_TYPE_UID_2, "test")
            .withChannelDefinitions(Arrays.asList(CHANNEL_DEF_1, CHANNEL_DEF_2)).build();

    private ManagedThingProvider managedThingProvider;
    private ProviderChangeListener<Thing> thingChangeListener;

    @Before
    public void setup() {
        registerVolatileStorageService();
        SimpleThingTypeProvider simpleThingTypeProvider = new SimpleThingTypeProvider(Arrays.asList(THING_TYPE_2));
        registerService(simpleThingTypeProvider);
        managedThingProvider = getService(ManagedThingProvider.class);
        assertThat(managedThingProvider, is(notNullValue()));
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

    @Test
    public void assertThatThingChannelCanBeModified() {
        Thing thing1 = ThingBuilder.create(THING_TYPE_UID_2, THING1_ID).build();
        managedThingProvider.add(thing1);
        Thing thing = managedThingProvider.get(thing1.getUID());
        assertThat(thing, is(notNullValue()));
        assertThat(thing.getChannels().size(), is(0));
        registerThingHandlerFactory(THING_TYPE_UID_2, t -> new BaseThingHandler(t) {
            @Override
            public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
            }

            @Override
            public void initialize() {
                updateStatus(ThingStatus.ONLINE);
            }
        });
        thing = managedThingProvider.get(thing1.getUID());
        assertThat(thing.getChannels().size(), is(2));
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

    private void registerThingHandlerFactory(ThingTypeUID thingTypeUID,
            Function<Thing, ThingHandler> thingHandlerProducer) {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getBundleContext()).thenReturn(bundleContext);

        TestThingHandlerFactory mockThingHandlerFactory = new TestThingHandlerFactory(thingTypeUID,
                thingHandlerProducer);
        mockThingHandlerFactory.activate(context);
        registerService(mockThingHandlerFactory, ThingHandlerFactory.class.getName());
    }

    private static class TestThingHandlerFactory extends BaseThingHandlerFactory {

        private final ThingTypeUID thingTypeUID;
        private final Function<Thing, ThingHandler> thingHandlerProducer;

        public TestThingHandlerFactory(ThingTypeUID thingTypeUID, Function<Thing, ThingHandler> thingHandlerProducer) {
            this.thingTypeUID = thingTypeUID;
            this.thingHandlerProducer = thingHandlerProducer;
        }

        @Override
        public void activate(ComponentContext context) {
            super.activate(context);
        }

        @Override
        public boolean supportsThingType(@NonNull ThingTypeUID thingTypeUID) {
            return this.thingTypeUID.equals(thingTypeUID);
        }

        @Override
        protected @Nullable ThingHandler createHandler(@NonNull Thing thing) {
            return thingHandlerProducer.apply(thing);
        }

        @Override
        public @Nullable Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration,
                @Nullable ThingUID thingUID, @Nullable ThingUID bridgeUID) {
            if (thingUID != null) {
                return ThingFactory.createThing(THING_TYPE_2, thingUID, configuration, bridgeUID,
                        getConfigDescriptionRegistry());
            } else {
                return null;
            }
        }

    }
}
