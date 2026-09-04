/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.tools.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.tools.Upgrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DialogRegistrationStorageUpgrader} renames the storage file for DialogRegistration.
 *
 * @since 5.3.0
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class DialogRegistrationStorageUpgrader implements Upgrader {
    private final Logger logger = LoggerFactory.getLogger(DialogRegistrationStorageUpgrader.class);

    private static final String TARGET_VERSION = "5.3.0";

    @Override
    public String getName() {
        return "dialogRegistrationStorageRename";
    }

    @Override
    public String getDescription() {
        return "Rename DialogRegistration JSON storage file and backups to new package structure";
    }

    @Override
    public @Nullable String getTargetVersion() {
        return TARGET_VERSION;
    }

    @Override
    public boolean execute(@Nullable Path userdataPath, @Nullable Path confPath) {
        if (userdataPath == null) {
            logger.error("{} skipped: no userdata directory found.", getName());
            return false;
        }

        Path jsondbPath = userdataPath.resolve("jsondb");
        if (Files.notExists(jsondbPath)) {
            logger.debug("{} skipped: jsondb directory not found.", getName());
            return true;
        }

        Path oldFilePath = jsondbPath.resolve("org.openhab.core.voice.DialogRegistration.json");
        Path newFilePath = jsondbPath.resolve("org.openhab.core.voice.dialog.DialogRegistration.json");

        if (Files.exists(oldFilePath)) {
            try {
                if (Files.exists(newFilePath)) {
                    logger.warn(
                            "Both old and new DialogRegistration storage files exist. Skipping file rename to prevent overwriting.");
                } else {
                    Files.move(oldFilePath, newFilePath);
                    logger.info("Moved DialogRegistration storage file from '{}' to '{}'", oldFilePath, newFilePath);
                }
            } catch (IOException e) {
                logger.error("Failed to move DialogRegistration storage file: {}", e.getMessage());
                return false;
            }
        }

        // Also rename backup files
        Path backupPath = jsondbPath.resolve("backup");
        if (Files.isDirectory(backupPath)) {
            try (var stream = Files.list(backupPath)) {
                String oldSuffix = "_org.openhab.core.voice.DialogRegistration.json";
                String newSuffix = "_org.openhab.core.voice.dialog.DialogRegistration.json";
                stream.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(oldSuffix))
                        .forEach(path -> {
                            String name = path.getFileName().toString();
                            String newName = name.substring(0, name.length() - oldSuffix.length()) + newSuffix;
                            Path targetPath = backupPath.resolve(newName);
                            try {
                                if (Files.exists(targetPath)) {
                                    Files.delete(path);
                                } else {
                                    Files.move(path, targetPath);
                                }
                            } catch (IOException e) {
                                logger.warn("Failed to migrate backup file '{}': {}", name, e.getMessage());
                            }
                        });
            } catch (IOException e) {
                logger.warn("Failed to access jsondb backup folder for migration: {}", e.getMessage());
            }
        }

        return true;
    }
}
