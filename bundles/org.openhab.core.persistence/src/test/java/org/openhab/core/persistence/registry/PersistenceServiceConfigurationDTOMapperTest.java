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
package org.openhab.core.persistence.registry;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.persistence.PersistenceItemConfiguration;
import org.openhab.core.persistence.config.PersistenceAllConfig;
import org.openhab.core.persistence.config.PersistenceGroupConfig;
import org.openhab.core.persistence.config.PersistenceGroupExcludeConfig;
import org.openhab.core.persistence.config.PersistenceItemConfig;
import org.openhab.core.persistence.config.PersistenceItemExcludeConfig;
import org.openhab.core.persistence.dto.PersistenceCronStrategyDTO;
import org.openhab.core.persistence.dto.PersistenceFilterDTO;
import org.openhab.core.persistence.dto.PersistenceItemConfigurationDTO;
import org.openhab.core.persistence.dto.PersistenceServiceConfigurationDTO;
import org.openhab.core.persistence.filter.PersistenceThresholdFilter;
import org.openhab.core.persistence.strategy.PersistenceCronStrategy;
import org.openhab.core.persistence.strategy.PersistenceStrategy.Globals;

/**
 * Verifies the mapping used by managed persistence configurations.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
class PersistenceServiceConfigurationDTOMapperTest {
    private static final String CRON = "0 0 * * * ?";

    @Test
    void roundTripsItemSelectorsAliasesAndNamedDefinitions() {
        PersistenceItemConfigurationDTO item = new PersistenceItemConfigurationDTO();
        item.items = List.of("*", "Temperatures*", "Hallway", "!Diagnostics*", "!TestSensor");
        item.strategies = List.of("everyChange", "hourly");
        item.filters = List.of("significant");

        PersistenceCronStrategyDTO hourly = new PersistenceCronStrategyDTO();
        hourly.name = "hourly";
        hourly.cronExpression = CRON;
        PersistenceFilterDTO threshold = new PersistenceFilterDTO();
        threshold.name = "significant";
        threshold.value = new BigDecimal("2.5");
        threshold.unit = "%";
        threshold.relative = true;

        PersistenceServiceConfigurationDTO dto = configuration(item);
        dto.aliases = Map.of("Hallway", "hallway_temperature");
        dto.cronStrategies = List.of(hourly);
        dto.thresholdFilters = List.of(threshold);

        PersistenceServiceConfiguration decoded = PersistenceServiceConfigurationDTOMapper.map(dto);
        PersistenceItemConfiguration entry = decoded.getConfigs().getFirst();
        assertInstanceOf(PersistenceAllConfig.class, entry.items().get(0));
        assertEquals("Temperatures", assertInstanceOf(PersistenceGroupConfig.class, entry.items().get(1)).getGroup());
        assertEquals("Hallway", assertInstanceOf(PersistenceItemConfig.class, entry.items().get(2)).getItem());
        assertEquals("Diagnostics",
                assertInstanceOf(PersistenceGroupExcludeConfig.class, entry.items().get(3)).getGroup());
        assertEquals("TestSensor",
                assertInstanceOf(PersistenceItemExcludeConfig.class, entry.items().get(4)).getItem());
        assertEquals(Globals.CHANGE, entry.strategies().getFirst());
        PersistenceCronStrategy cron = assertInstanceOf(PersistenceCronStrategy.class, entry.strategies().get(1));
        assertEquals(CRON, cron.getCronExpression());
        PersistenceThresholdFilter filter = assertInstanceOf(PersistenceThresholdFilter.class,
                entry.filters().getFirst());
        assertEquals(threshold.value, filter.getValue());
        assertEquals("%", filter.getUnit());
        assertTrue(filter.isRelative());

        PersistenceServiceConfigurationDTO encoded = PersistenceServiceConfigurationDTOMapper.map(decoded);
        assertEquals(dto.serviceId, encoded.serviceId);
        assertEquals(dto.aliases, encoded.aliases);
        PersistenceItemConfigurationDTO encodedItem = encoded.configs.iterator().next();
        assertEquals(item.items, encodedItem.items);
        assertEquals(item.strategies, encodedItem.strategies);
        assertEquals(item.filters, encodedItem.filters);
        assertEquals(CRON, encoded.cronStrategies.iterator().next().cronExpression);
        assertEquals(threshold.value, encoded.thresholdFilters.iterator().next().value);
    }

    @Test
    void rejectsAConfigurationThatReferencesAnUnknownStrategy() {
        PersistenceItemConfigurationDTO item = new PersistenceItemConfigurationDTO();
        item.strategies = List.of("everyChange", "missingStrategy");

        assertThrows(IllegalArgumentException.class,
                () -> PersistenceServiceConfigurationDTOMapper.map(configuration(item)));
    }

    @Test
    void rejectsAConfigurationThatReferencesAnUnknownFilter() {
        PersistenceItemConfigurationDTO item = new PersistenceItemConfigurationDTO();
        item.strategies = List.of("everyChange");
        item.filters = List.of("missingFilter");

        assertThrows(IllegalArgumentException.class,
                () -> PersistenceServiceConfigurationDTOMapper.map(configuration(item)));
    }

    private static PersistenceServiceConfigurationDTO configuration(PersistenceItemConfigurationDTO item) {
        PersistenceServiceConfigurationDTO dto = new PersistenceServiceConfigurationDTO();
        dto.serviceId = "test-persistence";
        dto.configs = List.of(item);
        return dto;
    }
}
