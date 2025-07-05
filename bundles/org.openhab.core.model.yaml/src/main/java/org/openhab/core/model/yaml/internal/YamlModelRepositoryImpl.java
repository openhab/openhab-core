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
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.BidiSetBag;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.model.yaml.internal.items.YamlItemDTO;
import org.openhab.core.model.yaml.internal.semantics.YamlSemanticTagDTO;
import org.openhab.core.model.yaml.internal.things.YamlThingDTO;
import org.openhab.core.model.yaml.internal.util.preprocessor.YamlPreprocessor;
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
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * @author Laurent Garnier - Map used instead of table
 * @author Laurent Garnier - Added basic version management
 * @author Laurent Garnier - Added method generateSyntaxFromElements + new parameters
 *         for method isValid
 * @author Jimmy Tanagra - Added Yaml preprocessor to support !include, variable substitutions, and packages
 */
@NonNullByDefault
@Component(immediate = true)
public class YamlModelRepositoryImpl implements WatchService.WatchEventListener, YamlModelRepository {
    private static final int DEFAULT_MODEL_VERSION = 1;
    private static final String VERSION = "version";
    private static final String READ_ONLY = "readOnly";
    private static final Set<String> KNOWN_ELEMENTS = Set.of( //
            // "version", "readOnly" are reserved keys
            // "variables" and "packages" are reserved elements for YamlPreprocessor
            getElementName(YamlSemanticTagDTO.class), // "tags"
            getElementName(YamlThingDTO.class), // "things"
            getElementName(YamlItemDTO.class) // "items"
    );

    private static final String UNWANTED_EXCEPTION_TEXT = "at [Source: UNKNOWN; byte offset: #UNKNOWN] ";

    private static final List<Path> WATCHED_PATHS = Stream.of("things", "items", "tags", "yaml").map(Path::of).toList();

    private final Logger logger = LoggerFactory.getLogger(YamlModelRepositoryImpl.class);

    private final WatchService watchService;
    private final Path mainWatchPath;
    private final ObjectMapper objectMapper;

    private final Map<String, List<YamlModelListener<?>>> elementListeners = new ConcurrentHashMap<>();
    // all model nodes, ordered by model name (full path as string) and type
    private final Map<String, YamlModelWrapper> modelCache = new ConcurrentHashMap<>();

    // keep track of include files so we can reload the main model when they change
    // Bidirectional Map of modelName <-> include path by this model
    private final BidiSetBag<String, Path> modelIncludes = new BidiSetBag<>();
    private boolean initializing = true;

    @Activate
    public YamlModelRepositoryImpl(@Reference(target = WatchService.CONFIG_WATCHER_FILTER) WatchService watchService) {
        YAMLFactory yamlFactory = YAMLFactory.builder() //
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER) // omit "---" at file start
                .disable(YAMLGenerator.Feature.SPLIT_LINES) // do not split long lines
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR) // indent arrays
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES) // use quotes only where necessary
                .enable(YAMLParser.Feature.PARSE_BOOLEAN_LIKE_WORDS_AS_STRINGS) // do not parse ON/OFF/... as booleans
                .build();
        this.objectMapper = new ObjectMapper(yamlFactory);
        objectMapper.findAndRegisterModules();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

        this.watchService = watchService;
        this.mainWatchPath = watchService.getWatchPath();

        watchService.registerListener(this, WATCHED_PATHS);

        // read initial contents
        WATCHED_PATHS.forEach(watchPath -> {
            Path fullPath = mainWatchPath.resolve(watchPath);
            try {
                Files.walkFileTree(fullPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(@NonNullByDefault({}) Path file,
                            @NonNullByDefault({}) BasicFileAttributes attrs) throws IOException {
                        if (attrs.isRegularFile()) {
                            processWatchEvent(CREATE, file);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(@NonNullByDefault({}) Path file,
                            @NonNullByDefault({}) IOException exc) throws IOException {
                        logger.warn("Failed to process {}: {}", file.toAbsolutePath(), exc.getMessage());
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                logger.warn("Could not list YAML files in '{}', models might be missing: {}", watchPath,
                        e.getMessage());
            }
        });
        initializing = false;
    }

    @Deactivate
    public void deactivate() {
        watchService.unregisterListener(this);
        modelIncludes.clear();
    }

    // The method is "synchronized" to avoid concurrent files processing
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public synchronized void processWatchEvent(Kind kind, Path fullPath) {
        Path relativePath = mainWatchPath.relativize(fullPath);
        String modelName = relativePath.toString();

        // always clear the list of includes if it's a model
        // if it loads correctly, it will be re-populated
        modelIncludes.removeKey(modelName);

        // check here because include files can have any extension
        if (!initializing && processIncludeFile(kind, fullPath)) {
            return;
        }

        if ((!modelName.endsWith(".yaml") && !modelName.endsWith(".yml")) || modelName.endsWith(".inc.yaml")
                || modelName.endsWith(".inc.yml")) {
            logger.trace("Ignored {}", fullPath);
            return;
        }

        try {
            if (kind == WatchService.Kind.DELETE) {
                removeModel(modelName);
            } else if (!Files.isHidden(fullPath) && Files.isReadable(fullPath) && !Files.isDirectory(fullPath)) {
                Object yamlObject = YamlPreprocessor.load(fullPath, includePath -> {
                    modelIncludes.put(modelName, includePath);
                });
                JsonNode fileContent = objectMapper.valueToTree(yamlObject);

                // check version
                JsonNode versionNode = fileContent.get(VERSION);
                if (versionNode == null || !versionNode.canConvertToInt()) {
                    logger.warn("Version is missing or not a number in model {}. Ignoring it.", modelName);
                    removeModel(modelName);
                    return;
                }
                int modelVersion = versionNode.asInt();
                if (modelVersion < 1 || modelVersion > DEFAULT_MODEL_VERSION) {
                    logger.warn(
                            "Model {} has version {}, but only versions between 1 and {} are supported. Ignoring it.",
                            modelName, modelVersion, DEFAULT_MODEL_VERSION);
                    removeModel(modelName);
                    return;
                }
                if (kind == Kind.CREATE) {
                    logger.info("Adding YAML model {}", modelName);
                } else {
                    logger.info("Updating YAML model {}", modelName);
                }
                JsonNode readOnlyNode = fileContent.get(READ_ONLY);
                boolean readOnly = readOnlyNode == null || readOnlyNode.asBoolean(false);

                YamlModelWrapper model = Objects.requireNonNull(
                        modelCache.computeIfAbsent(modelName, k -> new YamlModelWrapper(modelVersion, readOnly)));

                List<String> newElementNames = new ArrayList<>();
                // get sub-elements
                Iterator<Map.Entry<String, JsonNode>> it = fileContent.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> element = it.next();
                    String elementName = element.getKey();
                    if (elementName.equals(VERSION) || elementName.equals(READ_ONLY)) {
                        continue;
                    }

                    newElementNames.add(elementName);
                    JsonNode node = element.getValue();

                    if (!node.isContainerNode() || node.isArray()) {
                        // all processable sub-elements are container nodes (not array)
                        logger.warn("Element {} in model {} is not a container object, ignoring it", elementName,
                                modelName);
                        if (getElementName(YamlSemanticTagDTO.class).equals(elementName) && node.isArray()) {
                            logger.warn(
                                    "Your YAML model {} contains custom tags with an old and now unsupported syntax. An upgrade of this model is required to upgrade to the new syntax. This can be done by running the upgrade tool.",
                                    modelName);
                        }
                        continue;
                    }
                    JsonNode newNodeElements = node;

                    JsonNode oldNodeElements = model.getNodes().get(elementName);

                    for (YamlModelListener<?> elementListener : getElementListeners(elementName, modelVersion)) {
                        Class<? extends YamlElement> elementClass = elementListener.getElementClass();

                        List<String> errors = new ArrayList<>();
                        List<String> warnings = new ArrayList<>();

                        Map<String, ? extends YamlElement> oldElements = listToMap(
                                parseJsonMapNode(oldNodeElements, elementClass, null, null));
                        Map<String, ? extends YamlElement> newElements = listToMap(
                                parseJsonMapNode(newNodeElements, elementClass, errors, warnings));

                        errors.forEach(error -> {
                            logger.warn("YAML model {}: {}", modelName, error);
                        });
                        warnings.forEach(warning -> {
                            logger.info("YAML model {}: {}", modelName, warning);
                        });

                        List addedElements = newElements.values().stream()
                                .filter(e -> !oldElements.containsKey(e.getId())).toList();
                        List removedElements = oldElements.values().stream()
                                .filter(e -> !newElements.containsKey(e.getId())).toList();
                        List updatedElements = newElements.values().stream().filter(
                                e -> oldElements.containsKey(e.getId()) && !e.equals(oldElements.get(e.getId())))
                                .toList();

                        if (elementListener.isDeprecated()
                                && (!addedElements.isEmpty() || !updatedElements.isEmpty())) {
                            logger.warn(
                                    "Element {} in model {} version {} is still supported but deprecated, please consider migrating your model to a more recent version",
                                    elementName, modelName, modelVersion);
                        }

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

                // remove removed elements
                model.getNodes().entrySet().removeIf(e -> {
                    String elementName = e.getKey();
                    if (newElementNames.contains(elementName)) {
                        return false;
                    }

                    JsonNode removedNode = e.getValue();
                    getElementListeners(elementName, modelVersion).forEach(listener -> {
                        List removedElements = parseJsonMapNode(removedNode, listener.getElementClass(), null, null);
                        listener.removedModel(modelName, removedElements);
                    });
                    return true;
                });

                checkElementNames(modelName, model);
            } else {
                logger.trace("Ignored {}", fullPath);
            }
        } catch (IOException e) {
            logger.warn("Failed to process model {}: {}", modelName, e.getMessage());
        }
    }

    private boolean processIncludeFile(Kind kind, Path fullPath) {
        boolean logged = false;

        Set<String> dependingModels = modelIncludes.getKeys(fullPath);

        if (dependingModels.isEmpty()) {
            return false;
        }

        logger.info("An include file '{}' was {}", fullPath, switch (kind) {
            case WatchService.Kind.CREATE -> "created";
            case WatchService.Kind.DELETE -> "deleted";
            case WatchService.Kind.MODIFY -> "modified";
            default -> "unknown";
        });

        dependingModels.forEach(modelName -> {
            Path modelPath = mainWatchPath.resolve(modelName);
            try {
                // reprocess the model that depends on this include file
                processWatchEvent(WatchService.Kind.MODIFY, modelPath);
            } catch (Exception e) {
                logger.warn("Failed to reprocess model {} after include file change: {}", modelName, e.getMessage());
            }
        });

        return true;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void removeModel(String modelName) {
        YamlModelWrapper removedModel = modelCache.remove(modelName);
        if (removedModel == null) {
            return;
        }
        logger.info("Removing YAML model {}", modelName);
        modelIncludes.removeKey(modelName);

        int version = removedModel.getVersion();
        for (Map.Entry<String, @Nullable JsonNode> modelEntry : removedModel.getNodes().entrySet()) {
            String elementName = modelEntry.getKey();
            JsonNode removedMapNode = modelEntry.getValue();
            if (removedMapNode != null) {
                getElementListeners(elementName, version).forEach(listener -> {
                    List removedElements = parseJsonMapNode(removedMapNode, listener.getElementClass(), null, null);
                    listener.removedModel(modelName, removedElements);
                });
            }
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addYamlModelListener(YamlModelListener<? extends YamlElement> listener) {
        Class<? extends YamlElement> elementClass = listener.getElementClass();
        String elementName = getElementName(elementClass);
        Objects.requireNonNull(elementListeners.computeIfAbsent(elementName, k -> new CopyOnWriteArrayList<>()))
                .add(listener);

        // iterate over all models and notify the new listener of already existing models with this type
        modelCache.forEach((modelName, model) -> {
            int modelVersion = model.getVersion();
            if (!listener.isVersionSupported(modelVersion)) {
                return;
            }
            if (listener.isDeprecated()) {
                logger.warn(
                        "Element {} in model {} version {} is still supported but deprecated, please consider migrating your model to a more recent version",
                        elementName, modelName, modelVersion);
            }
            JsonNode modelMapNode = model.getNodes().get(elementName);
            if (modelMapNode == null) {
                return;
            }
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            List modelElements = parseJsonMapNode(modelMapNode, elementClass, errors, warnings);
            errors.forEach(error -> {
                logger.warn("YAML model {}: {}", modelName, error);
            });
            warnings.forEach(warning -> {
                logger.info("YAML model {}: {}", modelName, warning);
            });
            listener.addedModel(modelName, modelElements);
            checkElementNames(modelName, model);
        });
    }

    public void removeYamlModelListener(YamlModelListener<? extends YamlElement> listener) {
        String elementName = getElementName(listener.getElementClass());
        elementListeners.computeIfPresent(elementName, (k, v) -> {
            v.remove(listener);
            return v.isEmpty() ? null : v;
        });
    }

    private void checkElementNames(String modelName, YamlModelWrapper model) {
        Set<String> elementListenerNames = elementListeners.keySet();
        if (elementListenerNames.containsAll(KNOWN_ELEMENTS)) {
            Set<String> modelElementNames = model.getNodes().keySet();
            modelElementNames.stream().filter(e -> !KNOWN_ELEMENTS.contains(e)).forEach(unknownElement -> {
                logger.warn("Element '{}' in model {} is unknown.", unknownElement, modelName);
            });
        }
    }

    private static String getElementName(Class<? extends YamlElement> elementClass) {
        YamlElementName annotation = elementClass.getAnnotation(YamlElementName.class);
        if (annotation == null) {
            throw new IllegalStateException("Class " + elementClass.getName()
                    + " is missing the mandatory YamlElementName annotation. This is a bug.");
        }
        return annotation.value();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addElementToModel(String modelName, YamlElement element) {
        String elementName = getElementName(element.getClass());
        String id = element.getId();
        YamlModelWrapper model = Objects.requireNonNull(
                modelCache.computeIfAbsent(modelName, k -> new YamlModelWrapper(DEFAULT_MODEL_VERSION, false)));
        if (model.isReadOnly()) {
            logger.warn("Modifying {} is not allowed, model is marked read-only", modelName);
            return;
        }

        JsonNode mapAddedNode = objectMapper.valueToTree(Map.of(id, element.cloneWithoutId()));
        JsonNode modelMapNode = model.getNodes().get(elementName);
        if (modelMapNode == null) {
            model.getNodes().put(elementName, mapAddedNode);
        } else {
            JsonNode newNode = objectMapper.convertValue(element.cloneWithoutId(), JsonNode.class);
            ((ObjectNode) modelMapNode).set(id, newNode);
        }
        // notify listeners
        for (YamlModelListener<?> l : getElementListeners(elementName, model.getVersion())) {
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            List newElements = parseJsonMapNode(mapAddedNode, l.getElementClass(), errors, warnings);
            errors.forEach(error -> {
                logger.warn("YAML model {}: {}", modelName, error);
            });
            warnings.forEach(warning -> {
                logger.info("YAML model {}: {}", modelName, warning);
            });
            if (!newElements.isEmpty()) {
                l.addedModel(modelName, newElements);
            }
        }

        writeModel(modelName);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void removeElementFromModel(String modelName, YamlElement element) {
        String elementName = getElementName(element.getClass());
        String id = element.getId();
        YamlModelWrapper model = modelCache.get(modelName);
        if (model == null) {
            logger.warn("Failed to remove {} from model {} because the model is not known.", element, modelName);
            return;
        }
        if (model.isReadOnly()) {
            logger.warn("Modifying {} is not allowed, model is marked read-only", modelName);
            return;
        }

        JsonNode modelMapNode = model.getNodes().get(elementName);
        if (modelMapNode == null) {
            logger.warn("Failed to remove {} from model {} because type {} is not known in the model.", element,
                    modelName, elementName);
            return;
        }
        if (!modelMapNode.has(id)) {
            logger.warn("Failed to remove {} from model {} because element is not in model.", element, modelName);
            return;
        }
        ((ObjectNode) modelMapNode).remove(id);
        JsonNode mapRemovedNode = objectMapper.valueToTree(Map.of(id, element.cloneWithoutId()));
        // notify listeners
        for (YamlModelListener<?> l : getElementListeners(elementName, model.getVersion())) {
            List oldElements = parseJsonMapNode(mapRemovedNode, l.getElementClass(), null, null);
            if (!oldElements.isEmpty()) {
                l.removedModel(modelName, oldElements);
            }
        }

        writeModel(modelName);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void updateElementInModel(String modelName, YamlElement element) {
        String elementName = getElementName(element.getClass());
        String id = element.getId();
        YamlModelWrapper model = modelCache.get(modelName);
        if (model == null) {
            logger.warn("Failed to update {} in model {} because the model is not known.", element, modelName);
            return;
        }
        if (model.isReadOnly()) {
            logger.warn("Modifying {} is not allowed, model is marked read-only", modelName);
            return;
        }

        JsonNode modelMapNode = model.getNodes().get(elementName);
        if (modelMapNode == null) {
            logger.warn("Failed to update {} in model {} because type {} is not known in the model.", element,
                    modelName, elementName);
            return;
        }
        if (!modelMapNode.has(id)) {
            logger.warn("Failed to update {} in model {} because element is not in model.", element, modelName);
            return;
        }
        JsonNode newNode = objectMapper.convertValue(element.cloneWithoutId(), JsonNode.class);
        ((ObjectNode) modelMapNode).set(id, newNode);
        JsonNode mapUpdatedNode = objectMapper.valueToTree(Map.of(id, element.cloneWithoutId()));
        // notify listeners
        for (YamlModelListener<?> l : getElementListeners(elementName, model.getVersion())) {
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            List newElements = parseJsonMapNode(mapUpdatedNode, l.getElementClass(), errors, warnings);
            errors.forEach(error -> {
                logger.warn("YAML model {}: {}", modelName, error);
            });
            warnings.forEach(warning -> {
                logger.info("YAML model {}: {}", modelName, warning);
            });
            if (!newElements.isEmpty()) {
                l.updatedModel(modelName, newElements);
            }
        }

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

        rootNode.put(VERSION, model.getVersion());
        rootNode.put(READ_ONLY, model.isReadOnly());
        for (Map.Entry<String, @Nullable JsonNode> elementNodes : model.getNodes().entrySet()) {
            if (elementNodes.getValue() != null) {
                rootNode.set(elementNodes.getKey(), elementNodes.getValue());
            }
        }

        try {
            Path outFile = mainWatchPath.resolve(modelName);
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

    @Override
    public void generateSyntaxFromElements(OutputStream out, List<YamlElement> elements) {
        // create the model
        JsonNodeFactory nodeFactory = objectMapper.getNodeFactory();
        ObjectNode rootNode = nodeFactory.objectNode();

        rootNode.put(VERSION, DEFAULT_MODEL_VERSION);

        // First separate elements per type
        Map<String, List<YamlElement>> elementsPerTypes = new HashMap<>();
        elements.forEach(element -> {
            String elementName = getElementName(element.getClass());
            List<YamlElement> elts = elementsPerTypes.get(elementName);
            if (elts == null) {
                elts = new ArrayList<>();
                elementsPerTypes.put(elementName, elts);
            }
            elts.add(element);
        });
        // Generate one entry for each element type
        elementsPerTypes.entrySet().forEach(entry -> {
            Map<String, YamlElement> mapElts = new LinkedHashMap<>();
            entry.getValue().forEach(elt -> {
                mapElts.put(elt.getId(), elt.cloneWithoutId());
            });
            rootNode.set(entry.getKey(), objectMapper.valueToTree(mapElts));
        });

        try {
            objectMapper.writeValue(out, rootNode);
        } catch (IOException e) {
            logger.warn("Failed to serialize model: {}", e.getMessage());
        }
    }

    private List<YamlModelListener<?>> getElementListeners(String elementName) {
        return elementListeners.getOrDefault(elementName, List.of());
    }

    private List<YamlModelListener<?>> getElementListeners(String elementName, int version) {
        return getElementListeners(elementName).stream().filter(l -> l.isVersionSupported(version)).toList();
    }

    private Map<String, ? extends YamlElement> listToMap(List<? extends YamlElement> elements) {
        return elements.stream().collect(Collectors.toMap(YamlElement::getId, e -> e));
    }

    private <T extends YamlElement> List<T> parseJsonMapNode(@Nullable JsonNode mapNode, Class<T> elementClass,
            @Nullable List<String> errors, @Nullable List<String> warnings) {
        List<T> elements = new ArrayList<>();
        if (mapNode != null) {
            Iterator<String> it = mapNode.fieldNames();
            while (it.hasNext()) {
                String id = it.next();
                @Nullable
                T elt = null;
                JsonNode node = mapNode.get(id);
                if (node.isEmpty()) {
                    try {
                        elt = elementClass.getDeclaredConstructor().newInstance();
                        elt.setId(id);
                    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                        if (errors != null) {
                            errors.add("could not create new instance of %s".formatted(elementClass.getSimpleName()));
                        }
                    }
                } else {
                    try {
                        elt = objectMapper.treeToValue(node, elementClass);
                        elt.setId(id);
                    } catch (JsonProcessingException e) {
                        if (errors != null) {
                            String msg = e.getMessage();
                            errors.add("could not parse element %s to %s: %s".formatted(node.toPrettyString(),
                                    elementClass.getSimpleName(),
                                    msg == null ? "" : msg.replace(UNWANTED_EXCEPTION_TEXT, "")));
                        }
                    }
                }
                if (elt != null && elt.isValid(errors, warnings)) {
                    elements.add(elt);
                }
            }
        }
        return elements;
    }
}
