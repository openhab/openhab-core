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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.persistence.strategy.PersistenceStrategy;

/**
 * This class represents the configuration for a persistence service.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class PersistenceServiceConfiguration {
    private final List<PersistenceItemConfiguration> configs;
    private final List<PersistenceStrategy> defaults;
    private final List<PersistenceStrategy> strategies;

    public PersistenceServiceConfiguration(final Collection<PersistenceItemConfiguration> configs,
            final Collection<PersistenceStrategy> defaults, final Collection<PersistenceStrategy> strategies) {
        this.configs = Collections.unmodifiableList(new LinkedList<>(configs));
        this.defaults = Collections.unmodifiableList(new LinkedList<>(defaults));
        this.strategies = Collections.unmodifiableList(new LinkedList<>(strategies));
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
     * Get the default strategies.
     *
     * @return an unmodifiable list of the default strategies
     */
    public List<PersistenceStrategy> getDefaults() {
        return defaults;
    }

    /**
     * Get all defined strategies.
     *
     * @return an unmodifiable list of the defined strategies
     */
    public List<PersistenceStrategy> getStrategies() {
        return strategies;
    }
}
