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
 */
@NonNullByDefault
@Component(immediate = true, service = { DSLRuleProvider.class, RuleProvider.class, DSLScriptContextProvider.class })
public class DSLRuleProvider
        implements RuleProvider, ModelRepositoryChangeListener, DSLScriptContextProvider, ReadyTracker {

    static final String MIMETYPE_OPENHAB_DSL_RULE = "application/vnd.openhab.dsl.rule";

    private final Logger logger = LoggerFactory.getLogger(DSLRuleProvider.class);
    private final Collection<ProviderChangeListener<Rule>> listeners = new ArrayList<>();
    private final Map<String, Rule> rules = new ConcurrentHashMap<>();
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
        rules.clear();
        contexts.clear();
        xExpressions.clear();
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<Rule> listener) {
        listeners.add(listener);
    }

    @Override
    public Collection<Rule> getAll() {
        return rules.values();
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<Rule> listener) {
        listeners.remove(listener);
    }

    @Override
    public void modelChanged(String modelFileName, EventType type) {
        String ruleModelType = modelFileName.substring(modelFileName.lastIndexOf(".") + 1);
        if ("rules".equalsIgnoreCase(ruleModelType)) {
            String ruleModelName = modelFileName.substring(0, modelFileName.lastIndexOf("."));
            List<ModelRulePair> modelRules = new ArrayList<>();
            switch (type) {
                case ADDED:
                    EObject model = modelRepository.getModel(modelFileName);
                    if (model instanceof RuleModel ruleModel) {
                        int index = 1;
                        for (org.openhab.core.model.rule.rules.Rule rule : ruleModel.getRules()) {
                            Rule newRule = toRule(ruleModelName, rule, index);
                            rules.put(newRule.getUID(), newRule);
                            xExpressions.put(ruleModelName + "-" + index, rule.getScript());
                            modelRules.add(new ModelRulePair(newRule, null));
                            index++;
                        }
                        handleVarDeclarations(ruleModelName, ruleModel);
                    }
                    break;
                case MODIFIED:
                    removeRuleModel(ruleModelName);
                    EObject modifiedModel = modelRepository.getModel(modelFileName);
                    if (modifiedModel instanceof RuleModel ruleModel) {
                        int index = 1;
                        for (org.openhab.core.model.rule.rules.Rule rule : ruleModel.getRules()) {
                            Rule newRule = toRule(ruleModelName, rule, index);
                            Rule oldRule = rules.remove(ruleModelName);
                            rules.put(newRule.getUID(), newRule);
                            xExpressions.put(ruleModelName + "-" + index, rule.getScript());
                            modelRules.add(new ModelRulePair(newRule, oldRule));
                            index++;
                        }
                        handleVarDeclarations(ruleModelName, ruleModel);
                    }
                    break;
                case REMOVED:
                    removeRuleModel(ruleModelName);
                    break;
                default:
                    logger.debug("Unknown event type.");
            }
            notifyProviderChangeListeners(modelRules);
        } else if ("script".equals(ruleModelType)) {
            List<ModelRulePair> modelRules = new ArrayList<>();
            switch (type) {
                case MODIFIED:
                case ADDED:
                    EObject model = modelRepository.getModel(modelFileName);
                    if (model instanceof Script script) {
                        Rule oldRule = rules.remove(modelFileName);
                        Rule newRule = toRule(modelFileName, script);
                        rules.put(newRule.getUID(), newRule);
                        modelRules.add(new ModelRulePair(newRule, oldRule));
                    }
                    break;
                case REMOVED:
                    Rule oldRule = rules.remove(modelFileName);
                    if (oldRule != null) {
                        listeners.forEach(listener -> listener.removed(this, oldRule));
                    }
                    break;
                default:
                    logger.debug("Unknown event type.");
            }
            notifyProviderChangeListeners(modelRules);
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

    private void removeRuleModel(String modelName) {
        Iterator<Entry<String, Rule>> it = rules.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Rule> entry = it.next();
            if (belongsToModel(entry.getKey(), modelName)) {
                listeners.forEach(listener -> listener.removed(this, entry.getValue()));

                it.remove();
            }
        }
        Iterator<Entry<String, XExpression>> it2 = xExpressions.entrySet().iterator();
        while (it2.hasNext()) {
            Entry<String, XExpression> entry = it2.next();
            if (belongsToModel(entry.getKey(), modelName)) {
                it2.remove();
            }
        }
        contexts.remove(modelName);
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
        String uid = modelName + "-" + index;

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
        for (String ruleFileName : modelRepository.getAllModelNamesOfType("rules")) {
            EObject model = modelRepository.getModel(ruleFileName);
            String ruleModelName = ruleFileName.substring(0, ruleFileName.indexOf("."));
            if (model instanceof RuleModel ruleModel) {
                int index = 1;
                List<ModelRulePair> modelRules = new ArrayList<>();
                for (org.openhab.core.model.rule.rules.Rule rule : ruleModel.getRules()) {
                    Rule newRule = toRule(ruleModelName, rule, index);
                    xExpressions.put(ruleModelName + "-" + index, rule.getScript());
                    modelRules.add(new ModelRulePair(newRule, null));
                    index++;
                }
                handleVarDeclarations(ruleModelName, ruleModel);

                notifyProviderChangeListeners(modelRules);
            }
        }
        modelRepository.addModelRepositoryChangeListener(this);
        readyService.markReady(marker);
    }

    private void notifyProviderChangeListeners(List<ModelRulePair> modelRules) {
        modelRules.forEach(rulePair -> {
            Rule oldRule = rulePair.oldRule();
            if (oldRule != null) {
                rules.remove(oldRule.getUID());
                rules.put(rulePair.newRule().getUID(), rulePair.newRule());
                listeners.forEach(listener -> listener.updated(this, oldRule, rulePair.newRule()));
            } else {
                rules.put(rulePair.newRule().getUID(), rulePair.newRule());
                listeners.forEach(listener -> listener.added(this, rulePair.newRule()));
            }
        });
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        readyService.unmarkReady(marker);
    }

    private record ModelRulePair(Rule newRule, @Nullable Rule oldRule) {
    }
}
