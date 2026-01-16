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
package org.openhab.core.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.json.internal.JsonStorage;
import org.openhab.core.thing.dto.ThingDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ExtractedAddonUpgrader} checks if the parent addon was previously
 * installed, and if specific things exist, and if so installs the
 * corresponding addon.
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public abstract class ExtractedAddonUpgrader implements Upgrader {
    private final Logger logger = LoggerFactory.getLogger(ExtractedAddonUpgrader.class);

    public abstract String getParentAddonName();

    public abstract String getAddonName();

    public abstract boolean thingTypeMatches(String thingTypeUID);

    @Override
    public boolean execute(@Nullable Path userdataPath, @Nullable Path confPath) {
        if (userdataPath == null) {
            logger.error("{} skipped: no userdata directory found.", getName());
            return false;
        }

        Path addonsConfigPath = userdataPath.resolve("config/org/openhab/addons.config");
        if (!Files.isWritable(addonsConfigPath)) {
            logger.error("Cannot access addon config '{}', check path and access rights.", addonsConfigPath);
            return false;
        }

        List<String> configLines;
        try {
            configLines = Files.readAllLines(addonsConfigPath);
        } catch (IOException e) {
            logger.error("Failed to read addon config '{}', check path and access rights.", addonsConfigPath);
            return false;
        }

        boolean changed = false;

        for (int i = 0; i < configLines.size(); i++) {
            String line = Objects.requireNonNull(configLines.get(i));
            if (line.contains("," + getAddonName())) {
                logger.info("{} addon already installed, skipping installation.", getAddonName());
                return true;
            }
            if (line.contains("," + getParentAddonName())) {
                String newLine = line.replace("," + getParentAddonName(),
                        "," + getParentAddonName() + "," + getAddonName());
                configLines.set(i, newLine);
                changed = true;
                break;
            }
        }
        if (!changed) {
            logger.info("{} addon not installed, skipping {} addon installation.", getParentAddonName(),
                    getAddonName());
            return true;
        }

        Path thingJsonDatabasePath = userdataPath.resolve("jsondb/org.openhab.core.thing.Thing.json");
        if (!Files.isReadable(thingJsonDatabasePath)) {
            logger.error("Cannot access thing database '{}', check path and access rights.", thingJsonDatabasePath);
            return false;
        }

        JsonStorage<ThingDTO> thingStorage = new JsonStorage<>(thingJsonDatabasePath.toFile(), null, 5, 0, 0,
                List.of());

        boolean hasHAThing = false;
        for (String thingId : thingStorage.getKeys()) {
            ThingDTO thing = thingStorage.get(thingId);
            if (thing != null && thing.thingTypeUID != null && thingTypeMatches(thing.thingTypeUID.toString())) {
                hasHAThing = true;
                break;
            }
        }

        if (!hasHAThing) {
            logger.info("No {} Things found, skipping addon installation.", getAddonName());
            return true;
        }

        logger.info("Marking {} addon for installation.", getAddonName());

        try {
            Files.write(addonsConfigPath, configLines);
        } catch (IOException e) {
            logger.error("Failed to write addon config '{}', check path and access rights.", addonsConfigPath);
            return false;
        }

        return true;
    }
}
