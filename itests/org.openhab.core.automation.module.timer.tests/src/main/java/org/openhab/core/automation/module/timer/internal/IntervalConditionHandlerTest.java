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
package org.openhab.core.automation.module.timer.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.internal.RuleEngineImpl;
import org.openhab.core.automation.internal.module.handler.IntervalConditionHandler;
import org.openhab.core.automation.internal.module.handler.ItemCommandActionHandler;
import org.openhab.core.automation.internal.module.handler.ItemStateTriggerHandler;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.service.ReadyMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests the Interval Condition.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class IntervalConditionHandlerTest extends BasicConditionHandlerTest {
    private final Logger logger = LoggerFactory.getLogger(IntervalConditionHandlerTest.class);

    /**
     * This checks if the condition on its own works properly.
     */
    @Test
    public void assertThatConditionWorks() throws InterruptedException {
        // The minimum interval is 10ms
        IntervalConditionHandler handler = getIntervalConditionHandler(BigDecimal.valueOf(100));
        // First execution -> should return true
        assertThat(handler.isSatisfied(Map.of()), is(true));
        // Subsequent immediate execution -> should return false
        assertThat(handler.isSatisfied(Map.of()), is(false));
        Thread.sleep(200);
        // Execute after 200ms -> should return true
        assertThat(handler.isSatisfied(Map.of()), is(true));
    }

    private IntervalConditionHandler getIntervalConditionHandler(BigDecimal minInterval) {
        return new IntervalConditionHandler(getIntervalCondition(minInterval));
    }

    private Condition getIntervalCondition(BigDecimal minInterval) {
        Configuration config = getIntervalConfiguration(minInterval);
        return ModuleBuilder.createCondition().withId("testIntervalCondition")
                .withTypeUID(IntervalConditionHandler.MODULE_TYPE_ID).withConfiguration(config).build();
    }

    private Configuration getIntervalConfiguration(BigDecimal minInterval) {
        return new Configuration(Map.of(IntervalConditionHandler.CFG_MIN_INTERVAL, minInterval));
    }

    @Override
    public Condition getPassingCondition() {
        return getIntervalCondition(BigDecimal.valueOf(100));
    }

    @Override
    public Configuration getFailingConfiguration() {
        return getIntervalConfiguration(BigDecimal.valueOf(10000));
    }

    // This is copied from BasicConditionHandlerTest with some modifications
    @Override
    @Test
    public void assertThatConditionWorksInRule() throws ItemNotFoundException, InterruptedException {
        String testItemName1 = "TriggeredItem";
        String testItemName2 = "SwitchedItem";

        /*
         * Create Rule
         */
        logger.info("Create rule");
        Configuration triggerConfig = new Configuration(Map.of("itemName", testItemName1));
        List<Trigger> triggers = List.of(ModuleBuilder.createTrigger().withId("MyTrigger")
                .withTypeUID(ItemStateTriggerHandler.UPDATE_MODULE_TYPE_ID).withConfiguration(triggerConfig).build());

        List<Condition> conditions = List.of(getPassingCondition());

        Map<String, Object> cfgEntries = new HashMap<>();
        cfgEntries.put("itemName", testItemName2);
        cfgEntries.put("command", "ON");
        Configuration actionConfig = new Configuration(cfgEntries);
        List<Action> actions = List.of(ModuleBuilder.createAction().withId("MyItemPostCommandAction")
                .withTypeUID(ItemCommandActionHandler.ITEM_COMMAND_ACTION).withConfiguration(actionConfig).build());

        // prepare the execution
        EventPublisher eventPublisher = getService(EventPublisher.class);

        // start rule engine
        RuleEngineImpl ruleEngine = Objects.requireNonNull((RuleEngineImpl) getService(RuleManager.class));
        ruleEngine.onReadyMarkerAdded(new ReadyMarker("", ""));
        waitForAssert(() -> assertTrue(ruleEngine.isStarted()));

        EventSubscriber itemEventHandler = new EventSubscriber() {

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Set.of(ItemCommandEvent.TYPE);
            }

            @Override
            public void receive(Event event) {
                logger.info("Event: {}", event.getTopic());
                if (event.getTopic().contains(testItemName2)) {
                    IntervalConditionHandlerTest.this.itemEvent = event;
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
        eventPublisher.post(ItemEventFactory.createStateUpdatedEvent(testItemName1, OnOffType.ON, null));

        // the first event is always processed
        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
            assertThat(((ItemCommandEvent) itemEvent).getItemCommand(), is(OnOffType.ON));
        });

        long minInterval = ((BigDecimal) conditions.getFirst().getConfiguration()
                .get(IntervalConditionHandler.CFG_MIN_INTERVAL)).longValue();
        Thread.sleep(minInterval + 50);

        // Send a second event to check if the condition is still satisfied
        itemEvent = null; // reset it
        eventPublisher.post(ItemEventFactory.createStateUpdatedEvent(testItemName1, OnOffType.ON, null));

        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
            assertThat(((ItemCommandEvent) itemEvent).getItemCommand(), is(OnOffType.ON));
        });
        logger.info("item state is ON");

        // now make the condition fail
        Rule rule2 = RuleBuilder.create(rule).withConditions(ModuleBuilder
                .createCondition(rule.getConditions().getFirst()).withConfiguration(getFailingConfiguration()).build())
                .build();
        ruleRegistry.update(rule2);

        // prepare the execution
        itemEvent = null;
        eventPublisher.post(ItemEventFactory.createStateUpdatedEvent(testItemName1, OnOffType.ON, null));

        // the first event is always allowed
        waitForAssert(() -> {
            assertThat(itemEvent, is(notNullValue()));
        });

        Thread.sleep(200); // some time is passing but less than the failing condition's minInterval

        // the second event is not allowed
        itemEvent = null;
        eventPublisher.post(ItemEventFactory.createStateUpdatedEvent(testItemName1, OnOffType.ON, null));
        Thread.sleep(200); // without this, the assertion will be immediately fulfilled regardless of event processing
        assertThat(itemEvent, is(nullValue()));
    }

    @SuppressWarnings("null")
    @Test
    public void checkIfModuleTypeIsRegistered() {
        ModuleTypeRegistry mtr = getService(ModuleTypeRegistry.class);
        waitForAssert(() -> {
            assertThat(mtr.get(IntervalConditionHandler.MODULE_TYPE_ID), is(notNullValue()));
        }, 3000, 100);
    }
}
