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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.RuleManager;
import org.eclipse.smarthome.automation.RulePredicates;
import org.eclipse.smarthome.automation.RuleRegistry;
import org.eclipse.smarthome.automation.RuleStatus;
import org.eclipse.smarthome.automation.RuleStatusInfo;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.core.ManagedRuleProvider;
import org.eclipse.smarthome.automation.events.RuleStatusInfoEvent;
import org.eclipse.smarthome.automation.type.ActionType;
import org.eclipse.smarthome.automation.type.Input;
import org.eclipse.smarthome.automation.type.ModuleTypeRegistry;
import org.eclipse.smarthome.automation.type.Output;
import org.eclipse.smarthome.automation.type.TriggerType;
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
import org.eclipse.smarthome.core.types.UnDefType;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.eclipse.smarthome.test.storage.VolatileStorageService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests the RuleEngineImpl and the import from JSON resources contained in the ESH-INF folder.
 * This test must be run first otherwise imported rules will be cleared.
 *
 * @author Benedikt Niehues - initial contribution
 * @author Marin Mitev - make the test to pass on each run
 * @author Kai Kreuzer - refactored to Java
 *
 */
public class AutomationIntegrationJsonTest extends JavaOSGiTest {

    final Logger logger = LoggerFactory.getLogger(AutomationIntegrationJsonTest.class);
    private EventPublisher eventPublisher;
    private ItemRegistry itemRegistry;
    private RuleRegistry ruleRegistry;
    private RuleManager ruleManager;
    private ManagedRuleProvider managedRuleProvider;
    private ModuleTypeRegistry moduleTypeRegistry;
    private Event ruleEvent;
    public Event itemEvent;

    public static VolatileStorageService VOLATILE_STORAGE_SERVICE = new VolatileStorageService(); // keep storage with
                                                                                                  // rules imported from
                                                                                                  // json files

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
                HashSet<Item> items = new HashSet<>();
                items.add(new SwitchItem("myMotionItem"));
                items.add(new SwitchItem("myPresenceItem"));
                items.add(new SwitchItem("myLampItem"));
                items.add(new SwitchItem("myMotionItem2"));
                items.add(new SwitchItem("myPresenceItem2"));
                items.add(new SwitchItem("myLampItem2"));
                items.add(new SwitchItem("myMotionItem11"));
                items.add(new SwitchItem("myLampItem11"));
                items.add(new SwitchItem("myMotionItem3"));
                items.add(new SwitchItem("templ_MotionItem"));
                items.add(new SwitchItem("templ_LampItem"));

                return items;
            }

            @Override
            public void removeProviderChangeListener(@NonNull ProviderChangeListener<@NonNull Item> listener) {
            }
        };

        registerService(itemProvider);
        registerVolatileStorageService();

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
                logger.info("RuleEvent: " + e.getTopic() + " --> " + e.getPayload());
                System.out.println("RuleEvent: " + e.getTopic() + " --> " + e.getPayload());
                if (e.getPayload().contains("RUNNING")) {
                    ruleEvent = e;
                }
            }
        };

        registerService(ruleEventHandler);

        StorageService storageService = getService(StorageService.class);
        managedRuleProvider = getService(ManagedRuleProvider.class);
        eventPublisher = getService(EventPublisher.class);
        itemRegistry = getService(ItemRegistry.class);
        ruleRegistry = getService(RuleRegistry.class);
        ruleManager = getService(RuleManager.class);
        moduleTypeRegistry = getService(ModuleTypeRegistry.class);

        waitForAssert(() -> {
            assertThat(storageService, is(notNullValue()));
            // sometimes assert fails because EventPublisher service is null
            assertThat(eventPublisher, is(notNullValue()));
            assertThat(itemRegistry, is(notNullValue()));
            assertThat(ruleRegistry, is(notNullValue()));
            assertThat(ruleManager, is(notNullValue()));
            assertThat(managedRuleProvider, is(notNullValue()));
            assertThat(moduleTypeRegistry, is(notNullValue()));
        }, 9000, 1000);
        logger.info("@Before.finish");

    }

    @After
    public void after() {
        logger.info("@After");
    }

    @Override
    protected void registerVolatileStorageService() {
        registerService(VOLATILE_STORAGE_SERVICE);
    }

    @Test
    public void assertThatModuleTypeInputsAndOutputsFromJsonFileAreParsedCorrectly() {
        logger.info("assert that module type inputs and outputs from json file are parsed correctly");

        // WAIT until module type resources are parsed
        waitForAssert(() -> {
            assertThat(moduleTypeRegistry.getTriggers().isEmpty(), is(false));
            assertThat(moduleTypeRegistry.getActions().isEmpty(), is(false));

            TriggerType moduleType1 = (TriggerType) moduleTypeRegistry.get("CustomTrigger1");
            TriggerType moduleType2 = (TriggerType) moduleTypeRegistry.get("CustomTrigger2");
            ActionType moduleType3 = (ActionType) moduleTypeRegistry.get("CustomAction1");
            ActionType moduleType4 = (ActionType) moduleTypeRegistry.get("CustomAction2");

            assertThat(moduleType1.getOutputs(), is(notNullValue()));
            Optional<Output> output1 = moduleType1.getOutputs().stream()
                    .filter(o -> o.getName().equals("customTriggerOutput1")).findFirst();
            assertThat(output1.isPresent(), is(true));
            assertThat(output1.get().getDefaultValue(), is("true"));

            assertThat(moduleType2.getOutputs(), is(notNullValue()));
            Optional<Output> output2 = moduleType2.getOutputs().stream()
                    .filter(o -> o.getName().equals("customTriggerOutput2")).findFirst();
            assertThat(output2.isPresent(), is(true));
            assertThat(output2.get().getDefaultValue(), is("event"));

            assertThat(moduleType4.getInputs(), is(notNullValue()));
            Optional<Input> input = moduleType4.getInputs().stream()
                    .filter(o -> o.getName().equals("customActionInput")).findFirst();
            assertThat(input.isPresent(), is(true));
            assertThat(input.get().getDefaultValue(), is("5"));

            assertThat(moduleType3.getOutputs(), is(notNullValue()));
            Optional<Output> output3 = moduleType3.getOutputs().stream()
                    .filter(o -> o.getName().equals("customActionOutput3")).findFirst();
            assertThat(output3.isPresent(), is(true));
            assertThat(output3.get().getDefaultValue(), is("{\"command\":\"OFF\"}"));
        }, 10000, 200);

    }

    @Test
    public void assertThatARuleFromJsonFileIsAddedAutomatically() {
        logger.info("assert that a rule from json file is added automatically");

        // WAIT until Rule modules types are parsed and the rule becomes IDLE
        waitForAssert(() -> {
            assertThat(ruleRegistry.getAll().isEmpty(), is(false));
            Rule rule2 = ruleRegistry.stream().filter(
                    RulePredicates.hasAnyOfTags("jsonTest").and(RulePredicates.hasAnyOfTags("references").negate()))
                    .findFirst().orElse(null);
            assertThat(rule2, is(notNullValue()));
            RuleStatusInfo ruleStatus2 = ruleManager.getStatusInfo(rule2.getUID());
            assertThat(ruleStatus2.getStatus(), is(RuleStatus.IDLE));
        }, 10000, 200);

        Rule rule = ruleRegistry.stream()
                .filter(RulePredicates.hasAnyOfTags("jsonTest").and(RulePredicates.hasAnyOfTags("references").negate()))
                .findFirst().orElse(null);
        assertThat(rule, is(notNullValue()));
        assertThat(rule.getName(), is("ItemSampleRule"));
        assertTrue(rule.getTags().contains("sample"));
        assertTrue(rule.getTags().contains("item"));
        assertTrue(rule.getTags().contains("rule"));
        Optional<? extends Trigger> trigger = rule.getTriggers().stream()
                .filter(t -> t.getId().equals("ItemStateChangeTriggerID")).findFirst();
        assertThat(trigger.isPresent(), is(true));
        assertThat(trigger.get().getTypeUID(), is("core.GenericEventTrigger"));
        assertThat(trigger.get().getConfiguration().get("eventSource"), is("myMotionItem"));
        assertThat(trigger.get().getConfiguration().get("eventTopic"), is("smarthome/items/*"));
        assertThat(trigger.get().getConfiguration().get("eventTypes"), is("ItemStateEvent"));
        // def condition1 = rule.conditions.find{it.id.equals("ItemStateConditionID")} as Condition
        // assertThat(condition1, is(notNullValue())
        // assertThat(condition1.typeUID, is("core.GenericEventCondition")
        // assertThat(condition1.configuration.get("topic"), is("smarthome/items/myMotionItem/state")
        // assertThat(condition1.configuration.get("payload"), is(".*ON.*")
        Optional<? extends Action> action = rule.getActions().stream()
                .filter(a -> a.getId().equals("ItemPostCommandActionID")).findFirst();
        assertThat(action.isPresent(), is(true));
        assertThat(action.get().getTypeUID(), is("core.ItemCommandAction"));
        assertThat(action.get().getConfiguration().get("itemName"), is("myLampItem"));
        assertThat(action.get().getConfiguration().get("command"), is("ON"));
        RuleStatusInfo ruleStatus = ruleManager.getStatusInfo(rule.getUID());
        assertThat(ruleStatus.getStatus(), is(RuleStatus.IDLE));
    }

    @Test
    public void assertThatARuleFromJsonFileIsAddedAutomaticallyAndTheRuntimeRuleHasResolvedModuleReferences() {
        logger.info(
                "assert that a rule from json file is added automatically and the runtime rule has resolved module references");

        // WAIT until Rule modules types are parsed and the rule becomes IDLE
        waitForAssert(() -> {
            assertThat(ruleRegistry.getAll().isEmpty(), is(false));
            Rule rule2 = ruleRegistry.stream().filter(RulePredicates.hasAllTags("jsonTest", "references")).findFirst()
                    .orElse(null);
            assertThat(rule2, is(notNullValue()));
            RuleStatusInfo ruleStatus2 = ruleManager.getStatusInfo(rule2.getUID());
            assertThat(ruleStatus2.getStatus(), is(RuleStatus.IDLE));
        }, 10000, 200);
        Rule rule = ruleRegistry.stream().filter(RulePredicates.hasAllTags("jsonTest", "references")).findFirst()
                .orElse(null);
        assertThat(rule, is(notNullValue()));
        assertThat(rule.getName(), is("ItemSampleRuleWithReferences"));
        assertTrue(rule.getTags().contains("sample"));
        assertTrue(rule.getTags().contains("item"));
        assertTrue(rule.getTags().contains("rule"));
        assertTrue(rule.getTags().contains("references"));
        Optional<? extends Trigger> trigger = rule.getTriggers().stream()
                .filter(t -> t.getId().equals("ItemStateChangeTriggerID")).findFirst();
        assertThat(trigger.isPresent(), is(true));
        assertThat(trigger.get().getTypeUID(), is("core.GenericEventTrigger"));
        assertThat(trigger.get().getConfiguration().get("eventTopic"), is("smarthome/items/*"));
        assertThat(trigger.get().getConfiguration().get("eventTypes"), is("ItemStateEvent"));
        // def condition1 = rule.conditions.find{it.id.equals("ItemStateConditionID")} as Condition
        // assertThat(condition1, is(notNullValue())
        // assertThat(condition1.typeUID, is("core.GenericEventCondition")
        // assertThat(condition1.configuration.get("topic"), is("smarthome/items/myMotionItem/state")
        // assertThat(condition1.configuration.get("payload"), is(".*ON.*")
        Optional<? extends Action> action = rule.getActions().stream()
                .filter(a -> a.getId().equals("ItemPostCommandActionID")).findFirst();
        assertThat(action.isPresent(), is(true));
        assertThat(action.get().getTypeUID(), is("core.ItemCommandAction"));
        assertThat(action.get().getConfiguration().get("command"), is("ON"));
        RuleStatusInfo ruleStatus = ruleManager.getStatusInfo(rule.getUID());
        assertThat(ruleStatus.getStatus(), is(RuleStatus.IDLE));

        // run the rule to check if the runtime rule has resolved module references and is executed successfully
        EventPublisher eventPublisher = getService(EventPublisher.class);

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
                if (e.getTopic().contains("myLampItem")) {
                    itemEvent = e;
                }
            }

        };

        registerService(itemEventHandler);
        eventPublisher.post(ItemEventFactory.createStateEvent("myMotionItem", OnOffType.ON));
        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
        }, 3000, 100);
        assertThat(itemEvent.getTopic(), is(equalTo("smarthome/items/myLampItem/command")));
        assertThat(((ItemCommandEvent) itemEvent).getItemCommand(), is(OnOffType.ON));
    }

    @Test
    public void assertThatARuleFromJsonFileIsExecutedCorrectly() throws ItemNotFoundException {
        logger.info("assert that rule added by json is executed correctly");
        waitForAssert(() -> {
            assertThat(ruleRegistry.getAll().isEmpty(), is(false));
            Rule r = ruleRegistry.get("ItemSampleRule");
            assertThat(r, is(notNullValue()));
            assertThat(ruleManager.getStatusInfo(r.getUID()).getStatus(), is(RuleStatus.IDLE));
        }, 9000, 200);

        SwitchItem myPresenceItem = (SwitchItem) itemRegistry.getItem("myPresenceItem");
        myPresenceItem.setState(OnOffType.ON);
        SwitchItem myLampItem = (SwitchItem) itemRegistry.getItem("myLampItem");

        assertThat(myLampItem.getState(), is(UnDefType.NULL));
        SwitchItem myMotionItem = (SwitchItem) itemRegistry.getItem("myMotionItem");
        EventSubscriber eventHandler = new EventSubscriber() {

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
                if (e.getTopic().equals("smarthome/items/myLampItem/command")) {
                    itemEvent = e;
                }
            }
        };

        registerService(eventHandler);
        eventPublisher.post(ItemEventFactory.createStateEvent("myMotionItem", OnOffType.ON));
        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
            assertThat(((ItemCommandEvent) itemEvent).getItemCommand(), is(OnOffType.ON));
        });
    }

}