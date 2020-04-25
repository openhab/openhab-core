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
package org.openhab.core.thing.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingManager;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.binding.builder.BridgeBuilder;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.link.ManagedItemChannelLinkProvider;
import org.openhab.core.thing.type.ChannelDefinitionBuilder;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeBuilder;
import org.openhab.core.thing.type.ChannelGroupTypeProvider;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.types.Command;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;

/**
 * OSGi tests for {@link ThingManager}.
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Christoph Weitkamp - Added preconfigured ChannelGroupBuilder
 */
@NonNullByDefault
public class ThingManagerOSGiJavaTest extends JavaOSGiTest {

    private static final String CONFIG_PARAM_NAME = "test";

    private static final String BINDING_ID = "binding";
    private static final String CHANNEL_ID = "channel";

    private static final ChannelTypeUID CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, CHANNEL_ID);

    private static final String CHANNEL_GROUP_ID = "channel-group";
    private static final ChannelGroupTypeUID CHANNEL_GROUP_TYPE_UID = new ChannelGroupTypeUID(BINDING_ID,
            CHANNEL_GROUP_ID);

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("binding:thing");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "thing");

    private static final ThingTypeUID BRIDGE_TYPE_UID = new ThingTypeUID("binding:bridge");
    private static final ThingUID BRIDGE_UID = new ThingUID(BRIDGE_TYPE_UID, "bridge");

    private static final ChannelUID CHANNEL_UID = new ChannelUID(THING_UID, CHANNEL_ID);

    private static final ChannelGroupUID CHANNEL_GROUP_UID = new ChannelGroupUID(THING_UID, CHANNEL_GROUP_ID);

    private @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @NonNullByDefault({}) ManagedThingProvider managedThingProvider;
    private @NonNullByDefault({}) ThingRegistry thingRegistry;
    private @NonNullByDefault({}) ReadyService readyService;
    private @NonNullByDefault({}) Storage<Boolean> storage;
    private @NonNullByDefault({}) ThingManager thingManager;

    private @NonNullByDefault({}) URI configDescriptionChannel;
    private @NonNullByDefault({}) URI configDescriptionThing;
    private @NonNullByDefault({}) Thing thing;

    @Before
    public void setUp() throws Exception {
        configDescriptionChannel = new URI("test:channel");
        configDescriptionThing = new URI("test:test");
        thing = ThingBuilder.create(THING_TYPE_UID, THING_UID).withChannels(Collections.singletonList( //
                ChannelBuilder.create(CHANNEL_UID, "Switch").withLabel("Test Label").withDescription("Test Description")
                        .withType(CHANNEL_TYPE_UID).withDefaultTags(Collections.singleton("Test Tag")).build() //
        )).build();
        registerVolatileStorageService();

        managedThingProvider = getService(ManagedThingProvider.class);

        itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);

        thingRegistry = getService(ThingRegistry.class);
        assertNotNull(thingRegistry);

        StorageService storageService;
        storageService = getService(StorageService.class);
        assertNotNull(storageService);
        storage = storageService.getStorage("thing_status_storage");

        itemChannelLinkRegistry = getService(ItemChannelLinkRegistry.class);
        assertNotNull(itemChannelLinkRegistry);

        readyService = getService(ReadyService.class);
        assertNotNull(readyService);

        thingManager = getService(ThingManager.class);
        assertNotNull(thingManager);

        waitForAssert(() -> {
            try {
                assertThat(
                        bundleContext.getServiceReferences(ReadyMarker.class,
                                "(esh.xmlThingTypes=" + bundleContext.getBundle().getSymbolicName() + ")"),
                        is(notNullValue()));
            } catch (InvalidSyntaxException e) {
                throw new RuntimeException(e);
            }
        });
        waitForAssert(() -> {
            try {
                assertThat(bundleContext.getServiceReferences(ChannelItemProvider.class, null), is(notNullValue()));
            } catch (InvalidSyntaxException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @After
    public void teardown() throws Exception {
        managedThingProvider.getAll().forEach(it -> {
            managedThingProvider.remove(it.getUID());
        });
        storage.remove(THING_UID.getAsString());
    }

    @Test
    public void testInitializeCallsThingUpdated() throws Exception {
        registerThingTypeProvider();
        AtomicReference<ThingHandlerCallback> thc = new AtomicReference<>();
        AtomicReference<Boolean> initializeRunning = new AtomicReference<>(false);
        registerThingHandlerFactory(THING_TYPE_UID, thing -> {
            ThingHandler mockHandler = mock(ThingHandler.class);
            doAnswer(a -> {
                thc.set((ThingHandlerCallback) a.getArguments()[0]);
                return null;
            }).when(mockHandler).setCallback(ArgumentMatchers.isA(ThingHandlerCallback.class));
            doAnswer(a -> {
                initializeRunning.set(true);

                // call thingUpdated() from within initialize()
                thc.get().thingUpdated(thing);

                // hang on a little to provoke a potential dead-lock
                Thread.sleep(1000);

                initializeRunning.set(false);
                return null;
            }).when(mockHandler).initialize();
            when(mockHandler.getThing()).thenReturn(thing);
            return mockHandler;
        });

        new Thread((Runnable) () -> managedThingProvider.add(thing)).start();

        waitForAssert(() -> {
            assertThat(thing.getStatus(), is(ThingStatus.INITIALIZING));
        });

        // ensure it didn't run into a dead-lock which gets resolved by the SafeCaller.
        waitForAssert(() -> {
            assertThat(initializeRunning.get(), is(false));
        }, SafeCaller.DEFAULT_TIMEOUT - 100, 50);
    }

    @Test
    public void testThatIsLinkedReturnsFalse() throws Exception {
        AtomicReference<ThingHandlerCallback> thc = initializeThingHandlerCallback();

        assertFalse(thc.get().isChannelLinked(CHANNEL_UID));
    }

    @Test
    public void testThatIsLinkedReturnsTrue() throws Exception {
        AtomicReference<ThingHandlerCallback> thc = initializeThingHandlerCallback();

        ManagedItemChannelLinkProvider managedItemChannelLinkProvider = getService(
                ManagedItemChannelLinkProvider.class);
        assertNotNull(managedItemChannelLinkProvider);

        managedItemChannelLinkProvider.add(new ItemChannelLink("item", CHANNEL_UID));

        assertTrue(thc.get().isChannelLinked(CHANNEL_UID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateChannelBuilderThrowsIllegalArgumentException() throws Exception {
        AtomicReference<ThingHandlerCallback> thc = initializeThingHandlerCallback();

        thc.get().createChannelBuilder(CHANNEL_UID, new ChannelTypeUID(BINDING_ID, "invalid-channel"));
    }

    @Test
    public void testCreateChannelBuilder() throws Exception {
        AtomicReference<ThingHandlerCallback> thc = initializeThingHandlerCallback();

        ChannelBuilder channelBuilder = thc.get().createChannelBuilder(CHANNEL_UID, CHANNEL_TYPE_UID);
        assertNotNull(channelBuilder);
        validateChannel(channelBuilder.build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEditChannelBuilderThrowsIllegalArgumentException() throws Exception {
        AtomicReference<ThingHandlerCallback> thc = initializeThingHandlerCallback();

        thc.get().editChannel(thing, new ChannelUID(THING_UID, "invalid-channel"));
    }

    @Test
    public void testEditChannelBuilder() throws Exception {
        AtomicReference<ThingHandlerCallback> thc = initializeThingHandlerCallback();

        ChannelBuilder channelBuilder = thc.get().editChannel(thing, CHANNEL_UID);
        assertNotNull(channelBuilder);
        validateChannel(channelBuilder.build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateChannelGroupBuilderThrowsIllegalArgumentException() throws Exception {
        AtomicReference<ThingHandlerCallback> thc = initializeThingHandlerCallback();

        thc.get().createChannelBuilders(CHANNEL_GROUP_UID,
                new ChannelGroupTypeUID(BINDING_ID, "invalid-channel-group"));
    }

    @Test
    public void testCreateChannelGroupBuilder() throws Exception {
        AtomicReference<ThingHandlerCallback> thc = initializeThingHandlerCallback();

        List<ChannelBuilder> channelBuilders = thc.get().createChannelBuilders(CHANNEL_GROUP_UID,
                CHANNEL_GROUP_TYPE_UID);
        assertNotNull(channelBuilders);
        assertEquals(1, channelBuilders.size());

        for (ChannelBuilder channelBuilder : channelBuilders) {
            assertNotNull(channelBuilder);
            validateChannel(channelBuilder.build());
        }
    }

    private void validateChannel(Channel channel) {
        assertNotNull(channel);
        assertEquals("Test Label", channel.getLabel());
        assertEquals("Test Description", channel.getDescription());
        assertEquals("Switch", channel.getAcceptedItemType());
        assertEquals(CHANNEL_TYPE_UID, channel.getChannelTypeUID());
        assertNotNull(channel.getDefaultTags());
        assertEquals(1, channel.getDefaultTags().size());
        assertEquals("Test Tag", channel.getDefaultTags().iterator().next());
    }

    @Test
    public void testInitializeOnlyIfInitializable() throws Exception {
        registerThingTypeProvider();
        registerChannelTypeProvider();
        registerThingHandlerFactory(THING_TYPE_UID, thing -> new BaseThingHandler(thing) {
            @Override
            public void handleCommand(ChannelUID channelUID, Command command) {
            }
        });

        ConfigDescriptionProvider mockConfigDescriptionProvider = mock(ConfigDescriptionProvider.class);
        List<ConfigDescriptionParameter> parameters = Collections.singletonList( //
                ConfigDescriptionParameterBuilder.create(CONFIG_PARAM_NAME, Type.TEXT).withRequired(true).build() //
        );
        registerService(mockConfigDescriptionProvider, ConfigDescriptionProvider.class.getName());

        // verify a missing mandatory thing config prevents it from getting initialized
        when(mockConfigDescriptionProvider.getConfigDescription(eq(configDescriptionThing), any()))
                .thenReturn(new ConfigDescription(configDescriptionThing, parameters));
        assertThingStatus(Collections.emptyMap(), Collections.emptyMap(), ThingStatus.UNINITIALIZED,
                ThingStatusDetail.HANDLER_CONFIGURATION_PENDING);

        // verify a missing mandatory channel config prevents it from getting initialized
        when(mockConfigDescriptionProvider.getConfigDescription(eq(configDescriptionChannel), any()))
                .thenReturn(new ConfigDescription(configDescriptionChannel, parameters));
        assertThingStatus(Collections.singletonMap(CONFIG_PARAM_NAME, "value"), Collections.emptyMap(),
                ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_CONFIGURATION_PENDING);

        // verify a satisfied config does not prevent it from getting initialized anymore
        assertThingStatus(Collections.singletonMap(CONFIG_PARAM_NAME, "value"),
                Collections.singletonMap(CONFIG_PARAM_NAME, "value"), ThingStatus.ONLINE, ThingStatusDetail.NONE);
    }

    @Test
    public void testChildHandlerInitializedReplacedUnitializedThing() {
        Semaphore childHandlerInitializedSemaphore = new Semaphore(1);
        Semaphore thingUpdatedSemapthore = new Semaphore(1);

        registerThingHandlerFactory(BRIDGE_TYPE_UID, bridge -> new BaseBridgeHandler((Bridge) bridge) {
            @Override
            public void handleCommand(ChannelUID channelUID, Command command) {
            }

            @Override
            public void initialize() {
                updateStatus(ThingStatus.ONLINE);
            }

            @Override
            public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
                try {
                    childHandlerInitializedSemaphore.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };
        });
        registerThingHandlerFactory(THING_TYPE_UID, thing -> new BaseThingHandler(thing) {
            @Override
            public void handleCommand(ChannelUID channelUID, Command command) {
            }

            @Override
            public void initialize() {
                if (getBridge() == null) {
                    throw new RuntimeException("Fail because of missing bridge");
                }
                updateStatus(ThingStatus.ONLINE);
            }

            @Override
            public void thingUpdated(Thing thing) {
                this.thing = thing;
                try {
                    thingUpdatedSemapthore.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };
        });

        Bridge bridge = BridgeBuilder.create(BRIDGE_TYPE_UID, BRIDGE_UID).build();
        managedThingProvider.add(bridge);
        waitForAssert(() -> {
            assertEquals(ThingStatus.ONLINE, bridge.getStatus());
        });

        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID).build();
        managedThingProvider.add(thing);
        waitForAssert(() -> {
            assertEquals(ThingStatus.UNINITIALIZED, thing.getStatus());
            assertEquals(ThingStatusDetail.HANDLER_INITIALIZING_ERROR, thing.getStatusInfo().getStatusDetail());
        });

        assertEquals(1, childHandlerInitializedSemaphore.availablePermits());

        Thing thing2 = ThingBuilder.create(THING_TYPE_UID, THING_UID).withBridge(BRIDGE_UID).build();
        managedThingProvider.update(thing2);
        waitForAssert(() -> {
            assertEquals(ThingStatus.ONLINE, thing2.getStatus());
        });

        // childHandlerInitialized(...) must be called
        waitForAssert(() -> assertEquals(0, childHandlerInitializedSemaphore.availablePermits()));

        // thingUpdated(...) is not called
        assertEquals(1, thingUpdatedSemapthore.availablePermits());
    }

    @Test
    public void testChildHandlerInitializedModifiedUninitializedThing() {
        Semaphore childHandlerInitializedSemaphore = new Semaphore(1);
        Semaphore thingUpdatedSemapthore = new Semaphore(1);

        registerThingHandlerFactory(BRIDGE_TYPE_UID, bridge -> new BaseBridgeHandler((Bridge) bridge) {
            @Override
            public void handleCommand(ChannelUID channelUID, Command command) {
            }

            @Override
            public void initialize() {
                updateStatus(ThingStatus.ONLINE);
            }

            @Override
            public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
                try {
                    childHandlerInitializedSemaphore.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };
        });
        registerThingHandlerFactory(THING_TYPE_UID, thing -> new BaseThingHandler(thing) {
            @Override
            public void handleCommand(ChannelUID channelUID, Command command) {
            }

            @Override
            public void initialize() {
                if (getBridge() == null) {
                    throw new RuntimeException("Fail because of missing bridge");
                }
                updateStatus(ThingStatus.ONLINE);
            }

            @Override
            public void thingUpdated(Thing thing) {
                this.thing = thing;
                try {
                    thingUpdatedSemapthore.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };
        });

        Bridge bridge = BridgeBuilder.create(BRIDGE_TYPE_UID, BRIDGE_UID).build();
        managedThingProvider.add(bridge);
        waitForAssert(() -> {
            assertEquals(ThingStatus.ONLINE, bridge.getStatus());
        });

        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID).build();
        managedThingProvider.add(thing);
        waitForAssert(() -> {
            assertEquals(ThingStatus.UNINITIALIZED, thing.getStatus());
            assertEquals(ThingStatusDetail.HANDLER_INITIALIZING_ERROR, thing.getStatusInfo().getStatusDetail());
        });

        assertEquals(1, childHandlerInitializedSemaphore.availablePermits());

        thing.setBridgeUID(bridge.getUID());
        managedThingProvider.update(thing);
        waitForAssert(() -> {
            assertEquals(ThingStatus.ONLINE, thing.getStatus());
        });

        // childHandlerInitialized(...) must be called
        waitForAssert(() -> assertEquals(0, childHandlerInitializedSemaphore.availablePermits()));

        // thingUpdated(...) is not called
        assertEquals(1, thingUpdatedSemapthore.availablePermits());
    }

    @Test
    public void testChildHandlerInitializedReplacedInitializedThing() {
        Semaphore childHandlerInitializedSemaphore = new Semaphore(1);
        Semaphore thingUpdatedSemapthore = new Semaphore(1);

        registerThingHandlerFactory(BRIDGE_TYPE_UID, bridge -> new BaseBridgeHandler((Bridge) bridge) {
            @Override
            public void handleCommand(ChannelUID channelUID, Command command) {
            }

            @Override
            public void initialize() {
                updateStatus(ThingStatus.ONLINE);
            }

            @Override
            public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
                try {
                    childHandlerInitializedSemaphore.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };
        });
        registerThingHandlerFactory(THING_TYPE_UID, thing -> new BaseThingHandler(thing) {
            @Override
            public void handleCommand(ChannelUID channelUID, Command command) {
            }

            @Override
            public void initialize() {
                updateStatus(ThingStatus.ONLINE);
            }

            @Override
            public void thingUpdated(Thing thing) {
                this.thing = thing;
                try {
                    thingUpdatedSemapthore.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };
        });

        Bridge bridge = BridgeBuilder.create(BRIDGE_TYPE_UID, BRIDGE_UID).build();
        managedThingProvider.add(bridge);
        waitForAssert(() -> {
            assertEquals(ThingStatus.ONLINE, bridge.getStatus());
        });

        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID).build();
        managedThingProvider.add(thing);
        waitForAssert(() -> {
            assertEquals(ThingStatus.ONLINE, thing.getStatus());
        });

        assertEquals(1, childHandlerInitializedSemaphore.availablePermits());

        Thing thing2 = ThingBuilder.create(THING_TYPE_UID, THING_UID).withBridge(BRIDGE_UID).build();
        managedThingProvider.update(thing2);
        waitForAssert(() -> {
            assertEquals(ThingStatus.ONLINE, thing2.getStatus());
        });

        // childHandlerInitialized(...) is not be called - framework calls ThingHandler.thingUpdated(...) instead.
        assertEquals(1, childHandlerInitializedSemaphore.availablePermits());

        // ThingHandler.thingUpdated(...) must be called
        assertEquals(0, thingUpdatedSemapthore.availablePermits());
    }

    @Test
    public void testSetEnabledWithHandler() throws Exception {
        registerThingTypeProvider();

        AtomicReference<ThingHandlerCallback> thingHandlerCallback = new AtomicReference<>();
        AtomicReference<Boolean> initializeInvoked = new AtomicReference<>(false);
        AtomicReference<Boolean> disposeInvoked = new AtomicReference<>(false);

        registerThingHandlerFactory(THING_TYPE_UID, thing -> {
            ThingHandler mockHandler = mock(ThingHandler.class);

            doAnswer(a -> {
                thingHandlerCallback.set((ThingHandlerCallback) a.getArguments()[0]);
                return null;
            }).when(mockHandler).setCallback(ArgumentMatchers.isA(ThingHandlerCallback.class));

            doAnswer(a -> {
                initializeInvoked.set(true);

                // call thingUpdated() from within initialize()
                thingHandlerCallback.get().thingUpdated(thing);

                // hang on a little to provoke a potential dead-lock
                Thread.sleep(1000);

                ThingStatusInfo thingStatusInfo = ThingStatusInfoBuilder
                        .create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build();
                thing.setStatusInfo(thingStatusInfo);

                return null;
            }).when(mockHandler).initialize();

            doAnswer(a -> {
                disposeInvoked.set(true);
                return null;
            }).when(mockHandler).dispose();

            when(mockHandler.getThing()).thenReturn(thing);
            return mockHandler;
        });

        ThingStatusInfo thingStatusInfo = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE).build();
        thing.setStatusInfo(thingStatusInfo);

        new Thread((Runnable) () -> managedThingProvider.add(thing)).start();

        waitForAssert(() -> {
            assertThat(initializeInvoked.get(), is(true));
            assertThat(thing.getStatus(), is(ThingStatus.INITIALIZING));
        });

        // Reset the flag
        initializeInvoked.set(false);

        // Disable the thing
        thingManager.setEnabled(THING_UID, false);

        waitForAssert(() -> {
            assertThat(storage.containsKey(THING_UID.getAsString()), is(true));
            assertThat(disposeInvoked.get(), is(true));
            assertThat(thing.getStatus(), is(ThingStatus.UNINITIALIZED));
            assertThat(thing.getStatusInfo().getStatusDetail(), is(ThingStatusDetail.DISABLED));
        }, SafeCaller.DEFAULT_TIMEOUT - 100, 50);

        // Reset the flag
        disposeInvoked.set(false);

        // Enable the thing
        thingManager.setEnabled(THING_UID, true);
        waitForAssert(() -> {
            assertThat(storage.containsKey(THING_UID.getAsString()), is(false));
            assertThat(thing.getStatus(), is(ThingStatus.ONLINE));
        });
    }

    @Test
    public void testSetEnabledWithoutHandlerFactory() throws Exception {
        registerThingTypeProvider();

        ThingStatusInfo thingStatusInfo = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE).build();
        thing.setStatusInfo(thingStatusInfo);

        new Thread((Runnable) () -> managedThingProvider.add(thing)).start();

        waitForAssert(() -> {
            assertThat(thingRegistry.get(THING_UID), is(notNullValue()));
            assertThat(thing.getStatus(), is(ThingStatus.UNINITIALIZED));
        });

        Thread.sleep(1000);
        thingManager.setEnabled(THING_UID, false);

        waitForAssert(() -> {
            assertThat(storage.containsKey(THING_UID.getAsString()), is(true));
            assertThat(thing.getStatus(), is(ThingStatus.UNINITIALIZED));
            assertThat(thing.getStatusInfo().getStatusDetail(), is(ThingStatusDetail.DISABLED));
        }, SafeCaller.DEFAULT_TIMEOUT - 100, 50);

        thingManager.setEnabled(THING_UID, true);

        waitForAssert(() -> {
            assertThat(storage.containsKey(THING_UID.getAsString()), is(false));
            assertThat(thing.getStatus(), is(ThingStatus.UNINITIALIZED));
            assertThat(thing.getStatusInfo().getStatusDetail(), is(ThingStatusDetail.HANDLER_MISSING_ERROR));
        }, SafeCaller.DEFAULT_TIMEOUT - 100, 50);
    }

    @Test
    public void testInitializeNotInvokedOnAlreadyEnabledThing() {
        AtomicReference<ThingHandlerCallback> thingHandlerCallback = new AtomicReference<>();
        AtomicReference<Boolean> initializeInvoked = new AtomicReference<>(false);
        AtomicReference<Boolean> disposeInvoked = new AtomicReference<>(false);

        registerThingHandlerFactory(THING_TYPE_UID, thing -> {
            ThingHandler mockHandler = mock(ThingHandler.class);

            doAnswer(a -> {
                thingHandlerCallback.set((ThingHandlerCallback) a.getArguments()[0]);
                return null;
            }).when(mockHandler).setCallback(ArgumentMatchers.isA(ThingHandlerCallback.class));

            doAnswer(a -> {
                initializeInvoked.set(true);

                // call thingUpdated() from within initialize()
                thingHandlerCallback.get().thingUpdated(thing);

                // hang on a little to provoke a potential dead-lock
                Thread.sleep(1000);

                ThingStatusInfo thingStatusInfo = ThingStatusInfoBuilder
                        .create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build();
                thing.setStatusInfo(thingStatusInfo);

                return null;
            }).when(mockHandler).initialize();

            doAnswer(a -> {
                disposeInvoked.set(true);
                return null;
            }).when(mockHandler).dispose();

            when(mockHandler.getThing()).thenReturn(thing);
            return mockHandler;
        });

        ThingStatusInfo enabledStatusInfo = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE).build();
        thing.setStatusInfo(enabledStatusInfo);
        new Thread((Runnable) () -> managedThingProvider.add(thing)).start();

        waitForAssert(() -> {
            assertThat(initializeInvoked.get(), is(true));
            assertThat(thing.getStatus(), is(ThingStatus.INITIALIZING));
        }, SafeCaller.DEFAULT_TIMEOUT - 100, 50);

        initializeInvoked.set(false);

        // enable the thing
        new Thread((Runnable) () -> thingManager.setEnabled(thing.getUID(), true)).start();

        waitForAssert(() -> {
            assertThat(thing.getStatus(), is(ThingStatus.ONLINE));
            assertThat(initializeInvoked.get(), is(false));
        }, SafeCaller.DEFAULT_TIMEOUT - 100, 50);
    }

    @Test
    public void testDisposeNotInvokedOnAlreadyDisabledThing() throws Exception {
        registerThingTypeProvider();

        AtomicReference<ThingHandlerCallback> thingHandlerCallback = new AtomicReference<>();
        AtomicReference<Boolean> initializeInvoked = new AtomicReference<>(false);
        AtomicReference<Boolean> disposeInvoked = new AtomicReference<>(false);

        registerThingHandlerFactory(THING_TYPE_UID, thing -> {
            ThingHandler mockHandler = mock(ThingHandler.class);

            doAnswer(a -> {
                thingHandlerCallback.set((ThingHandlerCallback) a.getArguments()[0]);
                return null;
            }).when(mockHandler).setCallback(ArgumentMatchers.isA(ThingHandlerCallback.class));

            doAnswer(a -> {
                initializeInvoked.set(true);

                // call thingUpdated() from within initialize()
                thingHandlerCallback.get().thingUpdated(thing);

                // hang on a little to provoke a potential dead-lock
                Thread.sleep(1000);

                ThingStatusInfo thingStatusInfo = ThingStatusInfoBuilder
                        .create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build();
                thing.setStatusInfo(thingStatusInfo);

                return null;
            }).when(mockHandler).initialize();

            doAnswer(a -> {
                disposeInvoked.set(true);
                return null;
            }).when(mockHandler).dispose();

            when(mockHandler.getThing()).thenReturn(thing);
            return mockHandler;
        });

        ThingStatusInfo thingStatusInfo = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE).build();
        thing.setStatusInfo(thingStatusInfo);

        new Thread((Runnable) () -> managedThingProvider.add(thing)).start();

        waitForAssert(() -> {
            assertThat(thing.getStatus(), is(ThingStatus.INITIALIZING));
        });

        // ensure it didn't run into a dead-lock which gets resolved by the SafeCaller.
        waitForAssert(() -> {
            assertThat(initializeInvoked.get(), is(true));
        }, SafeCaller.DEFAULT_TIMEOUT - 100, 50);

        thingManager.setEnabled(THING_UID, false);

        waitForAssert(() -> {
            assertThat(storage.containsKey(THING_UID.getAsString()), is(true));
            assertThat(disposeInvoked.get(), is(true));
            assertThat(thing.getStatus(), is(ThingStatus.UNINITIALIZED));
            assertThat(thing.getStatusInfo().getStatusDetail(), is(ThingStatusDetail.DISABLED));
        }, SafeCaller.DEFAULT_TIMEOUT - 100, 50);

        disposeInvoked.set(false);

        thingManager.setEnabled(THING_UID, false);

        waitForAssert(() -> {
            assertThat(storage.containsKey(THING_UID.getAsString()), is(true));
            assertThat(disposeInvoked.get(), is(false));
            assertThat(thing.getStatus(), is(ThingStatus.UNINITIALIZED));
            assertThat(thing.getStatusInfo().getStatusDetail(), is(ThingStatusDetail.DISABLED));
        }, SafeCaller.DEFAULT_TIMEOUT - 100, 50);
    }

    @Test
    public void testUpdateThing() throws Exception {
        registerThingTypeProvider();

        AtomicReference<ThingHandlerCallback> thingHandlerCallback = new AtomicReference<>();
        AtomicReference<Boolean> initializeInvoked = new AtomicReference<>(false);
        AtomicReference<Boolean> disposeInvoked = new AtomicReference<>(false);

        AtomicReference<Boolean> updatedInvoked = new AtomicReference<>(false);

        registerThingHandlerFactory(THING_TYPE_UID, thing -> {
            ThingHandler mockHandler = mock(ThingHandler.class);

            doAnswer(a -> {
                thingHandlerCallback.set((ThingHandlerCallback) a.getArguments()[0]);
                return null;
            }).when(mockHandler).setCallback(ArgumentMatchers.isA(ThingHandlerCallback.class));

            doAnswer(a -> {
                initializeInvoked.set(true);

                // call thingUpdated() from within initialize()
                thingHandlerCallback.get().thingUpdated(thing);

                // hang on a little to provoke a potential dead-lock
                Thread.sleep(1000);

                ThingStatusInfo thingStatusInfo = ThingStatusInfoBuilder
                        .create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build();
                thing.setStatusInfo(thingStatusInfo);

                return null;
            }).when(mockHandler).initialize();

            doAnswer(a -> {
                disposeInvoked.set(true);
                return null;
            }).when(mockHandler).dispose();

            doAnswer(a -> {
                updatedInvoked.set(true);
                return null;
            }).when(mockHandler).thingUpdated(any());

            when(mockHandler.getThing()).thenReturn(thing);
            return mockHandler;
        });

        ThingStatusInfo thingStatusInfo = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE).build();
        thing.setStatusInfo(thingStatusInfo);

        new Thread((Runnable) () -> managedThingProvider.add(thing)).start();

        waitForAssert(() -> {
            assertThat(thing.getStatus(), is(ThingStatus.INITIALIZING));
        });

        waitForAssert(() -> {
            assertThat(initializeInvoked.get(), is(true));
        }, SafeCaller.DEFAULT_TIMEOUT - 100, 50);

        new Thread((Runnable) () -> managedThingProvider.update(thing)).start();

        waitForAssert(() -> {
            assertThat(updatedInvoked.get(), is(true));
        }, SafeCaller.DEFAULT_TIMEOUT - 100, 50);

        updatedInvoked.set(false);

        thingManager.setEnabled(THING_UID, false);

        waitForAssert(() -> {
            assertThat(storage.containsKey(THING_UID.getAsString()), is(true));
            assertThat(disposeInvoked.get(), is(true));
            assertThat(thing.getStatus(), is(ThingStatus.UNINITIALIZED));
            assertThat(thing.getStatusInfo().getStatusDetail(), is(ThingStatusDetail.DISABLED));
        }, SafeCaller.DEFAULT_TIMEOUT - 100, 50);

        disposeInvoked.set(false);

        new Thread((Runnable) () -> managedThingProvider.update(thing)).start();

        waitForAssert(() -> {
            assertThat(updatedInvoked.get(), is(false));
        }, SafeCaller.DEFAULT_TIMEOUT - 100, 50);
    }

    @Test
    public void testStorageEntryRetainedOnThingRemoval() throws Exception {
        registerThingTypeProvider();

        AtomicReference<ThingHandlerCallback> thingHandlerCallback = new AtomicReference<>();
        AtomicReference<Boolean> initializeInvoked = new AtomicReference<>(false);
        AtomicReference<Boolean> disposeInvoked = new AtomicReference<>(false);

        registerThingHandlerFactory(THING_TYPE_UID, thing -> {
            ThingHandler mockHandler = mock(ThingHandler.class);

            doAnswer(a -> {
                thingHandlerCallback.set((ThingHandlerCallback) a.getArguments()[0]);
                return null;
            }).when(mockHandler).setCallback(ArgumentMatchers.isA(ThingHandlerCallback.class));

            doAnswer(a -> {

                initializeInvoked.set(true);

                // call thingUpdated() from within initialize()
                thingHandlerCallback.get().thingUpdated(thing);

                // hang on a little to provoke a potential dead-lock
                Thread.sleep(1000);

                ThingStatusInfo thingStatusInfo = ThingStatusInfoBuilder
                        .create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build();
                thing.setStatusInfo(thingStatusInfo);

                return null;
            }).when(mockHandler).initialize();

            doAnswer(a -> {
                disposeInvoked.set(true);
                return null;
            }).when(mockHandler).dispose();

            when(mockHandler.getThing()).thenReturn(thing);
            return mockHandler;
        });

        ThingStatusInfo thingStatusInfo = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE).build();
        thing.setStatusInfo(thingStatusInfo);

        new Thread((Runnable) () -> managedThingProvider.add(thing)).start();

        waitForAssert(() -> {
            assertThat(initializeInvoked.get(), is(true));
            assertThat(thing.getStatus(), is(ThingStatus.INITIALIZING));
        });

        initializeInvoked.set(false);

        thingManager.setEnabled(THING_UID, false);

        waitForAssert(() -> {
            assertThat(storage.containsKey(THING_UID.getAsString()), is(true));
            assertThat(disposeInvoked.get(), is(true));
            assertThat(thing.getStatus(), is(ThingStatus.UNINITIALIZED));
            assertThat(thing.getStatusInfo().getStatusDetail(), is(ThingStatusDetail.DISABLED));
        }, SafeCaller.DEFAULT_TIMEOUT - 100, 50);

        disposeInvoked.set(false);

        new Thread((Runnable) () -> managedThingProvider.remove(thing.getUID())).start();

        waitForAssert(() -> {
            assertThat(thingRegistry.get(thing.getUID()), is(equalTo(null)));
        }, SafeCaller.DEFAULT_TIMEOUT - 100, 50);

        assertThat(storage.containsKey(THING_UID.getAsString()), is(true));
    }

    private void assertThingStatus(Map<String, Object> propsThing, Map<String, Object> propsChannel, ThingStatus status,
            ThingStatusDetail statusDetail) {
        Configuration configThing = new Configuration(propsThing);
        Configuration configChannel = new Configuration(propsChannel);

        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID).withChannels(Collections.singletonList( //
                ChannelBuilder.create(CHANNEL_UID, "Switch").withType(CHANNEL_TYPE_UID).withConfiguration(configChannel)
                        .build() //
        )).withConfiguration(configThing).build();

        managedThingProvider.add(thing);

        waitForAssert(() -> {
            assertEquals(status, thing.getStatus());
            assertEquals(statusDetail, thing.getStatusInfo().getStatusDetail());
        });

        managedThingProvider.remove(thing.getUID());
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

    private void registerThingTypeProvider() throws Exception {
        ThingType thingType = ThingTypeBuilder.instance(THING_TYPE_UID, "label")
                .withConfigDescriptionURI(configDescriptionThing)
                .withChannelDefinitions(
                        Collections.singletonList(new ChannelDefinitionBuilder(CHANNEL_ID, CHANNEL_TYPE_UID).build()))
                .build();

        ThingTypeProvider mockThingTypeProvider = mock(ThingTypeProvider.class);
        when(mockThingTypeProvider.getThingType(eq(THING_TYPE_UID), any())).thenReturn(thingType);
        registerService(mockThingTypeProvider);
    }

    private void registerChannelTypeProvider() throws Exception {
        ChannelType channelType = ChannelTypeBuilder.state(CHANNEL_TYPE_UID, "Test Label", "Switch")
                .withDescription("Test Description").withCategory("Test Category").withTag("Test Tag")
                .withConfigDescriptionURI(new URI("test:channel")).build();

        ChannelTypeProvider mockChannelTypeProvider = mock(ChannelTypeProvider.class);
        when(mockChannelTypeProvider.getChannelType(eq(CHANNEL_TYPE_UID), any())).thenReturn(channelType);
        registerService(mockChannelTypeProvider);
    }

    private void registerChannelGroupTypeProvider() throws Exception {
        ChannelGroupType channelGroupType = ChannelGroupTypeBuilder.instance(CHANNEL_GROUP_TYPE_UID, "Test Group Label")
                .withDescription("Test Group Description").withCategory("Test Group Category")
                .withChannelDefinitions(
                        Collections.singletonList(new ChannelDefinitionBuilder(CHANNEL_ID, CHANNEL_TYPE_UID).build()))
                .build();

        ChannelGroupTypeProvider mockChannelGroupTypeProvider = mock(ChannelGroupTypeProvider.class);
        when(mockChannelGroupTypeProvider.getChannelGroupType(eq(CHANNEL_GROUP_TYPE_UID), any()))
                .thenReturn(channelGroupType);
        registerService(mockChannelGroupTypeProvider);
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
        public boolean supportsThingType(ThingTypeUID thingTypeUID) {
            return this.thingTypeUID.equals(thingTypeUID);
        }

        @Override
        protected @Nullable ThingHandler createHandler(Thing thing) {
            return thingHandlerProducer.apply(thing);
        }
    };

    private AtomicReference<ThingHandlerCallback> initializeThingHandlerCallback() throws Exception {
        registerThingTypeProvider();
        registerChannelTypeProvider();
        registerChannelGroupTypeProvider();
        AtomicReference<ThingHandlerCallback> thc = new AtomicReference<>();
        ThingHandlerFactory thingHandlerFactory = new BaseThingHandlerFactory() {
            @Override
            public boolean supportsThingType(ThingTypeUID thingTypeUID) {
                return true;
            }

            @Override
            protected @Nullable ThingHandler createHandler(Thing thing) {
                ThingHandler mockHandler = mock(ThingHandler.class);
                doAnswer(a -> {
                    thc.set((ThingHandlerCallback) a.getArguments()[0]);
                    return null;
                }).when(mockHandler).setCallback(any(ThingHandlerCallback.class));
                when(mockHandler.getThing()).thenReturn(thing);
                return mockHandler;
            }
        };
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());
        new Thread((Runnable) () -> managedThingProvider.add(thing)).start();

        waitForAssert(() -> {
            assertNotNull(thc.get());
        });
        return thc;
    }
}
