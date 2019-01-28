/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.persistence;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.smarthome.core.persistence.strategy.SimpleStrategy;

/**
 * This class represents the configuration for a persistence service.
 *
 * @author Markus Rathgeb - Initial contribution and API
 */
public class PersistenceServiceConfiguration {
    private final List<SimpleItemConfiguration> configs;
    private final List<SimpleStrategy> defaults;
    private final List<SimpleStrategy> strategies;

    public PersistenceServiceConfiguration(final List<SimpleItemConfiguration> configs,
            final List<SimpleStrategy> defaults, final List<SimpleStrategy> strategies) {
        this.configs = Collections.unmodifiableList(new LinkedList<>(configs));
        this.defaults = Collections.unmodifiableList(new LinkedList<>(defaults));
        this.strategies = Collections.unmodifiableList(new LinkedList<>(strategies));
    }

    /**
     * Get the item configurations.
     *
     * @return an unmodifiable list of the item configurations
     */
    public List<SimpleItemConfiguration> getConfigs() {
        return configs;
    }

    /**
     * Get the default strategies.
     *
     * @return an unmodifiable list of the default strategies
     */
    public List<SimpleStrategy> getDefaults() {
        return defaults;
    }

    /**
     * Get all defined strategies.
     *
     * @return an unmodifiable list of the defined strategies
     */
    public List<SimpleStrategy> getStrategies() {
        return strategies;
    }

}
