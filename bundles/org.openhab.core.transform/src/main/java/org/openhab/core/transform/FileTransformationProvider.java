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
package org.openhab.core.transform;

import static org.openhab.core.service.WatchService.Kind.CREATE;
import static org.openhab.core.service.WatchService.Kind.DELETE;
import static org.openhab.core.service.WatchService.Kind.MODIFY;
import static org.openhab.core.transform.Transformation.FUNCTION;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.service.WatchService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link FileTransformationProvider} implements a {@link TransformationProvider} for
 * supporting transformations stored in configuration files
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(service = TransformationProvider.class, immediate = true)
public class FileTransformationProvider implements WatchService.WatchEventListener, TransformationProvider {
    private static final Set<String> IGNORED_EXTENSIONS = Set.of("txt", "swp");
    private static final Pattern FILENAME_PATTERN = Pattern
            .compile("(?<filename>.+?)(_(?<language>[a-z]{2}))?\\.(?<extension>[^.]*)$");

    private final Logger logger = LoggerFactory.getLogger(FileTransformationProvider.class);

    private final Set<ProviderChangeListener<Transformation>> listeners = ConcurrentHashMap.newKeySet();
    private final Map<Path, Transformation> transformationConfigurations = new ConcurrentHashMap<>();
    private final Path transformationPath;
    private final WatchService watchService;

    @Activate
    public FileTransformationProvider(
            @Reference(target = WatchService.CONFIG_WATCHER_FILTER) WatchService watchService) {
        this.watchService = watchService;
        this.transformationPath = watchService.getWatchPath().resolve(TransformationService.TRANSFORM_FOLDER_NAME);

        watchService.registerListener(this, transformationPath);
        // read initial contents
        try (Stream<Path> files = Files.walk(transformationPath)) {
            files.filter(Files::isRegularFile).map(transformationPath::relativize).forEach(f -> processPath(CREATE, f));
        } catch (IOException e) {
            logger.warn("Could not list files in '{}', transformation configurations might be missing: {}",
                    transformationPath, e.getMessage());
        }
    }

    @Deactivate
    public void deactivate() {
        watchService.unregisterListener(this);
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<Transformation> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<Transformation> listener) {
        listeners.remove(listener);
    }

    @Override
    public Collection<Transformation> getAll() {
        return transformationConfigurations.values();
    }

    private void processPath(WatchService.Kind kind, Path path) {
        Path finalPath = transformationPath.resolve(path);
        if (kind == DELETE) {
            Transformation oldElement = transformationConfigurations.remove(path);
            if (oldElement != null) {
                logger.trace("Removed configuration from file '{}", path);
                listeners.forEach(listener -> listener.removed(this, oldElement));
            }
        } else if (Files.isRegularFile(finalPath) && ((kind == CREATE) || (kind == MODIFY))) {
            try {
                String fileName = path.getFileName().toString();
                Matcher m = FILENAME_PATTERN.matcher(fileName);
                if (!m.matches()) {
                    logger.debug("Skipping {} event for '{}' - no file extensions found or remaining filename empty",
                            kind, path);
                    return;
                }

                String fileExtension = m.group("extension");
                if (IGNORED_EXTENSIONS.contains(fileExtension)) {
                    logger.debug("Skipping {} event for '{}' - file extension '{}' is ignored", kind, path,
                            fileExtension);
                    return;
                }

                String content = new String(Files.readAllBytes(finalPath));
                String uid = path.toString();

                Transformation newElement = new Transformation(uid, uid, fileExtension, Map.of(FUNCTION, content));
                Transformation oldElement = transformationConfigurations.put(path, newElement);
                if (oldElement == null) {
                    logger.trace("Added new configuration from file '{}'", path);
                    listeners.forEach(listener -> listener.added(this, newElement));
                } else {
                    logger.trace("Updated new configuration from file '{}'", path);
                    listeners.forEach(listener -> listener.updated(this, oldElement, newElement));
                }
            } catch (IOException e) {
                logger.warn("Skipping {} event for '{}' - failed to read content: {}", kind, path, e.getMessage());
            }
        } else {
            logger.trace("Skipping {} event for '{}' - not a regular file", kind, path);
        }
    }

    @Override
    public void processWatchEvent(WatchService.Kind kind, Path path) {
        processPath(kind, path);
    }
}
