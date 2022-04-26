/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
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
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.ScriptEngineManager;
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
import org.openhab.core.model.rule.rules.SystemStartlevelTrigger;
import org.openhab.core.model.rule.rules.ThingStateChangedEventTrigger;
import org.openhab.core.model.rule.rules.ThingStateUpdateEventTrigger;
import org.openhab.core.model.rule.rules.TimerTrigger;
import org.openhab.core.model.rule.rules.DateTimeTrigger;
import org.openhab.core.model.rule.rules.UpdateEventTrigger;
import org.openhab.core.model.script.runtime.DSLScriptContextProvider;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.ReadyService.ReadyTracker;
import org.openhab.core.service.StartLevelService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;

/**
 * This RuleProvider provides rules that are defined in DSL rule files.
 * All rules consist out of a list of triggers and a single script action.
 * No rule conditions are used as this concept does not exist for DSL rules.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { DSLRuleProvider.class, RuleProvider.class,
        DSLScriptContextProvider.class })
public class DSLRuleProvider
        implements RuleProvider, ModelRepositoryChangeListener, DSLScriptContextProvider, ReadyTracker {

    public static final String SCRIPT_LOADED_RULE_NAME = "scriptLoaded";
    public static final String SCRIPT_UNLOADED_RULE_NAME = "scriptUnloaded";
    static final String MIMETYPE_OPENHAB_DSL_RULE = "application/vnd.openhab.dsl.rule";

    private final Logger logger = LoggerFactory.getLogger(DSLRuleProvider.class);
    private final Collection<ProviderChangeListener<Rule>> listeners = new ArrayList<>();
    private final Map<String, Rule> rules = new ConcurrentHashMap<>();

    private final Map<String, String> loadScripts = new ConcurrentHashMap<>();
    private final Map<String, String> unloadScripts = new ConcurrentHashMap<>();
    private final Map<String, IEvaluationContext> contexts = new ConcurrentHashMap<>();
    private final Map<String, XExpression> xExpressions = new ConcurrentHashMap<>();
    private static final ReadyMarker PROVIDER_READY_MARKER = new ReadyMarker("rules", "dslprovider");
    private static final ReadyMarker REFRESH_READY_MARKER = new ReadyMarker(RulesRefresher.RULES_REFRESH_MARKER_TYPE, RulesRefresher.RULES_REFRESH);
   private static final ReadyMarker SHUTDOWN_READY_MARKER = new ReadyMarker("openhab.xmlConfig", "org.eclipse.osgi");
    private final ScriptEngineManager scriptEngineManager;
    private final StartLevelService startLevelService;
    private int triggerId = 0;

    private final ModelRepository modelRepository;
    private final ReadyService readyService;

    @Activate
    public DSLRuleProvider(@Reference ModelRepository modelRepository, @Reference ReadyService readyService,
            @Reference ScriptEngineManager scriptEngineManager, @Reference StartLevelService startLevelService) {
        this.modelRepository = modelRepository;
        this.readyService = readyService;
        this.scriptEngineManager = scriptEngineManager;
        this.startLevelService = startLevelService;
    }

    @Activate
    protected void activate() {
        readyService.registerTracker(this);
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
            switch (type) {
                case ADDED:
                    EObject model = modelRepository.getModel(modelFileName);
                    addRulesFromModel(ruleModelName, model);
                    break;
                case MODIFIED:
                    removeRuleModel(ruleModelName);
                    EObject modifiedModel = modelRepository.getModel(modelFileName);
                    addRulesFromModel(ruleModelName, modifiedModel);
                    break;
                case REMOVING:
                    String unloadScript = unloadScripts.remove(ruleModelName);
                    executeGlobalScript(unloadScript, ruleModelName, SCRIPT_UNLOADED_RULE_NAME);
                    break;
                case REMOVED:
                    removeRuleModel(ruleModelName);
                    break;
                default:
                    logger.debug("Unknown event type.");
            }
        }
    }

    private void addRulesFromModel(String ruleModelName, @Nullable EObject model) {
        if (model instanceof RuleModel) {
            RuleModel ruleModel = (RuleModel) model;
            int index = 1;
            String scriptLoadedScript = null;
            for (org.openhab.core.model.rule.rules.Rule rule : ruleModel.getRules()) {
                if (SCRIPT_LOADED_RULE_NAME.equals(rule.getName())) {
                    scriptLoadedScript = createGlobalScript(rule.getScript(), ruleModelName,
                            SCRIPT_LOADED_RULE_NAME);
                } else if (SCRIPT_UNLOADED_RULE_NAME.equals(rule.getName())) {
                    unloadScripts.put(ruleModelName,
                            createGlobalScript(rule.getScript(), ruleModelName, SCRIPT_UNLOADED_RULE_NAME));
                } else {
                    addRule(toRule(ruleModelName, rule, index));
                    xExpressions.put(ruleModelName + "-" + index, rule.getScript());
                    index++;
                }
            }
            handleVarDeclarations(ruleModelName, ruleModel);
            if (startLevelService.getStartLevel() >= StartLevelService.STARTLEVEL_RULES) {
                executeGlobalScript(scriptLoadedScript, ruleModelName, SCRIPT_LOADED_RULE_NAME);
            } else if (scriptLoadedScript != null) {
                loadScripts.put(ruleModelName, scriptLoadedScript);
            }
        }
    }

    private String createGlobalScript(XBlockExpression script, String ruleModelName, String name) {
        String context = DSLScriptContextProvider.CONTEXT_IDENTIFIER + ruleModelName + "-" + name + "\n";
        String scriptString = NodeModelUtils.findActualNodeFor(script).getText();
        xExpressions.put(ruleModelName + "-" + name, script);
        return removeIndentation(context + scriptString);
    }

    private void executeGlobalScript(@Nullable String script, String ruleModelName, String name) {
        if (script != null) {
            ScriptEngineContainer container = scriptEngineManager.createScriptEngine(MIMETYPE_OPENHAB_DSL_RULE,
                    UUID.randomUUID().toString());
            if (container != null) {
                try {
                    container.getScriptEngine().eval(script);
                } catch (ScriptException e) {
                    logger.warn("Could not execute '{}' for model '{}'", name, ruleModelName, e);
                }
            } else {
                logger.warn("Could not create script engine when trying to execute '{}' for model '{}'", name,
                        ruleModelName);
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

    private void addRule(Rule newRule) {
            rules.put(newRule.getUID(), newRule);
            for (ProviderChangeListener<Rule> providerChangeListener : listeners) {
                providerChangeListener.added(this, newRule);
            }
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
        xExpressions.entrySet().removeIf(entry -> belongsToModel(entry.getKey(), modelName));
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

    private Rule toRule(String modelName, org.openhab.core.model.rule.rules.Rule rule, int index) {
        String name = rule.getName();
        String uid = modelName + "-" + index;

        // Create Triggers
        triggerId = 0;
        List<Trigger> triggers = new ArrayList<>();
        EList<EventTrigger> triggerList = rule.getEventtrigger();
        if   (triggerList.isEmpty()) {
            logger.warn("Rule '{}' in model '{}' has no triggers!", name, modelName);
        } else {
            for (EventTrigger t : triggerList) {
                Trigger trigger = mapTrigger(t);
                if (trigger != null) {
                    triggers.add(trigger);
                }
            }
        }

        // Create Action
        String context = DSLScriptContextProvider.CONTEXT_IDENTIFIER + modelName + "-" + index + "\n";
        XBlockExpression expression = rule.getScript();
        String script = NodeModelUtils.findActualNodeFor(expression).getText();
        Configuration cfg = new Configuration();
        cfg.put("script", context + removeIndentation(script));
        cfg.put("type", MIMETYPE_OPENHAB_DSL_RULE);
        List<Action> actions = List.of(
                ActionBuilder.create().withId("script").withTypeUID("script.ScriptAction").withConfiguration(cfg)
                        .build());

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
        return s.lines().map(line -> line.startsWith(indentation) ? line.substring(indentation.length()) : line)
                .collect(Collectors.joining("\n"));
    }

    private @Nullable Trigger mapTrigger(EventTrigger t) {
        if (t instanceof SystemOnStartupTrigger) {
            Configuration cfg = new Configuration();
            cfg.put("startlevel", 20);
            return TriggerBuilder.create().withId(Integer.toString(triggerId++))
                    .withTypeUID("core.SystemStartlevelTrigger").withConfiguration(cfg).build();
        } else if (t instanceof SystemStartlevelTrigger) {
            SystemStartlevelTrigger slTrigger = (SystemStartlevelTrigger) t;
            Configuration cfg = new Configuration();
            cfg.put("startlevel", slTrigger.getLevel());
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
                cfg.put("cronExpression", id);
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
        } else if (t instanceof DateTimeTrigger) {
            DateTimeTrigger tt = (DateTimeTrigger) t;
            Configuration cfg = new Configuration();
            cfg.put("itemName", tt.getItem());
            cfg.put("timeOnly", tt.isTimeOnly());
            return TriggerBuilder.create().withId(Integer.toString((triggerId++))).withTypeUID("timer.DateTimeTrigger")
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

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        if (REFRESH_READY_MARKER.equals(readyMarker)) {
            for (String ruleFileName : modelRepository.getAllModelNamesOfType("rules")) {
                EObject model = modelRepository.getModel(ruleFileName);
                String ruleModelName = ruleFileName.substring(0, ruleFileName.indexOf("."));
                String unloadScript = unloadScripts.remove(ruleModelName);
                executeGlobalScript(unloadScript, ruleModelName, SCRIPT_UNLOADED_RULE_NAME);
                removeRuleModel(ruleModelName);
                addRulesFromModel(ruleModelName, model);
            }
            modelRepository.addModelRepositoryChangeListener(this);
            readyService.markReady(PROVIDER_READY_MARKER);
        } else if (StartLevelService.STARTLEVEL_MARKER_TYPE.equals(readyMarker.getType()) && startLevelService.getStartLevel() == 40) {
            for (String ruleModelName : loadScripts.keySet()) {
                String loadScript = loadScripts.remove(ruleModelName);
                executeGlobalScript(loadScript, ruleModelName, SCRIPT_LOADED_RULE_NAME);
            }
        }
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        if (SHUTDOWN_READY_MARKER.equals(readyMarker)) {
            for (String ruleModelName : unloadScripts.keySet()) {
                String unloadScript = unloadScripts.remove(ruleModelName);
                executeGlobalScript(unloadScript, ruleModelName, SCRIPT_UNLOADED_RULE_NAME);
            }
            } else if (REFRESH_READY_MARKER.equals(readyMarker)) {
            readyService.unmarkReady(PROVIDER_READY_MARKER);
        }
    }
}
