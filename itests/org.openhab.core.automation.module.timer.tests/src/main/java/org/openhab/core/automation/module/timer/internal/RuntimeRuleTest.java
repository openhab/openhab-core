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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.RuleStatusDetail;
import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.internal.module.handler.GenericCronTriggerHandler;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemProvider;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.test.storage.VolatileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this tests the Timer Trigger
 *
 * @author Christoph Knauf - Initial contribution
 * @author Markus Rathgeb - fix module timer test
 * @author Kai Kreuzer - migrated to Java
 */
public class RuntimeRuleTest extends JavaOSGiTest {

    private final Logger logger = LoggerFactory.getLogger(RuntimeRuleTest.class);
    private VolatileStorageService volatileStorageService = new VolatileStorageService();
    private RuleRegistry ruleRegistry;
    private RuleManager ruleEngine;

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

    @NonNullByDefault
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
        public void addProviderChangeListener(ProviderChangeListener<Item> listener) {
        }

        @Override
        public void removeProviderChangeListener(ProviderChangeListener<Item> listener) {
        }
    }

}
