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
package org.openhab.core.automation.integration.test;

import static java.util.stream.Collectors.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.ManagedRuleProvider;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleProvider;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.events.RuleAddedEvent;
import org.openhab.core.automation.events.RuleRemovedEvent;
import org.openhab.core.automation.events.RuleStatusInfoEvent;
import org.openhab.core.automation.events.RuleUpdatedEvent;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.RuleTemplateProvider;
import org.openhab.core.automation.template.Template;
import org.openhab.core.automation.template.TemplateRegistry;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeProvider;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.automation.type.TriggerType;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.ConfigDescriptionParameter;
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
import org.openhab.core.storage.StorageService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests the RuleEngineImpl.
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Marin Mitev - various fixes and extracted JSON parser test to separate file
 */
@NonNullByDefault
public class AutomationIntegrationTest extends JavaOSGiTest {

    private final Logger logger = LoggerFactory.getLogger(AutomationIntegrationTest.class);
    private @Nullable EventPublisher eventPublisher;
    private @Nullable ItemRegistry itemRegistry;
    private @Nullable RuleRegistry ruleRegistry;
    private @Nullable RuleManager ruleEngine;
    private @Nullable ManagedRuleProvider managedRuleProvider;
    private @Nullable ModuleTypeRegistry moduleTypeRegistry;
    private @Nullable TemplateRegistry<RuleTemplate> templateRegistry;

    private @Nullable Event ruleEvent;
    private @Nullable Event itemEvent;

    @Before
    public void before() {
        logger.info("@Before.begin");

        getService(ItemRegistry.class);

        ItemProvider itemProvider = new ItemProvider() {
            @Override
            public void addProviderChangeListener(ProviderChangeListener<Item> listener) {
            }

            @Override
            public Collection<Item> getAll() {
                Set<Item> items = new HashSet<>();
                items.add(new SwitchItem("myMotionItem"));
                items.add(new SwitchItem("myPresenceItem"));
                items.add(new SwitchItem("myLampItem"));
                items.add(new SwitchItem("myMotionItem2"));
                items.add(new SwitchItem("myPresenceItem2"));
                items.add(new SwitchItem("myLampItem2"));
                items.add(new SwitchItem("myMotionItem3"));
                items.add(new SwitchItem("templ_MotionItem"));
                items.add(new SwitchItem("templ_LampItem"));
                items.add(new SwitchItem("myMotionItem3"));
                items.add(new SwitchItem("myPresenceItem3"));
                items.add(new SwitchItem("myLampItem3"));
                items.add(new SwitchItem("myMotionItem4"));
                items.add(new SwitchItem("myPresenceItem4"));
                items.add(new SwitchItem("myLampItem4"));
                items.add(new SwitchItem("myMotionItem5"));
                items.add(new SwitchItem("myPresenceItem5"));
                items.add(new SwitchItem("myLampItem5"));
                items.add(new SwitchItem("xtempl_MotionItem"));
                items.add(new SwitchItem("xtempl_LampItem"));
                return items;
            }

            @Override
            public void removeProviderChangeListener(ProviderChangeListener<Item> listener) {
            }
        };

        registerService(itemProvider);
        registerVolatileStorageService();

        StorageService storageService = getService(StorageService.class);
        eventPublisher = getService(EventPublisher.class);
        itemRegistry = getService(ItemRegistry.class);
        ruleRegistry = getService(RuleRegistry.class);
        ruleEngine = getService(RuleManager.class);
        managedRuleProvider = getService(ManagedRuleProvider.class);
        moduleTypeRegistry = getService(ModuleTypeRegistry.class);
        templateRegistry = getService(TemplateRegistry.class);
        waitForAssert(() -> {
            assertThat(eventPublisher, is(notNullValue()));
            assertThat(storageService, is(notNullValue()));
            assertThat(itemRegistry, is(notNullValue()));
            assertThat(ruleRegistry, is(notNullValue()));
            assertThat(ruleEngine, is(notNullValue()));
            assertThat(moduleTypeRegistry, is(notNullValue()));
            assertThat(templateRegistry, is(notNullValue()));
            assertThat(managedRuleProvider, is(notNullValue()));
        }, 9000, 1000);
        logger.info("@Before.finish");
    }

    @After
    public void after() {
        logger.info("@After");
    }

    @Override
    protected void registerVolatileStorageService() {
        registerService(AutomationIntegrationJsonTest.VOLATILE_STORAGE_SERVICE);
    }

    @Test
    public void assertThatARuleCanBeAddedUpdatedAndRemovedByTheApi() {
        logger.info("assert that a rule can be added, updated and removed by the api");

        EventSubscriber ruleEventHandler = new EventSubscriber() {
            @Override
            public Set<String> getSubscribedEventTypes() {
                return Stream.of(RuleAddedEvent.TYPE, RuleRemovedEvent.TYPE, RuleUpdatedEvent.TYPE).collect(toSet());
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event e) {
                logger.info("RuleEvent: {}", e.getTopic());
                ruleEvent = e;
            }
        };
        registerService(ruleEventHandler);

        // ADD
        Rule rule = createSimpleRule();
        ruleRegistry.add(rule);
        waitForAssert(() -> {
            assertThat(ruleEvent, is(notNullValue()));
            assertThat(ruleEvent, is(instanceOf(RuleAddedEvent.class)));
            RuleAddedEvent ruleAddedEvent = (RuleAddedEvent) ruleEvent;
            assertThat(ruleAddedEvent.getRule().uid, is(rule.getUID()));
        }, 5000, 500);
        Rule ruleAdded = ruleRegistry.get(rule.getUID());
        assertThat(ruleAdded, is(notNullValue()));
        assertThat(ruleEngine.getStatusInfo(rule.getUID()).getStatus(), is(RuleStatus.IDLE));

        // UPDATE
        ruleEvent = null;
        if (ruleAdded == null) {
            throw new AssertionError("ruleAdded is null");
        }
        Rule updatedRule = RuleBuilder.create(ruleAdded).withDescription("TestDescription").build();
        Rule oldRule = ruleRegistry.update(updatedRule);
        waitForAssert(() -> {
            assertThat(ruleEvent, is(notNullValue()));
            assertThat(ruleEvent, is(instanceOf(RuleUpdatedEvent.class)));
            RuleUpdatedEvent ruEvent = (RuleUpdatedEvent) ruleEvent;
            assertThat(ruEvent.getRule().uid, is(rule.getUID()));
            assertThat(ruEvent.getOldRule().uid, is(rule.getUID()));
            assertThat(ruEvent.getRule().description, is("TestDescription"));
            assertThat(ruEvent.getOldRule().description, is(nullValue()));
        });
        assertThat(oldRule, is(notNullValue()));
        assertThat(oldRule, is(rule));

        // REMOVE
        ruleEvent = null;
        Rule removed = ruleRegistry.remove(rule.getUID());
        waitForAssert(() -> {
            assertThat(ruleEvent, is(notNullValue()));
            assertThat(ruleEvent, is(instanceOf(RuleRemovedEvent.class)));
            RuleRemovedEvent reEvent = (RuleRemovedEvent) ruleEvent;
            assertThat(reEvent.getRule().uid, is(removed.getUID()));
        });
        assertThat(removed, is(notNullValue()));
        assertThat(removed, is(ruleAdded));
        assertThat(ruleRegistry.get(removed.getUID()), is(nullValue()));
    }

    @Test
    public void assertThatARuleWithConnectionsIsExecuted() {
        logger.info("assert that a rule with connections is executed");
        Map<String, Object> params = new HashMap<>();
        params.put("eventSource", "myMotionItem3");
        params.put("eventTopic", "smarthome/*");
        params.put("eventTypes", "ItemStateEvent");
        Configuration triggerConfig = new Configuration(params);
        params = new HashMap<>();
        params.put("eventTopic", "smarthome/*");
        Configuration condition1Config = new Configuration(params);
        params = new HashMap<>();
        params.put("itemName", "myLampItem3");
        params.put("command", "ON");
        Configuration actionConfig = new Configuration(params);
        List<Trigger> triggers = Collections
                .singletonList(ModuleBuilder.createTrigger().withId("ItemStateChangeTrigger")
                        .withTypeUID("core.GenericEventTrigger").withConfiguration(triggerConfig).build());
        Map<String, String> inputs = new HashMap<>();
        inputs.put("topic", "ItemStateChangeTrigger.topic");
        inputs.put("event", "ItemStateChangeTrigger.event");

        List<Condition> conditions = Collections.singletonList(
                ModuleBuilder.createCondition().withId("EventCondition_2").withTypeUID("core.GenericEventCondition")
                        .withConfiguration(condition1Config).withInputs(inputs).build());

        List<Action> actions = Collections.singletonList(ModuleBuilder.createAction().withId("ItemPostCommandAction2")
                .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build());

        Rule rule = RuleBuilder.create("myRule21_ConnectionTest").withTriggers(triggers).withConditions(conditions)
                .withActions(actions).withName("RuleByJAVA_API" + new Random().nextInt()).build();
        ruleRegistry.add(rule);

        logger.info("Rule created and added: {}", rule.getUID());

        List<RuleStatusInfoEvent> ruleEvents = new CopyOnWriteArrayList<>();

        EventSubscriber ruleEventHandler = new EventSubscriber() {
            @Override
            public Set<String> getSubscribedEventTypes() {
                return Collections.singleton(RuleStatusInfoEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event e) {
                logger.info("RuleEvent: {}", e.getTopic());
                ruleEvents.add((RuleStatusInfoEvent) e);
            }
        };
        registerService(ruleEventHandler);

        eventPublisher.post(ItemEventFactory.createStateEvent("myMotionItem3", OnOffType.ON));

        waitForAssert(() -> {
            assertTrue(ruleEvents.stream().filter(r -> r.getStatusInfo().getStatus() == RuleStatus.RUNNING).findFirst()
                    .isPresent());
        });
        waitForAssert(() -> {
            assertThat(ruleEngine.getStatusInfo(rule.getUID()).getStatus(), is(not(RuleStatus.RUNNING)));
        });
    }

    @Test
    public void assertThatARuleWithNonExistingModuleTypeHandlerIsAddedToTheRuleRegistryInStateUNINITIALIZED() {
        logger.info(
                "assert that a rule with non existing moduleTypeHandler is added to the ruleRegistry in state UNINITIALIZED");
        Map<String, Object> params = new HashMap<>();
        params.put("eventSource", "myMotionItem");
        params.put("eventTopic", "smarthome/*");
        params.put("eventTypes", "ItemStateEvent");
        Configuration triggerConfig = new Configuration(params);
        params = new HashMap<>();
        params.put("topic", "smarthome/*");
        Configuration condition1Config = new Configuration(params);
        params = new HashMap<>();
        params.put("itemName", "myLampItem3");
        params.put("command", "ON");
        Configuration actionConfig = new Configuration(params);
        List<Trigger> triggers = Collections
                .singletonList(ModuleBuilder.createTrigger().withId("ItemStateChangeTrigger")
                        .withTypeUID("GenericEventTriggerWhichDoesNotExist").withConfiguration(triggerConfig).build());
        Map<String, String> inputs = new HashMap<>();
        inputs.put("topic", "ItemStateChangeTrigger.topic");
        inputs.put("event", "ItemStateChangeTrigger.event");

        // def conditionInputs=[topicConnection] as Set
        List<Condition> conditions = Collections.singletonList(
                ModuleBuilder.createCondition().withId("EventCondition_2").withTypeUID("core.GenericEventCondition")
                        .withConfiguration(condition1Config).withInputs(inputs).build());
        List<Action> actions = Collections.singletonList(ModuleBuilder.createAction().withId("ItemPostCommandAction2")
                .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build());

        Rule rule = RuleBuilder.create("myRule21_UNINITIALIZED").withTriggers(triggers).withConditions(conditions)
                .withActions(actions).withName("RuleByJAVA_API" + new Random().nextInt()).build();
        ruleRegistry.add(rule);
        assertThat(ruleEngine.getStatusInfo(rule.getUID()).getStatus(), is(RuleStatus.UNINITIALIZED));
    }

    @Test
    public void assertThatARuleBasedOnACompositeModuleIsInitializedAndExecutedCorrectly() {
        Map<String, Object> params = new HashMap<>();
        params.put("itemName", "myMotionItem3");
        Configuration triggerConfig = new Configuration(params);
        params = new HashMap<>();
        params.put("itemName", "myMotionItem3");
        params.put("state", "ON");
        Configuration condition1Config = new Configuration(params);
        Map<String, Object> eventInputs = Collections.singletonMap("event", "ItemStateChangeTrigger3.event");
        params = new HashMap<>();
        params.put("operator", "=");
        params.put("itemName", "myPresenceItem3");
        params.put("state", "ON");
        Configuration condition2Config = new Configuration(params);
        params = new HashMap<>();
        params.put("itemName", "myLampItem3");
        params.put("command", "ON");
        Configuration actionConfig = new Configuration(params);
        List<Trigger> triggers = Collections
                .singletonList(ModuleBuilder.createTrigger().withId("ItemStateChangeTrigger3")
                        .withTypeUID("core.ItemStateChangeTrigger").withConfiguration(triggerConfig).build());
        List<Action> actions = Collections.singletonList(ModuleBuilder.createAction().withId("ItemPostCommandAction3")
                .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build());

        Rule rule = RuleBuilder.create("myRule21" + new Random().nextInt() + "_COMPOSITE").withTriggers(triggers)
                .withActions(actions).withName("RuleByJAVA_API_WIthCompositeTrigger").build();

        logger.info("Rule created: {}", rule.getUID());

        RuleRegistry ruleRegistry = getService(RuleRegistry.class);
        ruleRegistry.add(rule);

        // TEST RULE
        waitForAssert(() -> {
            assertThat(ruleEngine.getStatusInfo(rule.getUID()).getStatus(), is(RuleStatus.IDLE));
        });

        EventPublisher eventPublisher = getService(EventPublisher.class);
        eventPublisher.post(ItemEventFactory.createStateEvent("myPresenceItem3", OnOffType.ON));

        EventSubscriber itemEventHandler = new EventSubscriber() {
            @Override
            public Set<String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event e) {
                logger.info("Event: {}", e.getTopic());
                if (e.getTopic().contains("myLampItem3")) {
                    itemEvent = e;
                }
            }
        };

        registerService(itemEventHandler);
        eventPublisher.post(ItemEventFactory.createStateEvent("myMotionItem3", OnOffType.ON));
        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
        }, 3000, 100);
        assertThat(itemEvent.getTopic(), is(equalTo("smarthome/items/myLampItem3/command")));
        assertThat(((ItemCommandEvent) itemEvent).getItemCommand(), is(OnOffType.ON));
    }

    @Test
    public void assertThatRuleNowMethodExecutesActionsOfTheRule() throws ItemNotFoundException {
        Configuration triggerConfig = new Configuration(Collections.singletonMap("eventTopic", "runNowEventTopic/*"));
        Map<String, Object> params = new HashMap<>();
        params.put("itemName", "myLampItem3");
        params.put("command", "TOGGLE");
        Configuration actionConfig = new Configuration(params);
        params = new HashMap<>();
        params.put("itemName", "myLampItem3");
        params.put("command", "ON");
        Configuration actionConfig2 = new Configuration(params);
        params = new HashMap<>();
        params.put("itemName", "myLampItem3");
        params.put("command", "OFFF");
        Configuration actionConfig3 = new Configuration(params);
        List<Trigger> triggers = Collections.singletonList(ModuleBuilder.createTrigger().withId("GenericEventTriggerId")
                .withTypeUID("core.GenericEventTrigger").withConfiguration(triggerConfig).build());
        List<Action> actions = Arrays.asList(new Action[] {
                ModuleBuilder.createAction().withId("ItemPostCommandActionId").withTypeUID("core.ItemCommandAction")
                        .withConfiguration(actionConfig).build(),
                ModuleBuilder.createAction().withId("ItemPostCommandActionId2").withTypeUID("core.ItemCommandAction")
                        .withConfiguration(actionConfig2).build(),
                ModuleBuilder.createAction().withId("ItemPostCommandActionId3").withTypeUID("core.ItemCommandAction")
                        .withConfiguration(actionConfig3).build() });

        Rule rule = RuleBuilder.create("runNowRule" + new Random().nextInt()).withTriggers(triggers)
                .withActions(actions).build();
        logger.info("Rule created: {}", rule.getUID());

        ruleRegistry.add(rule);

        // TEST RULE
        waitForAssert(() -> {
            assertThat(ruleEngine.getStatusInfo(rule.getUID()).getStatus(), is(RuleStatus.IDLE));
        }, 3000, 100);

        Item myLampItem3 = itemRegistry.getItem("myLampItem3");

        EventSubscriber itemEventHandler = new EventSubscriber() {
            @Override
            public Set<String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event e) {
                logger.info("Event: {}", e.getTopic());
                if (e.getTopic().contains("myLampItem3")) {
                    itemEvent = e;
                }
            }
        };
        registerService(itemEventHandler);

        ruleEngine.runNow(rule.getUID());
        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
        }, 3000, 100);
        waitForAssert(() -> {
            assertThat(((ItemCommandEvent) itemEvent).getItemCommand(), is(OnOffType.ON));
        }, 3000, 100);

        ruleRegistry.remove(rule.getUID());
    }

    @Test
    public void assertThatRuleCanBeUpdated() throws ItemNotFoundException {
        Configuration triggerConfig = new Configuration(Collections.singletonMap("eventTopic", "runNowEventTopic/*"));
        Map<String, Object> params = new HashMap<>();
        params.put("itemName", "myLampItem3");
        params.put("command", "ON");
        Configuration actionConfig = new Configuration(params);
        List<Trigger> triggers = Collections.singletonList(ModuleBuilder.createTrigger().withId("GenericEventTriggerId")
                .withTypeUID("core.GenericEventTrigger").withConfiguration(triggerConfig).build());
        List<Action> actions = Collections.singletonList(ModuleBuilder.createAction().withId("ItemPostCommandActionId")
                .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build());

        String ruleId = "runNowRule" + new Random().nextInt();
        Rule rule = RuleBuilder.create(ruleId).withTriggers(triggers).withActions(actions).build();
        logger.info("Rule created: {}", rule.getUID());

        ruleRegistry.add(rule);

        // TEST RULE
        waitForAssert(() -> {
            assertThat(ruleEngine.getStatusInfo(rule.getUID()).getStatus(), is(RuleStatus.IDLE));
        }, 3000, 100);

        EventSubscriber itemEventHandler = new EventSubscriber() {
            @Override
            public Set<String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event e) {
                logger.info("Event: {}", e.getTopic());
                if (e.getTopic().contains("myLampItem3")) {
                    itemEvent = e;
                }
            }
        };
        registerService(itemEventHandler);

        ruleEngine.runNow(rule.getUID());
        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
        }, 3000, 100);
        waitForAssert(() -> {
            assertThat(((ItemCommandEvent) itemEvent).getItemCommand(), is(OnOffType.ON));
        }, 3000, 100);

        params.put("command", "OFF");
        actionConfig = new Configuration(params);
        actions = Collections.singletonList(ModuleBuilder.createAction().withId("ItemPostCommandActionId")
                .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build());

        Rule updatedRule = RuleBuilder.create(ruleId).withTriggers(triggers).withActions(actions).build();
        ruleRegistry.update(updatedRule);
        waitForAssert(() -> {
            assertThat(ruleEngine.getStatusInfo(updatedRule.getUID()).getStatus(), is(RuleStatus.IDLE));
        }, 3000, 100);
        logger.info("Rule updated: {}", updatedRule.getUID());
        itemEvent = null;

        ruleEngine.runNow(updatedRule.getUID());
        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
        }, 3000, 100);
        waitForAssert(() -> {
            assertThat(((ItemCommandEvent) itemEvent).getItemCommand(), is(OnOffType.OFF));
        }, 3000, 100);

        ruleRegistry.remove(updatedRule.getUID());
    }

    @Test
    public void testChainOfCompositeModules() throws ItemNotFoundException {
        Configuration triggerConfig = new Configuration(Collections.singletonMap("itemName", "myMotionItem4"));
        Map<String, Object> eventInputs = Collections.singletonMap("event", "ItemStateChangeTrigger4.event");
        Map<String, Object> params = new HashMap<>();
        params.put("itemName", "myLampItem4");
        params.put("command", "ON");
        Configuration actionConfig = new Configuration(params);
        List<Trigger> triggers = Collections
                .singletonList(ModuleBuilder.createTrigger().withId("ItemStateChangeTrigger4")
                        .withTypeUID("core.ItemStateChangeTrigger").withConfiguration(triggerConfig).build());
        List<Action> actions = Collections.singletonList(ModuleBuilder.createAction().withId("ItemPostCommandAction4")
                .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build());

        Rule rule = RuleBuilder.create("myRule21" + new Random().nextInt() + "_COMPOSITE").withTriggers(triggers)
                .withActions(actions).withName("RuleByJAVA_API_ChainedComposite").build();
        logger.info("Rule created: {}", rule.getUID());

        RuleRegistry ruleRegistry = getService(RuleRegistry.class);
        ruleRegistry.add(rule);

        // TEST RULE
        waitForAssert(() -> {
            assertThat(ruleEngine.getStatusInfo(rule.getUID()).getStatus(), is(RuleStatus.IDLE));
        });

        EventPublisher eventPublisher = getService(EventPublisher.class);
        SwitchItem myPresenceItem = (SwitchItem) itemRegistry.getItem("myPresenceItem4");

        // prepare the presenceItems state to be on to match the second condition of the rule
        eventPublisher.post(ItemEventFactory.createStateEvent("myPresenceItem4", OnOffType.ON));

        EventSubscriber itemEventHandler = new EventSubscriber() {
            @Override
            public Set<String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event e) {
                logger.info("Event: {}", e.getTopic());
                if (e.getTopic().contains("myLampItem4")) {
                    itemEvent = e;
                }
            }
        };

        registerService(itemEventHandler);
        // causing the event to trigger the rule
        eventPublisher.post(ItemEventFactory.createStateEvent("myMotionItem4", OnOffType.ON));
        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
        }, 5000, 100);
        assertThat(itemEvent.getTopic(), is(equalTo("smarthome/items/myLampItem4/command")));
        assertThat(((ItemCommandEvent) itemEvent).getItemCommand(), is(OnOffType.ON));
    }

    @Test
    public void assertARuleAddedByApiIsExecutedAsExpected() {
        logger.info("assert a rule added by api is executed as expected");
        // Creation of RULE
        Map<String, Object> params = new HashMap<>();
        params.put("eventSource", "myMotionItem2");
        params.put("eventTopic", "smarthome/*");
        params.put("eventTypes", "ItemStateEvent");
        Configuration triggerConfig = new Configuration(params);
        params = new HashMap<>();
        params.put("itemName", "myLampItem2");
        params.put("command", "ON");
        Configuration actionConfig = new Configuration(params);
        List<Trigger> triggers = Collections
                .singletonList(ModuleBuilder.createTrigger().withId("ItemStateChangeTrigger2")
                        .withTypeUID("core.GenericEventTrigger").withConfiguration(triggerConfig).build());
        List<Action> actions = Collections.singletonList(ModuleBuilder.createAction().withId("ItemPostCommandAction2")
                .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build());

        Rule rule = RuleBuilder.create("myRule21").withTriggers(triggers).withActions(actions)
                .withName("RuleByJAVA_API").withTags("myRule21").build();
        logger.info("Rule created: {}", rule.getUID());

        ruleRegistry.add(rule);
        ruleEngine.setEnabled(rule.getUID(), true);
        ruleRegistry.remove(rule.getUID());
    }

    @Test
    public void assertThatARuleCanBeAddedByARuleProvider() {
        logger.info("assert that a rule can be added by a ruleProvider");
        Rule rule = createSimpleRule();

        RuleProvider ruleProvider = new RuleProvider() {
            @Override
            public void addProviderChangeListener(ProviderChangeListener<Rule> listener) {
            }

            @Override
            public Collection<Rule> getAll() {
                return Collections.singleton(rule);
            }

            @Override
            public void removeProviderChangeListener(ProviderChangeListener<Rule> listener) {
            }
        };

        registerService(ruleProvider);
        assertThat(ruleRegistry.get(rule.getUID()), is(notNullValue()));
        unregisterService(ruleProvider);
        assertThat(ruleRegistry.get(rule.getUID()), is(nullValue()));

        Rule rule2 = createSimpleRule();
        assertThat(ruleRegistry.get(rule2.getUID()), is(nullValue()));
        managedRuleProvider.add(rule2);
        assertThat(ruleRegistry.get(rule2.getUID()), is(notNullValue()));
        managedRuleProvider.remove(rule2.getUID());
        assertThat(ruleRegistry.get(rule2.getUID()), is(nullValue()));
    }

    @Test
    public void assertThatARuleCreatedFromATemplateIsExecutedAsExpected() {
        logger.info("assert that a rule created from a template is executed as expected");
        TemplateRegistry<?> templateRegistry = getService(TemplateRegistry.class);
        assertThat(templateRegistry, is(notNullValue()));
        waitForAssert(() -> {
            Template template = null;
            template = templateRegistry.get("SimpleTestTemplate");
            assertThat(template, is(notNullValue()));
            assertThat(template.getTags(), is(notNullValue()));
            assertThat(template.getTags().size(), is(not(0)));
        });

        Map<String, Object> configs = new HashMap<>();
        configs.put("onItem", "templ_MotionItem");
        configs.put("ifState", "ON");
        configs.put("updateItem", "templ_LampItem");
        configs.put("updateCommand", "ON");
        Rule templateRule = RuleBuilder.create("templateRuleUID").withTemplateUID("SimpleTestTemplate")
                .withConfiguration(new Configuration(configs)).build();
        ruleRegistry.add(templateRule);
        assertThat(ruleRegistry.get(templateRule.getUID()), is(notNullValue()));

        EventSubscriber itemEventHandler = new EventSubscriber() {
            @Override
            public Set<String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event e) {
                logger.info("Event: {}", e.getTopic());
                if (e.getTopic().contains("templ_LampItem")) {
                    itemEvent = e;
                }
            }
        };
        registerService(itemEventHandler);

        // causing the event to trigger the rule
        eventPublisher.post(ItemEventFactory.createStateEvent("templ_MotionItem", OnOffType.ON));
        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
        });
        assertThat(itemEvent.getTopic(), is(equalTo("smarthome/items/templ_LampItem/command")));
        assertThat(((ItemCommandEvent) itemEvent).getItemCommand(), is(OnOffType.ON));
    }

    @Test
    public void assertThatARuleCreatedFromAMoreComplexTemplateIsExecutedAsExpected() {
        logger.info("assert that a rule created from a more complex template is executed as expected");
        TemplateRegistry<?> templateRegistry = getService(TemplateRegistry.class);
        assertThat(templateRegistry, is(notNullValue()));
        waitForAssert(() -> {
            Template template = null;
            template = templateRegistry.get("TestTemplateWithCompositeModules");
            assertThat(template, is(notNullValue()));
            assertThat(template.getTags(), is(notNullValue()));
            assertThat(template.getTags().size(), is(not(0)));
        });

        Map<String, Object> configs = new HashMap<>();
        configs.put("onItem", "xtempl_MotionItem");
        configs.put("ifState", ".*ON.*");
        configs.put("updateItem", "xtempl_LampItem");
        configs.put("updateCommand", "ON");
        Configuration config = new Configuration(configs);
        Rule templateRule = RuleBuilder.create("xtemplateRuleUID").withTemplateUID("TestTemplateWithCompositeModules")
                .withConfiguration(config).build();

        ruleRegistry.add(templateRule);
        assertThat(ruleRegistry.get(templateRule.getUID()), is(notNullValue()));
        waitForAssert(() -> {
            assertThat(ruleRegistry.get(templateRule.getUID()), is(notNullValue()));
            assertThat(ruleEngine.getStatus(templateRule.getUID()), is(RuleStatus.IDLE));
        });

        EventSubscriber itemEventHandler = new EventSubscriber() {
            @Override
            public Set<String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event e) {
                logger.info("Event: {}", e.getTopic());
                if (e.getTopic().contains("xtempl_LampItem")) {
                    itemEvent = e;
                }
            }
        };
        registerService(itemEventHandler);

        // bring the rule to execution:
        eventPublisher.post(ItemEventFactory.createStateEvent("xtempl_MotionItem", OnOffType.ON));

        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
        });
        assertThat(itemEvent.getTopic(), is(equalTo("smarthome/items/xtempl_LampItem/command")));
        assertThat(((ItemCommandEvent) itemEvent).getItemCommand(), is(OnOffType.ON));
    }

    @Test
    public void testModuleTypeProviderAndTemplateProvider() {
        logger.info("test ModuleTypeProvider and TemplateProvider");
        TemplateRegistry<RuleTemplate> templateRegistry = getService(TemplateRegistry.class);
        ModuleTypeRegistry moduleTypeRegistry = getService(ModuleTypeRegistry.class);
        String templateUID = "testTemplate1";
        Set<String> tags = Stream.of("test", "testTag").collect(toSet());
        List<Trigger> templateTriggers = Collections.emptyList();
        List<Condition> templateConditions = Collections.emptyList();
        List<Action> templateActions = Collections.emptyList();
        List<ConfigDescriptionParameter> templateConfigDescriptionParameters = Collections
                .singletonList(new ConfigDescriptionParameter("param", ConfigDescriptionParameter.Type.TEXT));
        RuleTemplate template = new RuleTemplate(templateUID, "Test template Label", "Test template description", tags,
                templateTriggers, templateConditions, templateActions, templateConfigDescriptionParameters,
                Visibility.VISIBLE);

        String triggerTypeUID = "testTrigger1";
        TriggerType triggerType = new TriggerType(triggerTypeUID, templateConfigDescriptionParameters, null);
        String actionTypeUID = "testAction1";
        ActionType actionType = new ActionType(actionTypeUID, templateConfigDescriptionParameters, null);

        RuleTemplateProvider templateProvider = new RuleTemplateProvider() {
            @Override
            public @Nullable RuleTemplate getTemplate(String UID, @Nullable Locale locale) {
                if (UID == templateUID) {
                    return template;
                } else {
                    return null;
                }
            }

            @Override
            public Collection<RuleTemplate> getTemplates(@Nullable Locale locale) {
                return Collections.singleton(template);
            }

            @Override
            public void addProviderChangeListener(ProviderChangeListener<RuleTemplate> listener) {
            }

            @Override
            public Collection<RuleTemplate> getAll() {
                return Collections.singleton(template);
            }

            @Override
            public void removeProviderChangeListener(ProviderChangeListener<RuleTemplate> listener) {
            }
        };

        ModuleTypeProvider moduleTypeProvider = new ModuleTypeProvider() {
            @Override
            public void addProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
            }

            @Override
            public Collection<ModuleType> getAll() {
                return Stream.of(triggerType, actionType).collect(toSet());
            }

            @Override
            public void removeProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
            }

            @Override
            public <T extends ModuleType> @Nullable T getModuleType(String UID, @Nullable Locale locale) {
                if (UID == triggerTypeUID) {
                    return (T) triggerType;
                } else if (UID == actionTypeUID) {
                    return (T) actionType;
                } else {
                    return null;
                }
            }

            @Override
            public <T extends ModuleType> Collection<T> getModuleTypes(@Nullable Locale locale) {
                return (Collection<T>) Stream.of(triggerType, actionType).collect(toSet());
            }
        };

        registerService(templateProvider);
        assertThat(templateRegistry.get(templateUID), is(notNullValue()));
        unregisterService(templateProvider);
        assertThat(templateRegistry.get(templateUID), is(nullValue()));

        registerService(moduleTypeProvider);
        assertThat(moduleTypeRegistry.get(actionTypeUID), is(notNullValue()));
        assertThat(moduleTypeRegistry.get(triggerTypeUID), is(notNullValue()));
        unregisterService(moduleTypeProvider);
        assertThat(moduleTypeRegistry.get(actionTypeUID), is(nullValue()));
        assertThat(moduleTypeRegistry.get(triggerTypeUID), is(nullValue()));
    }

    /**
     * creates a simple rule
     */
    private Rule createSimpleRule() {
        logger.info("createSimpleRule");
        int rand = new Random().nextInt();

        Map<String, Object> configs = new HashMap<>();
        configs.put("eventSource", "myMotionItem2");
        configs.put("eventTopic", "smarthome/*");
        configs.put("eventTypes", "ItemStateEvent");
        Configuration triggerConfig = new Configuration(configs);
        configs = new HashMap<>();
        configs.put("itemName", "myLampItem2");
        configs.put("command", "ON");
        Configuration actionConfig = new Configuration(configs);
        String triggerUID = "ItemStateChangeTrigger_" + rand;
        List<Trigger> triggers = Collections.singletonList(ModuleBuilder.createTrigger().withId(triggerUID)
                .withTypeUID("core.GenericEventTrigger").withConfiguration(triggerConfig).build());
        List<Action> actions = Collections
                .singletonList(ModuleBuilder.createAction().withId("ItemPostCommandAction_" + rand)
                        .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build());

        Rule rule = RuleBuilder.create("myRule_" + rand).withTriggers(triggers).withActions(actions)
                .withName("RuleByJAVA_API_" + rand).build();
        logger.info("Rule created: {}", rule.getUID());
        return rule;
    }

    @Test
    public void assertARuleWithGenericConditionWorks() throws ItemNotFoundException {
        int random = new Random().nextInt(100000);
        logger.info("assert a rule with generic condition works");
        // Creation of RULE
        Map<String, Object> configs = new HashMap<>();
        configs.put("eventSource", "myMotionItem5");
        configs.put("eventTopic", "smarthome/*");
        configs.put("eventTypes", "ItemStateEvent");
        Configuration triggerConfig = new Configuration(configs);
        configs = new HashMap<>();
        configs.put("operator", "matches");
        configs.put("right", ".*ON.*");
        configs.put("inputproperty", "payload");
        Configuration condition1Config = new Configuration(configs);
        configs = new HashMap<>();
        configs.put("operator", "=");
        configs.put("right", "myMotionItem5");
        configs.put("inputproperty", "itemName");
        Configuration condition2Config = new Configuration(configs);
        configs = new HashMap<>();
        configs.put("itemName", "myLampItem5");
        configs.put("command", "ON");
        Configuration actionConfig = new Configuration(configs);
        String triggerId = "ItemStateChangeTrigger" + random;
        List<Trigger> triggers = Collections.singletonList(ModuleBuilder.createTrigger().withId(triggerId)
                .withTypeUID("core.GenericEventTrigger").withConfiguration(triggerConfig).build());
        List<Condition> conditions = Stream.of(
                ModuleBuilder.createCondition().withId("ItemStateCondition" + random)
                        .withTypeUID("core.GenericCompareCondition").withConfiguration(condition1Config)
                        .withInputs(Collections.singletonMap("input", triggerId + ".event")).build(),
                ModuleBuilder.createCondition().withId("ItemStateCondition" + (random + 1))
                        .withTypeUID("core.GenericCompareCondition").withConfiguration(condition2Config)
                        .withInputs(Collections.singletonMap("input", triggerId + ".event")).build())
                .collect(toList());

        List<Action> actions = Collections
                .singletonList(ModuleBuilder.createAction().withId("ItemPostCommandAction" + random)
                        .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build());

        Rule rule = RuleBuilder.create("myRule_" + random).withTriggers(triggers).withConditions(conditions)
                .withActions(actions).withName("RuleByJAVA_API" + random).withTags("myRule_" + random).build();
        logger.info("Rule created: {}", rule.getUID());

        ruleRegistry.add(rule);
        ruleEngine.setEnabled(rule.getUID(), true);

        // WAIT until Rule modules types are parsed and the rule becomes IDLE
        waitForAssert(() -> {
            assertThat(ruleRegistry.getAll().isEmpty(), is(false));
            Rule rule2 = ruleRegistry.get(rule.getUID());
            assertThat(rule2, is(notNullValue()));
            RuleStatus ruleStatus2 = ruleEngine.getStatusInfo(rule2.getUID()).getStatus();
            assertThat(ruleStatus2, is(RuleStatus.IDLE));
        }, 10000, 200);

        // TEST RULE

        EventPublisher eventPublisher = getService(EventPublisher.class);
        ItemRegistry itemRegistry = getService(ItemRegistry.class);
        SwitchItem myMotionItem = (SwitchItem) itemRegistry.getItem("myPresenceItem5");
        myMotionItem.setState(OnOffType.ON);

        EventSubscriber itemEventHandler = new EventSubscriber() {
            @Override
            public Set<String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event e) {
                logger.info("Event: {}", e.getTopic());
                if (e.getTopic().contains("myLampItem5")) {
                    itemEvent = e;
                }
            }
        };

        registerService(itemEventHandler);
        eventPublisher.post(ItemEventFactory.createStateEvent("myMotionItem5", OnOffType.ON));
        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
        }, 3000, 100);
        assertThat(itemEvent.getTopic(), is(equalTo("smarthome/items/myLampItem5/command")));
        assertThat(((ItemCommandEvent) itemEvent).getItemCommand(), is(OnOffType.ON));
    }
}
