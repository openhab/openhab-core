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
package org.openhab.core.addon.marketplace.internal.automation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.dto.RuleTemplateDTO;
import org.openhab.core.automation.dto.RuleTemplateDTOMapper;
import org.openhab.core.automation.parser.Parser;
import org.openhab.core.automation.parser.ParsingException;
import org.openhab.core.automation.parser.ParsingNestedException;
import org.openhab.core.automation.parser.ValidationException;
import org.openhab.core.automation.parser.ValidationException.ObjectType;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.RuleTemplateProvider;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * This is a {@link RuleTemplateProvider}, which gets its content from the marketplace add-on service
 * and stores it through the OH storage service.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Yannick Schaus - refactoring
 * @author Arne Seime - refactored rule template parsing
 */
@NonNullByDefault
@Component(service = { MarketplaceRuleTemplateProvider.class, RuleTemplateProvider.class })
public class MarketplaceRuleTemplateProvider extends AbstractManagedProvider<RuleTemplate, String, RuleTemplateDTO>
        implements RuleTemplateProvider {

    private final Map<String, Parser<RuleTemplate>> parsers = new ConcurrentHashMap<>();
    ObjectMapper yamlMapper;

    @Activate
    public MarketplaceRuleTemplateProvider(final @Reference StorageService storageService) {
        super(storageService);
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.findAndRegisterModules();
    }

    /**
     * Registers a {@link Parser}.
     *
     * @param parser the {@link Parser} service to register.
     * @param properties the properties.
     */
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target = "(parser.type=parser.template)")
    public void addParser(Parser<RuleTemplate> parser, Map<String, String> properties) {
        String parserType = properties.get(Parser.FORMAT);
        parserType = parserType == null ? Parser.FORMAT_JSON : parserType;
        parsers.put(parserType, parser);
    }

    /**
     * Unregisters a {@link Parser}.
     *
     * @param parser the {@link Parser} service to unregister.
     * @param properties the properties.
     */
    public void removeParser(Parser<RuleTemplate> parser, Map<String, String> properties) {
        String parserType = properties.get(Parser.FORMAT);
        parserType = parserType == null ? Parser.FORMAT_JSON : parserType;
        parsers.remove(parserType);
    }

    @Override
    public @Nullable RuleTemplate getTemplate(String uid, @Nullable Locale locale) {
        return get(uid);
    }

    @Override
    public Collection<RuleTemplate> getTemplates(@Nullable Locale locale) {
        return getAll();
    }

    @Override
    protected String getStorageName() {
        return "marketplace_ruletemplates";
    }

    @Override
    protected String keyToString(String key) {
        return key;
    }

    @Override
    protected @Nullable RuleTemplate toElement(String key, RuleTemplateDTO persistableElement) {
        return RuleTemplateDTOMapper.map(persistableElement);
    }

    @Override
    protected RuleTemplateDTO toPersistableElement(RuleTemplate element) {
        return RuleTemplateDTOMapper.map(element);
    }

    /**
     * Adds a new rule template to persistent storage from its {@code JSON} representation.
     *
     * @param uid the marketplace UID to use.
     * @param json the template content as a {@code JSON} string
     * @throws ParsingException If the parsing fails.
     * @throws ValidationException If the validation fails.
     */
    public void addTemplateAsJSON(String uid, String json) throws ParsingException, ValidationException {
        addTemplate(uid, json, Parser.FORMAT_JSON);
    }

    /**
     * Adds a new rule template to persistent storage from its {@code YAML} representation.
     *
     * @param uid the marketplace UID to use.
     * @param yaml the template content as a {@code YAML} string
     * @throws ParsingException If the parsing fails.
     * @throws ValidationException If the validation fails.
     */
    public void addTemplateAsYAML(String uid, String yaml) throws ParsingException, ValidationException {
        addTemplate(uid, yaml, Parser.FORMAT_YAML);
    }

    /**
     * Adds one or ore new {@link RuleTemplate}s parsed from the provided content using the specified parser.
     *
     * @param uid the marketplace UID to use.
     * @param content the content to parse.
     * @param format the format to parse.
     * @throws ParsingException If the parsing fails.
     * @throws ValidationException If the validation fails.
     */
    protected void addTemplate(String uid, String content, String format) throws ParsingException, ValidationException {
        Parser<RuleTemplate> parser = parsers.get(format);

        // The parser might not have been registered yet
        if (parser == null) {
            throw new ParsingException(new ParsingNestedException(ParsingNestedException.TEMPLATE,
                    "No " + format.toUpperCase(Locale.ROOT) + " parser available", null));
        }

        try (InputStreamReader isr = new InputStreamReader(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))) {
            Set<RuleTemplate> templates = parser.parse(isr);

            // Add a tag with the marketplace add-on ID to be able to identify the template in the registry
            Set<String> tags;
            for (RuleTemplate template : templates) {
                validateTemplate(template);
                tags = new HashSet<String>(template.getTags());
                tags.add(uid);
                add(new RuleTemplate(template.getUID(), template.getLabel(), template.getDescription(), tags,
                        template.getTriggers(), template.getConditions(), template.getActions(),
                        template.getConfigurationDescriptions(), template.getVisibility()));
            }
        } catch (IOException e) {
            // Impossible for ByteArrayInputStream
        }
    }

    /**
     * Validates that the parsed template is valid.
     *
     * @param template the {@link RuleTemplate} to validate.
     * @throws ValidationException If the validation failed.
     */
    @SuppressWarnings("null")
    protected void validateTemplate(RuleTemplate template) throws ValidationException {
        String s;
        if ((s = template.getUID()) == null || s.isBlank()) {
            throw new ValidationException(ObjectType.TEMPLATE, null, "UID cannot be blank");
        }
        if ((s = template.getLabel()) == null || s.isBlank()) {
            throw new ValidationException(ObjectType.TEMPLATE, template.getUID(), "Label cannot be blank");
        }
        if (template.getModules(Module.class).isEmpty()) {
            throw new ValidationException(ObjectType.TEMPLATE, template.getUID(), "There must be at least one module");
        }
    }
}
