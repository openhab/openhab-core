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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.persistence.config.PersistenceConfig;
import org.openhab.core.persistence.strategy.PersistenceStrategy;

/**
 * This class holds the configuration of a persistence strategy for specific items.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class PersistenceItemConfiguration {

    private final List<PersistenceConfig> items;
    private final @Nullable String alias;
    private final @Nullable List<PersistenceStrategy> strategies;
    private final @Nullable List<PersistenceFilter> filters;

    public PersistenceItemConfiguration(final List<PersistenceConfig> items, @Nullable final String alias,
            @Nullable final List<PersistenceStrategy> strategies, @Nullable final List<PersistenceFilter> filters) {
        this.items = items;
        this.alias = alias;
        this.strategies = strategies;
        this.filters = filters;
    }

    public List<PersistenceConfig> getItems() {
        return items;
    }

    public @Nullable String getAlias() {
        return alias;
    }

    public @Nullable List<PersistenceStrategy> getStrategies() {
        return strategies;
    }

    public @Nullable List<PersistenceFilter> getFilters() {
        return filters;
    }

    @Override
    public String toString() {
        return String.format("%s [items=%s, alias=%s, strategies=%s, filters=%s]", getClass().getSimpleName(),
                Arrays.toString(items.toArray()), alias, Arrays.toString(strategies.toArray()),
                Arrays.toString(filters.toArray()));
    }
}
