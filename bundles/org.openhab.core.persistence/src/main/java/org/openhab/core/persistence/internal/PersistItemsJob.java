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
package org.openhab.core.persistence.internal;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openhab.core.items.Item;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.PersistenceServiceConfiguration;
import org.openhab.core.persistence.PersistenceItemConfiguration;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.openhab.core.scheduler.SchedulerRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a persistence job that could be executed e.g. for specific cron expressions.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Separation of persistence core and model, drop Quartz usage.
 */
public class PersistItemsJob implements SchedulerRunnable {

    private final Logger logger = LoggerFactory.getLogger(PersistItemsJob.class);

    private final PersistenceManagerImpl manager;
    private final String dbId;
    private final String strategyName;

    public PersistItemsJob(final PersistenceManagerImpl manager, final String dbId, final String strategyName) {
        this.manager = manager;
        this.dbId = dbId;
        this.strategyName = strategyName;
    }

    @Override
    public void run() {
        synchronized (manager.persistenceServiceConfigs) {
            final PersistenceService persistenceService = manager.persistenceServices.get(dbId);
            final PersistenceServiceConfiguration config = manager.persistenceServiceConfigs.get(dbId);

            if (persistenceService != null) {
                for (PersistenceItemConfiguration itemConfig : config.getConfigs()) {
                    if (hasStrategy(config.getDefaults(), itemConfig, strategyName)) {
                        for (Item item : manager.getAllItems(itemConfig)) {
                            long startTime = System.nanoTime();
                            persistenceService.store(item, itemConfig.getAlias());
                            logger.trace("Storing item '{}' with persistence service '{}' took {}ms", item.getName(),
                                    dbId, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
                        }
                    }

                }
            }
        }
    }

    private boolean hasStrategy(List<PersistenceStrategy> defaults, PersistenceItemConfiguration config, String strategyName) {
        // check if the strategy is directly defined on the config
        for (PersistenceStrategy strategy : config.getStrategies()) {
            if (strategyName.equals(strategy.getName())) {
                return true;
            }
        }
        // if no strategies are given, check the default strategies to use
        if (config.getStrategies().isEmpty() && isDefault(defaults, strategyName)) {
            return true;
        }
        return false;
    }

    private boolean isDefault(List<PersistenceStrategy> defaults, String strategyName) {
        for (PersistenceStrategy strategy : defaults) {
            if (strategy.getName().equals(strategyName)) {
                return true;
            }
        }
        return false;
    }

}
