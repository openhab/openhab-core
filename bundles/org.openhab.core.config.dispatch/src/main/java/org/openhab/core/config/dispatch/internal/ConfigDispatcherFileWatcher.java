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
package org.openhab.core.config.dispatch.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.OpenHAB;
import org.openhab.core.service.WatchService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches file-system events and passes them to our {@link ConfigDispatcher}
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Stefan Triller - factored out this code from {@link ConfigDispatcher}
 */
@Component(immediate = true)
@NonNullByDefault
public class ConfigDispatcherFileWatcher implements WatchService.WatchEventListener {
    public static final String SERVICEDIR_PROG_ARGUMENT = "openhab.servicedir";

    /** The default folder name of the configuration folder of services */
    public static final String SERVICES_FOLDER = "services";
    private final Logger logger = LoggerFactory.getLogger(ConfigDispatcherFileWatcher.class);

    private final ConfigDispatcher configDispatcher;
    private final WatchService watchService;

    @Activate
    public ConfigDispatcherFileWatcher(final @Reference ConfigDispatcher configDispatcher,
            final @Reference(target = WatchService.CONFIG_WATCHER_FILTER) WatchService watchService) {
        this.configDispatcher = configDispatcher;

        String servicesFolder = System.getProperty(SERVICEDIR_PROG_ARGUMENT, SERVICES_FOLDER);

        this.watchService = watchService;

        watchService.registerListener(this, Path.of(servicesFolder), false);
        configDispatcher.processConfigFile(Path.of(OpenHAB.getConfigFolder(), servicesFolder).toFile());
    }

    @Deactivate
    public void deactivate() {
        watchService.unregisterListener(this);
    }

    @Override
    public void processWatchEvent(WatchService.Kind kind, Path path) {
        Path fullPath = watchService.getWatchPath().resolve(SERVICES_FOLDER).resolve(path);
        try {
            if (kind == WatchService.Kind.CREATE || kind == WatchService.Kind.MODIFY) {
                if (!Files.isHidden(fullPath) && fullPath.toString().endsWith(".cfg")) {
                    configDispatcher.processConfigFile(fullPath.toFile());
                }
            } else if (kind == WatchService.Kind.DELETE) {
                // Detect if a service specific configuration file was removed. We want to
                // notify the service in this case with an updated empty configuration.
                if (Files.isHidden(fullPath) || Files.isDirectory(fullPath) || !fullPath.toString().endsWith(".cfg")) {
                    return;
                }
                configDispatcher.fileRemoved(fullPath.toString());
            }
        } catch (IOException e) {
            logger.error("Failed to process watch event {} for {}", kind, path, e);
        }
    }
}
