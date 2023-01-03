/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.dto.RuleTemplateDTO;
import org.openhab.core.automation.dto.RuleTemplateDTOMapper;
import org.openhab.core.automation.parser.Parser;
import org.openhab.core.automation.parser.ParsingException;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.RuleTemplateProvider;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * This is a {@link RuleTemplateProvider}, which gets its content from the marketplace add-on service
 * and stores it through the OH storage service.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Yannick Schaus - refactoring
 *
 */
@NonNullByDefault
@Component(service = { MarketplaceRuleTemplateProvider.class, RuleTemplateProvider.class })
public class MarketplaceRuleTemplateProvider extends AbstractManagedProvider<RuleTemplate, String, RuleTemplateDTO>
        implements RuleTemplateProvider {

    private final Logger logger = LoggerFactory.getLogger(MarketplaceRuleTemplateProvider.class);

    private final Parser<RuleTemplate> parser;
    ObjectMapper yamlMapper;

    @Activate
    public MarketplaceRuleTemplateProvider(final @Reference StorageService storageService,
            final @Reference(target = "(&(format=json)(parser.type=parser.template))") Parser<RuleTemplate> parser) {
        super(storageService);
        this.parser = parser;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.findAndRegisterModules();
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
     * This adds a new rule template to the persistent storage from its JSON representation.
     *
     * @param uid the UID to be used for the template
     * @param json the template content as a JSON string
     *
     * @throws ParsingException if the content cannot be parsed correctly
     */
    public void addTemplateAsJSON(String uid, String json) throws ParsingException {
        try (InputStreamReader isr = new InputStreamReader(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            Set<RuleTemplate> templates = parser.parse(isr);
            if (templates.size() != 1) {
                throw new IllegalArgumentException("JSON must contain exactly one template!");
            } else {
                RuleTemplate entry = templates.iterator().next();
                // add a tag with the add-on ID to be able to identify the widget in the registry
                entry.getTags().add(uid);
                RuleTemplate template = new RuleTemplate(entry.getUID(), entry.getLabel(), entry.getDescription(),
                        entry.getTags(), entry.getTriggers(), entry.getConditions(), entry.getActions(),
                        entry.getConfigurationDescriptions(), entry.getVisibility());
                add(template);
            }
        } catch (IOException e) {
            logger.error("Cannot close input stream.", e);
        }
    }

    /**
     * This adds a new rule template to the persistent storage from its YAML representation.
     *
     * @param uid the UID to be used for the template
     * @param json the template content as a YAML string
     *
     * @throws ParsingException if the content cannot be parsed correctly
     */
    public void addTemplateAsYAML(String uid, String yaml) throws ParsingException {
        try {
            RuleTemplateDTO dto = yamlMapper.readValue(yaml, RuleTemplateDTO.class);
            // add a tag with the add-on ID to be able to identify the widget in the registry
            dto.tags = new HashSet<@Nullable String>((dto.tags != null) ? dto.tags : new HashSet<String>());
            dto.tags.add(uid);
            RuleTemplate entry = RuleTemplateDTOMapper.map(dto);
            RuleTemplate template = new RuleTemplate(entry.getUID(), entry.getLabel(), entry.getDescription(),
                    entry.getTags(), entry.getTriggers(), entry.getConditions(), entry.getActions(),
                    entry.getConfigurationDescriptions(), entry.getVisibility());
            add(template);
        } catch (IOException e) {
            logger.error("Unable to parse YAML: {}", e.getMessage());
            throw new IllegalArgumentException("Unable to parse YAML");
        }
    }
}
