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
package org.openhab.core.thing.binding;

import static java.util.Collections.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.status.ConfigStatusMessage;
import org.openhab.core.config.core.status.ConfigStatusProvider;
import org.openhab.core.config.core.status.ConfigStatusService;
import org.openhab.core.config.core.validation.ConfigValidationException;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.BridgeBuilder;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;

/**
 * Tests for {@link ManagedThingProvider}.
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Dennis Nobel - Added test for bridgeInitialized and bridgeDisposed callbacks
 * @auther Thomas Höfer - Added config status tests
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@SuppressWarnings("null")
public class BindingBaseClassesOSGiTest extends JavaOSGiTest {

    private static final String BINDING_ID = "testBinding";
    private static final String THING_TYPE_ID = "testThingType";
    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID(BINDING_ID, THING_TYPE_ID);

    private ManagedThingProvider managedThingProvider;
    private ThingRegistry thingRegistry;

    private @Mock ComponentContext componentContext;

    public @Rule MockitoRule rule = MockitoJUnit.rule();

    @Before
    public void setup() {
        registerVolatileStorageService();
        managedThingProvider = getService(ManagedThingProvider.class);
        assertThat(managedThingProvider, is(notNullValue()));
        thingRegistry = getService(ThingRegistry.class);
        assertThat(thingRegistry, is(notNullValue()));
        when(componentContext.getBundleContext()).thenReturn(bundleContext);
    }

    @After
    public void teardown() {
        managedThingProvider.getAll().forEach(t -> managedThingProvider.remove(t.getUID()));
    }

    class SimpleThingHandlerFactory extends BaseThingHandlerFactory {
        private final Set<ThingHandler> handlers = new HashSet<>();

        @Override
        public boolean supportsThingType(ThingTypeUID thingTypeUID) {
            return true;
        }

        @Override
        protected ThingHandler createHandler(Thing thing) {
            ThingHandler handler = (thing instanceof Bridge) ? new SimpleBridgeHandler((Bridge) thing)
                    : new SimpleThingHandler(thing);
            handlers.add(handler);
            return handler;
        }

        public Set<ThingHandler> getHandlers() {
            return handlers;
        }
    }

    class SimpleThingHandler extends BaseThingHandler {

        SimpleThingHandler(Thing thing) {
            super(thing);
        }

        @Override
        public void handleCommand(ChannelUID channelUID, Command command) {
            // check getBridge works
            assertThat(getBridge().getUID().toString(), is("bindingId:type1:bridgeId"));
        }

        @Override
        public void initialize() {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    class SimpleBridgeHandler extends BaseBridgeHandler {

        SimpleBridgeHandler(Bridge bridge) {
            super(bridge);
        }

        @Override
        public void handleCommand(ChannelUID channelUID, Command command) {
        }

        public void updateBridgetatus(ThingStatus status) {
            updateStatus(status);
        }

        @Override
        public void initialize() {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Test
    public void assertBaseThingHandlerFactoryRegistersHandlerAndBaseThingHandlersGetBridgeWorks() {
        SimpleThingHandlerFactory thingHandlerFactory = new SimpleThingHandlerFactory();
        thingHandlerFactory.activate(componentContext);
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());

        ThingTypeUID bridgeTypeUID = new ThingTypeUID("bindingId:type1");
        ThingUID bridgeUID = new ThingUID("bindingId:type1:bridgeId");
        Bridge bridge = BridgeBuilder.create(bridgeTypeUID, bridgeUID).build();

        ThingTypeUID thingTypeUID = new ThingTypeUID("bindingId:type2");
        ThingUID thingUID = new ThingUID("bindingId:type2:thingId");
        Thing thing = ThingBuilder.create(thingTypeUID, thingUID).withBridge(bridge.getUID()).build();

        managedThingProvider.add(bridge);
        managedThingProvider.add(thing);

        waitForAssert(() -> {
            ThingHandler handler = thing.getHandler();
            assertThat(handler, is(not(nullValue())));
        });

        ThingHandler handler = thing.getHandler();

        ThingHandler retrievedHandler = getThingHandler(thingHandlerFactory, SimpleThingHandler.class);
        assertThat(retrievedHandler, is(handler));

        // check that base thing handler does not implement config status provider
        waitForAssert(() -> {
            ConfigStatusProvider configStatusProviderOsgiService = getService(ConfigStatusProvider.class);
            assertThat(configStatusProviderOsgiService, is(nullValue()));
        });

        // the assertion is in handle command
        handler.handleCommand(new ChannelUID("bindingId:type2:thingId:channel"), RefreshType.REFRESH);

        unregisterService(ThingHandlerFactory.class.getName());
        thingHandlerFactory.deactivate(componentContext);
    }

    @Test
    public void assertBaseThingHandlerFactoryRegistersConfigStatusProvider() {
        ConfigStatusProviderThingHandlerFactory thingHandlerFactory = new ConfigStatusProviderThingHandlerFactory();
        thingHandlerFactory.activate(componentContext);
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());

        ThingTypeUID thingTypeUID = new ThingTypeUID("bindingId:type");
        ThingUID thingUID = new ThingUID("bindingId:type:thingId");
        Thing thing = ThingBuilder.create(thingTypeUID, thingUID).build();

        managedThingProvider.add(thing);

        ThingHandler handler = thing.getHandler();
        assertThat(handler, is(not(nullValue())));

        // check that the config status provider is registered as OSGi service
        ConfigStatusProvider configStatusProviderOsgiService = getService(ConfigStatusProvider.class);
        assertThat(configStatusProviderOsgiService, is(handler));

        unregisterService(ThingHandlerFactory.class.getName());
        thingHandlerFactory.deactivate(componentContext);
    }

    class ConfigStatusInfoEventSubscriber implements EventSubscriber {
        private final ThingUID thingUID;
        private Event receivedEvent;

        ConfigStatusInfoEventSubscriber(ThingUID thingUID) {
            this.thingUID = thingUID;
        }

        @Override
        public Set<String> getSubscribedEventTypes() {
            return unmodifiableSet(Stream.of("ConfigStatusInfoEvent").collect(Collectors.toSet()));
        }

        @Override
        public EventFilter getEventFilter() {
            return new EventFilter() {
                @Override
                public boolean apply(Event event) {
                    return event.getTopic().equals(
                            "smarthome/things/{thingUID}/config/status".replace("{thingUID}", thingUID.getAsString()));
                };
            };
        }

        @Override
        public void receive(Event event) {
            this.receivedEvent = event;
        }

        public Event getReceivedEvent() {
            return receivedEvent;
        }

        public void resetReceivedEvent() {
            receivedEvent = null;
        }
    }

    @Test
    public void assertConfigStatusIsPropagated() throws Exception {
        ConfigStatusProviderThingHandlerFactory thingHandlerFactory = new ConfigStatusProviderThingHandlerFactory();
        thingHandlerFactory.activate(componentContext);
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());

        ThingTypeUID thingTypeUID = new ThingTypeUID("bindingId:type");
        ThingUID thingUID = new ThingUID("bindingId:type:thingId");
        Thing thing = ThingBuilder.create(thingTypeUID, thingUID).build();

        managedThingProvider.add(thing);

        ConfigStatusService service = getService(ConfigStatusService.class);

        TranslationProvider translationProvider = mock(TranslationProvider.class);
        when(translationProvider.getText(nullable(Bundle.class), nullable(String.class), nullable(String.class),
                nullable(Locale.class), nullable(Object[].class))).then(new Answer<String>() {
                    @Override
                    public String answer(InvocationOnMock invocation) throws Throwable {
                        String key = (String) invocation.getArgument(1);
                        return key.endsWith("param.invalid") ? "param invalid" : "param ok";
                    }
                });

        Field field = service.getClass().getDeclaredField("translationProvider");
        field.setAccessible(true);
        field.set(service, translationProvider);

        ConfigStatusInfoEventSubscriber eventSubscriber = new ConfigStatusInfoEventSubscriber(thingUID);
        registerService(eventSubscriber, EventSubscriber.class.getName());

        Thread.sleep(2000);

        thing.getHandler().handleConfigurationUpdate(singletonMap("param", "invalid"));

        waitForAssert(() -> {
            Event event = eventSubscriber.getReceivedEvent();
            assertThat(event, is(notNullValue()));
            assertThat(event.getPayload(), CoreMatchers
                    .containsString("\"parameterName\":\"param\",\"type\":\"ERROR\",\"message\":\"param invalid\"}"));
            eventSubscriber.resetReceivedEvent();
        }, 2500, DFL_SLEEP_TIME);

        thing.getHandler().handleConfigurationUpdate(singletonMap("param", "ok"));

        waitForAssert(() -> {
            Event event = eventSubscriber.getReceivedEvent();
            assertThat(event, is(notNullValue()));
            assertThat(event.getPayload(), CoreMatchers
                    .containsString("\"parameterName\":\"param\",\"type\":\"INFORMATION\",\"message\":\"param ok\"}"));
        }, 2500, DFL_SLEEP_TIME);
    }

    @Test
    public void assertBaseThingHandlerNotifiesThingManagerAboutConfigurationUpdates() {
        // register ThingTypeProvider & ConfigurationDescription with 'required' parameter
        registerThingTypeProvider();
        registerConfigDescriptionProvider(true);

        // register thing handler factory
        SimpleThingHandlerFactory thingHandlerFactory = new SimpleThingHandlerFactory();
        thingHandlerFactory.activate(componentContext);
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());

        ThingUID thingUID = new ThingUID(THING_TYPE_UID, "thingId");
        Thing thing = ThingBuilder.create(THING_TYPE_UID, thingUID).build();

        // add thing with empty configuration
        managedThingProvider.add(thing);

        // ThingHandler.initialize() has not been called; thing with status UNINITIALIZED.HANDLER_CONFIGURATION_PENDING
        ThingStatusInfo uninitialized = ThingStatusInfoBuilder
                .create(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_CONFIGURATION_PENDING).build();
        assertThat(thing.getStatusInfo(), is(uninitialized));

        thingRegistry.updateConfiguration(thingUID, singletonMap("parameter", "value"));

        // ThingHandler.initialize() has been called; thing with status ONLINE.NONE
        final ThingStatusInfo online = ThingStatusInfoBuilder.create(ThingStatus.ONLINE).build();
        waitForAssert(() -> {
            assertThat(thing.getStatusInfo(), is(online));
        }, 4000, DFL_SLEEP_TIME);
    }

    class ConfigStatusProviderThingHandlerFactory extends BaseThingHandlerFactory {

        @Override
        public boolean supportsThingType(ThingTypeUID thingTypeUID) {
            return true;
        }

        @Override
        protected ThingHandler createHandler(Thing thing) {
            return new ConfigStatusProviderThingHandler(thing);
        }
    }

    static class ConfigStatusProviderThingHandler extends ConfigStatusThingHandler {

        private static final String PARAM = "param";
        private static final ConfigStatusMessage ERROR = ConfigStatusMessage.Builder.error(PARAM)
                .withMessageKeySuffix("param.invalid").build();
        private static final ConfigStatusMessage INFO = ConfigStatusMessage.Builder.information(PARAM)
                .withMessageKeySuffix("param.ok").build();

        ConfigStatusProviderThingHandler(Thing thing) {
            super(thing);
        }

        @Override
        public void handleCommand(ChannelUID channelUID, Command command) {
            // not implemented
        }

        @Override
        public void initialize() {
            updateStatus(ThingStatus.ONLINE);
        }

        @Override
        public Collection<ConfigStatusMessage> getConfigStatus() {
            if ("invalid".equals(getThing().getConfiguration().get(PARAM))) {
                return singletonList(ERROR);
            }
            return singletonList(INFO);
        }
    }

    class YetAnotherThingHandlerFactory extends BaseThingHandlerFactory {

        @Override
        public boolean supportsThingType(ThingTypeUID thingTypeUID) {
            return true;
        }

        @Override
        protected ThingHandler createHandler(Thing thing) {
            return new YetAnotherThingHandler(thing);
        }
    }

    class YetAnotherThingHandler extends BaseThingHandler {

        YetAnotherThingHandler(Thing thing) {
            super(thing);
        }

        @Override
        public void initialize() {
            ThingBuilder thingBuilder = editThing();
            thingBuilder
                    .withChannel(ChannelBuilder.create(new ChannelUID("bindingId:type:thingId:1"), "String").build());
            updateThing(thingBuilder.build());
            updateStatus(ThingStatus.ONLINE);
        }

        @Override
        public void handleCommand(ChannelUID channelUID, Command command) {
        }

        public void updateConfig() {
            Configuration configuration = editConfiguration();
            configuration.put("key", "value");
            updateConfiguration(configuration);
        }

        public void updateProperties() {
            Map<String, String> properties = editProperties();
            properties.put(Thing.PROPERTY_MODEL_ID, "1234");
            updateProperties(properties);
        }

        public void updateProperty() {
            updateProperty(Thing.PROPERTY_VENDOR, "vendor");
        }
    }

    class ThingRegistryChangeListener implements RegistryChangeListener<Thing> {
        private boolean updated;
        private Thing thing;

        @Override
        public void added(Thing thing) {
        }

        @Override
        public void removed(Thing thing) {
        }

        @Override
        public void updated(Thing oldThing, Thing thing) {
            updated = true;
            this.thing = thing;
        }

        public boolean isUpdated() {
            return updated;
        }

        public Thing getThing() {
            return thing;
        }
    };

    @Test
    public void assertThingCanBeUpdatedFromThingHandler() {
        registerThingTypeProvider();
        YetAnotherThingHandlerFactory thingHandlerFactory = new YetAnotherThingHandlerFactory();
        thingHandlerFactory.activate(componentContext);
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());

        final ThingRegistryChangeListener listener = new ThingRegistryChangeListener();

        try {
            thingRegistry.addRegistryChangeListener(listener);
            ThingTypeUID thingTypeUID = new ThingTypeUID("bindingId:type");
            ThingUID thingUID = new ThingUID("bindingId:type:thingId");
            Thing thing = ThingBuilder.create(thingTypeUID, thingUID).build();
            assertThat(thing.getChannels().size(), is(0));
            managedThingProvider.add(thing);

            waitForAssert(() -> {
                assertThat(listener.isUpdated(), is(true));
                assertThat(listener.getThing().getChannels().size(), is(1));
            }, 4000, DFL_SLEEP_TIME);

            ((YetAnotherThingHandler) listener.getThing().getHandler()).updateConfig();
            assertThat(listener.getThing().getConfiguration().get("key"), is("value"));
        } finally {
            thingRegistry.removeRegistryChangeListener(listener);
        }
    }

    @Test
    public void assertPropertiesCanBeUpdatedFromThingHandler() {
        registerThingTypeProvider();
        YetAnotherThingHandlerFactory thingHandlerFactory = new YetAnotherThingHandlerFactory();
        thingHandlerFactory.activate(componentContext);
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());

        final ThingRegistryChangeListener listener = new ThingRegistryChangeListener();

        try {
            thingRegistry.addRegistryChangeListener(listener);
            Thing thing = ThingBuilder
                    .create(new ThingTypeUID("bindingId:type"), new ThingUID("bindingId:type:thingId")).build();

            managedThingProvider.add(thing);

            waitForAssert(() -> assertThat(listener.isUpdated(), is(true)), 10000, 100);

            assertThat(listener.getThing().getProperties().get(Thing.PROPERTY_MODEL_ID), is(nullValue()));
            assertThat(listener.getThing().getProperties().get(Thing.PROPERTY_VENDOR), is(nullValue()));

            ((YetAnotherThingHandler) listener.getThing().getHandler()).updateProperties();

            assertThat(listener.getThing().getProperties().get(Thing.PROPERTY_MODEL_ID), is("1234"));

            ((YetAnotherThingHandler) listener.getThing().getHandler()).updateProperty();

            assertThat(listener.getThing().getProperties().get(Thing.PROPERTY_VENDOR), is("vendor"));
        } finally {
            thingRegistry.removeRegistryChangeListener(listener);
        }
    }

    @Test
    public void assertConfigurationWillBeUpdatedByDefaultImplementation() {
        SimpleThingHandlerFactory thingHandlerFactory = new SimpleThingHandlerFactory();
        thingHandlerFactory.activate(componentContext);
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());

        final ThingRegistryChangeListener listener = new ThingRegistryChangeListener();

        try {
            thingRegistry.addRegistryChangeListener(listener);
            ThingTypeUID thingTypeUID = new ThingTypeUID("bindingId:type");
            ThingUID thingUID = new ThingUID("bindingId:type:thingId");
            Thing thing = ThingBuilder.create(thingTypeUID, thingUID).build();

            managedThingProvider.add(thing);

            thingRegistry.updateConfiguration(thingUID, singletonMap("parameter", "value"));

            waitForAssert(() -> assertThat(listener.isUpdated(), is(true)), 10000, 100);

            assertThat(listener.getThing().getConfiguration().get("parameter"), is("value"));
        } finally {
            thingRegistry.removeRegistryChangeListener(listener);
        }
    }

    @Test(expected = ConfigValidationException.class)
    public void assertConfigurationParametersAreValidated() {
        SimpleThingHandlerFactory thingHandlerFactory = new SimpleThingHandlerFactory();
        thingHandlerFactory.activate(componentContext);
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());

        registerThingTypeProvider();
        registerConfigDescriptionProvider(true);

        ThingTypeUID thingTypeUID = new ThingTypeUID("bindingId:type");
        ThingUID thingUID = new ThingUID("bindingId:type:thingId");
        Thing thing = ThingBuilder.create(thingTypeUID, thingUID)
                .withConfiguration(new Configuration(singletonMap("parameter", "someValue"))).build();

        managedThingProvider.add(thing);

        Map<String, Object> configuration = new HashMap<>();
        configuration.put("parameter", null);

        thingRegistry.updateConfiguration(thingUID, singletonMap("parameter", configuration));
    }

    @Test
    public void assertConfigurationIsRolledbackOnError() {
        SimpleThingHandlerFactory thingHandlerFactory = new SimpleThingHandlerFactory();
        thingHandlerFactory.activate(componentContext);
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());

        registerThingTypeAndConfigDescription();

        ThingRegistry thingRegistry = getService(ThingRegistry.class);

        ThingTypeUID thingTypeUID = new ThingTypeUID("bindingId:type");
        ThingUID thingUID = new ThingUID("bindingId:type:thingId");
        Thing thing = ThingBuilder.create(thingTypeUID, thingUID).build();

        managedThingProvider.add(thing);

        // set the config to an initial value
        thingRegistry.updateConfiguration(thingUID, singletonMap("parameter", "before"));
        assertThat(thing.getConfiguration().get("parameter"), is("before"));

        // let it fail next time...
        ThingHandlerCallback callback = mock(ThingHandlerCallback.class);
        Mockito.doThrow(new IllegalStateException()).when(callback).thingUpdated(thing);
        ((SimpleThingHandler) thing.getHandler()).setCallback(callback);

        try {
            thingRegistry.updateConfiguration(thingUID, singletonMap("parameter", "after"));
            fail("There should have been an exception!");
        } catch (IllegalStateException e) {
            // all good, we want that
        }

        // now check if the thing's configuration has been rolled back
        assertThat(thing.getConfiguration().get("parameter"), is("before"));
    }

    @Test
    public void assertBaseThingHandlerHandlesBridgeStatusUpdatesCorrectly() {
        SimpleThingHandlerFactory thingHandlerFactory = new SimpleThingHandlerFactory();
        thingHandlerFactory.activate(componentContext);
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());

        ThingTypeUID thingType1 = new ThingTypeUID("bindingId:type1");
        ThingTypeUID thingType2 = new ThingTypeUID("bindingId:type2");

        Bridge bridge = BridgeBuilder.create(thingType1, new ThingUID("bindingId:type1:bridgeId")).build();
        Thing thingA = ThingBuilder.create(thingType2, new ThingUID("bindingId:type2:thingIdA"))
                .withBridge(bridge.getUID()).build();
        Thing thingB = ThingBuilder.create(thingType2, new ThingUID("bindingId:type2:thingIdB"))
                .withBridge(bridge.getUID()).build();

        assertThat(bridge.getStatus(), is(ThingStatus.UNINITIALIZED));
        assertThat(thingA.getStatus(), is(ThingStatus.UNINITIALIZED));
        assertThat(thingB.getStatus(), is(ThingStatus.UNINITIALIZED));

        managedThingProvider.add(bridge);
        managedThingProvider.add(thingA);
        managedThingProvider.add(thingB);

        waitForAssert(() -> assertThat(bridge.getStatus(), is(ThingStatus.ONLINE)));
        waitForAssert(() -> assertThat(thingA.getStatus(), is(ThingStatus.ONLINE)));
        waitForAssert(() -> assertThat(thingB.getStatus(), is(ThingStatus.ONLINE)));

        // set bridge status to OFFLINE
        SimpleBridgeHandler bridgeHandler = getThingHandler(thingHandlerFactory, SimpleBridgeHandler.class);
        assertThat(bridgeHandler, not(nullValue()));
        bridgeHandler.updateBridgetatus(ThingStatus.OFFLINE);

        // child things are OFFLINE with detail BRIDGE_OFFLINE
        waitForAssert(() -> assertThat(bridge.getStatus(), is(ThingStatus.OFFLINE)));
        final ThingStatusInfo offline = ThingStatusInfoBuilder
                .create(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE).build();
        waitForAssert(() -> assertThat(thingA.getStatusInfo(), is(offline)));
        waitForAssert(() -> assertThat(thingB.getStatusInfo(), is(offline)));

        // set bridge status to ONLINE
        bridgeHandler.updateBridgetatus(ThingStatus.ONLINE);

        // child things are ONLINE with detail NONE
        waitForAssert(() -> assertThat(bridge.getStatus(), is(ThingStatus.ONLINE)));
        final ThingStatusInfo online = ThingStatusInfoBuilder.create(ThingStatus.ONLINE, ThingStatusDetail.NONE)
                .build();
        waitForAssert(() -> assertThat(thingA.getStatusInfo(), is(online)));
        waitForAssert(() -> assertThat(thingB.getStatusInfo(), is(online)));

        unregisterService(ThingHandlerFactory.class.getName());
        thingHandlerFactory.deactivate(componentContext);
    }

    @SuppressWarnings("unchecked")
    protected <T extends ThingHandler> T getThingHandler(SimpleThingHandlerFactory factory, Class<T> clazz) {
        for (ThingHandler handler : factory.getHandlers()) {
            if (clazz.isInstance(handler)) {
                return (T) handler;
            }
        }
        return null;
    }

    private URI configDescriptionUri() {
        try {
            return new URI("test:test");
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Error creating config description URI");
        }
    }

    private void registerThingTypeAndConfigDescription() {
        ThingType thingType = ThingTypeBuilder.instance(new ThingTypeUID(BINDING_ID, THING_TYPE_ID), "label")
                .withConfigDescriptionURI(configDescriptionUri()).build();
        ConfigDescription configDescription = ConfigDescriptionBuilder.create(configDescriptionUri())
                .withParameter(ConfigDescriptionParameterBuilder
                        .create("parameter", ConfigDescriptionParameter.Type.TEXT).withRequired(true).build())
                .build();

        ThingTypeProvider thingTypeProvider = mock(ThingTypeProvider.class);
        when(thingTypeProvider.getThingType(ArgumentMatchers.any(ThingTypeUID.class),
                ArgumentMatchers.any(Locale.class))).thenReturn(thingType);
        registerService(thingTypeProvider);

        ThingTypeRegistry thingTypeRegistry = mock(ThingTypeRegistry.class);
        when(thingTypeRegistry.getThingType(ArgumentMatchers.any(ThingTypeUID.class))).thenReturn(thingType);
        registerService(thingTypeRegistry);

        ConfigDescriptionProvider configDescriptionProvider = mock(ConfigDescriptionProvider.class);
        when(configDescriptionProvider.getConfigDescription(ArgumentMatchers.any(URI.class),
                ArgumentMatchers.nullable(Locale.class))).thenReturn(configDescription);
        registerService(configDescriptionProvider);
    }

    private void registerThingTypeProvider() {
        ThingType thingType = ThingTypeBuilder.instance(new ThingTypeUID(BINDING_ID, THING_TYPE_ID), "label")
                .withConfigDescriptionURI(configDescriptionUri()).build();

        ThingTypeProvider thingTypeProvider = mock(ThingTypeProvider.class);
        when(thingTypeProvider.getThingType(ArgumentMatchers.any(ThingTypeUID.class),
                ArgumentMatchers.nullable(Locale.class))).thenReturn(thingType);
        registerService(thingTypeProvider);

        ThingTypeRegistry thingTypeRegistry = mock(ThingTypeRegistry.class);
        when(thingTypeRegistry.getThingType(ArgumentMatchers.any(ThingTypeUID.class))).thenReturn(thingType);
        registerService(thingTypeRegistry);
    }

    private void registerConfigDescriptionProvider(boolean withRequiredParameter) {
        ConfigDescription configDescription = ConfigDescriptionBuilder.create(configDescriptionUri())
                .withParameter(
                        ConfigDescriptionParameterBuilder.create("parameter", ConfigDescriptionParameter.Type.TEXT)
                                .withRequired(withRequiredParameter).build())
                .build();

        ConfigDescriptionProvider configDescriptionProvider = mock(ConfigDescriptionProvider.class);
        when(configDescriptionProvider.getConfigDescription(ArgumentMatchers.any(URI.class),
                ArgumentMatchers.nullable(Locale.class))).thenReturn(configDescription);
        registerService(configDescriptionProvider);
    }
}
