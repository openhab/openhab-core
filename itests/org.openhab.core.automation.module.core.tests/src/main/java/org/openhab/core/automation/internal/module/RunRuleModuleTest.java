/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.module;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.internal.RuleEngineImpl;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
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
import org.openhab.core.test.storage.VolatileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests the RunRuleAction
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Markus Rathgeb - Migrated Groovy tests to pure Java ones and made it more robust
 */
@NonNullByDefault
public class RunRuleModuleTest extends JavaOSGiTest {

    private final Logger logger = LoggerFactory.getLogger(RunRuleModuleTest.class);
    private final VolatileStorageService volatileStorageService = new VolatileStorageService();

    @BeforeEach
    public void before() {
        registerService(new ItemProvider() {
            @Override
            public void addProviderChangeListener(final ProviderChangeListener<Item> listener) {
            }

            @Override
            public void removeProviderChangeListener(final ProviderChangeListener<Item> listener) {
            }

            @Override
            public Collection<Item> getAll() {
                return List.of(new SwitchItem("switch1"), new SwitchItem("switch2"), new SwitchItem("switch3"),
                        new SwitchItem("ruleTrigger"));
            }
        });
        registerService(volatileStorageService);

        // start rule engine
        ((RuleEngineImpl) getService(RuleManager.class)).onReadyMarkerAdded(new ReadyMarker("", ""));
    }

    private Rule createSceneRule() {
        final Configuration sceneRuleAction1Config = new Configuration(
                Map.ofEntries(entry("itemName", "switch1"), entry("command", "ON")));
        final Configuration sceneRuleAction2Config = new Configuration(
                Map.ofEntries(entry("itemName", "switch2"), entry("command", "ON")));
        final Configuration sceneRuleAction3Config = new Configuration(
                Map.ofEntries(entry("itemName", "switch3"), entry("command", "ON")));

        final Rule sceneRule = RuleBuilder.create("exampleSceneRule").withActions(
                ModuleBuilder.createAction().withId("sceneItemPostCommandAction1").withTypeUID("core.ItemCommandAction")
                        .withConfiguration(sceneRuleAction1Config).build(),
                ModuleBuilder.createAction().withId("sceneItemPostCommandAction2").withTypeUID("core.ItemCommandAction")
                        .withConfiguration(sceneRuleAction2Config).build(),
                ModuleBuilder.createAction().withId("sceneItemPostCommandAction3").withTypeUID("core.ItemCommandAction")
                        .withConfiguration(sceneRuleAction3Config).build())
                .withName("Example Scene").build();

        return sceneRule;
    }

    private Rule createOuterRule() {
        final Configuration outerRuleTriggerConfig = new Configuration(
                Map.ofEntries(entry("eventSource", "ruleTrigger"), entry("eventTopic", "openhab/*"),
                        entry("eventTypes", "ItemStateEvent")));

        final List<String> ruleUIDs = new ArrayList<>();
        ruleUIDs.add("exampleSceneRule");

        final Configuration outerRuleActionConfig = new Configuration(Map.of("ruleUIDs", ruleUIDs));

        final Rule outerRule = RuleBuilder.create("sceneActivationRule")
                .withTriggers(ModuleBuilder.createTrigger().withId("ItemStateChangeTrigger2")
                        .withTypeUID("core.GenericEventTrigger").withConfiguration(outerRuleTriggerConfig).build())
                .withActions(ModuleBuilder.createAction().withId("RunRuleAction1").withTypeUID("core.RunRuleAction")
                        .withConfiguration(outerRuleActionConfig).build())
                .withName("scene activator").build();

        return outerRule;
    }

    @Test
    public void sceneActivatedByRule() throws ItemNotFoundException, InterruptedException {
        final RuleRegistry ruleRegistry = getService(RuleRegistry.class);
        final RuleManager ruleEngine = getService(RuleManager.class);
        assertNotNull(ruleRegistry);

        // Scene rule

        final Rule sceneRule = createSceneRule();
        logger.info("SceneRule created: {}", sceneRule.getUID());

        ruleRegistry.add(sceneRule);
        ruleEngine.setEnabled(sceneRule.getUID(), true);
        waitForAssert(() -> {
            assertEquals(RuleStatus.IDLE, ruleEngine.getStatusInfo(sceneRule.getUID()).getStatus());
        });

        // Outer rule

        final Rule outerRule = createOuterRule();
        logger.info("SceneActivationRule created: {}", outerRule.getUID());

        ruleRegistry.add(outerRule);
        ruleEngine.setEnabled(outerRule.getUID(), true);
        waitForAssert(() -> {
            assertEquals(RuleStatus.IDLE, ruleEngine.getStatusInfo(outerRule.getUID()).getStatus());
        });

        // Test rule

        final EventPublisher eventPublisher = getService(EventPublisher.class);
        assertNotNull(eventPublisher);

        final ItemRegistry itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);

        final Queue<Event> events = new LinkedList<>();

        registerService(new EventSubscriber() {
            @Override
            public void receive(final Event event) {
                logger.info("Event: {}", event.getTopic());
                events.add(event);
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Set.of(ItemCommandEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }
        });

        // trigger rule by switching triggerItem ON
        eventPublisher.post(ItemEventFactory.createStateEvent("ruleTrigger", OnOffType.ON));
        waitForAssert(() -> {
            assertFalse(events.isEmpty());
            ItemCommandEvent event = (ItemCommandEvent) events.remove();
            assertEquals("openhab/items/switch3/command", event.getTopic());
            assertEquals(OnOffType.ON, event.getItemCommand());
        });
    }
}
