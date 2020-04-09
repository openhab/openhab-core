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
package org.openhab.core.config.discovery.internal;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.openhab.core.config.discovery.inbox.InboxPredicates.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryResultFlag;
import org.openhab.core.config.discovery.inbox.Inbox;
import org.openhab.core.config.discovery.inbox.InboxListener;
import org.openhab.core.config.discovery.inbox.events.InboxAddedEvent;
import org.openhab.core.config.discovery.inbox.events.InboxRemovedEvent;
import org.openhab.core.config.discovery.inbox.events.InboxUpdatedEvent;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.test.AsyncResultWrapper;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.binding.builder.BridgeBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.osgi.service.component.ComponentContext;

/**
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class InboxOSGiTest extends JavaOSGiTest {

    class DiscoveryService1 extends AbstractDiscoveryService {
        public DiscoveryService1() {
            super(5);
        }

        @Override
        protected void startScan() {
        }
    }

    class DiscoveryService2 extends AbstractDiscoveryService {
        public DiscoveryService2() {
            super(5);
        }

        @Override
        protected void startScan() {
        }
    }

    private DiscoveryService1 discoveryService1 = new DiscoveryService1();
    private DiscoveryService2 discoveryService2 = new DiscoveryService2();

    private static final int DEFAULT_TTL = 60;

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("bindingId", "thing");
    private static final ThingTypeUID BRIDGE_THING_TYPE_UID = new ThingTypeUID("bindingId", "bridge");

    private static final ThingUID BRIDGE_THING_UID = new ThingUID(BRIDGE_THING_TYPE_UID, "bridgeId");

    private static final DiscoveryResult BRIDGE = new DiscoveryResultImpl(BRIDGE_THING_TYPE_UID, BRIDGE_THING_UID, null,
            null, "Bridge", "bridge", DEFAULT_TTL);
    private static final DiscoveryResult THING1_WITH_BRIDGE = new DiscoveryResultImpl(THING_TYPE_UID,
            new ThingUID(THING_TYPE_UID, "id1"), BRIDGE_THING_UID, null, "Thing1", "thing1", DEFAULT_TTL);
    private static final DiscoveryResult THING2_WITH_BRIDGE = new DiscoveryResultImpl(THING_TYPE_UID,
            new ThingUID(THING_TYPE_UID, "id2"), BRIDGE_THING_UID, null, "Thing2", "thing2", DEFAULT_TTL);
    private static final DiscoveryResult THING_WITHOUT_BRIDGE = new DiscoveryResultImpl(THING_TYPE_UID,
            new ThingUID(THING_TYPE_UID, "id3"), null, null, "Thing3", "thing3", DEFAULT_TTL);
    private static final DiscoveryResult THING_WITH_OTHER_BRIDGE = new DiscoveryResultImpl(THING_TYPE_UID,
            new ThingUID(THING_TYPE_UID, "id4"), new ThingUID(THING_TYPE_UID, "id5"), null, "Thing4", "thing4",
            DEFAULT_TTL);

    private final URI testURI = createURI("http:dummy");
    private final String testThingLabel = "dummy_thing";
    private final ThingUID testUID = new ThingUID("binding:type:id");
    private final ThingTypeUID testTypeUID = new ThingTypeUID("binding:type");
    private final Thing testThing = ThingBuilder.create(testTypeUID, testUID).build();
    private final String discoveryResultLabel = "MyLabel";

    @SuppressWarnings("serial")
    private final Map<String, Object> discoveryResultProperties = new LinkedHashMap<String, Object>() {
        {
            put("ip", "192.168.3.99");
            put("pnr", 1234455);
            put("snr", 12345);
            put("manufacturer", "huawei");
            put("manufactured", new Date(12344));
        }
    };

    private final List<String> discoveryResultPropertyKeys = new ArrayList<>(discoveryResultProperties.keySet());

    private final DiscoveryResult testDiscoveryResult = DiscoveryResultBuilder.create(testThing.getUID())
            .withProperties(discoveryResultProperties).withLabel(discoveryResultLabel).build();
    private final ThingType testThingType = ThingTypeBuilder.instance(testTypeUID, "label")
            .withConfigDescriptionURI(testURI).build();
    private final ConfigDescription testConfigDescription = ConfigDescriptionBuilder.create(testURI)
            .withParameters(Stream
                    .of(new ConfigDescriptionParameter(discoveryResultPropertyKeys.get(0), Type.TEXT),
                            new ConfigDescriptionParameter(discoveryResultPropertyKeys.get(1), Type.INTEGER))
                    .collect(toList()))
            .build();
    private final String[] keysInConfigDescription = new String[] { discoveryResultPropertyKeys.get(0),
            discoveryResultPropertyKeys.get(1) };
    private final String[] keysNotInConfigDescription = new String[] { discoveryResultPropertyKeys.get(2),
            discoveryResultPropertyKeys.get(3), discoveryResultPropertyKeys.get(4) };
    private final Map<ThingUID, DiscoveryResult> discoveryResults = new HashMap<>();
    private final List<InboxListener> inboxListeners = new ArrayList<>();

    private @NonNullByDefault({}) PersistentInbox inbox;
    private @NonNullByDefault({}) ManagedThingProvider managedThingProvider;
    private @NonNullByDefault({}) ThingRegistry registry;

    @Before
    public void setUp() {
        registerVolatileStorageService();

        discoveryResults.clear();
        inboxListeners.clear();

        inbox = (PersistentInbox) getService(Inbox.class);
        assertThat(inbox, is(notNullValue()));

        managedThingProvider = getService(ManagedThingProvider.class);
        assertThat(managedThingProvider, is(notNullValue()));

        registry = getService(ThingRegistry.class);
        assertThat(registry, is(notNullValue()));

        ComponentContext componentContextMock = mock(ComponentContext.class);
        when(componentContextMock.getBundleContext()).thenReturn(bundleContext);

        inbox.addThingHandlerFactory(new DummyThingHandlerFactory(componentContextMock));
    }

    @After
    public void cleanUp() {
        discoveryResults.forEach((thingUID, discoveryResult) -> inbox.remove(thingUID));
        inboxListeners.forEach(listener -> inbox.removeInboxListener(listener));
        discoveryResults.clear();
        inboxListeners.clear();
        registry.remove(BRIDGE_THING_UID);
        managedThingProvider.getAll().forEach(thing -> managedThingProvider.remove(thing.getUID()));
    }

    private static URI createURI(String s) {
        try {
            return new URI(s);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to create URI for: " + s, e);
        }
    }

    private boolean addDiscoveryResult(DiscoveryResult discoveryResult) {
        boolean result = inbox.add(discoveryResult);
        if (result) {
            discoveryResults.put(discoveryResult.getThingUID(), discoveryResult);
        }
        return result;
    }

    private boolean removeDiscoveryResult(ThingUID thingUID) {
        boolean result = inbox.remove(thingUID);
        if (result) {
            discoveryResults.remove(thingUID);
        }
        return result;
    }

    private void addInboxListener(InboxListener inboxListener) {
        inbox.addInboxListener(inboxListener);
        // TODO: the test fails if this line is used
        // inboxListeners.add(inboxListener)
    }

    private void removeInboxListener(InboxListener inboxListener) {
        inbox.removeInboxListener(inboxListener);
        // TODO: the test fails if this line is used
        // inboxListeners.remove(inboxListener)
    }

    @Test
    public void assertThatGetAllIncludesPreviouslyAddedDiscoveryResult() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("dummyBindingId", "dummyThingType");
        ThingUID thingUID = new ThingUID(thingTypeUID, "thingId");

        List<DiscoveryResult> allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(0));

        Map<String, Object> props = new HashMap<>();
        props.put("property1", "property1value1");
        props.put("property2", "property2value1");

        DiscoveryResult discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID, null, props, "property1",
                "DummyLabel1", DEFAULT_TTL);

        assertTrue(addDiscoveryResult(discoveryResult));

        allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(1));

        DiscoveryResult actualDiscoveryResult = allDiscoveryResults.get(0);
        assertThat(actualDiscoveryResult.getThingUID(), is(thingUID));
        assertThat(actualDiscoveryResult.getThingTypeUID(), is(thingTypeUID));
        assertThat(actualDiscoveryResult.getBindingId(), is("dummyBindingId"));
        assertThat(actualDiscoveryResult.getFlag(), is(DiscoveryResultFlag.NEW));
        assertThat(actualDiscoveryResult.getLabel(), is("DummyLabel1"));
        assertThat(actualDiscoveryResult.getProperties().size(), is(2));
        assertThat(actualDiscoveryResult.getProperties().get("property1"), is("property1value1"));
        assertThat(actualDiscoveryResult.getProperties().get("property2"), is("property2value1"));
        assertThat(actualDiscoveryResult.getRepresentationProperty(), is("property1"));
    }

    @Test
    public void assertThatGetAllIncludesPreviouslyUpdatedDiscoveryResult() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("dummyBindingId", "dummyThingType");
        ThingUID thingUID = new ThingUID(thingTypeUID, "dummyThingId");

        List<DiscoveryResult> allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(0));

        Map<String, Object> props = new HashMap<>();
        props.put("property1", "property1value1");
        props.put("property2", "property2value1");

        DiscoveryResult discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID, null, props, "property1",
                "DummyLabel1", DEFAULT_TTL);
        assertTrue(addDiscoveryResult(discoveryResult));

        props.clear();
        props.put("property2", "property2value2");
        props.put("property3", "property3value1");

        discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID, null, props, "property3", "DummyLabel2",
                DEFAULT_TTL);

        assertTrue(addDiscoveryResult(discoveryResult));

        allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(1));

        DiscoveryResult actualDiscoveryResult = allDiscoveryResults.get(0);
        assertThat(actualDiscoveryResult.getThingUID(), is(thingUID));
        assertThat(actualDiscoveryResult.getThingTypeUID(), is(thingTypeUID));
        assertThat(actualDiscoveryResult.getBindingId(), is("dummyBindingId"));
        assertThat(actualDiscoveryResult.getFlag(), is(DiscoveryResultFlag.NEW));
        assertThat(actualDiscoveryResult.getLabel(), is("DummyLabel2"));
        assertThat(actualDiscoveryResult.getProperties().size(), is(2));
        assertThat(actualDiscoveryResult.getProperties().get("property2"), is("property2value2"));
        assertThat(actualDiscoveryResult.getProperties().get("property3"), is("property3value1"));
        assertThat(actualDiscoveryResult.getRepresentationProperty(), is("property3"));
    }

    @Test
    public void assertThatGetAllIncludesTwoPreviouslyAddedDiscoveryResults() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("dummyBindingId", "dummyThingType");
        ThingUID thingUID = new ThingUID(thingTypeUID, "dummyThingId");

        List<DiscoveryResult> allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(0));

        DiscoveryResult discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID, null, null, null,
                "DummyLabel1", DEFAULT_TTL);

        assertTrue(addDiscoveryResult(discoveryResult));

        ThingUID thingUID2 = new ThingUID(thingTypeUID, "dummyThingId2");
        discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID2, null, null, null, "DummyLabel2",
                DEFAULT_TTL);

        addDiscoveryResult(discoveryResult);

        allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(2));
    }

    @Test
    public void assertThatGetAllNotIncludesRemovedDiscoveryResult() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("dummyBindingId", "dummyThingType");
        ThingUID thingUID = new ThingUID(thingTypeUID, "dummyThingId");

        List<DiscoveryResult> allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(0));

        Map<String, Object> props = new HashMap<>();
        props.put("property1", "property1value1");
        props.put("property2", "property2value1");

        DiscoveryResult discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID, null, props, "property1",
                "DummyLabel1", DEFAULT_TTL);
        assertTrue(addDiscoveryResult(discoveryResult));

        allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(1));

        assertTrue(removeDiscoveryResult(thingUID));

        allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(0));
    }

    @Test
    public void assertThatGetAllIncludesRemovedUpdatedDiscoveryResult() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("dummyBindingId", "dummyThingType");
        ThingUID thingUID = new ThingUID(thingTypeUID, "dummyThingId");

        List<DiscoveryResult> allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(0));

        Map<String, Object> props = new HashMap<>();
        props.put("property1", "property1value1");
        props.put("property2", "property2value1");

        DiscoveryResult discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID, null, props, "property1",
                "DummyLabel1", DEFAULT_TTL);
        assertTrue(addDiscoveryResult(discoveryResult));

        allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(1));

        props.clear();
        props.put("property2", "property2value2");
        props.put("property3", "property3value1");

        DiscoveryResult discoveryResultUpdate = new DiscoveryResultImpl(thingTypeUID, thingUID, null, props,
                "property3", "DummyLabel2", DEFAULT_TTL);

        assertTrue(addDiscoveryResult(discoveryResultUpdate));

        allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(1));

        assertTrue(removeDiscoveryResult(thingUID));

        allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(0));
    }

    @Test
    public void assertThatInboxListenerIsNotifiedAboutPreviouslyAddedDiscoveryResult() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("dummyBindingId", "dummyThingType");
        ThingUID thingUID = new ThingUID(thingTypeUID, "dummyThingId");

        List<DiscoveryResult> allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(0));

        Map<String, Object> props = new HashMap<>();
        props.put("property1", "property1value1");
        props.put("property2", "property2value1");

        DiscoveryResult discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID, null, props, "property1",
                "DummyLabel1", DEFAULT_TTL);

        AsyncResultWrapper<DiscoveryResult> addedDiscoveryResultWrapper = new AsyncResultWrapper<>();
        AsyncResultWrapper<DiscoveryResult> updatedDiscoveryResultWrapper = new AsyncResultWrapper<>();
        AsyncResultWrapper<DiscoveryResult> removedDiscoveryResultWrapper = new AsyncResultWrapper<>();

        InboxListener inboxListener = new InboxListener() {
            @Override
            public void thingAdded(Inbox source, DiscoveryResult result) {
                if (source.equals(inbox)) {
                    addedDiscoveryResultWrapper.set(result);
                }
            }

            @Override
            public void thingUpdated(Inbox source, DiscoveryResult result) {
                if (source.equals(inbox)) {
                    updatedDiscoveryResultWrapper.set(result);
                }
            }

            @Override
            public void thingRemoved(Inbox source, DiscoveryResult result) {
                if (source.equals(inbox)) {
                    removedDiscoveryResultWrapper.set(result);
                }
            }
        };

        addInboxListener(inboxListener);

        assertTrue(addDiscoveryResult(discoveryResult));

        waitForAssert(() -> assertTrue(addedDiscoveryResultWrapper.isSet()));

        assertFalse(updatedDiscoveryResultWrapper.isSet());
        assertFalse(removedDiscoveryResultWrapper.isSet());

        DiscoveryResult actualDiscoveryResult = addedDiscoveryResultWrapper.getWrappedObject();

        assertThat(actualDiscoveryResult.getThingUID(), is(thingUID));
        assertThat(actualDiscoveryResult.getThingTypeUID(), is(thingTypeUID));
        assertThat(actualDiscoveryResult.getBindingId(), is("dummyBindingId"));
        assertThat(actualDiscoveryResult.getFlag(), is(DiscoveryResultFlag.NEW));
        assertThat(actualDiscoveryResult.getLabel(), is("DummyLabel1"));
        assertThat(actualDiscoveryResult.getProperties().size(), is(2));
        assertThat(actualDiscoveryResult.getProperties().get("property1"), is("property1value1"));
        assertThat(actualDiscoveryResult.getProperties().get("property2"), is("property2value1"));
        assertThat(actualDiscoveryResult.getRepresentationProperty(), is("property1"));
    }

    @Test
    public void assertThatInboxListenerIsNotifiedAboutPreviouslyUpdatedDiscoveryResult() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("dummyBindingId", "dummyThingType");
        ThingUID thingUID = new ThingUID(thingTypeUID, "dummyThingId");

        List<DiscoveryResult> allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(0));

        Map<String, Object> props = new HashMap<>();
        props.put("property1", "property1value1");
        props.put("property2", "property2value1");

        DiscoveryResult discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID, null, props, "property1",
                "DummyLabel1", DEFAULT_TTL);
        assertTrue(addDiscoveryResult(discoveryResult));

        props.clear();
        props.put("property2", "property2value2");
        props.put("property3", "property3value1");

        discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID, null, props, "property3", "DummyLabel2",
                DEFAULT_TTL);

        AsyncResultWrapper<DiscoveryResult> addedDiscoveryResultWrapper = new AsyncResultWrapper<>();
        AsyncResultWrapper<DiscoveryResult> updatedDiscoveryResultWrapper = new AsyncResultWrapper<>();
        AsyncResultWrapper<DiscoveryResult> removedDiscoveryResultWrapper = new AsyncResultWrapper<>();

        InboxListener inboxListener = new InboxListener() {
            @Override
            public void thingAdded(Inbox source, DiscoveryResult result) {
                if (source.equals(inbox)) {
                    addedDiscoveryResultWrapper.set(result);
                }
            }

            @Override
            public void thingUpdated(Inbox source, DiscoveryResult result) {
                if (source.equals(inbox)) {
                    updatedDiscoveryResultWrapper.set(result);
                }
            }

            @Override
            public void thingRemoved(Inbox source, DiscoveryResult result) {
                if (source.equals(inbox)) {
                    removedDiscoveryResultWrapper.set(result);
                }
            }
        };

        addInboxListener(inboxListener);

        assertTrue(addDiscoveryResult(discoveryResult));
        waitForAssert(() -> assertTrue(updatedDiscoveryResultWrapper.isSet()));

        assertFalse(addedDiscoveryResultWrapper.isSet());
        assertFalse(removedDiscoveryResultWrapper.isSet());

        DiscoveryResult actualDiscoveryResult = updatedDiscoveryResultWrapper.getWrappedObject();

        assertThat(actualDiscoveryResult.getThingUID(), is(thingUID));
        assertThat(actualDiscoveryResult.getThingTypeUID(), is(thingTypeUID));
        assertThat(actualDiscoveryResult.getBindingId(), is("dummyBindingId"));
        assertThat(actualDiscoveryResult.getFlag(), is(DiscoveryResultFlag.NEW));
        assertThat(actualDiscoveryResult.getLabel(), is("DummyLabel2"));
        assertThat(actualDiscoveryResult.getProperties().size(), is(2));
        assertThat(actualDiscoveryResult.getProperties().get("property2"), is("property2value2"));
        assertThat(actualDiscoveryResult.getProperties().get("property3"), is("property3value1"));
        assertThat(actualDiscoveryResult.getRepresentationProperty(), is("property3"));
    }

    @Test
    public void assertThatInboxListenerIsNotifiedAboutPreviouslyRemovedDiscoveryResult() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("dummyBindingId", "dummyThingType");
        ThingUID thingUID = new ThingUID(thingTypeUID, "dummyThingId");

        List<DiscoveryResult> allDiscoveryResults = inbox.getAll();
        assertThat(allDiscoveryResults.size(), is(0));

        Map<String, Object> props = new HashMap<>();
        props.put("property1", "property1value1");
        props.put("property2", "property2value1");

        DiscoveryResult discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID, null, props, "property1",
                "DummyLabel1", DEFAULT_TTL);
        assertTrue(addDiscoveryResult(discoveryResult));

        AsyncResultWrapper<DiscoveryResult> addedDiscoveryResultWrapper = new AsyncResultWrapper<>();
        AsyncResultWrapper<DiscoveryResult> updatedDiscoveryResultWrapper = new AsyncResultWrapper<>();
        AsyncResultWrapper<DiscoveryResult> removedDiscoveryResultWrapper = new AsyncResultWrapper<>();

        InboxListener inboxListener = new InboxListener() {
            @Override
            public void thingAdded(Inbox source, DiscoveryResult result) {
                if (source.equals(inbox)) {
                    addedDiscoveryResultWrapper.set(result);
                }
            }

            @Override
            public void thingUpdated(Inbox source, DiscoveryResult result) {
                if (source.equals(inbox)) {
                    updatedDiscoveryResultWrapper.set(result);
                }
            }

            @Override
            public void thingRemoved(Inbox source, DiscoveryResult result) {
                if (source.equals(inbox)) {
                    removedDiscoveryResultWrapper.set(result);
                }
            }
        };

        addInboxListener(inboxListener);

        assertTrue(removeDiscoveryResult(thingUID));

        waitForAssert(() -> assertTrue(removedDiscoveryResultWrapper.isSet()));

        assertFalse(updatedDiscoveryResultWrapper.isSet());
        assertFalse(addedDiscoveryResultWrapper.isSet());

        DiscoveryResult actualDiscoveryResult = removedDiscoveryResultWrapper.getWrappedObject();

        assertThat(actualDiscoveryResult.getThingUID(), is(thingUID));
        assertThat(actualDiscoveryResult.getThingTypeUID(), is(thingTypeUID));
        assertThat(actualDiscoveryResult.getBindingId(), is("dummyBindingId"));
        assertThat(actualDiscoveryResult.getFlag(), is(DiscoveryResultFlag.NEW));
        assertThat(actualDiscoveryResult.getLabel(), is("DummyLabel1"));
        assertThat(actualDiscoveryResult.getProperties().size(), is(2));
        assertThat(actualDiscoveryResult.getProperties().get("property1"), is("property1value1"));
        assertThat(actualDiscoveryResult.getProperties().get("property2"), is("property2value1"));
        assertThat(actualDiscoveryResult.getRepresentationProperty(), is("property1"));
    }

    @Test
    public void assertThatDiscoveryResultIsRemovedWhenThingIsAddedToThingRegistry() {
        assertThat(inbox.getAll().size(), is(0));

        ThingTypeUID thingTypeUID = new ThingTypeUID("dummyBindingId", "dummyThingType");
        ThingUID thingUID = new ThingUID(thingTypeUID, "dummyThingId");

        Map<String, Object> props = new HashMap<>();
        props.put("property1", "property1value1");
        props.put("property2", "property2value1");

        DiscoveryResult discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID, null, props, "property1",
                "DummyLabel1", DEFAULT_TTL);

        inbox.add(discoveryResult);

        assertThat(inbox.getAll().size(), is(1));

        managedThingProvider.add(ThingBuilder.create(thingTypeUID, "dummyThingId").build());

        assertThat(inbox.getAll().size(), is(0));
    }

    @Test
    public void assertThatDiscoveryResultIsNotAddedToInboxWhenThingWithSameUIDexists() {
        assertThat(inbox.getAll().size(), is(0));

        ThingTypeUID thingTypeUID = new ThingTypeUID("dummyBindingId", "dummyThingType");
        ThingUID thingUID = new ThingUID(thingTypeUID, "dummyThingId");

        managedThingProvider.add(ThingBuilder.create(thingTypeUID, "dummyThingId").build());

        Map<String, Object> props = new HashMap<>();
        props.put("property1", "property1value1");
        props.put("property2", "property2value1");

        DiscoveryResult discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID, null, null, null,
                "DummyLabel1", DEFAULT_TTL);

        inbox.add(discoveryResult);

        assertThat(inbox.getAll().size(), is(0));
    }

    @Test
    public void assertThatDiscoveryResultIsAddedToInboxWhenThingWithDifferentUIDexists() {
        assertThat(inbox.getAll().size(), is(0));

        ThingTypeUID thingTypeUID = new ThingTypeUID("dummyBindingId2", "dummyThingType");
        ThingUID thingUID = new ThingUID(thingTypeUID, "dummyThingId");

        managedThingProvider.add(ThingBuilder.create(thingTypeUID, "dummyThingId").build());

        Map<String, Object> props = new HashMap<>();
        props.put("property1", "property1value1");
        props.put("property2", "property2value1");

        DiscoveryResult discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID, null, null, null,
                "DummyLabel1", DEFAULT_TTL);

        inbox.add(discoveryResult);

        assertThat(inbox.getAll().size(), is(0));
    }

    public void assertIncludesAll(List<DiscoveryResult> expectedList, List<DiscoveryResult> actualList) {
        assertThat(actualList.size(), is(expectedList.size()));
        for (DiscoveryResult expectedResult : expectedList) {
            assertTrue(actualList.contains(expectedResult));
        }
    }

    @Test
    public void assertThatInboxEventSubscribersReceiveEventsAboutDiscoveryResultChanges() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("some", "thing");
        ThingUID thingUID = new ThingUID(thingTypeUID, "uid");
        final AsyncResultWrapper<Event> receivedEvent = new AsyncResultWrapper<>();

        EventSubscriber inboxEventSubscriber = new EventSubscriber() {
            @Override
            public void receive(Event event) {
                receivedEvent.set(event);
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Stream.of(InboxAddedEvent.TYPE, InboxRemovedEvent.TYPE, InboxUpdatedEvent.TYPE).collect(toSet());
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }
        };

        registerService(inboxEventSubscriber);

        // add discovery result
        DiscoveryResult discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID, null, null, null, null,
                DEFAULT_TTL);
        addDiscoveryResult(discoveryResult);
        waitForAssert(() -> assertThat(receivedEvent.getWrappedObject(), not(nullValue())));
        assertThat(receivedEvent.getWrappedObject(), is(instanceOf(InboxAddedEvent.class)));
        receivedEvent.reset();

        // update discovery result
        discoveryResult = new DiscoveryResultImpl(thingTypeUID, thingUID, null, null, null, null, DEFAULT_TTL);
        addDiscoveryResult(discoveryResult);
        waitForAssert(() -> assertThat(receivedEvent.getWrappedObject(), not(nullValue())));
        assertThat(receivedEvent.getWrappedObject(), is(instanceOf(InboxUpdatedEvent.class)));
        receivedEvent.reset();

        // remove discovery result
        removeDiscoveryResult(thingUID);
        waitForAssert(() -> assertThat(receivedEvent.getWrappedObject(), not(nullValue())));
        assertThat(receivedEvent.getWrappedObject(), is(instanceOf(InboxRemovedEvent.class)));
    }

    @Test
    public void assertThatRemoveRemovesAssociatedDiscoveryResultsFromInboxWhenBridgeIsRemoved() {
        List<Event> receivedEvents = new ArrayList<>();

        EventSubscriber inboxEventSubscriber = new EventSubscriber() {
            @Override
            public void receive(Event event) {
                receivedEvents.add(event);
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Stream.of(InboxAddedEvent.TYPE, InboxRemovedEvent.TYPE, InboxUpdatedEvent.TYPE).collect(toSet());
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }
        };

        registerService(inboxEventSubscriber);

        inbox.add(BRIDGE);
        inbox.add(THING1_WITH_BRIDGE);
        inbox.add(THING2_WITH_BRIDGE);
        inbox.add(THING_WITHOUT_BRIDGE);
        inbox.add(THING_WITH_OTHER_BRIDGE);
        waitForAssert(() -> assertThat(receivedEvents.size(), is(5)));
        receivedEvents.clear();

        assertTrue(inbox.remove(BRIDGE.getThingUID()));

        assertFalse(inbox.stream().anyMatch(forThingUID(BRIDGE.getThingUID()).and(withFlag(DiscoveryResultFlag.NEW))));
        assertFalse(inbox.stream()
                .anyMatch(forThingUID(THING1_WITH_BRIDGE.getThingUID()).and(withFlag(DiscoveryResultFlag.NEW))));
        assertFalse(inbox.stream()
                .anyMatch(forThingUID(THING2_WITH_BRIDGE.getThingUID()).and(withFlag(DiscoveryResultFlag.NEW))));
        assertThat(inbox.stream().filter(withFlag(DiscoveryResultFlag.NEW)).collect(toList()),
                hasItems(THING_WITHOUT_BRIDGE, THING_WITH_OTHER_BRIDGE));
        waitForAssert(() -> {
            assertThat(receivedEvents.size(), is(3));
            for (Event event : receivedEvents) {
                assertThat(event, is(instanceOf(InboxRemovedEvent.class)));
            }
        });
    }

    @Test
    public void assertThatRemoveLeavesAssociatedDiscoveryResultsInInboxWhenBridgeIsAddedToThingRegistry() {
        List<Event> receivedEvents = new ArrayList<>();

        EventSubscriber inboxEventSubscriber = new EventSubscriber() {
            @Override
            public void receive(Event event) {
                receivedEvents.add(event);
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Stream.of(InboxAddedEvent.TYPE, InboxRemovedEvent.TYPE, InboxUpdatedEvent.TYPE).collect(toSet());
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }
        };

        registerService(inboxEventSubscriber);

        inbox.add(BRIDGE);
        inbox.add(THING1_WITH_BRIDGE);
        inbox.add(THING2_WITH_BRIDGE);
        inbox.add(THING_WITHOUT_BRIDGE);
        waitForAssert(() -> assertThat(receivedEvents.size(), is(4)));
        receivedEvents.clear();

        registry.add(BridgeBuilder.create(BRIDGE_THING_TYPE_UID, BRIDGE_THING_UID).build());
        assertFalse(inbox.stream().anyMatch(forThingUID(BRIDGE.getThingUID()).and(withFlag(DiscoveryResultFlag.NEW))));
        assertThat(inbox.stream().filter(withFlag(DiscoveryResultFlag.NEW)).collect(toList()),
                hasItems(THING1_WITH_BRIDGE, THING2_WITH_BRIDGE, THING_WITHOUT_BRIDGE));
        waitForAssert(() -> {
            assertThat(receivedEvents.size(), is(1));
            for (Event event : receivedEvents) {
                assertThat(event, is(instanceOf(InboxRemovedEvent.class)));
            }
        });
    }

    @Test
    public void assertThatRemovingAbridgeThingFromTheRegistryRemovesItsDiscoveredChildThingsFromTheInbox() {
        List<Event> receivedEvents = new ArrayList<>();

        EventSubscriber inboxEventSubscriber = new EventSubscriber() {
            @Override
            public void receive(Event event) {
                receivedEvents.add(event);
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Stream.of(InboxAddedEvent.TYPE, InboxRemovedEvent.TYPE, InboxUpdatedEvent.TYPE).collect(toSet());
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }
        };

        registerService(inboxEventSubscriber);

        registry.add(BridgeBuilder.create(BRIDGE_THING_TYPE_UID, BRIDGE_THING_UID).build());

        inbox.add(THING1_WITH_BRIDGE);
        inbox.add(THING2_WITH_BRIDGE);
        inbox.add(THING_WITHOUT_BRIDGE);
        inbox.add(THING_WITH_OTHER_BRIDGE);
        assertThat(inbox.stream().filter(withFlag(DiscoveryResultFlag.NEW)).collect(toList()),
                hasItems(THING1_WITH_BRIDGE, THING2_WITH_BRIDGE, THING_WITHOUT_BRIDGE, THING_WITH_OTHER_BRIDGE));

        registry.forceRemove(BRIDGE.getThingUID());

        waitForAssert(() -> {
            assertFalse(inbox.stream()
                    .anyMatch(forThingUID(THING1_WITH_BRIDGE.getThingUID()).and(withFlag(DiscoveryResultFlag.NEW))));
            assertFalse(inbox.stream()
                    .anyMatch(forThingUID(THING2_WITH_BRIDGE.getThingUID()).and(withFlag(DiscoveryResultFlag.NEW))));
            assertThat(inbox.stream().filter(withFlag(DiscoveryResultFlag.NEW)).collect(toList()),
                    hasItems(THING_WITHOUT_BRIDGE, THING_WITH_OTHER_BRIDGE));
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void assertThatApproveThrowsIllegalArgumentExceptionIfNoDiscoveryResultForGivenThingUIDisAvailable() {
        inbox.approve(new ThingUID("1234"), "label");
    }

    @Test
    public void assertThatApproveAddsAllPropertiesOfDiscoveryResultToThingPropertiesIfNoConfigDescriptionParametersForTheThingTypeAreAvailable() {
        inbox.add(testDiscoveryResult);
        Thing approvedThing = inbox.approve(testThing.getUID(), testThingLabel);
        Thing addedThing = registry.get(testThing.getUID());

        assertFalse(addedThing == null);
        assertFalse(approvedThing == null);
        assertTrue(approvedThing.equals(addedThing));
        discoveryResultProperties.keySet().forEach(key -> {
            String thingProperty = addedThing.getProperties().get(key);
            String descResultParam = String.valueOf(discoveryResultProperties.get(key));
            assertFalse(thingProperty == null);
            assertFalse(descResultParam == null);
            assertTrue(thingProperty.equals(descResultParam));
        });
    }

    @Test
    @SuppressWarnings("null")
    public void assertThatApproveSetsTheExplicitlyGivenLabel() {
        inbox.add(testDiscoveryResult);
        Thing approvedThing = inbox.approve(testThing.getUID(), testThingLabel);
        Thing addedThing = registry.get(testThing.getUID());

        assertThat(approvedThing.getLabel(), is(testThingLabel));
        assertThat(addedThing.getLabel(), is(testThingLabel));
    }

    @Test
    @SuppressWarnings("null")
    public void assertThatApproveSetsTheDiscoveredLabelIfNoOtherIsGiven() {
        inbox.add(testDiscoveryResult);
        Thing approvedThing = inbox.approve(testThing.getUID(), null);
        Thing addedThing = registry.get(testThing.getUID());

        assertThat(approvedThing.getLabel(), is(discoveryResultLabel));
        assertThat(addedThing.getLabel(), is(discoveryResultLabel));
    }

    @Test
    @SuppressWarnings("null")
    public void assertThatApproveAddsPropertiesOfDiscoveryResultWhichAreConfigDescriptionParametersAsThingConfigurationPropertiesAndPropertiesWhichAreNoConfigDescriptionParametersAsThingProperties() {
        final List<Object> services = new LinkedList<>();

        inbox.add(testDiscoveryResult);

        final ThingTypeProvider thingTypeProvider = new ThingTypeProvider() {
            @Override
            public Collection<ThingType> getThingTypes(@Nullable Locale locale) {
                return Collections.singleton(testThingType);
            }

            @Override
            public @Nullable ThingType getThingType(ThingTypeUID thingTypeUID, @Nullable Locale locale) {
                return thingTypeUID.equals(testThingType.getUID()) ? testThingType : null;
            }
        };
        services.add(registerService(thingTypeProvider));
        final ThingTypeRegistry thingTypeRegistry = getService(ThingTypeRegistry.class);
        assertNotNull(thingTypeRegistry);
        waitForAssert(() -> assertNotNull(thingTypeRegistry.getThingType(testThingType.getUID())));

        final ConfigDescriptionProvider configDescriptionProvider = new ConfigDescriptionProvider() {
            @Override
            public Collection<ConfigDescription> getConfigDescriptions(@Nullable Locale locale) {
                return Collections.singleton(testConfigDescription);
            }

            @Override
            public @Nullable ConfigDescription getConfigDescription(URI uri, @Nullable Locale locale) {
                return uri.equals(testConfigDescription.getUID()) ? testConfigDescription : null;
            }
        };
        services.add(registerService(configDescriptionProvider));
        final ConfigDescriptionRegistry configDescriptionRegistry = getService(ConfigDescriptionRegistry.class);
        assertNotNull(configDescriptionRegistry);
        waitForAssert(
                () -> assertNotNull(configDescriptionRegistry.getConfigDescription(testConfigDescription.getUID())));

        Thing approvedThing = inbox.approve(testThing.getUID(), testThingLabel);
        Thing addedThing = registry.get(testThing.getUID());
        assertTrue(approvedThing.equals(addedThing));
        assertFalse(addedThing == null);
        for (String key : keysInConfigDescription) {
            Object thingConfItem = addedThing.getConfiguration().get(key);
            Object descResultParam = discoveryResultProperties.get(key);
            if (descResultParam instanceof Number) {
                descResultParam = new BigDecimal(descResultParam.toString());
            }
            assertFalse(thingConfItem == null);
            assertFalse(descResultParam == null);
            assertTrue(thingConfItem.equals(descResultParam));
        }
        for (String key : keysNotInConfigDescription) {
            String thingProperty = addedThing.getProperties().get(key);
            String descResultParam = String.valueOf(discoveryResultProperties.get(key));
            assertFalse(thingProperty == null);
            assertFalse(descResultParam == null);
            assertTrue(thingProperty.equals(descResultParam));
        }

        services.forEach(obj -> unregisterService(obj));
    }

    @Test
    public void assertThatRemoveOlderResultsOnlyRemovesResultsFromTheSameDiscoveryService() {
        inbox.thingDiscovered(discoveryService1, testDiscoveryResult);
        long now = new Date().getTime() + 1;
        assertThat(inbox.getAll().size(), is(1));

        // should not remove a result
        inbox.removeOlderResults(discoveryService2, now, singleton(testThingType.getUID()), null);
        assertThat(inbox.getAll().size(), is(1));

        // should remove a result
        inbox.removeOlderResults(discoveryService1, now, singleton(testThingType.getUID()), null);
        assertThat(inbox.getAll().size(), is(0));
    }

    @Test
    public void assertThatRemoveOlderResultsRemovesResultsWithoutAsource() {
        inbox.add(testDiscoveryResult);
        long now = new Date().getTime() + 1;
        assertThat(inbox.getAll().size(), is(1));

        // should remove a result
        inbox.removeOlderResults(discoveryService2, now, singleton(testThingType.getUID()), null);
        assertThat(inbox.getAll().size(), is(0));
    }

    class DummyThingHandlerFactory extends BaseThingHandlerFactory {

        public DummyThingHandlerFactory(ComponentContext context) {
            super.activate(context);
        }

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
            if (thingUID != null) {
                return ThingBuilder.create(thingTypeUID, thingUID).withBridge(bridgeUID)
                        .withConfiguration(configuration).build();
            }
            return null;
        }
    }

}
