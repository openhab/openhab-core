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
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.converter.RuleParser;
import org.openhab.core.automation.converter.RuleSerializer;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleRule;
import org.openhab.core.converter.SerializabilityResult;
import org.openhab.core.io.dto.SerializationException;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.model.yaml.internal.rules.YamlRuleDTO;
import org.openhab.core.model.yaml.internal.rules.YamlRuleProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link YamlRuleConverter} is the YAML converter for {@link Rule} objects.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { RuleSerializer.class, RuleParser.class })
public class YamlRuleConverter implements RuleSerializer, RuleParser {

    private final YamlModelRepository modelRepository;
    private final YamlRuleProvider ruleProvider;

    @Activate
    public YamlRuleConverter(@Reference YamlModelRepository modelRepository, @Reference YamlRuleProvider ruleProvider) {
        this.modelRepository = modelRepository;
        this.ruleProvider = ruleProvider;
    }

    @Override
    public String getGeneratedFormat() {
        return "YAML";
    }

    @Override
    public List<SerializabilityResult<String>> checkSerializability(Collection<Rule> rules) {
        List<SerializabilityResult<String>> result = new ArrayList<>(rules.size());
        boolean failed;
        for (Rule rule : rules) {
            failed = false;
            if (rule instanceof SimpleRule) {
                result.add(new SerializabilityResult<>(rule.getUID(), false,
                        "Rule '" + rule.getUID() + "' is a SimpleRule with an inaccessible action."));
                continue;
            }

            for (Action action : rule.getActions()) {
                if (action.getConfiguration().get("type") instanceof String type
                        && "application/vnd.openhab.dsl.rule".equals(type)
                        && action.getConfiguration().get(Module.SHARED_CONTEXT) instanceof Boolean shared
                        && shared.booleanValue()) {
                    result.add(new SerializabilityResult<>(rule.getUID(), false,
                            "Rule '" + rule.getUID() + "': action '" + action.getId() + "' has shared context."));
                    failed = true;
                    break;
                }
            }

            if (!failed) {
                result.add(new SerializabilityResult<>(rule.getUID(), true, ""));
            }
        }

        return result;
    }

    @Override
    public void setRulesToBeSerialized(String id, List<Rule> rules, RuleSerializationOption option)
            throws SerializationException {
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

        Set<Rule> handledRules = new HashSet<>();
        List<YamlElement> elements = new ArrayList<>(rules.size());
        for (Rule rule : rules) {
            if (handledRules.contains(rule)) {
                continue;
            }
            try {
                elements.add(new YamlRuleDTO(rule, option));
            } catch (RuntimeException e) {
                if (errors == null) {
                    errors = new ArrayList<>();
                }
                errors.add("Rule '" + rule.getUID() + "': " + e.getMessage());

            }
        }
        if (errors != null) {
            throw new SerializationException(
                    "Rule serialization attempt failed with:\n  " + String.join("\n  ", errors));
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
    public Collection<Rule> getParsedObjects(String modelName) {
        return ruleProvider.getAllFromModel(modelName);
    }

    @Override
    public void finishParsingFormat(String modelName) {
        modelRepository.removeIsolatedModel(modelName);
    }
}
