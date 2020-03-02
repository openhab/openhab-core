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

import static java.util.Collections.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.events.TopicEventFilter;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerUtils;
import org.openhab.core.service.ReadyService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingManager;
import org.openhab.core.thing.ThingProvider;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeMigrationService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.binding.builder.BridgeBuilder;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;
import org.openhab.core.thing.events.ThingEventFactory;
import org.openhab.core.thing.events.ThingStatusInfoChangedEvent;
import org.openhab.core.thing.events.ThingStatusInfoEvent;
import org.openhab.core.thing.i18n.ThingStatusInfoI18nLocalizationService;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.link.ManagedItemChannelLinkProvider;
import org.openhab.core.thing.testutil.i18n.DefaultLocaleSetter;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.types.Command;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

/**
 * {@link ThingManagerOSGiTest} tests the {@link ThingManager}.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class ThingManagerOSGiTest extends JavaOSGiTest {

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("binding:type");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "id");
    private static final ChannelUID CHANNEL_UID = new ChannelUID(THING_UID, "channel");
    private static final ChannelTypeUID CHANNEL_TYPE_UID = new ChannelTypeUID("binding:channelType");

    private ChannelTypeProvider channelTypeProvider;
    private EventPublisher eventPublisher;
    private ItemChannelLinkRegistry itemChannelLinkRegistry;
    private ItemRegistry itemRegistry;
    private ManagedItemChannelLinkProvider managedItemChannelLinkProvider;
    private ManagedThingProvider managedThingProvider;
    private ReadyService readyService;
    private Thing thing;

    @Before
    @SuppressWarnings("null")
    public void setUp() {
        thing = ThingBuilder.create(THING_TYPE_UID, THING_UID).withChannels(ChannelBuilder.create(CHANNEL_UID, "Switch")
                .withKind(ChannelKind.STATE).withType(CHANNEL_TYPE_UID).build()).build();

        registerVolatileStorageService();

        channelTypeProvider = mock(ChannelTypeProvider.class);
        when(channelTypeProvider.getChannelType(any(ChannelTypeUID.class), nullable(Locale.class)))
                .thenReturn(ChannelTypeBuilder.state(CHANNEL_TYPE_UID, "label", "Switch").build());
        registerService(channelTypeProvider);

        managedItemChannelLinkProvider = getService(ManagedItemChannelLinkProvider.class);
        managedThingProvider = getService(ManagedThingProvider.class);
        eventPublisher = getService(EventPublisher.class);

        itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);

        itemChannelLinkRegistry = getService(ItemChannelLinkRegistry.class);
        assertNotNull(itemChannelLinkRegistry);

        readyService = getService(ReadyService.class);
        assertNotNull(readyService);

        waitForAssert(() -> {
            try {
                assertThat(
                        bundleContext
                                .getServiceReferences(ReadyMarker.class,
                                        "(" + ThingManagerImpl.XML_THING_TYPE + "="
                                                + bundleContext.getBundle().getSymbolicName() + ")"),
                        is(notNullValue()));
            } catch (InvalidSyntaxException e) {
                fail("Failed to get service reference: " + e.getMessage());
            }
        });
        waitForAssert(() -> {
            try {
                assertThat(bundleContext.getServiceReferences(ChannelItemProvider.class, null), is(notNullValue()));
            } catch (InvalidSyntaxException e) {
                fail("Failed to get service reference: " + e.getMessage());
            }
        });

        Bundle bundle = mock(Bundle.class);
        when(bundle.getSymbolicName()).thenReturn("org.openhab.core.thing");

        BundleResolver bundleResolver = mock(BundleResolver.class);
        when(bundleResolver.resolveBundle(any())).thenReturn(bundle);

        ThingManagerImpl thingManager = (ThingManagerImpl) getService(ThingTypeMigrationService.class);
        thingManager.setBundleResolver(bundleResolver);
    }

    @After
    public void teardown() {
        managedThingProvider.getAll().forEach(t -> managedThingProvider.remove(t.getUID()));
        ComponentContext componentContext = mock(ComponentContext.class);
        when(componentContext.getProperties()).thenReturn(new Hashtable<>());
    }

    @Test
    @SuppressWarnings("null")
    public void thingManagerChangesTheThingType() {
        registerThingTypeProvider();

        ThingHandler thingHandler = mock(ThingHandler.class);
        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        managedThingProvider.add(thing);

        assertThat(thing.getThingTypeUID().getAsString(), is(equalTo(THING_TYPE_UID.getAsString())));

        ThingTypeUID newThingTypeUID = new ThingTypeUID("binding:type2");

        ThingTypeMigrationService migrator = getService(ThingTypeMigrationService.class);
        assertThat(migrator, is(not(nullValue())));

        migrator.migrateThingType(thing, newThingTypeUID, thing.getConfiguration());

        waitForAssert(
                () -> assertThat(thing.getThingTypeUID().getAsString(), is(equalTo(newThingTypeUID.getAsString()))));
    }

    @Test
    public void thingManagerChangesTheThingTypeCorrectlyEvenIfInitializeTakesLongAndCalledFromThere() {
        registerThingTypeProvider();

        ThingTypeUID newThingTypeUID = new ThingTypeUID("binding:type2");

        class ThingHandlerState {
            boolean initializeRunning;
            boolean raceCondition;
            boolean migrateBlocked;
            ThingHandlerCallback callback;
        }

        final ThingHandlerState state = new ThingHandlerState();

        ThingHandler thingHandler = mock(ThingHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                if (state.initializeRunning) {
                    state.raceCondition = true;
                }
                state.initializeRunning = true;
                long start = System.nanoTime();
                state.callback.migrateThingType(thing, newThingTypeUID, thing.getConfiguration());
                if (System.nanoTime() - start > TimeUnit.SECONDS.toNanos(1)) {
                    state.migrateBlocked = true;
                }
                Thread.sleep(3000);
                state.initializeRunning = false;
                return null;
            }
        }).when(thingHandler).initialize();

        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        managedThingProvider.add(thing);

        waitForAssert(
                () -> assertThat(thing.getThingTypeUID().getAsString(), is(equalTo(newThingTypeUID.getAsString()))));

        assertThat(state.migrateBlocked, is(false));
        assertThat(state.raceCondition, is(false));
    }

    @Test(expected = RuntimeException.class)
    @SuppressWarnings("null")
    public void thingManagerDoesNotChangeTheThingTypeWhenNewThingTypeIsNotRegistered() {
        ThingHandler thingHandler = mock(ThingHandler.class);
        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        managedThingProvider.add(thing);

        assertThat(thing.getThingTypeUID().getAsString(), is(equalTo(THING_TYPE_UID.getAsString())));

        ThingTypeUID newThingTypeUID = new ThingTypeUID("binding:type2");

        ThingTypeMigrationService migrator = getService(ThingTypeMigrationService.class);
        assertThat(migrator, is(not(null)));

        migrator.migrateThingType(thing, newThingTypeUID, thing.getConfiguration());
    }

    @Test
    public void thingManagerWaitsWithThingUpdatedUntilInitializeReturned() {
        registerThingTypeProvider();

        Thing thing2 = ThingBuilder.create(THING_TYPE_UID, THING_UID)
                .withChannels(singletonList(ChannelBuilder.create(CHANNEL_UID, "Switch").build())).build();

        class ThingHandlerState {
            boolean raceCondition;
            boolean initializeRunning;
            boolean thingUpdatedCalled;
        }

        final ThingHandlerState state = new ThingHandlerState();

        ThingHandler thingHandler = mock(ThingHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.initializeRunning = true;
                Thread.sleep(3000);
                state.initializeRunning = false;
                return null;
            }
        }).when(thingHandler).initialize();

        when(thingHandler.getThing()).thenReturn(thing);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.thingUpdatedCalled = true;
                if (state.initializeRunning) {
                    state.raceCondition = true;
                }
                return null;
            }
        }).when(thingHandler).thingUpdated(any(Thing.class));

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        new Thread(() -> managedThingProvider.add(thing)).start();
        waitForAssert(() -> assertThat(thing.getStatus(), is(ThingStatus.INITIALIZING)));

        new Thread(() -> managedThingProvider.update(thing2)).start();
        waitForAssert(() -> assertThat(state.thingUpdatedCalled, is(true)));

        assertThat(state.raceCondition, is(false));
    }

    @Test
    public void thingManagerCallsRegisterHandlerForAddedThing() {
        ThingHandler thingHandler = mock(ThingHandler.class);
        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        managedThingProvider.add(thing);

        verify(thingHandlerFactory, times(1)).registerHandler(thing);
    }

    @Test
    public void thingManagerCallsUnregisterHandlerForRemovedThing() {
        ThingHandler thingHandler = mock(ThingHandler.class);
        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        managedThingProvider.add(thing);
        managedThingProvider.remove(thing.getUID());

        waitForAssert(() -> verify(thingHandlerFactory, times(1)).removeThing(thing.getUID()));
        waitForAssert(() -> verify(thingHandlerFactory, times(1)).unregisterHandler(thing));
    }

    @Test
    public void thingManagerHandlesThingHandlerLifecycleCorrectly() {
        class ThingHandlerState {
            ThingHandlerCallback callback = null;
        }

        final ThingHandlerState state = new ThingHandlerState();

        ThingHandler thingHandler = mock(ThingHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.callback.statusUpdated(thing, ThingStatusInfoBuilder.create(ThingStatus.ONLINE).build());
                return null;
            }
        }).when(thingHandler).initialize();

        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        final ThingStatusInfo uninitializedNone = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE).build();
        assertThat(thing.getStatusInfo(), is(uninitializedNone));

        // add thing - provokes handler registration & initialization
        managedThingProvider.add(thing);
        waitForAssert(() -> verify(thingHandlerFactory, times(1)).registerHandler(thing));
        waitForAssert(() -> verify(thingHandler, times(1)).initialize());
        final ThingStatusInfo onlineNone = ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE)
                .build();
        waitForAssert(() -> assertThat(thing.getStatusInfo(), is(onlineNone)));

        // remove handler factory - provokes handler deregistration & disposal
        unregisterService(thingHandlerFactory);
        waitForAssert(() -> verify(thingHandlerFactory, times(1)).unregisterHandler(thing));
        waitForAssert(() -> verify(thingHandler, times(1)).dispose());
        final ThingStatusInfo uninitializedHandlerMissing = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_MISSING_ERROR).build();
        waitForAssert(() -> assertThat(thing.getStatusInfo(), is(uninitializedHandlerMissing)));

        // add handler factory - provokes handler registration & initialization
        registerService(thingHandlerFactory);
        waitForAssert(() -> verify(thingHandlerFactory, times(2)).registerHandler(thing));
        waitForAssert(() -> verify(thingHandler, times(2)).initialize());
        waitForAssert(() -> assertThat(thing.getStatusInfo(), is(onlineNone)));

        // remove thing - provokes handler deregistration & disposal
        managedThingProvider.remove(thing.getUID());
        waitForAssert(() -> verify(thingHandlerFactory, times(2)).unregisterHandler(thing));
        waitForAssert(() -> verify(thingHandler, times(2)).dispose());
        waitForAssert(() -> assertThat(thing.getStatusInfo(), is(uninitializedHandlerMissing)));
    }

    volatile int initCalledCounter = 0;
    volatile int disposedCalledCounter = 0;

    @Test
    public void thingManagerHandlesFailingHandlerInitializationCorrectly() {
        class ThingHandlerState {
            ThingHandlerCallback callback = null;
        }

        final ThingHandlerState state = new ThingHandlerState();

        Thing testThing = ThingBuilder.create(THING_TYPE_UID, THING_UID).withConfiguration(new Configuration()).build();
        testThing.getConfiguration().put("shouldFail", true);

        ThingHandler thingHandler = mock(ThingHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Boolean shouldFail = (Boolean) testThing.getConfiguration().get("shouldFail");
                if (shouldFail) {
                    throw new IllegalStateException("Invalid config!");
                } else {
                    state.callback.statusUpdated(testThing, ThingStatusInfoBuilder.create(ThingStatus.ONLINE).build());
                }
                return null;
            }
        }).when(thingHandler).initialize();

        when(thingHandler.getThing()).thenReturn(testThing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        final ThingStatusInfo uninitializedNone = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE).build();
        assertThat(testThing.getStatusInfo(), is(uninitializedNone));

        managedThingProvider.add(testThing);
        final ThingStatusInfo uninitializedError = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_INITIALIZING_ERROR)
                .withDescription("Invalid config!").build();
        waitForAssert(() -> assertThat(testThing.getStatusInfo(), is(uninitializedError)));

        testThing.getConfiguration().put("shouldFail", false);
        managedThingProvider.update(testThing);
        final ThingStatusInfo onlineNone = ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE)
                .build();
        waitForAssert(() -> assertThat(testThing.getStatusInfo(), is(onlineNone)));
    }

    @Test
    @SuppressWarnings("null")
    public void thingManagerHandlesBridgeThingHandlerLifeCycleCorrectly() {
        initCalledCounter = 0;
        disposedCalledCounter = 0;

        class BridgeHandlerState {
            boolean initCalled;
            int initCalledOrder;
            boolean disposedCalled;
            int disposedCalledOrder;
            ThingHandlerCallback callback;
            boolean callbackWasNull;
        }
        final BridgeHandlerState bridgeState = new BridgeHandlerState();

        Bridge bridge = BridgeBuilder
                .create(new ThingTypeUID("binding:test"), new ThingUID("binding:test:someBridgeUID-1")).build();

        BridgeHandler bridgeHandler = mock(BridgeHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(bridgeHandler).setCallback(any(ThingHandlerCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.initCalled = true;
                bridgeState.initCalledOrder = ++initCalledCounter;
                bridgeState.callback.statusUpdated(bridge,
                        ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build());
                return null;
            }
        }).when(bridgeHandler).initialize();

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.disposedCalled = true;
                bridgeState.disposedCalledOrder = ++disposedCalledCounter;
                bridgeState.callbackWasNull = bridgeState.callback == null;
                return null;
            }
        }).when(bridgeHandler).dispose();

        when(bridgeHandler.getThing()).thenReturn(bridge);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.callback.statusUpdated(bridge, ThingStatusInfoBuilder.create(ThingStatus.REMOVED).build());
                return null;
            }
        }).when(bridgeHandler).handleRemoval();

        class ThingHandlerState {
            boolean initCalled;
            int initCalledOrder;
            boolean disposedCalled;
            int disposedCalledOrder;
            ThingHandlerCallback callback;
            boolean callbackWasNull;
        }
        final ThingHandlerState thingState = new ThingHandlerState();

        Thing thing = ThingBuilder.create(new ThingTypeUID("binding:test"), new ThingUID("binding:test:someThingUID-1"))
                .withBridge(bridge.getUID()).build();

        ThingHandler thingHandler = mock(ThingHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                thingState.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                thingState.initCalled = true;
                thingState.initCalledOrder = ++initCalledCounter;
                bridgeState.callback.statusUpdated(thing,
                        ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build());
                return null;
            }
        }).when(thingHandler).initialize();

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                thingState.disposedCalled = true;
                thingState.disposedCalledOrder = ++disposedCalledCounter;
                thingState.callbackWasNull = thingState.callback == null;
                return null;
            }
        }).when(thingHandler).dispose();

        when(thingHandler.getThing()).thenReturn(thing);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.callback.statusUpdated(thing, ThingStatusInfoBuilder.create(ThingStatus.REMOVED).build());
                return null;
            }
        }).when(thingHandler).handleRemoval();

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        doAnswer(new Answer<ThingHandler>() {
            @Override
            public ThingHandler answer(InvocationOnMock invocation) throws Throwable {
                Thing thing = (Thing) invocation.getArgument(0);
                if (thing instanceof Bridge) {
                    return bridgeHandler;
                } else if (thing instanceof Thing) {
                    return thingHandler;
                }
                return null;
            }
        }).when(thingHandlerFactory).registerHandler(any(Thing.class));

        registerService(thingHandlerFactory);

        final ThingStatusInfo uninitializedNone = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE).build();
        assertThat(thing.getStatusInfo(), is(uninitializedNone));
        assertThat(bridge.getStatusInfo(), is(uninitializedNone));
        assertThat(bridgeState.initCalled, is(false));
        assertThat(bridgeState.initCalledOrder, is(0));
        assertThat(bridgeState.disposedCalled, is(false));
        assertThat(bridgeState.disposedCalledOrder, is(0));
        assertThat(thingState.initCalled, is(false));
        assertThat(thingState.initCalledOrder, is(0));
        assertThat(thingState.disposedCalled, is(false));
        assertThat(thingState.disposedCalled, is(false));
        assertThat(thingState.disposedCalledOrder, is(0));

        ThingRegistry thingRegistry = getService(ThingRegistry.class);
        assertThat(thingRegistry, not(nullValue()));

        // add thing - no thing initialization, because bridge is not available
        thingRegistry.add(thing);
        waitForAssert(() -> assertThat(thingState.initCalled, is(false)));
        final ThingStatusInfo bridgeUninitialized = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.BRIDGE_UNINITIALIZED).build();
        waitForAssert(() -> assertThat(thing.getStatusInfo(), is(bridgeUninitialized)));

        // add bridge - provokes bridge & thing initialization
        thingRegistry.add(bridge);
        waitForAssert(() -> assertThat(bridgeState.initCalled, is(true)));
        waitForAssert(() -> assertThat(bridgeState.initCalledOrder, is(1)));
        waitForAssert(() -> assertThat(thingState.initCalled, is(true)));
        waitForAssert(() -> assertThat(thingState.initCalledOrder, is(2)));
        bridgeState.initCalled = false;
        thingState.initCalled = false;
        final ThingStatusInfo onlineNone = ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE)
                .build();
        waitForAssert(() -> assertThat(thing.getStatusInfo(), is(onlineNone)));
        waitForAssert(() -> assertThat(bridge.getStatusInfo(), is(onlineNone)));

        // remove thing - provokes thing disposal
        bridgeState.callbackWasNull = false;
        thingState.callbackWasNull = false;
        thingRegistry.remove(thing.getUID());
        waitForAssert(() -> assertThat(thingState.disposedCalled, is(true)));
        waitForAssert(() -> assertThat(thingState.disposedCalledOrder, is(1)));
        thingState.disposedCalled = false;
        waitForAssert(() -> assertThat(bridge.getStatusInfo(), is(onlineNone)));
        final ThingStatusInfo thingUninitializedHandlerMissingError = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_MISSING_ERROR).build();
        waitForAssert(() -> assertThat(thing.getStatusInfo(), is(thingUninitializedHandlerMissingError)));
        assertThat(thingState.callbackWasNull, is(false));

        // add thing again - provokes thing initialization
        thingRegistry.add(thing);
        waitForAssert(() -> assertThat(thingState.initCalled, is(true)));
        waitForAssert(() -> assertThat(thingState.initCalledOrder, is(3)));
        thingState.initCalled = false;
        waitForAssert(() -> assertThat(thing.getStatusInfo(), is(onlineNone)));

        // remove bridge - provokes thing & bridge disposal
        bridgeState.callbackWasNull = false;
        thingState.callbackWasNull = false;
        thingRegistry.remove(bridge.getUID());
        waitForAssert(() -> assertThat(thingState.disposedCalled, is(true)));
        waitForAssert(() -> assertThat(thingState.disposedCalledOrder, is(2)));
        waitForAssert(() -> assertThat(bridgeState.disposedCalled, is(true)));
        waitForAssert(() -> assertThat(bridgeState.disposedCalledOrder, is(3)));
        thingState.disposedCalled = false;
        bridgeState.disposedCalled = false;
        waitForAssert(() -> assertThat(thing.getStatusInfo(), is(thingUninitializedHandlerMissingError)));
        waitForAssert(() -> assertThat(bridge.getStatusInfo(), is(thingUninitializedHandlerMissingError)));
        assertThat(bridgeState.callbackWasNull, is(false));
        assertThat(thingState.callbackWasNull, is(false));

        // add bridge again
        thingRegistry.add(bridge);
        waitForAssert(() -> assertThat(bridgeState.initCalled, is(true)));
        waitForAssert(() -> assertThat(bridgeState.initCalledOrder, is(4)));
        waitForAssert(() -> assertThat(thingState.initCalled, is(true)));
        waitForAssert(() -> assertThat(thingState.initCalledOrder, is(5)));
        bridgeState.initCalled = false;
        thingState.initCalled = false;
        waitForAssert(() -> assertThat(thing.getStatusInfo(), is(onlineNone)));
        waitForAssert(() -> assertThat(bridge.getStatusInfo(), is(onlineNone)));

        // unregister factory
        bridgeState.callbackWasNull = false;
        thingState.callbackWasNull = false;
        unregisterService(thingHandlerFactory);
        waitForAssert(() -> assertThat(thingState.disposedCalled, is(true)));
        waitForAssert(() -> assertThat(thingState.disposedCalledOrder, is(4)));
        waitForAssert(() -> assertThat(bridgeState.disposedCalled, is(true)));
        waitForAssert(() -> assertThat(bridgeState.disposedCalledOrder, is(5)));
        waitForAssert(() -> assertThat(thing.getStatusInfo(), is(thingUninitializedHandlerMissingError)));
        waitForAssert(() -> assertThat(bridge.getStatusInfo(), is(thingUninitializedHandlerMissingError)));
        assertThat(bridgeState.callbackWasNull, is(false));
        assertThat(thingState.callbackWasNull, is(false));
    }

    @Test
    public void thingManagerDoesNotDelegateUpdateEventsToItsSource() {
        registerThingTypeProvider();

        class ThingHandlerState {
            boolean handleCommandWasCalled;
            ThingHandlerCallback callback;
        }

        final ThingHandlerState state = new ThingHandlerState();

        ThingHandler thingHandler = mock(ThingHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.handleCommandWasCalled = true;
                state.callback.statusUpdated(thing, ThingStatusInfoBuilder.create(ThingStatus.ONLINE).build());
                return null;
            }
        }).when(thingHandler).handleCommand(any(ChannelUID.class), any(Command.class));

        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        String itemName = "name";
        managedThingProvider.add(thing);
        managedItemChannelLinkProvider.add(new ItemChannelLink(itemName, CHANNEL_UID));

        registerService(thingHandlerFactory);
        waitForAssert(() -> assertThat(itemRegistry.get(itemName), is(notNullValue())));

        state.callback.statusUpdated(thing, ThingStatusInfoBuilder.create(ThingStatus.ONLINE).build());

        // event should be delivered
        eventPublisher.post(ItemEventFactory.createCommandEvent(itemName, OnOffType.ON));
        waitForAssert(() -> assertThat(state.handleCommandWasCalled, is(true)));

        state.handleCommandWasCalled = false;

        // event should not be delivered, because the source is the same
        eventPublisher.post(ItemEventFactory.createCommandEvent(itemName, OnOffType.ON, CHANNEL_UID.toString()));

        waitFor(() -> state.handleCommandWasCalled);
        assertThat(state.handleCommandWasCalled, is(false));
    }

    @Test
    public void thingManagerHandlesStateUpdatesCorrectly() {
        registerThingTypeProvider();

        class ThingHandlerState {
            boolean thingUpdatedWasCalled;
            ThingHandlerCallback callback;
        }

        final ThingHandlerState state = new ThingHandlerState();

        ThingHandler thingHandler = mock(ThingHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.thingUpdatedWasCalled = true;
                return null;
            }
        }).when(thingHandler).thingUpdated(any(Thing.class));

        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        // Create item
        String itemName = "name";
        Item item = new StringItem(itemName);
        itemRegistry.add(item);

        managedThingProvider.add(thing);
        managedItemChannelLinkProvider.add(new ItemChannelLink(itemName, CHANNEL_UID));

        state.callback.statusUpdated(thing, ThingStatusInfoBuilder.create(ThingStatus.ONLINE).build());

        final List<Event> receivedEvents = new ArrayList<>();

        @NonNullByDefault
        EventSubscriber itemUpdateEventSubscriber = new EventSubscriber() {
            @Override
            public void receive(Event event) {
                receivedEvents.add(event);
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Collections.singleton(ItemStateEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return new TopicEventFilter("smarthome/items/.*/state");
            }
        };
        registerService(itemUpdateEventSubscriber);

        // thing manager posts the update to the event bus via EventPublisher
        state.callback.stateUpdated(CHANNEL_UID, new StringType("Value"));
        waitForAssert(() -> assertThat(receivedEvents.size(), is(1)));
        assertThat(receivedEvents.get(0), is(instanceOf(ItemStateEvent.class)));
        ItemStateEvent itemUpdateEvent = (ItemStateEvent) receivedEvents.get(0);
        assertThat(itemUpdateEvent.getTopic(), is("smarthome/items/name/state"));
        assertThat(itemUpdateEvent.getItemName(), is(itemName));
        assertThat(itemUpdateEvent.getSource(), is(CHANNEL_UID.toString()));
        assertThat(itemUpdateEvent.getItemState(), is(instanceOf(StringType.class)));
        assertThat(itemUpdateEvent.getItemState(), is("Value"));

        receivedEvents.clear();
        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID)
                .withChannel(ChannelBuilder.create(CHANNEL_UID, "Switch").build()).build();
        managedThingProvider.update(thing);

        state.callback.stateUpdated(CHANNEL_UID, new StringType("Value"));
        waitForAssert(() -> assertThat(receivedEvents.size(), is(1)));

        assertThat(receivedEvents.get(0), is(instanceOf(ItemStateEvent.class)));
        itemUpdateEvent = (ItemStateEvent) receivedEvents.get(0);
        assertThat(itemUpdateEvent.getTopic(), is("smarthome/items/name/state"));
        assertThat(itemUpdateEvent.getItemName(), is(itemName));
        assertThat(itemUpdateEvent.getSource(), is(CHANNEL_UID.toString()));
        assertThat(itemUpdateEvent.getItemState(), is(instanceOf(StringType.class)));
        assertThat(itemUpdateEvent.getItemState(), is("Value"));
        waitForAssert(() -> assertThat(state.thingUpdatedWasCalled, is(true)));
    }

    @Test
    public void thingManagerHandlesThingStatusUpdatesOnlineAndOfflineCorrectly() {
        class ThingHandlerState {
            ThingHandlerCallback callback;
        }

        final ThingHandlerState state = new ThingHandlerState();

        ThingHandler thingHandler = mock(ThingHandler.class);

        managedThingProvider.add(thing);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));
        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        ThingStatusInfo statusInfo = ThingStatusInfoBuilder.create(ThingStatus.UNKNOWN, ThingStatusDetail.NONE).build();
        state.callback.statusUpdated(thing, statusInfo);
        assertThat(thing.getStatusInfo(), is(statusInfo));

        statusInfo = ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build();
        state.callback.statusUpdated(thing, statusInfo);
        assertThat(thing.getStatusInfo(), is(statusInfo));

        statusInfo = ThingStatusInfoBuilder.create(ThingStatus.UNKNOWN, ThingStatusDetail.NONE).build();
        state.callback.statusUpdated(thing, statusInfo);
        assertThat(thing.getStatusInfo(), is(statusInfo));

        statusInfo = ThingStatusInfoBuilder.create(ThingStatus.OFFLINE, ThingStatusDetail.NONE).build();
        state.callback.statusUpdated(thing, statusInfo);
        assertThat(thing.getStatusInfo(), is(statusInfo));

        statusInfo = ThingStatusInfoBuilder.create(ThingStatus.UNKNOWN, ThingStatusDetail.NONE).build();
        state.callback.statusUpdated(thing, statusInfo);
        assertThat(thing.getStatusInfo(), is(statusInfo));

        final ThingStatusInfo removingNone = ThingStatusInfoBuilder.create(ThingStatus.REMOVING, ThingStatusDetail.NONE)
                .build();
        expectException(() -> state.callback.statusUpdated(thing, removingNone), IllegalArgumentException.class);

        final ThingStatusInfo uninitializedNone = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE).build();
        expectException(() -> state.callback.statusUpdated(thing, uninitializedNone), IllegalArgumentException.class);

        final ThingStatusInfo initializingNone = ThingStatusInfoBuilder
                .create(ThingStatus.INITIALIZING, ThingStatusDetail.NONE).build();
        expectException(() -> state.callback.statusUpdated(thing, initializingNone), IllegalArgumentException.class);

        thing.setStatusInfo(ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build());
        final ThingStatusInfo removedNone = ThingStatusInfoBuilder.create(ThingStatus.REMOVED, ThingStatusDetail.NONE)
                .build();
        expectException(() -> state.callback.statusUpdated(thing, removedNone), IllegalArgumentException.class);
    }

    private void expectException(Runnable runnable, Class<? extends Exception> exceptionType) {
        try {
            runnable.run();
            fail("Expected a " + exceptionType.getName());
        } catch (Exception e) {
            if (!exceptionType.isInstance(e)) {
                fail("Expected a " + exceptionType.getName() + " but got a " + e.getClass().getName());
            }
        }
    }

    @Test
    public void thingManagerHandlesThingStatusUpdatesUninitializedAndInitializingCorrectly() {
        registerThingTypeProvider();

        ThingHandler thingHandler = mock(ThingHandler.class);
        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        final ThingStatusInfo uninitializedNone = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE).build();
        assertThat(thing.getStatusInfo(), is(uninitializedNone));

        managedThingProvider.add(thing);

        final ThingStatusInfo initializingNone = ThingStatusInfoBuilder
                .create(ThingStatus.INITIALIZING, ThingStatusDetail.NONE).build();
        waitForAssert(() -> assertThat(thing.getStatusInfo(), is(initializingNone)));

        unregisterService(thingHandlerFactory);

        final ThingStatusInfo uninitializedHandlerMissing = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_MISSING_ERROR).build();
        waitForAssert(() -> assertThat(thing.getStatusInfo(), is(uninitializedHandlerMissing)));
    }

    @Test
    public void thingManagerHandlesThingStatusUpdateUninitializedWithAnExceptionCorrectly() {
        String exceptionMessage = "Some runtime exception occurred!";

        ThingHandler thingHandler = mock(ThingHandler.class);
        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenThrow(new RuntimeException(exceptionMessage));

        registerService(thingHandlerFactory);

        managedThingProvider.add(thing);

        ThingStatusInfo statusInfo = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_REGISTERING_ERROR)
                .withDescription(exceptionMessage).build();
        assertThat(thing.getStatusInfo(), is(statusInfo));
    }

    @Test
    @SuppressWarnings({ "null", "unchecked" })
    public void thingManagerHandlesThingUpdatesCorrectly() {
        String itemName = "name";

        managedThingProvider.add(thing);
        managedItemChannelLinkProvider.add(new ItemChannelLink(itemName, CHANNEL_UID));

        class ThingHandlerState {
            ThingHandlerCallback callback;
        }

        final ThingHandlerState state = new ThingHandlerState();

        ThingHandler thingHandler = mock(ThingHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));
        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        ThingRegistry thingRegistry = getService(ThingRegistry.class);
        RegistryChangeListener<Thing> registryChangeListener = mock(RegistryChangeListener.class);

        try {
            thingRegistry.addRegistryChangeListener(registryChangeListener);
            state.callback.thingUpdated(thing);
            verify(registryChangeListener, times(1)).updated(any(Thing.class), any(Thing.class));
        } finally {
            thingRegistry.removeRegistryChangeListener(registryChangeListener);
        }
    }

    @Test
    @SuppressWarnings({ "null", "unchecked" })
    public void thingManagerAllowsChangesToUnmanagedThings() throws Exception {
        ThingManager thingManager = (ThingManager) getService(ThingTypeMigrationService.class);
        assertThat(thingManager, is(notNullValue()));

        ThingProvider customThingProvider = mock(ThingProvider.class);
        when(customThingProvider.getAll()).thenReturn(Collections.singletonList(thing));

        registerService(customThingProvider);

        ThingRegistry thingRegistry = getService(ThingRegistry.class);
        RegistryChangeListener<Thing> registryChangeListener = mock(RegistryChangeListener.class);

        try {
            thingRegistry.addRegistryChangeListener(registryChangeListener);

            Field field = thingManager.getClass().getDeclaredField("thingHandlerCallback");
            field.setAccessible(true);
            ThingHandlerCallback callback = (ThingHandlerCallback) field.get(thingManager);

            callback.thingUpdated(thing);
            verify(registryChangeListener, times(1)).updated(any(Thing.class), any(Thing.class));
        } finally {
            thingRegistry.removeRegistryChangeListener(registryChangeListener);
        }
    }

    @Test
    public void thingManagerPostsThingStatusEventsIfTheStatusOfAThingIsUpdated() {
        registerThingTypeProvider();

        class ThingHandlerState {
            ThingHandlerCallback callback;
        }
        final ThingHandlerState state = new ThingHandlerState();

        ThingHandler thingHandler = mock(ThingHandler.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));
        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        final List<Event> receivedEvents = new ArrayList<>();

        @NonNullByDefault
        EventSubscriber thingStatusEventSubscriber = new EventSubscriber() {
            @Override
            public Set<String> getSubscribedEventTypes() {
                return singleton(ThingStatusInfoEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event event) {
                receivedEvents.add(event);
            }
        };

        registerService(thingStatusEventSubscriber);

        // set status to INITIALIZING
        ThingStatusInfo initializingNone = ThingStatusInfoBuilder
                .create(ThingStatus.INITIALIZING, ThingStatusDetail.NONE).build();
        ThingStatusInfoEvent event = ThingEventFactory.createStatusInfoEvent(thing.getUID(), initializingNone);
        managedThingProvider.add(thing);

        waitForAssert(() -> assertThat(receivedEvents.size(), is(1)));
        assertThat(receivedEvents.get(0).getType(), is(event.getType()));
        assertThat(receivedEvents.get(0).getPayload(), is(event.getPayload()));
        assertThat(receivedEvents.get(0).getTopic(), is(event.getTopic()));
        receivedEvents.clear();

        // set status to ONLINE
        ThingStatusInfo onlineNone = ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build();
        event = ThingEventFactory.createStatusInfoEvent(thing.getUID(), onlineNone);
        state.callback.statusUpdated(thing, onlineNone);

        waitForAssert(() -> assertThat(receivedEvents.size(), is(1)));
        assertThat(receivedEvents.get(0).getType(), is(event.getType()));
        assertThat(receivedEvents.get(0).getPayload(), is(event.getPayload()));
        assertThat(receivedEvents.get(0).getTopic(), is(event.getTopic()));
        receivedEvents.clear();

        // set status to OFFLINE
        ThingStatusInfo offlineCommError = ThingStatusInfoBuilder
                .create(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR).build();
        event = ThingEventFactory.createStatusInfoEvent(thing.getUID(), offlineCommError);
        state.callback.statusUpdated(thing, offlineCommError);

        waitForAssert(() -> assertThat(receivedEvents.size(), is(1)));
        assertThat(receivedEvents.get(0).getType(), is(event.getType()));
        assertThat(receivedEvents.get(0).getPayload(), is(event.getPayload()));
        assertThat(receivedEvents.get(0).getTopic(), is(event.getTopic()));
        receivedEvents.clear();

        // set status to UNINITIALIZED
        ThingStatusInfo uninitializedError = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_MISSING_ERROR).build();
        final Event uninitializedEvent = ThingEventFactory.createStatusInfoEvent(thing.getUID(), uninitializedError);
        unregisterService(thingHandlerFactory);

        waitForAssert(() -> {
            assertThat(receivedEvents.size(), is(2));
            assertThat(receivedEvents.get(1).getType(), is(uninitializedEvent.getType()));
            assertThat(receivedEvents.get(1).getPayload(), is(uninitializedEvent.getPayload()));
            assertThat(receivedEvents.get(1).getTopic(), is(uninitializedEvent.getTopic()));
        });
    }

    @Test
    public void thingManagerPostsThingStatusChangedEventsIfTheStatusOfAThingIsChanged() throws Exception {
        registerThingTypeProvider();

        class ThingHandlerState {
            ThingHandlerCallback callback;
        }
        final ThingHandlerState state = new ThingHandlerState();

        ThingHandler thingHandler = mock(ThingHandler.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));
        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        final List<ThingStatusInfoChangedEvent> infoChangedEvents = new ArrayList<>();

        @NonNullByDefault
        EventSubscriber thingStatusEventSubscriber = new EventSubscriber() {
            @Override
            public Set<String> getSubscribedEventTypes() {
                return singleton(ThingStatusInfoChangedEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event event) {
                infoChangedEvents.add((ThingStatusInfoChangedEvent) event);
            }
        };

        registerService(thingStatusEventSubscriber);

        // add thing (UNINITIALIZED -> INITIALIZING)
        managedThingProvider.add(thing);

        waitForAssert(() -> assertThat(infoChangedEvents.size(), is(1)));
        assertThat(infoChangedEvents.get(0).getType(), is(ThingStatusInfoChangedEvent.TYPE));
        assertThat(infoChangedEvents.get(0).getTopic(), is("smarthome/things/binding:type:id/statuschanged"));
        assertThat(infoChangedEvents.get(0).getStatusInfo().getStatus(), is(ThingStatus.INITIALIZING));
        assertThat(infoChangedEvents.get(0).getOldStatusInfo().getStatus(), is(ThingStatus.UNINITIALIZED));
        infoChangedEvents.clear();

        // set status to ONLINE (INITIALIZING -> ONLINE)
        ThingStatusInfo onlineNone = ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build();
        state.callback.statusUpdated(thing, onlineNone);

        waitForAssert(() -> assertThat(infoChangedEvents.size(), is(1)));
        assertThat(infoChangedEvents.get(0).getType(), is(ThingStatusInfoChangedEvent.TYPE));
        assertThat(infoChangedEvents.get(0).getTopic(), is("smarthome/things/binding:type:id/statuschanged"));
        assertThat(infoChangedEvents.get(0).getStatusInfo().getStatus(), is(ThingStatus.ONLINE));
        assertThat(infoChangedEvents.get(0).getOldStatusInfo().getStatus(), is(ThingStatus.INITIALIZING));
        infoChangedEvents.clear();

        // set status to ONLINE again
        state.callback.statusUpdated(thing, onlineNone);

        // make sure no event has been sent
        Thread.sleep(500);
        assertThat(infoChangedEvents.size(), is(0));
    }

    @Test
    @SuppressWarnings("null")
    public void thingManagerPostsLocalizedThingStatusInfoAndThingStatusInfoChangedEvents() throws Exception {
        registerThingTypeProvider();

        class ThingHandlerState {
            ThingHandlerCallback callback;
        }
        final ThingHandlerState state = new ThingHandlerState();

        ThingHandler thingHandler = mock(ThingHandler.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));
        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        BundleResolver bundleResolver = mock(BundleResolver.class);
        when(bundleResolver.resolveBundle(any())).thenReturn(bundleContext.getBundle());
        ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService = getService(
                ThingStatusInfoI18nLocalizationService.class);
        thingStatusInfoI18nLocalizationService.setBundleResolver(bundleResolver);

        final List<ThingStatusInfoEvent> infoEvents = new ArrayList<>();
        @NonNullByDefault
        EventSubscriber thingStatusInfoEventSubscriber = new EventSubscriber() {
            @Override
            public Set<String> getSubscribedEventTypes() {
                return singleton(ThingStatusInfoEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event event) {
                infoEvents.add((ThingStatusInfoEvent) event);
            }
        };
        registerService(thingStatusInfoEventSubscriber);

        final List<ThingStatusInfoChangedEvent> infoChangedEvents = new ArrayList<>();
        @NonNullByDefault
        EventSubscriber thingStatusInfoChangedEventSubscriber = new EventSubscriber() {
            @Override
            public Set<String> getSubscribedEventTypes() {
                return singleton(ThingStatusInfoChangedEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event event) {
                infoChangedEvents.add((ThingStatusInfoChangedEvent) event);
            }
        };
        registerService(thingStatusInfoChangedEventSubscriber);

        // add thing (UNINITIALIZED -> INITIALIZING)
        managedThingProvider.add(thing);

        waitForAssert(() -> {
            assertThat(infoEvents.size(), is(1));
            assertThat(infoChangedEvents.size(), is(1));
        });

        assertThat(infoEvents.get(0).getType(), is(ThingStatusInfoEvent.TYPE));
        assertThat(infoEvents.get(0).getTopic(), is("smarthome/things/binding:type:id/status"));
        assertThat(infoEvents.get(0).getStatusInfo().getStatus(), is(ThingStatus.INITIALIZING));
        assertThat(infoEvents.get(0).getStatusInfo().getStatusDetail(), is(ThingStatusDetail.NONE));
        assertThat(infoEvents.get(0).getStatusInfo().getDescription(), is(nullValue()));

        assertThat(infoChangedEvents.get(0).getType(), is(ThingStatusInfoChangedEvent.TYPE));
        assertThat(infoChangedEvents.get(0).getTopic(), is("smarthome/things/binding:type:id/statuschanged"));
        assertThat(infoChangedEvents.get(0).getStatusInfo().getStatus(), is(ThingStatus.INITIALIZING));
        assertThat(infoChangedEvents.get(0).getStatusInfo().getStatusDetail(), is(ThingStatusDetail.NONE));
        assertThat(infoChangedEvents.get(0).getStatusInfo().getDescription(), is(nullValue()));
        assertThat(infoChangedEvents.get(0).getOldStatusInfo().getStatus(), is(ThingStatus.UNINITIALIZED));
        assertThat(infoChangedEvents.get(0).getOldStatusInfo().getStatusDetail(), is(ThingStatusDetail.NONE));
        assertThat(infoChangedEvents.get(0).getOldStatusInfo().getDescription(), is(nullValue()));

        infoEvents.clear();
        infoChangedEvents.clear();

        LocaleProvider localeProvider = getService(LocaleProvider.class);
        assertThat(localeProvider, is(notNullValue()));
        Locale defaultLocale = localeProvider.getLocale();

        // set status to ONLINE (INITIALIZING -> ONLINE)
        new DefaultLocaleSetter(getService(ConfigurationAdmin.class)).setDefaultLocale(Locale.ENGLISH);
        waitForAssert(() -> assertThat(localeProvider.getLocale(), is(Locale.ENGLISH)));

        ThingStatusInfo onlineNone = ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE)
                .withDescription("@text/online").build();
        state.callback.statusUpdated(thing, onlineNone);

        waitForAssert(() -> {
            assertThat(infoEvents.size(), is(1));
            assertThat(infoChangedEvents.size(), is(1));
        });

        assertThat(infoEvents.get(0).getType(), is(ThingStatusInfoEvent.TYPE));
        assertThat(infoEvents.get(0).getTopic(), is("smarthome/things/binding:type:id/status"));
        assertThat(infoEvents.get(0).getStatusInfo().getStatus(), is(ThingStatus.ONLINE));
        assertThat(infoEvents.get(0).getStatusInfo().getStatusDetail(), is(ThingStatusDetail.NONE));
        assertThat(infoEvents.get(0).getStatusInfo().getDescription(), is("Thing is online."));

        assertThat(infoChangedEvents.get(0).getType(), is(ThingStatusInfoChangedEvent.TYPE));
        assertThat(infoChangedEvents.get(0).getTopic(), is("smarthome/things/binding:type:id/statuschanged"));
        assertThat(infoChangedEvents.get(0).getStatusInfo().getStatus(), is(ThingStatus.ONLINE));
        assertThat(infoChangedEvents.get(0).getStatusInfo().getStatusDetail(), is(ThingStatusDetail.NONE));
        assertThat(infoChangedEvents.get(0).getStatusInfo().getDescription(), is("Thing is online."));
        assertThat(infoChangedEvents.get(0).getOldStatusInfo().getStatus(), is(ThingStatus.INITIALIZING));
        assertThat(infoChangedEvents.get(0).getOldStatusInfo().getStatusDetail(), is(ThingStatusDetail.NONE));
        assertThat(infoChangedEvents.get(0).getOldStatusInfo().getDescription(), is(nullValue()));

        infoEvents.clear();
        infoChangedEvents.clear();

        // set status to OFFLINE (ONLINE -> OFFLINE)
        new DefaultLocaleSetter(getService(ConfigurationAdmin.class)).setDefaultLocale(Locale.GERMAN);
        waitForAssert(() -> assertThat(localeProvider.getLocale(), is(Locale.GERMAN)));

        ThingStatusInfo offlineNone = ThingStatusInfoBuilder.create(ThingStatus.OFFLINE, ThingStatusDetail.NONE)
                .withDescription("@text/offline.without-param").build();
        state.callback.statusUpdated(thing, offlineNone);

        waitForAssert(() -> {
            assertThat(infoEvents.size(), is(1));
            assertThat(infoChangedEvents.size(), is(1));
        });

        assertThat(infoEvents.get(0).getType(), is(ThingStatusInfoEvent.TYPE));
        assertThat(infoEvents.get(0).getTopic(), is("smarthome/things/binding:type:id/status"));
        assertThat(infoEvents.get(0).getStatusInfo().getStatus(), is(ThingStatus.OFFLINE));
        assertThat(infoEvents.get(0).getStatusInfo().getStatusDetail(), is(ThingStatusDetail.NONE));
        assertThat(infoEvents.get(0).getStatusInfo().getDescription(), is("Thing ist offline."));

        assertThat(infoChangedEvents.get(0).getType(), is(ThingStatusInfoChangedEvent.TYPE));
        assertThat(infoChangedEvents.get(0).getTopic(), is("smarthome/things/binding:type:id/statuschanged"));
        assertThat(infoChangedEvents.get(0).getStatusInfo().getStatus(), is(ThingStatus.OFFLINE));
        assertThat(infoChangedEvents.get(0).getStatusInfo().getStatusDetail(), is(ThingStatusDetail.NONE));
        assertThat(infoChangedEvents.get(0).getStatusInfo().getDescription(), is("Thing ist offline."));
        assertThat(infoChangedEvents.get(0).getOldStatusInfo().getStatus(), is(ThingStatus.ONLINE));
        assertThat(infoChangedEvents.get(0).getOldStatusInfo().getStatusDetail(), is(ThingStatusDetail.NONE));
        assertThat(infoChangedEvents.get(0).getOldStatusInfo().getDescription(), is("Thing ist online."));

        new DefaultLocaleSetter(getService(ConfigurationAdmin.class)).setDefaultLocale(defaultLocale);
        waitForAssert(() -> assertThat(localeProvider.getLocale(), is(defaultLocale)));
    }

    @Test
    public void thingManagerCallsInitializeForAddedThingCorrectly() {
        // register ThingTypeProvider & ConfigurationDescriptionProvider with 'required' parameter
        registerThingTypeProvider();
        registerConfigDescriptionProvider(true);

        ThingImpl thing = (ThingImpl) ThingBuilder.create(THING_TYPE_UID, THING_UID).build();

        class ThingHandlerState {
            ThingHandlerCallback callback;
        }
        final ThingHandlerState state = new ThingHandlerState();

        ThingHandler thingHandler = mock(ThingHandler.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));
        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        ThingStatusInfo uninitializedNone = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE).build();
        assertThat(thing.getStatusInfo(), is(uninitializedNone));

        // add thing with empty configuration
        managedThingProvider.add(thing);

        // ThingHandler.initialize() not called, thing status is UNINITIALIZED.HANDLER_CONFIGURATION_PENDING
        ThingStatusInfo uninitializedPending = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_CONFIGURATION_PENDING).build();
        verify(thingHandler, never()).initialize();
        assertThat(thing.getStatusInfo(), is(uninitializedPending));

        // set required configuration parameter
        Configuration configuration = new Configuration();
        configuration.put("parameter", "value");
        thing.setConfiguration(configuration);
        ThingStatusInfo onlineNone = ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build();
        state.callback.configurationUpdated(thing);
        state.callback.statusUpdated(thing, onlineNone);

        // ThingHandler.initialize() called, thing status is ONLINE.NONE
        waitForAssert(() -> {
            verify(thingHandler, times(1)).initialize();
            assertThat(thing.getStatusInfo(), is(onlineNone));
        });
    }

    @Test
    @SuppressWarnings("null")
    public void thingManagerWaitsWithInitializeUntilBundleProcessingIsFinished() throws Exception {
        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID).build();

        class ThingHandlerState {
            @SuppressWarnings("unused")
            ThingHandlerCallback callback;
        }
        final ThingHandlerState state = new ThingHandlerState();

        ThingHandler thingHandler = mock(ThingHandler.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));
        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        final ReadyMarker marker = new ReadyMarker(ThingManagerImpl.XML_THING_TYPE,
                ReadyMarkerUtils.getIdentifier(FrameworkUtil.getBundle(this.getClass())));
        waitForAssert(() -> {
            // wait for the XML processing to be finished, then remove the ready marker again
            assertThat(readyService.isReady(marker), is(true));
            readyService.unmarkReady(marker);
        });

        ThingStatusInfo uninitializedNone = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE).build();
        assertThat(thing.getStatusInfo(), is(uninitializedNone));

        managedThingProvider.add(thing);

        // just wait a little to make sure really nothing happens
        Thread.sleep(1000);
        verify(thingHandler, never()).initialize();
        assertThat(thing.getStatusInfo(), is(uninitializedNone));

        readyService.markReady(marker);

        // ThingHandler.initialize() called, thing status is INITIALIZING.NONE
        ThingStatusInfo initializingNone = ThingStatusInfoBuilder
                .create(ThingStatus.INITIALIZING, ThingStatusDetail.NONE).build();
        waitForAssert(() -> {
            verify(thingHandler, times(1)).initialize();
            assertThat(thing.getStatusInfo(), is(initializingNone));
        });
    }

    @Test
    @SuppressWarnings("null")
    public void thingManagerCallsBridgeStatusChangedOnThingHandlerCorrectly() {
        class BridgeHandlerState {
            ThingHandlerCallback callback;
        }
        final BridgeHandlerState bridgeState = new BridgeHandlerState();

        Bridge bridge = BridgeBuilder
                .create(new ThingTypeUID("binding:test"), new ThingUID("binding:test:someBridgeUID-1")).build();

        BridgeHandler bridgeHandler = mock(BridgeHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(bridgeHandler).setCallback(any(ThingHandlerCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.callback.statusUpdated(bridge,
                        ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build());
                return null;
            }
        }).when(bridgeHandler).initialize();

        when(bridgeHandler.getThing()).thenReturn(bridge);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.callback.statusUpdated(bridge,
                        ThingStatusInfoBuilder.create(ThingStatus.REMOVED, ThingStatusDetail.NONE).build());
                return null;
            }
        }).when(bridgeHandler).handleRemoval();

        class ThingHandlerState {
            ThingHandlerCallback callback;
        }
        final ThingHandlerState thingState = new ThingHandlerState();

        Thing thing = ThingBuilder.create(new ThingTypeUID("binding:type"), new ThingUID("binding:type:thingUID-1"))
                .withBridge(bridge.getUID()).build();

        ThingHandler thingHandler = mock(ThingHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                thingState.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                thingState.callback.statusUpdated(thing,
                        ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build());
                return null;
            }
        }).when(thingHandler).initialize();

        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        doAnswer(new Answer<ThingHandler>() {
            @Override
            public ThingHandler answer(InvocationOnMock invocation) throws Throwable {
                Thing thing = (Thing) invocation.getArgument(0);
                if (thing instanceof Bridge) {
                    return bridgeHandler;
                } else if (thing instanceof Thing) {
                    return thingHandler;
                }
                return null;
            }
        }).when(thingHandlerFactory).registerHandler(any(Thing.class));

        registerService(thingHandlerFactory);

        managedThingProvider.add(bridge);
        managedThingProvider.add(thing);

        waitForAssert(() -> assertThat(bridge.getStatus(), is(ThingStatus.ONLINE)));
        waitForAssert(() -> assertThat(thing.getStatus(), is(ThingStatus.ONLINE)));

        // initial bridge initialization is not reported as status change
        waitForAssert(() -> verify(thingHandler, never()).bridgeStatusChanged(any(ThingStatusInfo.class)));

        // the same status is also not reported, because it's not a change
        ThingStatusInfo onlineNone = ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build();
        bridgeState.callback.statusUpdated(bridge, onlineNone);
        waitForAssert(() -> verify(thingHandler, never()).bridgeStatusChanged(any(ThingStatusInfo.class)));

        // report a change to OFFLINE
        ThingStatusInfo offlineNone = ThingStatusInfoBuilder.create(ThingStatus.OFFLINE, ThingStatusDetail.NONE)
                .build();
        bridgeState.callback.statusUpdated(bridge, offlineNone);
        waitForAssert(() -> verify(thingHandler, times(1)).bridgeStatusChanged(any(ThingStatusInfo.class)));

        // report a change to ONLINE
        bridgeState.callback.statusUpdated(bridge, onlineNone);
        waitForAssert(() -> verify(thingHandler, times(2)).bridgeStatusChanged(any(ThingStatusInfo.class)));

        ThingRegistry thingRegistry = getService(ThingRegistry.class);
        thingRegistry.remove(bridge.getUID());
        waitForAssert(() -> {
            assertThat(bridge.getStatus(), is(equalTo(ThingStatus.UNINITIALIZED)));
            waitForAssert(() -> verify(thingHandler, times(2)).bridgeStatusChanged(any(ThingStatusInfo.class)));
        });
    }

    @Test
    public void thingManagerCallsChildHandlerInitializedAndChildHandlerDisposedOnBridgeHandlerCorrectly() {
        class BridgeHandlerState {
            boolean childHandlerInitializedCalled;
            ThingHandler initializedHandler;
            Thing initializedThing;
            boolean childHandlerDisposedCalled;
            ThingHandler disposedHandler;
            Thing disposedThing;
            ThingHandlerCallback callback;
        }
        final BridgeHandlerState bridgeState = new BridgeHandlerState();

        Bridge bridge = BridgeBuilder.create(new ThingTypeUID("binding:type"), new ThingUID("binding:type:bridgeUID-1"))
                .build();

        BridgeHandler bridgeHandler = mock(BridgeHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(bridgeHandler).setCallback(any(ThingHandlerCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.callback.statusUpdated(bridge,
                        ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build());
                return null;
            }
        }).when(bridgeHandler).initialize();

        when(bridgeHandler.getThing()).thenReturn(bridge);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.childHandlerInitializedCalled = true;
                bridgeState.initializedHandler = (ThingHandler) invocation.getArgument(0);
                bridgeState.initializedThing = (Thing) invocation.getArgument(1);
                return null;
            }
        }).when(bridgeHandler).childHandlerInitialized(any(ThingHandler.class), any(Thing.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.childHandlerDisposedCalled = true;
                bridgeState.disposedHandler = (ThingHandler) invocation.getArgument(0);
                bridgeState.disposedThing = (Thing) invocation.getArgument(1);
                return null;
            }
        }).when(bridgeHandler).childHandlerDisposed(any(ThingHandler.class), any(Thing.class));

        class ThingHandlerState {
            ThingHandlerCallback callback;
        }
        final ThingHandlerState thingState = new ThingHandlerState();

        Thing thing = ThingBuilder.create(new ThingTypeUID("binding:type"), new ThingUID("binding:type:thingUID-1"))
                .withBridge(bridge.getUID()).build();

        ThingHandler thingHandler = mock(ThingHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                thingState.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                thingState.callback.statusUpdated(thing,
                        ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build());
                return null;
            }
        }).when(thingHandler).initialize();

        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        doAnswer(new Answer<ThingHandler>() {
            @Override
            public ThingHandler answer(InvocationOnMock invocation) throws Throwable {
                Thing thing = (Thing) invocation.getArgument(0);
                if (thing instanceof Bridge) {
                    return bridgeHandler;
                } else if (thing instanceof Thing) {
                    return thingHandler;
                }
                return null;
            }
        }).when(thingHandlerFactory).registerHandler(any(Thing.class));

        registerService(thingHandlerFactory);

        managedThingProvider.add(bridge);

        assertThat(bridgeState.childHandlerInitializedCalled, is(false));
        assertThat(bridgeState.childHandlerDisposedCalled, is(false));

        managedThingProvider.add(thing);
        waitForAssert(() -> assertThat(bridgeState.childHandlerInitializedCalled, is(true)));
        assertThat(bridgeState.initializedThing, is(thing));
        assertThat(bridgeState.initializedHandler, is(thingHandler));

        managedThingProvider.remove(thing.getUID());
        waitForAssert(() -> assertThat(bridgeState.childHandlerDisposedCalled, is(true)));
        assertThat(bridgeState.disposedThing, is(thing));
        assertThat(bridgeState.disposedHandler, is(thingHandler));
    }

    @Test
    public void thingManagerCallsChildHandlerInitializedAndChildHandlerDisposedOnBridgeHandlerCorrectlyEvenIfChildRegistrationTakesTooLong() {
        class BridgeHandlerState {
            boolean childHandlerInitializedCalled;
            ThingHandler initializedHandler;
            Thing initializedThing;
            boolean childHandlerDisposedCalled;
            ThingHandler disposedHandler;
            Thing disposedThing;
            ThingHandlerCallback callback;
        }
        final BridgeHandlerState bridgeState = new BridgeHandlerState();

        Bridge bridge = BridgeBuilder.create(new ThingTypeUID("binding:type"), new ThingUID("binding:type:bridgeUID-1"))
                .build();

        BridgeHandler bridgeHandler = mock(BridgeHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(bridgeHandler).setCallback(any(ThingHandlerCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.callback.statusUpdated(bridge,
                        ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build());
                return null;
            }
        }).when(bridgeHandler).initialize();

        when(bridgeHandler.getThing()).thenReturn(bridge);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.childHandlerInitializedCalled = true;
                bridgeState.initializedHandler = (ThingHandler) invocation.getArgument(0);
                bridgeState.initializedThing = (Thing) invocation.getArgument(1);
                return null;
            }
        }).when(bridgeHandler).childHandlerInitialized(any(ThingHandler.class), any(Thing.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                bridgeState.childHandlerDisposedCalled = true;
                bridgeState.disposedHandler = (ThingHandler) invocation.getArgument(0);
                bridgeState.disposedThing = (Thing) invocation.getArgument(1);
                return null;
            }
        }).when(bridgeHandler).childHandlerDisposed(any(ThingHandler.class), any(Thing.class));

        class ThingHandlerState {
            ThingHandlerCallback callback;
        }
        final ThingHandlerState thingState = new ThingHandlerState();

        Thing thing = ThingBuilder.create(new ThingTypeUID("binding:type"), new ThingUID("binding:type:thingUID-1"))
                .withBridge(bridge.getUID()).build();

        ThingHandler thingHandler = mock(ThingHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                thingState.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                thingState.callback.statusUpdated(thing,
                        ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE).build());
                return null;
            }
        }).when(thingHandler).initialize();

        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        doAnswer(new Answer<ThingHandler>() {
            @Override
            public ThingHandler answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(6000); // Wait longer than the SafeMethodCaller timeout
                Thing thing = (Thing) invocation.getArgument(0);
                if (thing instanceof Bridge) {
                    return bridgeHandler;
                } else if (thing instanceof Thing) {
                    return thingHandler;
                }
                return null;
            }
        }).when(thingHandlerFactory).registerHandler(any(Thing.class));

        registerService(thingHandlerFactory);

        managedThingProvider.add(bridge);

        assertThat(bridgeState.childHandlerInitializedCalled, is(false));
        assertThat(bridgeState.childHandlerDisposedCalled, is(false));

        managedThingProvider.add(thing);
        waitForAssert(() -> assertThat(bridgeState.childHandlerInitializedCalled, is(true)));
        assertThat(bridgeState.initializedThing, is(thing));
        assertThat(bridgeState.initializedHandler, is(thingHandler));

        managedThingProvider.remove(thing.getUID());
        waitForAssert(() -> assertThat(bridgeState.childHandlerDisposedCalled, is(true)));
        assertThat(bridgeState.disposedThing, is(thing));
        assertThat(bridgeState.disposedHandler, is(thingHandler));
    }

    @Test
    public void thingManagerConsidersUNKNOWNasReadyToUseAndForwardsCommand() {
        class ThingHandlerState {
            boolean handleCommandCalled;
            ChannelUID calledChannelUID;
            Command calledCommand;
            ThingHandlerCallback callback;
        }
        final ThingHandlerState state = new ThingHandlerState();

        managedThingProvider.add(thing);

        ThingHandler thingHandler = mock(ThingHandler.class);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.callback = (ThingHandlerCallback) invocation.getArgument(0);
                return null;
            }
        }).when(thingHandler).setCallback(any(ThingHandlerCallback.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                state.handleCommandCalled = true;
                state.calledChannelUID = (ChannelUID) invocation.getArgument(0);
                state.calledCommand = (Command) invocation.getArgument(1);
                return null;
            }
        }).when(thingHandler).handleCommand(any(ChannelUID.class), any(Command.class));

        when(thingHandler.getThing()).thenReturn(thing);

        ThingHandlerFactory thingHandlerFactory = mock(ThingHandlerFactory.class);
        when(thingHandlerFactory.supportsThingType(any(ThingTypeUID.class))).thenReturn(true);
        when(thingHandlerFactory.registerHandler(any(Thing.class))).thenReturn(thingHandler);

        registerService(thingHandlerFactory);

        itemChannelLinkRegistry.add(new ItemChannelLink("testItem", new ChannelUID(thing.getUID(), "channel")));
        waitForAssert(() -> assertThat(itemRegistry.get("testItem"), is(notNullValue())));

        eventPublisher.post(ItemEventFactory.createCommandEvent("testItem", OnOffType.ON));

        assertThat(state.handleCommandCalled, is(false));

        ThingStatusInfo unknownNone = ThingStatusInfoBuilder.create(ThingStatus.UNKNOWN, ThingStatusDetail.NONE)
                .build();
        state.callback.statusUpdated(thing, unknownNone);
        assertThat(thing.getStatusInfo(), is(unknownNone));

        eventPublisher.post(ItemEventFactory.createCommandEvent("testItem", OnOffType.ON));

        waitForAssert(() -> {
            assertThat(state.handleCommandCalled, is(true));
            assertThat(state.calledChannelUID, is(equalTo(new ChannelUID(thing.getUID(), "channel"))));
            assertThat(state.calledCommand, is(equalTo(OnOffType.ON)));
        });
    }

    private URI configDescriptionUri() {
        try {
            return new URI("test:test");
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Error creating config description URI");
        }
    }

    private void registerThingTypeProvider() {
        ThingType thingType = ThingTypeBuilder.instance(new ThingTypeUID("binding", "type"), "label")
                .withConfigDescriptionURI(configDescriptionUri()).build();

        ThingTypeProvider thingTypeProvider = mock(ThingTypeProvider.class);
        when(thingTypeProvider.getThingType(any(ThingTypeUID.class), nullable(Locale.class))).thenReturn(thingType);
        registerService(thingTypeProvider);

        ThingTypeRegistry thingTypeRegistry = mock(ThingTypeRegistry.class);
        when(thingTypeRegistry.getThingType(any(ThingTypeUID.class))).thenReturn(thingType);
        registerService(thingTypeRegistry);
    }

    private void registerConfigDescriptionProvider(boolean withRequiredParameter) {
        ConfigDescription configDescription = new ConfigDescription(configDescriptionUri(),
                singletonList(
                        ConfigDescriptionParameterBuilder.create("parameter", ConfigDescriptionParameter.Type.TEXT)
                                .withRequired(withRequiredParameter).build()));

        ConfigDescriptionProvider configDescriptionProvider = mock(ConfigDescriptionProvider.class);
        when(configDescriptionProvider.getConfigDescription(any(URI.class), nullable(Locale.class)))
                .thenReturn(configDescription);
        registerService(configDescriptionProvider);
    }

}
