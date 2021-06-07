/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.io.rest.update.internal.factory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.rest.update.internal.enums.OperatingSystem;
import org.openhab.core.io.rest.update.internal.enums.PackageManager;
import org.openhab.core.io.rest.update.internal.updaters.BaseUpdater;
import org.openhab.core.io.rest.update.internal.updaters.DebianUpdater;
import org.openhab.core.io.rest.update.internal.updaters.MacUpdater;
import org.openhab.core.io.rest.update.internal.updaters.PacManUpdater;
import org.openhab.core.io.rest.update.internal.updaters.PortageUpdater;
import org.openhab.core.io.rest.update.internal.updaters.WindowsUpdater;
import org.openhab.core.io.rest.update.internal.updaters.YumUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UpdaterFactory} creates a xxxUpdater class based on the Operating System (and its respective package
 * manager if the Operating System is Linux).
 *
 * @author AndrewFG - initial contribution
 */
@NonNullByDefault
public class UpdaterFactory {
    // logger must be static because the class needs to log within static methods
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdaterFactory.class);

    private static final String PROPERTY_FILE = "/etc/os-release";
    private static final String[] PROPERTY_KEYS = new String[] { "ID_LIKE", "ID" };

    /**
     * Reads the properties in the '/etc/os-release' file and returns the package manager based on the 'ID_LIKE' or 'ID'
     * properties.
     *
     * @return
     */
    @SuppressWarnings("null")
    private static PackageManager getLinuxPackageManager() {
        try (InputStream stream = new FileInputStream(PROPERTY_FILE)) {
            Properties properties = new Properties();
            properties.load(stream);
            for (String propertyKey : PROPERTY_KEYS) {
                if (properties.containsKey(propertyKey)) {
                    if (properties.getProperty(propertyKey).contains("debian")) {
                        return PackageManager.DEBIAN_APT;
                    }
                    if (properties.getProperty(propertyKey).contains("fedora")) {
                        return PackageManager.REDHAT_YUM;
                    }
                    if (properties.getProperty(propertyKey).contains("gentoo")) {
                        return PackageManager.GENTOO_PORTAGE;
                    }
                }
            }
            LOGGER.debug("Property values ID_LIKE or ID not found");
        } catch (IOException e) {
            LOGGER.debug("Errror reading property file: {}, '{}'", PROPERTY_FILE, e.getMessage());
        }
        return PackageManager.UNKNOWN;
    }

    /**
     * Static method that returns an instance of an xxxUpdater class based on the Operating System (and its respective
     * package manager).
     *
     * @param targetVersion target version type to upgrade to (STABLE, MILESTONE, SNAPSHOT)
     * @param password system 'sudo' password on Linux systems
     * @param sleepTime number of seconds that scripts shall sleep while OpenHAB shuts down
     * @return an instance of the respective xxxUpdater class
     */
    public static @Nullable BaseUpdater newUpdater() {
        OperatingSystem opSys = OperatingSystem.getOperatingSystemVersion();
        switch (opSys) {
            case WINDOWS:
                return new WindowsUpdater();
            case MAC:
                return new MacUpdater();
            case UNIX:
                PackageManager pkgMan = getLinuxPackageManager();
                switch (pkgMan) {
                    case DEBIAN_APT:
                        return new DebianUpdater();
                    case REDHAT_YUM:
                        return new YumUpdater();
                    case GENTOO_PORTAGE:
                        return new PortageUpdater();
                    case ARCH_PACMAN:
                        return new PacManUpdater();
                    default:
                        LOGGER.debug("Updater not yet available for {} {}. => Please request.", opSys, pkgMan);
                }
            default:
                LOGGER.debug("Updater not yet available for {}. => Please request.", opSys);
        }
        return null;
    }
}
