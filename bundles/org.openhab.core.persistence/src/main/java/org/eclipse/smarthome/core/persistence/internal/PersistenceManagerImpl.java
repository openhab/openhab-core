/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.persistence.internal;

import java.text.DateFormat;
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
import org.eclipse.smarthome.core.common.SafeCaller;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.ItemRegistryChangeListener;
import org.eclipse.smarthome.core.items.StateChangeListener;
import org.eclipse.smarthome.core.persistence.FilterCriteria;
import org.eclipse.smarthome.core.persistence.HistoricItem;
import org.eclipse.smarthome.core.persistence.PersistenceManager;
import org.eclipse.smarthome.core.persistence.PersistenceService;
import org.eclipse.smarthome.core.persistence.PersistenceServiceConfiguration;
import org.eclipse.smarthome.core.persistence.QueryablePersistenceService;
import org.eclipse.smarthome.core.persistence.SimpleItemConfiguration;
import org.eclipse.smarthome.core.persistence.config.SimpleAllConfig;
import org.eclipse.smarthome.core.persistence.config.SimpleConfig;
import org.eclipse.smarthome.core.persistence.config.SimpleGroupConfig;
import org.eclipse.smarthome.core.persistence.config.SimpleItemConfig;
import org.eclipse.smarthome.core.persistence.strategy.SimpleCronStrategy;
import org.eclipse.smarthome.core.persistence.strategy.SimpleStrategy;
import org.eclipse.smarthome.core.scheduler.CronScheduler;
import org.eclipse.smarthome.core.scheduler.ScheduledCompletableFuture;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
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
    final Map<String, PersistenceServiceConfiguration> persistenceServiceConfigs = new HashMap<>();
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
            for (Entry<String, PersistenceServiceConfiguration> entry : persistenceServiceConfigs.entrySet()) {
                final String serviceName = entry.getKey();
                final PersistenceServiceConfiguration config = entry.getValue();
                if (persistenceServices.containsKey(serviceName)) {
                    for (SimpleItemConfiguration itemConfig : config.getConfigs()) {
                        if (hasStrategy(config, itemConfig,
                                onlyChanges ? SimpleStrategy.Globals.CHANGE : SimpleStrategy.Globals.UPDATE)) {
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
    private boolean hasStrategy(PersistenceServiceConfiguration config, SimpleItemConfiguration itemConfig,
            SimpleStrategy strategy) {
        if (config.getDefaults().contains(strategy) && itemConfig.getStrategies().isEmpty()) {
            return true;
        } else {
            for (SimpleStrategy s : itemConfig.getStrategies()) {
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
    private boolean appliesToItem(SimpleItemConfiguration config, Item item) {
        for (SimpleConfig itemCfg : config.getItems()) {
            if (itemCfg instanceof SimpleAllConfig) {
                return true;
            }
            if (itemCfg instanceof SimpleItemConfig) {
                SimpleItemConfig singleItemConfig = (SimpleItemConfig) itemCfg;
                if (item.getName().equals(singleItemConfig.getItem())) {
                    return true;
                }
            }
            if (itemCfg instanceof SimpleGroupConfig) {
                SimpleGroupConfig groupItemConfig = (SimpleGroupConfig) itemCfg;
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
    Iterable<Item> getAllItems(SimpleItemConfiguration config) {
        // first check, if we should return them all
        for (Object itemCfg : config.getItems()) {
            if (itemCfg instanceof SimpleAllConfig) {
                return itemRegistry.getItems();
            }
        }

        // otherwise, go through the detailed definitions
        Set<Item> items = new HashSet<>();
        for (Object itemCfg : config.getItems()) {
            if (itemCfg instanceof SimpleItemConfig) {
                SimpleItemConfig singleItemConfig = (SimpleItemConfig) itemCfg;
                String itemName = singleItemConfig.getItem();
                try {
                    items.add(itemRegistry.getItem(itemName));
                } catch (ItemNotFoundException e) {
                    logger.debug("Item '{}' does not exist.", itemName);
                }
            }
            if (itemCfg instanceof SimpleGroupConfig) {
                SimpleGroupConfig groupItemConfig = (SimpleGroupConfig) itemCfg;
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
        if (item.getState().equals(UnDefType.NULL) && item instanceof GenericItem) {
            for (Entry<String, PersistenceServiceConfiguration> entry : persistenceServiceConfigs.entrySet()) {
                final String serviceName = entry.getKey();
                final PersistenceServiceConfiguration config = entry.getValue();
                for (SimpleItemConfiguration itemConfig : config.getConfigs()) {
                    if (hasStrategy(config, itemConfig, SimpleStrategy.Globals.RESTORE)) {
                        if (appliesToItem(itemConfig, item)) {
                            PersistenceService service = persistenceServices.get(serviceName);
                            if (service instanceof QueryablePersistenceService) {
                                QueryablePersistenceService queryService = (QueryablePersistenceService) service;
                                FilterCriteria filter = new FilterCriteria().setItemName(item.getName()).setPageSize(1);
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
                                                    DateFormat.getDateTimeInstance()
                                                            .format(historicItem.getTimestamp()),
                                                    item.getName(), historicItem.getState());
                                        }
                                        return;
                                    }
                                }
                            } else if (service != null) {
                                logger.warn(
                                        "Failed to restore item states as persistence service '{}' can not be queried.",
                                        serviceName);
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
     * {@link SimpleStrategy strategies}.
     *
     * @param dbId the database id used by the persistence service
     * @param strategies a collection of strategies
     */
    private void createTimers(final String dbId, List<SimpleStrategy> strategies) {
        for (SimpleStrategy strategy : strategies) {
            if (strategy instanceof SimpleCronStrategy) {
                SimpleCronStrategy cronStrategy = (SimpleCronStrategy) strategy;
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
            persistenceServiceConfigs.remove(dbId);
        }
    }

    private void startEventHandling(final String serviceName) {
        synchronized (persistenceServiceConfigs) {
            final PersistenceServiceConfiguration config = persistenceServiceConfigs.get(serviceName);
            if (config != null) {
                for (SimpleItemConfiguration itemConfig : config.getConfigs()) {
                    if (hasStrategy(config, itemConfig, SimpleStrategy.Globals.RESTORE)) {
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
