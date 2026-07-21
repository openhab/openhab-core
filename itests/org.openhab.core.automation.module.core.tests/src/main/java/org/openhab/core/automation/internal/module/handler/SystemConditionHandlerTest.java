/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.module.handler;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.internal.RuleEngineImpl;
import org.openhab.core.automation.internal.module.factory.CoreModuleHandlerFactory;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemProvider;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.StartLevelService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.test.storage.VolatileStorageService;
import org.openhab.core.thing.ThingRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests the system conditions.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class SystemConditionHandlerTest extends JavaOSGiTest {
    private final Logger logger = LoggerFactory.getLogger(SystemConditionHandlerTest.class);
    private VolatileStorageService volatileStorageService = new VolatileStorageService();
    private @NonNullByDefault({}) RuleRegistry ruleRegistry;
    private @NonNullByDefault({}) RuleManager ruleEngine;
    private @Nullable Event itemEvent;
    private @NonNullByDefault({}) StartLevelService startLevelService;

    @BeforeEach
    public void setUp() throws IOException {
        startLevelService = mock(StartLevelService.class);
        when(startLevelService.getStartLevel()).thenReturn(100);
        registerService(startLevelService, StartLevelService.class.getName());
        EventPublisher eventPublisher = Objects.requireNonNull(getService(EventPublisher.class));
        ThingRegistry thingRegistry = Objects.requireNonNull(getService(ThingRegistry.class));
        ItemRegistry itemRegistry = Objects.requireNonNull(getService(ItemRegistry.class));
        CoreModuleHandlerFactory coreModuleHandlerFactory = new CoreModuleHandlerFactory(getBundleContext(),
                eventPublisher, thingRegistry, itemRegistry, mock(TimeZoneProvider.class), startLevelService);
        registerService(coreModuleHandlerFactory);

        ItemProvider itemProvider = new ItemProvider() {
            @Override
            public void addProviderChangeListener(ProviderChangeListener<Item> listener) {
            }

            @Override
            public Collection<Item> getAll() {
                return List.of(new SwitchItem("TriggeredItem"), new SwitchItem("SwitchedItem"));
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

        RuleEngineImpl ruleEngine = Objects.requireNonNull((RuleEngineImpl) getService(RuleManager.class));
        ruleEngine.onReadyMarkerAdded(new ReadyMarker("", ""));
        waitForAssert(() -> assertTrue(ruleEngine.isStarted()));
    }

    @Test
    public void assertThatConditionWorks() throws InterruptedException {
        SystemConditionHandler handler = getSystemConditionHandler(BigDecimal.valueOf(40));
        when(startLevelService.getStartLevel()).thenReturn(20);
        assertThat(handler.isSatisfied(Map.of()), is(false));
        when(startLevelService.getStartLevel()).thenReturn(39);
        assertThat(handler.isSatisfied(Map.of()), is(false));
        when(startLevelService.getStartLevel()).thenReturn(40);
        assertThat(handler.isSatisfied(Map.of()), is(true));
    }

    @Test
    public void assertThatConditionWorksInRule() throws Exception {
        String testItemName1 = "TriggeredItem";
        String testItemName2 = "SwitchedItem";

        // Create Rule
        logger.info("Create rule");
        Configuration triggerConfig = new Configuration(Map.of("itemName", testItemName1));
        List<Trigger> triggers = List.of(ModuleBuilder.createTrigger().withId("MyTrigger")
                .withTypeUID(ItemStateTriggerHandler.UPDATE_MODULE_TYPE_ID).withConfiguration(triggerConfig).build());

        List<Condition> conditions = List.of(getStartlevelCondition(BigDecimal.valueOf(60)));

        Map<String, Object> cfgEntries = new HashMap<>();
        cfgEntries.put("itemName", testItemName2);
        cfgEntries.put("command", "ON");
        Configuration actionConfig = new Configuration(cfgEntries);
        List<Action> actions = List.of(ModuleBuilder.createAction().withId("MyItemPostCommandAction")
                .withTypeUID(ItemCommandActionHandler.ITEM_COMMAND_ACTION).withConfiguration(actionConfig).build());

        // Prepare the execution
        EventPublisher eventPublisher = getService(EventPublisher.class);
        assertNotNull(eventPublisher);
        when(startLevelService.getStartLevel()).thenReturn(80);

        EventSubscriber itemEventHandler = new EventSubscriber() {

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Set.of(ItemCommandEvent.TYPE);
            }

            @Override
            public void receive(Event event) {
                logger.info("Event: {}", event.getTopic());
                if (event.getTopic().contains(testItemName2)) {
                    SystemConditionHandlerTest.this.itemEvent = event;
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
            RuleStatusInfo ruleStatus = ruleEngine.getStatusInfo(rule.getUID());
            if (ruleStatus == null) {
                fail("Failed to get rule status");
            } else {
                assertThat(ruleStatus.getStatus(), is(RuleStatus.IDLE));
            }
        });
        logger.info("Rule is enabled and idle");

        logger.info("Send and wait for item state is ON");
        eventPublisher.post(ItemEventFactory.createStateUpdatedEvent(testItemName1, OnOffType.ON, null));

        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
            if (itemEvent instanceof ItemCommandEvent event) {
                assertThat(event.getItemCommand(), is(OnOffType.ON));
            } else {
                fail("itemEvent is " + itemEvent);
            }
        });

        when(startLevelService.getStartLevel()).thenReturn(60);
        // Send another event to check if the condition is still satisfied
        itemEvent = null; // reset it
        eventPublisher.post(ItemEventFactory.createStateUpdatedEvent(testItemName1, OnOffType.ON, null));

        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
            if (itemEvent instanceof ItemCommandEvent event) {
                assertThat(event.getItemCommand(), is(OnOffType.ON));
            } else {
                fail("itemEvent is " + itemEvent);
            }
        });
        logger.info("item state is ON");

        // Make the condition fail
        Rule rule2 = RuleBuilder.create(rule)
                .withConditions(ModuleBuilder.createCondition(rule.getConditions().getFirst())
                        .withConfiguration(getStartlevelConfiguration(BigDecimal.valueOf(100))).build())
                .build();
        ruleRegistry.update(rule2);

        // Prepare the execution
        itemEvent = null;
        eventPublisher.post(ItemEventFactory.createStateUpdatedEvent(testItemName1, OnOffType.ON, null));

        // Give it some time to act
        Thread.sleep(300L);
        assertThat(itemEvent, is(nullValue()));
    }

    @Test
    public void checkIfModuleTypeIsRegistered() {
        ModuleTypeRegistry mtr = getService(ModuleTypeRegistry.class);
        if (mtr != null) {
            waitForAssert(() -> {
                assertThat(mtr.get(SystemConditionHandler.STARTLEVEL_MODULE_TYPE_ID), is(notNullValue()));
            }, 3000, 100);
        } else {
            fail("Failed to get ModuleTypeRegistry instance");
        }
    }

    private SystemConditionHandler getSystemConditionHandler(BigDecimal minStartlevel) {
        return new SystemConditionHandler(getStartlevelCondition(minStartlevel), startLevelService);
    }

    private Condition getStartlevelCondition(BigDecimal minStartlevel) {
        Configuration config = getStartlevelConfiguration(minStartlevel);
        return ModuleBuilder.createCondition().withId("testStartlevelCondition")
                .withTypeUID(SystemConditionHandler.STARTLEVEL_MODULE_TYPE_ID).withConfiguration(config).build();
    }

    private Configuration getStartlevelConfiguration(BigDecimal minStartlevel) {
        return new Configuration(Map.of(SystemConditionHandler.CFG_MIN_STARTLEVEL, minStartlevel));
    }
}
