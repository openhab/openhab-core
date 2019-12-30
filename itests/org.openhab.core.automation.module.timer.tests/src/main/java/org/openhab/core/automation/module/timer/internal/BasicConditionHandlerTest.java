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
package org.openhab.core.automation.module.timer.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.internal.module.handler.ItemCommandActionHandler;
import org.openhab.core.automation.internal.module.handler.ItemStateTriggerHandler;
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
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.test.storage.VolatileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This provides common functionality for all condition tests.
 *
 * @author Dominik Schlierf - Initial contribution
 * @author Kai Kreuzer - Initial contribution of TimeOfDayConditionHandlerTest
 */
public abstract class BasicConditionHandlerTest extends JavaOSGiTest {
    private final Logger logger = LoggerFactory.getLogger(BasicConditionHandlerTest.class);
    private VolatileStorageService volatileStorageService = new VolatileStorageService();
    private RuleRegistry ruleRegistry;
    private RuleManager ruleEngine;
    private Event itemEvent = null;

    /**
     * This executes before every test and before the
     *
     * @Before-annotated methods in sub-classes.
     */
    @Before
    public void beforeBase() {
        @NonNullByDefault
        ItemProvider itemProvider = new ItemProvider() {
            @Override
            public void addProviderChangeListener(ProviderChangeListener<Item> listener) {
            }

            @Override
            public Collection<Item> getAll() {
                List<Item> items = new ArrayList<>();
                items.add(new SwitchItem("TriggeredItem"));
                items.add(new SwitchItem("SwitchedItem"));
                return items;
            }

            @Override
            public void removeProviderChangeListener(ProviderChangeListener<Item> listener) {
            }

        };
        registerService(itemProvider);
        registerService(volatileStorageService);
        waitForAssert(() -> {
            ruleRegistry = getService(RuleRegistry.class);
            assertThat(ruleRegistry, is(notNullValue()));
        }, 3000, 100);
        waitForAssert(() -> {
            ruleEngine = getService(RuleManager.class);
            assertThat(ruleEngine, is(notNullValue()));
        }, 3000, 100);
    }

    @Test
    public void assertThatConditionWorksInRule() throws ItemNotFoundException {
        String testItemName1 = "TriggeredItem";
        String testItemName2 = "SwitchedItem";

        ItemRegistry itemRegistry = getService(ItemRegistry.class);
        SwitchItem triggeredItem = (SwitchItem) itemRegistry.getItem(testItemName1);
        SwitchItem switchedItem = (SwitchItem) itemRegistry.getItem(testItemName2);

        /*
         * Create Rule
         */
        logger.info("Create rule");
        Configuration triggerConfig = new Configuration(Collections.singletonMap("itemName", testItemName1));
        List<Trigger> triggers = Collections.singletonList(ModuleBuilder.createTrigger().withId("MyTrigger")
                .withTypeUID(ItemStateTriggerHandler.UPDATE_MODULE_TYPE_ID).withConfiguration(triggerConfig).build());

        List<Condition> conditions = Collections.singletonList(getPassingCondition());

        Map<String, Object> cfgEntries = new HashMap<>();
        cfgEntries.put("itemName", testItemName2);
        cfgEntries.put("command", "ON");
        Configuration actionConfig = new Configuration(cfgEntries);
        List<Action> actions = Collections.singletonList(ModuleBuilder.createAction().withId("MyItemPostCommandAction")
                .withTypeUID(ItemCommandActionHandler.ITEM_COMMAND_ACTION).withConfiguration(actionConfig).build());

        // prepare the execution
        EventPublisher eventPublisher = getService(EventPublisher.class);

        EventSubscriber itemEventHandler = new EventSubscriber() {

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public EventFilter getEventFilter() {
                return null;
            }

            @Override
            public void receive(Event event) {
                logger.info("Event: {}", event.getTopic());
                if (event.getTopic().contains(testItemName2)) {
                    BasicConditionHandlerTest.this.itemEvent = event;
                }
            }
        };
        registerService(itemEventHandler);

        Rule rule = RuleBuilder.create("MyRule" + new Random().nextInt()).withTriggers(triggers)
                .withConditions(conditions).withActions(actions).withName("MyConditionTestRule").build();
        logger.info("Rule created: {}", rule.getUID());

        logger.info("Add rule");
        ruleRegistry.add(rule);
        logger.info("Rule added");

        logger.info("Enable rule and wait for idle status");
        ruleEngine.setEnabled(rule.getUID(), true);
        waitForAssert(() -> {
            final RuleStatusInfo ruleStatus = ruleEngine.getStatusInfo(rule.getUID());
            assertThat(ruleStatus.getStatus(), is(RuleStatus.IDLE));
        });
        logger.info("Rule is enabled and idle");

        logger.info("Send and wait for item state is ON");
        eventPublisher.post(ItemEventFactory.createStateEvent(testItemName1, OnOffType.ON));

        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
            assertThat(((ItemCommandEvent) itemEvent).getItemCommand(), is(OnOffType.ON));
        });
        logger.info("item state is ON");

        // now make the condition fail
        Rule rule2 = RuleBuilder.create(rule).withConditions(ModuleBuilder.createCondition(rule.getConditions().get(0))
                .withConfiguration(getFailingConfiguration()).build()).build();
        ruleRegistry.update(rule2);

        // prepare the execution
        itemEvent = null;
        eventPublisher.post(ItemEventFactory.createStateEvent(testItemName1, OnOffType.ON));
        waitForAssert(() -> {
            assertThat(itemEvent, is(nullValue()));
        });
    }

    /**
     * This returns a Condition of the tested type, which is always evaluating as true.
     *
     * @return a Condition of the tested type, which is always evaluating as true.
     */
    protected abstract Condition getPassingCondition();

    /**
     * This returns a Configuration for the tested type of condition,
     * which makes the condition always evaluate as false.
     *
     * @return a Configuration for the tested type of condition,
     *         which makes the condition always evaluate as false.
     */
    protected abstract Configuration getFailingConfiguration();
}
