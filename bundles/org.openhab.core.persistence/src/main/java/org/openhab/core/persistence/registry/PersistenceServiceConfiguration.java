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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.persistence.PersistenceItemConfiguration;
import org.openhab.core.persistence.filter.PersistenceFilter;
import org.openhab.core.persistence.strategy.PersistenceStrategy;

/**
 * The {@link PersistenceServiceConfiguration} represents the configuration for a persistence service.
 *
 * @author Jan N. Klug - Initial contribution
 * @author Mark Herwege - Implement aliases
 * @author Mark Herwege - Make default strategy to be only a configuration suggestion
 */
@NonNullByDefault
public class PersistenceServiceConfiguration implements Identifiable<String> {
    private final String serviceId;
    private final List<PersistenceItemConfiguration> configs;
    private final Map<String, String> aliases;
    private final List<PersistenceStrategy> strategies;
    private final List<PersistenceFilter> filters;

    public PersistenceServiceConfiguration(String serviceId, Collection<PersistenceItemConfiguration> configs,
            Map<String, String> aliases, Collection<PersistenceStrategy> strategies,
            Collection<PersistenceFilter> filters) {
        this.serviceId = serviceId;
        this.configs = List.copyOf(configs);
        this.aliases = Map.copyOf(aliases);
        this.strategies = List.copyOf(strategies);
        this.filters = List.copyOf(filters);
    }

    @Override
    public String getUID() {
        return serviceId;
    }

    /**
     * Get the item configurations.
     *
     * @return an unmodifiable list of the item configurations
     */
    public List<PersistenceItemConfiguration> getConfigs() {
        return configs;
    }

    /**
     * Get the item aliases.
     *
     * @return a map of items to aliases
     */
    public Map<String, String> getAliases() {
        return aliases;
    }

    /**
     * Get all defined strategies.
     *
     * @return an unmodifiable list of the defined strategies
     */
    public List<PersistenceStrategy> getStrategies() {
        return strategies;
    }

    /**
     * Get all defined filters.
     *
     * @return an unmodifiable list of the defined filters
     */
    public List<PersistenceFilter> getFilters() {
        return filters;
    }
}
