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

import org.apache.commons.lang3.SerializationException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
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
@NonNullByDefault // TODO: (Nad) Cleanup + JavaDocs
@Component(immediate = true, service = { RuleProvider.class, YamlRuleProvider.class, YamlModelListener.class })
public class YamlRuleProvider extends AbstractProvider<Rule> implements RuleProvider, YamlModelListener<YamlRuleDTO> {

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
        }); // TODO: (Nad) Remember to create tests
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
        if ((s = ruleDto.templateUid) != null) {
            ruleBuilder.withTemplateUID(s);
        }
        Set<String> tags = ruleDto.tags;
        if (tags != null) {
            ruleBuilder.withTags(tags);
        }
        if ((s = ruleDto.description) != null) {
            ruleBuilder.withDescription(s);
        }
        Visibility visibility = ruleDto.getVisibility();
        if (visibility != null) {
            ruleBuilder.withVisibility(visibility);
        }
        Map<String, Object> configuration = ruleDto.config;
        if (configuration != null) {
            ruleBuilder.withConfiguration(new Configuration(configuration));
        }
        List<ConfigDescriptionParameter> configurationDescriptions = ruleDto.configDescriptions;
        if (configurationDescriptions != null) {
            ruleBuilder.withConfigurationDescriptions(configurationDescriptions);
        }
        List<YamlActionDTO> actionDTOs = ruleDto.actions;
        if (actionDTOs != null) {
            ruleBuilder.withActions(mapModules(actionDTOs, Action.class));
        }
        List<YamlConditionDTO> conditionDTOs = ruleDto.conditions;
        if (conditionDTOs != null) {
            ruleBuilder.withConditions(mapModules(conditionDTOs, Condition.class));
        }
        List<YamlModuleDTO> triggerDTOs = ruleDto.triggers;
        if (triggerDTOs != null) {
            ruleBuilder.withTriggers(mapModules(triggerDTOs, Trigger.class));
        }

        return ruleBuilder.build();
    }

    @SuppressWarnings("unchecked")
    private <T extends Module, D extends YamlModuleDTO> List<T> mapModules(List<D> dtos, Class<T> targetClazz) {
        List<T> modules = new ArrayList<>(dtos.size());
        int id = 0;
        boolean generateIds = dtos.stream().anyMatch(m -> m.id == null || m.id.isBlank());
        while (generateIds) {
            for (;;) {
                String ids = Integer.toString(++id);
                if (!dtos.stream().anyMatch(m -> ids.equals(m.id))) {
                    break;
                }
            }
            final String ids2 = Integer.toString(id);
            dtos.stream().filter(m -> m.id == null || m.id.isBlank()).findFirst().ifPresent(m -> m.id = ids2);
            generateIds = dtos.stream().anyMatch(m -> m.id == null || m.id.isBlank());
        }

        for (D dto : dtos) {
            try {
                if (targetClazz.isAssignableFrom(Condition.class) && dto instanceof YamlConditionDTO cDto) {
                    modules.add((T) ModuleBuilder.createCondition().withId(dto.id).withTypeUID(dto.type)
                            .withConfiguration(new Configuration(dto.config)).withInputs(cDto.inputs)
                            .withLabel(dto.label).withDescription(dto.description).build());
                } else if (targetClazz.isAssignableFrom(Action.class) && dto instanceof YamlActionDTO aDto) {
                    modules.add((T) ModuleBuilder.createAction().withId(dto.id).withTypeUID(dto.type)
                            .withConfiguration(new Configuration(dto.config)).withInputs(aDto.inputs)
                            .withLabel(dto.label).withDescription(dto.description).build());
                } else if (targetClazz.isAssignableFrom(Trigger.class)) {
                    modules.add((T) ModuleBuilder.createTrigger().withId(dto.id).withTypeUID(dto.type)
                            .withConfiguration(new Configuration(dto.config)).withLabel(dto.label)
                            .withDescription(dto.description).build());
                } else {
                    throw new IllegalArgumentException("Invalid combination of target and dto classes: "
                            + targetClazz.getSimpleName() + " <-> " + dto.getClass().getSimpleName());
                }
            } catch (RuntimeException e) {
                throw new SerializationException("Failed to process YAML rule " + targetClazz.getSimpleName() + ": \""
                        + dto + "\": " + e.getMessage(), e);
            }
        }

        return modules;
    }
}
