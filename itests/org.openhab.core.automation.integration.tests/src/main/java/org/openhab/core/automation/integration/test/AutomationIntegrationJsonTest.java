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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.ManagedRuleProvider;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RulePredicates;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.events.RuleStatusInfoEvent;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.automation.type.Output;
import org.openhab.core.automation.type.TriggerType;
import org.openhab.core.common.registry.ProviderChangeListener;
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
import org.openhab.core.test.storage.VolatileStorageService;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests the RuleEngineImpl and the import from JSON resources contained in the OH-INF folder.
 * This test must be run first otherwise imported rules will be cleared.
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Marin Mitev - make the test to pass on each run
 * @author Kai Kreuzer - refactored to Java
 */
public class AutomationIntegrationJsonTest extends JavaOSGiTest {

    private final Logger logger = LoggerFactory.getLogger(AutomationIntegrationJsonTest.class);
    private EventPublisher eventPublisher;
    private ItemRegistry itemRegistry;
    private RuleRegistry ruleRegistry;
    private RuleManager ruleManager;
    private ManagedRuleProvider managedRuleProvider;
    private ModuleTypeRegistry moduleTypeRegistry;
    private Event ruleEvent;
    public Event itemEvent;

    // keep storage rules imported from json files
    public static final VolatileStorageService VOLATILE_STORAGE_SERVICE = new VolatileStorageService();

    @Before
    public void before() {
        logger.info("@Before.begin");

        getService(ItemRegistry.class);

        @NonNullByDefault
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
                items.add(new SwitchItem("myMotionItem11"));
                items.add(new SwitchItem("myLampItem11"));
                items.add(new SwitchItem("myMotionItem3"));
                items.add(new SwitchItem("templ_MotionItem"));
                items.add(new SwitchItem("templ_LampItem"));

                return items;
            }

            @Override
            public void removeProviderChangeListener(ProviderChangeListener<Item> listener) {
            }
        };

        registerService(itemProvider);
        registerVolatileStorageService();

        @NonNullByDefault
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
                logger.info("RuleEvent: {} --> {}", e.getTopic(), e.getPayload());
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
            Optional<Rule> rule2 = ruleRegistry.stream().filter(
                    RulePredicates.hasAnyOfTags("jsonTest").and(RulePredicates.hasAnyOfTags("references").negate()))
                    .findFirst();
            assertThat(rule2.isPresent(), is(true));
            RuleStatusInfo ruleStatus2 = ruleManager.getStatusInfo(rule2.get().getUID());
            assertThat(ruleStatus2.getStatus(), is(RuleStatus.IDLE));
        }, 10000, 200);

        Optional<Rule> optionalRule = ruleRegistry.stream()
                .filter(RulePredicates.hasAnyOfTags("jsonTest").and(RulePredicates.hasAnyOfTags("references").negate()))
                .findFirst();
        assertThat(optionalRule.isPresent(), is(true));
        Rule rule = optionalRule.get();
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
            Optional<Rule> rule2 = ruleRegistry.stream().filter(RulePredicates.hasAllTags("jsonTest", "references"))
                    .findFirst();
            assertThat(rule2.isPresent(), is(true));
            RuleStatusInfo ruleStatus2 = ruleManager.getStatusInfo(rule2.get().getUID());
            assertThat(ruleStatus2.getStatus(), is(RuleStatus.IDLE));
        }, 10000, 200);
        Optional<Rule> optionalRule = ruleRegistry.stream().filter(RulePredicates.hasAllTags("jsonTest", "references"))
                .findFirst();
        assertThat(optionalRule.isPresent(), is(true));
        Rule rule = optionalRule.get();
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
        Optional<? extends Action> action = rule.getActions().stream()
                .filter(a -> a.getId().equals("ItemPostCommandActionID")).findFirst();
        assertThat(action.isPresent(), is(true));
        assertThat(action.get().getTypeUID(), is("core.ItemCommandAction"));
        assertThat(action.get().getConfiguration().get("command"), is("ON"));
        RuleStatusInfo ruleStatus = ruleManager.getStatusInfo(rule.getUID());
        assertThat(ruleStatus.getStatus(), is(RuleStatus.IDLE));

        // run the rule to check if the runtime rule has resolved module references and is executed successfully
        EventPublisher eventPublisher = getService(EventPublisher.class);

        @NonNullByDefault
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
            assertThat(ruleManager.getStatusInfo(r.getUID()), is(notNullValue()));
            assertThat(ruleManager.getStatusInfo(r.getUID()).getStatus(), is(RuleStatus.IDLE));
        }, 9000, 200);

        SwitchItem myPresenceItem = (SwitchItem) itemRegistry.getItem("myPresenceItem");
        myPresenceItem.setState(OnOffType.ON);
        SwitchItem myLampItem = (SwitchItem) itemRegistry.getItem("myLampItem");

        assertThat(myLampItem.getState(), is(UnDefType.NULL));
        SwitchItem myMotionItem = (SwitchItem) itemRegistry.getItem("myMotionItem");

        @NonNullByDefault
        EventSubscriber eventHandler = new EventSubscriber() {

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
