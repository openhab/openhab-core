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
package org.openhab.core.model.yaml.internal;

import static org.openhab.core.service.WatchService.Kind.CREATE;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

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
    private static final int DEFAULT_MODEL_VERSION = 1;
    private final Logger logger = LoggerFactory.getLogger(YamlModelRepositoryImpl.class);

    private final WatchService watchService;
    private final Path watchPath;
    private final ObjectMapper objectMapper;

    private final Map<String, List<YamlModelListener<?>>> elementListeners = new ConcurrentHashMap<>();
    // all model nodes, ordered by model name (full path as string) and type
    private final Map<String, YamlModelWrapper> modelCache = new ConcurrentHashMap<>();

    @Activate
    public YamlModelRepositoryImpl(@Reference(target = WatchService.CONFIG_WATCHER_FILTER) WatchService watchService) {
        YAMLFactory yamlFactory = YAMLFactory.builder() //
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER) // omit "---" at file start
                .disable(YAMLGenerator.Feature.SPLIT_LINES) // do not split long lines
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR) // indent arrays
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES) // use quotes only where necessary
                .enable(YAMLParser.Feature.PARSE_BOOLEAN_LIKE_WORDS_AS_STRINGS).build(); // do not parse ON/OFF/... as
                                                                                         // booleans
        this.objectMapper = new ObjectMapper(yamlFactory);
        objectMapper.findAndRegisterModules();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        this.watchService = watchService;
        watchService.registerListener(this, Path.of(""));
        watchPath = watchService.getWatchPath();

        // read initial contents
        try {
            Files.walkFileTree(watchPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(@NonNullByDefault({}) Path file,
                        @NonNullByDefault({}) BasicFileAttributes attrs) throws IOException {
                    if (attrs.isRegularFile()) {
                        Path relativePath = watchPath.relativize(file);
                        processWatchEvent(CREATE, relativePath);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(@NonNullByDefault({}) Path file,
                        @NonNullByDefault({}) IOException exc) throws IOException {
                    logger.warn("Failed to process {}: {}", file.toAbsolutePath(), exc.getClass().getSimpleName());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.warn("Could not list YAML files in '{}', models might be missing: {}", watchPath, e.getMessage());
        }
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
        String pathString = path.toString();
        if (path.startsWith("automation") || !pathString.endsWith(".yaml")) {
            logger.trace("Ignored {}", fullPath);
            return;
        }

        // strip extension for model name
        String modelName = pathString.substring(0, pathString.lastIndexOf("."));

        try {
            if (kind == WatchService.Kind.DELETE) {
                logger.info("Removing YAML model {}", modelName);
                YamlModelWrapper removedModel = modelCache.remove(modelName);
                if (removedModel == null) {
                    return;
                }
                for (Map.Entry<String, List<JsonNode>> modelEntry : removedModel.getNodes().entrySet()) {
                    String elementName = modelEntry.getKey();
                    List<JsonNode> removedNodes = modelEntry.getValue();
                    if (!removedNodes.isEmpty()) {
                        getElementListeners(elementName).forEach(listener -> {
                            List removedElements = parseJsonNodes(removedNodes, listener.getElementClass());
                            listener.removedModel(modelName, removedElements);
                        });
                    }
                }
            } else if (!Files.isHidden(fullPath) && Files.isReadable(fullPath) && !Files.isDirectory(fullPath)) {
                if (kind == Kind.CREATE) {
                    logger.info("Adding YAML model {}", modelName);
                } else {
                    logger.info("Updating YAML model {}", modelName);
                }
                JsonNode fileContent = objectMapper.readTree(fullPath.toFile());

                // check version
                JsonNode versionNode = fileContent.get("version");
                if (versionNode == null || !versionNode.canConvertToInt()) {
                    logger.warn("Version is missing or not a number in model {}. Ignoring it.", modelName);
                    return;
                }
                int modelVersion = versionNode.asInt();
                if (modelVersion != DEFAULT_MODEL_VERSION) {
                    logger.warn("Model {} has version {}, but only version 1 is supported. Ignoring it.", modelName,
                            modelVersion);
                    return;
                }
                JsonNode readOnlyNode = fileContent.get("readOnly");
                boolean readOnly = readOnlyNode == null || readOnlyNode.asBoolean(false);

                YamlModelWrapper model = Objects.requireNonNull(
                        modelCache.computeIfAbsent(modelName, k -> new YamlModelWrapper(modelVersion, readOnly)));

                // get sub-elements
                Iterator<Map.Entry<String, JsonNode>> it = fileContent.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> element = it.next();
                    String elementName = element.getKey();
                    JsonNode node = element.getValue();
                    if (!node.isArray()) {
                        // all processable sub-elements are arrays
                        logger.trace("Element {} in model {} is not an array, ignoring it", elementName, modelName);
                        continue;
                    }

                    List<JsonNode> oldNodeElements = model.getNodes().getOrDefault(elementName, List.of());
                    List<JsonNode> newNodeElements = new ArrayList<>();
                    node.elements().forEachRemaining(newNodeElements::add);

                    for (YamlModelListener<?> elementListener : getElementListeners(elementName)) {
                        Class<? extends YamlElement> elementClass = elementListener.getElementClass();

                        Map<String, ? extends YamlElement> oldElements = listToMap(
                                parseJsonNodes(oldNodeElements, elementClass));
                        Map<String, ? extends YamlElement> newElements = listToMap(
                                parseJsonNodes(newNodeElements, elementClass));

                        List addedElements = newElements.values().stream()
                                .filter(e -> !oldElements.containsKey(e.getId())).toList();
                        List removedElements = oldElements.values().stream()
                                .filter(e -> !newElements.containsKey(e.getId())).toList();
                        List updatedElements = newElements.values().stream().filter(
                                e -> oldElements.containsKey(e.getId()) && !e.equals(oldElements.get(e.getId())))
                                .toList();

                        if (!addedElements.isEmpty()) {
                            elementListener.addedModel(modelName, addedElements);
                        }
                        if (!removedElements.isEmpty()) {
                            elementListener.removedModel(modelName, removedElements);
                        }
                        if (!updatedElements.isEmpty()) {
                            elementListener.updatedModel(modelName, updatedElements);
                        }
                    }

                    // replace cache
                    model.getNodes().put(elementName, newNodeElements);
                }
            } else {
                logger.trace("Ignored {}", fullPath);
            }
        } catch (IOException e) {
            logger.warn("Failed to process model {}: {}", modelName, e.getMessage());
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addYamlModelListener(YamlModelListener<? extends YamlElement> listener) {
        Class<? extends YamlElement> elementClass = listener.getElementClass();
        YamlElementName annotation = elementClass.getAnnotation(YamlElementName.class);
        if (annotation == null) {
            logger.warn("Class {} is missing the mandatory YamlElementName annotation. This is a bug.", elementClass);
            return;
        }
        String elementName = annotation.value();
        getElementListeners(elementName).add(listener);

        // iterate over all models and notify he new listener of already existing models with this type
        for (Map.Entry<String, YamlModelWrapper> model : modelCache.entrySet()) {
            String modelName = model.getKey();
            List<JsonNode> modelNodes = model.getValue().getNodes().get(elementName);
            if (modelNodes == null || modelNodes.isEmpty()) {
                continue;
            }
            List modelElements = parseJsonNodes(modelNodes, elementClass);
            listener.addedModel(modelName, modelElements);
        }
    }

    public void removeYamlModelListener(YamlModelListener<? extends YamlElement> listener) {
        elementListeners.values().forEach(list -> list.remove(listener));
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
        String elementName = annotation.value();
        YamlModelWrapper model = Objects.requireNonNull(
                modelCache.computeIfAbsent(modelName, k -> new YamlModelWrapper(DEFAULT_MODEL_VERSION, false)));
        if (model.isReadOnly()) {
            logger.warn("Modifying {} is not allowed, model is marked read-only", modelName);
            return;
        }

        List<JsonNode> modelNodes = model.getNodes().computeIfAbsent(elementName, k -> new CopyOnWriteArrayList<>());
        JsonNode newNode = objectMapper.convertValue(element, JsonNode.class);
        modelNodes.add(newNode);
        // notify listeners
        getElementListeners(elementName).forEach(l -> {
            List newElements = parseJsonNodes(List.of(newNode), l.getElementClass());
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
        String elementName = annotation.value();
        YamlModelWrapper model = modelCache.get(modelName);
        if (model == null) {
            logger.warn("Failed to remove {} from model {} because the model is not known.", element, modelName);
            return;
        }
        if (model.isReadOnly()) {
            logger.warn("Modifying {} is not allowed, model is marked read-only", modelName);
            return;
        }

        List<JsonNode> modelNodes = model.getNodes().get(elementName);
        if (modelNodes == null) {
            logger.warn("Failed to remove {} from model {} because type {} is not known in the model.", element,
                    modelName, elementName);
            return;
        }
        JsonNode toRemove = findNodeById(modelNodes, element.getClass(), element.getId());
        if (toRemove == null) {
            logger.warn("Failed to remove {} from model {} because element is not in model.", element, modelName);
            return;
        }
        modelNodes.remove(toRemove);
        // notify listeners
        getElementListeners(elementName).forEach(l -> {
            List newElements = parseJsonNodes(List.of(toRemove), l.getElementClass());
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
        String elementName = annotation.value();
        YamlModelWrapper model = modelCache.get(modelName);
        if (model == null) {
            logger.warn("Failed to update {} in model {} because the model is not known.", element, modelName);
            return;
        }
        if (model.isReadOnly()) {
            logger.warn("Modifying {} is not allowed, model is marked read-only", modelName);
            return;
        }

        List<JsonNode> modelNodes = model.getNodes().get(elementName);
        if (modelNodes == null) {
            logger.warn("Failed to update {} in model {} because type {} is not known in the model.", element,
                    modelName, elementName);
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
        getElementListeners(elementName).forEach(l -> {
            List newElements = parseJsonNodes(List.of(newNode), l.getElementClass());
            if (!newElements.isEmpty()) {
                l.updatedModel(modelName, newElements);
            }
        });

        writeModel(modelName);
    }

    private void writeModel(String modelName) {
        YamlModelWrapper model = modelCache.get(modelName);
        if (model == null) {
            logger.warn("Failed to write model {} to disk because it is not known.", modelName);
            return;
        }

        if (model.isReadOnly()) {
            logger.warn("Failed to write model {} to disk because it is marked as read-only.", modelName);
            return;
        }

        // create the model
        JsonNodeFactory nodeFactory = objectMapper.getNodeFactory();
        ObjectNode rootNode = nodeFactory.objectNode();

        rootNode.put("version", model.getVersion());
        rootNode.put("readOnly", model.isReadOnly());
        for (Map.Entry<String, List<JsonNode>> elementNodes : model.getNodes().entrySet()) {
            ArrayNode arrayNode = nodeFactory.arrayNode();
            elementNodes.getValue().forEach(arrayNode::add);
            rootNode.set(elementNodes.getKey(), arrayNode);
        }

        try {
            Path outFile = watchPath.resolve(modelName + ".yaml");
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

    private List<YamlModelListener<?>> getElementListeners(String elementName) {
        return Objects.requireNonNull(elementListeners.computeIfAbsent(elementName, k -> new CopyOnWriteArrayList<>()));
    }

    private <T extends YamlElement> @Nullable JsonNode findNodeById(List<JsonNode> nodes, Class<T> elementClass,
            String id) {
        return nodes.stream().filter(node -> {
            Optional<T> parsedNode = parseJsonNode(node, elementClass);
            return parsedNode.filter(yamlDTO -> id.equals(yamlDTO.getId())).isPresent();
        }).findAny().orElse(null);
    }

    private Map<String, ? extends YamlElement> listToMap(List<? extends YamlElement> elements) {
        return elements.stream().collect(Collectors.toMap(YamlElement::getId, e -> e));
    }

    private <T extends YamlElement> List<T> parseJsonNodes(List<JsonNode> nodes, Class<T> elementClass) {
        return nodes.stream().map(nE -> parseJsonNode(nE, elementClass)).flatMap(Optional::stream)
                .filter(YamlElement::isValid).toList();
    }

    private <T extends YamlElement> Optional<T> parseJsonNode(JsonNode node, Class<T> elementClass) {
        try {
            return Optional.of(objectMapper.treeToValue(node, elementClass));
        } catch (JsonProcessingException e) {
            logger.warn("Could not parse element {} to {}: {}", node, elementClass, e.getMessage());
            return Optional.empty();
        }
    }
}
