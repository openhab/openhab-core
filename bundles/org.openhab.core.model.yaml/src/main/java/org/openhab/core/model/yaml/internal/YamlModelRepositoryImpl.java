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
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.model.yaml.YamlModelRepository;
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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * The {@link YamlModelRepositoryImpl} is an OSGi service, that encapsulates all YAML file processing
 * including file monitoring to detect created, updated and removed YAML configuration files.
 * Data processed from these files are consumed by registered OSGi services that implement {@link YamlModelListener}.
 *
 * @author Laurent Garnier - Initial contribution
 * @author Jan N. Klug - Refactored for multiple types per file and add modifying possibility
 */
@NonNullByDefault
@Component(immediate = true)
public class YamlModelRepositoryImpl implements WatchService.WatchEventListener, YamlModelRepository {
    private final Logger logger = LoggerFactory.getLogger(YamlModelRepositoryImpl.class);

    private final WatchService watchService;
    private final Path watchPath;
    private final ObjectMapper objectMapper;

    private final Map<String, List<YamlModelListener<?>>> typeListeners = new ConcurrentHashMap<>();
    // all model nodes, ordered by model name (full path as string) and type
    private final Map<String, Map<String, List<JsonNode>>> modelCache = new ConcurrentHashMap<>();

    @Activate
    public YamlModelRepositoryImpl(@Reference(target = WatchService.CONFIG_WATCHER_FILTER) WatchService watchService) {
        YAMLFactory yamlFactory = YAMLFactory.builder() //
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER) // omit "---" at file start
                .disable(YAMLGenerator.Feature.SPLIT_LINES) // do not split long lines
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR) // indent arrays
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES) // use quotes only where necessary
                .build();
        this.objectMapper = new ObjectMapper(yamlFactory);
        objectMapper.findAndRegisterModules();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        this.watchService = watchService;
        watchService.registerListener(this, Path.of(""));
        watchPath = watchService.getWatchPath();
    }

    @Deactivate
    public void deactivate() {
        watchService.unregisterListener(this);
    }

    // The method is "synchronized" to avoid concurrent files processing
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public synchronized void processWatchEvent(Kind kind, Path path) {
        Path fullPath = watchPath.resolve(path);
        String modelName = path.toString();

        if (Files.isDirectory(fullPath) || fullPath.toFile().isHidden() || !modelName.endsWith(".yaml")) {
            logger.trace("Ignored {}", fullPath);
            return;
        }

        if (kind == WatchService.Kind.DELETE) {
            logger.info("Removing YAML model {}", modelName);
            Map<String, List<JsonNode>> removedModel = modelCache.remove(modelName);
            if (removedModel == null) {
                return;
            }
            for (Map.Entry<String, List<JsonNode>> modelEntry : removedModel.entrySet()) {
                String typeName = modelEntry.getKey();
                List<JsonNode> removedNodes = modelEntry.getValue();
                if (!removedNodes.isEmpty()) {
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
                JsonNode fileContent = objectMapper.readTree(fullPath.toFile());

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

                Map<String, List<JsonNode>> model = Objects
                        .requireNonNull(modelCache.computeIfAbsent(modelName, k -> new ConcurrentHashMap<>()));

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

                    List<JsonNode> oldNodeElements = model.getOrDefault(typeName, List.of());
                    List<JsonNode> newNodeElements = new ArrayList<>();
                    node.elements().forEachRemaining(newNodeElements::add);

                    for (YamlModelListener<?> typeListener : getTypeListeners(typeName)) {
                        Class<? extends YamlElement> typeClass = typeListener.getTypeClass();

                        Map<String, ? extends YamlElement> oldElements = listToMap(
                                parseJsonNodes(oldNodeElements, typeClass));
                        Map<String, ? extends YamlElement> newElements = listToMap(
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
                    model.put(typeName, newNodeElements);
                }
            } catch (IOException e) {
                logger.warn("Failed to read {}: {}", modelName, e.getMessage());
            }
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addYamlModelListener(YamlModelListener<? extends YamlElement> listener) {
        Class<? extends YamlElement> typeClass = listener.getTypeClass();
        YamlElementName annotation = typeClass.getAnnotation(YamlElementName.class);
        if (annotation == null) {
            logger.warn("Class {} is missing the mandatory YamlElementName annotation. This is a bug.", typeClass);
            return;
        }
        String typeName = annotation.value();
        getTypeListeners(typeName).add(listener);

        // iterate over all models and notify he new listener of already existing models with this type
        for (Map.Entry<String, Map<String, List<JsonNode>>> model : modelCache.entrySet()) {
            String modelName = model.getKey();
            List<JsonNode> modelNodes = model.getValue().get(typeName);
            if (modelNodes == null || modelNodes.isEmpty()) {
                continue;
            }
            List modelElements = parseJsonNodes(modelNodes, typeClass);
            listener.addedModel(modelName, modelElements);
        }
    }

    public void removeYamlModelListener(YamlModelListener<? extends YamlElement> listener) {
        typeListeners.values().forEach(list -> list.remove(listener));
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addElementToModel(String modelName, YamlElement element) {
        YamlElementName annotation = element.getClass().getAnnotation(YamlElementName.class);
        if (annotation == null) {
            logger.warn(
                    "Failed to add element {}. Class {}) is missing the mandatory YamlElementName annotation. This is a bug.",
                    element.getId(), element.getClass());
            return;
        }
        String typeName = annotation.value();
        Map<String, List<JsonNode>> model = Objects
                .requireNonNull(modelCache.computeIfAbsent(modelName, k -> new ConcurrentHashMap<>()));
        List<JsonNode> modelNodes = model.computeIfAbsent(typeName, k -> new CopyOnWriteArrayList<>());
        JsonNode newNode = objectMapper.convertValue(element, JsonNode.class);
        modelNodes.add(newNode);
        // notify listeners
        getTypeListeners(typeName).forEach(l -> {
            List newElements = parseJsonNodes(List.of(newNode), l.getTypeClass());
            if (!newElements.isEmpty()) {
                l.addedModel(modelName, newElements);
            }
        });

        writeModel(modelName);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void removeElementFromModel(String modelName, YamlElement element) {
        YamlElementName annotation = element.getClass().getAnnotation(YamlElementName.class);
        if (annotation == null) {
            logger.warn(
                    "Failed to remove element {}. Class {}) is missing the mandatory YamlElementName annotation. This is a bug.",
                    element.getId(), element.getClass());
            return;
        }
        String typeName = annotation.value();
        Map<String, List<JsonNode>> model = modelCache.get(modelName);
        if (model == null) {
            logger.warn("Failed to remove {} from model {} because the model is not known.", element, modelName);
            return;
        }

        List<JsonNode> modelNodes = model.get(typeName);
        if (modelNodes == null) {
            logger.warn("Failed to remove {} from model {} because type {} is not known in the model.", element,
                    modelName, typeName);
            return;
        }
        JsonNode toRemove = findNodeById(modelNodes, element.getClass(), element.getId());
        if (toRemove == null) {
            logger.warn("Failed to remove {} from model {} because element is not in model.", element, modelName);
            return;
        }
        modelNodes.remove(toRemove);
        // notify listeners
        getTypeListeners(typeName).forEach(l -> {
            List newElements = parseJsonNodes(List.of(toRemove), l.getTypeClass());
            if (!newElements.isEmpty()) {
                l.addedModel(modelName, newElements);
            }
        });

        writeModel(modelName);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void updateElementInModel(String modelName, YamlElement element) {
        YamlElementName annotation = element.getClass().getAnnotation(YamlElementName.class);
        if (annotation == null) {
            logger.warn(
                    "Failed to update element {}. Class {}) is missing the mandatory YamlElementName annotation. This is a bug.",
                    element.getId(), element.getClass());
            return;
        }
        String typeName = annotation.value();
        Map<String, List<JsonNode>> model = modelCache.get(modelName);
        if (model == null) {
            logger.warn("Failed to update {} in model {} because the model is not known.", element, modelName);
            return;
        }
        List<JsonNode> modelNodes = model.get(typeName);
        if (modelNodes == null) {
            logger.warn("Failed to update {} in model {} because type {} is not known in the model.", element,
                    modelName, typeName);
            return;
        }
        JsonNode oldElement = findNodeById(modelNodes, element.getClass(), element.getId());
        if (oldElement == null) {
            logger.warn("Failed to update {} in model {} because element is not in model.", element, modelName);
            return;
        }

        JsonNode newNode = objectMapper.convertValue(element, JsonNode.class);
        modelNodes.set(modelNodes.indexOf(oldElement), newNode);
        // notify listeners
        getTypeListeners(typeName).forEach(l -> {
            List newElements = parseJsonNodes(List.of(newNode), l.getTypeClass());
            if (!newElements.isEmpty()) {
                l.updatedModel(modelName, newElements);
            }
        });

        writeModel(modelName);
    }

    private void writeModel(String modelName) {
        Map<String, List<JsonNode>> model = modelCache.get(modelName);
        if (model == null) {
            logger.warn("Failed to write model to disc {} because it is not known.", modelName);
            return;
        }

        // create the model
        JsonNodeFactory nodeFactory = objectMapper.getNodeFactory();
        ObjectNode rootNode = nodeFactory.objectNode();
        rootNode.put("version", 1);
        for (Map.Entry<String, List<JsonNode>> typeNodes : model.entrySet()) {
            ArrayNode arrayNode = nodeFactory.arrayNode();
            typeNodes.getValue().forEach(arrayNode::add);
            rootNode.set(typeNodes.getKey(), arrayNode);
        }

        try {
            Path outFile = watchPath.resolve(modelName);
            String fileContent = objectMapper.writeValueAsString(rootNode);
            if (Files.exists(outFile) && !Files.isWritable(outFile)) {
                logger.warn("Failed writing model {}: model exists but is read-only.", modelName);
                return;
            }
            Files.writeString(outFile, fileContent);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize model {}: {}", modelName, e.getMessage());
        } catch (IOException e) {
            logger.warn("Failed writing model {}: {}", modelName, e.getMessage());
        }
    }

    private List<YamlModelListener<?>> getTypeListeners(String typeName) {
        return Objects.requireNonNull(typeListeners.computeIfAbsent(typeName, k -> new CopyOnWriteArrayList<>()));
    }

    private <T extends YamlElement> @Nullable JsonNode findNodeById(List<JsonNode> nodes, Class<T> typeClass,
            String id) {
        return nodes.stream().filter(node -> {
            Optional<T> parsedNode = parseJsonNode(node, typeClass);
            return parsedNode.filter(yamlDTO -> id.equals(yamlDTO.getId())).isPresent();
        }).findAny().orElse(null);
    }

    private Map<String, ? extends YamlElement> listToMap(List<? extends YamlElement> elements) {
        return elements.stream().collect(Collectors.toMap(YamlElement::getId, e -> e));
    }

    private <T extends YamlElement> List<T> parseJsonNodes(List<JsonNode> nodes, Class<T> typeClass) {
        return nodes.stream().map(nE -> parseJsonNode(nE, typeClass)).filter(Optional::isPresent).map(Optional::get)
                .filter(YamlElement::isValid).toList();
    }

    private <T extends YamlElement> Optional<T> parseJsonNode(JsonNode node, Class<T> typeClass) {
        try {
            return Optional.of(objectMapper.treeToValue(node, typeClass));
        } catch (JsonProcessingException e) {
            logger.warn("Could not parse element {} to {}: {}", node, typeClass, e.getMessage());
            return Optional.empty();
        }
    }
}
