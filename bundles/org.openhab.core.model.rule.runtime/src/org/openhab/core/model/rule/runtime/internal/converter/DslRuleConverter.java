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
package org.openhab.core.model.rule.runtime.internal.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XStringLiteral;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.converter.RuleParser;
import org.openhab.core.automation.converter.RuleSerializer;
import org.openhab.core.automation.internal.module.handler.ChannelEventTriggerHandler;
import org.openhab.core.automation.internal.module.handler.DateTimeTriggerHandler;
import org.openhab.core.automation.internal.module.handler.GenericCronTriggerHandler;
import org.openhab.core.automation.internal.module.handler.GroupCommandTriggerHandler;
import org.openhab.core.automation.internal.module.handler.GroupStateTriggerHandler;
import org.openhab.core.automation.internal.module.handler.ItemCommandTriggerHandler;
import org.openhab.core.automation.internal.module.handler.ItemStateTriggerHandler;
import org.openhab.core.automation.internal.module.handler.SystemTriggerHandler;
import org.openhab.core.automation.internal.module.handler.ThingStatusTriggerHandler;
import org.openhab.core.automation.internal.module.handler.TimeOfDayTriggerHandler;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleRule;
import org.openhab.core.automation.util.ActionBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.converter.SerializabilityResult;
import org.openhab.core.io.dto.SerializationException;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.rule.rules.ChangedEventTrigger;
import org.openhab.core.model.rule.rules.CommandEventTrigger;
import org.openhab.core.model.rule.rules.DateTimeTrigger;
import org.openhab.core.model.rule.rules.EventEmittedTrigger;
import org.openhab.core.model.rule.rules.EventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberChangedEventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberCommandEventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberUpdateEventTrigger;
import org.openhab.core.model.rule.rules.RuleModel;
import org.openhab.core.model.rule.rules.RulesFactory;
import org.openhab.core.model.rule.rules.SystemStartlevelTrigger;
import org.openhab.core.model.rule.rules.ThingStateChangedEventTrigger;
import org.openhab.core.model.rule.rules.ThingStateUpdateEventTrigger;
import org.openhab.core.model.rule.rules.TimerTrigger;
import org.openhab.core.model.rule.rules.UpdateEventTrigger;
import org.openhab.core.model.rule.rules.ValidCommand;
import org.openhab.core.model.rule.rules.ValidState;
import org.openhab.core.model.rule.rules.ValidTrigger;
import org.openhab.core.model.rule.runtime.internal.DSLRuleProvider;
import org.openhab.core.model.script.scoping.StateAndCommandProvider;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DslRuleFileConverter} is the DSL converter for {@link Rule} objects, which can parse and generate Rule DSL
 * syntax.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { RuleSerializer.class, RuleParser.class })
public class DslRuleConverter implements RuleSerializer, RuleParser {

    private static final String SCRIPT_PLACEHOLDER_PREFIX = "SCRIPT_PLACEHOLDER_";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "(?<=then\\R)^\\s*val\\splaceholder=\"SCRIPT_PLACEHOLDER_(?<uid>[^\"]+)\"\\s*$\\R*", Pattern.MULTILINE);
    private static final Pattern CONTEXT_COMMENT_PATTERN = Pattern.compile("^// context:.*$\\R", Pattern.MULTILINE);
    private static final Pattern INDENTATION_PATTERN = Pattern.compile("^(?=.)", Pattern.MULTILINE);
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern INDEX_PATTERN = Pattern.compile("-(?<idx>\\d+)$");
    private final Set<String> enumStates;
    private final Set<String> enumCommands;

    private final Logger logger = LoggerFactory.getLogger(DslRuleConverter.class);

    private final ModelRepository modelRepository;
    private final DSLRuleProvider ruleProvider;

    private record ScriptElement(String placeholderLiteral, String scriptContent) {
    }

    private final Map<String, RuleModel> elementsToGenerate = new ConcurrentHashMap<>();

    private final Map<String, List<ScriptElement>> scriptElements = new ConcurrentHashMap<>();

    @Activate
    public DslRuleConverter(@Reference ModelRepository modelRepository, @Reference DSLRuleProvider ruleProvider) {
        this.modelRepository = modelRepository;
        this.ruleProvider = ruleProvider;

        Set<String> enums = new LinkedHashSet<>();
        for (State state : StateAndCommandProvider.getAllStates()) {
            enums.add(state.toString());
        }
        this.enumStates = Set.copyOf(enums);

        enums = new LinkedHashSet<>();
        for (Command command : StateAndCommandProvider.getAllCommands()) {
            enums.add(command.toString());
        }
        this.enumCommands = Set.copyOf(enums);
    }

    @Override
    public @NonNull String getParserFormat() {
        return "DSL";
    }

    @Override
    public String getGeneratedFormat() {
        return "DSL";
    }

    @Override
    public List<SerializabilityResult<String>> checkSerializability(Collection<Rule> rules) {
        List<SerializabilityResult<String>> result = new ArrayList<>(rules.size());
        List<String> errors = new ArrayList<>();
        String s;
        for (Rule rule : rules) {
            if (rule instanceof SimpleRule) {
                result.add(new SerializabilityResult<>(rule.getUID(), false,
                        "Rule '" + rule.getUID() + "' is a SimpleRule with an inaccessible action."));
                continue;
            }
            errors.clear();
            if (rule.getVisibility() != Visibility.VISIBLE) {
                errors.add("isn't visible");
            }
            if ((s = rule.getDescription()) != null && !s.isBlank()) {
                errors.add("has a description");
            }
            List<Trigger> triggers = rule.getTriggers();
            if (triggers.isEmpty()) {
                errors.add("has no triggers");
            } else {
                for (Trigger trigger : triggers) {
                    try {
                        buildModelTrigger(trigger);
                    } catch (SerializationException e) {
                        errors.add("trigger '" + trigger.getId() + "': " + e.getMessage());
                    }
                }
            }

            if (!rule.getConditions().isEmpty()) {
                errors.add("has conditions");
            }
            if (rule.getActions().size() != 1) {
                errors.add("has " + rule.getActions().size() + " actions but exactly 1 is required");
            } else {
                Action action = rule.getActions().getFirst();
                if (action.getConfiguration().get("type") instanceof String type) {
                    if (DSLRuleProvider.MIMETYPE_OPENHAB_DSL_RULE.equals(type)) {
                        if (!(action.getConfiguration().get("script") instanceof String)) {
                            errors.add("has no action script");
                        }
                        if (action.getConfiguration().get(Module.SHARED_CONTEXT) instanceof Boolean shared
                                && shared.booleanValue()) {
                            errors.add("action '" + action.getId() + "' has shared context");
                        }
                    } else {
                        errors.add("doesn't have a scripted DSL action");
                    }
                } else {
                    errors.add("doesn't have a scripted action");
                }
            }

            if (errors.isEmpty()) {
                result.add(new SerializabilityResult<>(rule.getUID(), true, ""));
            } else {
                result.add(new SerializabilityResult<>(rule.getUID(), false,
                        "Rule '" + rule.getUID() + "': " + String.join(", ", errors) + '.'));
            }
        }

        return result;
    }

    @Override
    public void setRulesToBeSerialized(String modelName, List<Rule> rules, RuleSerializationOption option)
            throws SerializationException {
        if (rules.isEmpty()) {
            return;
        }
        if (option != RuleSerializationOption.NORMAL) {
            throw new SerializationException("DSL rules don't support serialization option '" + option + '\'');
        }
        List<String> errors = null;
        List<SerializabilityResult<String>> checks = checkSerializability(rules);
        for (SerializabilityResult<String> check : checks) {
            if (!check.ok()) {
                if (errors == null) {
                    errors = new ArrayList<>();
                }
                errors.add(check.failureReason());
            }
        }
        if (errors != null) {
            throw new SerializationException(
                    "Rule serialization attempt failed with:\n  " + String.join("\n  ", errors));
        }

        RuleModel model = RulesFactory.eINSTANCE.createRuleModel();

        // Ensure that the variable collection is not null, calling get() creates an empty collection.
        model.getVariables();

        Set<Rule> handledRules = new HashSet<>();
        for (Rule rule : rules) {
            if (handledRules.contains(rule)) {
                continue;
            }
            org.openhab.core.model.rule.rules.Rule modelRule = RulesFactory.eINSTANCE.createRule();
            model.getRules().add(modelRule);
            try {
                String placeholderUid = UUID.randomUUID().toString();
                String placeholderLiteral = SCRIPT_PLACEHOLDER_PREFIX + placeholderUid;
                buildModelRule(rule, modelRule, placeholderLiteral, handledRules);
                scriptElements.compute(modelName, (k, v) -> {
                    List<ScriptElement> r = v == null ? new ArrayList<>() : v;
                    if (rule.getActions().getFirst().getConfiguration().get("script") instanceof String script) {
                        r.add(new ScriptElement(placeholderUid, script));
                    } else {
                        r.add(new ScriptElement(placeholderUid, ""));
                    }
                    return r;
                });
            } catch (SerializationException e) {
                logger.warn("Failed to serialize rule '{}': {}", rule.getUID(), e.getMessage());
                throw new SerializationException("Rule '" + rule.getUID() + "': " + e.getMessage(), e);
            }
        }
        elementsToGenerate.put(modelName, model);
    }

    @Override
    public void generateFormat(String modelName, OutputStream out) {
        RuleModel model = elementsToGenerate.remove(modelName);
        if (model != null) {
            if (logger.isDebugEnabled()) {
                org.eclipse.emf.common.util.Diagnostic diagnostic = Diagnostician.INSTANCE.validate(model);
                if (diagnostic.getSeverity() != org.eclipse.emf.common.util.Diagnostic.OK) {
                    for (org.eclipse.emf.common.util.Diagnostic child : diagnostic.getChildren()) {
                        logger.warn("Model Validation Error: {}", child.getMessage());
                    }
                }
            }

            // Replace the placeholder with the actual script content
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            modelRepository.generateFileFormat(outputStream, "rules", model);
            String generated = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            Matcher m = PLACEHOLDER_PATTERN.matcher(generated);
            int safetyValve = 0;
            List<ScriptElement> elements;
            ScriptElement element;
            String scriptContent;
            while (m.find() && safetyValve < 1000) {
                elements = scriptElements.get(modelName);
                String uid = m.group("uid");
                if (uid != null && elements != null) {
                    element = elements.stream().filter(e -> uid.equals(e.placeholderLiteral)).findAny().orElse(null);
                    if (element != null) {
                        scriptContent = CONTEXT_COMMENT_PATTERN.matcher(element.scriptContent).replaceFirst("");
                        scriptContent = INDENTATION_PATTERN.matcher(scriptContent).replaceAll("\t");
                        if (!scriptContent.endsWith("\n")) {
                            scriptContent += '\n';
                        }
                        generated = m.replaceFirst(scriptContent);
                    }
                }
                m = PLACEHOLDER_PATTERN.matcher(generated);
                safetyValve++;
            }
            if (safetyValve >= 1000) {
                logger.warn(
                        "Aborted replacing placeholders with script content to avoid endless loop, generated Rule DSL for '{}' will be invalid",
                        modelName);
            }

            try {
                out.write(generated.getBytes());
            } catch (IOException e) {
                logger.warn("Exception when writing the generated syntax {}", e.getMessage());
            }
        }
    }

    @Override
    public @Nullable String startParsingFormat(String syntax, List<String> errors, List<String> warnings) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(syntax.getBytes());
        String result = modelRepository.createIsolatedModel("rules", inputStream, errors, warnings);
        return result;
    }

    @Override
    public @NonNull Collection<Rule> getParsedObjects(String modelName) {
        List<Rule> result = new ArrayList<>();
        RuleBuilder builder;
        List<Action> actions = new ArrayList<>();
        ActionBuilder aBuilder;
        LinkedHashMap<String, @Nullable Object> props;
        Set<String> usedUids = new HashSet<>();
        String strippedModeName = modelName.replace(".rules", "");
        String uid;
        for (Rule rule : ruleProvider.getAllFromModel(modelName)) {
            if ((uid = rule.getUID()).startsWith(strippedModeName) || usedUids.contains(uid)) {
                builder = RuleBuilder.create(generateUid(rule, usedUids), rule);
            } else {
                usedUids.add(uid);
                builder = RuleBuilder.create(uid, rule);
            }
            actions.clear();
            for (Action action : rule.getActions()) {
                aBuilder = ActionBuilder.create(action);
                props = new LinkedHashMap<>(action.getConfiguration().getProperties());
                if (props.get("script") instanceof String script) {
                    props.put("script", CONTEXT_COMMENT_PATTERN.matcher(script).replaceFirst(""));
                }
                aBuilder.withConfiguration(new Configuration(props));
                actions.add(aBuilder.build());
            }
            builder.withActions(actions);
            result.add(builder.build());
        }
        return result;
    }

    private String generateUid(Rule rule, Set<String> usedUids) {

        String result = rule.getName();
        if (result == null || result.isBlank()) {
            result = "generated-1";
        } else {
            result = result.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
        }

        Matcher matcher;
        while (usedUids.contains(result)) {
            matcher = INDEX_PATTERN.matcher(result);
            if (matcher.find()) {
                result = matcher.replaceFirst("-" + Integer.parseInt(matcher.group("idx")) + 1);
            } else {
                result += "-2";
            }
        }

        usedUids.add(result);
        return result;
    }

    @Override
    public void finishParsingFormat(String modelName) {
        modelRepository.removeModel(modelName);
        scriptElements.remove(modelName);
    }

    private org.openhab.core.model.rule.rules.Rule buildModelRule(Rule rule,
            org.openhab.core.model.rule.rules.Rule model, String placeholderLiteral, Set<Rule> handledRules)
            throws SerializationException {
        model.setUid(rule.getUID());
        model.setName(rule.getName());
        EList<String> tags = model.getTags();
        for (String tag : rule.getTags()) {
            tags.add(tag);
        }

        for (Trigger trigger : rule.getTriggers()) {
            model.getEventtrigger().add(buildModelTrigger(trigger));
        }

        model.setScript(createPlaceholder(placeholderLiteral));

        handledRules.add(rule);

        return model;
    }

    private EventTrigger buildModelTrigger(Trigger trigger) throws SerializationException {
        String type = trigger.getTypeUID();
        Object value;
        RulesFactory factory = RulesFactory.eINSTANCE;
        switch (type) {
            case SystemTriggerHandler.STARTLEVEL_MODULE_TYPE_ID:
                value = trigger.getConfiguration().get(SystemTriggerHandler.CFG_STARTLEVEL);
                if (value instanceof Number num) {
                    int level = num.intValue();
                    if (level == 40) {
                        return factory.createSystemOnStartupTrigger();
                    } else {
                        SystemStartlevelTrigger result = factory.createSystemStartlevelTrigger();
                        result.setLevel(level);
                        return result;
                    }
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger);
                }
            case ItemCommandTriggerHandler.MODULE_TYPE_ID:
                value = trigger.getConfiguration().get(ItemCommandTriggerHandler.CFG_ITEMNAME);
                if (value instanceof String str) {
                    CommandEventTrigger result = factory.createCommandEventTrigger();
                    result.setItem(str);
                    value = trigger.getConfiguration().get(ItemCommandTriggerHandler.CFG_COMMAND);
                    if (value instanceof String command) {
                        result.setCommand(createValidCommand(command));
                    }
                    return result;
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger);
                }
            case GroupCommandTriggerHandler.MODULE_TYPE_ID:
                value = trigger.getConfiguration().get(GroupCommandTriggerHandler.CFG_GROUPNAME);
                if (value instanceof String str) {
                    GroupMemberCommandEventTrigger result = factory.createGroupMemberCommandEventTrigger();
                    result.setGroup(str);
                    value = trigger.getConfiguration().get(GroupCommandTriggerHandler.CFG_COMMAND);
                    if (value instanceof String command) {
                        result.setCommand(createValidCommand(command));
                    }
                    return result;
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger);
                }
            case ItemStateTriggerHandler.UPDATE_MODULE_TYPE_ID:
                value = trigger.getConfiguration().get(ItemStateTriggerHandler.CFG_ITEMNAME);
                if (value instanceof String str) {
                    UpdateEventTrigger result = factory.createUpdateEventTrigger();
                    result.setItem(str);
                    value = trigger.getConfiguration().get(ItemStateTriggerHandler.CFG_STATE);
                    if (value instanceof String state) {
                        result.setState(createValidState(state));
                    }
                    return result;
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger);
                }
            case ItemStateTriggerHandler.CHANGE_MODULE_TYPE_ID:
                value = trigger.getConfiguration().get(ItemStateTriggerHandler.CFG_ITEMNAME);
                if (value instanceof String str) {
                    ChangedEventTrigger result = factory.createChangedEventTrigger();
                    result.setItem(str);
                    value = trigger.getConfiguration().get(ItemStateTriggerHandler.CFG_STATE);
                    if (value instanceof String state) {
                        result.setNewState(createValidState(state));
                    }
                    value = trigger.getConfiguration().get(ItemStateTriggerHandler.CFG_PREVIOUS_STATE);
                    if (value instanceof String state) {
                        result.setOldState(createValidState(state));
                    }
                    return result;
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger);
                }
            case GroupStateTriggerHandler.UPDATE_MODULE_TYPE_ID:
                value = trigger.getConfiguration().get(GroupStateTriggerHandler.CFG_GROUPNAME);
                if (value instanceof String str) {
                    GroupMemberUpdateEventTrigger result = factory.createGroupMemberUpdateEventTrigger();
                    result.setGroup(str);
                    value = trigger.getConfiguration().get(GroupStateTriggerHandler.CFG_STATE);
                    if (value instanceof String state) {
                        result.setState(createValidState(state));
                    }
                    return result;
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger);
                }
            case GroupStateTriggerHandler.CHANGE_MODULE_TYPE_ID:
                value = trigger.getConfiguration().get(GroupStateTriggerHandler.CFG_GROUPNAME);
                if (value instanceof String str) {
                    GroupMemberChangedEventTrigger result = factory.createGroupMemberChangedEventTrigger();
                    result.setGroup(str);
                    value = trigger.getConfiguration().get(GroupStateTriggerHandler.CFG_STATE);
                    if (value instanceof String state) {
                        result.setNewState(createValidState(state));
                    }
                    value = trigger.getConfiguration().get(GroupStateTriggerHandler.CFG_PREVIOUS_STATE);
                    if (value instanceof String state) {
                        result.setOldState(createValidState(state));
                    }
                    return result;
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger);
                }
            case GenericCronTriggerHandler.MODULE_TYPE_ID:
                value = trigger.getConfiguration().get(GenericCronTriggerHandler.CFG_CRON_EXPRESSION);
                if (value instanceof String str) {
                    TimerTrigger result = factory.createTimerTrigger();
                    if ("0 0 12 * * ?".equals(str)) {
                        result.setTime("noon");
                    } else if ("0 0 0 * * ?".equals(str)) {
                        result.setTime("midnight");
                    } else {
                        result.setCron(str);
                    }
                    return result;
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger);
                }
            case TimeOfDayTriggerHandler.MODULE_TYPE_ID:
                value = trigger.getConfiguration().get(TimeOfDayTriggerHandler.CFG_TIME);
                if (value instanceof String str) {
                    TimerTrigger result = factory.createTimerTrigger();
                    if ("12:00".equals(str)) {
                        result.setTime("noon");
                    } else if ("00:00".equals(str)) {
                        result.setTime("midnight");
                    } else {
                        result.setTime(str);
                    }
                    return result;
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger);
                }
            case DateTimeTriggerHandler.MODULE_TYPE_ID:
                value = trigger.getConfiguration().get(DateTimeTriggerHandler.CONFIG_ITEM_NAME);
                if (value instanceof String str) {
                    DateTimeTrigger result = factory.createDateTimeTrigger();
                    result.setItem(str);
                    value = trigger.getConfiguration().get(DateTimeTriggerHandler.CONFIG_TIME_ONLY);
                    if (value instanceof Boolean timeOnly) {
                        result.setTimeOnly(timeOnly);
                    }
                    value = trigger.getConfiguration().get(DateTimeTriggerHandler.CONFIG_OFFSET);
                    if (value instanceof String offset) {
                        result.setOffset(offset);
                        return result;
                    }
                }
                throw new SerializationException("Invalid trigger: " + trigger);
            case ChannelEventTriggerHandler.MODULE_TYPE_ID:
                value = trigger.getConfiguration().get(ChannelEventTriggerHandler.CFG_CHANNEL);
                if (value instanceof String str) {
                    EventEmittedTrigger result = factory.createEventEmittedTrigger();
                    result.setChannel(str);
                    value = trigger.getConfiguration().get(ChannelEventTriggerHandler.CFG_CHANNEL_EVENT);
                    if (value instanceof String event) {
                        ValidTrigger trg = factory.createValidTrigger();
                        trg.setValue(event);
                        result.setTrigger(trg);
                    }
                    return result;
                }
                throw new SerializationException("Invalid trigger: " + trigger);
            case ThingStatusTriggerHandler.UPDATE_MODULE_TYPE_ID:
                value = trigger.getConfiguration().get(ThingStatusTriggerHandler.CFG_THING_UID);
                if (value instanceof String str) {
                    ThingStateUpdateEventTrigger result = factory.createThingStateUpdateEventTrigger();
                    result.setThing(str);
                    value = trigger.getConfiguration().get(ThingStatusTriggerHandler.CFG_STATUS);
                    if (value instanceof String status) {
                        result.setState(status);
                        return result;
                    }
                }
                throw new SerializationException("Invalid trigger: " + trigger);
            case ThingStatusTriggerHandler.CHANGE_MODULE_TYPE_ID:
                value = trigger.getConfiguration().get(ThingStatusTriggerHandler.CFG_THING_UID);
                if (value instanceof String str) {
                    ThingStateChangedEventTrigger result = factory.createThingStateChangedEventTrigger();
                    result.setThing(str);
                    value = trigger.getConfiguration().get(ThingStatusTriggerHandler.CFG_STATUS);
                    if (value instanceof String status) {
                        result.setNewState(status);
                        value = trigger.getConfiguration().get(ThingStatusTriggerHandler.CFG_PREVIOUS_STATUS);
                        if (value instanceof String previousStatus) {
                            result.setOldState(previousStatus);
                            return result;
                        }
                    }
                }
                throw new SerializationException("Invalid trigger: " + trigger);
            default:
                throw new SerializationException("Unsupported trigger: " + trigger);
        }
    }

    private ValidState createValidState(String stateValue) {
        ValidState result;
        if (NUMERIC_PATTERN.matcher(stateValue).matches()) {
            result = RulesFactory.eINSTANCE.createValidStateNumber();
        } else if (enumStates.contains(stateValue)) {
            result = RulesFactory.eINSTANCE.createValidStateId();
        } else {
            result = RulesFactory.eINSTANCE.createValidStateString();
        }
        result.setValue(stateValue);
        return result;
    }

    private ValidCommand createValidCommand(String commandValue) {
        ValidCommand result;
        if (NUMERIC_PATTERN.matcher(commandValue).matches()) {
            result = RulesFactory.eINSTANCE.createValidCommandNumber();
        } else if (enumCommands.contains(commandValue)) {
            result = RulesFactory.eINSTANCE.createValidCommandId();
        } else {
            result = RulesFactory.eINSTANCE.createValidCommandString();
        }
        result.setValue(commandValue);
        return result;
    }

    private XBlockExpression createPlaceholder(String placeholderLiteral) {
        // Creates expression: 'val placeholder="<placeholderLiteral>"'
        XbaseFactory factory = XbaseFactory.eINSTANCE;
        XBlockExpression result = factory.createXBlockExpression();
        XVariableDeclaration varDecl = factory.createXVariableDeclaration();
        varDecl.setName("placeholder");
        XStringLiteral stringLit = factory.createXStringLiteral();
        stringLit.setValue(placeholderLiteral);
        varDecl.setRight(stringLit);
        result.getExpressions().add(varDecl);
        return result;
    }
}
