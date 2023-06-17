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
package org.openhab.core.semantics.model.yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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
    private final List<Path> watchPaths = new CopyOnWriteArrayList<>();
    private final ObjectMapper yamlReader;
    private final List<YamlModelListener<?>> listeners = new CopyOnWriteArrayList<>();
    private final Map<Path, List<? extends YamlElement>> objects = new ConcurrentHashMap<>();

    @Activate
    public YamlModelRepository(@Reference(target = WatchService.CONFIG_WATCHER_FILTER) WatchService watchService) {
        this.watchService = watchService;
        this.yamlReader = new ObjectMapper(new YAMLFactory());
        yamlReader.findAndRegisterModules();
    }

    @Deactivate
    public void deactivate() {
        objects.clear();
    }

    @Override
    public synchronized void processWatchEvent(Kind kind, Path path) {
        // path parameter is currently wrong ! It is a path in userdata folder
        // Temporary workaround: build a path in tags folder
        Path fixecPath = watchService.getWatchPath().resolve("tags").resolve(path.getFileName());
        logger.debug("processWatchEvent {} path={} fixecPath={}", kind, path.toFile().getAbsolutePath(),
                fixecPath.toFile().getAbsolutePath());
        YamlModelListener<?> listener = findModelListener(fixecPath);
        if (listener == null) {
            logger.warn("No YAML file consumer available for {}; file is ignored",
                    fixecPath.toFile().getAbsolutePath());
            return;
        }
        processWatchEvent(kind, fixecPath, listener);
    }

    private void processWatchEvent(Kind kind, Path path, YamlModelListener<?> listener) {
        logger.debug("processWatchEvent {} {} {}", kind, listener.getRootName(), path.toFile().getAbsolutePath());
        if (Files.isDirectory(path) || path.toFile().isHidden() || !path.toFile().getName().endsWith(".yaml")) {
            logger.debug("{} is ignored", path.toFile().getAbsolutePath());
            return;
        }
        Map<String, ? extends YamlElement> oldObjects;
        Map<String, ? extends YamlElement> newObjects;
        if (kind == WatchService.Kind.DELETE) {
            newObjects = Map.of();

            List<? extends YamlElement> oldListObjects = objects.remove(path);
            if (oldListObjects == null) {
                oldListObjects = List.of();
            }
            oldObjects = oldListObjects.stream().collect(Collectors.toMap(YamlElement::getId, obj -> obj));
        } else {
            YamlFile yamlData;
            try {
                yamlData = readYamlFile(path, listener.getFileClass());
            } catch (YamlParseException e) {
                logger.warn("Failed to parse Yaml file {} with DTO class {}: {}", path.toFile().getAbsolutePath(),
                        listener.getFileClass().getName(), e.getMessage());
                return;
            }
            List<? extends YamlElement> newListObjects = yamlData.getElements();
            newObjects = newListObjects.stream().collect(Collectors.toMap(YamlElement::getId, obj -> obj));

            List<? extends YamlElement> oldListObjects = objects.get(path);
            if (oldListObjects == null) {
                oldListObjects = List.of();
            }
            oldObjects = oldListObjects.stream().collect(Collectors.toMap(YamlElement::getId, obj -> obj));

            objects.put(path, newListObjects);
        }

        String modelName = path.toFile().getName();
        modelName = modelName.substring(0, modelName.indexOf(".yaml"));
        List<? extends YamlElement> listElts;
        listElts = oldObjects.entrySet().stream()
                .filter(entry -> entry.getValue().getClass().equals(listener.getElementClass())
                        && !newObjects.containsKey(entry.getKey()))
                .map(entry -> entry.getValue()).toList();
        if (!listElts.isEmpty()) {
            listener.removedModel(modelName, listElts);
        }

        listElts = newObjects.entrySet().stream()
                .filter(entry -> entry.getValue().getClass().equals(listener.getElementClass())
                        && !oldObjects.containsKey(entry.getKey()))
                .map(entry -> entry.getValue()).toList();
        if (!listElts.isEmpty()) {
            listener.addedModel(modelName, listElts);
        }

        // Object is ignored if unchanged
        listElts = newObjects.entrySet().stream()
                .filter(entry -> entry.getValue().getClass().equals(listener.getElementClass())
                        && oldObjects.containsKey(entry.getKey())
                        && !entry.getValue().equals(oldObjects.get(entry.getKey())))
                .map(entry -> entry.getValue()).toList();
        if (!listElts.isEmpty()) {
            listener.updatedModel(modelName, listElts);
        }
    }

    private @Nullable YamlModelListener<?> findModelListener(Path path) {
        for (YamlModelListener<?> listener : listeners) {
            if (path.startsWith(watchService.getWatchPath().resolve(listener.getRootName()))) {
                return listener;
            }
        }
        return null;
    }

    private YamlFile readYamlFile(Path path, Class<? extends YamlFile> dtoClass) throws YamlParseException {
        logger.debug("readYamlFile {} with {}", path.toFile().getAbsolutePath(), dtoClass.getName());
        try {
            YamlFile dto = yamlReader.readValue(path.toFile(), dtoClass);
            dto.checkValidity();
            return dto;
        } catch (IOException e) {
            throw new YamlParseException(e);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addYamlModelListener(YamlModelListener<?> listener) {
        logger.debug("addYamlModelListener {}", listener.getRootName());
        listeners.add(listener);

        Path watchPath = watchService.getWatchPath().resolve(listener.getRootName());
        watchPaths.add(watchPath);
        watchService.unregisterListener(this);
        watchService.registerListener(this, watchPaths);

        // Load all existing YAML files
        try (Stream<Path> stream = Files.walk(watchPath)) {
            stream.forEach(path -> processWatchEvent(Kind.CREATE, path, listener));
        } catch (IOException e) {
        }
    }

    protected void removeYamlModelListener(YamlModelListener<?> listener) {
        listeners.remove(listener);

        Path watchPath = watchService.getWatchPath().resolve(listener.getRootName());
        watchPaths.remove(watchPath);
        watchService.unregisterListener(this);
        if (!watchPaths.isEmpty()) {
            watchService.registerListener(this, watchPaths);
        }
    }
}
