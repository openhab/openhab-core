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

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemRegistryChangeListener;
import org.openhab.core.items.StateChangeListener;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceItemConfiguration;
import org.openhab.core.persistence.PersistenceManager;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.PersistenceServiceConfiguration;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.persistence.config.PersistenceAllConfig;
import org.openhab.core.persistence.config.PersistenceConfig;
import org.openhab.core.persistence.config.PersistenceGroupConfig;
import org.openhab.core.persistence.config.PersistenceItemConfig;
import org.openhab.core.persistence.strategy.PersistenceCronStrategy;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.openhab.core.scheduler.CronScheduler;
import org.openhab.core.scheduler.ScheduledCompletableFuture;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a persistence manager to manage all persistence services etc.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Separation of persistence core and model, drop Quartz usage.
 */
@Component(immediate = true, service = PersistenceManager.class)
@NonNullByDefault
public class PersistenceManagerImpl implements ItemRegistryChangeListener, PersistenceManager, StateChangeListener {

    private final Logger logger = LoggerFactory.getLogger(PersistenceManagerImpl.class);

    // the scheduler used for timer events
    private final CronScheduler scheduler;
    private final ItemRegistry itemRegistry;
    private final SafeCaller safeCaller;
    private volatile boolean started = false;

    final Map<String, PersistenceService> persistenceServices = new HashMap<>();
    final Map<String, @Nullable PersistenceServiceConfiguration> persistenceServiceConfigs = new HashMap<>();
    private final Map<String, Set<ScheduledCompletableFuture<?>>> persistenceJobs = new HashMap<>();

    @Activate
    public PersistenceManagerImpl(final @Reference CronScheduler scheduler, final @Reference ItemRegistry itemRegistry,
            final @Reference SafeCaller safeCaller) {
        this.scheduler = scheduler;
        this.itemRegistry = itemRegistry;
        this.safeCaller = safeCaller;
    }

    @Activate
    protected void activate() {
        allItemsChanged(Collections.emptySet());
        started = true;
        itemRegistry.addRegistryChangeListener(this);
    }

    @Deactivate
    protected void deactivate() {
        itemRegistry.removeRegistryChangeListener(this);
        started = false;
        removeTimers();
        removeItemStateChangeListeners();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addPersistenceService(PersistenceService persistenceService) {
        logger.debug("Initializing {} persistence service.", persistenceService.getId());
        persistenceServices.put(persistenceService.getId(), persistenceService);
        persistenceServiceConfigs.putIfAbsent(persistenceService.getId(), getDefaultConfig(persistenceService));
        if (started) {
            stopEventHandling(persistenceService.getId());
            startEventHandling(persistenceService.getId());
        }
    }

    protected void removePersistenceService(PersistenceService persistenceService) {
        stopEventHandling(persistenceService.getId());
        persistenceServices.remove(persistenceService.getId());
    }

    /**
     * Calls all persistence services which use change or update policy for the given item
     *
     * @param item the item to persist
     * @param onlyChanges true, if it has the change strategy, false otherwise
     */
    private void handleStateEvent(Item item, boolean onlyChanges) {
        synchronized (persistenceServiceConfigs) {
            for (Entry<String, @Nullable PersistenceServiceConfiguration> entry : persistenceServiceConfigs
                    .entrySet()) {
                final String serviceName = entry.getKey();
                final PersistenceServiceConfiguration config = entry.getValue();
                if (config != null && persistenceServices.containsKey(serviceName)) {
                    for (PersistenceItemConfiguration itemConfig : config.getConfigs()) {
                        if (hasStrategy(config, itemConfig, onlyChanges ? PersistenceStrategy.Globals.CHANGE
                                : PersistenceStrategy.Globals.UPDATE)) {
                            if (appliesToItem(itemConfig, item)) {
                                persistenceServices.get(serviceName).store(item, itemConfig.getAlias());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if a given persistence configuration entry has a certain strategy for the given service
     *
     * @param config the configuration to check for
     * @param itemConfig the persistence configuration entry
     * @param strategy the strategy to check for
     * @return true, if it has the given strategy
     */
    private boolean hasStrategy(PersistenceServiceConfiguration config, PersistenceItemConfiguration itemConfig,
            PersistenceStrategy strategy) {
        if (config.getDefaults().contains(strategy) && itemConfig.getStrategies().isEmpty()) {
            return true;
        } else {
            for (PersistenceStrategy s : itemConfig.getStrategies()) {
                if (s.equals(strategy)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Checks if a given persistence configuration entry is relevant for an item
     *
     * @param config the persistence configuration entry
     * @param item to check if the configuration applies to
     * @return true, if the configuration applies to the item
     */
    private boolean appliesToItem(PersistenceItemConfiguration config, Item item) {
        for (PersistenceConfig itemCfg : config.getItems()) {
            if (itemCfg instanceof PersistenceAllConfig) {
                return true;
            }
            if (itemCfg instanceof PersistenceItemConfig) {
                PersistenceItemConfig singleItemConfig = (PersistenceItemConfig) itemCfg;
                if (item.getName().equals(singleItemConfig.getItem())) {
                    return true;
                }
            }
            if (itemCfg instanceof PersistenceGroupConfig) {
                PersistenceGroupConfig groupItemConfig = (PersistenceGroupConfig) itemCfg;
                try {
                    Item gItem = itemRegistry.getItem(groupItemConfig.getGroup());
                    if (gItem instanceof GroupItem) {
                        GroupItem groupItem = (GroupItem) gItem;
                        if (groupItem.getAllMembers().contains(item)) {
                            return true;
                        }
                    }
                } catch (ItemNotFoundException e) {
                    // do nothing
                }
            }
        }
        return false;
    }

    /**
     * Retrieves all items for which the persistence configuration applies to.
     *
     * @param config the persistence configuration entry
     * @return all items that this configuration applies to
     */
    Iterable<Item> getAllItems(PersistenceItemConfiguration config) {
        // first check, if we should return them all
        for (Object itemCfg : config.getItems()) {
            if (itemCfg instanceof PersistenceAllConfig) {
                return itemRegistry.getItems();
            }
        }

        // otherwise, go through the detailed definitions
        Set<Item> items = new HashSet<>();
        for (Object itemCfg : config.getItems()) {
            if (itemCfg instanceof PersistenceItemConfig) {
                PersistenceItemConfig singleItemConfig = (PersistenceItemConfig) itemCfg;
                String itemName = singleItemConfig.getItem();
                try {
                    items.add(itemRegistry.getItem(itemName));
                } catch (ItemNotFoundException e) {
                    logger.debug("Item '{}' does not exist.", itemName);
                }
            }
            if (itemCfg instanceof PersistenceGroupConfig) {
                PersistenceGroupConfig groupItemConfig = (PersistenceGroupConfig) itemCfg;
                String groupName = groupItemConfig.getGroup();
                try {
                    Item gItem = itemRegistry.getItem(groupName);
                    if (gItem instanceof GroupItem) {
                        GroupItem groupItem = (GroupItem) gItem;
                        items.addAll(groupItem.getAllMembers());
                    }
                } catch (ItemNotFoundException e) {
                    logger.debug("Item group '{}' does not exist.", groupName);
                }
            }
        }
        return items;
    }

    /**
     * Handles the "restoreOnStartup" strategy for the item.
     * If the item state is still undefined when entering this method, all persistence configurations are checked,
     * if they have the "restoreOnStartup" strategy configured for the item. If so, the item state will be set
     * to its last persisted value.
     *
     * @param item the item to restore the state for
     */
    @SuppressWarnings("null")
    private void initialize(Item item) {
        // get the last persisted state from the persistence service if no state is yet set
        if (UnDefType.NULL.equals(item.getState()) && item instanceof GenericItem) {
            for (Entry<String, @Nullable PersistenceServiceConfiguration> entry : persistenceServiceConfigs
                    .entrySet()) {
                final String serviceName = entry.getKey();
                final PersistenceServiceConfiguration config = entry.getValue();
                if (config != null) {
                    for (PersistenceItemConfiguration itemConfig : config.getConfigs()) {
                        if (hasStrategy(config, itemConfig, PersistenceStrategy.Globals.RESTORE)) {
                            if (appliesToItem(itemConfig, item)) {
                                PersistenceService service = persistenceServices.get(serviceName);
                                if (service instanceof QueryablePersistenceService) {
                                    QueryablePersistenceService queryService = (QueryablePersistenceService) service;
                                    FilterCriteria filter = new FilterCriteria().setItemName(item.getName())
                                            .setPageSize(1);
                                    Iterable<HistoricItem> result = safeCaller
                                            .create(queryService, QueryablePersistenceService.class).onTimeout(() -> {
                                                logger.warn("Querying persistence service '{}' takes more than {}ms.",
                                                        queryService.getId(), SafeCaller.DEFAULT_TIMEOUT);
                                            }).onException(e -> {
                                                logger.error(
                                                        "Exception occurred while querying persistence service '{}': {}",
                                                        queryService.getId(), e.getMessage(), e);
                                            }).build().query(filter);
                                    if (result != null) {
                                        Iterator<HistoricItem> it = result.iterator();
                                        if (it.hasNext()) {
                                            HistoricItem historicItem = it.next();
                                            GenericItem genericItem = (GenericItem) item;
                                            genericItem.removeStateChangeListener(this);
                                            genericItem.setState(historicItem.getState());
                                            genericItem.addStateChangeListener(this);
                                            if (logger.isDebugEnabled()) {
                                                logger.debug("Restored item state from '{}' for item '{}' -> '{}'",
                                                        DateTimeFormatter.ISO_ZONED_DATE_TIME
                                                                .format(historicItem.getTimestamp()),
                                                        item.getName(), historicItem.getState());
                                            }
                                            return;
                                        }
                                    }
                                } else if (service != null) {
                                    logger.warn(
                                            "Failed to restore item states as persistence service '{}' cannot be queried.",
                                            serviceName);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void removeItemStateChangeListeners() {
        for (Item item : itemRegistry.getAll()) {
            if (item instanceof GenericItem) {
                ((GenericItem) item).removeStateChangeListener(this);
            }
        }
    }

    /**
     * Creates new {@link ScheduledCompletableFuture}s in the group <code>dbId</code> for the given collection of
     * {@link PersistenceStrategy strategies}.
     *
     * @param dbId the database id used by the persistence service
     * @param strategies a collection of strategies
     */
    private void createTimers(final String dbId, List<PersistenceStrategy> strategies) {
        for (PersistenceStrategy strategy : strategies) {
            if (strategy instanceof PersistenceCronStrategy) {
                PersistenceCronStrategy cronStrategy = (PersistenceCronStrategy) strategy;
                String cronExpression = cronStrategy.getCronExpression();

                final PersistItemsJob job = new PersistItemsJob(this, dbId, cronStrategy.getName());
                ScheduledCompletableFuture<?> schedule = scheduler.schedule(job, cronExpression);
                if (persistenceJobs.containsKey(dbId)) {
                    persistenceJobs.get(dbId).add(schedule);
                } else {
                    final Set<ScheduledCompletableFuture<?>> jobs = new HashSet<>();
                    jobs.add(schedule);
                    persistenceJobs.put(dbId, jobs);
                }

                logger.debug("Scheduled strategy {} with cron expression {}", cronStrategy.getName(), cronExpression);
            }
        }
    }

    /**
     * Deletes all {@link ScheduledCompletableFuture}s of the group <code>dbId</code>.
     *
     * @param dbId the database id used by the persistence service
     */
    private void removeTimers(String dbId) {
        if (!persistenceJobs.containsKey(dbId)) {
            return;
        }
        for (final ScheduledCompletableFuture<?> job : persistenceJobs.get(dbId)) {
            job.cancel(true);
            logger.debug("Removed scheduled cron job for persistence service '{}'", dbId);
        }
        persistenceJobs.remove(dbId);
    }

    private void removeTimers() {
        Set<String> dbIds = new HashSet<>(persistenceJobs.keySet());
        for (String dbId : dbIds) {
            removeTimers(dbId);
        }
    }

    @Override
    public void addConfig(final String dbId, final PersistenceServiceConfiguration config) {
        synchronized (persistenceServiceConfigs) {
            if (persistenceServiceConfigs.containsKey(dbId)) {
                stopEventHandling(dbId);
            }
            persistenceServiceConfigs.put(dbId, config);
            if (persistenceServices.containsKey(dbId)) {
                startEventHandling(dbId);
            }
        }
    }

    @Override
    public void removeConfig(final String dbId) {
        synchronized (persistenceServiceConfigs) {
            stopEventHandling(dbId);
            PersistenceService persistenceService = persistenceServices.get(dbId);
            if (persistenceService != null) {
                persistenceServiceConfigs.put(dbId, getDefaultConfig(persistenceService));
                startEventHandling(dbId);
            } else {
                persistenceServiceConfigs.remove(dbId);
            }
        }
    }

    private void startEventHandling(final String serviceName) {
        synchronized (persistenceServiceConfigs) {
            final PersistenceServiceConfiguration config = persistenceServiceConfigs.get(serviceName);
            if (config != null) {
                for (PersistenceItemConfiguration itemConfig : config.getConfigs()) {
                    if (hasStrategy(config, itemConfig, PersistenceStrategy.Globals.RESTORE)) {
                        for (Item item : getAllItems(itemConfig)) {
                            initialize(item);
                        }
                    }
                }
                createTimers(serviceName, config.getStrategies());
            }
        }
    }

    private void stopEventHandling(String dbId) {
        synchronized (persistenceServiceConfigs) {
            removeTimers(dbId);
        }
    }

    private @Nullable PersistenceServiceConfiguration getDefaultConfig(PersistenceService persistenceService) {
        List<PersistenceStrategy> strategies = persistenceService.getDefaultStrategies();
        List<PersistenceItemConfiguration> configs = List
                .of(new PersistenceItemConfiguration(List.of(new PersistenceAllConfig()), null, strategies, null));
        return new PersistenceServiceConfiguration(configs, strategies, strategies);
    }

    @Override
    public void allItemsChanged(Collection<String> oldItemNames) {
        for (Item item : itemRegistry.getItems()) {
            added(item);
        }
    }

    @Override
    public void added(Item item) {
        initialize(item);
        if (item instanceof GenericItem) {
            GenericItem genericItem = (GenericItem) item;
            genericItem.addStateChangeListener(this);
        }
    }

    @Override
    public void removed(Item item) {
        if (item instanceof GenericItem) {
            GenericItem genericItem = (GenericItem) item;
            genericItem.removeStateChangeListener(this);
        }
    }

    @Override
    public void updated(Item oldItem, Item item) {
        removed(oldItem);
        added(item);
    }

    @Override
    public void stateChanged(Item item, State oldState, State newState) {
        handleStateEvent(item, true);
    }

    @Override
    public void stateUpdated(Item item, State state) {
        handleStateEvent(item, false);
    }
}
