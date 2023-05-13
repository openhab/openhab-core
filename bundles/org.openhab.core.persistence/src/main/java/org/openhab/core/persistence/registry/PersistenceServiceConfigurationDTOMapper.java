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
package org.openhab.core.persistence.registry;

import static org.openhab.core.persistence.strategy.PersistenceStrategy.Globals.STRATEGIES;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.persistence.PersistenceItemConfiguration;
import org.openhab.core.persistence.config.PersistenceAllConfig;
import org.openhab.core.persistence.config.PersistenceConfig;
import org.openhab.core.persistence.config.PersistenceGroupConfig;
import org.openhab.core.persistence.config.PersistenceItemConfig;
import org.openhab.core.persistence.dto.PersistenceCronStrategyDTO;
import org.openhab.core.persistence.dto.PersistenceFilterDTO;
import org.openhab.core.persistence.dto.PersistenceItemConfigurationDTO;
import org.openhab.core.persistence.dto.PersistenceServiceConfigurationDTO;
import org.openhab.core.persistence.filter.PersistenceFilter;
import org.openhab.core.persistence.filter.PersistenceThresholdFilter;
import org.openhab.core.persistence.filter.PersistenceTimeFilter;
import org.openhab.core.persistence.strategy.PersistenceCronStrategy;
import org.openhab.core.persistence.strategy.PersistenceStrategy;

/**
 * The {@link PersistenceServiceConfigurationDTOMapper} is a utility class to map persistence configurations for storage
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class PersistenceServiceConfigurationDTOMapper {

    private PersistenceServiceConfigurationDTOMapper() {
        // prevent initialization
    }

    public static PersistenceServiceConfigurationDTO map(
            PersistenceServiceConfiguration persistenceServiceConfiguration) {
        PersistenceServiceConfigurationDTO dto = new PersistenceServiceConfigurationDTO();
        dto.serviceId = persistenceServiceConfiguration.getUID();
        dto.configs = persistenceServiceConfiguration.getConfigs().stream()
                .map(PersistenceServiceConfigurationDTOMapper::mapPersistenceItemConfig).toList();
        dto.defaults = persistenceServiceConfiguration.getDefaults().stream().map(PersistenceStrategy::getName)
                .toList();
        dto.cronStrategies = filterList(persistenceServiceConfiguration.getStrategies(), PersistenceCronStrategy.class,
                PersistenceServiceConfigurationDTOMapper::mapPersistenceCronStrategy);
        dto.thresholdFilters = filterList(persistenceServiceConfiguration.getFilters(),
                PersistenceThresholdFilter.class,
                PersistenceServiceConfigurationDTOMapper::mapPersistenceThresholdFilter);
        dto.timeFilters = filterList(persistenceServiceConfiguration.getFilters(), PersistenceTimeFilter.class,
                PersistenceServiceConfigurationDTOMapper::mapPersistenceTimeFilter);

        return dto;
    }

    public static PersistenceServiceConfiguration map(PersistenceServiceConfigurationDTO dto) {
        Map<String, PersistenceStrategy> strategyMap = dto.cronStrategies.stream()
                .collect(Collectors.toMap(e -> e.name, e -> new PersistenceCronStrategy(e.name, e.cronExpression)));

        Map<String, PersistenceFilter> filterMap = Stream
                .concat(dto.thresholdFilters.stream().map(f -> new PersistenceThresholdFilter(f.name, f.value, f.unit)),
                        dto.timeFilters.stream()
                                .map(f -> new PersistenceTimeFilter(f.name, f.value.intValue(), f.unit)))
                .collect(Collectors.toMap(PersistenceFilter::getName, e -> e));

        List<PersistenceStrategy> defaults = dto.defaults.stream()
                .map(str -> stringToPersistenceStrategy(str, strategyMap, dto.serviceId)).toList();

        List<PersistenceItemConfiguration> configs = dto.configs.stream().map(config -> {
            List<PersistenceConfig> items = config.items.stream()
                    .map(PersistenceServiceConfigurationDTOMapper::stringToPersistenceConfig).toList();
            List<PersistenceStrategy> strategies = config.strategies.stream()
                    .map(str -> stringToPersistenceStrategy(str, strategyMap, dto.serviceId)).toList();
            return new PersistenceItemConfiguration(items, config.alias, strategies, List.of());
        }).toList();

        return new PersistenceServiceConfiguration(dto.serviceId, configs, defaults, strategyMap.values(),
                filterMap.values());
    }

    private static <T, R> Collection<R> filterList(Collection<? super T> list, Class<T> clazz, Function<T, R> mapper) {
        return list.stream().filter(clazz::isInstance).map(clazz::cast).map(mapper).toList();
    }

    private static PersistenceConfig stringToPersistenceConfig(String string) {
        if ("*".equals(string)) {
            return new PersistenceAllConfig();
        } else if (string.endsWith("*")) {
            return new PersistenceGroupConfig(string.substring(0, string.length() - 1));
        } else {
            return new PersistenceItemConfig(string);
        }
    }

    private static PersistenceStrategy stringToPersistenceStrategy(String string,
            Map<String, PersistenceStrategy> strategyMap, String serviceId) {
        PersistenceStrategy strategy = strategyMap.get(string);
        if (strategy != null) {
            return strategy;
        }
        strategy = STRATEGIES.get(string);
        if (strategy != null) {
            return strategy;
        }
        throw new IllegalArgumentException("Strategy '" + string + "' unknown for service '" + serviceId + "'");
    }

    private static String persistenceConfigToString(PersistenceConfig config) {
        if (config instanceof PersistenceAllConfig) {
            return "*";
        } else if (config instanceof PersistenceGroupConfig persistenceGroupConfig) {
            return persistenceGroupConfig.getGroup() + "*";
        } else if (config instanceof PersistenceItemConfig persistenceItemConfig) {
            return persistenceItemConfig.getItem();
        }
        throw new IllegalArgumentException("Unknown persistence config class " + config.getClass());
    }

    private static PersistenceItemConfigurationDTO mapPersistenceItemConfig(PersistenceItemConfiguration config) {
        PersistenceItemConfigurationDTO itemDto = new PersistenceItemConfigurationDTO();
        itemDto.items = config.items().stream().map(PersistenceServiceConfigurationDTOMapper::persistenceConfigToString)
                .toList();
        itemDto.strategies = config.strategies().stream().map(PersistenceStrategy::getName).toList();
        itemDto.alias = config.alias();
        return itemDto;
    }

    private static PersistenceCronStrategyDTO mapPersistenceCronStrategy(PersistenceCronStrategy cronStrategy) {
        PersistenceCronStrategyDTO cronStrategyDTO = new PersistenceCronStrategyDTO();
        cronStrategyDTO.name = cronStrategy.getName();
        cronStrategyDTO.cronExpression = cronStrategy.getCronExpression();
        return cronStrategyDTO;
    }

    private static PersistenceFilterDTO mapPersistenceThresholdFilter(PersistenceThresholdFilter thresholdFilter) {
        PersistenceFilterDTO filterDTO = new PersistenceFilterDTO();
        filterDTO.name = thresholdFilter.getName();
        filterDTO.value = thresholdFilter.getValue();
        filterDTO.unit = thresholdFilter.getUnit();
        return filterDTO;
    }

    private static PersistenceFilterDTO mapPersistenceTimeFilter(PersistenceTimeFilter persistenceTimeFilter) {
        PersistenceFilterDTO filterDTO = new PersistenceFilterDTO();
        filterDTO.name = persistenceTimeFilter.getName();
        filterDTO.value = new BigDecimal(persistenceTimeFilter.getValue());
        filterDTO.unit = persistenceTimeFilter.getUnit();
        return filterDTO;
    }
}
