/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleExecution;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleProvider;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.internal.RuleEngineImpl;
import org.openhab.core.automation.internal.module.handler.DayOfWeekConditionHandler;
import org.openhab.core.automation.internal.module.handler.EphemerisConditionHandler;
import org.openhab.core.automation.internal.module.handler.GenericCronTriggerHandler;
import org.openhab.core.automation.internal.module.handler.TimeOfDayConditionHandler;
import org.openhab.core.automation.internal.module.handler.TimeOfDayTriggerHandler;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.storage.StorageService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the simulation of rule executions with the {@link org.openhab.core.automation.internal.RuleExecutionSimulator}
 *
 * @author Sönke Küper - Initial contribution
 */
@NonNullByDefault
public class RuleSimulationTest extends JavaOSGiTest {

    private final Logger logger = LoggerFactory.getLogger(RuleSimulationTest.class);

    private @Nullable RuleRegistry ruleRegistry;
    private @Nullable RuleManager ruleEngine;

    @BeforeEach
    public void before() {
        registerVolatileStorageService();

        StorageService storageService = getService(StorageService.class);

        ruleRegistry = getService(RuleRegistry.class);
        ruleEngine = getService(RuleManager.class);
        waitForAssert(() -> {
            assertThat(storageService, is(notNullValue()));
            assertThat(ruleRegistry, is(notNullValue()));
            assertThat(ruleEngine, is(notNullValue()));
        }, 9000, 1000);

        // start rule engine
        ((RuleEngineImpl) ruleEngine).onReadyMarkerAdded(new ReadyMarker("", ""));

        waitForAssert(() -> {
            assertThat(((RuleEngineImpl) ruleEngine).isStarted(), is(true));
        }, 5000, 1000);
    }

    @Override
    protected void registerVolatileStorageService() {
        registerService(AutomationIntegrationJsonTest.VOLATILE_STORAGE_SERVICE);
    }

    @Test
    public void testSimulateRuleWithCronTrigger() {
        logger.info("assert that rules are simulated correct");

        final Rule cronRuleWithTimeOfDayCondition = createRuleWithCronExpressionTrigger();
        final Rule disabledRule = createRuleWithCronExpressionTrigger();
        final Rule timeOfDayTriggerWithDayOfWeekCondition = createRuleWithTimeOfDayTrigger();
        final Rule timeOfDayTriggerWithEphemerisCondition = createRuleWithEphemerisCondition();

        RuleProvider ruleProvider = new RuleProvider() {
            @Override
            public void addProviderChangeListener(ProviderChangeListener<Rule> listener) {
            }

            @Override
            public Collection<Rule> getAll() {
                return Set.of(cronRuleWithTimeOfDayCondition, timeOfDayTriggerWithDayOfWeekCondition,
                        timeOfDayTriggerWithEphemerisCondition, disabledRule);
            }

            @Override
            public void removeProviderChangeListener(ProviderChangeListener<Rule> listener) {
            }
        };

        // Register the RuleProvider, so that the rules are registered within the ruleRegistry.
        registerService(ruleProvider);
        assertThat(ruleRegistry.get(cronRuleWithTimeOfDayCondition.getUID()), is(notNullValue()));
        assertThat(ruleRegistry.get(disabledRule.getUID()), is(notNullValue()));
        assertThat(ruleRegistry.get(timeOfDayTriggerWithDayOfWeekCondition.getUID()), is(notNullValue()));
        assertThat(ruleRegistry.get(timeOfDayTriggerWithEphemerisCondition.getUID()), is(notNullValue()));

        // Disable one rule, so it must not be contained within the simulation
        ruleEngine.setEnabled(disabledRule.getUID(), false);
        assertFalse(ruleEngine.isEnabled(disabledRule.getUID()));

        // Simulate for two weeks
        final ZonedDateTime from = ZonedDateTime.of(2021, 1, 4, 0, 0, 0, 0, ZoneId.systemDefault());
        final ZonedDateTime until = ZonedDateTime.of(2021, 1, 17, 23, 59, 59, 0, ZoneId.systemDefault());

        List<RuleExecution> executions = ruleEngine.simulateRuleExecutions(from, until).collect(Collectors.toList());

        // Every rule fires twice a week. We simulate for two weeks so we expect 12 executions
        // TODO: must be 12, but Ephemeris Condition is not yet evaluated in test, because dayset is not configured.
        assertEquals(8, executions.size());

        Iterator<RuleExecution> it = executions.iterator();

        // due the result is sorted, check the results by position.
        // First week
        checkExecution(it.next(), timeOfDayTriggerWithDayOfWeekCondition, 4, 16, 00);
        checkExecution(it.next(), cronRuleWithTimeOfDayCondition, 6, 10, 30);
        checkExecution(it.next(), timeOfDayTriggerWithDayOfWeekCondition, 6, 16, 00);
        checkExecution(it.next(), cronRuleWithTimeOfDayCondition, 8, 10, 30);
        // checkExecution(it.next(), timeOfDayTriggerWithEphemerisCondition, 9, 10, 00);
        // checkExecution(it.next(), timeOfDayTriggerWithEphemerisCondition, 10, 10, 00);

        // Second week
        checkExecution(it.next(), timeOfDayTriggerWithDayOfWeekCondition, 11, 16, 00);
        checkExecution(it.next(), cronRuleWithTimeOfDayCondition, 13, 10, 30);
        checkExecution(it.next(), timeOfDayTriggerWithDayOfWeekCondition, 13, 16, 00);
        checkExecution(it.next(), cronRuleWithTimeOfDayCondition, 15, 10, 30);
        // checkExecution(it.next(), timeOfDayTriggerWithEphemerisCondition, 16, 10, 00);
        // checkExecution(it.next(), timeOfDayTriggerWithEphemerisCondition, 17, 10, 00);
        assertFalse(it.hasNext());
    }

    private static void checkExecution(RuleExecution ruleExecution, Rule expectedRule, int day, int hour, int minute) {
        assertThat(ruleExecution.getRule().getUID(), is(expectedRule.getUID()));
        Date expectedDate = createExpectedDate(day, hour, minute);
        assertThat(ruleExecution.getDate(), is(expectedDate));
    }

    private static Date createExpectedDate(int day, int hour, int minute) {
        return new GregorianCalendar(2021, Calendar.JANUARY, day, hour, minute, 0).getTime();
    }

    /**
     * creates a rule with an cron expression trigger, that fires Wednesday and Friday at 10:30. Has one
     * condition that it should only run between 8:00 and 11:00.
     */
    private static Rule createRuleWithCronExpressionTrigger() {
        int rand = new Random().nextInt();

        Map<String, Object> configs = new HashMap<>();
        configs.put(GenericCronTriggerHandler.CFG_CRON_EXPRESSION, "0 30 10-11 ? * WED,FRI");
        final Configuration triggerConfig = new Configuration(configs);
        final String triggerUID = "CronTrigger_" + rand;
        List<Trigger> triggers = List.of(ModuleBuilder.createTrigger().withId(triggerUID)
                .withTypeUID(GenericCronTriggerHandler.MODULE_TYPE_ID).withConfiguration(triggerConfig).build());

        final Configuration actionConfig = new Configuration();
        List<Action> actions = List.of(ModuleBuilder.createAction().withId("ItemPostCommandAction_" + rand)
                .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build());

        configs = new HashMap<>();
        configs.put(TimeOfDayConditionHandler.CFG_START_TIME, "08:00");
        configs.put(TimeOfDayConditionHandler.CFG_END_TIME, "11:00");
        final Configuration conditionConfig = new Configuration(configs);
        List<Condition> conditions = List.of(ModuleBuilder.createCondition().withId("DayCondition" + rand)
                .withTypeUID(TimeOfDayConditionHandler.MODULE_TYPE_ID).withConfiguration(conditionConfig).build());

        return RuleBuilder.create("cronRule_" + rand).withTriggers(triggers).withActions(actions)
                .withConditions(conditions).withName("CronRule").withTags("Schedule").build();
    }

    /**
     * creates a rule with an day of time trigger at 16:00, that has an side condition to fire only
     * on Monday and Wednesday.
     */
    private static Rule createRuleWithTimeOfDayTrigger() {
        int rand = new Random().nextInt();

        Map<String, Object> configs = new HashMap<>();
        configs.put(TimeOfDayTriggerHandler.CFG_TIME, "16:00");
        final Configuration triggerConfig = new Configuration(configs);
        final String triggerUID = "TimeOfDayTrigger_" + rand;
        List<Trigger> triggers = List.of(ModuleBuilder.createTrigger().withId(triggerUID)
                .withTypeUID(TimeOfDayTriggerHandler.MODULE_TYPE_ID).withConfiguration(triggerConfig).build());

        final Configuration actionConfig = new Configuration();
        List<Action> actions = List.of(ModuleBuilder.createAction().withId("ItemPostCommandAction_" + rand)
                .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build());

        configs = new HashMap<>();
        configs.put(DayOfWeekConditionHandler.CFG_DAYS, Arrays.asList("MON", "WED"));
        final Configuration conditionConfig = new Configuration(configs);
        List<Condition> conditions = List.of(ModuleBuilder.createCondition().withId("DayCondition" + rand)
                .withTypeUID(DayOfWeekConditionHandler.MODULE_TYPE_ID).withConfiguration(conditionConfig).build());

        return RuleBuilder.create("timeOfdayRule_" + rand).withTriggers(triggers).withActions(actions)
                .withConditions(conditions).withName("TimeOfDayRule").withTags("Schedule").build();
    }

    /**
     * creates a rule with an day of time trigger at 10:00, that has an side condition to fire only
     * on the weekend.
     */
    private static Rule createRuleWithEphemerisCondition() {
        int rand = new Random().nextInt();

        Map<String, Object> configs = new HashMap<>();
        configs.put(TimeOfDayTriggerHandler.CFG_TIME, "10:00");
        final Configuration triggerConfig = new Configuration(configs);
        final String triggerUID = "TimeOfDayTrigger_" + rand;
        List<Trigger> triggers = List.of(ModuleBuilder.createTrigger().withId(triggerUID)
                .withTypeUID(TimeOfDayTriggerHandler.MODULE_TYPE_ID).withConfiguration(triggerConfig).build());

        final Configuration actionConfig = new Configuration();
        List<Action> actions = List.of(ModuleBuilder.createAction().withId("ItemPostCommandAction_" + rand)
                .withTypeUID("core.ItemCommandAction").withConfiguration(actionConfig).build());

        final Configuration conditionConfig = new Configuration();
        List<Condition> conditions = List.of(ModuleBuilder.createCondition().withId("DayCondition" + rand)
                .withTypeUID(EphemerisConditionHandler.WEEKEND_MODULE_TYPE_ID).withConfiguration(conditionConfig)
                .build());

        return RuleBuilder.create("weekdayRule_" + rand).withTriggers(triggers).withActions(actions)
                .withConditions(conditions).withName("WeekdaysRule").withTags("Schedule").build();
    }
}
