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
package org.openhab.core.model.yaml.internal.semantics;

import static org.openhab.core.model.yaml.YamlModelUtils.isIsolatedModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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

/**
 * {@link YamlSemanticTagProvider} is an OSGi service, that allows to define semantic tags
 * in YAML configuration files in folder conf/tags.
 * Files can be added, updated or removed at runtime.
 * These semantic tags are automatically exposed to the {@link org.openhab.core.semantics.SemanticTagRegistry}.
 *
 * @author Laurent Garnier - Initial contribution
 * @author Laurent Garnier - Added basic version management
 * @author Laurent Garnier - Managed isolated models
 */
@NonNullByDefault
@Component(immediate = true, service = { SemanticTagProvider.class, YamlSemanticTagProvider.class,
        YamlModelListener.class })
public class YamlSemanticTagProvider extends AbstractProvider<SemanticTag>
        implements SemanticTagProvider, YamlModelListener<YamlSemanticTagDTO> {

    private final Logger logger = LoggerFactory.getLogger(YamlSemanticTagProvider.class);

    private final Map<String, Collection<SemanticTag>> tagsMap = new ConcurrentHashMap<>();

    @Activate
    public YamlSemanticTagProvider() {
    }

    @Deactivate
    public void deactivate() {
        tagsMap.clear();
    }

    @Override
    public Collection<SemanticTag> getAll() {
        // Ignore isolated models
        // Tags are returned sorted by their UID
        return tagsMap.entrySet().stream().filter(entry -> !isIsolatedModel(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream()).sorted(Comparator.comparing(SemanticTag::getUID)).toList();
    }

    public Collection<SemanticTag> getAllFromModel(String modelName) {
        // Tags are returned not sorted but rather in their original order in the file
        return tagsMap.getOrDefault(modelName, List.of());
    }

    @Override
    public Class<YamlSemanticTagDTO> getElementClass() {
        return YamlSemanticTagDTO.class;
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
    public void addedModel(String modelName, Collection<YamlSemanticTagDTO> elements) {
        boolean isolated = isIsolatedModel(modelName);
        List<SemanticTag> added = elements.stream().map(this::mapSemanticTag).toList();
        Collection<SemanticTag> modelTags = Objects
                .requireNonNull(tagsMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        modelTags.addAll(added);
        added.stream().sorted(Comparator.comparing(SemanticTag::getUID)).forEach(t -> {
            logger.debug("model {} added tag {}", modelName, t.getUID());
            if (!isolated) {
                notifyListenersAboutAddedElement(t);
            }
        });
    }

    @Override
    public void updatedModel(String modelName, Collection<YamlSemanticTagDTO> elements) {
        boolean isolated = isIsolatedModel(modelName);
        List<SemanticTag> updated = elements.stream().map(this::mapSemanticTag).toList();
        Collection<SemanticTag> modelTags = Objects
                .requireNonNull(tagsMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        updated.stream().sorted(Comparator.comparing(SemanticTag::getUID)).forEach(t -> {
            modelTags.stream().filter(tag -> tag.getUID().equals(t.getUID())).findFirst().ifPresentOrElse(oldTag -> {
                modelTags.remove(oldTag);
                modelTags.add(t);
                logger.debug("model {} updated tag {}", modelName, t.getUID());
                if (!isolated) {
                    notifyListenersAboutUpdatedElement(oldTag, t);
                }
            }, () -> {
                modelTags.add(t);
                logger.debug("model {} added tag {}", modelName, t.getUID());
                if (!isolated) {
                    notifyListenersAboutAddedElement(t);
                }
            });
        });
    }

    @Override
    public void removedModel(String modelName, Collection<YamlSemanticTagDTO> elements) {
        boolean isolated = isIsolatedModel(modelName);
        Collection<SemanticTag> modelTags = tagsMap.getOrDefault(modelName, List.of());
        elements.stream().map(elt -> elt.uid).sorted(Comparator.reverseOrder()).forEach(uid -> {
            modelTags.stream().filter(tag -> tag.getUID().equals(uid)).findFirst().ifPresentOrElse(oldTag -> {
                modelTags.remove(oldTag);
                logger.debug("model {} removed tag {}", modelName, uid);
                if (!isolated) {
                    notifyListenersAboutRemovedElement(oldTag);
                }
            }, () -> logger.debug("model {} tag {} not found", modelName, uid));
        });
        if (modelTags.isEmpty()) {
            tagsMap.remove(modelName);
        }
    }

    private SemanticTag mapSemanticTag(YamlSemanticTagDTO tagDTO) {
        return new SemanticTagImpl(tagDTO.uid, tagDTO.label, tagDTO.description, tagDTO.synonyms);
    }
}
