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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openhab.core.config.discovery.inbox.InboxPredicates.withFlag;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryResultFlag;
import org.openhab.core.config.discovery.DiscoveryServiceRegistry;
import org.openhab.core.test.storage.VolatileStorageService;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.events.ThingStatusInfoChangedEvent;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.thing.type.ThingTypeRegistry;

/**
 * @author Andre Fuechsel - Initial contribution
 * @author Henning Sudbrock - Added tests for auto-approving inbox entries
 */
public class AutomaticInboxProcessorTest {

    private static final String DEVICE_ID = "deviceId";
    private static final String DEVICE_ID_KEY = "deviceIdKey";
    private static final String OTHER_KEY = "otherKey";
    private static final String OTHER_VALUE = "deviceId";
    private static final String CONFIG_KEY = "configKey";
    private static final String CONFIG_VALUE = "configValue";

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("test", "test");
    private static final ThingTypeUID THING_TYPE_UID2 = new ThingTypeUID("test2", "test2");
    private static final ThingTypeUID THING_TYPE_UID3 = new ThingTypeUID("test3", "test3");

    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "test");
    private static final ThingUID THING_UID2 = new ThingUID(THING_TYPE_UID, "test2");
    private static final ThingUID THING_UID3 = new ThingUID(THING_TYPE_UID3, "test3");

    private static final ThingType THING_TYPE = ThingTypeBuilder.instance(THING_TYPE_UID, "label").isListed(true)
            .withRepresentationProperty(DEVICE_ID_KEY).build();
    private static final ThingType THING_TYPE2 = ThingTypeBuilder.instance(THING_TYPE_UID2, "label").isListed(true)
            .withRepresentationProperty(CONFIG_KEY).build();
    private static final ThingType THING_TYPE3 = ThingTypeBuilder.instance(THING_TYPE_UID3, "label").isListed(true)
            .withRepresentationProperty(OTHER_KEY).build();

    private static final Map<String, String> THING_PROPERTIES = Collections.singletonMap(DEVICE_ID_KEY, DEVICE_ID);
    private static final Map<String, String> OTHER_THING_PROPERTIES = Collections.singletonMap(OTHER_KEY, OTHER_VALUE);

    private static final Configuration CONFIG = new Configuration(Collections.singletonMap(CONFIG_KEY, CONFIG_VALUE));

    private AutomaticInboxProcessor automaticInboxProcessor;
    private PersistentInbox inbox;

    @Mock
    private ThingRegistry thingRegistry;

    @Mock
    private ThingTypeRegistry thingTypeRegistry;

    @Mock
    private Thing thing;

    @Mock
    private Thing thing2;

    @Mock
    private Thing thing3;

    @Mock
    private ThingStatusInfoChangedEvent thingStatusInfoChangedEvent;

    @Mock
    private ConfigDescriptionRegistry configDescriptionRegistry;

    @Mock
    private ThingHandlerFactory thingHandlerFactory;

    @Mock
    private ManagedThingProvider thingProvider;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(thing.getConfiguration()).thenReturn(CONFIG);
        when(thing.getThingTypeUID()).thenReturn(THING_TYPE_UID);
        when(thing.getProperties()).thenReturn(THING_PROPERTIES);
        when(thing.getStatus()).thenReturn(ThingStatus.ONLINE);
        when(thing.getUID()).thenReturn(THING_UID);

        when(thing2.getConfiguration()).thenReturn(CONFIG);
        when(thing2.getThingTypeUID()).thenReturn(THING_TYPE_UID);
        when(thing2.getProperties()).thenReturn(THING_PROPERTIES);
        when(thing2.getStatus()).thenReturn(ThingStatus.ONLINE);
        when(thing2.getUID()).thenReturn(THING_UID2);

        when(thing3.getConfiguration()).thenReturn(CONFIG);
        when(thing3.getThingTypeUID()).thenReturn(THING_TYPE_UID3);
        when(thing3.getProperties()).thenReturn(OTHER_THING_PROPERTIES);
        when(thing3.getStatus()).thenReturn(ThingStatus.ONLINE);
        when(thing3.getUID()).thenReturn(THING_UID3);

        when(thingRegistry.stream()).thenReturn(Stream.empty());

        when(thingTypeRegistry.getThingType(THING_TYPE_UID)).thenReturn(THING_TYPE);
        when(thingTypeRegistry.getThingType(THING_TYPE_UID2)).thenReturn(THING_TYPE2);
        when(thingTypeRegistry.getThingType(THING_TYPE_UID3)).thenReturn(THING_TYPE3);

        when(thingHandlerFactory.supportsThingType(eq(THING_TYPE_UID))).thenReturn(true);
        when(thingHandlerFactory.supportsThingType(eq(THING_TYPE_UID3))).thenReturn(true);
        when(thingHandlerFactory.createThing(any(ThingTypeUID.class), any(Configuration.class), any(ThingUID.class),
                nullable(ThingUID.class)))
                        .then(invocation -> ThingBuilder
                                .create((ThingTypeUID) invocation.getArguments()[0],
                                        (ThingUID) invocation.getArguments()[2])
                                .withConfiguration((Configuration) invocation.getArguments()[1]).build());

        inbox = new PersistentInbox(new VolatileStorageService(), mock(DiscoveryServiceRegistry.class), thingRegistry,
                thingProvider, thingTypeRegistry, configDescriptionRegistry);
        inbox.addThingHandlerFactory(thingHandlerFactory);
        inbox.activate();

        automaticInboxProcessor = new AutomaticInboxProcessor(thingTypeRegistry, thingRegistry, inbox);
        automaticInboxProcessor.activate(null);
    }

    @After
    public void tearDown() {
        automaticInboxProcessor.deactivate();
    }

    /**
     * This test is just like the test testThingWentOnline in the AutomaticInboxProcessorTest, but in contrast to the
     * above test (where a thing with the same binding ID and the same representation property value went online) here a
     * thing with another binding ID and the same representation property value goes online.
     * <p/>
     * In this case, the discovery result should not be ignored, since it has a different thing type.
     */
    @Test
    public void testThingWithOtherBindingIDButSameRepresentationPropertyWentOnline() {
        // Add discovery result with thing type THING_TYPE_UID and representation property value DEVICE_ID
        inbox.add(DiscoveryResultBuilder.create(THING_UID).withProperty(DEVICE_ID_KEY, DEVICE_ID)
                .withRepresentationProperty(DEVICE_ID_KEY).build());

        // Then there is a discovery result which is NEW
        List<DiscoveryResult> results = inbox.stream().filter(withFlag(DiscoveryResultFlag.NEW))
                .collect(Collectors.toList());
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getThingUID(), is(equalTo(THING_UID)));

        // Now a thing with thing type THING_TYPE_UID3 goes online, with representation property value being also the
        // device id
        when(thingRegistry.get(THING_UID3)).thenReturn(thing3);
        when(thingStatusInfoChangedEvent.getStatusInfo())
                .thenReturn(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, null));
        when(thingStatusInfoChangedEvent.getThingUID()).thenReturn(THING_UID3);
        automaticInboxProcessor.receive(thingStatusInfoChangedEvent);

        // Then there should still be the NEW discovery result, but no IGNORED discovery result
        results = inbox.stream().filter(withFlag(DiscoveryResultFlag.NEW)).collect(Collectors.toList());
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getThingUID(), is(equalTo(THING_UID)));
        results = inbox.stream().filter(withFlag(DiscoveryResultFlag.IGNORED)).collect(Collectors.toList());
        assertThat(results.size(), is(0));
    }

    @Test
    public void testThingWithOtherBindingIDButSameRepresentationPropertyIsDiscovered() {
        // insert thing with thing type THING_TYPE_UID3 and representation property value DEVICE_ID in registry
        when(thingRegistry.get(THING_UID)).thenReturn(thing);
        when(thingRegistry.stream()).thenReturn(Stream.of(thing));

        // Add discovery result with thing type THING_TYPE_UID3 and representation property value DEVICE_ID
        inbox.add(DiscoveryResultBuilder.create(THING_UID3).withProperty(DEVICE_ID_KEY, DEVICE_ID)
                .withRepresentationProperty(DEVICE_ID_KEY).build());

        // Do NOT ignore this discovery result because it has a different binding ID
        List<DiscoveryResult> results = inbox.stream().filter(withFlag(DiscoveryResultFlag.IGNORED))
                .collect(Collectors.toList());
        assertThat(results.size(), is(0));

        // Then there is a discovery result which is NEW
        results = inbox.stream().filter(withFlag(DiscoveryResultFlag.NEW)).collect(Collectors.toList());
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getThingUID(), is(equalTo(THING_UID3)));
    }

    @Test
    public void testThingWentOnline() {
        inbox.add(DiscoveryResultBuilder.create(THING_UID).withProperty(DEVICE_ID_KEY, DEVICE_ID)
                .withRepresentationProperty(DEVICE_ID_KEY).build());

        List<DiscoveryResult> results = inbox.stream().filter(withFlag(DiscoveryResultFlag.NEW))
                .collect(Collectors.toList());
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getThingUID(), is(equalTo(THING_UID)));

        when(thingRegistry.get(THING_UID)).thenReturn(thing);
        when(thingStatusInfoChangedEvent.getStatusInfo())
                .thenReturn(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, null));
        when(thingStatusInfoChangedEvent.getThingUID()).thenReturn(THING_UID);
        automaticInboxProcessor.receive(thingStatusInfoChangedEvent);

        results = inbox.stream().filter(withFlag(DiscoveryResultFlag.NEW)).collect(Collectors.toList());
        assertThat(results.size(), is(0));
        results = inbox.stream().filter(withFlag(DiscoveryResultFlag.IGNORED)).collect(Collectors.toList());
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getThingUID(), is(equalTo(THING_UID)));
    }

    @Test
    public void testNoDiscoveryResultIfNoRepresentationPropertySet() {
        List<DiscoveryResult> results = inbox.stream().filter(withFlag(DiscoveryResultFlag.NEW))
                .collect(Collectors.toList());
        assertThat(results.size(), is(0));
    }

    @Test
    public void testThingWhenNoRepresentationPropertySet() {
        inbox.add(DiscoveryResultBuilder.create(THING_UID).withProperty(DEVICE_ID_KEY, DEVICE_ID).build());
        List<DiscoveryResult> results = inbox.stream().filter(withFlag(DiscoveryResultFlag.NEW))
                .collect(Collectors.toList());
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getThingUID(), is(equalTo(THING_UID)));

        when(thing.getProperties()).thenReturn(Collections.emptyMap());
        when(thingStatusInfoChangedEvent.getStatusInfo())
                .thenReturn(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, null));
        when(thingStatusInfoChangedEvent.getThingUID()).thenReturn(THING_UID);
        automaticInboxProcessor.receive(thingStatusInfoChangedEvent);

        results = inbox.stream().filter(withFlag(DiscoveryResultFlag.IGNORED)).collect(Collectors.toList());
        assertThat(results.size(), is(0));
    }

    @Test
    public void testInboxHasBeenChanged() {
        inbox.stream().map(DiscoveryResult::getThingUID).forEach(t -> inbox.remove(t));
        assertThat(inbox.getAll().size(), is(0));

        when(thingRegistry.get(THING_UID)).thenReturn(thing);
        when(thingRegistry.stream()).thenReturn(Stream.of(thing));

        inbox.add(DiscoveryResultBuilder.create(THING_UID2).withProperty(DEVICE_ID_KEY, DEVICE_ID)
                .withRepresentationProperty(DEVICE_ID_KEY).build());

        List<DiscoveryResult> results = inbox.stream().filter(withFlag(DiscoveryResultFlag.NEW))
                .collect(Collectors.toList());
        assertThat(results.size(), is(0));
        results = inbox.stream().filter(withFlag(DiscoveryResultFlag.IGNORED)).collect(Collectors.toList());
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getThingUID(), is(equalTo(THING_UID2)));
    }

    @Test
    public void testThingIsBeingRemoved() {
        inbox.add(DiscoveryResultBuilder.create(THING_UID).withProperty(DEVICE_ID_KEY, DEVICE_ID)
                .withRepresentationProperty(DEVICE_ID_KEY).build());

        inbox.setFlag(THING_UID, DiscoveryResultFlag.IGNORED);
        List<DiscoveryResult> results = inbox.stream().filter(withFlag(DiscoveryResultFlag.IGNORED))
                .collect(Collectors.toList());
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getThingUID(), is(equalTo(THING_UID)));

        automaticInboxProcessor.removed(thing);

        results = inbox.getAll();
        assertThat(results.size(), is(0));
    }

    @Test
    public void testOneThingOutOfTwoWithSameRepresentationPropertyButDifferentBindingIdIsBeingRemoved() {
        inbox.add(DiscoveryResultBuilder.create(THING_UID).withProperty(DEVICE_ID_KEY, DEVICE_ID)
                .withRepresentationProperty(DEVICE_ID_KEY).build());
        inbox.setFlag(THING_UID, DiscoveryResultFlag.IGNORED);

        inbox.add(DiscoveryResultBuilder.create(THING_UID3).withProperty(DEVICE_ID_KEY, DEVICE_ID)
                .withRepresentationProperty(DEVICE_ID_KEY).build());
        inbox.setFlag(THING_UID3, DiscoveryResultFlag.IGNORED);

        List<DiscoveryResult> results = inbox.stream().filter(withFlag(DiscoveryResultFlag.IGNORED))
                .collect(Collectors.toList());
        assertThat(results.size(), is(2));

        automaticInboxProcessor.removed(thing);

        results = inbox.getAll();
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getThingUID(), is(equalTo(THING_UID3)));
    }

    @Test
    public void testThingWithConfigWentOnline() {
        inbox.add(DiscoveryResultBuilder.create(THING_UID2).withProperty(OTHER_KEY, OTHER_VALUE)
                .withRepresentationProperty(OTHER_KEY).build());

        List<DiscoveryResult> results = inbox.stream().filter(withFlag(DiscoveryResultFlag.NEW))
                .collect(Collectors.toList());
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getThingUID(), is(equalTo(THING_UID2)));

        when(thingRegistry.get(THING_UID2)).thenReturn(thing2);
        when(thingStatusInfoChangedEvent.getStatusInfo())
                .thenReturn(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, null));
        when(thingStatusInfoChangedEvent.getThingUID()).thenReturn(THING_UID2);
        automaticInboxProcessor.receive(thingStatusInfoChangedEvent);

        results = inbox.stream().filter(withFlag(DiscoveryResultFlag.NEW)).collect(Collectors.toList());
        assertThat(results.size(), is(0));
        results = inbox.stream().filter(withFlag(DiscoveryResultFlag.IGNORED)).collect(Collectors.toList());
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getThingUID(), is(equalTo(THING_UID2)));
    }

    @Test
    public void testInboxWithConfigHasBeenChanged() {
        inbox.stream().map(DiscoveryResult::getThingUID).forEach(t -> inbox.remove(t));
        assertThat(inbox.getAll().size(), is(0));

        when(thingRegistry.get(THING_UID2)).thenReturn(thing2);
        when(thingRegistry.stream()).thenReturn(Stream.of(thing2));

        inbox.add(DiscoveryResultBuilder.create(THING_UID).withProperty(OTHER_KEY, OTHER_VALUE)
                .withRepresentationProperty(OTHER_KEY).build());

        List<DiscoveryResult> results = inbox.stream().filter(withFlag(DiscoveryResultFlag.NEW))
                .collect(Collectors.toList());
        assertThat(results.size(), is(0));
        results = inbox.stream().filter(withFlag(DiscoveryResultFlag.IGNORED)).collect(Collectors.toList());
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getThingUID(), is(equalTo(THING_UID)));
    }

    @Test
    public void testThingWithConfigIsBeingRemoved() {
        inbox.add(DiscoveryResultBuilder.create(THING_UID2).withProperty(OTHER_KEY, OTHER_VALUE)
                .withRepresentationProperty(OTHER_KEY).build());

        inbox.setFlag(THING_UID2, DiscoveryResultFlag.IGNORED);
        List<DiscoveryResult> results = inbox.stream().filter(withFlag(DiscoveryResultFlag.IGNORED))
                .collect(Collectors.toList());
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getThingUID(), is(equalTo(THING_UID2)));

        automaticInboxProcessor.removed(thing2);

        results = inbox.getAll();
        assertThat(results.size(), is(0));
    }

    @Test
    public void testAutomaticDiscoveryResultApprovalIfInboxEntriesAddedAfterApprovalPredicatesAreAdded() {
        automaticInboxProcessor.addInboxAutoApprovePredicate(
                discoveryResult -> THING_TYPE_UID.equals(discoveryResult.getThingTypeUID()));

        // The following discovery result is automatically approved, as it has matching thing type UID
        inbox.add(DiscoveryResultBuilder.create(THING_UID).build());
        verify(thingRegistry, times(1)).add(argThat(thing -> THING_UID.equals(thing.getUID())));

        // The following discovery result is not automatically approved, as it does not have matching thing type UID
        inbox.add(DiscoveryResultBuilder.create(THING_UID3).build());
        verify(thingRegistry, never()).add(argThat(thing -> THING_UID3.equals(thing.getUID())));
    }

    @Test
    public void testAutomaticDiscoveryResultApprovalIfInboxEntriesExistBeforeApprovalPredicatesAreAdded() {
        inbox.add(DiscoveryResultBuilder.create(THING_UID).build());
        inbox.add(DiscoveryResultBuilder.create(THING_UID2).build());
        inbox.add(DiscoveryResultBuilder.create(THING_UID3).build());

        // Adding this inboxAutoApprovePredicate will auto-approve the first two discovery results as they have matching
        // thing type UID.
        automaticInboxProcessor.addInboxAutoApprovePredicate(
                discoveryResult -> THING_TYPE_UID.equals(discoveryResult.getThingTypeUID()));

        verify(thingRegistry, times(1)).add(argThat(thing -> THING_UID.equals(thing.getUID())));
        verify(thingRegistry, times(1)).add(argThat(thing -> THING_UID2.equals(thing.getUID())));
        verify(thingRegistry, never()).add(argThat(thing -> THING_UID3.equals(thing.getUID())));

        // Adding this inboxAutoApprovePredicate will auto-approve the third discovery results as it has matching
        // thing type UID.
        automaticInboxProcessor.addInboxAutoApprovePredicate(
                discoveryResult -> THING_TYPE_UID3.equals(discoveryResult.getThingTypeUID()));

        verify(thingRegistry, times(1)).add(argThat(thing -> THING_UID.equals(thing.getUID())));
        verify(thingRegistry, times(1)).add(argThat(thing -> THING_UID2.equals(thing.getUID())));
        verify(thingRegistry, times(1)).add(argThat(thing -> THING_UID3.equals(thing.getUID())));
    }

    @Test
    public void testAlwaysAutoApproveInboxEntries() {
        // Before setting the always auto approve property, existing inbox results are not approved
        inbox.add(DiscoveryResultBuilder.create(THING_UID).build());

        verify(thingRegistry, never()).add(argThat(thing -> THING_UID.equals(thing.getUID())));

        // After setting the always auto approve property, all existing inbox results are approved.
        Map<String, Object> configProperties = new HashMap<>();
        configProperties.put(AutomaticInboxProcessor.ALWAYS_AUTO_APPROVE_CONFIG_PROPERTY, true);
        automaticInboxProcessor.activate(configProperties);

        verify(thingRegistry, times(1)).add(argThat(thing -> THING_UID.equals(thing.getUID())));

        // Newly added inbox results are also approved.
        inbox.add(DiscoveryResultBuilder.create(THING_UID2).build());
        verify(thingRegistry, times(1)).add(argThat(thing -> THING_UID2.equals(thing.getUID())));
    }

    @Test
    @Ignore("Should this test pass? It will fail currently, as RuntimeExceptions are not explicitly caught in AutomaticInboxProcessor#isToBeAutoApproved")
    public void testRogueInboxAutoApprovePredicatesDoNoHarm() {
        automaticInboxProcessor.addInboxAutoApprovePredicate(discoveryResult -> {
            throw new RuntimeException("I am an evil inboxAutoApprovePredicate");
        });
        automaticInboxProcessor.addInboxAutoApprovePredicate(
                discoveryResult -> THING_TYPE_UID.equals(discoveryResult.getThingTypeUID()));
        automaticInboxProcessor.addInboxAutoApprovePredicate(discoveryResult -> {
            throw new RuntimeException("I am another evil inboxAutoApprovePredicate");
        });

        // The discovery result is auto-approved in the presence of the evil predicates
        inbox.add(DiscoveryResultBuilder.create(THING_UID).build());
        verify(thingRegistry, times(1)).add(argThat(thing -> THING_UID.equals(thing.getUID())));
    }

}
