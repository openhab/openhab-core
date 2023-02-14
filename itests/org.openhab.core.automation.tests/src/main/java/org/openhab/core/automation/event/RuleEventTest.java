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
package org.openhab.core.automation.event;

import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.events.RuleAddedEvent;
import org.openhab.core.automation.events.RuleRemovedEvent;
import org.openhab.core.automation.events.RuleStatusInfoEvent;
import org.openhab.core.automation.events.RuleUpdatedEvent;
import org.openhab.core.automation.internal.RuleEngineImpl;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemProvider;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.test.java.JavaOSGiTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests events of rules
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Kai Kreuzer - ported test to Java
 */
@NonNullByDefault
public class RuleEventTest extends JavaOSGiTest {

    private final Logger logger = LoggerFactory.getLogger(RuleEventTest.class);

    private @Nullable Event itemEvent = null;
    private @Nullable Event ruleRemovedEvent = null;

    public RuleEventTest() {
    }

    @BeforeEach
    public void before() {
        ItemProvider itemProvider = new ItemProvider() {

            @Override
            public void addProviderChangeListener(ProviderChangeListener<Item> listener) {
            }

            @Override
            public Collection<Item> getAll() {
                return List.of(new SwitchItem("myMotionItem"), new SwitchItem("myPresenceItem"),
                        new SwitchItem("myLampItem"), new SwitchItem("myMotionItem2"),
                        new SwitchItem("myPresenceItem2"), new SwitchItem("myLampItem2"));
            }

            @Override
            public void removeProviderChangeListener(ProviderChangeListener<Item> listener) {
            }
        };
        registerService(itemProvider);
        registerVolatileStorageService();

        // start rule engine
        RuleEngineImpl ruleEngine = Objects.requireNonNull((RuleEngineImpl) getService(RuleManager.class));
        ruleEngine.onReadyMarkerAdded(new ReadyMarker("", ""));
        waitForAssert(() -> assertTrue(ruleEngine.isStarted()));
    }

    @Test
    public void testRuleEvents() throws ItemNotFoundException {
        // Registering eventSubscriber
        List<Event> ruleEvents = new ArrayList<>();

        EventSubscriber ruleEventHandler = new EventSubscriber() {
            @Override
            public Set<String> getSubscribedEventTypes() {
                return Set.of(RuleAddedEvent.TYPE, RuleRemovedEvent.TYPE, RuleStatusInfoEvent.TYPE,
                        RuleUpdatedEvent.TYPE);
            }

            @Override
            public void receive(Event event) {
                logger.info("RuleEvent: {}", event.getTopic());
                ruleEvents.add(event);
            }
        };
        registerService(ruleEventHandler);

        // Creation of RULE
        Configuration triggerConfig = new Configuration(
                Map.ofEntries(entry("topic", "openhab/items/myMotionItem2/*"), entry("types", "ItemStateEvent")));

        Configuration actionConfig = new Configuration(
                Map.ofEntries(entry("itemName", "myLampItem2"), entry("command", "ON")));

        List<Trigger> triggers = List.of(ModuleBuilder.createTrigger().withId("ItemStateChangeTrigger2")
                .withTypeUID("core.GenericEventTrigger").withConfiguration(triggerConfig).build());
        List<Action> actions = List.of(ModuleBuilder.createAction().withId("ItemPostCommandAction2")
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
                return Set.of(ItemCommandEvent.TYPE);
            }
        };
        registerService(itemEventHandler);
        eventPublisher.post(ItemEventFactory.createStateEvent("myMotionItem2", OnOffType.ON));
        waitForAssert(() -> assertThat(itemEvent, is(notNullValue())));
        assertThat(itemEvent.getTopic(), is(equalTo("openhab/items/myLampItem2/command")));
        assertThat(((ItemCommandEvent) itemEvent).getItemCommand(), is(OnOffType.ON));
        assertThat(ruleEvents.size(), is(not(0)));
        assertThat(ruleEvents.stream().filter(e -> "openhab/rules/myRule21/added".equals(e.getTopic())).findFirst()
                .isPresent(), is(true));
        assertThat(ruleEvents.stream().filter(e -> "openhab/rules/myRule21/state".equals(e.getTopic())).findFirst()
                .isPresent(), is(true));
        List<Event> stateEvents = ruleEvents.stream().filter(e -> "openhab/rules/myRule21/state".equals(e.getTopic()))
                .collect(Collectors.toList());
        assertThat(stateEvents, is(notNullValue()));
        Optional<Event> runningEvent = stateEvents.stream()
                .filter(e -> ((RuleStatusInfoEvent) e).getStatusInfo().getStatus() == RuleStatus.RUNNING).findFirst();
        assertThat(runningEvent.isPresent(), is(true));

        EventSubscriber ruleRemovedEventHandler = new EventSubscriber() {

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Set.of(RuleRemovedEvent.TYPE);
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
            assertThat(ruleRemovedEvent.getTopic(), is(equalTo("openhab/rules/myRule21/removed")));
        });
    }
}
