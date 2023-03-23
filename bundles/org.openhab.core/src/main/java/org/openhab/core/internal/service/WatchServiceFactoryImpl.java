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
package org.openhab.core.internal.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.OpenHAB;
import org.openhab.core.service.WatchService;
import org.openhab.core.service.WatchServiceFactory;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link WatchServiceFactoryImpl} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = WatchServiceFactory.class)
public class WatchServiceFactoryImpl implements WatchServiceFactory {
    private final Logger logger = LoggerFactory.getLogger(WatchServiceFactoryImpl.class);

    private final ConfigurationAdmin cm;

    @Activate
    public WatchServiceFactoryImpl(@Reference ConfigurationAdmin cm) {
        this.cm = cm;

        // make sure we start with a clean configuration.
        clearConfigurationAdmin();

        createWatchService(WatchService.CONFIG_WATCHER_NAME, Path.of(OpenHAB.getConfigFolder()));
    }

    @Deactivate
    public void deactivate() {
        clearConfigurationAdmin();
    }

    @Override
    public void createWatchService(String name, Path basePath) {
        try {
            String filter = "(&(name=" + name + ")" + "(service.factoryPid=" + WatchService.SERVICE_PID + "))";
            Configuration[] configurations = cm.listConfigurations(filter);

            if (configurations == null || configurations.length == 0) {
                Configuration config = cm.createFactoryConfiguration(WatchService.SERVICE_PID, "?");
                Dictionary<String, Object> map = new Hashtable<>();

                map.put("name", name);
                map.put("path", basePath.toString());
                config.update(map);
            } else {
                Configuration config = configurations[0];
                Dictionary<String, Object> map = config.getProperties();
                map.put("name", name);
                map.put("path", basePath.toString());
                config.update(map);
            }
        } catch (IOException | InvalidSyntaxException e1) {
            logger.error("Failed to create configuration with name `{}' and path '{}'", name, basePath);
        }
    }

    @Override
    public void removeWatchService(String name) {
        try {
            String filter = "(&(name=" + name + ")" + "(service.factoryPid=" + WatchService.SERVICE_PID + "))";
            Configuration[] configurations = this.cm.listConfigurations(filter);
            if (configurations != null) {
                configurations[0].delete();
            }
        } catch (IOException | InvalidSyntaxException e) {
            logger.error("Failed to remove configuration with name '{}", name);
        }
    }

    private void clearConfigurationAdmin() {
        try {
            String filter = "(service.factoryPid=" + WatchService.SERVICE_PID + ")";
            Configuration[] configurations = this.cm.listConfigurations(filter);
            if (configurations != null) {
                for (Configuration configuration : configurations) {
                    try {
                        configuration.delete();
                    } catch (IOException e) {
                        logger.error("Failed to remove configuration with name '{}",
                                configuration.getProperties().get("name"));
                    }
                }
            }
        } catch (IOException | InvalidSyntaxException e) {
            logger.error("Failed to remove services.");
        }
    }
}
