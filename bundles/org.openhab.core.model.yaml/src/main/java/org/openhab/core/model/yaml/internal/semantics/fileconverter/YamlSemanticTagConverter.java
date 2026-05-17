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
package org.openhab.core.model.yaml.internal.semantics.fileconverter;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.model.yaml.internal.semantics.YamlSemanticTagDTO;
import org.openhab.core.model.yaml.internal.semantics.YamlSemanticTagProvider;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.fileconverter.SemanticTagParser;
import org.openhab.core.semantics.fileconverter.SemanticTagSerializer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link YamlSemanticTagConverter} is the YAML converter for {@link SemanticTag} objects.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { SemanticTagSerializer.class, SemanticTagParser.class })
public class YamlSemanticTagConverter implements SemanticTagSerializer, SemanticTagParser {

    private final YamlModelRepository modelRepository;
    private final YamlSemanticTagProvider semanticTagProvider;

    @Activate
    public YamlSemanticTagConverter(final @Reference YamlModelRepository modelRepository,
            final @Reference YamlSemanticTagProvider semanticTagProvider) {
        this.modelRepository = modelRepository;
        this.semanticTagProvider = semanticTagProvider;
    }

    @Override
    public String getGeneratedFormat() {
        return "YAML";
    }

    @Override
    public void setSemanticTagsToBeSerialized(String id, List<SemanticTag> tags) {
        List<YamlElement> elements = new ArrayList<>();
        tags.forEach(tag -> {
            elements.add(buildSemanticTagDTO(tag));
        });
        modelRepository.addElementsToBeGenerated(id, elements);
    }

    @Override
    public void generateFormat(String id, OutputStream out) {
        modelRepository.generateFileFormat(id, out);
    }

    private YamlSemanticTagDTO buildSemanticTagDTO(SemanticTag tag) {
        YamlSemanticTagDTO dto = new YamlSemanticTagDTO();
        dto.uid = tag.getUID();
        dto.label = tag.getLabel().isBlank() ? null : tag.getLabel();
        dto.description = tag.getDescription().isBlank() ? null : tag.getDescription();
        dto.synonyms = tag.getSynonyms().isEmpty() ? null : List.copyOf(tag.getSynonyms());
        return dto;
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
    public Collection<SemanticTag> getParsedObjects(String modelName) {
        return semanticTagProvider.getAllFromModel(modelName);
    }

    @Override
    public void finishParsingFormat(String modelName) {
        modelRepository.removeIsolatedModel(modelName);
    }
}
