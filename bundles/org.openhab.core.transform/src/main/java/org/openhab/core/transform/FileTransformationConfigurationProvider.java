/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.service.AbstractWatchService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link FileTransformationConfigurationProvider} implements a {@link TransformationConfigurationProvider} for
 * supporting configurations stored in configuration files
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(service = TransformationConfigurationProvider.class, immediate = true)
public class FileTransformationConfigurationProvider extends AbstractWatchService
        implements TransformationConfigurationProvider {
    private static final WatchEvent.Kind<?>[] WATCH_EVENTS = { StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY };
    private static final Set<String> IGNORED_EXTENSIONS = Set.of("txt");
    private static final Pattern FILENAME_PATTERN = Pattern
            .compile("(?<filename>.+?)(_(?<language>[a-z]{2}))?\\.(?<extension>[^.]*)$");
    private static final Path TRANSFORMATION_PATH = Path.of(OpenHAB.getConfigFolder(),
            TransformationService.TRANSFORM_FOLDER_NAME);

    private final Logger logger = LoggerFactory.getLogger(FileTransformationConfigurationProvider.class);

    private final Set<ProviderChangeListener<TransformationConfiguration>> listeners = ConcurrentHashMap.newKeySet();
    private final Map<Path, TransformationConfiguration> transformationConfigurations = new ConcurrentHashMap<>();
    private final Path transformationPath;

    public FileTransformationConfigurationProvider() {
        this(TRANSFORMATION_PATH);
    }

    // constructor package private used for testing
    FileTransformationConfigurationProvider(Path transformationPath) {
        super(transformationPath.toString());
        this.transformationPath = transformationPath;

        // read initial contents
        try {
            Files.walk(transformationPath).filter(Files::isRegularFile)
                    .forEach(f -> processPath(StandardWatchEventKinds.ENTRY_CREATE, f));
        } catch (IOException e) {
            logger.warn("Could not list files in '{}', transformation configurations might be missing: {}",
                    transformationPath, e.getMessage());
        }
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<TransformationConfiguration> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<TransformationConfiguration> listener) {
        listeners.remove(listener);
    }

    @Override
    public Collection<TransformationConfiguration> getAll() {
        return transformationConfigurations.values();
    }

    @Override
    protected boolean watchSubDirectories() {
        return true;
    }

    @Override
    protected WatchEvent.Kind<?> @Nullable [] getWatchEventKinds(Path directory) {
        return WATCH_EVENTS;
    }

    @Override
    protected void processWatchEvent(WatchEvent<?> event, WatchEvent.Kind<?> kind, Path path) {
        processPath(kind, path);
    }

    private void processPath(WatchEvent.Kind<?> kind, Path path) {
        if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
            TransformationConfiguration oldElement = transformationConfigurations.remove(path);
            if (oldElement != null) {
                logger.trace("Removed configuration from file '{}", path);
                listeners.forEach(listener -> listener.removed(this, oldElement));
            }
        } else if (Files.isRegularFile(path) && (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)
                || StandardWatchEventKinds.ENTRY_MODIFY.equals(kind))) {
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

                String content = new String(Files.readAllBytes(path));
                String uid = transformationPath.relativize(path).toString();

                TransformationConfiguration newElement = new TransformationConfiguration(uid, uid, fileExtension,
                        m.group("language"), content);
                TransformationConfiguration oldElement = transformationConfigurations.put(path, newElement);
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
}
