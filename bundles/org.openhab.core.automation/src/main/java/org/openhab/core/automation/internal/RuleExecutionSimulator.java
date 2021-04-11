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
package org.openhab.core.automation.internal;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleExecution;
import org.openhab.core.automation.RulePredicates;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.ConditionHandler;
import org.openhab.core.automation.handler.TimeBasedConditionHandler;
import org.openhab.core.automation.handler.TimeBasedTriggerHandler;
import org.openhab.core.automation.handler.TriggerHandler;
import org.openhab.core.scheduler.SchedulerTemporalAdjuster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulates {@link Rule rules} to show up their next execution times within the schedule.
 *
 * @author Sönke Küper - Initial contribution
 */
@NonNullByDefault
final class RuleExecutionSimulator {

    /**
     * Tag that all rules must have, do be included within the simulation.
     */
    private static final String TAG_SCHEDULE = "Schedule";

    private final Logger logger = LoggerFactory.getLogger(RuleExecutionSimulator.class);

    private final RuleRegistry ruleRegistry;
    private final RuleEngineImpl ruleEngine;

    /**
     * Constructs an new {@link RuleExecutionSimulator}.
     */
    public RuleExecutionSimulator(RuleRegistry ruleRegistry, RuleEngineImpl ruleEngine) {
        this.ruleRegistry = ruleRegistry;
        this.ruleEngine = ruleEngine;
    }

    /**
     * Simulates the execution of all rules with tag 'Schedule' for the given time interval.
     * The result is sorted ascending by execution time.
     *
     * @param from {@link ZonedDateTime} earliest time to be contained in the rule simulation.
     * @param until {@link ZonedDateTime} latest time to be contained in the rule simulation.
     * @return A {@link Stream} with all expected {@link RuleExecution}.
     */
    public Stream<RuleExecution> simulateRuleExecutions(ZonedDateTime from, ZonedDateTime until) {
        logger.debug("Simulating rules from {} until {}.", from, until);

        return ruleRegistry.stream() //
                .filter(RulePredicates.hasAllTags(TAG_SCHEDULE)) //
                .filter((Rule r) -> ruleEngine.isEnabled(r.getUID())) //
                .map((Rule r) -> simulateExecutionsForRule(r, from, until)) //
                .flatMap(List::stream).sorted();
    }

    /**
     * Simulates the next executions for the given {@link Rule} until the given {@link Date}.
     *
     * @param rule {@link Rule} to be simulated.
     * @param from {@link ZonedDateTime} earliest time to be contained in the rule simulation.
     * @param until {@link ZonedDateTime} latest time to be contained in the rule simulation.
     * @return List of expected {@link RuleExecution}.
     */
    private List<RuleExecution> simulateExecutionsForRule(Rule rule, ZonedDateTime from, ZonedDateTime until) {
        final List<RuleExecution> executions = new ArrayList<>();

        for (Trigger trigger : rule.getTriggers()) {

            TriggerHandler triggerHandler = (TriggerHandler) this.ruleEngine.getModuleHandler(trigger, rule.getUID());

            // Only triggers that are time-based will be considered within the simulation
            if (triggerHandler instanceof TimeBasedTriggerHandler) {
                SchedulerTemporalAdjuster temporalAdjuster = ((TimeBasedTriggerHandler) triggerHandler)
                        .getTemporalAdjuster();
                if (temporalAdjuster != null) {
                    executions.addAll(simulateExecutionsForCronBasedRule(rule, from, until, temporalAdjuster));
                }
            }
        }
        logger.debug("Created {} rule simulations for rule {}.", executions.size(), rule.getName());
        return executions;
    }

    /**
     * Simulates all {@link RuleExecution} for the given cron expression of the given rule.
     *
     * @param rule {@link Rule} to be simulated.
     * @param from {@link ZonedDateTime} earliest time to be contained in the rule simulation.
     * @param until {@link ZonedDateTime} latest time to be contained in the rule simulation.
     * @param cron cron-expression to be evaluated for determining the execution times.
     * @return a list of expected executions.
     */
    private List<RuleExecution> simulateExecutionsForCronBasedRule(Rule rule, ZonedDateTime from, ZonedDateTime until,
            SchedulerTemporalAdjuster temporalAdjuster) {

        final List<RuleExecution> result = new ArrayList<>();
        ZonedDateTime currentTime = ZonedDateTime.from(temporalAdjuster.adjustInto(from));

        while (!temporalAdjuster.isDone(currentTime) && currentTime.isBefore(until)) {
            // if the current time satisfies all conditions add the instance to the result
            if (checkConditions(rule, currentTime)) {
                result.add(new RuleExecution(Date.from(currentTime.toInstant()), rule));
            }
            currentTime = ZonedDateTime.from(temporalAdjuster.adjustInto(currentTime));

        }
        return result;
    }

    /**
     * Checks if all defined conditions for the given rule are satisfied.
     *
     * @param rule The rule to check
     * @param current the time to check
     * @return <code>true</code> if and only if all conditions are satisfied for the current time.
     */
    private boolean checkConditions(Rule rule, ZonedDateTime current) {
        for (Condition condition : rule.getConditions()) {
            ConditionHandler conditionHandler = (ConditionHandler) this.ruleEngine.getModuleHandler(condition,
                    rule.getUID());

            // Only conditions that are time based are checked
            if (conditionHandler instanceof TimeBasedConditionHandler
                    && !((TimeBasedConditionHandler) conditionHandler).isSatisfiedAt(current)) {
                return false;
            }
        }
        return true;
    }
}
