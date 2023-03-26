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
package org.openhab.core.thing.internal.update;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;
import static org.openhab.core.thing.internal.ThingManagerImpl.PROPERTY_THING_TYPE_VERSION;

import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.service.ReadyService;
import org.openhab.core.test.SyntheticBundleInstaller;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.types.Command;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link ThingUpdateInstructionReader} and {@link ThingUpdateInstruction} implementations.
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ThingUpdateOSGiTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "thingUpdateTest.bundle";
    private static final String BINDING_ID = "testBinding";
    private static final ThingTypeUID ADD_CHANNEL_THING_TYPE_UID = new ThingTypeUID(BINDING_ID, "testThingTypeAdd");
    private static final ThingTypeUID ADD_GROUP_CHANNEL_THING_TYPE_UID = new ThingTypeUID(BINDING_ID,
            "testThingTypeGroupAdd");

    private static final ThingTypeUID UPDATE_CHANNEL_THING_TYPE_UID = new ThingTypeUID(BINDING_ID,
            "testThingTypeUpdate");
    private static final ThingTypeUID REMOVE_CHANNEL_THING_TYPE_UID = new ThingTypeUID(BINDING_ID,
            "testThingTypeRemove");
    private static final ThingTypeUID MULTIPLE_CHANNEL_THING_TYPE_UID = new ThingTypeUID(BINDING_ID,
            "testThingTypeMultiple");

    private static final String[] ADDED_TAGS = { "Tag1", "Tag2" };

    private static final String THING_ID = "thing";

    private @NonNullByDefault({}) Bundle testBundle;
    private @NonNullByDefault({}) BundleResolver bundleResolver;
    private @NonNullByDefault({}) ReadyService readyService;
    private @NonNullByDefault({}) ThingRegistry thingRegistry;
    private @NonNullByDefault({}) ManagedThingProvider managedThingProvider;
    private @NonNullByDefault({}) TestThingHandlerFactory thingHandlerFactory;

    @BeforeEach
    public void beforeEach() throws Exception {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
        bundleResolver = new BundleResolverImpl();
        registerService(bundleResolver, BundleResolver.class.getName(), properties);

        registerVolatileStorageService();

        testBundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME, "*.xml");
        assertThat(testBundle, is(notNullValue()));

        readyService = getService(ReadyService.class);
        assertThat(readyService, is(notNullValue()));

        thingRegistry = getService(ThingRegistry.class);
        assertThat(thingRegistry, is(notNullValue()));

        managedThingProvider = getService(ManagedThingProvider.class);
        assertThat(managedThingProvider, is(notNullValue()));

        thingHandlerFactory = new TestThingHandlerFactory();
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());
    }

    @AfterEach
    public void afterEach() throws Exception {
        testBundle.uninstall();
        managedThingProvider.getAll().forEach(t -> managedThingProvider.remove(t.getUID()));
    }

    @Test
    public void testSingleChannelAddition() {
        registerThingType(ADD_CHANNEL_THING_TYPE_UID);

        ChannelTypeUID channelTypeUID = new ChannelTypeUID(BINDING_ID, "testChannelTypeId");
        registerChannelTypes(channelTypeUID);

        ThingUID thingUID = new ThingUID(ADD_CHANNEL_THING_TYPE_UID, THING_ID);
        Thing thing = ThingBuilder.create(ADD_CHANNEL_THING_TYPE_UID, thingUID).build();
        managedThingProvider.add(thing);

        Thing updatedThing = assertThing(thing, 1);

        assertThat(updatedThing.getChannels(), hasSize(3));

        Channel channel1 = updatedThing.getChannel("testChannel1");
        assertChannel(channel1, channelTypeUID, null, null);

        Channel channel2 = updatedThing.getChannel("testChannel2");
        assertChannel(channel2, channelTypeUID, "Test Label", null);
        assertThat(channel2.getDefaultTags(), containsInAnyOrder(ADDED_TAGS));

        Channel channel3 = updatedThing.getChannel("testChannel3");
        assertChannel(channel3, channelTypeUID, "Test Label", "Test Description");
    }

    @Test
    public void testSingleChannelUpdate() {
        registerThingType(UPDATE_CHANNEL_THING_TYPE_UID);

        ChannelTypeUID channelTypeOldUID = new ChannelTypeUID(BINDING_ID, "testChannelOldTypeId");
        ChannelTypeUID channelTypeNewUID = new ChannelTypeUID(BINDING_ID, "testChannelNewTypeId");
        registerChannelTypes(channelTypeOldUID, channelTypeNewUID);

        ThingUID thingUID = new ThingUID(UPDATE_CHANNEL_THING_TYPE_UID, THING_ID);
        ChannelUID channelUID1 = new ChannelUID(thingUID, "testChannel1");
        ChannelUID channelUID2 = new ChannelUID(thingUID, "testChannel2");
        Configuration channelConfig = new Configuration(Map.of("foo", "bar"));

        Channel origChannel1 = ChannelBuilder.create(channelUID1).withType(channelTypeOldUID)
                .withConfiguration(channelConfig).build();
        Channel origChannel2 = ChannelBuilder.create(channelUID2).withType(channelTypeOldUID)
                .withConfiguration(channelConfig).build();

        Thing thing = ThingBuilder.create(UPDATE_CHANNEL_THING_TYPE_UID, thingUID)
                .withChannels(origChannel1, origChannel2).build();

        managedThingProvider.add(thing);

        Thing updatedThing = assertThing(thing, 1);
        assertThat(updatedThing.getChannels(), hasSize(2));

        Channel channel1 = updatedThing.getChannel(channelUID1);
        assertChannel(channel1, channelTypeNewUID, "New Test Label", null);
        assertThat(channel1.getConfiguration(), is(channelConfig));

        Channel channel2 = updatedThing.getChannel(channelUID2);
        assertChannel(channel2, channelTypeNewUID, null, null);
        assertThat(channel2.getConfiguration().getProperties(), is(anEmptyMap()));
    }

    @Test
    public void testSingleChannelRemoval() {
        registerThingType(REMOVE_CHANNEL_THING_TYPE_UID);

        ChannelTypeUID channelTypeUID = new ChannelTypeUID(BINDING_ID, "testChannelTypeId");
        registerChannelTypes(channelTypeUID);

        ThingUID thingUID = new ThingUID(REMOVE_CHANNEL_THING_TYPE_UID, THING_ID);
        ChannelUID channelUID = new ChannelUID(thingUID, "testChannel");

        Thing thing = ThingBuilder.create(REMOVE_CHANNEL_THING_TYPE_UID, thingUID)
                .withChannel(ChannelBuilder.create(channelUID).withType(channelTypeUID).build()).build();

        managedThingProvider.add(thing);

        Thing updatedThing = assertThing(thing, 1);
        assertThat(updatedThing.getChannels(), hasSize(0));
    }

    @Test
    public void testMultipleChannelUpdates() {
        registerThingType(MULTIPLE_CHANNEL_THING_TYPE_UID);

        ChannelTypeUID channelTypeOldUID = new ChannelTypeUID(BINDING_ID, "testChannelOldTypeId");
        ChannelTypeUID channelTypeNewUID = new ChannelTypeUID(BINDING_ID, "testChannelNewTypeId");
        registerChannelTypes(channelTypeOldUID, channelTypeNewUID);

        ThingUID thingUID = new ThingUID(MULTIPLE_CHANNEL_THING_TYPE_UID, THING_ID);
        ChannelUID channelUID0 = new ChannelUID(thingUID, "testChannel0");
        ChannelUID channelUID1 = new ChannelUID(thingUID, "testChannel1");

        Thing thing = ThingBuilder.create(MULTIPLE_CHANNEL_THING_TYPE_UID, thingUID)
                .withChannel(ChannelBuilder.create(channelUID0).withType(channelTypeOldUID).build())
                .withChannel(ChannelBuilder.create(channelUID1).withType(channelTypeOldUID).build()).build();

        managedThingProvider.add(thing);

        Thing updatedThing = assertThing(thing, 3);
        assertThat(updatedThing.getChannels(), hasSize(2));

        Channel channel1 = updatedThing.getChannel("testChannel1");
        assertChannel(channel1, channelTypeNewUID, "Test Label", null);

        Channel channel2 = updatedThing.getChannel("testChannel2");
        assertChannel(channel2, channelTypeOldUID, "TestLabel", null);
    }

    @Test
    public void testOnlyMatchingInstructionsUpdate() {
        registerThingType(MULTIPLE_CHANNEL_THING_TYPE_UID);

        ChannelTypeUID channelTypeOldUID = new ChannelTypeUID(BINDING_ID, "testChannelOldTypeId");
        registerChannelTypes(channelTypeOldUID);

        ThingUID thingUID = new ThingUID(MULTIPLE_CHANNEL_THING_TYPE_UID, THING_ID);
        ChannelUID channelUID0 = new ChannelUID(thingUID, "testChannel0");
        ChannelUID channelUID1 = new ChannelUID(thingUID, "testChannel1");

        Thing thing = ThingBuilder.create(MULTIPLE_CHANNEL_THING_TYPE_UID, thingUID)
                .withChannel(ChannelBuilder.create(channelUID0).withType(channelTypeOldUID).build())
                .withChannel(ChannelBuilder.create(channelUID1).withType(channelTypeOldUID).build())
                .withProperty(PROPERTY_THING_TYPE_VERSION, "2").build();

        managedThingProvider.add(thing);

        Thing updatedThing = assertThing(thing, 3);
        assertThat(updatedThing.getChannels(), hasSize(1));

        Channel channel1 = updatedThing.getChannel("testChannel1");
        assertChannel(channel1, channelTypeOldUID, null, null);
    }

    @Test
    public void testSingleChannelAdditionGroup() {
        registerThingType(ADD_GROUP_CHANNEL_THING_TYPE_UID);

        ChannelTypeUID channelTypeUID = new ChannelTypeUID(BINDING_ID, "testChannelTypeId");
        registerChannelTypes(channelTypeUID);

        ThingUID thingUID = new ThingUID(ADD_GROUP_CHANNEL_THING_TYPE_UID, THING_ID);
        Thing thing = ThingBuilder.create(ADD_GROUP_CHANNEL_THING_TYPE_UID, thingUID).build();
        managedThingProvider.add(thing);

        Thing updatedThing = assertThing(thing, 1);

        assertThat(updatedThing.getChannels(), hasSize(2));

        List<Channel> channels1 = updatedThing.getChannelsOfGroup("group1");
        assertThat(channels1, hasSize(1));
        assertChannel(channels1.get(0), channelTypeUID, null, null);

        List<Channel> channels2 = updatedThing.getChannelsOfGroup("group2");
        assertThat(channels2, hasSize(1));
        assertChannel(channels2.get(0), channelTypeUID, null, null);
    }

    @Test
    public void testNotModifiedIfHigherVersion() {
        registerThingType(ADD_CHANNEL_THING_TYPE_UID);

        ChannelTypeUID channelTypeUID = new ChannelTypeUID(BINDING_ID, "testChannelTypeId");
        registerChannelTypes(channelTypeUID);

        ThingUID thingUID = new ThingUID(ADD_CHANNEL_THING_TYPE_UID, THING_ID);
        Thing thing = ThingBuilder.create(ADD_CHANNEL_THING_TYPE_UID, thingUID)
                .withProperty(PROPERTY_THING_TYPE_VERSION, "1").build();
        managedThingProvider.add(thing);

        waitForAssert(() -> assertThat(thing.getStatus(), is(ThingStatus.ONLINE)));
        assertThat(thingRegistry.get(thingUID), is(sameInstance(thing)));
        assertThat(thing.getChannels(), is(emptyCollectionOf(Channel.class)));
    }

    private Thing assertThing(Thing oldThing, int expectedNewThingTypeVersion) {
        ThingUID thingUID = oldThing.getUID();

        waitForAssert(() -> {
            @Nullable
            Thing updatedThing = thingRegistry.get(thingUID);
            assertThat(updatedThing, is(not(sameInstance(oldThing))));
        });

        @Nullable
        Thing updatedThing = thingRegistry.get(thingUID);
        assertThat(updatedThing.getStatus(), is(ThingStatus.ONLINE));

        // check thing type version is upgraded
        @Nullable
        String thingTypeVersion = updatedThing.getProperties().get(PROPERTY_THING_TYPE_VERSION);
        assertThat(thingTypeVersion, is(Integer.toString(expectedNewThingTypeVersion)));

        return updatedThing;
    }

    private void assertChannel(@Nullable Channel channel, ChannelTypeUID channelTypeUID, @Nullable String label,
            @Nullable String description) {
        assertThat(channel, is(notNullValue()));
        assertThat(channel.getChannelTypeUID(), is(channelTypeUID));
        if (label != null) {
            assertThat(channel.getLabel(), is(label));
        } else {
            assertThat(channel.getLabel(), is(nullValue()));
        }
        if (description != null) {
            assertThat(channel.getDescription(), is(description));
        } else {
            assertThat(channel.getDescription(), is(nullValue()));
        }
    }

    private void registerThingType(ThingTypeUID thingTypeUID) {
        ThingType thingType = ThingTypeBuilder.instance(thingTypeUID, "label").build();

        ThingTypeProvider thingTypeProvider = mock(ThingTypeProvider.class);
        when(thingTypeProvider.getThingType(eq(thingTypeUID), nullable(Locale.class))).thenReturn(thingType);
        registerService(thingTypeProvider);

        ThingTypeRegistry thingTypeRegistry = mock(ThingTypeRegistry.class);
        when(thingTypeRegistry.getThingType(eq(thingTypeUID))).thenReturn(thingType);
        registerService(thingTypeRegistry);
    }

    private void registerChannelTypes(ChannelTypeUID... channelTypeUIDs) {
        ChannelTypeProvider channelTypeProvider = mock(ChannelTypeProvider.class);
        ChannelTypeRegistry channelTypeRegistry = mock(ChannelTypeRegistry.class);

        for (ChannelTypeUID channelTypeUID : channelTypeUIDs) {
            ChannelType channelType = ChannelTypeBuilder.state(channelTypeUID, "label", "Number").build();
            when(channelTypeProvider.getChannelType(eq(channelTypeUID), nullable(Locale.class)))
                    .thenReturn(channelType);
            when(channelTypeRegistry.getChannelType(eq(channelTypeUID))).thenReturn(channelType);
        }

        registerService(channelTypeProvider);
        registerService(channelTypeRegistry);
    }

    class TestThingHandlerFactory extends BaseThingHandlerFactory {
        Logger logger = LoggerFactory.getLogger(TestThingHandlerFactory.class);

        @Override
        public void activate(final ComponentContext ctx) {
            super.activate(ctx);
        }

        @Override
        public boolean supportsThingType(ThingTypeUID thingTypeUID) {
            return true;
        }

        @Override
        protected @Nullable ThingHandler createHandler(Thing thing) {
            return new BaseThingHandler(thing) {
                @Override
                public void initialize() {
                    updateStatus(ThingStatus.ONLINE);
                }

                @Override
                public void handleCommand(ChannelUID channelUID, Command command) {
                }
            };
        }
    }

    private class BundleResolverImpl implements BundleResolver {
        @Override
        public Bundle resolveBundle(@NonNullByDefault({}) Class<?> clazz) {
            // return the test bundle if the class is TestThingHandlerFactory
            if (clazz != null && clazz.equals(TestThingHandlerFactory.class)) {
                return testBundle;
            } else {
                return FrameworkUtil.getBundle(clazz);
            }
        }
    }
}
