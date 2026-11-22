/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.smoke.test;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.openhab.core.service.StartLevelService.*;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.ModifiablePersistenceService;
import org.openhab.core.persistence.PersistenceItemConfiguration;
import org.openhab.core.persistence.PersistenceManager;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.config.PersistenceItemConfig;
import org.openhab.core.persistence.registry.PersistenceServiceConfiguration;
import org.openhab.core.persistence.registry.PersistenceServiceConfigurationRegistry;
import org.openhab.core.persistence.strategy.PersistenceStrategy.Globals;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.osgi.framework.ServiceRegistration;

/**
 * Exercises the event bus, item updater and persistence manager with a registered persistence backend.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
public class PersistenceSmokeTest extends JavaOSGiTest {
    private static final String ITEM = "SmokeTemperature";
    private static final String ALIAS = "temperature";

    @Test
    public void savesTimeSeriesAndReplacesOnlyTheRequestedInterval() {
        ItemRegistry items = requireNonNull(getService(ItemRegistry.class));
        PersistenceServiceConfigurationRegistry configs = requireNonNull(
                getService(PersistenceServiceConfigurationRegistry.class));
        requireNonNull(getService(PersistenceManager.class));
        ReadyService ready = requireNonNull(getService(ReadyService.class));
        EventPublisher events = requireNonNull(getService(EventPublisher.class));
        ((AbstractRegistry<?, ?, ?>) items).waitForCompletedAsyncActivationTasks();
        ((AbstractRegistry<?, ?, ?>) configs).waitForCompletedAsyncActivationTasks();

        NumberItem item = new NumberItem(ITEM);
        RecordingBackend backend = new RecordingBackend();
        PersistenceItemConfiguration entry = new PersistenceItemConfiguration(List.of(new PersistenceItemConfig(ITEM)),
                List.of(Globals.UPDATE), List.of());
        PersistenceServiceConfiguration config = new PersistenceServiceConfiguration(backend.getId(), List.of(entry),
                Map.of(ITEM, ALIAS), List.of(), List.of());
        ReadyMarker modelReady = new ReadyMarker(STARTLEVEL_MARKER_TYPE, Integer.toString(STARTLEVEL_MODEL));
        ReadyMarker restored = new ReadyMarker("persistence", "restore");
        ServiceRegistration<PersistenceService> registration = getBundleContext()
                .registerService(PersistenceService.class, backend, null);
        try {
            items.add(item);
            configs.add(config);
            ready.markReady(modelReady);
            waitForAssert(() -> assertTrue(ready.isReady(restored)), 15_000, 50);

            Instant before = Instant.parse("2025-01-01T00:00:00Z");
            Instant begin = before.plusSeconds(60);
            Instant obsolete = begin.plusSeconds(60);
            Instant end = obsolete.plusSeconds(60);
            Instant after = end.plusSeconds(60);
            DecimalType beforeState = new DecimalType(20);
            DecimalType firstState = new DecimalType(21);
            DecimalType obsoleteState = new DecimalType(22);
            DecimalType replacement = new DecimalType(23);
            DecimalType lastState = new DecimalType(24);
            DecimalType afterState = new DecimalType(25);

            TimeSeries initial = new TimeSeries(TimeSeries.Policy.ADD);
            initial.add(before, beforeState);
            initial.add(begin, firstState);
            initial.add(obsolete, obsoleteState);
            initial.add(after, afterState);
            events.post(ItemEventFactory.createTimeSeriesEvent(ITEM, initial, "smoke"));
            waitForAssert(() -> {
                assertEquals(afterState, item.getState());
                assertEquals(Map.of(before, beforeState, begin, firstState, obsolete, obsoleteState, after, afterState),
                        backend.samples);
            }, 15_000, 50);

            TimeSeries update = new TimeSeries(TimeSeries.Policy.REPLACE);
            update.add(begin, replacement);
            update.add(end, lastState);
            events.post(ItemEventFactory.createTimeSeriesEvent(ITEM, update, "smoke"));
            waitForAssert(() -> {
                assertEquals(Map.of(before, beforeState, begin, replacement, end, lastState, after, afterState),
                        backend.samples);
                assertEquals(afterState, item.getState());
                ZonedDateTime stateUpdate = item.getLastStateUpdate();
                assertNotNull(stateUpdate);
                assertEquals(after, stateUpdate.toInstant());
            }, 15_000, 50);
        } finally {
            ready.unmarkReady(modelReady);
            configs.remove(config.getUID());
            items.remove(ITEM);
            registration.unregister();
        }
    }

    private static class RecordingBackend implements ModifiablePersistenceService {
        private final NavigableMap<Instant, State> samples = new ConcurrentSkipListMap<>();

        @Override
        public String getId() {
            return "smoke-persistence";
        }

        @Override
        public String getLabel(@Nullable Locale locale) {
            return getId();
        }

        @Override
        public void store(Item item) {
            store(item, null);
        }

        @Override
        public void store(Item item, @Nullable String alias) {
            throw new UnsupportedOperationException("This backend expects timestamped samples");
        }

        @Override
        public void store(Item item, ZonedDateTime date, State state) {
            store(item, date, state, null);
        }

        @Override
        public void store(Item item, ZonedDateTime date, State state, @Nullable String alias) {
            if (ITEM.equals(item.getName()) && ALIAS.equals(alias)) {
                samples.put(date.toInstant(), state);
            }
        }

        @Override
        public boolean remove(FilterCriteria filter) {
            return remove(filter, null);
        }

        @Override
        public boolean remove(FilterCriteria filter, @Nullable String alias) {
            if (!ITEM.equals(filter.getItemName()) || !ALIAS.equals(alias)) {
                return false;
            }
            Instant begin = requireNonNull(filter.getBeginDate()).toInstant();
            Instant end = requireNonNull(filter.getEndDate()).toInstant();
            return samples.navigableKeySet()
                    .removeIf(timestamp -> !timestamp.isBefore(begin) && !timestamp.isAfter(end));
        }

        @Override
        public Iterable<HistoricItem> query(FilterCriteria filter) {
            return List.of();
        }
    }
}
