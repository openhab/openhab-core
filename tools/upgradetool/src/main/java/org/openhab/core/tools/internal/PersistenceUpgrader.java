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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.persistence.dto.PersistenceCronStrategyDTO;
import org.openhab.core.persistence.dto.PersistenceItemConfigurationDTO;
import org.openhab.core.persistence.dto.PersistenceServiceConfigurationDTO;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
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
        if (confPath == null) {
            logger.error("{} skipped: no conf directory found.", getName());
            return false;
        }

        List<String> managedConfigs;
        try {
            managedConfigs = managedPersistenceConfigs(installedPersistenceAddons(userdataPath),
                    unmanagedPersistenceConfigs(confPath));
        } catch (IOException e) {
            logger.error("{} skipped: failed to read config: {}", getName(), e.getMessage());
            return false;
        }
        if (managedConfigs.isEmpty()) {
            // No managed persistence configurations, so no need to upgrade
            return true;
        }

        Path persistenceJsonDatabasePath = userdataPath
                .resolve(Path.of("jsondb", "org.openhab.core.persistence.PersistenceServiceConfiguration.json"));
        if (Files.notExists(persistenceJsonDatabasePath)) {
            // No configuration, but persistence addons are installed and there is no unmanaged configuration for it, so
            // it needs to be created. Add JSON content to avoid warning from JSONDB.
            try {
                Files.createFile(persistenceJsonDatabasePath);
                Files.writeString(persistenceJsonDatabasePath, "{}");
            } catch (IOException e) {
                logger.error("Cannot create persistence configuration database '{}', check path and access rights.",
                        persistenceJsonDatabasePath);
                return false;
            }
        }

        if (!Files.isWritable(persistenceJsonDatabasePath)) {
            logger.error("Cannot access persistence configuration database '{}', check path and access rights.",
                    persistenceJsonDatabasePath);
            return false;
        }
        JsonStorage<PersistenceServiceConfigurationDTO> persistenceStorage = new JsonStorage<>(
                persistenceJsonDatabasePath.toFile(), null, 5, 0, 0, List.of());

        List<String> persistenceStorageKeys = List.copyOf(persistenceStorage.getKeys());

        // Add all installed persistence services without explicit configuration with their previous default
        // configuration
        List<String> managedConfigsToAdd = managedConfigs.stream()
                .filter(serviceId -> !persistenceStorageKeys.contains(serviceId)).toList();
        managedConfigsToAdd.forEach(serviceId -> {
            PersistenceServiceConfigurationDTO serviceConfigDTO = defaultServiceConfig(serviceId);
            if (serviceConfigDTO != null) {
                persistenceStorage.put(serviceId, serviceConfigDTO);
                logger.info("{}: added strategy configurations for persistence service without configuration", serviceId);
            }
        });

        // Update existing managed configurations
        persistenceStorageKeys.forEach(serviceId -> {
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

    private List<String> installedPersistenceAddons(Path userdataPath) throws IOException {
        Path addonsConfigPath = userdataPath.resolve("config/org/openhab/addons.config");
        if (Files.notExists(addonsConfigPath)) {
            throw new IOException(
                    "Cannot access addon config '" + addonsConfigPath + "', check path and access rights.");
        }

        List<String> configLines;
        configLines = Files.readAllLines(addonsConfigPath);

        for (String line : configLines) {
            if (line.startsWith("persistence")) {
                String[] persistenceLine = line.split("=");
                if (persistenceLine.length > 1) {
                    String[] persistenceAddons = persistenceLine[1].replace("\"", "").split(",");
                    return Stream.of(persistenceAddons).map(String::trim).toList();
                }
            }
        }
        return List.of();
    }

    private List<String> unmanagedPersistenceConfigs(Path configPath) throws IOException {
        Path persistenceConfigPath = configPath.resolve("persistence");
        try (Stream<Path> files = Files.list(persistenceConfigPath)) {
            return files.filter(configFile -> configFile.endsWith(".persist"))
                    .map(configFile -> configFile.getFileName().toString().replace(".persist", "")).toList();
        }
    }

    private List<String> managedPersistenceConfigs(List<String> installedAddons, List<String> unmanagedConfigs) {
        return installedAddons.stream().filter(a -> !unmanagedConfigs.contains(a)).toList();
    }

    private @Nullable PersistenceServiceConfigurationDTO defaultServiceConfig(String serviceId) {
        PersistenceItemConfigurationDTO itemConfigDTO = new PersistenceItemConfigurationDTO();
        List<String> strategies = defaultPersistenceStrategies(serviceId);
        if (strategies != null) {
            itemConfigDTO.items = List.of("*");
            itemConfigDTO.strategies = strategies;
            PersistenceServiceConfigurationDTO serviceConfigDTO = new PersistenceServiceConfigurationDTO();
            serviceConfigDTO.serviceId = serviceId;
            serviceConfigDTO.configs = List.of(itemConfigDTO);
            List<PersistenceCronStrategyDTO> cronStrategies = defaultCronStrategies(serviceId);
            if (cronStrategies != null) {
                serviceConfigDTO.cronStrategies = cronStrategies;
            }
            return serviceConfigDTO;
        }
        return null;
    }

    private @Nullable List<String> defaultPersistenceStrategies(String service) {
        return switch (service) {
            case "rrd4j" -> List.of(PersistenceStrategy.Globals.RESTORE.getName(),
                    PersistenceStrategy.Globals.CHANGE.getName(), "everyMinute");
            case "mapdb" ->
                List.of(PersistenceStrategy.Globals.RESTORE.getName(), PersistenceStrategy.Globals.CHANGE.getName());
            case "inmemory" -> List.of(PersistenceStrategy.Globals.FORECAST.getName());
            case "jdbc" -> List.of(PersistenceStrategy.Globals.CHANGE.getName());
            case "influxdb" ->
                List.of(PersistenceStrategy.Globals.RESTORE.getName(), PersistenceStrategy.Globals.CHANGE.getName());
            case "dynamodb" ->
                List.of(PersistenceStrategy.Globals.RESTORE.getName(), PersistenceStrategy.Globals.CHANGE.getName());
            default -> null;
        };
    }

    private @Nullable List<PersistenceCronStrategyDTO> defaultCronStrategies(String service) {
        return switch (service) {
            case "rrd4j" -> List.of(everyMinuteStrategy());
            default -> null;
        };
    }

    private PersistenceCronStrategyDTO everyMinuteStrategy() {
        PersistenceCronStrategyDTO everyMinuteStrategy = new PersistenceCronStrategyDTO();
        everyMinuteStrategy.name = "everyMinute";
        everyMinuteStrategy.cronExpression = "0 * * * * ?";
        return everyMinuteStrategy;
    }
}
