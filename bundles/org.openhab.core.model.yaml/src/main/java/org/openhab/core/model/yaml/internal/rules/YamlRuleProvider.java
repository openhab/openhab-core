/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleProvider;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.model.yaml.YamlModelListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link YamlRuleProvider} is an OSGi service, that allows definition of rules in YAML configuration files. Files can
 * be added, updated or removed at runtime. The rules are automatically registered with
 * {@link org.openhab.core.automation.RuleRegistry}.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { RuleProvider.class, YamlRuleProvider.class, YamlModelListener.class })
public class YamlRuleProvider extends AbstractProvider<Rule> implements RuleProvider, YamlModelListener<YamlRuleDTO> { // TODO:
                                                                                                                       // (Nad)
                                                                                                                       // Cleanup
                                                                                                                       // +
                                                                                                                       // JavaDocs

    private final Logger logger = LoggerFactory.getLogger(YamlRuleProvider.class);

    private final Map<String, Collection<Rule>> rulesMap = new ConcurrentHashMap<>();

    @Activate
    public YamlRuleProvider() {
    }

    @Deactivate
    public void deactivate() {
        rulesMap.clear();
    }

    @Override
    public Collection<Rule> getAll() {
        return rulesMap.values().stream().flatMap(list -> list.stream()).toList();
    }

    @Override
    public Class<YamlRuleDTO> getElementClass() {
        return YamlRuleDTO.class;
    }

    @Override
    public boolean isVersionSupported(int version) {
        return version >= 2;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public void addedModel(String modelName, Collection<YamlRuleDTO> elements) {
        List<Rule> added = elements.stream().map(this::mapRule).filter(Objects::nonNull).toList();
        Collection<Rule> modelRules = Objects
                .requireNonNull(rulesMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        modelRules.addAll(added);
        added.forEach(r -> {
            logger.debug("model {} added rule {}", modelName, r.getUID());
            notifyListenersAboutAddedElement(r);
        });
    }

    @Override
    public void updatedModel(String modelName, Collection<YamlRuleDTO> elements) {
        List<Rule> updated = elements.stream().map(this::mapRule).filter(Objects::nonNull).toList();
        Collection<Rule> modelRules = Objects
                .requireNonNull(rulesMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        updated.forEach(r -> {
            modelRules.stream().filter(rule -> rule.getUID().equals(r.getUID())).findFirst()
                    .ifPresentOrElse(oldRule -> {
                        modelRules.remove(oldRule);
                        modelRules.add(r);
                        logger.debug("model {} updated rule {}", modelName, r.getUID());
                        notifyListenersAboutUpdatedElement(oldRule, r);
                    }, () -> {
                        modelRules.add(r);
                        logger.debug("model {} added rule {}", modelName, r.getUID());
                        notifyListenersAboutAddedElement(r);
                    });
        });
    }

    @Override
    public void removedModel(String modelName, Collection<YamlRuleDTO> elements) {
        List<Rule> removed = elements.stream().map(this::mapRule).filter(Objects::nonNull).toList();
        Collection<Rule> modelRules = rulesMap.getOrDefault(modelName, List.of());
        removed.forEach(r -> {
            modelRules.stream().filter(rule -> rule.getUID().equals(r.getUID())).findFirst()
                    .ifPresentOrElse(oldRule -> {
                        modelRules.remove(oldRule);
                        logger.debug("model {} removed rule {}", modelName, r.getUID());
                        notifyListenersAboutRemovedElement(oldRule);
                    }, () -> logger.debug("model {} rule {} not found", modelName, r.getUID()));
        });
        if (modelRules.isEmpty()) {
            rulesMap.remove(modelName);
        }
    }

    private @Nullable Rule mapRule(YamlRuleDTO ruleDto) {

        String s;
        RuleBuilder ruleBuilder = RuleBuilder.create(ruleDto.uid);
        if ((s = ruleDto.name) != null) {
            ruleBuilder.withName(s);
        }
        if ((s = ruleDto.templateUID) != null) {
            ruleBuilder.withTemplateUID(s);
        }
        Set<String> tags = ruleDto.tags;
        if (tags != null) {
            ruleBuilder.withTags(tags);
        }
        if ((s = ruleDto.description) != null) {
            ruleBuilder.withDescription(s);
        }
        Visibility visibility = ruleDto.visibility;
        if (visibility != null) {
            ruleBuilder.withVisibility(visibility);
        }
        Map<String, Object> configuration = ruleDto.config;
        if (configuration != null) {
            ruleBuilder.withConfiguration(new Configuration(configuration));
        }
        List<ConfigDescriptionParameter> configurationDescriptions = ruleDto.configurationDescriptions;
        if (configurationDescriptions != null) {
            ruleBuilder.withConfigurationDescriptions(configurationDescriptions);
        }
        List<YamlConditionDTO> conditionDTOs = ruleDto.conditions;
        if (conditionDTOs != null) {
            List<Condition> conditions = new ArrayList<>(conditionDTOs.size());
            for (YamlConditionDTO conditionDto : conditionDTOs) {
                conditions.add(ModuleBuilder.createCondition().withId(conditionDto.id).withTypeUID(conditionDto.type)
                        .withConfiguration(new Configuration(conditionDto.config)).withInputs(conditionDto.inputs)
                        .withLabel(conditionDto.label).withDescription(conditionDto.description).build());
            }
            ruleBuilder.withConditions(conditions);
        }
        List<YamlActionDTO> actionDTOs = ruleDto.actions;
        if (actionDTOs != null) {
            List<Action> actions = new ArrayList<>(actionDTOs.size());
            for (YamlActionDTO actionDto : actionDTOs) {
                actions.add(ModuleBuilder.createAction().withId(actionDto.id).withTypeUID(actionDto.type)
                        .withConfiguration(new Configuration(actionDto.config)).withInputs(actionDto.inputs)
                        .withLabel(actionDto.label).withDescription(actionDto.description).build());
            }
            ruleBuilder.withActions(actions);
        }
        List<YamlModuleDTO> triggerDTOs = ruleDto.triggers;
        if (triggerDTOs != null) {
            List<Trigger> triggers = new ArrayList<>(triggerDTOs.size());
            for (YamlModuleDTO triggerDto : triggerDTOs) {
                triggers.add(ModuleBuilder.createTrigger().withId(triggerDto.id).withTypeUID(triggerDto.type)
                        .withConfiguration(new Configuration(triggerDto.config)).withLabel(triggerDto.label)
                        .withDescription(triggerDto.description).build());
            }
            ruleBuilder.withTriggers(triggers);
        }

        return ruleBuilder.build();
    }
}
