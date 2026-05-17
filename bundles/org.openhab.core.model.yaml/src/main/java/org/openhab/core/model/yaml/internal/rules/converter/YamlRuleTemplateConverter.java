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
package org.openhab.core.model.yaml.internal.rules.converter;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.converter.RuleTemplateParser;
import org.openhab.core.automation.converter.RuleTemplateSerializer;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.converter.SerializabilityResult;
import org.openhab.core.io.dto.SerializationException;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.model.yaml.internal.rules.YamlRuleTemplateDTO;
import org.openhab.core.model.yaml.internal.rules.YamlRuleTemplateProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link YamlRuleTemplateConverter} is the YAML converter for {@link RuleTemplate} objects.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { RuleTemplateSerializer.class, RuleTemplateParser.class })
public class YamlRuleTemplateConverter implements RuleTemplateSerializer, RuleTemplateParser {

    private final YamlModelRepository modelRepository;
    private final YamlRuleTemplateProvider templateProvider;

    @Activate
    public YamlRuleTemplateConverter(@Reference YamlModelRepository modelRepository,
            @Reference YamlRuleTemplateProvider templateProvider) {
        this.modelRepository = modelRepository;
        this.templateProvider = templateProvider;
    }

    @Override
    public String getGeneratedFormat() {
        return "YAML";
    }

    @Override
    public List<SerializabilityResult<String>> checkSerializability(Collection<RuleTemplate> templates) {
        List<SerializabilityResult<String>> result = new ArrayList<>(templates.size());
        for (RuleTemplate template : templates) {
            // There are no known circumstances under which a rule template can't be serialized to YAML
            result.add(new SerializabilityResult<>(template.getUID(), true, ""));
        }
        return result;
    }

    @Override
    public void setTemplatesToBeSerialized(String id, List<RuleTemplate> templates,
            RuleTemplateSerializationOption option) throws SerializationException {
        List<String> errors = null;
        List<SerializabilityResult<String>> checks = checkSerializability(templates);
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
                    "Rule template serialization attempt failed with:\n  " + String.join("\n  ", errors));
        }

        Set<RuleTemplate> handledTemplates = new HashSet<>();
        List<YamlElement> elements = new ArrayList<>(templates.size());
        for (RuleTemplate template : templates) {
            if (handledTemplates.contains(template)) {
                continue;
            }
            try {
                elements.add(new YamlRuleTemplateDTO(template, option));
            } catch (RuntimeException e) {
                if (errors == null) {
                    errors = new ArrayList<>();
                }
                errors.add("Rule template '" + template.getUID() + "': " + e.getMessage());

            }
        }
        if (errors != null) {
            throw new SerializationException(
                    "Rule template serialization attempt failed with:\n  " + String.join("\n  ", errors));
        }

        modelRepository.addElementsToBeGenerated(id, elements);
    }

    @Override
    public void generateFormat(String id, OutputStream out) {
        modelRepository.generateFileFormat(id, out);
    }

    @Override
    public String getParserFormat() {
        return "YAML";
    }

    @Override
    public @Nullable String startParsingFormat(String syntax, List<String> errors, List<String> warnings) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(syntax.getBytes());
        return modelRepository.createIsolatedModel(inputStream, errors, warnings);
    }

    @Override
    public Collection<RuleTemplate> getParsedObjects(String modelName) {
        return templateProvider.getAllFromModel(modelName);
    }

    @Override
    public void finishParsingFormat(String modelName) {
        modelRepository.removeIsolatedModel(modelName);
    }
}
