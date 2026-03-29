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
package org.openhab.core.model.yaml.internal.items;

import static org.openhab.core.model.yaml.YamlModelUtils.isIsolatedModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.items.ItemProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class serves as a provider for all metadata that is found within YAML files.
 * It is filled with content by the {@link YamlItemProvider}, which cannot itself implement the
 * {@link MetadataProvider} interface as it already implements {@link ItemProvider}, which would lead to duplicate
 * methods.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(service = { MetadataProvider.class, YamlMetadataProvider.class })
public class YamlMetadataProvider extends AbstractProvider<Metadata> implements MetadataProvider {

    private final Logger logger = LoggerFactory.getLogger(YamlMetadataProvider.class);

    // Map the metadata to each namespace and then to each item name and finally to each model name
    private Map<String, Map<String, Map<String, Metadata>>> metadataMap = new ConcurrentHashMap<>();

    @Override
    public Collection<Metadata> getAll() {
        // Ignore isolated models
        return metadataMap.keySet().stream().filter(name -> !isIsolatedModel(name))
                .map(name -> metadataMap.getOrDefault(name, Map.of())).flatMap(m -> m.values().stream())
                .flatMap(m -> m.values().stream()).toList();
    }

    public Collection<Metadata> getAllFromModel(String modelName) {
        return metadataMap.getOrDefault(modelName, Map.of()).values().stream().flatMap(m -> m.values().stream())
                .toList();
    }

    public void updateMetadata(String modelName, String itemName, Map<String, YamlMetadataDTO> metadata) {
        Map<String, Map<String, Metadata>> itemsMetadataMap = Objects
                .requireNonNull(metadataMap.computeIfAbsent(modelName, k -> new ConcurrentHashMap<>()));
        Map<String, Metadata> namespacesMetadataMap = Objects
                .requireNonNull(itemsMetadataMap.computeIfAbsent(itemName, k -> new ConcurrentHashMap<>()));

        Set<String> namespaceToBeRemoved = new HashSet<>(namespacesMetadataMap.keySet());

        for (Map.Entry<String, YamlMetadataDTO> entry : metadata.entrySet()) {
            String namespace = entry.getKey();
            YamlMetadataDTO mdDTO = entry.getValue();
            MetadataKey key = new MetadataKey(namespace, itemName);
            Metadata md = new Metadata(key, mdDTO.value == null ? "" : mdDTO.value, mdDTO.config);

            namespaceToBeRemoved.remove(namespace);
            Metadata oldMd = namespacesMetadataMap.get(namespace);
            if (oldMd == null) {
                namespacesMetadataMap.put(namespace, md);
                logger.debug("model {} added metadata {}", modelName, namespace);
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutAddedElement(md);
                }
            } else if (!md.getValue().equals(oldMd.getValue())
                    || !Objects.equals(md.getConfiguration(), oldMd.getConfiguration())) {
                namespacesMetadataMap.put(namespace, md);
                logger.debug("model {} updated metadata {}", modelName, namespace);
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutUpdatedElement(oldMd, md);
                }
            }
        }

        namespaceToBeRemoved.forEach(namespace -> {
            Metadata md = namespacesMetadataMap.remove(namespace);
            if (md != null) {
                logger.debug("model {} removed metadata {}", modelName, namespace);
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutRemovedElement(md);
                }
            }
        });
        if (namespacesMetadataMap.isEmpty()) {
            itemsMetadataMap.remove(itemName);
        }
        if (itemsMetadataMap.isEmpty()) {
            metadataMap.remove(modelName);
        }
    }
}
