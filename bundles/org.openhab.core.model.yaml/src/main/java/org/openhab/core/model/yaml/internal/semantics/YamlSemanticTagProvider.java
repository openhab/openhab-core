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
package org.openhab.core.model.yaml.internal.semantics;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.SemanticTagImpl;
import org.openhab.core.semantics.SemanticTagProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link YamlSemanticTagProvider} is an OSGi service, that allows to define semantic tags
 * in YAML configuration files in folder conf/tags.
 * Files can be added, updated or removed at runtime.
 * These semantic tags are automatically exposed to the {@link org.openhab.core.semantics.SemanticTagRegistry}.
 *
 * @author Laurent Garnier - Initial contribution
 * @author Laurent Garnier - Added basic version management
 */
@NonNullByDefault
@Component(immediate = true, service = { SemanticTagProvider.class, YamlSemanticTagProvider.class,
        YamlModelListener.class })
public class YamlSemanticTagProvider extends AbstractProvider<SemanticTag>
        implements SemanticTagProvider, YamlModelListener<YamlSemanticTagDTO> {

    private final Logger logger = LoggerFactory.getLogger(YamlSemanticTagProvider.class);

    private final Set<SemanticTag> tags = new TreeSet<>(Comparator.comparing(SemanticTag::getUID));

    @Activate
    public YamlSemanticTagProvider() {
    }

    @Deactivate
    public void deactivate() {
        tags.clear();
    }

    @Override
    public Collection<SemanticTag> getAll() {
        return tags;
    }

    @Override
    public Class<YamlSemanticTagDTO> getElementClass() {
        return YamlSemanticTagDTO.class;
    }

    @Override
    public void addedModel(String modelName, ObjectMapper yamlMapper, Collection<YamlSemanticTagDTO> elements) {
        List<SemanticTag> added = elements.stream().map(this::mapSemanticTag)
                .sorted(Comparator.comparing(SemanticTag::getUID)).toList();
        tags.addAll(added);
        added.forEach(t -> {
            logger.debug("model {} added tag {}", modelName, t.getUID());
            notifyListenersAboutAddedElement(t);
        });
    }

    @Override
    public void updatedModel(String modelName, ObjectMapper yamlMapper, Collection<YamlSemanticTagDTO> elements) {
        List<SemanticTag> updated = elements.stream().map(this::mapSemanticTag).toList();
        updated.forEach(t -> {
            tags.stream().filter(tag -> tag.getUID().equals(t.getUID())).findFirst().ifPresentOrElse(oldTag -> {
                tags.remove(oldTag);
                tags.add(t);
                logger.debug("model {} updated tag {}", modelName, t.getUID());
                notifyListenersAboutUpdatedElement(oldTag, t);
            }, () -> logger.debug("model {} tag {} not found", modelName, t.getUID()));
        });
    }

    @Override
    public void removedModel(String modelName, ObjectMapper yamlMapper, Collection<YamlSemanticTagDTO> elements) {
        List<SemanticTag> removed = elements.stream().map(this::mapSemanticTag)
                .sorted(Comparator.comparing(SemanticTag::getUID).reversed()).toList();
        removed.forEach(t -> {
            tags.stream().filter(tag -> tag.getUID().equals(t.getUID())).findFirst().ifPresentOrElse(oldTag -> {
                tags.remove(oldTag);
                logger.debug("model {} removed tag {}", modelName, t.getUID());
                notifyListenersAboutRemovedElement(oldTag);
            }, () -> logger.debug("model {} tag {} not found", modelName, t.getUID()));
        });
    }

    private SemanticTag mapSemanticTag(YamlSemanticTagDTO tagDTO) {
        return new SemanticTagImpl(tagDTO.uid, tagDTO.label, tagDTO.description, tagDTO.synonyms);
    }

    @Override
    public boolean isVersionSupported(int version) {
        return version >= 1;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }
}
