/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.config.dispatch.internal;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;

import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.service.AbstractWatchService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * Watches file-system events and passes them to our {@link ConfigDispatcher}
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Stefan Triller - factored out this code from {@link ConfigDispatcher}
 *
 */
@Component(immediate = true)
public class ConfigDispatcherFileWatcher extends AbstractWatchService {

    /** The program argument name for setting the service config directory path */
    public static final String SERVICEDIR_PROG_ARGUMENT = "smarthome.servicedir";

    /** The default folder name of the configuration folder of services */
    public static final String SERVICES_FOLDER = "services";

    private ConfigDispatcher configDispatcher;

    public ConfigDispatcherFileWatcher() {
        super(getPathToWatch());
    }

    private static String getPathToWatch() {
        String progArg = System.getProperty(SERVICEDIR_PROG_ARGUMENT);
        if (progArg != null) {
            return ConfigConstants.getConfigFolder() + File.separator + progArg;
        } else {
            return ConfigConstants.getConfigFolder() + File.separator + SERVICES_FOLDER;
        }
    }

    @Override
    @Activate
    public void activate() {
        super.activate();
        configDispatcher.processConfigFile(getSourcePath().toFile());
    }

    @Deactivate
    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected boolean watchSubDirectories() {
        return false;
    }

    @Override
    protected Kind<?>[] getWatchEventKinds(Path subDir) {
        return new Kind<?>[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY };
    }

    @Override
    protected void processWatchEvent(WatchEvent<?> event, Kind<?> kind, Path path) {
        if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
            File f = path.toFile();
            if (!f.isHidden() && f.getName().endsWith(".cfg")) {
                configDispatcher.processConfigFile(f);
            }
        } else if (kind == ENTRY_DELETE) {
            // Detect if a service specific configuration file was removed. We want to
            // notify the service in this case with an updated empty configuration.
            File configFile = path.toFile();
            if (configFile.isHidden() || configFile.isDirectory() || !configFile.getName().endsWith(".cfg")) {
                return;
            }
            configDispatcher.fileRemoved(configFile.getAbsolutePath());
        }
    }

    @Reference
    public void setConfigDispatcher(ConfigDispatcher configDispatcher) {
        this.configDispatcher = configDispatcher;
    }

    public void unsetConfigDispatcher(ConfigDispatcher configDispatcher) {
        this.configDispatcher = null;
    }

}
