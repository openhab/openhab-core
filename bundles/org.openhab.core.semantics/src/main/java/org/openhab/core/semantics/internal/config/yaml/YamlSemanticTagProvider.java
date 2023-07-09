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
package org.openhab.core.semantics.internal.config.yaml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.model.core.yaml.YamlElement;
import org.openhab.core.model.core.yaml.YamlFile;
import org.openhab.core.model.core.yaml.YamlModelListener;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.SemanticTagImpl;
import org.openhab.core.semantics.SemanticTagProvider;
import org.openhab.core.semantics.SemanticTagRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link YamlSemanticTagProvider} is an OSGi service, that allows to define semantic tags
 * in YAML configuration files in folder conf/tags.
 * Files can be added, updated or removed at runtime.
 * These semantic tags are automatically exposed to the {@link SemanticTagRegistry}.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { SemanticTagProvider.class, YamlSemanticTagProvider.class,
        YamlModelListener.class })
public class YamlSemanticTagProvider extends AbstractProvider<SemanticTag>
        implements SemanticTagProvider, YamlModelListener<YamlSemanticTag> {

    private final Logger logger = LoggerFactory.getLogger(YamlSemanticTagProvider.class);

    private final List<SemanticTag> tags = new ArrayList<>();

    @Activate
    public YamlSemanticTagProvider() {
    }

    @Deactivate
    public void deactivate() {
        tags.clear();
    }

    @Override
    public Collection<SemanticTag> getAll() {
        return tags.stream().sorted(Comparator.comparing(SemanticTag::getUID)).toList();
    }

    @Override
    public String getRootName() {
        return "tags";
    }

    @Override
    public Class<? extends YamlFile> getFileClass() {
        return YamlSemanticTags.class;
    }

    @Override
    public Class<YamlSemanticTag> getElementClass() {
        return YamlSemanticTag.class;
    }

    @Override
    public void addedModel(String modelName, List<? extends YamlElement> elements) {
        List<SemanticTag> added = elements.stream().map(e -> mapSemanticTag((YamlSemanticTag) e))
                .sorted(Comparator.comparing(SemanticTag::getUID)).toList();
        tags.addAll(added);
        added.forEach(t -> {
            logger.debug("model {} added tag {}", modelName, t.getUID());
            notifyListenersAboutAddedElement(t);
        });
    }

    @Override
    public void updatedModel(String modelName, List<? extends YamlElement> elements) {
        List<SemanticTag> updated = elements.stream().map(e -> mapSemanticTag((YamlSemanticTag) e)).toList();
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
    public void removedModel(String modelName, List<? extends YamlElement> elements) {
        List<SemanticTag> removed = elements.stream().map(e -> mapSemanticTag((YamlSemanticTag) e))
                .sorted(Comparator.comparing(SemanticTag::getUID).reversed()).toList();
        removed.forEach(t -> {
            tags.stream().filter(tag -> tag.getUID().equals(t.getUID())).findFirst().ifPresentOrElse(oldTag -> {
                tags.remove(oldTag);
                logger.debug("model {} removed tag {}", modelName, t.getUID());
                notifyListenersAboutRemovedElement(oldTag);
            }, () -> logger.debug("model {} tag {} not found", modelName, t.getUID()));
        });
    }

    private SemanticTag mapSemanticTag(YamlSemanticTag tagDTO) {
        if (tagDTO.uid == null) {
            throw new IllegalArgumentException("The argument 'tagDTO.uid' must not be null.");
        }
        return new SemanticTagImpl(tagDTO.uid, tagDTO.label, tagDTO.description, tagDTO.synonyms);
    }
}
