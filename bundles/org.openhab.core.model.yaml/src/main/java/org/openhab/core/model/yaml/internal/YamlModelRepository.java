/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.model.yaml.YamlDTO;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.service.WatchService;
import org.openhab.core.service.WatchService.Kind;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * The {@link YamlModelRepository} is an OSGi service, that encapsulates all YAML file processing
 * including file monitoring to detect created, updated and removed YAML configuration files.
 * Data processed from these files are consumed by registered OSGi services that implement {@link YamlModelListener}.
 *
 * @author Laurent Garnier - Initial contribution
 * @author Jan N. Klug - Refactored for multiple types per file
 */
@NonNullByDefault
@Component(immediate = true)
public class YamlModelRepository implements WatchService.WatchEventListener {
    private final Logger logger = LoggerFactory.getLogger(YamlModelRepository.class);

    private final WatchService watchService;
    private final Path watchPath;
    private final ObjectMapper yamlReader;

    private final Map<String, List<YamlModelListener<?>>> typeListeners = new ConcurrentHashMap<>();
    // all model nodes, ordered by type and model name (full path as string)
    private final Map<String, Map<String, List<JsonNode>>> modelCache = new ConcurrentHashMap<>();

    @Activate
    public YamlModelRepository(@Reference(target = WatchService.CONFIG_WATCHER_FILTER) WatchService watchService) {
        this.watchService = watchService;
        this.yamlReader = new ObjectMapper(new YAMLFactory());
        yamlReader.findAndRegisterModules();

        watchService.registerListener(this, Path.of(""));
        watchPath = watchService.getWatchPath();
    }

    @Deactivate
    public void deactivate() {
        watchService.unregisterListener(this);
    }

    // The method is "synchronized" to avoid concurrent files processing
    @Override
    public synchronized void processWatchEvent(Kind kind, Path path) {
        Path fullPath = watchPath.resolve(path);
        String modelName = fullPath.toString();

        if (Files.isDirectory(fullPath) || fullPath.toFile().isHidden() || !modelName.endsWith(".yaml")) {
            logger.trace("Ignored {}", fullPath);
            return;
        }

        process(modelName, kind, fullPath.toFile());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    void process(String modelName, Kind kind, File file) {
        if (kind == WatchService.Kind.DELETE) {
            logger.info("Removing YAML model {}", modelName);
            for (Map.Entry<String, Map<String, List<JsonNode>>> typeEntry : modelCache.entrySet()) {
                String typeName = typeEntry.getKey();
                List<JsonNode> removedNodes = typeEntry.getValue().remove(modelName);
                if (removedNodes != null && !removedNodes.isEmpty()) {
                    getTypeListeners(typeName).forEach(listener -> {
                        List removedElements = parseJsonNodes(removedNodes, listener.getTypeClass());
                        listener.removedModel(modelName, removedElements);
                    });
                }
            }
        } else {
            if (kind == Kind.CREATE) {
                logger.info("Adding YAML model {}", modelName);
            } else {
                logger.info("Updating YAML model {}", modelName);
            }
            try {
                JsonNode fileContent = yamlReader.readTree(file);

                // check version
                JsonNode versionNode = fileContent.get("version");
                if (versionNode == null || !versionNode.canConvertToInt()) {
                    logger.warn("Version is missing or not a number in model {}. Ignoring it.", modelName);
                    return;
                }
                int fileVersion = versionNode.asInt();
                if (fileVersion != 1) {
                    logger.warn("Model {} has version {}, but only version 1 is supported. Ignoring it.", modelName,
                            fileVersion);
                    return;
                }

                // get sub-elements
                Iterator<Map.Entry<String, JsonNode>> it = fileContent.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> element = it.next();
                    String typeName = element.getKey();
                    JsonNode node = element.getValue();
                    if (!node.isArray()) {
                        // all processable sub-elements are arrays
                        logger.trace("Element {} in model {} is not an array, ignoring it", typeName, modelName);
                        continue;
                    }

                    Map<String, List<JsonNode>> typeCache = Objects
                            .requireNonNull(modelCache.computeIfAbsent(typeName, k -> new ConcurrentHashMap<>()));
                    List<JsonNode> oldNodeElements = typeCache.getOrDefault(modelName, List.of());
                    List<JsonNode> newNodeElements = new ArrayList<>();
                    node.elements().forEachRemaining(newNodeElements::add);

                    for (YamlModelListener<?> typeListener : getTypeListeners(typeName)) {
                        Class<? extends YamlDTO> typeClass = typeListener.getTypeClass();

                        Map<String, ? extends YamlDTO> oldElements = listToMap(
                                parseJsonNodes(oldNodeElements, typeClass));
                        Map<String, ? extends YamlDTO> newElements = listToMap(
                                parseJsonNodes(newNodeElements, typeClass));

                        List addedElements = newElements.values().stream()
                                .filter(e -> !oldElements.containsKey(e.getId())).toList();
                        List removedElements = oldElements.values().stream()
                                .filter(e -> !newElements.containsKey(e.getId())).toList();
                        List updatedElements = newElements.values().stream().filter(
                                e -> oldElements.containsKey(e.getId()) && !e.equals(oldElements.get(e.getId())))
                                .toList();

                        if (!addedElements.isEmpty()) {
                            typeListener.addedModel(modelName, addedElements);
                        }
                        if (!removedElements.isEmpty()) {
                            typeListener.removedModel(modelName, removedElements);
                        }
                        if (!updatedElements.isEmpty()) {
                            typeListener.updatedModel(modelName, updatedElements);
                        }
                    }

                    // replace cache
                    typeCache.put(modelName, newNodeElements);
                }
            } catch (IOException e) {
                logger.warn("Failed to read {}: {}", modelName, e.getMessage());
            }
        }
    }

    private Map<String, ? extends YamlDTO> listToMap(List<? extends YamlDTO> elements) {
        return elements.stream().collect(Collectors.toMap(YamlDTO::getId, e -> e));
    }

    private <T extends YamlDTO> List<T> parseJsonNodes(List<JsonNode> nodes, Class<T> typeClass) {
        return nodes.stream().map(nE -> parseJsonNode(nE, typeClass)).filter(Optional::isPresent).map(Optional::get)
                .filter(YamlDTO::isValid).toList();
    }

    private <T extends YamlDTO> Optional<T> parseJsonNode(JsonNode node, Class<T> typeClass) {
        try {
            return Optional.of(yamlReader.treeToValue(node, typeClass));
        } catch (JsonProcessingException e) {
            logger.warn("Could not parse element {} to {}: {}", node.toString(), typeClass, e.getMessage());
            return Optional.empty();
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void addYamlModelListener(YamlModelListener<?> listener) {
        String typeName = listener.getTypeName();
        Class<? extends YamlDTO> typeClass = listener.getTypeClass();
        logger.debug("Adding model listener for {}", typeName);
        getTypeListeners(typeName).add(listener);

        Map<String, List<JsonNode>> cachedModels = modelCache.getOrDefault(typeName, Map.of());
        for (Map.Entry<String, List<JsonNode>> model : cachedModels.entrySet()) {
            if (model.getValue().isEmpty()) {
                continue;
            }
            List modelElements = parseJsonNodes(model.getValue(), typeClass);
            listener.addedModel(model.getKey(), modelElements);
        }
    }

    protected void removeYamlModelListener(YamlModelListener<?> listener) {
        String typeName = listener.getTypeName();
        logger.debug("Removing model listener for {}", typeName);
        getTypeListeners(typeName).remove(listener);
    }

    private List<YamlModelListener<?>> getTypeListeners(String typeName) {
        return Objects.requireNonNull(typeListeners.computeIfAbsent(typeName, k -> new CopyOnWriteArrayList<>()));
    }
}
