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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.model.yaml.AbstractYamlFile;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.model.yaml.YamlParseException;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * The {@link YamlModelRepository} is an OSGi service, that encapsulates all YAML file processing
 * including file monitoring to detect created, updated and removed YAML configuration files.
 * Data processed from these files are consumed by registered OSGi services that implement {@link YamlModelListener}.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class YamlModelRepository implements WatchService.WatchEventListener {

    private final Logger logger = LoggerFactory.getLogger(YamlModelRepository.class);

    private final WatchService watchService;
    private final Path watchPath;
    private final ObjectMapper yamlReader;

    private final Map<String, List<YamlModelListener<?>>> listeners = new ConcurrentHashMap<>();
    private final Map<Path, List<? extends YamlElement>> objects = new ConcurrentHashMap<>();

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
        String dirName = path.subpath(0, 1).toString();

        if (Files.isDirectory(fullPath) || fullPath.toFile().isHidden() || !fullPath.toString().endsWith(".yaml")) {
            logger.trace("Ignored {}", fullPath);
            return;
        }

        getListeners(dirName).forEach(listener -> processWatchEvent(dirName, kind, fullPath, listener));
    }

    private void processWatchEvent(String dirName, Kind kind, Path fullPath, YamlModelListener<?> listener) {
        logger.debug("processWatchEvent dirName={} kind={} fullPath={} listener={}", dirName, kind, fullPath,
                listener.getClass().getSimpleName());
        Map<String, ? extends YamlElement> oldObjects;
        Map<String, ? extends YamlElement> newObjects;
        if (kind == WatchService.Kind.DELETE) {
            newObjects = Map.of();

            List<? extends YamlElement> oldListObjects = objects.remove(fullPath);
            if (oldListObjects == null) {
                oldListObjects = List.of();
            }
            oldObjects = oldListObjects.stream().collect(Collectors.toMap(YamlElement::getId, obj -> obj));
        } else {
            AbstractYamlFile yamlData;
            try {
                yamlData = readYamlFile(fullPath, listener.getFileClass());
            } catch (YamlParseException e) {
                logger.warn("Failed to parse Yaml file {} with DTO class {}: {}", fullPath,
                        listener.getFileClass().getName(), e.getMessage());
                return;
            }
            List<? extends YamlElement> newListObjects = yamlData.getElements();
            newObjects = newListObjects.stream().collect(Collectors.toMap(YamlElement::getId, obj -> obj));

            List<? extends YamlElement> oldListObjects = objects.get(fullPath);
            if (oldListObjects == null) {
                oldListObjects = List.of();
            }
            oldObjects = oldListObjects.stream().collect(Collectors.toMap(YamlElement::getId, obj -> obj));

            objects.put(fullPath, newListObjects);
        }

        String modelName = fullPath.toFile().getName();
        modelName = modelName.substring(0, modelName.indexOf(".yaml"));
        List<? extends YamlElement> listElements;
        listElements = oldObjects.entrySet().stream()
                .filter(entry -> entry.getValue().getClass().equals(listener.getElementClass())
                        && !newObjects.containsKey(entry.getKey()))
                .map(Map.Entry::getValue).toList();
        if (!listElements.isEmpty()) {
            listener.removedModel(modelName, listElements);
        }

        listElements = newObjects.entrySet().stream()
                .filter(entry -> entry.getValue().getClass().equals(listener.getElementClass())
                        && !oldObjects.containsKey(entry.getKey()))
                .map(Map.Entry::getValue).toList();
        if (!listElements.isEmpty()) {
            listener.addedModel(modelName, listElements);
        }

        // Object is ignored if unchanged
        listElements = newObjects.entrySet().stream()
                .filter(entry -> entry.getValue().getClass().equals(listener.getElementClass())
                        && oldObjects.containsKey(entry.getKey())
                        && !entry.getValue().equals(oldObjects.get(entry.getKey())))
                .map(Map.Entry::getValue).toList();
        if (!listElements.isEmpty()) {
            listener.updatedModel(modelName, listElements);
        }
    }

    private AbstractYamlFile readYamlFile(Path path, Class<? extends AbstractYamlFile> dtoClass)
            throws YamlParseException {
        logger.info("Loading model '{}'", path.toFile().getName());
        logger.debug("readYamlFile {} with {}", path.toFile().getAbsolutePath(), dtoClass.getName());
        try {
            AbstractYamlFile dto = yamlReader.readValue(path.toFile(), dtoClass);
            if (!dto.isValid()) {
                throw new YamlParseException("The file is not valid, some checks failed!");
            }
            return dto;
        } catch (IOException e) {
            throw new YamlParseException(e);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addYamlModelListener(YamlModelListener<?> listener) {
        String dirName = listener.getRootName();
        logger.debug("Adding model listener for {}", dirName);
        getListeners(dirName).add(listener);

        // Load all existing YAML files
        try (Stream<Path> stream = Files.walk(watchPath.resolve(dirName))) {
            stream.forEach(path -> {
                if (!Files.isDirectory(path) && !path.toFile().isHidden() && path.toString().endsWith(".yaml")) {
                    processWatchEvent(dirName, Kind.CREATE, path, listener);
                }
            });
        } catch (IOException ignored) {
        }
    }

    protected void removeYamlModelListener(YamlModelListener<?> listener) {
        String dirName = listener.getRootName();
        logger.debug("Removing model listener for {}", dirName);
        getListeners(dirName).remove(listener);
    }

    private List<YamlModelListener<?>> getListeners(String dirName) {
        return Objects.requireNonNull(listeners.computeIfAbsent(dirName, k -> new CopyOnWriteArrayList<>()));
    }
}
