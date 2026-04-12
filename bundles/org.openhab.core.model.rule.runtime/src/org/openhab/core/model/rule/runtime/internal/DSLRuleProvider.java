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
package org.openhab.core.model.rule.runtime.internal;

import static org.openhab.core.model.core.ModelCoreConstants.isIsolatedModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleProvider;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.internal.module.handler.ChannelEventTriggerHandler;
import org.openhab.core.automation.internal.module.handler.DateTimeTriggerHandler;
import org.openhab.core.automation.internal.module.handler.DayOfWeekConditionHandler;
import org.openhab.core.automation.internal.module.handler.EphemerisConditionHandler;
import org.openhab.core.automation.internal.module.handler.GenericCronTriggerHandler;
import org.openhab.core.automation.internal.module.handler.GroupCommandTriggerHandler;
import org.openhab.core.automation.internal.module.handler.GroupStateTriggerHandler;
import org.openhab.core.automation.internal.module.handler.IntervalConditionHandler;
import org.openhab.core.automation.internal.module.handler.ItemCommandTriggerHandler;
import org.openhab.core.automation.internal.module.handler.ItemStateConditionHandler;
import org.openhab.core.automation.internal.module.handler.ItemStateTriggerHandler;
import org.openhab.core.automation.internal.module.handler.SystemTriggerHandler;
import org.openhab.core.automation.internal.module.handler.ThingStatusConditionHandler;
import org.openhab.core.automation.internal.module.handler.ThingStatusTriggerHandler;
import org.openhab.core.automation.internal.module.handler.TimeOfDayConditionHandler;
import org.openhab.core.automation.internal.module.handler.TimeOfDayTriggerHandler;
import org.openhab.core.automation.module.script.internal.handler.AbstractScriptModuleHandler;
import org.openhab.core.automation.module.script.internal.handler.ScriptActionHandler;
import org.openhab.core.automation.util.ActionBuilder;
import org.openhab.core.automation.util.ConditionBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.automation.util.TriggerBuilder;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.model.core.EventType;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.core.ModelRepositoryChangeListener;
import org.openhab.core.model.rule.jvmmodel.RulesRefresher;
import org.openhab.core.model.rule.rules.ChangedEventTrigger;
import org.openhab.core.model.rule.rules.CommandEventTrigger;
import org.openhab.core.model.rule.rules.DateTimeTrigger;
import org.openhab.core.model.rule.rules.DayOfWeekCondition;
import org.openhab.core.model.rule.rules.EventEmittedTrigger;
import org.openhab.core.model.rule.rules.EventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberChangedEventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberCommandEventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberUpdateEventTrigger;
import org.openhab.core.model.rule.rules.HolidayCondition;
import org.openhab.core.model.rule.rules.InDaysetCondition;
import org.openhab.core.model.rule.rules.IntervalCondition;
import org.openhab.core.model.rule.rules.ItemStateCondition;
import org.openhab.core.model.rule.rules.RuleModel;
import org.openhab.core.model.rule.rules.SystemOnShutdownTrigger;
import org.openhab.core.model.rule.rules.SystemOnStartupTrigger;
import org.openhab.core.model.rule.rules.SystemStartlevelTrigger;
import org.openhab.core.model.rule.rules.ThingStateChangedEventTrigger;
import org.openhab.core.model.rule.rules.ThingStateUpdateEventTrigger;
import org.openhab.core.model.rule.rules.ThingStatusCondition;
import org.openhab.core.model.rule.rules.TimeOfDayCondition;
import org.openhab.core.model.rule.rules.TimerTrigger;
import org.openhab.core.model.rule.rules.UpdateEventTrigger;
import org.openhab.core.model.rule.rules.WeekdayCondition;
import org.openhab.core.model.script.runtime.DSLScriptContextProvider;
import org.openhab.core.model.script.script.Script;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.ReadyService.ReadyTracker;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This RuleProvider provides rules that are defined in DSL rule files.
 * All rules consist out of a list of triggers and a single script action.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Laurent Garnier - Add support for conditions
 * @author Laurent Garnier - Add optional rule UID + registry notification refactoring
 */
@NonNullByDefault
@Component(immediate = true, service = { DSLRuleProvider.class, RuleProvider.class, DSLScriptContextProvider.class })
public class DSLRuleProvider
        implements RuleProvider, ModelRepositoryChangeListener, DSLScriptContextProvider, ReadyTracker {

    static final String MIMETYPE_OPENHAB_DSL_RULE = "application/vnd.openhab.dsl.rule";

    private final Logger logger = LoggerFactory.getLogger(DSLRuleProvider.class);
    private final Collection<ProviderChangeListener<Rule>> listeners = new ArrayList<>();
    private final Map<String, List<Rule>> rulesMap = new ConcurrentHashMap<>();
    private final Map<String, IEvaluationContext> contexts = new ConcurrentHashMap<>();
    private final Map<String, XExpression> xExpressions = new ConcurrentHashMap<>();
    private final ReadyMarker marker = new ReadyMarker("rules", "dslprovider");
    private int triggerId = 0;

    private final ModelRepository modelRepository;
    private final ReadyService readyService;

    @Activate
    public DSLRuleProvider(@Reference ModelRepository modelRepository, @Reference ReadyService readyService) {
        this.modelRepository = modelRepository;
        this.readyService = readyService;
    }

    @Activate
    protected void activate() {
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(RulesRefresher.RULES_REFRESH_MARKER_TYPE)
                .withIdentifier(RulesRefresher.RULES_REFRESH));
    }

    @Deactivate
    protected void deactivate() {
        modelRepository.removeModelRepositoryChangeListener(this);
        rulesMap.clear();
        contexts.clear();
        xExpressions.clear();
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<Rule> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<Rule> listener) {
        listeners.remove(listener);
    }

    @Override
    public Collection<Rule> getAll() {
        // Ignore isolated models
        return rulesMap.entrySet().stream().filter(e -> !isIsolatedModel(e.getKey()))
                .flatMap(e -> e.getValue().stream()).toList();
    }

    /**
     * Returns all rules originating from the given model name.
     *
     * @param modelFileName the full model file name, including ".rules" or ".script" extension
     * @return the rules associated with the given model name, or an empty collection if none exist
     */
    public Collection<Rule> getAllFromModel(String modelFileName) {
        return List.copyOf(rulesMap.getOrDefault(modelFileName, List.of()));
    }

    @Override
    public void modelChanged(String modelFileName, EventType type) {
        String ruleModelType = modelFileName.substring(modelFileName.lastIndexOf(".") + 1);
        List<Rule> oldRules;
        List<Rule> newRules = new ArrayList<>();
        if ("rules".equalsIgnoreCase(ruleModelType)) {
            boolean isolated = isIsolatedModel(modelFileName);
            String ruleModelName = modelFileName.substring(0, modelFileName.lastIndexOf("."));
            switch (type) {
                case ADDED:
                case MODIFIED:
                    EObject model = modelRepository.getModel(modelFileName);
                    int index = 1;
                    if (model instanceof RuleModel ruleModel) {
                        for (org.openhab.core.model.rule.rules.Rule rule : ruleModel.getRules()) {
                            newRules.add(toRule(ruleModelName, rule, index));
                            if (!isolated) {
                                xExpressions.put(ruleModelName + "-" + index, rule.getScript());
                            }
                            index++;
                        }
                        if (!isolated) {
                            handleVarDeclarations(ruleModelName, ruleModel);
                        }
                    }
                    oldRules = rulesMap.put(modelFileName, newRules);
                    if (!isolated) {
                        // Cleanup xExpressions for old rules
                        int nbOldRules = oldRules == null ? 0 : oldRules.size();
                        while (index <= nbOldRules) {
                            xExpressions.remove(ruleModelName + "-" + index);
                            index++;
                        }
                        // Cleanup contexts if no new rules
                        if (newRules.isEmpty()) {
                            contexts.remove(ruleModelName);
                        }
                        notifyProviderChangeListeners(calcChanges(modelFileName, oldRules, newRules));
                    }
                    break;
                case REMOVED:
                    oldRules = rulesMap.remove(modelFileName);
                    if (!isolated) {
                        for (Iterator<Entry<String, XExpression>> it = xExpressions.entrySet().iterator(); it
                                .hasNext();) {
                            Entry<String, XExpression> entry = it.next();
                            if (belongsToModel(entry.getKey(), ruleModelName)) {
                                it.remove();
                            }
                        }
                        contexts.remove(ruleModelName);
                        notifyProviderChangeListeners(calcChanges(modelFileName, oldRules, null));
                    }
                    break;
                default:
                    logger.debug("Unknown event type.");
            }
        } else if ("script".equals(ruleModelType)) {
            switch (type) {
                case ADDED:
                case MODIFIED:
                    EObject model = modelRepository.getModel(modelFileName);
                    if (model instanceof Script script) {
                        newRules.add(toRule(modelFileName, script));
                    }
                    oldRules = rulesMap.put(modelFileName, newRules);
                    if (!isIsolatedModel(modelFileName)) {
                        notifyProviderChangeListeners(calcChanges(modelFileName, oldRules, newRules));
                    }
                    break;
                case REMOVED:
                    oldRules = rulesMap.remove(modelFileName);
                    if (!isIsolatedModel(modelFileName)) {
                        notifyProviderChangeListeners(calcChanges(modelFileName, oldRules, null));
                    }
                    break;
                default:
                    logger.debug("Unknown event type.");
            }
        }
    }

    @Override
    public @Nullable IEvaluationContext getContext(String contextName) {
        return contexts.get(contextName);
    }

    @Override
    public @Nullable XExpression getParsedScript(String modelName, String index) {
        return xExpressions.get(modelName + "-" + index);
    }

    private void handleVarDeclarations(String modelName, RuleModel ruleModel) {
        IEvaluationContext context = RuleContextHelper.getContext(ruleModel);
        contexts.put(modelName, context);
    }

    private Changes calcChanges(String modelFileName, @Nullable List<Rule> oldRules, @Nullable List<Rule> newRules) {
        if (oldRules == null || oldRules.isEmpty()) {
            return new Changes(newRules == null ? List.of() : List.copyOf(newRules), List.of(), List.of());
        }
        List<Rule> oldMutable = new ArrayList<>(oldRules);
        List<Rule> newMutable = newRules == null ? new ArrayList<>() : new ArrayList<>(newRules);
        List<RulePair> modified = new ArrayList<>();
        Rule oldRule, newRule;
        boolean found;
        for (Iterator<Rule> iterator = oldMutable.iterator(); iterator.hasNext();) {
            oldRule = iterator.next();
            found = false;
            String uid = oldRule.getUID();
            for (Iterator<Rule> newIterator = newMutable.iterator(); newIterator.hasNext() && !found;) {
                newRule = newIterator.next();
                if (uid.equals(newRule.getUID())) {
                    modified.add(new RulePair(oldRule, newRule));
                    newIterator.remove();
                    found = true;
                }
            }
            if (!found) {
                // Check if the old rule exists in another model file with the same UID
                Rule existingRule = rulesMap.entrySet().stream()
                        .filter(e -> !e.getKey().equals(modelFileName) && !isIsolatedModel(e.getKey()))
                        .flatMap(e -> e.getValue().stream()).filter(r -> uid.equals(r.getUID())).findAny().orElse(null);
                if (existingRule != null) {
                    modified.add(new RulePair(oldRule, existingRule));
                    found = true;
                }
            }
            if (found) {
                iterator.remove();
            }
        }
        return new Changes(newMutable, modified, oldMutable);
    }

    private boolean belongsToModel(String id, String modelName) {
        int idx = id.lastIndexOf("-");
        if (idx >= 0) {
            String prefix = id.substring(0, idx);
            return prefix.equals(modelName);
        }
        return false;
    }

    private Rule toRule(String modelName, Script script) {
        String scriptText = NodeModelUtils.findActualNodeFor(script).getText();

        Configuration cfg = new Configuration();
        cfg.put(AbstractScriptModuleHandler.CONFIG_SCRIPT, removeIndentation(scriptText));
        cfg.put(AbstractScriptModuleHandler.CONFIG_SCRIPT_TYPE, MIMETYPE_OPENHAB_DSL_RULE);
        List<Action> actions = List.of(ActionBuilder.create().withId("script").withTypeUID(ScriptActionHandler.TYPE_ID)
                .withConfiguration(cfg).build());

        return RuleBuilder.create(modelName).withTags("Script").withName(modelName).withActions(actions).build();
    }

    private Rule toRule(String modelName, org.openhab.core.model.rule.rules.Rule rule, int index) {
        String name = rule.getName();
        String uid = rule.getUid();
        if (uid == null || uid.isBlank()) {
            uid = modelName + "-" + index;
        }

        // Create Triggers
        triggerId = 0;
        List<Trigger> triggers = rule.getEventtrigger().stream().map(this::mapTrigger).filter(Objects::nonNull)
                .toList();

        // Conditions
        List<Condition> conditions = rule.getConditions().stream().map(this::mapCondition).filter(Objects::nonNull)
                .toList();

        // Create Action
        String context = DSLScriptContextProvider.CONTEXT_IDENTIFIER + modelName + "-" + index + "\n";
        XExpression expression = rule.getScript();
        String script = NodeModelUtils.findActualNodeFor(expression).getText();
        Configuration cfg = new Configuration();
        cfg.put(AbstractScriptModuleHandler.CONFIG_SCRIPT, context + removeIndentation(script));
        cfg.put(AbstractScriptModuleHandler.CONFIG_SCRIPT_TYPE, MIMETYPE_OPENHAB_DSL_RULE);
        List<Action> actions = List.of(ActionBuilder.create().withId("script").withTypeUID(ScriptActionHandler.TYPE_ID)
                .withConfiguration(cfg).build());

        List<String> ruleTags = rule.getTags();
        Set<String> tags = ruleTags == null ? Set.of() : Set.copyOf(ruleTags);

        return RuleBuilder.create(uid).withTags(tags).withName(name).withTriggers(triggers).withActions(actions)
                .withConditions(conditions).build();
    }

    private String removeIndentation(String script) {
        String s = script;
        // first let's remove empty lines at the beginning and add an empty line at the end to beautify the yaml style.
        if (s.startsWith("\n")) {
            s = s.substring(1);
        }
        if (s.startsWith("\r\n")) {
            s = s.substring(2);
        }
        if (!(s.endsWith("\n\n") || s.endsWith("\r\n\r\n"))) {
            s += "\n\n";
        }
        String firstLine = s.lines().findFirst().orElse("");
        String indentation = firstLine == null ? ""
                : firstLine.substring(0, firstLine.length() - firstLine.stripLeading().length());
        return s.lines().map(line -> (line.startsWith(indentation) ? line.substring(indentation.length()) : line))
                .collect(Collectors.joining("\n"));
    }

    private @Nullable Trigger mapTrigger(EventTrigger eventTrigger) {
        Configuration cfg = new Configuration();
        return switch (eventTrigger) {
            case SystemOnStartupTrigger t -> {
                cfg.put(SystemTriggerHandler.CFG_STARTLEVEL, 40);
                yield TriggerBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(SystemTriggerHandler.STARTLEVEL_MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case SystemStartlevelTrigger sllTrigger -> {
                cfg.put(SystemTriggerHandler.CFG_STARTLEVEL, sllTrigger.getLevel());
                yield TriggerBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(SystemTriggerHandler.STARTLEVEL_MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case CommandEventTrigger ceTrigger -> {
                cfg.put(ItemCommandTriggerHandler.CFG_ITEMNAME, ceTrigger.getItem());
                if (ceTrigger.getCommand() != null) {
                    cfg.put(ItemCommandTriggerHandler.CFG_COMMAND, ceTrigger.getCommand().getValue());
                }
                yield TriggerBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(ItemCommandTriggerHandler.MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case GroupMemberCommandEventTrigger ceTrigger -> {
                cfg.put(GroupCommandTriggerHandler.CFG_GROUPNAME, ceTrigger.getGroup());
                if (ceTrigger.getCommand() != null) {
                    cfg.put(GroupCommandTriggerHandler.CFG_COMMAND, ceTrigger.getCommand().getValue());
                }
                yield TriggerBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(GroupCommandTriggerHandler.MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case UpdateEventTrigger ueTrigger -> {
                cfg.put(ItemStateTriggerHandler.CFG_ITEMNAME, ueTrigger.getItem());
                if (ueTrigger.getState() != null) {
                    cfg.put(ItemStateTriggerHandler.CFG_STATE, ueTrigger.getState().getValue());
                }
                yield TriggerBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(ItemStateTriggerHandler.UPDATE_MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case GroupMemberUpdateEventTrigger ueTrigger -> {
                cfg.put(GroupStateTriggerHandler.CFG_GROUPNAME, ueTrigger.getGroup());
                if (ueTrigger.getState() != null) {
                    cfg.put(GroupStateTriggerHandler.CFG_STATE, ueTrigger.getState().getValue());
                }
                yield TriggerBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(GroupStateTriggerHandler.UPDATE_MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case ChangedEventTrigger ceTrigger -> {
                cfg.put(ItemStateTriggerHandler.CFG_ITEMNAME, ceTrigger.getItem());
                if (ceTrigger.getNewState() != null) {
                    cfg.put(ItemStateTriggerHandler.CFG_STATE, ceTrigger.getNewState().getValue());
                }
                if (ceTrigger.getOldState() != null) {
                    cfg.put(ItemStateTriggerHandler.CFG_PREVIOUS_STATE, ceTrigger.getOldState().getValue());
                }
                yield TriggerBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(ItemStateTriggerHandler.CHANGE_MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case GroupMemberChangedEventTrigger ceTrigger -> {
                cfg.put(GroupStateTriggerHandler.CFG_GROUPNAME, ceTrigger.getGroup());
                if (ceTrigger.getNewState() != null) {
                    cfg.put(GroupStateTriggerHandler.CFG_STATE, ceTrigger.getNewState().getValue());
                }
                if (ceTrigger.getOldState() != null) {
                    cfg.put(GroupStateTriggerHandler.CFG_PREVIOUS_STATE, ceTrigger.getOldState().getValue());
                }
                yield TriggerBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(GroupStateTriggerHandler.CHANGE_MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case TimerTrigger timeTrigger when timeTrigger.getCron() != null -> {
                cfg.put(GenericCronTriggerHandler.CFG_CRON_EXPRESSION, timeTrigger.getCron());
                yield TriggerBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(GenericCronTriggerHandler.MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case TimerTrigger timeTrigger when timeTrigger.getCron() == null -> {
                String id = timeTrigger.getTime();
                if ("noon".equals(id)) {
                    cfg.put(TimeOfDayTriggerHandler.CFG_TIME, "12:00");
                } else if ("midnight".equals(id)) {
                    cfg.put(TimeOfDayTriggerHandler.CFG_TIME, "00:00");
                } else {
                    cfg.put(TimeOfDayTriggerHandler.CFG_TIME, id);
                }
                yield TriggerBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(TimeOfDayTriggerHandler.MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case DateTimeTrigger dtTrigger -> {
                cfg.put(DateTimeTriggerHandler.CONFIG_ITEM_NAME, dtTrigger.getItem());
                cfg.put(DateTimeTriggerHandler.CONFIG_TIME_ONLY, dtTrigger.isTimeOnly());
                cfg.put(DateTimeTriggerHandler.CONFIG_OFFSET, dtTrigger.getOffset());
                yield TriggerBuilder.create().withId(Integer.toString((triggerId++)))
                        .withTypeUID(DateTimeTriggerHandler.MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case EventEmittedTrigger eeTrigger -> {
                cfg.put(ChannelEventTriggerHandler.CFG_CHANNEL, eeTrigger.getChannel());
                if (eeTrigger.getTrigger() != null) {
                    cfg.put(ChannelEventTriggerHandler.CFG_CHANNEL_EVENT, eeTrigger.getTrigger().getValue());
                }
                yield TriggerBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(ChannelEventTriggerHandler.MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case ThingStateUpdateEventTrigger tsuTrigger -> {
                cfg.put(ThingStatusTriggerHandler.CFG_THING_UID, tsuTrigger.getThing());
                cfg.put(ThingStatusTriggerHandler.CFG_STATUS, tsuTrigger.getState());
                yield TriggerBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(ThingStatusTriggerHandler.UPDATE_MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case ThingStateChangedEventTrigger tscTrigger -> {
                cfg.put(ThingStatusTriggerHandler.CFG_THING_UID, tscTrigger.getThing());
                cfg.put(ThingStatusTriggerHandler.CFG_STATUS, tscTrigger.getNewState());
                cfg.put(ThingStatusTriggerHandler.CFG_PREVIOUS_STATUS, tscTrigger.getOldState());
                yield TriggerBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(ThingStatusTriggerHandler.CHANGE_MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case SystemOnShutdownTrigger t -> {
                logger.warn("System shutdown rule triggers are no longer supported!");
                yield null;
            }
            default -> {
                logger.warn("Unknown trigger type '{}' - ignoring it.", eventTrigger.getClass().getSimpleName());
                yield null;
            }
        };
    }

    private @Nullable Condition mapCondition(org.openhab.core.model.rule.rules.Condition condition) {
        Configuration cfg = new Configuration();
        return switch (condition) {
            case TimeOfDayCondition todCond -> {
                cfg.put(TimeOfDayConditionHandler.CFG_START_TIME, todCond.getStart());
                cfg.put(TimeOfDayConditionHandler.CFG_END_TIME, todCond.getEnd());
                yield ConditionBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(TimeOfDayConditionHandler.MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case DayOfWeekCondition dowCond -> {
                List<String> days = new ArrayList<>();
                dowCond.getWeekDays().forEach(day -> {
                    String d = switch (day) {
                        case "Monday" -> "MON";
                        case "Tuesday" -> "TUE";
                        case "Wednesday" -> "WED";
                        case "Thursday" -> "THU";
                        case "Friday" -> "FRI";
                        case "Saturday" -> "SAT";
                        case "Sunday" -> "SUN";
                        default -> null;
                    };
                    if (d != null) {
                        days.add(d);
                    }
                });
                cfg.put(DayOfWeekConditionHandler.CFG_DAYS, days);
                yield ConditionBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(DayOfWeekConditionHandler.MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case WeekdayCondition weekdayCond -> {
                String offset = weekdayCond.getOffset();
                cfg.put("offset", offset == null ? 0 : Integer.valueOf(offset));
                yield ConditionBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID("weekday".equals(weekdayCond.getType())
                                ? EphemerisConditionHandler.WEEKDAY_MODULE_TYPE_ID
                                : EphemerisConditionHandler.WEEKEND_MODULE_TYPE_ID)
                        .withConfiguration(cfg).build();
            }
            case HolidayCondition holidayCond -> {
                String offset = holidayCond.getOffset();
                cfg.put("offset", offset == null ? 0 : Integer.valueOf(offset));
                yield ConditionBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(holidayCond.isNegation() ? EphemerisConditionHandler.NOT_HOLIDAY_MODULE_TYPE_ID
                                : EphemerisConditionHandler.HOLIDAY_MODULE_TYPE_ID)
                        .withConfiguration(cfg).build();
            }
            case InDaysetCondition daysetCond -> {
                String offset = daysetCond.getOffset();
                cfg.put("dayset", daysetCond.getDayset());
                cfg.put("offset", offset == null ? 0 : Integer.valueOf(offset));
                yield ConditionBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(EphemerisConditionHandler.DAYSET_MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case IntervalCondition intervalCond -> {
                cfg.put(IntervalConditionHandler.CFG_MIN_INTERVAL, intervalCond.getInterval());
                yield ConditionBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(IntervalConditionHandler.MODULE_TYPE_ID).withConfiguration(cfg).build();
            }
            case ThingStatusCondition tsCond -> {
                cfg.put(ThingStatusConditionHandler.CFG_THING_UID, tsCond.getThing());
                cfg.put(ThingStatusConditionHandler.CFG_OPERATOR, tsCond.isNegation() ? "!=" : "=");
                cfg.put(ThingStatusConditionHandler.CFG_STATUS, tsCond.getStatus());
                yield ConditionBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(ThingStatusConditionHandler.THING_STATUS_CONDITION).withConfiguration(cfg).build();
            }
            case ItemStateCondition isCond -> {
                String operator = isCond.getOperator();
                cfg.put(ItemStateConditionHandler.ITEM_NAME, isCond.getItem());
                cfg.put(ItemStateConditionHandler.OPERATOR, operator == null ? "=" : operator);
                cfg.put(ItemStateConditionHandler.STATE, isCond.getState().getValue());
                yield ConditionBuilder.create().withId(Integer.toString(triggerId++))
                        .withTypeUID(ItemStateConditionHandler.ITEM_STATE_CONDITION).withConfiguration(cfg).build();
            }
            default -> {
                logger.warn("Unknown condition type '{}' - ignoring it.", condition.getClass().getSimpleName());
                yield null;
            }
        };
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        for (String modelFileName : modelRepository.getAllModelNamesOfType("rules")) {
            EObject model = modelRepository.getModel(modelFileName);
            if (model instanceof RuleModel ruleModel) {
                boolean isolated = isIsolatedModel(modelFileName);
                String ruleModelName = modelFileName.substring(0, modelFileName.lastIndexOf("."));
                int index = 1;
                List<Rule> newRules = new ArrayList<>();
                for (org.openhab.core.model.rule.rules.Rule rule : ruleModel.getRules()) {
                    newRules.add(toRule(ruleModelName, rule, index));
                    if (!isolated) {
                        xExpressions.put(ruleModelName + "-" + index, rule.getScript());
                    }
                    index++;
                }
                List<Rule> oldRules = rulesMap.put(modelFileName, newRules);
                if (!isolated) {
                    handleVarDeclarations(ruleModelName, ruleModel);
                    notifyProviderChangeListeners(calcChanges(modelFileName, oldRules, newRules));
                }
            }
        }
        modelRepository.addModelRepositoryChangeListener(this);
        readyService.markReady(marker);
    }

    private void notifyProviderChangeListeners(Changes changes) {
        if (listeners.isEmpty()) {
            return;
        }
        for (Rule rule : changes.removed) {
            for (ProviderChangeListener<Rule> listener : listeners) {
                listener.removed(this, rule);
            }
        }
        for (RulePair pair : changes.modified) {
            for (ProviderChangeListener<Rule> listener : listeners) {
                listener.updated(this, pair.oldRule, pair.newRule);
            }
        }
        for (Rule rule : changes.added) {
            for (ProviderChangeListener<Rule> listener : listeners) {
                listener.added(this, rule);
            }
        }
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        readyService.unmarkReady(marker);
    }

    private record RulePair(Rule oldRule, Rule newRule) {
    }

    private record Changes(List<Rule> added, List<RulePair> modified, List<Rule> removed) {
    }
}
