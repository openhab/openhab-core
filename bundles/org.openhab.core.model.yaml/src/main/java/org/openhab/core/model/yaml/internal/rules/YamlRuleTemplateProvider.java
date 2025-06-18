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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.RuleTemplateProvider;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.model.yaml.internal.config.YamlConfigDescriptionParameterDTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link YamlRuleTemplateProvider} is an OSGi service, that allows definition of rule templates in YAML configuration
 * files. Files can
 * be added, updated or removed at runtime. The rule templates are automatically registered with
 * {@link org.openhab.core.automation.internal.template.RuleTemplateRegistry}.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { RuleTemplateProvider.class, YamlRuleTemplateProvider.class,
        YamlModelListener.class })
public class YamlRuleTemplateProvider extends AbstractYamlRuleProvider<RuleTemplate>
        implements RuleTemplateProvider, YamlModelListener<YamlRuleTemplateDTO> {

    private final Logger logger = LoggerFactory.getLogger(YamlRuleTemplateProvider.class);

    private final Map<String, Collection<RuleTemplate>> ruleTemplatesMap = new ConcurrentHashMap<>();

    @Activate
    public YamlRuleTemplateProvider() {
    }

    @Deactivate
    public void deactivate() {
        ruleTemplatesMap.clear();
    }

    @Override
    public Collection<RuleTemplate> getAll() {
        return getTemplates(null);
    }

    @Override
    public @Nullable RuleTemplate getTemplate(String uid, @Nullable Locale locale) {
        return ruleTemplatesMap.values().stream().flatMap(list -> list.stream()).filter(t -> uid.equals(t.getUID()))
                .findAny().orElse(null);
    }

    @Override
    public Collection<RuleTemplate> getTemplates(@Nullable Locale locale) {
        return ruleTemplatesMap.values().stream().flatMap(list -> list.stream()).toList();
    }

    @Override
    public Class<YamlRuleTemplateDTO> getElementClass() {
        return YamlRuleTemplateDTO.class;
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
    public void addedModel(String modelName, Collection<YamlRuleTemplateDTO> elements) {
        List<RuleTemplate> added = elements.stream().map(this::mapRuleTemplate).filter(Objects::nonNull).toList();
        Collection<RuleTemplate> modelRuleTemplates = Objects
                .requireNonNull(ruleTemplatesMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        modelRuleTemplates.addAll(added);
        added.forEach(t -> {
            logger.debug("model {} added rule template {}", modelName, t.getUID());
            notifyListenersAboutAddedElement(t);
        });
    }

    @Override
    public void updatedModel(String modelName, Collection<YamlRuleTemplateDTO> elements) {
        List<RuleTemplate> updated = elements.stream().map(this::mapRuleTemplate).filter(Objects::nonNull).toList();
        Collection<RuleTemplate> modelRules = Objects
                .requireNonNull(ruleTemplatesMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        updated.forEach(t -> {
            modelRules.stream().filter(template -> template.getUID().equals(t.getUID())).findFirst()
                    .ifPresentOrElse(oldTemplate -> {
                        modelRules.remove(oldTemplate);
                        modelRules.add(t);
                        logger.debug("model {} updated rule template {}", modelName, t.getUID());
                        notifyListenersAboutUpdatedElement(oldTemplate, t);
                    }, () -> {
                        modelRules.add(t);
                        logger.debug("model {} added rule template {}", modelName, t.getUID());
                        notifyListenersAboutAddedElement(t);
                    });
        });
    }

    @Override
    public void removedModel(String modelName, Collection<YamlRuleTemplateDTO> elements) {
        List<RuleTemplate> removed = elements.stream().map(this::mapRuleTemplate).filter(Objects::nonNull).toList();
        Collection<RuleTemplate> modelRuleTemplates = ruleTemplatesMap.getOrDefault(modelName, List.of());
        removed.forEach(t -> {
            modelRuleTemplates.stream().filter(template -> template.getUID().equals(t.getUID())).findFirst()
                    .ifPresentOrElse(oldTemplate -> {
                        modelRuleTemplates.remove(oldTemplate);
                        logger.debug("model {} removed rule template {}", modelName, t.getUID());
                        notifyListenersAboutRemovedElement(oldTemplate);
                    }, () -> logger.debug("model {} rule template {} not found", modelName, t.getUID()));
        });
        if (modelRuleTemplates.isEmpty()) {
            ruleTemplatesMap.remove(modelName);
        }
    }

    private @Nullable RuleTemplate mapRuleTemplate(YamlRuleTemplateDTO ruleTemplateDto) {
        List<YamlConfigDescriptionParameterDTO> configDescriptionDtos = ruleTemplateDto.configDescriptions;
        List<ConfigDescriptionParameter> configDescriptions = null;
        if (configDescriptionDtos != null) {
            configDescriptions = YamlConfigDescriptionParameterDTO.mapConfigDescriptions(configDescriptionDtos);
        }
        List<YamlModuleDTO> triggerDTOs = ruleTemplateDto.triggers;
        List<YamlConditionDTO> conditionDTOs = ruleTemplateDto.conditions;
        List<YamlActionDTO> actionDTOs = ruleTemplateDto.actions;
        List<Trigger> triggers = null;
        if (triggerDTOs != null) {
            triggers = mapModules(triggerDTOs, extractModuleIds(conditionDTOs, actionDTOs), Trigger.class);
        }
        List<Condition> conditions = null;
        if (conditionDTOs != null) {
            conditions = mapModules(conditionDTOs, extractModuleIds(triggers, actionDTOs), Condition.class);
        }
        List<Action> actions = null;
        if (actionDTOs != null) {
            actions = mapModules(actionDTOs, extractModuleIds(triggers, conditions), Action.class);
        }

        return new RuleTemplate(ruleTemplateDto.uid, ruleTemplateDto.label, ruleTemplateDto.description,
                ruleTemplateDto.tags, triggers, conditions, actions, configDescriptions,
                ruleTemplateDto.getVisibility());
    }
}
