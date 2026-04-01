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
import org.openhab.core.model.rule.rules.DateTimeTrigger;
import org.openhab.core.model.rule.rules.EventEmittedTrigger;
import org.openhab.core.model.rule.rules.EventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberChangedEventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberCommandEventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberUpdateEventTrigger;
import org.openhab.core.model.rule.rules.RuleModel;
import org.openhab.core.model.rule.rules.SystemOnShutdownTrigger;
import org.openhab.core.model.rule.rules.SystemOnStartupTrigger;
import org.openhab.core.model.rule.rules.SystemStartlevelTrigger;
import org.openhab.core.model.rule.rules.ThingStateChangedEventTrigger;
import org.openhab.core.model.rule.rules.ThingStateUpdateEventTrigger;
import org.openhab.core.model.rule.rules.TimerTrigger;
import org.openhab.core.model.rule.rules.UpdateEventTrigger;
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
 * No rule conditions are used as this concept does not exist for DSL rules.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Laurent Garnier - Add optional rule UID + rules stored in a map per model
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
                        notifyProviderChangeListeners(calcChanges(oldRules, newRules));
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
                        notifyProviderChangeListeners(calcChanges(oldRules, null));
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
                        notifyProviderChangeListeners(calcChanges(oldRules, newRules));
                    }
                    break;
                case REMOVED:
                    oldRules = rulesMap.remove(modelFileName);
                    if (!isIsolatedModel(modelFileName)) {
                        notifyProviderChangeListeners(calcChanges(oldRules, null));
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

    private Changes calcChanges(@Nullable List<Rule> oldRules, @Nullable List<Rule> newRules) {
        if (newRules == null || newRules.isEmpty()) {
            return new Changes(List.of(), List.of(), oldRules == null ? List.of() : List.copyOf(oldRules));
        }
        if (oldRules == null || oldRules.isEmpty()) {
            return new Changes(List.copyOf(newRules), List.of(), List.of());
        }
        List<Rule> oldMutable = new ArrayList<>(oldRules);
        List<Rule> newMutable = new ArrayList<>(newRules);
        List<RulePair> modified = new ArrayList<>();
        Rule oldRule, newRule;
        String uid;
        boolean found;
        for (Iterator<Rule> iterator = oldMutable.iterator(); iterator.hasNext();) {
            oldRule = iterator.next();
            found = false;
            uid = oldRule.getUID();
            for (Iterator<Rule> newIterator = newMutable.iterator(); newIterator.hasNext() && !found;) {
                newRule = newIterator.next();
                if (uid.equals(newRule.getUID())) {
                    modified.add(new RulePair(oldRule, newRule));
                    newIterator.remove();
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
        cfg.put("script", removeIndentation(scriptText));
        cfg.put("type", MIMETYPE_OPENHAB_DSL_RULE);
        List<Action> actions = List.of(ActionBuilder.create().withId("script").withTypeUID("script.ScriptAction")
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
        List<Trigger> triggers = new ArrayList<>();
        for (EventTrigger t : rule.getEventtrigger()) {
            Trigger trigger = mapTrigger(t);
            if (trigger != null) {
                triggers.add(trigger);
            }
        }

        // Create Action
        String context = DSLScriptContextProvider.CONTEXT_IDENTIFIER + modelName + "-" + index + "\n";
        XExpression expression = rule.getScript();
        String script = NodeModelUtils.findActualNodeFor(expression).getText();
        Configuration cfg = new Configuration();
        cfg.put("script", context + removeIndentation(script));
        cfg.put("type", MIMETYPE_OPENHAB_DSL_RULE);
        List<Action> actions = List.of(ActionBuilder.create().withId("script").withTypeUID("script.ScriptAction")
                .withConfiguration(cfg).build());

        List<String> ruleTags = rule.getTags();
        Set<String> tags = ruleTags == null ? Set.of() : Set.copyOf(ruleTags);

        return RuleBuilder.create(uid).withTags(tags).withName(name).withTriggers(triggers).withActions(actions)
                .build();
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

    private @Nullable Trigger mapTrigger(EventTrigger t) {
        if (t instanceof SystemOnStartupTrigger) {
            Configuration cfg = new Configuration();
            cfg.put("startlevel", 40);
            return TriggerBuilder.create().withId(Integer.toString(triggerId++))
                    .withTypeUID("core.SystemStartlevelTrigger").withConfiguration(cfg).build();
        } else if (t instanceof SystemStartlevelTrigger slTrigger) {
            Configuration cfg = new Configuration();
            cfg.put("startlevel", slTrigger.getLevel());
            return TriggerBuilder.create().withId(Integer.toString(triggerId++))
                    .withTypeUID("core.SystemStartlevelTrigger").withConfiguration(cfg).build();
        } else if (t instanceof SystemOnShutdownTrigger) {
            logger.warn("System shutdown rule triggers are no longer supported!");
            return null;
        } else if (t instanceof CommandEventTrigger ceTrigger) {
            Configuration cfg = new Configuration();
            cfg.put("itemName", ceTrigger.getItem());
            if (ceTrigger.getCommand() != null) {
                cfg.put("command", ceTrigger.getCommand().getValue());
            }
            return TriggerBuilder.create().withId(Integer.toString(triggerId++)).withTypeUID("core.ItemCommandTrigger")
                    .withConfiguration(cfg).build();
        } else if (t instanceof GroupMemberCommandEventTrigger ceTrigger) {
            Configuration cfg = new Configuration();
            cfg.put("groupName", ceTrigger.getGroup());
            if (ceTrigger.getCommand() != null) {
                cfg.put("command", ceTrigger.getCommand().getValue());
            }
            return TriggerBuilder.create().withId(Integer.toString(triggerId++)).withTypeUID("core.GroupCommandTrigger")
                    .withConfiguration(cfg).build();
        } else if (t instanceof UpdateEventTrigger ueTrigger) {
            Configuration cfg = new Configuration();
            cfg.put("itemName", ueTrigger.getItem());
            if (ueTrigger.getState() != null) {
                cfg.put("state", ueTrigger.getState().getValue());
            }
            return TriggerBuilder.create().withId(Integer.toString(triggerId++))
                    .withTypeUID("core.ItemStateUpdateTrigger").withConfiguration(cfg).build();
        } else if (t instanceof GroupMemberUpdateEventTrigger ueTrigger) {
            Configuration cfg = new Configuration();
            cfg.put("groupName", ueTrigger.getGroup());
            if (ueTrigger.getState() != null) {
                cfg.put("state", ueTrigger.getState().getValue());
            }
            return TriggerBuilder.create().withId(Integer.toString(triggerId++))
                    .withTypeUID("core.GroupStateUpdateTrigger").withConfiguration(cfg).build();
        } else if (t instanceof ChangedEventTrigger ceTrigger) {
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
        } else if (t instanceof GroupMemberChangedEventTrigger ceTrigger) {
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
        } else if (t instanceof TimerTrigger tt) {
            String triggerType;
            Configuration cfg = new Configuration();
            if (tt.getCron() != null) {
                triggerType = "timer.GenericCronTrigger";
                cfg.put("cronExpression", tt.getCron());
            } else {
                triggerType = "timer.TimeOfDayTrigger";
                String id = tt.getTime();
                if ("noon".equals(id)) {
                    cfg.put("time", "12:00");
                } else if ("midnight".equals(id)) {
                    cfg.put("time", "00:00");
                } else {
                    cfg.put("time", id);
                }
            }
            return TriggerBuilder.create().withId(Integer.toString(triggerId++)).withTypeUID(triggerType)
                    .withConfiguration(cfg).build();
        } else if (t instanceof DateTimeTrigger tt) {
            Configuration cfg = new Configuration();
            cfg.put("itemName", tt.getItem());
            cfg.put("timeOnly", tt.isTimeOnly());
            cfg.put("offset", tt.getOffset());
            return TriggerBuilder.create().withId(Integer.toString((triggerId++))).withTypeUID("timer.DateTimeTrigger")
                    .withConfiguration(cfg).build();
        } else if (t instanceof EventEmittedTrigger eeTrigger) {
            Configuration cfg = new Configuration();
            cfg.put("channelUID", eeTrigger.getChannel());
            if (eeTrigger.getTrigger() != null) {
                cfg.put("event", eeTrigger.getTrigger().getValue());
            }
            return TriggerBuilder.create().withId(Integer.toString(triggerId++)).withTypeUID("core.ChannelEventTrigger")
                    .withConfiguration(cfg).build();
        } else if (t instanceof ThingStateUpdateEventTrigger tsuTrigger) {
            Configuration cfg = new Configuration();
            cfg.put("thingUID", tsuTrigger.getThing());
            cfg.put("status", tsuTrigger.getState());
            return TriggerBuilder.create().withId(Integer.toString(triggerId++))
                    .withTypeUID("core.ThingStatusUpdateTrigger").withConfiguration(cfg).build();
        } else if (t instanceof ThingStateChangedEventTrigger tscTrigger) {
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
                    notifyProviderChangeListeners(calcChanges(oldRules, newRules));
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
