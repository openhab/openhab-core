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
package org.openhab.core.automation.internal.provider.file;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.service.WatchService;

/**
 * This class is an implementation of {@link AbstractWatchService} which is responsible for tracking changes in file
 * system by Java WatchService.
 * <p>
 * It provides functionality for tracking {@link #watchingDir} changes to import or remove the automation objects.
 *
 * @author Ana Dimova - Initial contribution
 */
@SuppressWarnings("rawtypes")
@NonNullByDefault
public class AutomationWatchService implements WatchService.WatchEventListener {

    private final WatchService watchService;
    private final Path watchingDir;
    private AbstractFileProvider provider;

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
        File file = path.toFile();
        if (!file.isHidden()) {
            if (kind == WatchService.Kind.DELETE) {
                provider.removeResources(file);
            } else if (file.canRead()) {
                provider.importResources(file);
            }
        }
    }
}
