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
package org.openhab.core.model.rule.runtime.internal.engine;

import static org.openhab.core.model.rule.runtime.internal.engine.RuleTriggerManager.TriggerTypes.*;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.openhab.core.items.Item;
import org.openhab.core.model.rule.rules.ChangedEventTrigger;
import org.openhab.core.model.rule.rules.CommandEventTrigger;
import org.openhab.core.model.rule.rules.EventEmittedTrigger;
import org.openhab.core.model.rule.rules.EventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberChangedEventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberCommandEventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberUpdateEventTrigger;
import org.openhab.core.model.rule.rules.Rule;
import org.openhab.core.model.rule.rules.RuleModel;
import org.openhab.core.model.rule.rules.SystemOnShutdownTrigger;
import org.openhab.core.model.rule.rules.SystemOnStartupTrigger;
import org.openhab.core.model.rule.rules.ThingStateChangedEventTrigger;
import org.openhab.core.model.rule.rules.ThingStateUpdateEventTrigger;
import org.openhab.core.model.rule.rules.TimerTrigger;
import org.openhab.core.model.rule.rules.UpdateEventTrigger;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.TypeParser;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

/**
 * This is a helper class which deals with everything about rule triggers.
 * It keeps lists of which rule must be executed for which trigger and takes
 * over the evaluation of states and trigger conditions for the rule engine.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class RuleTriggerManager {

    private final Logger logger = LoggerFactory.getLogger(RuleTriggerManager.class);

    public enum TriggerTypes {
        UPDATE, // fires whenever a status update is received for an item
        CHANGE, // same as UPDATE, but only fires if the current item state is changed by the update
        COMMAND, // fires whenever a command is received for an item
        TRIGGER, // fires whenever a trigger is emitted on a channel
        STARTUP, // fires when the rule engine bundle starts and once as soon as all required items are available
        SHUTDOWN, // fires when the rule engine bundle is stopped
        TIMER, // fires at a given time
        THINGUPDATE, // fires whenever the thing state is updated.
        THINGCHANGE, // fires if the thing state is changed by the update
    }

    // Group name prefix for maps
    private static final String GROUP_NAME_PREFIX = "*GROUP*";

    // lookup maps for different triggering conditions
    private final Map<String, Set<Rule>> updateEventTriggeredRules = new HashMap<>();
    private final Map<String, Set<Rule>> changedEventTriggeredRules = new HashMap<>();
    private final Map<String, Set<Rule>> commandEventTriggeredRules = new HashMap<>();
    private final Map<String, Set<Rule>> thingUpdateEventTriggeredRules = new HashMap<>();
    private final Map<String, Set<Rule>> thingChangedEventTriggeredRules = new HashMap<>();
    // Maps from channelName -> Rules
    private final Map<String, Set<Rule>> triggerEventTriggeredRules = new HashMap<>();
    private final Set<Rule> systemStartupTriggeredRules = new CopyOnWriteArraySet<>();
    private final Set<Rule> systemShutdownTriggeredRules = new CopyOnWriteArraySet<>();
    private final Set<Rule> timerEventTriggeredRules = new CopyOnWriteArraySet<>();

    // the scheduler used for timer events
    private Scheduler scheduler;

    public RuleTriggerManager(Injector injector) {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.setJobFactory(injector.getInstance(GuiceAwareJobFactory.class));

            // we want to defer timer rule execution until after the startup rules have been executed.
            scheduler.standby();
        } catch (SchedulerException e) {
            logger.error("initializing scheduler throws exception", e);
        }
    }

    /**
     * Returns all rules which have a trigger of a given type
     *
     * @param type the trigger type of the rules to return
     * @return rules with triggers of the given type
     */
    public Iterable<Rule> getRules(TriggerTypes type) {
        Iterable<Rule> result;
        switch (type) {
            case STARTUP:
                result = systemStartupTriggeredRules;
                break;
            case SHUTDOWN:
                result = systemShutdownTriggeredRules;
                break;
            case TIMER:
                result = timerEventTriggeredRules;
                break;
            case UPDATE:
                result = updateEventTriggeredRules.values().stream().flatMap(Collection::stream)
                        .collect(Collectors.toList());
                break;
            case CHANGE:
                result = changedEventTriggeredRules.values().stream().flatMap(Collection::stream)
                        .collect(Collectors.toList());
                break;
            case COMMAND:
                result = commandEventTriggeredRules.values().stream().flatMap(Collection::stream)
                        .collect(Collectors.toList());
                break;
            case TRIGGER:
                result = triggerEventTriggeredRules.values().stream().flatMap(Collection::stream)
                        .collect(Collectors.toList());
                break;
            case THINGUPDATE:
                result = thingUpdateEventTriggeredRules.values().stream().flatMap(Collection::stream)
                        .collect(Collectors.toList());
                break;
            case THINGCHANGE:
                result = thingChangedEventTriggeredRules.values().stream().flatMap(Collection::stream)
                        .collect(Collectors.toList());
                break;
            default:
                result = new HashSet<>();
        }
        List<Rule> filteredList = new ArrayList<>();
        for (Rule rule : result) {
            // we really only want to return rules that are still loaded
            if (rule.eResource() != null && !rule.eIsProxy()) {
                filteredList.add(rule);
            }
        }

        return filteredList;
    }

    /**
     * Returns all rules for which the trigger condition is true for the given type, item and state.
     *
     * @param triggerType
     * @param item
     * @param state
     * @return all rules for which the trigger condition is true
     */
    public Iterable<Rule> getRules(TriggerTypes triggerType, Item item, State state) {
        return internalGetRules(triggerType, item, null, state);
    }

    /**
     * Returns all rules for which the trigger condition is true for the given type, item and states.
     *
     * @param triggerType
     * @param item
     * @param oldState
     * @param newState
     * @return all rules for which the trigger condition is true
     */
    public Iterable<Rule> getRules(TriggerTypes triggerType, Item item, State oldState, State newState) {
        return internalGetRules(triggerType, item, oldState, newState);
    }

    /**
     * Returns all rules for which the trigger condition is true for the given type, item and command.
     *
     * @param triggerType
     * @param item
     * @param command
     * @return all rules for which the trigger condition is true
     */
    public Iterable<Rule> getRules(TriggerTypes triggerType, Item item, Command command) {
        return internalGetRules(triggerType, item, null, command);
    }

    /**
     * Returns all rules for which the trigger condition is true for the given type and channel.
     *
     * @param triggerType
     * @param channel
     * @return all rules for which the trigger condition is true
     */
    public Iterable<Rule> getRules(TriggerTypes triggerType, String channel, String event) {
        List<Rule> result = new ArrayList<>();

        switch (triggerType) {
            case TRIGGER:
                Set<Rule> rules = triggerEventTriggeredRules.get(channel);
                if (rules == null) {
                    return Collections.emptyList();
                }
                for (Rule rule : rules) {
                    for (EventTrigger t : rule.getEventtrigger()) {
                        if (t instanceof EventEmittedTrigger) {
                            EventEmittedTrigger et = (EventEmittedTrigger) t;

                            if (et.getChannel().equals(channel)
                                    && (et.getTrigger() == null || et.getTrigger().getValue().equals(event))) {
                                // if the rule does not have a specific event , execute it on any event
                                result.add(rule);
                            }
                        }
                    }
                }
                break;
            default:
                return Collections.emptyList();
        }

        return result;
    }

    public Iterable<Rule> getRules(TriggerTypes triggerType, String thingUid, ThingStatus state) {
        return internalGetThingRules(triggerType, thingUid, null, state);
    }

    public Iterable<Rule> getRules(TriggerTypes triggerType, String thingUid, ThingStatus oldState,
            ThingStatus newState) {
        return internalGetThingRules(triggerType, thingUid, oldState, newState);
    }

    private Iterable<Rule> getAllRules(TriggerTypes type, String name) {
        Iterable<Rule> rules = null;
        switch (type) {
            case STARTUP:
                rules = systemStartupTriggeredRules;
                break;
            case SHUTDOWN:
                rules = systemShutdownTriggeredRules;
                break;
            case UPDATE:
                rules = updateEventTriggeredRules.get(name);
                break;
            case CHANGE:
                rules = changedEventTriggeredRules.get(name);
                break;
            case COMMAND:
                rules = commandEventTriggeredRules.get(name);
                break;
            case THINGUPDATE:
                rules = thingUpdateEventTriggeredRules.get(name);
                break;
            case THINGCHANGE:
                rules = thingChangedEventTriggeredRules.get(name);
                break;
            default:
                break;
        }
        if (rules == null) {
            rules = Collections.emptySet();
        }
        return rules;
    }

    private void internalGetUpdateRules(String name, Boolean isGroup, List<Class<? extends State>> acceptedDataTypes,
            State state, List<Rule> result) {
        final String mapName = (isGroup) ? GROUP_NAME_PREFIX + name : name;
        for (Rule rule : getAllRules(UPDATE, mapName)) {
            for (EventTrigger t : rule.getEventtrigger()) {
                String triggerStateString = null;
                if ((!isGroup) && (t instanceof UpdateEventTrigger)) {
                    final UpdateEventTrigger ut = (UpdateEventTrigger) t;
                    if (ut.getItem().equals(name)) {
                        triggerStateString = ut.getState() != null ? ut.getState().getValue() : null;
                    } else {
                        continue;
                    }
                } else if ((isGroup) && (t instanceof GroupMemberUpdateEventTrigger)) {
                    final GroupMemberUpdateEventTrigger gmut = (GroupMemberUpdateEventTrigger) t;
                    if (gmut.getGroup().equals(name)) {
                        triggerStateString = gmut.getState() != null ? gmut.getState().getValue() : null;
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
                if (triggerStateString != null) {
                    final State triggerState = TypeParser.parseState(acceptedDataTypes, triggerStateString);
                    if (!state.equals(triggerState)) {
                        continue;
                    }
                }
                result.add(rule);
            }
        }
    }

    private void internalGetChangeRules(String name, Boolean isGroup, List<Class<? extends State>> acceptedDataTypes,
            State newState, State oldState, List<Rule> result) {
        final String mapName = (isGroup) ? GROUP_NAME_PREFIX + name : name;
        for (Rule rule : getAllRules(CHANGE, mapName)) {
            for (EventTrigger t : rule.getEventtrigger()) {
                String triggerOldStateString = null;
                String triggerNewStateString = null;
                if ((!isGroup) && (t instanceof ChangedEventTrigger)) {
                    final ChangedEventTrigger ct = (ChangedEventTrigger) t;
                    if (ct.getItem().equals(name)) {
                        triggerOldStateString = ct.getOldState() != null ? ct.getOldState().getValue() : null;
                        triggerNewStateString = ct.getNewState() != null ? ct.getNewState().getValue() : null;
                    } else {
                        continue;
                    }
                } else if ((isGroup) && (t instanceof GroupMemberChangedEventTrigger)) {
                    final GroupMemberChangedEventTrigger gmct = (GroupMemberChangedEventTrigger) t;
                    if (gmct.getGroup().equals(name)) {
                        triggerOldStateString = gmct.getOldState() != null ? gmct.getOldState().getValue() : null;
                        triggerNewStateString = gmct.getNewState() != null ? gmct.getNewState().getValue() : null;
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
                if (triggerOldStateString != null) {
                    final State triggerOldState = TypeParser.parseState(acceptedDataTypes, triggerOldStateString);
                    if (!oldState.equals(triggerOldState)) {
                        continue;
                    }
                }
                if (triggerNewStateString != null) {
                    final State triggerNewState = TypeParser.parseState(acceptedDataTypes, triggerNewStateString);
                    if (!newState.equals(triggerNewState)) {
                        continue;
                    }
                }
                result.add(rule);
            }
        }
    }

    private void internalGetCommandRules(String name, Boolean isGroup,
            List<Class<? extends Command>> acceptedCommandTypes, Command command, List<Rule> result) {
        final String mapName = (isGroup) ? GROUP_NAME_PREFIX + name : name;
        for (Rule rule : getAllRules(COMMAND, mapName)) {
            for (final EventTrigger t : rule.getEventtrigger()) {
                String triggerCommandString = null;
                if ((!isGroup) && (t instanceof CommandEventTrigger)) {
                    final CommandEventTrigger ct = (CommandEventTrigger) t;
                    if (ct.getItem().equals(name)) {
                        triggerCommandString = ct.getCommand() != null ? ct.getCommand().getValue() : null;
                    } else {
                        continue;
                    }
                } else if ((isGroup) && (t instanceof GroupMemberCommandEventTrigger)) {
                    final GroupMemberCommandEventTrigger gmct = (GroupMemberCommandEventTrigger) t;
                    if (gmct.getGroup().equals(name)) {
                        triggerCommandString = gmct.getCommand() != null ? gmct.getCommand().getValue() : null;
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
                if (triggerCommandString != null) {
                    final Command triggerCommand = TypeParser.parseCommand(acceptedCommandTypes, triggerCommandString);
                    if (!command.equals(triggerCommand)) {
                        continue;
                    }
                }
                result.add(rule);
            }
        }
    }

    private Iterable<Rule> internalGetRules(TriggerTypes triggerType, Item item, Type oldType, Type newType) {
        List<Rule> result = new ArrayList<>();
        switch (triggerType) {
            case STARTUP:
                return systemStartupTriggeredRules;
            case SHUTDOWN:
                return systemShutdownTriggeredRules;
            case TIMER:
                return timerEventTriggeredRules;
            case UPDATE:
                if (newType instanceof State) {
                    List<Class<? extends State>> acceptedDataTypes = item.getAcceptedDataTypes();
                    final State state = (State) newType;
                    internalGetUpdateRules(item.getName(), false, acceptedDataTypes, state, result);
                    for (String groupName : item.getGroupNames()) {
                        internalGetUpdateRules(groupName, true, acceptedDataTypes, state, result);
                    }
                }
                break;
            case CHANGE:
                if (newType instanceof State && oldType instanceof State) {
                    List<Class<? extends State>> acceptedDataTypes = item.getAcceptedDataTypes();
                    final State newState = (State) newType;
                    final State oldState = (State) oldType;
                    internalGetChangeRules(item.getName(), false, acceptedDataTypes, newState, oldState, result);
                    for (String groupName : item.getGroupNames()) {
                        internalGetChangeRules(groupName, true, acceptedDataTypes, newState, oldState, result);
                    }
                }
                break;
            case COMMAND:
                if (newType instanceof Command) {
                    List<Class<? extends Command>> acceptedCommandTypes = item.getAcceptedCommandTypes();
                    final Command command = (Command) newType;
                    internalGetCommandRules(item.getName(), false, acceptedCommandTypes, command, result);
                    for (String groupName : item.getGroupNames()) {
                        internalGetCommandRules(groupName, true, acceptedCommandTypes, command, result);
                    }
                }
                break;
            default:
                break;
        }
        return result;
    }

    private Iterable<Rule> internalGetThingRules(TriggerTypes triggerType, String thingUid, ThingStatus oldStatus,
            ThingStatus newStatus) {
        List<Rule> result = new ArrayList<>();
        Iterable<Rule> rules = getAllRules(triggerType, thingUid);

        switch (triggerType) {
            case THINGUPDATE:
                for (Rule rule : rules) {
                    for (EventTrigger t : rule.getEventtrigger()) {
                        if (t instanceof ThingStateUpdateEventTrigger) {
                            ThingStateUpdateEventTrigger tt = (ThingStateUpdateEventTrigger) t;
                            if (tt.getThing().equals(thingUid)) {
                                String stateString = tt.getState();
                                if (stateString != null) {
                                    ThingStatus triggerState = ThingStatus.valueOf(stateString);
                                    if (!newStatus.equals(triggerState)) {
                                        continue;
                                    }
                                }
                                result.add(rule);
                            }
                        }
                    }
                }
                break;
            case THINGCHANGE:
                for (Rule rule : rules) {
                    for (EventTrigger t : rule.getEventtrigger()) {
                        if (t instanceof ThingStateChangedEventTrigger) {
                            ThingStateChangedEventTrigger ct = (ThingStateChangedEventTrigger) t;
                            if (ct.getThing().equals(thingUid)) {
                                String oldStatusString = ct.getOldState();
                                if (oldStatusString != null) {
                                    ThingStatus triggerOldState = ThingStatus.valueOf(oldStatusString);
                                    if (!oldStatus.equals(triggerOldState)) {
                                        continue;
                                    }
                                }

                                String newStatusString = ct.getNewState();
                                if (newStatusString != null) {
                                    ThingStatus triggerNewState = ThingStatus.valueOf(newStatusString);
                                    if (!newStatus.equals(triggerNewState)) {
                                        continue;
                                    }
                                }
                                result.add(rule);
                            }
                        }
                    }
                }
                break;
            default:
                break;
        }
        return result;
    }

    /**
     * Removes all rules with a given trigger type from the mapping tables.
     *
     * @param type the trigger type
     */
    public void clear(TriggerTypes type) {
        switch (type) {
            case STARTUP:
                systemStartupTriggeredRules.clear();
                break;
            case SHUTDOWN:
                systemShutdownTriggeredRules.clear();
                break;
            case UPDATE:
                updateEventTriggeredRules.clear();
                break;
            case CHANGE:
                changedEventTriggeredRules.clear();
                break;
            case COMMAND:
                commandEventTriggeredRules.clear();
                break;
            case TRIGGER:
                triggerEventTriggeredRules.clear();
                break;
            case TIMER:
                for (Rule rule : timerEventTriggeredRules) {
                    removeTimerRule(rule);
                }
                timerEventTriggeredRules.clear();
                break;
            case THINGUPDATE:
                thingUpdateEventTriggeredRules.clear();
                break;
            case THINGCHANGE:
                thingChangedEventTriggeredRules.clear();
                break;
        }
    }

    /**
     * Removes all rules from all mapping tables.
     */
    public void clearAll() {
        clear(STARTUP);
        clear(SHUTDOWN);
        clear(UPDATE);
        clear(CHANGE);
        clear(COMMAND);
        clear(TIMER);
        clear(TRIGGER);
        clear(THINGUPDATE);
        clear(THINGCHANGE);
    }

    /**
     * Adds a given rule to the mapping tables
     *
     * @param rule the rule to add
     */
    public synchronized void addRule(Rule rule) {
        for (EventTrigger t : rule.getEventtrigger()) {
            // add the rule to the lookup map for the trigger kind
            if (t instanceof SystemOnStartupTrigger) {
                systemStartupTriggeredRules.add(rule);
            } else if (t instanceof SystemOnShutdownTrigger) {
                systemShutdownTriggeredRules.add(rule);
            } else if (t instanceof CommandEventTrigger) {
                CommandEventTrigger ceTrigger = (CommandEventTrigger) t;
                Set<Rule> rules = commandEventTriggeredRules.get(ceTrigger.getItem());
                if (rules == null) {
                    rules = new HashSet<>();
                    commandEventTriggeredRules.put(ceTrigger.getItem(), rules);
                }
                rules.add(rule);
            } else if (t instanceof GroupMemberCommandEventTrigger) {
                GroupMemberCommandEventTrigger gmceTrigger = (GroupMemberCommandEventTrigger) t;
                Set<Rule> rules = commandEventTriggeredRules.get(GROUP_NAME_PREFIX + gmceTrigger.getGroup());
                if (rules == null) {
                    rules = new HashSet<>();
                    commandEventTriggeredRules.put(GROUP_NAME_PREFIX + gmceTrigger.getGroup(), rules);
                }
                rules.add(rule);
            } else if (t instanceof UpdateEventTrigger) {
                UpdateEventTrigger ueTrigger = (UpdateEventTrigger) t;
                Set<Rule> rules = updateEventTriggeredRules.get(ueTrigger.getItem());
                if (rules == null) {
                    rules = new HashSet<>();
                    updateEventTriggeredRules.put(ueTrigger.getItem(), rules);
                }
                rules.add(rule);
            } else if (t instanceof GroupMemberUpdateEventTrigger) {
                GroupMemberUpdateEventTrigger gmueTrigger = (GroupMemberUpdateEventTrigger) t;
                Set<Rule> rules = updateEventTriggeredRules.get(GROUP_NAME_PREFIX + gmueTrigger.getGroup());
                if (rules == null) {
                    rules = new HashSet<>();
                    updateEventTriggeredRules.put(GROUP_NAME_PREFIX + gmueTrigger.getGroup(), rules);
                }
                rules.add(rule);
            } else if (t instanceof ChangedEventTrigger) {
                ChangedEventTrigger ceTrigger = (ChangedEventTrigger) t;
                Set<Rule> rules = changedEventTriggeredRules.get(ceTrigger.getItem());
                if (rules == null) {
                    rules = new HashSet<>();
                    changedEventTriggeredRules.put(ceTrigger.getItem(), rules);
                }
                rules.add(rule);
            } else if (t instanceof GroupMemberChangedEventTrigger) {
                GroupMemberChangedEventTrigger gmceTrigger = (GroupMemberChangedEventTrigger) t;
                Set<Rule> rules = changedEventTriggeredRules.get(GROUP_NAME_PREFIX + gmceTrigger.getGroup());
                if (rules == null) {
                    rules = new HashSet<>();
                    changedEventTriggeredRules.put(GROUP_NAME_PREFIX + gmceTrigger.getGroup(), rules);
                }
                rules.add(rule);
            } else if (t instanceof TimerTrigger) {
                try {
                    createTimer(rule, (TimerTrigger) t);
                    timerEventTriggeredRules.add(rule);
                } catch (SchedulerException e) {
                    logger.error("Cannot create timer for rule '{}': {}", rule.getName(), e.getMessage());
                }
            } else if (t instanceof EventEmittedTrigger) {
                EventEmittedTrigger eeTrigger = (EventEmittedTrigger) t;
                Set<Rule> rules = triggerEventTriggeredRules.get(eeTrigger.getChannel());
                if (rules == null) {
                    rules = new HashSet<>();
                    triggerEventTriggeredRules.put(eeTrigger.getChannel(), rules);
                }
                rules.add(rule);
            } else if (t instanceof ThingStateUpdateEventTrigger) {
                ThingStateUpdateEventTrigger tsuTrigger = (ThingStateUpdateEventTrigger) t;
                Set<Rule> rules = thingUpdateEventTriggeredRules.get(tsuTrigger.getThing());
                if (rules == null) {
                    rules = new HashSet<>();
                    thingUpdateEventTriggeredRules.put(tsuTrigger.getThing(), rules);
                }
                rules.add(rule);
            } else if (t instanceof ThingStateChangedEventTrigger) {
                ThingStateChangedEventTrigger tscTrigger = (ThingStateChangedEventTrigger) t;
                Set<Rule> rules = thingChangedEventTriggeredRules.get(tscTrigger.getThing());
                if (rules == null) {
                    rules = new HashSet<>();
                    thingChangedEventTriggeredRules.put(tscTrigger.getThing(), rules);
                }
                rules.add(rule);
            }
        }
    }

    /**
     * Removes a given rule from the mapping tables of a certain trigger type
     *
     * @param type the trigger type for which the rule should be removed
     * @param rule the rule to add
     */
    public void removeRule(TriggerTypes type, Rule rule) {
        switch (type) {
            case STARTUP:
                systemStartupTriggeredRules.remove(rule);
                break;
            case SHUTDOWN:
                systemShutdownTriggeredRules.remove(rule);
                break;
            case UPDATE:
                for (Set<Rule> rules : updateEventTriggeredRules.values()) {
                    rules.remove(rule);
                }
                break;
            case CHANGE:
                for (Set<Rule> rules : changedEventTriggeredRules.values()) {
                    rules.remove(rule);
                }
                break;
            case COMMAND:
                for (Set<Rule> rules : commandEventTriggeredRules.values()) {
                    rules.remove(rule);
                }
                break;
            case TRIGGER:
                for (Set<Rule> rules : triggerEventTriggeredRules.values()) {
                    rules.remove(rule);
                }
                break;
            case TIMER:
                timerEventTriggeredRules.remove(rule);
                removeTimerRule(rule);
                break;
            case THINGUPDATE:
                for (Set<Rule> rules : thingUpdateEventTriggeredRules.values()) {
                    rules.remove(rule);
                }
                break;
            case THINGCHANGE:
                for (Set<Rule> rules : thingChangedEventTriggeredRules.values()) {
                    rules.remove(rule);
                }
                break;
        }
    }

    /**
     * Adds all rules of a model to the mapping tables
     *
     * @param model the rule model
     */
    public void addRuleModel(RuleModel model) {
        for (Rule rule : model.getRules()) {
            addRule(rule);
        }
    }

    /**
     * Removes all rules of a given model (file) from the mapping tables.
     *
     * @param ruleModel the rule model
     */
    public void removeRuleModel(RuleModel ruleModel) {
        removeRules(UPDATE, updateEventTriggeredRules.values(), ruleModel);
        removeRules(CHANGE, changedEventTriggeredRules.values(), ruleModel);
        removeRules(COMMAND, commandEventTriggeredRules.values(), ruleModel);
        removeRules(TRIGGER, triggerEventTriggeredRules.values(), ruleModel);
        removeRules(STARTUP, Collections.singletonList(systemStartupTriggeredRules), ruleModel);
        removeRules(SHUTDOWN, Collections.singletonList(systemShutdownTriggeredRules), ruleModel);
        removeRules(TIMER, Collections.singletonList(timerEventTriggeredRules), ruleModel);
        removeRules(THINGUPDATE, thingUpdateEventTriggeredRules.values(), ruleModel);
        removeRules(THINGCHANGE, thingChangedEventTriggeredRules.values(), ruleModel);
    }

    private void removeRules(TriggerTypes type, Collection<? extends Collection<Rule>> ruleSets, RuleModel model) {
        for (Collection<Rule> ruleSet : ruleSets) {
            Set<Rule> clonedSet = new HashSet<>(ruleSet);
            // first remove all rules of the model, if not null (=non-existent)
            if (model != null) {
                for (Rule newRule : model.getRules()) {
                    for (Rule oldRule : clonedSet) {
                        if (newRule.getName().equals(oldRule.getName())) {
                            ruleSet.remove(oldRule);
                            if (type == TIMER) {
                                removeTimerRule(oldRule);
                            }
                        }
                    }
                }
            }

            // now also remove all proxified rules from the set
            clonedSet = new HashSet<>(ruleSet);
            for (Rule rule : clonedSet) {
                if (rule.eResource() == null) {
                    ruleSet.remove(rule);
                    if (type == TIMER) {
                        removeTimerRule(rule);
                    }
                }
            }
        }
    }

    private void removeTimerRule(Rule rule) {
        try {
            removeTimer(rule);
        } catch (SchedulerException e) {
            logger.error("Cannot remove timer for rule '{}'", rule.getName(), e);
        }
    }

    /**
     * Creates and schedules a new quartz-job and trigger with model and rule name as jobData.
     *
     * @param rule the rule to schedule
     * @param trigger the defined trigger
     *
     * @throws SchedulerException if there is an internal Scheduler error.
     */
    private void createTimer(Rule rule, TimerTrigger trigger) throws SchedulerException {
        String cronExpression = trigger.getCron();
        if (trigger.getTime() != null) {
            if ("noon".equals(trigger.getTime())) {
                cronExpression = "0 0 12 * * ?";
            } else if ("midnight".equals(trigger.getTime())) {
                cronExpression = "0 0 0 * * ?";
            } else {
                logger.warn("Unrecognized time expression '{}' in rule '{}'", trigger.getTime(), rule.getName());
                return;
            }
        }

        String jobIdentity = getJobIdentityString(rule, trigger);

        try {
            JobDetail job = newJob(ExecuteRuleJob.class)
                    .usingJobData(ExecuteRuleJob.JOB_DATA_RULEMODEL, rule.eResource().getURI().path())
                    .usingJobData(ExecuteRuleJob.JOB_DATA_RULENAME, rule.getName()).withIdentity(jobIdentity).build();
            Trigger quartzTrigger = newTrigger().withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)).build();
            scheduler.scheduleJob(job, Collections.singleton(quartzTrigger), true);

            logger.debug("Scheduled rule '{}' with cron expression '{}'", rule.getName(), cronExpression);
        } catch (RuntimeException e) {
            throw new SchedulerException(e.getMessage());
        }
    }

    /**
     * Delete all {@link Job}s of the DEFAULT group whose name starts with <code>rule.getName()</code>.
     *
     * @throws SchedulerException if there is an internal Scheduler error.
     */
    private void removeTimer(Rule rule) throws SchedulerException {
        Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(Scheduler.DEFAULT_GROUP));
        for (JobKey jobKey : jobKeys) {
            String jobIdentityString = getJobIdentityString(rule, null);
            if (jobKey.getName().startsWith(jobIdentityString)) {
                boolean success = scheduler.deleteJob(jobKey);
                if (!success) {
                    logger.warn("Failed to delete cron job '{}'", jobKey.getName());
                } else {
                    logger.debug("Removed scheduled cron job '{}'", jobKey.getName());
                }
            }
        }
    }

    private String getJobIdentityString(Rule rule, TimerTrigger trigger) {
        String jobIdentity = EcoreUtil.getURI(rule).trimFragment().appendFragment(rule.getName()).toString();
        if (trigger != null) {
            if (trigger.getTime() != null) {
                jobIdentity += "#" + trigger.getTime();
            } else if (trigger.getCron() != null) {
                jobIdentity += "#" + trigger.getCron();
            }
        }
        return jobIdentity;
    }

    public void startTimerRuleExecution() {
        try {
            scheduler.start();
        } catch (SchedulerException e) {
            logger.error("Error while starting the scheduler service: {}", e.getMessage());
        }
    }
}
