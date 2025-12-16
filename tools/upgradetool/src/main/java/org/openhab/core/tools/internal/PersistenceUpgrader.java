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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.persistence.dto.PersistenceItemConfigurationDTO;
import org.openhab.core.persistence.dto.PersistenceServiceConfigurationDTO;
import org.openhab.core.storage.json.internal.JsonStorage;
import org.openhab.core.tools.Upgrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PersistenceUpgrader} removes the default persistence strategy.
 *
 * It upgrades the PersistenceServiceConfiguration database, removing default strategies and setting strategies on each
 * configuration that has no strategy defined.
 * See <a href="https://github.com/openhab/openhab-core/pull/4682">openhab/openhab-core#4682</a>.
 *
 * @author Mark Herwege - Initial Contribution
 */
@NonNullByDefault
public class PersistenceUpgrader implements Upgrader {
    private final Logger logger = LoggerFactory.getLogger(PersistenceUpgrader.class);

    @Override
    public String getName() {
        return "persistenceCopyDefaultStrategy";
    }

    @Override
    public String getDescription() {
        return "Move persistence default strategy configuration to all persistence configuration without strategy defined";
    }

    @Override
    public boolean execute(@Nullable Path userdataPath, @Nullable Path confPath) {
        if (userdataPath == null) {
            logger.error("{} skipped: no userdata directory found.", getName());
            return false;
        }

        Path persistenceJsonDatabasePath = userdataPath
                .resolve(Path.of("jsondb", "org.openhab.core.persistence.PersistenceServiceConfiguration.json"));
        if (Files.notExists(persistenceJsonDatabasePath)) {
            // No managed persistence configurations, so no need to upgrade
            return true;
        }

        logger.info("Setting default strategy on managed persistence configurations without strategy '{}'",
                persistenceJsonDatabasePath);

        if (!Files.isWritable(persistenceJsonDatabasePath)) {
            logger.error("Cannot access persistence configuration database '{}', check path and access rights.",
                    persistenceJsonDatabasePath);
            return false;
        }
        JsonStorage<PersistenceServiceConfigurationDTO> persistenceStorage = new JsonStorage<>(
                persistenceJsonDatabasePath.toFile(), null, 5, 0, 0, List.of());

        List.copyOf(persistenceStorage.getKeys()).forEach(serviceId -> {
            PersistenceServiceConfigurationDTO serviceConfigDTO = Objects
                    .requireNonNull(persistenceStorage.get(serviceId));
            Collection<String> defaults = serviceConfigDTO.defaults;
            if (defaults != null) {
                Collection<PersistenceItemConfigurationDTO> configs = serviceConfigDTO.configs;
                configs.forEach(config -> {
                    Collection<String> strategies = config.strategies;
                    if (strategies.isEmpty()) {
                        config.strategies = defaults;
                    }
                });
                serviceConfigDTO.defaults = null;

                persistenceStorage.put(serviceId, serviceConfigDTO);
                logger.info("{}: updated strategy configurations and removed default strategies", serviceId);
            }
        });

        persistenceStorage.flush();
        return true;
    }
}
