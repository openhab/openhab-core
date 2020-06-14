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
package org.openhab.core.model.rule.runtime.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleProvider;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.util.ActionBuilder;
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
import org.openhab.core.model.rule.rules.EventEmittedTrigger;
import org.openhab.core.model.rule.rules.EventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberChangedEventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberCommandEventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberUpdateEventTrigger;
import org.openhab.core.model.rule.rules.RuleModel;
import org.openhab.core.model.rule.rules.SystemOnShutdownTrigger;
import org.openhab.core.model.rule.rules.SystemOnStartupTrigger;
import org.openhab.core.model.rule.rules.ThingStateChangedEventTrigger;
import org.openhab.core.model.rule.rules.ThingStateUpdateEventTrigger;
import org.openhab.core.model.rule.rules.TimerTrigger;
import org.openhab.core.model.rule.rules.UpdateEventTrigger;
import org.openhab.core.model.script.runtime.DSLScriptContextProvider;
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
 * No rule conditions are used as this concept does not exist for DSL rules.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { DSLRuleProvider.class, RuleProvider.class, DSLScriptContextProvider.class })
public class DSLRuleProvider
        implements RuleProvider, ModelRepositoryChangeListener, DSLScriptContextProvider, ReadyTracker {

    private static final String RULES_MODEL_NAME = "rules";
    private static final String ITEMS_MODEL_NAME = "items";
    private static final String THINGS_MODEL_NAME = "things";
    static final String MIMETYPE_OPENHAB_DSL_RULE = "application/vnd.openhab.dsl.rule";

    private final Logger logger = LoggerFactory.getLogger(DSLRuleProvider.class);
    private final Collection<ProviderChangeListener<Rule>> listeners = new ArrayList<>();
    private final Map<String, Rule> rules = new HashMap<>();
    private final Map<String, IEvaluationContext> contexts = new HashMap<>();
    private final Map<String, XExpression> xExpressions = new HashMap<>();
    private int triggerId = 0;
    private Set<String> markers = new HashSet<>();

    private final ModelRepository modelRepository;
    private final ReadyService readyService;

    @Activate
    public DSLRuleProvider(@Reference ModelRepository modelRepository, @Reference ReadyService readyService) {
        this.modelRepository = modelRepository;
        this.readyService = readyService;
    }

    @Activate
    protected void activate() {
        readyService.registerTracker(this, new ReadyMarkerFilter().withType("dsl"));
    }

    @Deactivate
    protected void deactivate() {
        modelRepository.removeModelRepositoryChangeListener(this);
        rules.clear();
        contexts.clear();
        xExpressions.clear();
        markers.clear();
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
        String ruleModelName = modelFileName.substring(0, modelFileName.indexOf("."));
        switch (type) {
            case ADDED:
                EObject model = modelRepository.getModel(modelFileName);
                if (model instanceof RuleModel) {
                    RuleModel ruleModel = (RuleModel) model;
                    int index = 1;
                    for (org.openhab.core.model.rule.rules.Rule rule : ruleModel.getRules()) {
                        addRule(toRule(ruleModelName, rule, index));
                        xExpressions.put(ruleModelName + "-" + index, rule.getScript());
                        index++;
                    }
                    handleVarDeclarations(ruleModelName, ruleModel);
                }
                break;
            case MODIFIED:
                removeRuleModel(ruleModelName);
                EObject modifiedModel = modelRepository.getModel(modelFileName);
                if (modifiedModel instanceof RuleModel) {
                    RuleModel ruleModel = (RuleModel) modifiedModel;
                    int index = 1;
                    for (org.openhab.core.model.rule.rules.Rule rule : ruleModel.getRules()) {
                        Rule newRule = toRule(ruleModelName, rule, index);
                        Rule oldRule = rules.get(ruleModelName);
                        updateRule(oldRule, newRule);
                        xExpressions.put(ruleModelName + "-" + index, rule.getScript());
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

    private void addRule(Rule rule) {
        rules.put(rule.getUID(), rule);

        for (ProviderChangeListener<Rule> providerChangeListener : listeners) {
            providerChangeListener.added(this, rule);
        }
    }

    private void updateRule(@Nullable Rule oldRule, Rule newRule) {
        if (oldRule != null) {
            rules.remove(oldRule.getUID());
            for (ProviderChangeListener<Rule> providerChangeListener : listeners) {
                providerChangeListener.updated(this, oldRule, newRule);
            }
        } else {
            for (ProviderChangeListener<Rule> providerChangeListener : listeners) {
                providerChangeListener.added(this, newRule);
            }
        }
        rules.put(newRule.getUID(), newRule);
    }

    private void removeRuleModel(String modelName) {
        Iterator<Entry<String, Rule>> it = rules.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Rule> entry = it.next();
            if (entry.getKey().startsWith(modelName + "-")) {
                removeRule(entry.getValue());
                it.remove();
            }
        }
        Iterator<Entry<String, XExpression>> it2 = xExpressions.entrySet().iterator();
        while (it2.hasNext()) {
            Entry<String, XExpression> entry = it2.next();
            if (entry.getKey().startsWith(modelName + "-")) {
                it2.remove();
            }
        }
        contexts.remove(modelName);
    }

    private void removeRule(Rule rule) {
        for (ProviderChangeListener<Rule> providerChangeListener : listeners) {
            providerChangeListener.removed(this, rule);
        }
    }

    private Rule toRule(String modelName, org.openhab.core.model.rule.rules.Rule rule, int index) {
        String name = rule.getName();
        String uid = modelName + "-" + index;

        // Create Triggers
        triggerId = 0;
        List<Trigger> triggers = new ArrayList<>();
        for (EventTrigger t : rule.getEventtrigger()) {
            Trigger trigger = mapTrigger(t);
            if (trigger != null) {
                triggers.add(trigger);
            }
        }

        // Create Action
        String context = DSLScriptContextProvider.CONTEXT_IDENTIFIER + modelName + "-" + index + "\n";
        XBlockExpression expression = rule.getScript();
        String script = NodeModelUtils.findActualNodeFor(expression).getText();
        Configuration cfg = new Configuration();
        cfg.put("script", context + removeIndentation(script));
        cfg.put("type", MIMETYPE_OPENHAB_DSL_RULE);
        List<Action> actions = Collections.singletonList(ActionBuilder.create().withId("script")
                .withTypeUID("script.ScriptAction").withConfiguration(cfg).build());

        return RuleBuilder.create(uid).withName(name).withTriggers(triggers).withActions(actions).build();
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
        String indentation = firstLine.substring(0, firstLine.length() - firstLine.stripLeading().length());
        return s.lines().map(line -> {
            return line.startsWith(indentation) ? line.substring(indentation.length()) : line;
        }).collect(Collectors.joining("\n"));
    }

    private @Nullable Trigger mapTrigger(EventTrigger t) {
        if (t instanceof SystemOnStartupTrigger) {
            Configuration cfg = new Configuration();
            cfg.put("startlevel", 20);
            return TriggerBuilder.create().withId(Integer.toString(triggerId++))
                    .withTypeUID("core.SystemStartlevelTrigger").withConfiguration(cfg).build();
        } else if (t instanceof SystemOnShutdownTrigger) {
            logger.warn("System shutdown rule triggers are no longer supported!");
            return null;
        } else if (t instanceof CommandEventTrigger) {
            CommandEventTrigger ceTrigger = (CommandEventTrigger) t;
            Configuration cfg = new Configuration();
            cfg.put("itemName", ceTrigger.getItem());
            if (ceTrigger.getCommand() != null) {
                cfg.put("command", ceTrigger.getCommand().getValue());
            }
            return TriggerBuilder.create().withId(Integer.toString(triggerId++)).withTypeUID("core.ItemCommandTrigger")
                    .withConfiguration(cfg).build();
        } else if (t instanceof GroupMemberCommandEventTrigger) {
            GroupMemberCommandEventTrigger ceTrigger = (GroupMemberCommandEventTrigger) t;
            Configuration cfg = new Configuration();
            cfg.put("groupName", ceTrigger.getGroup());
            if (ceTrigger.getCommand() != null) {
                cfg.put("command", ceTrigger.getCommand().getValue());
            }
            return TriggerBuilder.create().withId(Integer.toString(triggerId++)).withTypeUID("core.GroupCommandTrigger")
                    .withConfiguration(cfg).build();
        } else if (t instanceof UpdateEventTrigger) {
            UpdateEventTrigger ueTrigger = (UpdateEventTrigger) t;
            Configuration cfg = new Configuration();
            cfg.put("itemName", ueTrigger.getItem());
            if (ueTrigger.getState() != null) {
                cfg.put("state", ueTrigger.getState().getValue());
            }
            return TriggerBuilder.create().withId(Integer.toString(triggerId++))
                    .withTypeUID("core.ItemStateUpdateTrigger").withConfiguration(cfg).build();
        } else if (t instanceof GroupMemberUpdateEventTrigger) {
            GroupMemberUpdateEventTrigger ueTrigger = (GroupMemberUpdateEventTrigger) t;
            Configuration cfg = new Configuration();
            cfg.put("groupName", ueTrigger.getGroup());
            if (ueTrigger.getState() != null) {
                cfg.put("state", ueTrigger.getState().getValue());
            }
            return TriggerBuilder.create().withId(Integer.toString(triggerId++))
                    .withTypeUID("core.GroupStateUpdateTrigger").withConfiguration(cfg).build();
        } else if (t instanceof ChangedEventTrigger) {
            ChangedEventTrigger ceTrigger = (ChangedEventTrigger) t;
            Configuration cfg = new Configuration();
            cfg.put("itemName", ceTrigger.getItem());
            if (ceTrigger.getNewState() != null) {
                cfg.put("state", ceTrigger.getNewState().getValue());
            }
            if (ceTrigger.getOldState() != null) {
                cfg.put("previousState", ceTrigger.getOldState().getValue());
            }
            return TriggerBuilder.create().withId(Integer.toString(triggerId++))
                    .withTypeUID("core.ItemStateChangeTrigger").withConfiguration(cfg).build();
        } else if (t instanceof GroupMemberChangedEventTrigger) {
            GroupMemberChangedEventTrigger ceTrigger = (GroupMemberChangedEventTrigger) t;
            Configuration cfg = new Configuration();
            cfg.put("groupName", ceTrigger.getGroup());
            if (ceTrigger.getNewState() != null) {
                cfg.put("state", ceTrigger.getNewState().getValue());
            }
            if (ceTrigger.getOldState() != null) {
                cfg.put("previousState", ceTrigger.getOldState().getValue());
            }
            return TriggerBuilder.create().withId(Integer.toString(triggerId++))
                    .withTypeUID("core.GroupStateChangeTrigger").withConfiguration(cfg).build();
        } else if (t instanceof TimerTrigger) {
            TimerTrigger tt = (TimerTrigger) t;
            Configuration cfg = new Configuration();
            String id;
            if (tt.getCron() != null) {
                id = tt.getCron();
                cfg.put("cronExpression", tt.getCron());
            } else {
                id = tt.getTime();
                if (id.equals("noon")) {
                    cfg.put("cronExpression", "0 0 12 * * ?");
                } else if (id.equals("midnight")) {
                    cfg.put("cronExpression", "0 0 0 * * ?");
                } else {
                    logger.warn("Unrecognized time expression '{}' in rule trigger", tt.getTime());
                    return null;
                }
            }
            return TriggerBuilder.create().withId(Integer.toString(triggerId++)).withTypeUID("timer.GenericCronTrigger")
                    .withConfiguration(cfg).build();
        } else if (t instanceof EventEmittedTrigger) {
            EventEmittedTrigger eeTrigger = (EventEmittedTrigger) t;
            Configuration cfg = new Configuration();
            cfg.put("channelUID", eeTrigger.getChannel());
            if (eeTrigger.getTrigger() != null) {
                cfg.put("event", eeTrigger.getTrigger().getValue());
            }
            return TriggerBuilder.create().withId(Integer.toString(triggerId++)).withTypeUID("core.ChannelEventTrigger")
                    .withConfiguration(cfg).build();
        } else if (t instanceof ThingStateUpdateEventTrigger) {
            ThingStateUpdateEventTrigger tsuTrigger = (ThingStateUpdateEventTrigger) t;
            Configuration cfg = new Configuration();
            cfg.put("thingUID", tsuTrigger.getThing());
            cfg.put("status", tsuTrigger.getState());
            return TriggerBuilder.create().withId(Integer.toString(triggerId++))
                    .withTypeUID("core.ThingStatusUpdateTrigger").withConfiguration(cfg).build();
        } else if (t instanceof ThingStateChangedEventTrigger) {
            ThingStateChangedEventTrigger tscTrigger = (ThingStateChangedEventTrigger) t;
            Configuration cfg = new Configuration();
            cfg.put("thingUID", tscTrigger.getThing());
            cfg.put("status", tscTrigger.getNewState());
            cfg.put("previousStatus", tscTrigger.getOldState());
            return TriggerBuilder.create().withId(Integer.toString(triggerId++))
                    .withTypeUID("core.ThingStatusChangeTrigger").withConfiguration(cfg).build();
        } else {
            logger.warn("Unknown trigger type '{}' - ignoring it.", t.getClass().getSimpleName());
            return null;
        }
    }

    private boolean isReady() {
        return markers.containsAll(
                Set.of(ITEMS_MODEL_NAME, THINGS_MODEL_NAME, RULES_MODEL_NAME, RulesRefresher.RULES_REFRESH));
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        markers.add(readyMarker.getIdentifier());
        if (isReady()) {
            for (String ruleFileName : modelRepository.getAllModelNamesOfType(RULES_MODEL_NAME)) {
                EObject model = modelRepository.getModel(ruleFileName);
                String ruleModelName = ruleFileName.substring(0, ruleFileName.indexOf("."));
                if (model instanceof RuleModel) {
                    RuleModel ruleModel = (RuleModel) model;
                    int index = 1;
                    for (org.openhab.core.model.rule.rules.Rule rule : ruleModel.getRules()) {
                        addRule(toRule(ruleModelName, rule, index));
                        xExpressions.put(ruleModelName + "-" + index, rule.getScript());
                        index++;
                    }
                    handleVarDeclarations(ruleModelName, ruleModel);
                }
            }
            modelRepository.addModelRepositoryChangeListener(this);
        }
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        markers.remove(readyMarker.getIdentifier());
    }
}
