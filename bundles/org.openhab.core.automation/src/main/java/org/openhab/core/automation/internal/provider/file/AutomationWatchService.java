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
package org.openhab.core.automation.internal.provider.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.service.WatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an implementation of {@link WatchService.WatchEventListener} which is responsible for tracking file
 * system changes.
 * <p>
 * It provides functionality for tracking {@link #watchingDir} changes to import or remove the automation objects.
 *
 * @author Ana Dimova - Initial contribution
 * @author Arne Seime - Fixed watch event handling
 */
@SuppressWarnings("rawtypes")
@NonNullByDefault
public class AutomationWatchService implements WatchService.WatchEventListener {

    private final WatchService watchService;
    private final Path watchingDir;
    private AbstractFileProvider provider;
    private final Logger logger = LoggerFactory.getLogger(AutomationWatchService.class);

    public AutomationWatchService(AbstractFileProvider provider, WatchService watchService, String watchingDir) {
        this.watchService = watchService;
        this.watchingDir = Path.of(watchingDir);
        this.provider = provider;
    }

    public void activate() {
        watchService.registerListener(this, watchingDir);
    }

    public void deactivate() {
        watchService.unregisterListener(this);
    }

    public Path getSourcePath() {
        return watchingDir;
    }

    @Override
    public void processWatchEvent(WatchService.Kind kind, Path path) {
        Path fullPath = watchingDir.resolve(path);
        try {
            if (kind == WatchService.Kind.DELETE) {
                provider.removeResources(fullPath.toFile());
            } else if (!Files.isHidden(fullPath)
                    && (kind == WatchService.Kind.CREATE || kind == WatchService.Kind.MODIFY)) {
                provider.importResources(fullPath.toFile());
            }
        } catch (IOException e) {
            logger.error("Failed to process automation watch event {} for \"{}\": {}", kind, fullPath, e.getMessage());
            logger.trace("", e);
        }
    }
}
