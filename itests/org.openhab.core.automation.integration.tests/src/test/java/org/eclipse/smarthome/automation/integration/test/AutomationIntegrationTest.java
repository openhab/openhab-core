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
package org.eclipse.smarthome.automation.integration.test;

import static java.util.stream.Collectors.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
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
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.RuleManager;
import org.eclipse.smarthome.automation.RuleProvider;
import org.eclipse.smarthome.automation.RuleRegistry;
import org.eclipse.smarthome.automation.RuleStatus;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.Visibility;
import org.eclipse.smarthome.automation.core.ManagedRuleProvider;
import org.eclipse.smarthome.automation.core.util.ModuleBuilder;
import org.eclipse.smarthome.automation.core.util.RuleBuilder;
import org.eclipse.smarthome.automation.events.RuleAddedEvent;
import org.eclipse.smarthome.automation.events.RuleRemovedEvent;
import org.eclipse.smarthome.automation.events.RuleStatusInfoEvent;
import org.eclipse.smarthome.automation.events.RuleUpdatedEvent;
import org.eclipse.smarthome.automation.module.core.handler.GenericEventTriggerHandler;
import org.eclipse.smarthome.automation.template.RuleTemplate;
import org.eclipse.smarthome.automation.template.RuleTemplateProvider;
import org.eclipse.smarthome.automation.template.Template;
import org.eclipse.smarthome.automation.template.TemplateRegistry;
import org.eclipse.smarthome.automation.type.ActionType;
import org.eclipse.smarthome.automation.type.ModuleType;
import org.eclipse.smarthome.automation.type.ModuleTypeProvider;
import org.eclipse.smarthome.automation.type.ModuleTypeRegistry;
import org.eclipse.smarthome.automation.type.TriggerType;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
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
import org.eclipse.smarthome.core.storage.StorageService;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests the RuleEngineImpl.
 *
 * @author Benedikt Niehues - initial contribution
 * @author Marin Mitev - various fixes and extracted JSON parser test to separate file
 *
 */
public class AutomationIntegrationTest extends JavaOSGiTest {

    final Logger logger = LoggerFactory.getLogger(AutomationIntegrationTest.class);
    private EventPublisher eventPublisher;
    private ItemRegistry itemRegistry;
    private RuleRegistry ruleRegistry;
    private RuleManager ruleEngine;
    private ManagedRuleProvider managedRuleProvider;
    private ModuleTypeRegistry moduleTypeRegistry;
    private TemplateRegistry<RuleTemplate> templateRegistry;

    Event ruleEvent = null;
    Event itemEvent = null;

    @Before
    public void before() {
        logger.info("@Before.begin");

        getService(ItemRegistry.class);
        ItemProvider itemProvider = new ItemProvider() {

            @Override
            public void addProviderChangeListener(@NonNull ProviderChangeListener<@NonNull Item> listener) {
            }

            @Override
            public @NonNull Collection<@NonNull Item> getAll() {
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
            public void removeProviderChangeListener(@NonNull ProviderChangeListener<@NonNull Item> listener) {
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
            public @NonNull Set<@NonNull String> getSubscribedEventTypes() {
                return Stream.of(RuleAddedEvent.TYPE, RuleRemovedEvent.TYPE, RuleUpdatedEvent.TYPE).collect(toSet());
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(@NonNull Event e) {
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
            throw new NullPointerException();
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

        // def conditionInputs=[topicConnection] as Set
        List<Condition> conditions = Collections.singletonList(
                ModuleBuilder.createCondition().withId("EventCondition_2").withTypeUID("core.GenericEventCondition")
                        .withConfiguration(condition1Config).withInputs(inputs).build());

        List<Action> actions = Collections.singletonList(ModuleBuilder.createAction().withId("ItemPostCommandAction2")
                .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build());

        Rule rule = RuleBuilder.create("myRule21_ConnectionTest").withTriggers(triggers).withConditions(conditions)
                .withActions(actions).withName("RuleByJAVA_API" + new Random().nextInt()).build();
        ruleRegistry.add(rule);

        logger.info("Rule created and added: {}", rule.getUID());

        List<RuleStatusInfoEvent> ruleEvents = new ArrayList<>();

        EventSubscriber ruleEventHandler = new EventSubscriber() {

            @Override
            public @NonNull Set<@NonNull String> getSubscribedEventTypes() {
                return Collections.singleton(RuleStatusInfoEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(@NonNull Event e) {
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
    public void assertThatARuleSwitchesFromIDLEtoUNINITIALIZEDifAModuleHandlerDisappearsAndBackToIDLEifItAppearsAgain()
            throws BundleException {
        logger.info(
                "assert that a rule switches from IDLE to UNINITIALIZED if a moduleHanlder disappears and back to IDLE if it appears again");
        Rule rule = createSimpleRule();
        ruleRegistry.add(rule);
        assertThat(ruleEngine.getStatusInfo(rule.getUID()).getStatus(), is(RuleStatus.IDLE));

        Bundle moduleBundle = FrameworkUtil.getBundle(GenericEventTriggerHandler.class);
        moduleBundle.stop();
        waitForAssert(() -> {
            logger.info("RuleStatus: {}", ruleEngine.getStatusInfo(rule.getUID()).getStatus());
            assertThat(ruleEngine.getStatusInfo(rule.getUID()).getStatus(), is(RuleStatus.UNINITIALIZED));
        }, 3000, 100);

        moduleBundle.start();
        ruleEngine.setEnabled(rule.getUID(), true);
        waitForAssert(() -> {
            logger.info("RuleStatus: {}", ruleEngine.getStatusInfo(rule.getUID()));
            assertThat(ruleEngine.getStatusInfo(rule.getUID()).getStatus(), is(RuleStatus.IDLE));
        }, 3000, 100);
    }

    @Test
    @Ignore // this assumes that the sample.json bundle is started as part of the test, which is usually not the case
            // (and we should not have a fixed dependency on it
    public void assertThatAModuleTypesAndTemplatesAreDisappearedWhenTheProviderWasUninstalled() throws BundleException {
        logger.info("assert that a module types and templates are disappeared when the provider was uninstalled");

        waitForAssert(() -> {
            logger.info("RuleStatus: {}", moduleTypeRegistry.get("SampleTrigger"));
            assertThat(moduleTypeRegistry.get("SampleTrigger"), is(notNullValue()));
            assertThat(moduleTypeRegistry.get("SampleCondition"), is(notNullValue()));
            assertThat(moduleTypeRegistry.get("SampleAction"), is(notNullValue()));
            assertThat(templateRegistry.get("SampleRuleTemplate"), is(notNullValue()));
        }, 3000, 100);

        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().equals("org.eclipse.smarthome.automation.sample.extension.json")) {
                bundle.uninstall();
                break;
            }
        }

        waitForAssert(() -> {
            logger.info("RuleStatus: {}", moduleTypeRegistry.get("SampleTrigger"));
            assertThat(moduleTypeRegistry.get("SampleTrigger"), is(nullValue()));
            assertThat(moduleTypeRegistry.get("SampleCondition"), is(nullValue()));
            assertThat(moduleTypeRegistry.get("SampleAction"), is(nullValue()));
            assertThat(templateRegistry.get("SampleRuleTemplate"), is(nullValue()));
        }, 3000, 100);
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
            public @NonNull Set<@NonNull String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(@NonNull Event e) {
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
            public @NonNull Set<@NonNull String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(@NonNull Event e) {
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
            public @NonNull Set<@NonNull String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(@NonNull Event e) {
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
        logger.info("Rule created: " + rule.getUID());

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
            public @NonNull Set<@NonNull String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(@NonNull Event e) {
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
        logger.info("Rule created: " + rule.getUID());

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
            public void addProviderChangeListener(@NonNull ProviderChangeListener<Rule> listener) {
            }

            @Override
            public @NonNull Collection<Rule> getAll() {
                return Collections.singleton(rule);
            }

            @Override
            public void removeProviderChangeListener(@NonNull ProviderChangeListener<Rule> listener) {
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
            public @NonNull Set<@NonNull String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(@NonNull Event e) {
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
            public @NonNull Set<@NonNull String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(@NonNull Event e) {
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
            public RuleTemplate getTemplate(String UID, Locale locale) {
                if (UID == templateUID) {
                    return template;
                } else {
                    return null;
                }
            }

            @Override
            public Collection<RuleTemplate> getTemplates(Locale locale) {
                return Collections.singleton(template);
            }

            @Override
            public void addProviderChangeListener(@NonNull ProviderChangeListener<RuleTemplate> listener) {
            }

            @Override
            public @NonNull Collection<RuleTemplate> getAll() {
                return Collections.singleton(template);
            }

            @Override
            public void removeProviderChangeListener(@NonNull ProviderChangeListener<RuleTemplate> listener) {
            }
        };

        ModuleTypeProvider moduleTypeProvider = new ModuleTypeProvider() {

            @Override
            public void addProviderChangeListener(@NonNull ProviderChangeListener<ModuleType> listener) {
            }

            @Override
            public @NonNull Collection<ModuleType> getAll() {
                return Stream.of(triggerType, actionType).collect(toSet());
            }

            @Override
            public void removeProviderChangeListener(@NonNull ProviderChangeListener<ModuleType> listener) {
            }

            @Override
            public <T extends ModuleType> T getModuleType(String UID, Locale locale) {
                if (UID == triggerTypeUID) {
                    return (T) triggerType;
                } else if (UID == actionTypeUID) {
                    return (T) actionType;
                } else {
                    return null;
                }
            }

            @Override
            public <T extends ModuleType> Collection<T> getModuleTypes(Locale locale) {
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
        logger.info("Rule created: " + rule.getUID());

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
            public @NonNull Set<@NonNull String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(@NonNull Event e) {
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
