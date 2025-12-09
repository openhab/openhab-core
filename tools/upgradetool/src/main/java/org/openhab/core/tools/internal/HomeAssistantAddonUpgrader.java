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
package org.openhab.core.tools.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.json.internal.JsonStorage;
import org.openhab.core.thing.dto.ThingDTO;
import org.openhab.core.tools.Upgrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HomeAssistantAddonUpgrader} checks if the MQTT addon was previously
 * installed, and if Home Assistant things exist, and if so installs the
 * Home Assistant addon.
 *
 * @author Cody Cutrer - Initial contribution
 * @author Jimmy Tanagra - Refactored into a separate class
 */
@NonNullByDefault
public class HomeAssistantAddonUpgrader implements Upgrader {
    private final Logger logger = LoggerFactory.getLogger(HomeAssistantAddonUpgrader.class);

    @Override
    public String getName() {
        return "homeAssistantAddonUpgrader";
    }

    @Override
    public String getDescription() {
        return "Installs the Home Assistant addon if Home Assistant Things are present";
    }

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
            if (line.contains(",homeassistant")) {
                logger.info("Home Assistant addon already installed, skipping installation.");
                return true;
            }
            if (line.contains(",mqtt")) {
                String newLine = line.replace(",mqtt", ",mqtt,homeassistant");
                configLines.set(i, newLine);
                changed = true;
                break;
            }
        }
        if (!changed) {
            logger.info("MQTT addon not installed, skipping Home Assistant addon installation.");
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
            if (thing != null && thing.thingTypeUID != null
                    && thing.thingTypeUID.toString().startsWith("mqtt:homeassistant")) {
                hasHAThing = true;
                break;
            }
        }

        if (!hasHAThing) {
            logger.info("No Home Assistant Things found, skipping addon installation.");
            return true;
        }

        logger.info("Marking Home Assistant addon for installation.");

        try {
            Files.write(addonsConfigPath, configLines);
        } catch (IOException e) {
            logger.error("Failed to write addon config '{}', check path and access rights.", addonsConfigPath);
            return false;
        }

        return true;
    }
}
