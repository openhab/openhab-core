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
package org.openhab.core.persistence.extensions;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.FilterCriteria.Ordering;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.ModifiablePersistenceService;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.openhab.core.types.State;

/**
 * A simple persistence service working with cached HistoricItems used for unit tests.
 *
 * @author Florian Binder - Initial contribution
 */
@NonNullByDefault
public class TestCachedValuesPersistenceService implements ModifiablePersistenceService {

    public static final String ID = "testCachedHistoricItems";

    private final List<HistoricItem> historicItems = new ArrayList<>();

    public TestCachedValuesPersistenceService() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void store(Item item) {
    }

    @Override
    public void store(Item item, @Nullable String alias) {
    }

    @Override
    public void store(Item item, ZonedDateTime date, State state) {
        historicItems.add(new CachedHistoricItem(date, state, item.getName()));
    }

    @Override
    public void store(Item item, ZonedDateTime date, State state, @Nullable String alias) {
    }

    @Override
    public boolean remove(FilterCriteria filter) throws IllegalArgumentException {
        return historicItems.removeAll(StreamSupport.stream(query(filter).spliterator(), false).toList());
    }

    @Override
    public Iterable<HistoricItem> query(FilterCriteria filter) {
        Stream<HistoricItem> stream = historicItems.stream();

        if (filter.getState() != null) {
            throw new UnsupportedOperationException("state filtering is not supported yet");
        }

        String itemName = filter.getItemName();
        if (itemName != null) {
            stream = stream.filter(hi -> itemName.equals(hi.getName()));
        }

        ZonedDateTime beginDate = filter.getBeginDate();
        if (beginDate != null) {
            stream = stream.filter(hi -> !beginDate.isAfter(hi.getTimestamp()));
        }

        ZonedDateTime endDate = filter.getEndDate();
        if (endDate != null) {
            stream = stream.filter(hi -> !endDate.isBefore(hi.getTimestamp()));
        }

        if (filter.getOrdering() == Ordering.ASCENDING) {
            stream = stream.sorted(((o1, o2) -> o1.getTimestamp().compareTo(o2.getTimestamp())));
        } else if (filter.getOrdering() == Ordering.DESCENDING) {
            stream = stream.sorted(((o1, o2) -> -o1.getTimestamp().compareTo(o2.getTimestamp())));
        }

        if (filter.getPageNumber() > 0) {
            stream = stream.skip(filter.getPageSize() * filter.getPageNumber());
        }

        if (filter.getPageSize() != Integer.MAX_VALUE) {
            stream = stream.limit(filter.getPageSize());
        }

        return stream.toList();
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return "Test Label";
    }

    @Override
    public List<PersistenceStrategy> getSuggestedStrategies() {
        return List.of();
    }

    private static class CachedHistoricItem implements HistoricItem {
        private final ZonedDateTime timestamp;
        private final State state;
        private final String name;

        public CachedHistoricItem(ZonedDateTime timestamp, State state, String name) {
            this.timestamp = timestamp;
            this.state = state;
            this.name = name;
        }

        @Override
        public ZonedDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public State getState() {
            return state;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "CachedHistoricItem [timestamp=" + timestamp + ", state=" + state + ", name=" + name + "]";
        }
    }
}
