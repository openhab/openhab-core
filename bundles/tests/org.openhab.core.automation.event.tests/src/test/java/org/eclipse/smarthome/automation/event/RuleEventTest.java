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
package org.eclipse.smarthome.automation.event;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.RuleManager;
import org.eclipse.smarthome.automation.RuleRegistry;
import org.eclipse.smarthome.automation.RuleStatus;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.core.util.ModuleBuilder;
import org.eclipse.smarthome.automation.core.util.RuleBuilder;
import org.eclipse.smarthome.automation.events.RuleAddedEvent;
import org.eclipse.smarthome.automation.events.RuleRemovedEvent;
import org.eclipse.smarthome.automation.events.RuleStatusInfoEvent;
import org.eclipse.smarthome.automation.events.RuleUpdatedEvent;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemProvider;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.events.ItemCommandEvent;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.eclipse.smarthome.test.storage.VolatileStorageService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests events of rules
 *
 * @author Benedikt Niehues - initial contribution
 * @author Kai Kreuzer - ported test to Java
 *
 */
public class RuleEventTest extends JavaOSGiTest {

    final Logger logger = LoggerFactory.getLogger(RuleEventTest.class);
    VolatileStorageService volatileStorageService = new VolatileStorageService();

    Event itemEvent = null;
    Event ruleRemovedEvent = null;

    public RuleEventTest() {
    }

    @Before
    public void before() {
        ItemProvider itemProvider = new ItemProvider() {

            @Override
            public void addProviderChangeListener(@NonNull ProviderChangeListener<@NonNull Item> listener) {
            }

            @Override
            public @NonNull Collection<@NonNull Item> getAll() {
                return Arrays.asList(new Item[] { new SwitchItem("myMotionItem"), new SwitchItem("myPresenceItem"),
                        new SwitchItem("myLampItem"), new SwitchItem("myMotionItem2"),
                        new SwitchItem("myPresenceItem2"), new SwitchItem("myLampItem2") });
            }

            @Override
            public void removeProviderChangeListener(@NonNull ProviderChangeListener<@NonNull Item> listener) {
            }
        };
        registerService(itemProvider);
        registerVolatileStorageService();
    }

    @Test
    public void testRuleEvents() throws ItemNotFoundException {

        // Registering eventSubscriber
        List<Event> ruleEvents = new ArrayList<>();

        EventSubscriber ruleEventHandler = new EventSubscriber() {
            @Override
            public Set<String> getSubscribedEventTypes() {
                Set<String> types = new HashSet<>();
                types.add(RuleAddedEvent.TYPE);
                types.add(RuleRemovedEvent.TYPE);
                types.add(RuleStatusInfoEvent.TYPE);
                types.add(RuleUpdatedEvent.TYPE);
                return types;
            }

            @Override
            public EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event event) {
                logger.info("RuleEvent: {}", event.getTopic());
                ruleEvents.add(event);
            }
        };
        registerService(ruleEventHandler);

        // Creation of RULE
        Map<String, Object> triggerCfgEntries = new HashMap<>();
        triggerCfgEntries.put("eventSource", "myMotionItem2");
        triggerCfgEntries.put("eventTopic", "smarthome/*");
        triggerCfgEntries.put("eventTypes", "ItemStateEvent");
        Configuration triggerConfig = new Configuration(triggerCfgEntries);

        Map<String, Object> actionCfgEntries = new HashMap<>();
        actionCfgEntries.put("itemName", "myLampItem2");
        actionCfgEntries.put("command", "ON");
        Configuration actionConfig = new Configuration(actionCfgEntries);

        List<Trigger> triggers = Collections
                .singletonList(ModuleBuilder.createTrigger().withId("ItemStateChangeTrigger2")
                        .withTypeUID("core.GenericEventTrigger").withConfiguration(triggerConfig).build());
        List<Action> actions = Collections.singletonList(ModuleBuilder.createAction().withId("ItemPostCommandAction2")
                .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build());

        Rule rule = RuleBuilder.create("myRule21").withTriggers(triggers).withActions(actions)
                .withName("RuleEventTestingRule").build();

        logger.info("Rule created: {}", rule.getUID());

        RuleRegistry ruleRegistry = getService(RuleRegistry.class);
        RuleManager ruleEngine = getService(RuleManager.class);
        ruleRegistry.add(rule);
        ruleEngine.setEnabled(rule.getUID(), true);

        waitForAssert(() -> {
            assertThat(ruleEngine.getStatusInfo(rule.getUID()).getStatus(), is(RuleStatus.IDLE));
        });

        // TEST RULE

        EventPublisher eventPublisher = getService(EventPublisher.class);
        ItemRegistry itemRegistry = getService(ItemRegistry.class);
        SwitchItem myMotionItem = (SwitchItem) itemRegistry.getItem("myMotionItem2");
        assertNotNull(myMotionItem);
        eventPublisher.post(ItemEventFactory.createStateEvent("myPresenceItem2", OnOffType.ON));

        EventSubscriber itemEventHandler = new EventSubscriber() {

            @Override
            public void receive(Event event) {
                logger.info("Event: {}", event.getTopic());
                if (event instanceof ItemCommandEvent && event.getTopic().contains("myLampItem2")) {
                    itemEvent = event;
                }
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public EventFilter getEventFilter() {
                return null;
            }
        };
        registerService(itemEventHandler);
        eventPublisher.post(ItemEventFactory.createStateEvent("myMotionItem2", OnOffType.ON));
        waitForAssert(() -> assertThat(itemEvent, is(notNullValue())));
        assertThat(itemEvent.getTopic(), is(equalTo("smarthome/items/myLampItem2/command")));
        assertThat(((ItemCommandEvent) itemEvent).getItemCommand(), is(OnOffType.ON));
        assertThat(ruleEvents.size(), is(not(0)));
        assertThat(ruleEvents.stream().filter(e -> e.getTopic().equals("smarthome/rules/myRule21/added")).findFirst()
                .isPresent(), is(true));
        assertThat(ruleEvents.stream().filter(e -> e.getTopic().equals("smarthome/rules/myRule21/state")).findFirst()
                .isPresent(), is(true));
        List<Event> stateEvents = ruleEvents.stream().filter(e -> e.getTopic().equals("smarthome/rules/myRule21/state"))
                .collect(Collectors.toList());
        assertThat(stateEvents, is(notNullValue()));
        Optional<Event> runningEvent = stateEvents.stream()
                .filter(e -> ((RuleStatusInfoEvent) e).getStatusInfo().getStatus() == RuleStatus.RUNNING).findFirst();
        assertThat(runningEvent.isPresent(), is(true));

        EventSubscriber ruleRemovedEventHandler = new EventSubscriber() {

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Collections.singleton(RuleRemovedEvent.TYPE);
            }

            @Override
            public EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event event) {
                logger.info("RuleRemovedEvent: {}", event.getTopic());
                ruleRemovedEvent = event;
            }
        };
        registerService(ruleRemovedEventHandler);

        ruleRegistry.remove("myRule21");
        waitForAssert(() -> {
            assertThat(ruleRemovedEvent, is(notNullValue()));
            assertThat(ruleRemovedEvent.getTopic(), is(equalTo("smarthome/rules/myRule21/removed")));
        });
    }
}