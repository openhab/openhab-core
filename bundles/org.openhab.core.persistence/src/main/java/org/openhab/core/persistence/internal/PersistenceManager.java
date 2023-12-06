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
package org.openhab.core.persistence.internal;

import static org.openhab.core.persistence.FilterCriteria.Ordering.ASCENDING;
import static org.openhab.core.persistence.strategy.PersistenceStrategy.Globals.FORECAST;
import static org.openhab.core.persistence.strategy.PersistenceStrategy.Globals.RESTORE;
import static org.openhab.core.persistence.strategy.PersistenceStrategy.Globals.UPDATE;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemRegistryChangeListener;
import org.openhab.core.items.StateChangeListener;
import org.openhab.core.items.TimeSeriesListener;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.ModifiablePersistenceService;
import org.openhab.core.persistence.PersistenceItemConfiguration;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.persistence.config.PersistenceAllConfig;
import org.openhab.core.persistence.config.PersistenceConfig;
import org.openhab.core.persistence.config.PersistenceGroupConfig;
import org.openhab.core.persistence.config.PersistenceItemConfig;
import org.openhab.core.persistence.registry.PersistenceServiceConfiguration;
import org.openhab.core.persistence.registry.PersistenceServiceConfigurationRegistry;
import org.openhab.core.persistence.registry.PersistenceServiceConfigurationRegistryChangeListener;
import org.openhab.core.persistence.strategy.PersistenceCronStrategy;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.openhab.core.scheduler.CronScheduler;
import org.openhab.core.scheduler.ScheduledCompletableFuture;
import org.openhab.core.scheduler.Scheduler;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.ReadyService.ReadyTracker;
import org.openhab.core.service.StartLevelService;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
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
 * @author Jan N. Klug - Refactored to use service configuration registry
 * @author Jan N. Klug - Added time series support
 */
@Component(immediate = true)
@NonNullByDefault
public class PersistenceManager implements ItemRegistryChangeListener, StateChangeListener, ReadyTracker,
        PersistenceServiceConfigurationRegistryChangeListener, TimeSeriesListener {
    private final Logger logger = LoggerFactory.getLogger(PersistenceManager.class);

    private final ReadyMarker marker = new ReadyMarker("persistence", "restore");

    // the scheduler used for timer events
    private final CronScheduler cronScheduler;
    private final Scheduler scheduler;
    private final ItemRegistry itemRegistry;
    private final SafeCaller safeCaller;
    private final ReadyService readyService;
    private final PersistenceServiceConfigurationRegistry persistenceServiceConfigurationRegistry;

    private volatile boolean started = false;

    private final Map<String, PersistenceServiceContainer> persistenceServiceContainers = new ConcurrentHashMap<>();

    @Activate
    public PersistenceManager(final @Reference CronScheduler cronScheduler, final @Reference Scheduler scheduler,
            final @Reference ItemRegistry itemRegistry, final @Reference SafeCaller safeCaller,
            final @Reference ReadyService readyService,
            final @Reference PersistenceServiceConfigurationRegistry persistenceServiceConfigurationRegistry) {
        this.cronScheduler = cronScheduler;
        this.scheduler = scheduler;
        this.itemRegistry = itemRegistry;
        this.safeCaller = safeCaller;
        this.readyService = readyService;
        this.persistenceServiceConfigurationRegistry = persistenceServiceConfigurationRegistry;

        persistenceServiceConfigurationRegistry.addRegistryChangeListener(this);
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(StartLevelService.STARTLEVEL_MARKER_TYPE)
                .withIdentifier(Integer.toString(StartLevelService.STARTLEVEL_MODEL)));
    }

    @Deactivate
    protected void deactivate() {
        itemRegistry.removeRegistryChangeListener(this);
        persistenceServiceConfigurationRegistry.removeRegistryChangeListener(this);
        started = false;

        persistenceServiceContainers.values().forEach(PersistenceServiceContainer::cancelPersistJobs);
        persistenceServiceContainers.values().forEach(PersistenceServiceContainer::cancelForecastJobs);

        // remove item state change listeners
        itemRegistry.stream().filter(GenericItem.class::isInstance)
                .forEach(item -> ((GenericItem) item).removeStateChangeListener(this));
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addPersistenceService(PersistenceService persistenceService) {
        String serviceId = persistenceService.getId();
        logger.debug("Initializing {} persistence service.", serviceId);
        PersistenceServiceContainer container = new PersistenceServiceContainer(persistenceService,
                persistenceServiceConfigurationRegistry.get(serviceId));

        PersistenceServiceContainer oldContainer = persistenceServiceContainers.put(serviceId, container);

        if (oldContainer != null) { // cancel all jobs if the persistence service is set and an old configuration is
                                    // already present
            oldContainer.cancelPersistJobs();
            oldContainer.cancelForecastJobs();
        }

        if (started) {
            startEventHandling(container);
        }
    }

    protected void removePersistenceService(PersistenceService persistenceService) {
        PersistenceServiceContainer container = persistenceServiceContainers.remove(persistenceService.getId());
        if (container != null) {
            container.cancelPersistJobs();
            container.cancelForecastJobs();
        }
    }

    /**
     * Calls all persistence services which use change or update policy for the given item
     *
     * @param item the item to persist
     * @param changed true, if it has the change strategy, false otherwise
     */
    private void handleStateEvent(Item item, boolean changed) {
        PersistenceStrategy changeStrategy = changed ? PersistenceStrategy.Globals.CHANGE
                : PersistenceStrategy.Globals.UPDATE;

        persistenceServiceContainers.values()
                .forEach(container -> container.getMatchingConfigurations(changeStrategy)
                        .filter(itemConfig -> appliesToItem(itemConfig, item))
                        .filter(itemConfig -> itemConfig.filters().stream().allMatch(filter -> filter.apply(item)))
                        .forEach(itemConfig -> {
                            itemConfig.filters().forEach(filter -> filter.persisted(item));
                            container.getPersistenceService().store(item, itemConfig.alias());
                        }));
    }

    /**
     * Checks if a given persistence configuration entry is relevant for an item
     *
     * @param itemConfig the persistence configuration entry
     * @param item to check if the configuration applies to
     * @return true, if the configuration applies to the item
     */
    private boolean appliesToItem(PersistenceItemConfiguration itemConfig, Item item) {
        for (PersistenceConfig itemCfg : itemConfig.items()) {
            if (itemCfg instanceof PersistenceAllConfig) {
                return true;
            } else if (itemCfg instanceof PersistenceItemConfig) {
                if (item.getName().equals(((PersistenceItemConfig) itemCfg).getItem())) {
                    return true;
                }
            } else if (itemCfg instanceof PersistenceGroupConfig) {
                try {
                    Item gItem = itemRegistry.getItem(((PersistenceGroupConfig) itemCfg).getGroup());
                    if (gItem instanceof GroupItem gItem2 && gItem2.getAllMembers().contains(item)) {
                        return true;
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
    private Iterable<Item> getAllItems(PersistenceItemConfiguration config) {
        // first check, if we should return them all
        if (config.items().stream().anyMatch(PersistenceAllConfig.class::isInstance)) {
            return itemRegistry.getItems();
        }

        // otherwise, go through the detailed definitions
        Set<Item> items = new HashSet<>();
        for (Object itemCfg : config.items()) {
            if (itemCfg instanceof PersistenceItemConfig) {
                String itemName = ((PersistenceItemConfig) itemCfg).getItem();
                try {
                    items.add(itemRegistry.getItem(itemName));
                } catch (ItemNotFoundException e) {
                    logger.debug("Item '{}' does not exist.", itemName);
                }
            }
            if (itemCfg instanceof PersistenceGroupConfig) {
                String groupName = ((PersistenceGroupConfig) itemCfg).getGroup();
                try {
                    Item gItem = itemRegistry.getItem(groupName);
                    if (gItem instanceof GroupItem groupItem) {
                        items.addAll(groupItem.getAllMembers());
                    }
                } catch (ItemNotFoundException e) {
                    logger.debug("Item group '{}' does not exist.", groupName);
                }
            }
        }
        return items;
    }

    private void startEventHandling(PersistenceServiceContainer serviceContainer) {
        serviceContainer.restoreStatesAndScheduleForecastJobs();
        serviceContainer.schedulePersistJobs();
    }

    // ItemStateChangeListener methods

    @Override
    public void allItemsChanged(Collection<String> oldItemNames) {
        itemRegistry.getItems().forEach(this::added);
    }

    @Override
    public void added(Item item) {
        persistenceServiceContainers.values().forEach(container -> container.addItem(item));
        if (item instanceof GenericItem genericItem) {
            genericItem.addStateChangeListener(this);
            genericItem.addTimeSeriesListener(this);
        }
    }

    @Override
    public void removed(Item item) {
        persistenceServiceContainers.values().forEach(container -> container.removeItem(item.getName()));
        if (item instanceof GenericItem genericItem) {
            genericItem.removeStateChangeListener(this);
            genericItem.removeTimeSeriesListener(this);
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

    @Override
    public void timeSeriesUpdated(Item item, TimeSeries timeSeries) {
        if (timeSeries.size() == 0) {
            // discard empty time series
            return;
        }
        persistenceServiceContainers.values().stream()
                .filter(psc -> psc.persistenceService instanceof ModifiablePersistenceService)
                .forEach(container -> Stream
                        .concat(container.getMatchingConfigurations(UPDATE),
                                container.getMatchingConfigurations(FORECAST))
                        .distinct().filter(itemConfig -> appliesToItem(itemConfig, item)).forEach(itemConfig -> {
                            ModifiablePersistenceService service = (ModifiablePersistenceService) container
                                    .getPersistenceService();
                            // remove old values if replace selected
                            if (timeSeries.getPolicy() == TimeSeries.Policy.REPLACE) {
                                ZonedDateTime begin = timeSeries.getBegin().atZone(ZoneId.systemDefault());
                                ZonedDateTime end = timeSeries.getEnd().atZone(ZoneId.systemDefault());
                                FilterCriteria removeFilter = new FilterCriteria().setItemName(item.getName())
                                        .setBeginDate(begin).setEndDate(end);
                                service.remove(removeFilter);
                                ScheduledCompletableFuture<?> forecastJob = container.forecastJobs.get(item.getName());
                                if (forecastJob != null && forecastJob.getScheduledTime().isAfter(begin)
                                        && forecastJob.getScheduledTime().isBefore(end)) {
                                    forecastJob.cancel(true);
                                    container.forecastJobs.remove(item.getName());
                                }
                            }
                            // update states
                            timeSeries.getStates().forEach(
                                    e -> service.store(item, e.timestamp().atZone(ZoneId.systemDefault()), e.state()));
                            timeSeries.getStates().filter(s -> s.timestamp().isAfter(Instant.now())).findFirst()
                                    .ifPresent(s -> {
                                        ScheduledCompletableFuture<?> forecastJob = container.forecastJobs
                                                .get(item.getName());
                                        if (forecastJob == null || forecastJob.getScheduledTime()
                                                .isAfter(s.timestamp().atZone(ZoneId.systemDefault()))) {
                                            container.scheduleNextForecastForItem(item.getName(), s.timestamp(),
                                                    s.state());
                                        }
                                    });
                        }));
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        ExecutorService scheduler = Executors.newSingleThreadExecutor(new NamedThreadFactory("persistenceManager"));
        scheduler.submit(() -> {
            allItemsChanged(Set.of());
            persistenceServiceContainers.values().forEach(this::startEventHandling);
            started = true;
            readyService.markReady(marker);
            itemRegistry.addRegistryChangeListener(this);
        });
        scheduler.shutdown();
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        readyService.unmarkReady(marker);
    }

    @Override
    public void added(PersistenceServiceConfiguration element) {
        PersistenceServiceContainer container = persistenceServiceContainers.get(element.getUID());
        if (container != null) {
            container.setConfiguration(element);
            if (started) {
                startEventHandling(container);
            }
        }
    }

    @Override
    public void removed(PersistenceServiceConfiguration element) {
        PersistenceServiceContainer container = persistenceServiceContainers.get(element.getUID());
        if (container != null) {
            container.setConfiguration(null);
            if (started) {
                startEventHandling(container);
            }
        }
    }

    @Override
    public void updated(PersistenceServiceConfiguration oldElement, PersistenceServiceConfiguration element) {
        // no need to remove before, configuration is overwritten if possible
        added(element);
    }

    private class PersistenceServiceContainer {
        private final PersistenceService persistenceService;
        private final Set<ScheduledCompletableFuture<?>> persistJobs = new HashSet<>();
        private final Map<String, ScheduledCompletableFuture<?>> forecastJobs = new ConcurrentHashMap<>();
        private final Map<PersistenceStrategy, Collection<PersistenceItemConfiguration>> strategyCache = new ConcurrentHashMap<>();

        private PersistenceServiceConfiguration configuration;

        public PersistenceServiceContainer(PersistenceService persistenceService,
                @Nullable PersistenceServiceConfiguration configuration) {
            this.persistenceService = persistenceService;
            this.configuration = Objects.requireNonNullElseGet(configuration, this::getDefaultConfig);
        }

        public PersistenceService getPersistenceService() {
            return persistenceService;
        }

        /**
         * Set a new configuration for this persistence service (also cancels all cron jobs)
         *
         * @param configuration the new {@link PersistenceServiceConfiguration}, if {@code null} the default
         *            configuration of the service is used
         */
        public void setConfiguration(@Nullable PersistenceServiceConfiguration configuration) {
            cancelPersistJobs();
            cancelForecastJobs();
            this.configuration = Objects.requireNonNullElseGet(configuration, this::getDefaultConfig);
            strategyCache.clear();
        }

        /**
         * Get all item configurations from this service that match a certain strategy
         *
         * @param strategy the {@link PersistenceStrategy} to look for
         * @return a {@link Stream<PersistenceItemConfiguration>} of the result
         */
        public Stream<PersistenceItemConfiguration> getMatchingConfigurations(PersistenceStrategy strategy) {
            return Objects.requireNonNull(strategyCache.computeIfAbsent(strategy, s -> {
                boolean matchesDefaultStrategies = configuration.getDefaults().contains(strategy);
                return configuration.getConfigs().stream()
                        .filter(itemConfig -> itemConfig.strategies().contains(strategy)
                                || (itemConfig.strategies().isEmpty() && matchesDefaultStrategies))
                        .toList();
            }).stream());
        }

        private PersistenceServiceConfiguration getDefaultConfig() {
            List<PersistenceStrategy> strategies = persistenceService.getDefaultStrategies();
            List<PersistenceItemConfiguration> configs = List
                    .of(new PersistenceItemConfiguration(List.of(new PersistenceAllConfig()), null, strategies, null));
            return new PersistenceServiceConfiguration(persistenceService.getId(), configs, strategies, strategies,
                    List.of());
        }

        /**
         * Cancel all scheduled cron jobs / strategies for this service
         */
        public void cancelPersistJobs() {
            synchronized (persistJobs) {
                persistJobs.forEach(job -> job.cancel(true));
                persistJobs.clear();
            }
            logger.debug("Removed scheduled cron jobs for persistence service '{}'", configuration.getUID());
        }

        public void cancelForecastJobs() {
            synchronized (forecastJobs) {
                forecastJobs.values().forEach(job -> job.cancel(true));
                forecastJobs.clear();
            }
            logger.debug("Removed scheduled forecast jobs for persistence service '{}'", configuration.getUID());
        }

        /**
         * Schedule all necessary cron jobs / strategies for this service
         */
        public void schedulePersistJobs() {
            configuration.getStrategies().stream().filter(PersistenceCronStrategy.class::isInstance)
                    .forEach(strategy -> {
                        PersistenceCronStrategy cronStrategy = (PersistenceCronStrategy) strategy;
                        String cronExpression = cronStrategy.getCronExpression();
                        List<PersistenceItemConfiguration> itemConfigs = getMatchingConfigurations(strategy).toList();
                        persistJobs.add(cronScheduler.schedule(() -> persistJob(itemConfigs), cronExpression));

                        logger.debug("Scheduled strategy {} with cron expression {} for service {}",
                                cronStrategy.getName(), cronExpression, configuration.getUID());

                    });
        }

        public void restoreStatesAndScheduleForecastJobs() {
            itemRegistry.getItems().forEach(this::addItem);
        }

        public void addItem(Item item) {
            if (persistenceService instanceof QueryablePersistenceService) {
                if (UnDefType.NULL.equals(item.getState())
                        && (getMatchingConfigurations(RESTORE)
                                .anyMatch(configuration -> appliesToItem(configuration, item)))
                        || getMatchingConfigurations(FORECAST)
                                .anyMatch(configuration -> appliesToItem(configuration, item))) {
                    restoreItemStateIfPossible(item);
                }
                if (getMatchingConfigurations(FORECAST).anyMatch(configuration -> appliesToItem(configuration, item))) {
                    scheduleNextPersistedForecastForItem(item.getName());

                }
            }
        }

        public void removeItem(String itemName) {
            ScheduledCompletableFuture<?> job = forecastJobs.remove(itemName);
            if (job != null) {
                job.cancel(true);
            }
        }

        private void restoreItemStateIfPossible(Item item) {
            QueryablePersistenceService queryService = (QueryablePersistenceService) persistenceService;

            FilterCriteria filter = new FilterCriteria().setItemName(item.getName()).setEndDate(ZonedDateTime.now())
                    .setPageSize(1);
            Iterable<HistoricItem> result = safeCaller.create(queryService, QueryablePersistenceService.class)
                    .onTimeout(
                            () -> logger.warn("Querying persistence service '{}' to restore '{}' takes more than {}ms.",
                                    queryService.getId(), item.getName(), SafeCaller.DEFAULT_TIMEOUT))
                    .onException(e -> logger.error(
                            "Exception occurred while querying persistence service '{}' to restore '{}': {}",
                            queryService.getId(), item.getName(), e.getMessage(), e))
                    .build().query(filter);
            if (result == null) {
                // in case of an exception or timeout, the safe caller returns null
                return;
            }
            Iterator<HistoricItem> it = result.iterator();
            if (it.hasNext()) {
                HistoricItem historicItem = it.next();
                GenericItem genericItem = (GenericItem) item;
                if (!UnDefType.NULL.equals(item.getState())) {
                    // someone else already restored the state or a new state was set
                    return;
                }
                genericItem.removeStateChangeListener(PersistenceManager.this);
                genericItem.setState(historicItem.getState());
                genericItem.addStateChangeListener(PersistenceManager.this);
                if (logger.isDebugEnabled()) {
                    logger.debug("Restored item state from '{}' for item '{}' -> '{}'",
                            DateTimeFormatter.ISO_ZONED_DATE_TIME.format(historicItem.getTimestamp()), item.getName(),
                            historicItem.getState());
                }
            }
        }

        public void scheduleNextForecastForItem(String itemName, Instant time, State state) {
            ScheduledFuture<?> oldJob = forecastJobs.remove(itemName);
            if (oldJob != null) {
                oldJob.cancel(true);
            }
            forecastJobs.put(itemName, scheduler.at(() -> restoreItemState(itemName, state), time));
            logger.trace("Scheduled forecasted value for {} at {}", itemName, time);
        }

        public void scheduleNextPersistedForecastForItem(String itemName) {
            Item item = itemRegistry.get(itemName);
            if (item instanceof GenericItem) {
                QueryablePersistenceService queryService = (QueryablePersistenceService) persistenceService;
                FilterCriteria filter = new FilterCriteria().setItemName(itemName).setBeginDate(ZonedDateTime.now())
                        .setOrdering(ASCENDING);
                Iterator<HistoricItem> result = safeCaller.create(queryService, QueryablePersistenceService.class)
                        .onTimeout(() -> logger.warn("Querying persistence service '{}' takes more than {}ms.",
                                queryService.getId(), SafeCaller.DEFAULT_TIMEOUT))
                        .onException(e -> logger.error("Exception occurred while querying persistence service '{}': {}",
                                queryService.getId(), e.getMessage(), e))
                        .build().query(filter).iterator();
                while (result.hasNext()) {
                    HistoricItem next = result.next();
                    if (next.getTimestamp().isAfter(ZonedDateTime.now())) {
                        scheduleNextForecastForItem(itemName, next.getTimestamp().toInstant(), next.getState());
                        break;
                    }
                }
            }
        }

        private void restoreItemState(String itemName, State state) {
            Item item = itemRegistry.get(itemName);
            if (item != null) {
                ((GenericItem) item).setState(state);
            }
            scheduleNextPersistedForecastForItem(itemName);
        }

        private void persistJob(List<PersistenceItemConfiguration> itemConfigs) {
            itemConfigs.forEach(itemConfig -> {
                for (Item item : getAllItems(itemConfig)) {
                    if (itemConfig.filters().stream().allMatch(filter -> filter.apply(item))) {
                        long startTime = System.nanoTime();
                        itemConfig.filters().forEach(filter -> filter.persisted(item));
                        persistenceService.store(item, itemConfig.alias());
                        logger.trace("Storing item '{}' with persistence service '{}' took {}ms", item.getName(),
                                configuration.getUID(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
                    }
                }
            });
        }
    }
}
