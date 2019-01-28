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
package org.eclipse.smarthome.automation.module.timer.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.RuleManager;
import org.eclipse.smarthome.automation.RuleRegistry;
import org.eclipse.smarthome.automation.RuleStatus;
import org.eclipse.smarthome.automation.RuleStatusDetail;
import org.eclipse.smarthome.automation.RuleStatusInfo;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.core.util.ModuleBuilder;
import org.eclipse.smarthome.automation.core.util.RuleBuilder;
import org.eclipse.smarthome.automation.module.timer.handler.GenericCronTriggerHandler;
import org.eclipse.smarthome.automation.type.ModuleTypeRegistry;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemProvider;
import org.eclipse.smarthome.core.items.events.ItemCommandEvent;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.eclipse.smarthome.test.storage.VolatileStorageService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this tests the Timer Trigger
 *
 * @author Christoph Knauf - initial contribution
 * @author Markus Rathgeb - fix module timer test
 * @author Kai Kreuzer - migrated to Java
 *
 */
public class RuntimeRuleTest extends JavaOSGiTest {

    final Logger logger = LoggerFactory.getLogger(RuntimeRuleTest.class);
    VolatileStorageService volatileStorageService = new VolatileStorageService();
    RuleRegistry ruleRegistry;
    RuleManager ruleEngine;

    public RuntimeRuleTest() {
    }

    @Before
    public void before() {
        ItemProvider itemProvider = new TestItemProvider(Collections.singleton(new SwitchItem("myLampItem")));
        registerService(itemProvider);
        registerService(volatileStorageService);
        waitForAssert(() -> {
            ruleRegistry = getService(RuleRegistry.class);
            assertThat("RuleRegistry service not found", ruleRegistry, is(notNullValue()));
        }, 3000, 100);
        waitForAssert(() -> {
            ruleEngine = getService(RuleManager.class);
            assertThat("RuleManager service not found", ruleEngine, is(notNullValue()));
        }, 3000, 100);
    }

    @Test
    public void checkIfTimerTriggerModuleTypeIsRegistered() {
        ModuleTypeRegistry mtr = getService(ModuleTypeRegistry.class);
        waitForAssert(() -> {
            assertThat(mtr.get(GenericCronTriggerHandler.MODULE_TYPE_ID), is(notNullValue()));
        }, 3000, 100);
    }

    @Test
    public void checkDisableAndEnableOfTimerTriggeredRule() {
        /*
         * Create Rule
         */
        logger.info("Create rule");
        String testExpression = "* * * * * ?";

        ;
        Configuration triggerConfig = new Configuration(Collections.singletonMap("cronExpression", testExpression));
        List<Trigger> triggers = Collections.singletonList(ModuleBuilder.createTrigger().withId("MyTimerTrigger")
                .withTypeUID(GenericCronTriggerHandler.MODULE_TYPE_ID).withConfiguration(triggerConfig).build());

        Rule rule = RuleBuilder.create("MyRule" + new Random().nextInt()).withTriggers(triggers)
                .withName("MyTimerTriggerTestEnableDisableRule").build();
        logger.info("Rule created: {}", rule.getUID());

        logger.info("Add rule");
        ruleRegistry.add(rule);
        logger.info("Rule added");

        int numberOfTests = 1000;
        for (int i = 0; i < numberOfTests; ++i) {
            logger.info("Disable rule");
            ruleEngine.setEnabled(rule.getUID(), false);
            waitForAssert(() -> {
                final RuleStatusInfo ruleStatus = ruleEngine.getStatusInfo(rule.getUID());
                logger.info("Rule status (should be DISABLED): {}", ruleStatus);
                assertThat(ruleStatus.getStatusDetail(), is(RuleStatusDetail.DISABLED));
            });
            logger.info("Rule is disabled");

            logger.info("Enable rule");
            ruleEngine.setEnabled(rule.getUID(), true);
            waitForAssert(() -> {
                final RuleStatusInfo ruleStatus = ruleEngine.getStatusInfo(rule.getUID());
                logger.info("Rule status (should be IDLE or RUNNING): {}", ruleStatus);
                boolean allFine;
                if (ruleStatus.getStatus().equals(RuleStatus.IDLE)
                        || ruleStatus.getStatus().equals(RuleStatus.RUNNING)) {
                    allFine = true;
                } else {
                    allFine = false;
                }
                assertThat(allFine, is(true));
            });
            logger.info("Rule is enabled");
        }
    }

    @Test
    public void assertThatTimerTriggerWorks() {
        String testItemName = "myLampItem";

        List<Event> itemEvents = new LinkedList<>();
        EventSubscriber itemEventHandler = new EventSubscriber() {
            @Override
            public void receive(Event event) {
                if (event.getTopic().contains(testItemName)) {
                    itemEvents.add(event);
                }
            };

            @Override
            public java.util.Set<String> getSubscribedEventTypes() {
                return Collections.singleton(ItemCommandEvent.TYPE);
            }

            @Override
            public EventFilter getEventFilter() {
                return null;
            };
        };
        registerService(itemEventHandler);

        /*
         * Create Rule
         */
        logger.info("Create rule");
        String testExpression = "* * * * * ?";

        Configuration triggerConfig = new Configuration(Collections.singletonMap("cronExpression", testExpression));
        List<Trigger> triggers = Collections.singletonList(ModuleBuilder.createTrigger().withId("MyTimerTrigger")
                .withTypeUID(GenericCronTriggerHandler.MODULE_TYPE_ID).withConfiguration(triggerConfig).build());

        Map<String, Object> cfgEntries = new HashMap<>();
        cfgEntries.put("itemName", testItemName);
        cfgEntries.put("command", "ON");
        Configuration actionConfig = new Configuration(cfgEntries);
        List<Action> actions = Collections.singletonList(ModuleBuilder.createAction().withId("MyItemPostCommandAction")
                .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build());

        Rule rule = RuleBuilder.create("MyRule" + new Random().nextInt()).withTriggers(triggers).withActions(actions)
                .withName("MyTimerTriggerTestRule").build();
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

        waitForAssert(() -> {
            assertThat(itemEvents.size(), is(3));
        });
    }

    class TestItemProvider implements ItemProvider {
        private final Collection<Item> items;

        TestItemProvider(Collection<Item> items) {
            this.items = items;
        }

        @Override
        public Collection<Item> getAll() {
            return items;
        }

        @Override
        public void addProviderChangeListener(@NonNull ProviderChangeListener<@NonNull Item> listener) {
        }

        @Override
        public void removeProviderChangeListener(@NonNull ProviderChangeListener<@NonNull Item> listener) {
        }
    }

}
