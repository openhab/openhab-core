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
package org.openhab.core.automation.internal.module;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.RuleStatusDetail;
import org.openhab.core.automation.events.RuleStatusInfoEvent;
import org.openhab.core.automation.internal.RuleEngineImpl;
import org.openhab.core.automation.internal.module.handler.CompareConditionHandler;
import org.openhab.core.automation.type.ModuleTypeRegistry;
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
import org.openhab.core.test.storage.VolatileStorageService;
import org.openhab.core.types.Command;
import org.openhab.core.types.TypeParser;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests the RuleEngineImpl.
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Markus Rathgeb - Migrated Groovy tests to pure Java ones and made it more robust
 */
@NonNullByDefault
public class RuntimeRuleTest extends JavaOSGiTest {

    private final Logger logger = LoggerFactory.getLogger(RuntimeRuleTest.class);
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
                return List.of(new SwitchItem("myMotionItem"), new SwitchItem("myPresenceItem"),
                        new SwitchItem("myLampItem"), new SwitchItem("myMotionItem2"),
                        new SwitchItem("myPresenceItem2"), new SwitchItem("myLampItem2"),
                        new SwitchItem("myMotionItem3"), new SwitchItem("myPresenceItem3"),
                        new SwitchItem("myLampItem3"), new SwitchItem("myMotionItem4"),
                        new SwitchItem("myPresenceItem4"), new SwitchItem("myLampItem4"));
            }
        });
        registerService(volatileStorageService);

        // start rule engine
        RuleEngineImpl ruleEngine = Objects.requireNonNull((RuleEngineImpl) getService(RuleManager.class));
        ruleEngine.onReadyMarkerAdded(new ReadyMarker("", ""));
        waitForAssert(() -> assertTrue(ruleEngine.isStarted()));
    }

    private void subscribeToEvents(String eventType, final Queue<Event> events) {
        EventSubscriber eventSubscriber = new EventSubscriber() {
            @Override
            public void receive(final Event event) {
                logger.info("Event: {}", event.getTopic());
                events.add(event);
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Set.of(eventType);
            }
        };

        ServiceReference<?> subscriberReference = registerService(eventSubscriber).getReference();
        assertNotNull(getServices(EventSubscriber.class, (reference) -> reference.equals(subscriberReference)));
    }

    @Test
    @Disabled
    public void testPredefinedRule() throws ItemNotFoundException, InterruptedException {
        final Queue<Event> events = new LinkedList<>();
        subscribeToEvents(ItemCommandEvent.TYPE, events);

        final EventPublisher eventPublisher = getService(EventPublisher.class);
        eventPublisher.post(ItemEventFactory.createStateEvent("myMotionItem", OnOffType.ON));

        waitForAssert(() -> {
            assertFalse(events.isEmpty());
            ItemCommandEvent event = (ItemCommandEvent) events.remove();
            assertEquals("openhab/items/myLampItem/command", event.getTopic());
            assertEquals(OnOffType.ON, event.getItemCommand());
        });
    }

    @Test
    public void itemStateUpdatedBySimpleRule() throws ItemNotFoundException, InterruptedException {
        final Configuration triggerConfig = new Configuration(
                Map.ofEntries(entry("topic", "openhab/items/myMotionItem2/*"), entry("types", "ItemStateEvent")));
        final Configuration actionConfig = new Configuration(
                Map.ofEntries(entry("itemName", "myLampItem2"), entry("command", "ON")));
        final Rule rule = RuleBuilder.create("myRule21" + new Random().nextInt())
                .withTriggers(ModuleBuilder.createTrigger().withId("ItemStateChangeTrigger2")
                        .withTypeUID("core.GenericEventTrigger").withConfiguration(triggerConfig).build())
                .withActions(ModuleBuilder.createAction().withId("ItemPostCommandAction2")
                        .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build())
                .withName("RuleByJAVA_API").build();

        logger.info("RuleImpl created: {}", rule.getUID());

        final RuleRegistry ruleRegistry = getService(RuleRegistry.class);
        final RuleManager ruleEngine = getService(RuleManager.class);
        ruleRegistry.add(rule);
        ruleEngine.setEnabled(rule.getUID(), true);

        waitForAssert(() -> {
            assertEquals(RuleStatus.IDLE, ruleEngine.getStatusInfo(rule.getUID()).getStatus());
        });

        // Test rule
        final EventPublisher eventPublisher = getService(EventPublisher.class);
        assertNotNull(eventPublisher);

        eventPublisher.post(ItemEventFactory.createStateEvent("myPresenceItem2", OnOffType.ON));

        final Queue<Event> events = new LinkedList<>();

        subscribeToEvents(ItemCommandEvent.TYPE, events);

        eventPublisher.post(ItemEventFactory.createStateEvent("myMotionItem2", OnOffType.ON));

        waitForAssert(() -> {
            assertFalse(events.isEmpty());
            ItemCommandEvent event = (ItemCommandEvent) events.remove();
            assertEquals("openhab/items/myLampItem2/command", event.getTopic());
            assertEquals(OnOffType.ON, event.getItemCommand());
        });
    }

    @Test
    public void modeTypesRegistration() {
        final ModuleTypeRegistry mtr = getService(ModuleTypeRegistry.class);
        waitForAssert(() -> {
            assertNotNull(mtr.get("core.GenericEventTrigger"));
            assertNotNull(mtr.get("core.GenericEventCondition"));
            assertNotNull(mtr.get("core.ItemStateChangeTrigger"));
            assertNotNull(mtr.get("core.ItemStateUpdateTrigger"));
            assertNotNull(mtr.get(CompareConditionHandler.MODULE_TYPE));
        });
    }

    private Configuration newRightOperatorConfig(final Object right, final Object operator) {
        return new Configuration(Map.of("right", right, "operator", operator));
    }

    private Configuration newRightOperatorInputPropertyConfig(final Object right, final Object operator,
            final Object inputProperty) {
        return new Configuration(Map.of("right", right, "operator", operator, "inputproperty", inputProperty));
    }

    private void assertSatisfiedHandlerInput(final CompareConditionHandler handler, final boolean expected,
            final @Nullable Object input) {
        final boolean is = handler.isSatisfied(Collections.singletonMap("input", input));
        if (expected) {
            assertTrue(is);
        } else {
            assertFalse(is);
        }
    }

    @Test
    public void compareConditionWorks() {
        final Configuration conditionConfig = newRightOperatorConfig("ON", "=");
        final Map<String, String> inputs = Map.of("input", "someTrigger.someoutput");
        Condition condition = ModuleBuilder.createCondition().withId("id").withTypeUID("core.GenericCompareCondition")
                .withConfiguration(conditionConfig).withInputs(inputs).build();
        CompareConditionHandler handler = new CompareConditionHandler(condition);

        assertSatisfiedHandlerInput(handler, true, OnOffType.ON);
        assertSatisfiedHandlerInput(handler, true, "ON");
        assertSatisfiedHandlerInput(handler, false, OnOffType.OFF);
        assertSatisfiedHandlerInput(handler, false, "OFF");

        condition = ModuleBuilder.createCondition(condition).withConfiguration(newRightOperatorConfig("21", "="))
                .build();
        handler = new CompareConditionHandler(condition);
        assertSatisfiedHandlerInput(handler, true, 21);
        assertSatisfiedHandlerInput(handler, false, 22);

        condition = ModuleBuilder.createCondition(condition).withConfiguration(newRightOperatorConfig("21", "<"))
                .build();
        handler = new CompareConditionHandler(condition);
        assertSatisfiedHandlerInput(handler, true, 20);
        assertSatisfiedHandlerInput(handler, false, 22);

        assertSatisfiedHandlerInput(handler, true, 20l);
        assertSatisfiedHandlerInput(handler, false, 22l);

        assertSatisfiedHandlerInput(handler, true, 20.9d);
        assertSatisfiedHandlerInput(handler, false, 21.1d);

        condition = ModuleBuilder.createCondition(condition).withConfiguration(newRightOperatorConfig("21", "<="))
                .build();
        handler = new CompareConditionHandler(condition);
        assertSatisfiedHandlerInput(handler, true, 20);
        assertSatisfiedHandlerInput(handler, true, 21);
        assertSatisfiedHandlerInput(handler, false, 22);

        assertSatisfiedHandlerInput(handler, true, 20l);
        assertSatisfiedHandlerInput(handler, true, 21l);
        assertSatisfiedHandlerInput(handler, false, 22l);

        assertSatisfiedHandlerInput(handler, true, 20.9d);
        assertSatisfiedHandlerInput(handler, true, 21.0d);
        assertSatisfiedHandlerInput(handler, false, 21.1d);

        condition = ModuleBuilder.createCondition(condition).withConfiguration(newRightOperatorConfig("21", "<"))
                .build();
        handler = new CompareConditionHandler(condition);
        assertSatisfiedHandlerInput(handler, true, 20);
        assertSatisfiedHandlerInput(handler, false, 22);

        assertSatisfiedHandlerInput(handler, true, 20l);
        assertSatisfiedHandlerInput(handler, false, 22l);

        assertSatisfiedHandlerInput(handler, true, 20.9d);
        assertSatisfiedHandlerInput(handler, false, 21.1d);

        condition = ModuleBuilder.createCondition(condition).withConfiguration(newRightOperatorConfig("21", "<="))
                .build();
        handler = new CompareConditionHandler(condition);
        assertSatisfiedHandlerInput(handler, true, 20);
        assertSatisfiedHandlerInput(handler, true, 21);
        assertSatisfiedHandlerInput(handler, false, 22);

        assertSatisfiedHandlerInput(handler, true, 20l);
        assertSatisfiedHandlerInput(handler, true, 21l);
        assertSatisfiedHandlerInput(handler, false, 22l);

        assertSatisfiedHandlerInput(handler, true, 20.9d);
        assertSatisfiedHandlerInput(handler, true, 21.0d);
        assertSatisfiedHandlerInput(handler, false, 21.1d);

        condition = ModuleBuilder.createCondition(condition)
                .withConfiguration(newRightOperatorConfig(".*anything.*", "matches")).build();
        handler = new CompareConditionHandler(condition);
        assertSatisfiedHandlerInput(handler, false, "something matches?");
        assertSatisfiedHandlerInput(handler, true, "anything matches?");

        assertFalse(handler.isSatisfied(Map.of("nothing", "nothing")));

        condition = ModuleBuilder.createCondition(condition)
                .withConfiguration(newRightOperatorConfig("ONOFF", "matches")).build();
        handler = new CompareConditionHandler(condition);
        assertSatisfiedHandlerInput(handler, false, OnOffType.ON);

        final Event event = ItemEventFactory.createStateEvent("itemName", OnOffType.OFF, "source");
        condition = ModuleBuilder.createCondition(condition)
                .withConfiguration(newRightOperatorInputPropertyConfig(".*ON.*", "matches", "itemName")).build();
        handler = new CompareConditionHandler(condition);
        assertSatisfiedHandlerInput(handler, false, event);
        condition = ModuleBuilder.createCondition(condition)
                .withConfiguration(newRightOperatorInputPropertyConfig("itemName", "matches", "itemName")).build();
        handler = new CompareConditionHandler(condition);
        assertSatisfiedHandlerInput(handler, true, event);

        condition = ModuleBuilder.createCondition(condition).withConfiguration(newRightOperatorConfig("null", "="))
                .build();
        handler = new CompareConditionHandler(condition);
        assertSatisfiedHandlerInput(handler, true, null);
        condition = ModuleBuilder.createCondition(condition).withConfiguration(newRightOperatorConfig("notnull", "="))
                .build();
        handler = new CompareConditionHandler(condition);
        assertSatisfiedHandlerInput(handler, false, null);
        condition = ModuleBuilder.createCondition(condition).withConfiguration(newRightOperatorConfig("ON", "<"))
                .build();
        handler = new CompareConditionHandler(condition);
        assertSatisfiedHandlerInput(handler, false, OnOffType.ON);

        condition = ModuleBuilder.createCondition(condition)
                .withConfiguration(newRightOperatorInputPropertyConfig("ON", "<", "nothing")).build();
        handler = new CompareConditionHandler(condition);
        assertSatisfiedHandlerInput(handler, false, event);
        condition = ModuleBuilder.createCondition(condition)
                .withConfiguration(newRightOperatorInputPropertyConfig("ON", "=", "nothing")).build();
        handler = new CompareConditionHandler(condition);
        assertSatisfiedHandlerInput(handler, true, "ON");
    }

    @Test
    public void ruleTriggeredByCompositeTrigger() throws ItemNotFoundException, InterruptedException {
        final Configuration triggerConfig = new Configuration(Map.of("itemName", "myMotionItem3"));
        final Configuration actionConfig = new Configuration(
                Map.ofEntries(entry("itemName", "myLampItem3"), entry("command", "ON")));
        final Rule rule = RuleBuilder.create("myRule21" + new Random().nextInt() + "_COMPOSITE")
                .withTriggers(ModuleBuilder.createTrigger().withId("ItemStateChangeTrigger3")
                        .withTypeUID("core.ItemStateChangeTrigger").withConfiguration(triggerConfig).build())
                .withActions(ModuleBuilder.createAction().withId("ItemPostCommandAction3")
                        .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build())
                .withName("RuleByJAVA_API_WithCompositeTrigger").build();

        logger.info("RuleImpl created: {}", rule.getUID());

        final RuleRegistry ruleRegistry = getService(RuleRegistry.class);
        final RuleManager ruleEngine = getService(RuleManager.class);
        ruleRegistry.add(rule);

        // Test rule

        waitForAssert(() -> {
            assertEquals(RuleStatus.IDLE, ruleEngine.getStatusInfo(rule.getUID()).getStatus());
        });

        final Queue<Event> events = new LinkedList<>();
        subscribeToEvents(RuleStatusInfoEvent.TYPE, events);

        final EventPublisher eventPublisher = getService(EventPublisher.class);
        eventPublisher.post(ItemEventFactory.createStateEvent("myPresenceItem3", OnOffType.ON));
        eventPublisher.post(ItemEventFactory.createStateEvent("myMotionItem3", OnOffType.ON));

        waitForAssert(() -> {
            assertFalse(events.isEmpty());
            RuleStatusInfoEvent event = (RuleStatusInfoEvent) events.remove();
            assertEquals(RuleStatus.RUNNING, event.getStatusInfo().getStatus());
        });

        waitForAssert(() -> {
            assertFalse(events.isEmpty());
            RuleStatusInfoEvent event = (RuleStatusInfoEvent) events.remove();
            assertEquals(RuleStatus.IDLE, event.getStatusInfo().getStatus());
        });
    }

    @Test
    @Disabled
    public void ruleEnableHandlerWorks() throws ItemNotFoundException {
        final RuleRegistry ruleRegistry = getService(RuleRegistry.class);
        final RuleManager ruleEngine = getService(RuleManager.class);
        final String firstRuleUID = "FirstTestRule";
        final String secondRuleUID = "SecondTestRule";
        final String thirdRuleUID = "ThirdTestRule";
        final String[] firstConfig = new String[] { "FirstTestRule", "SecondTestRule" };
        final String[] secondConfig = new String[] { "FirstTestRule" };

        final String firstRuleAction = "firstRuleAction";
        final String secondRuleAction = "secondRuleAction";

        try {
            final Configuration triggerConfig = new Configuration(Map.of("itemName", "myMotionItem3"));
            final Configuration actionConfig = new Configuration(
                    Map.ofEntries(entry("enable", false), entry("ruleUIDs", firstConfig)));

            final Rule rule = RuleBuilder.create(firstRuleAction)
                    .withTriggers(ModuleBuilder.createTrigger().withId("ItemStateChangeTrigger3")
                            .withTypeUID("core.ItemStateChangeTrigger").withConfiguration(triggerConfig).build())
                    .withActions(ModuleBuilder.createAction().withId("RuleAction")
                            .withTypeUID("core.RuleEnablementAction").withConfiguration(actionConfig).build())
                    .build();

            ruleRegistry.add(RuleBuilder.create(firstRuleUID).build());
            ruleRegistry.add(RuleBuilder.create(secondRuleUID).build());
            ruleRegistry.add(RuleBuilder.create(thirdRuleUID).build());
            ruleRegistry.add(rule);

            final ItemRegistry itemRegistry = getService(ItemRegistry.class);
            final EventPublisher eventPublisher = getService(EventPublisher.class);
            final Item myMotionItem = itemRegistry.getItem("myMotionItem3");
            Command command = TypeParser.parseCommand(myMotionItem.getAcceptedCommandTypes(), "ON");
            assertNotNull(command);
            if (command != null) {
                eventPublisher.post(ItemEventFactory.createCommandEvent("myMotionItem3", command));
            }

            waitForAssert(() -> {
                assertEquals(RuleStatusDetail.DISABLED, ruleEngine.getStatusInfo(firstRuleUID).getStatusDetail());
                assertEquals(RuleStatusDetail.DISABLED, ruleEngine.getStatusInfo(secondRuleUID).getStatusDetail());
                assertEquals(RuleStatus.IDLE, ruleEngine.getStatus(thirdRuleUID));
            });

            final Configuration triggerConfig2 = new Configuration(Map.of("itemName", "myMotionItem3"));
            final Configuration actionConfig2 = new Configuration(
                    Map.ofEntries(entry("enable", true), entry("ruleUIDs", secondConfig)));

            final Rule rule2 = RuleBuilder.create(secondRuleAction)
                    .withTriggers(ModuleBuilder.createTrigger().withId("ItemStateChangeTrigger3")
                            .withTypeUID("core.ItemStateChangeTrigger").withConfiguration(triggerConfig2).build())
                    .withActions(ModuleBuilder.createAction().withId("RuleAction")
                            .withTypeUID("core.RuleEnablementAction").withConfiguration(actionConfig2).build())
                    .build();
            ruleRegistry.add(rule2);

            command = TypeParser.parseCommand(myMotionItem.getAcceptedCommandTypes(), "OFF");
            assertNotNull(command);
            if (command != null) {
                eventPublisher.post(ItemEventFactory.createCommandEvent("myMotionItem3", command));
            }

            waitForAssert(() -> {
                assertEquals(RuleStatus.IDLE, ruleEngine.getStatus(firstRuleUID));
                assertEquals(RuleStatusDetail.DISABLED, ruleEngine.getStatusInfo(secondRuleUID).getStatusDetail());
                assertEquals(RuleStatus.IDLE, ruleEngine.getStatus(thirdRuleUID));
            });
        } finally {
            ruleRegistry.remove(firstRuleUID);
            ruleRegistry.remove(secondRuleUID);
            ruleRegistry.remove(thirdRuleUID);
            ruleRegistry.remove(firstRuleAction);
            ruleRegistry.remove(secondRuleAction);
        }
    }
}
