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
package org.openhab.core.persistence;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.persistence.config.PersistenceConfig;
import org.openhab.core.persistence.filter.PersistenceFilter;
import org.openhab.core.persistence.strategy.PersistenceStrategy;

/**
 * This class holds the configuration of a persistence strategy for specific items.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public record PersistenceItemConfiguration(List<PersistenceConfig> items, @Nullable String alias,
        List<PersistenceStrategy> strategies, List<PersistenceFilter> filters) {

    public PersistenceItemConfiguration(final List<PersistenceConfig> items, @Nullable final String alias,
            @Nullable final List<PersistenceStrategy> strategies, @Nullable final List<PersistenceFilter> filters) {
        this.items = items;
        this.alias = alias;
        this.strategies = Objects.requireNonNullElse(strategies, List.of());
        this.filters = Objects.requireNonNullElse(filters, List.of());
    }
}
