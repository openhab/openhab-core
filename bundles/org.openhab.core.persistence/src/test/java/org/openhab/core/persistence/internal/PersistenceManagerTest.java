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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.common.SafeCallerBuilder;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.ModifiablePersistenceService;
import org.openhab.core.persistence.PersistenceItemConfiguration;
import org.openhab.core.persistence.PersistenceItemInfo;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.persistence.config.PersistenceAllConfig;
import org.openhab.core.persistence.config.PersistenceConfig;
import org.openhab.core.persistence.config.PersistenceGroupConfig;
import org.openhab.core.persistence.config.PersistenceItemConfig;
import org.openhab.core.persistence.filter.PersistenceFilter;
import org.openhab.core.persistence.filter.PersistenceThresholdFilter;
import org.openhab.core.persistence.registry.PersistenceServiceConfiguration;
import org.openhab.core.persistence.registry.PersistenceServiceConfigurationRegistry;
import org.openhab.core.persistence.strategy.PersistenceCronStrategy;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.openhab.core.scheduler.CronScheduler;
import org.openhab.core.scheduler.ScheduledCompletableFuture;
import org.openhab.core.scheduler.Scheduler;
import org.openhab.core.scheduler.SchedulerRunnable;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.UnDefType;

/**
 * The {@link PersistenceManagerTest} contains tests for the {@link PersistenceManager}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PersistenceManagerTest {
    private static final String TEST_ITEM_NAME = "testItem";
    private static final String TEST_ITEM2_NAME = "testItem2";
    private static final String TEST_ITEM3_NAME = "testItem3";
    private static final String TEST_GROUP_ITEM_NAME = "groupItem";

    private static final StringItem TEST_ITEM = new StringItem(TEST_ITEM_NAME);
    private static final StringItem TEST_ITEM2 = new StringItem(TEST_ITEM2_NAME);
    private static final NumberItem TEST_ITEM3 = new NumberItem(TEST_ITEM3_NAME);
    private static final GroupItem TEST_GROUP_ITEM = new GroupItem(TEST_GROUP_ITEM_NAME);

    private static final State TEST_STATE = new StringType("testState1");

    private static final HistoricItem TEST_HISTORIC_ITEM = new HistoricItem() {
        @Override
        public ZonedDateTime getTimestamp() {
            return ZonedDateTime.now().minusDays(1);
        }

        @Override
        public State getState() {
            return TEST_STATE;
        }

        @Override
        public String getName() {
            return TEST_ITEM_NAME;
        }
    };

    private static final String TEST_PERSISTENCE_SERVICE_ID = "testPersistenceService";
    private static final String TEST_QUERYABLE_PERSISTENCE_SERVICE_ID = "testQueryablePersistenceService";

    private static final String TEST_MODIFIABLE_PERSISTENCE_SERVICE_ID = "testModifiablePersistenceService";

    private @NonNullByDefault({}) @Mock CronScheduler cronSchedulerMock;
    private @NonNullByDefault({}) @Mock Scheduler schedulerMock;
    private @NonNullByDefault({}) @Mock ScheduledCompletableFuture<Void> scheduledFutureMock;
    private @NonNullByDefault({}) @Mock ItemRegistry itemRegistryMock;
    private @NonNullByDefault({}) @Mock SafeCaller safeCallerMock;
    private @NonNullByDefault({}) @Mock SafeCallerBuilder<QueryablePersistenceService> safeCallerBuilderMock;
    private @NonNullByDefault({}) @Mock ReadyService readyServiceMock;
    private @NonNullByDefault({}) @Mock PersistenceServiceConfigurationRegistry persistenceServiceConfigurationRegistryMock;

    private @NonNullByDefault({}) @Mock PersistenceService persistenceServiceMock;
    private @NonNullByDefault({}) @Mock QueryablePersistenceService queryablePersistenceServiceMock;
    private @NonNullByDefault({}) @Mock ModifiablePersistenceService modifiablePersistenceServiceMock;

    private @NonNullByDefault({}) PersistenceManager manager;

    @BeforeEach
    public void setUp() throws ItemNotFoundException {
        TEST_GROUP_ITEM.addMember(TEST_ITEM);

        // set initial states
        TEST_ITEM.setState(UnDefType.NULL);
        TEST_ITEM2.setState(UnDefType.NULL);
        TEST_ITEM3.setState(DecimalType.ZERO);
        TEST_GROUP_ITEM.setState(UnDefType.NULL);

        when(itemRegistryMock.getItem(TEST_GROUP_ITEM_NAME)).thenReturn(TEST_GROUP_ITEM);
        when(itemRegistryMock.getItem(TEST_ITEM_NAME)).thenReturn(TEST_ITEM);
        when(itemRegistryMock.getItem(TEST_ITEM2_NAME)).thenReturn(TEST_ITEM2);
        when(itemRegistryMock.getItem(TEST_ITEM3_NAME)).thenReturn(TEST_ITEM3);
        when(itemRegistryMock.getItems()).thenReturn(List.of(TEST_ITEM, TEST_ITEM2, TEST_ITEM3, TEST_GROUP_ITEM));
        when(persistenceServiceMock.getId()).thenReturn(TEST_PERSISTENCE_SERVICE_ID);
        when(queryablePersistenceServiceMock.getId()).thenReturn(TEST_QUERYABLE_PERSISTENCE_SERVICE_ID);
        when(queryablePersistenceServiceMock.query(any())).thenReturn(List.of(TEST_HISTORIC_ITEM));
        when(modifiablePersistenceServiceMock.getId()).thenReturn(TEST_MODIFIABLE_PERSISTENCE_SERVICE_ID);

        manager = new PersistenceManager(cronSchedulerMock, schedulerMock, itemRegistryMock, safeCallerMock,
                readyServiceMock, persistenceServiceConfigurationRegistryMock);
        manager.addPersistenceService(persistenceServiceMock);
        manager.addPersistenceService(queryablePersistenceServiceMock);
        manager.addPersistenceService(modifiablePersistenceServiceMock);

        clearInvocations(persistenceServiceMock, queryablePersistenceServiceMock, modifiablePersistenceServiceMock);
    }

    @Test
    public void appliesToItemWithItemConfig() {
        addConfiguration(TEST_PERSISTENCE_SERVICE_ID, new PersistenceItemConfig(TEST_ITEM_NAME),
                PersistenceStrategy.Globals.UPDATE, null);

        manager.stateUpdated(TEST_ITEM, TEST_STATE);

        verify(persistenceServiceMock).store(TEST_ITEM, null);
        verifyNoMoreInteractions(persistenceServiceMock);
    }

    @Test
    public void doesNotApplyToItemWithItemConfig() {
        addConfiguration(TEST_PERSISTENCE_SERVICE_ID, new PersistenceItemConfig(TEST_ITEM_NAME),
                PersistenceStrategy.Globals.UPDATE, null);

        manager.stateUpdated(TEST_ITEM2, TEST_STATE);

        verifyNoMoreInteractions(persistenceServiceMock);
    }

    @Test
    public void appliesToGroupItemWithItemConfig() {
        addConfiguration(TEST_PERSISTENCE_SERVICE_ID, new PersistenceItemConfig(TEST_GROUP_ITEM_NAME),
                PersistenceStrategy.Globals.UPDATE, null);

        manager.stateUpdated(TEST_GROUP_ITEM, TEST_STATE);

        verify(persistenceServiceMock).store(TEST_GROUP_ITEM, null);
        verifyNoMoreInteractions(persistenceServiceMock);
    }

    @Test
    public void appliesToItemWithGroupConfig() {
        addConfiguration(TEST_PERSISTENCE_SERVICE_ID, new PersistenceGroupConfig(TEST_GROUP_ITEM_NAME),
                PersistenceStrategy.Globals.UPDATE, null);

        manager.stateUpdated(TEST_ITEM, TEST_STATE);

        verify(persistenceServiceMock).store(TEST_ITEM, null);
        verifyNoMoreInteractions(persistenceServiceMock);
    }

    @Test
    public void doesNotApplyToItemWithGroupConfig() {
        addConfiguration(TEST_PERSISTENCE_SERVICE_ID, new PersistenceGroupConfig(TEST_GROUP_ITEM_NAME),
                PersistenceStrategy.Globals.UPDATE, null);

        manager.stateUpdated(TEST_ITEM2, TEST_STATE);
        manager.stateUpdated(TEST_GROUP_ITEM, TEST_STATE);

        verifyNoMoreInteractions(persistenceServiceMock);
    }

    @Test
    public void appliesToItemWithAllConfig() {
        addConfiguration(TEST_PERSISTENCE_SERVICE_ID, new PersistenceAllConfig(), PersistenceStrategy.Globals.UPDATE,
                null);

        manager.stateUpdated(TEST_ITEM, TEST_STATE);
        manager.stateUpdated(TEST_ITEM2, TEST_STATE);
        manager.stateUpdated(TEST_GROUP_ITEM, TEST_STATE);

        verify(persistenceServiceMock).store(TEST_ITEM, null);
        verify(persistenceServiceMock).store(TEST_ITEM2, null);
        verify(persistenceServiceMock).store(TEST_GROUP_ITEM, null);

        verifyNoMoreInteractions(persistenceServiceMock);
    }

    @Test
    public void updatedStatePersistsEveryUpdate() {
        addConfiguration(TEST_PERSISTENCE_SERVICE_ID, new PersistenceAllConfig(), PersistenceStrategy.Globals.UPDATE,
                null);

        manager.stateUpdated(TEST_ITEM, TEST_STATE);
        manager.stateUpdated(TEST_ITEM, TEST_STATE);

        verify(persistenceServiceMock, times(2)).store(TEST_ITEM, null);

        verifyNoMoreInteractions(persistenceServiceMock);
    }

    @Test
    public void updatedStateDoesNotPersistWithChangeStrategy() {
        addConfiguration(TEST_PERSISTENCE_SERVICE_ID, new PersistenceAllConfig(), PersistenceStrategy.Globals.CHANGE,
                null);

        manager.stateUpdated(TEST_ITEM, TEST_STATE);
        verifyNoMoreInteractions(persistenceServiceMock);
    }

    @Test
    public void changedStatePersistsWithChangeStrategy() {
        addConfiguration(TEST_PERSISTENCE_SERVICE_ID, new PersistenceAllConfig(), PersistenceStrategy.Globals.CHANGE,
                null);

        manager.stateChanged(TEST_ITEM, UnDefType.UNDEF, TEST_STATE);

        verify(persistenceServiceMock).store(TEST_ITEM, null);
        verifyNoMoreInteractions(persistenceServiceMock);
    }

    @Test
    public void changedStateDoesNotPersistWithUpdateStrategy() {
        addConfiguration(TEST_PERSISTENCE_SERVICE_ID, new PersistenceAllConfig(), PersistenceStrategy.Globals.UPDATE,
                null);

        manager.stateChanged(TEST_ITEM, UnDefType.UNDEF, TEST_STATE);

        verifyNoMoreInteractions(persistenceServiceMock);
    }

    @Test
    public void restoreOnStartupWhenItemNull() {
        setupPersistence(new PersistenceAllConfig());

        manager.onReadyMarkerAdded(new ReadyMarker("", ""));
        verify(readyServiceMock, timeout(1000)).markReady(any());

        assertThat(TEST_ITEM2.getState(), is(TEST_STATE));
        assertThat(TEST_ITEM.getState(), is(TEST_STATE));
        assertThat(TEST_GROUP_ITEM.getState(), is(TEST_STATE));

        verify(queryablePersistenceServiceMock, times(3)).query(any());

        verifyNoMoreInteractions(queryablePersistenceServiceMock);
        verifyNoMoreInteractions(persistenceServiceMock);
    }

    @Test
    public void noRestoreOnStartupWhenItemNotNull() {
        setupPersistence(new PersistenceAllConfig());

        // set TEST_ITEM state to a value
        StringType initialValue = new StringType("value");
        TEST_ITEM.setState(initialValue);

        manager.onReadyMarkerAdded(new ReadyMarker("", ""));
        verify(readyServiceMock, timeout(1000)).markReady(any());

        assertThat(TEST_ITEM.getState(), is(initialValue));
        assertThat(TEST_ITEM2.getState(), is(TEST_STATE));
        assertThat(TEST_GROUP_ITEM.getState(), is(TEST_STATE));

        verify(queryablePersistenceServiceMock, times(2)).query(any());

        verifyNoMoreInteractions(queryablePersistenceServiceMock);
        verifyNoMoreInteractions(persistenceServiceMock);
    }

    @Test
    public void storeTimeSeriesAndForecastsScheduled() {
        List<ScheduledCompletableFuture<?>> futures = new ArrayList<>();
        TestModifiablePersistenceService service = spy(new TestModifiablePersistenceService());
        manager.addPersistenceService(service);

        when(schedulerMock.at(any(SchedulerRunnable.class), any(Instant.class))).thenAnswer(i -> {
            ScheduledCompletableFuture<?> future = mock(ScheduledCompletableFuture.class);
            when(future.getScheduledTime()).thenReturn(((Instant) i.getArgument(1)).atZone(ZoneId.systemDefault()));
            futures.add(future);
            return future;
        });

        addConfiguration(TestModifiablePersistenceService.ID, new PersistenceAllConfig(),
                PersistenceStrategy.Globals.FORECAST, null);

        Instant time1 = Instant.now().minusSeconds(1000);
        Instant time2 = Instant.now().plusSeconds(1000);
        Instant time3 = Instant.now().plusSeconds(2000);
        Instant time4 = Instant.now().plusSeconds(3000);

        // add elements
        TimeSeries timeSeries = new TimeSeries(TimeSeries.Policy.ADD);
        timeSeries.add(time1, new StringType("one"));
        timeSeries.add(time2, new StringType("two"));
        timeSeries.add(time3, new StringType("three"));
        timeSeries.add(time4, new StringType("four"));

        manager.timeSeriesUpdated(TEST_ITEM, timeSeries);
        InOrder inOrder = inOrder(service, schedulerMock);

        // verify elements are stored
        timeSeries.getStates().forEach(entry -> inOrder.verify(service).store(any(Item.class),
                eq(entry.timestamp().atZone(ZoneId.systemDefault())), eq(entry.state())));

        // first element not scheduled, because it is in the past, check if second is scheduled
        inOrder.verify(schedulerMock).at(any(SchedulerRunnable.class), eq(time2));
        inOrder.verifyNoMoreInteractions();

        // replace elements
        TimeSeries timeSeries2 = new TimeSeries(TimeSeries.Policy.REPLACE);
        timeSeries2.add(time3, new StringType("three2"));
        timeSeries2.add(time4, new StringType("four2"));

        manager.timeSeriesUpdated(TEST_ITEM, timeSeries2);

        // verify removal of old elements from service
        ArgumentCaptor<FilterCriteria> filterCaptor = ArgumentCaptor.forClass(FilterCriteria.class);
        inOrder.verify(service).remove(filterCaptor.capture());
        FilterCriteria filterCriteria = filterCaptor.getValue();
        assertThat(filterCriteria.getItemName(), is(TEST_ITEM_NAME));
        assertThat(filterCriteria.getBeginDate(), is(time3.atZone(ZoneId.systemDefault())));
        assertThat(filterCriteria.getEndDate(), is(time4.atZone(ZoneId.systemDefault())));

        // verify restore future is not cancelled
        verify(futures.get(0), never()).cancel(anyBoolean());

        // verify new values are stored
        inOrder.verify(service, times(2)).store(any(Item.class), any(ZonedDateTime.class), any(State.class));
        inOrder.verifyNoMoreInteractions();

        // try adding a new element in front and check it is correctly scheduled
        Instant time5 = Instant.now().plusSeconds(500);
        // add elements
        TimeSeries timeSeries3 = new TimeSeries(TimeSeries.Policy.ADD);
        timeSeries3.add(time5, new StringType("five"));

        manager.timeSeriesUpdated(TEST_ITEM, timeSeries3);
        // verify old restore future is cancelled
        inOrder.verify(service, times(1)).store(any(Item.class), any(ZonedDateTime.class), any(State.class));
        verify(futures.get(0)).cancel(true);

        // verify new restore future is properly created
        inOrder.verify(schedulerMock).at(any(SchedulerRunnable.class), eq(time5));
    }

    @Test
    public void cronStrategyIsScheduledAndCancelledAndPersistsValue() throws Exception {
        ArgumentCaptor<SchedulerRunnable> runnableCaptor = ArgumentCaptor.forClass(SchedulerRunnable.class);
        when(cronSchedulerMock.schedule(runnableCaptor.capture(), any())).thenReturn(scheduledFutureMock);

        addConfiguration(TEST_PERSISTENCE_SERVICE_ID, new PersistenceItemConfig(TEST_ITEM3_NAME),
                new PersistenceCronStrategy("withoutFilter", "0 0 * * * ?"), null);
        addConfiguration(TEST_QUERYABLE_PERSISTENCE_SERVICE_ID, new PersistenceItemConfig(TEST_ITEM3_NAME),
                new PersistenceCronStrategy("withFilter", "0 * * * * ?"),
                new PersistenceThresholdFilter("test", BigDecimal.TEN, "", false));

        manager.onReadyMarkerAdded(new ReadyMarker("", ""));

        verify(readyServiceMock, timeout(1000)).markReady(any());
        List<SchedulerRunnable> runnables = runnableCaptor.getAllValues();
        assertThat(runnables.size(), is(2));
        runnables.get(0).run();
        runnables.get(0).run();
        runnables.get(1).run();
        runnables.get(1).run();

        manager.deactivate();

        verify(cronSchedulerMock, times(2)).schedule(any(), any());
        verify(scheduledFutureMock, times(2)).cancel(true);
        // no filter - persist everything
        verify(persistenceServiceMock, times(2)).store(TEST_ITEM3, null);
        // filter - persist filtered value
        verify(queryablePersistenceServiceMock, times(1)).store(TEST_ITEM3, null);
    }

    @Test
    public void cronStrategyIsProperlyUpdated() {
        when(cronSchedulerMock.schedule(any(), any())).thenReturn(scheduledFutureMock);

        PersistenceServiceConfiguration configuration = addConfiguration(TEST_PERSISTENCE_SERVICE_ID,
                new PersistenceItemConfig(TEST_ITEM_NAME), new PersistenceCronStrategy("everyHour", "0 0 * * * ?"),
                null);

        manager.onReadyMarkerAdded(new ReadyMarker("", ""));

        verify(readyServiceMock, timeout(1000)).markReady(any());

        manager.updated(configuration, configuration);
        manager.deactivate();

        verify(cronSchedulerMock, times(2)).schedule(any(), any());
        verify(scheduledFutureMock, times(2)).cancel(true);
    }

    @Test
    public void filterAppliesOnStateUpdate() {
        addConfiguration(TEST_PERSISTENCE_SERVICE_ID, new PersistenceAllConfig(), PersistenceStrategy.Globals.UPDATE,
                new PersistenceThresholdFilter("test", BigDecimal.TEN, "", false));

        manager.stateUpdated(TEST_ITEM3, DecimalType.ZERO);
        manager.stateUpdated(TEST_ITEM3, DecimalType.ZERO);

        verify(persistenceServiceMock, times(1)).store(TEST_ITEM3, null);

        verifyNoMoreInteractions(persistenceServiceMock);
    }

    /**
     * Add a configuration for restoring TEST_ITEM and mock the SafeCaller
     */
    private void setupPersistence(PersistenceConfig itemConfig) {
        addConfiguration(TEST_PERSISTENCE_SERVICE_ID, itemConfig, PersistenceStrategy.Globals.RESTORE, null);
        addConfiguration(TEST_QUERYABLE_PERSISTENCE_SERVICE_ID, itemConfig, PersistenceStrategy.Globals.RESTORE, null);

        when(safeCallerMock.create(queryablePersistenceServiceMock, QueryablePersistenceService.class))
                .thenReturn(safeCallerBuilderMock);
        when(safeCallerBuilderMock.onTimeout(any())).thenReturn(safeCallerBuilderMock);
        when(safeCallerBuilderMock.onException(any())).thenReturn(safeCallerBuilderMock);
        when(safeCallerBuilderMock.build()).thenReturn(queryablePersistenceServiceMock);
    }

    /**
     * Add a configuration to the manager
     *
     * @param serviceId the persistence service id
     * @param itemConfig the item configuration
     * @param strategy the strategy
     * @param filter a persistence filter
     * @return the added strategy
     */
    private PersistenceServiceConfiguration addConfiguration(String serviceId, PersistenceConfig itemConfig,
            PersistenceStrategy strategy, @Nullable PersistenceFilter filter) {
        List<PersistenceFilter> filters = filter != null ? List.of(filter) : List.of();

        PersistenceItemConfiguration itemConfiguration = new PersistenceItemConfiguration(List.of(itemConfig), null,
                List.of(strategy), filters);

        List<PersistenceStrategy> strategies = PersistenceStrategy.Globals.STRATEGIES.containsValue(strategy)
                ? List.of()
                : List.of(strategy);

        PersistenceServiceConfiguration serviceConfiguration = new PersistenceServiceConfiguration(serviceId,
                List.of(itemConfiguration), List.of(), strategies, filters);
        manager.added(serviceConfiguration);

        return serviceConfiguration;
    }

    private static class TestModifiablePersistenceService implements ModifiablePersistenceService {
        public static final String ID = "TMPS";
        private final Map<ZonedDateTime, State> states = new HashMap<>();

        @Override
        public void store(Item item, ZonedDateTime date, State state) {
            states.put(date, state);
        }

        @Override
        public void store(Item item, ZonedDateTime date, State state, @Nullable String alias) {
            store(item, date, state);
        }

        @Override
        public boolean remove(FilterCriteria filter) throws IllegalArgumentException {
            ZonedDateTime begin = Objects.requireNonNull(filter.getBeginDate());
            ZonedDateTime end = Objects.requireNonNull(filter.getEndDate());
            List<ZonedDateTime> keys = states.keySet().stream().filter(t -> t.isAfter(begin) && t.isBefore(end))
                    .toList();
            keys.forEach(states::remove);
            return !keys.isEmpty();
        }

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public String getLabel(@Nullable Locale locale) {
            return ID;
        }

        @Override
        public void store(Item item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void store(Item item, @Nullable String alias) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<PersistenceStrategy> getDefaultStrategies() {
            return List.of();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Iterable<HistoricItem> query(FilterCriteria filter) {
            ZonedDateTime begin = Objects.requireNonNull(filter.getBeginDate());
            ZonedDateTime end = Objects.requireNonNull(filter.getEndDate());
            List<ZonedDateTime> keys = states.keySet().stream().filter(t -> t.isAfter(begin) && t.isBefore(end))
                    .toList();
            return (Iterable<HistoricItem>) states.entrySet().stream().filter(e -> keys.contains(e.getKey()))
                    .map(e -> new HistoricItem() {
                        @Override
                        public ZonedDateTime getTimestamp() {
                            return e.getKey();
                        }

                        @Override
                        public State getState() {
                            return e.getValue();
                        }

                        @Override
                        public String getName() {
                            return "item";
                        }
                    }).iterator();
        }

        @Override
        public Set<PersistenceItemInfo> getItemInfo() {
            return Set.of();
        }
    }
}
