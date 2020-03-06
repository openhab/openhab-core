/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.persistence;

import java.util.Arrays;
import java.util.List;

import org.openhab.core.persistence.config.PersistenceConfig;
import org.openhab.core.persistence.strategy.PersistenceStrategy;

/**
 * This class holds the configuration of a persistence strategy for specific items.
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class PersistenceItemConfiguration {

    private final List<PersistenceConfig> items;
    private final String alias;
    private final List<PersistenceStrategy> strategies;
    private final List<PersistenceFilter> filters;

    public PersistenceItemConfiguration(final List<PersistenceConfig> items, final String alias,
            final List<PersistenceStrategy> strategies, final List<PersistenceFilter> filters) {
        this.items = items;
        this.alias = alias;
        this.strategies = strategies;
        this.filters = filters;
    }

    public List<PersistenceConfig> getItems() {
        return items;
    }

    public String getAlias() {
        return alias;
    }

    public List<PersistenceStrategy> getStrategies() {
        return strategies;
    }

    public List<PersistenceFilter> getFilters() {
        return filters;
    }

    @Override
    public String toString() {
        return String.format("%s [items=%s, alias=%s, strategies=%s, filters=%s]", getClass().getSimpleName(),
                Arrays.toString(items.toArray()), alias, Arrays.toString(strategies.toArray()),
                Arrays.toString(filters.toArray()));
    }
}
