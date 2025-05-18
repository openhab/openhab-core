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
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.io.dto.SerializationException;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.model.yaml.internal.config.YamlConfigDescriptionParameterDTO;
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
public class YamlRuleProvider extends AbstractYamlRuleProvider<Rule>
        implements RuleProvider, YamlModelListener<YamlRuleDTO> {

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
        return version >= 1;
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
        if ((s = ruleDto.label) != null) {
            ruleBuilder.withName(s);
        }
        if ((s = ruleDto.template) != null) {
            ruleBuilder.withTemplateUID(s);
        }
        if (ruleDto.templateState != null) {
            ruleBuilder.withTemplateState(ruleDto.templateState);
        }
        Set<String> tags = ruleDto.tags;
        if (tags != null) {
            ruleBuilder.withTags(tags);
        }
        if ((s = ruleDto.description) != null) {
            ruleBuilder.withDescription(s);
        }
        if (ruleDto.visibility != null) {
            ruleBuilder.withVisibility(ruleDto.visibility);
        }
        Map<String, Object> configuration = ruleDto.config;
        if (configuration != null) {
            ruleBuilder.withConfiguration(new Configuration(configuration));
        }
        List<YamlConfigDescriptionParameterDTO> configDescriptionDtos = ruleDto.configDescriptions;
        if (configDescriptionDtos != null) {
            ruleBuilder.withConfigurationDescriptions(
                    YamlConfigDescriptionParameterDTO.mapConfigDescriptions(configDescriptionDtos));
        }
        List<YamlModuleDTO> triggerDTOs = ruleDto.triggers;
        List<YamlConditionDTO> conditionDTOs = ruleDto.conditions;
        List<YamlActionDTO> actionDTOs = ruleDto.actions;
        List<Trigger> triggers = null;
        if (triggerDTOs != null) {
            try {
                triggers = mapModules(triggerDTOs, extractModuleIds(conditionDTOs, actionDTOs), Trigger.class);
            } catch (SerializationException e) {
                logger.error("Could not parse triggers for rule {}: {}", ruleDto.uid, e.getMessage());
                logger.trace("", e);
                return null;
            }
            ruleBuilder.withTriggers(triggers);
        }
        List<Condition> conditions = null;
        if (conditionDTOs != null) {
            try {
                conditions = mapModules(conditionDTOs, extractModuleIds(triggers, actionDTOs), Condition.class);
            } catch (SerializationException e) {
                logger.error("Could not parse conditions for rule {}: {}", ruleDto.uid, e.getMessage());
                logger.trace("", e);
                return null;
            }
            ruleBuilder.withConditions(conditions);
        }
        if (actionDTOs != null) {
            try {
                ruleBuilder.withActions(mapModules(actionDTOs, extractModuleIds(triggers, conditions), Action.class));
            } catch (SerializationException e) {
                logger.error("Could not parse actions for rule {}: {}", ruleDto.uid, e.getMessage());
                logger.trace("", e);
                return null;
            }
        }

        return ruleBuilder.build();
    }
}
