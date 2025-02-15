/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.FilterCriteria.Ordering;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceItemInfo;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.openhab.core.types.State;

/**
 * A simple persistence service used for unit tests
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Mark Herwege - Allow future values
 */
@NonNullByDefault
public class TestPersistenceService implements QueryablePersistenceService {

    public static final String SERVICE_ID = "test";

    static final int SWITCH_START = -15;
    static final int SWITCH_ON_1 = -15;
    static final int SWITCH_ON_INTERMEDIATE_1 = -12;
    static final int SWITCH_OFF_1 = -10;
    static final int SWITCH_OFF_INTERMEDIATE_1 = -6;
    static final int SWITCH_ON_2 = -5;
    static final int SWITCH_ON_INTERMEDIATE_21 = -1;
    static final int SWITCH_ON_INTERMEDIATE_22 = +1;
    static final int SWITCH_OFF_2 = +5;
    static final int SWITCH_OFF_INTERMEDIATE_2 = +7;
    static final int SWITCH_ON_3 = +10;
    static final int SWITCH_ON_INTERMEDIATE_3 = +12;
    static final int SWITCH_OFF_3 = +15;
    static final int SWITCH_END = +15;
    static final OnOffType SWITCH_STATE = OnOffType.ON;

    static final int BEFORE_START = 1940;
    static final int HISTORIC_START = 1950;
    static final int HISTORIC_INTERMEDIATE_VALUE_1 = 2005;
    static final int HISTORIC_INTERMEDIATE_VALUE_2 = 2011;
    static final int HISTORIC_END = 2012;
    static final int HISTORIC_INTERMEDIATE_NOVALUE_3 = 2019;
    static final int HISTORIC_INTERMEDIATE_NOVALUE_4 = 2021;
    static final int FUTURE_INTERMEDIATE_NOVALUE_1 = 2051;
    static final int FUTURE_INTERMEDIATE_NOVALUE_2 = 2056;
    static final int FUTURE_START = 2060;
    static final int FUTURE_INTERMEDIATE_VALUE_3 = 2070;
    static final int FUTURE_INTERMEDIATE_VALUE_4 = 2077;
    static final int FUTURE_END = 2100;
    static final int AFTER_END = 2110;
    static final DecimalType STATE = new DecimalType(HISTORIC_END);

    private final ItemRegistry itemRegistry;

    public TestPersistenceService(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    @Override
    public String getId() {
        return SERVICE_ID;
    }

    @Override
    public void store(Item item) {
    }

    @Override
    public void store(Item item, @Nullable String alias) {
    }

    @Override
    public Iterable<HistoricItem> query(FilterCriteria filter) {
        if (PersistenceExtensionsTest.TEST_SWITCH.equals(filter.getItemName())) {
            ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES);
            ZonedDateTime nowMinusHours = now.plusHours(SWITCH_START);
            ZonedDateTime endDate = filter.getEndDate();
            endDate = endDate != null ? endDate : now;
            ZonedDateTime beginDate = filter.getBeginDate();
            beginDate = beginDate != null ? beginDate : endDate.isAfter(now) ? now : endDate.minusHours(1);

            List<HistoricItem> results = new ArrayList<>(31);
            for (int i = SWITCH_START; i <= SWITCH_END; i++) {
                final int hour = i;
                final ZonedDateTime theDate = nowMinusHours.plusHours(i - SWITCH_START);
                if (!theDate.isBefore(beginDate) && !theDate.isAfter(endDate)) {
                    results.add(new HistoricItem() {
                        @Override
                        public ZonedDateTime getTimestamp() {
                            return theDate;
                        }

                        @Override
                        public State getState() {
                            return OnOffType.from(hour < SWITCH_OFF_1 || (hour >= SWITCH_ON_2 && hour < SWITCH_OFF_2)
                                    || hour >= SWITCH_ON_3);
                        }

                        @Override
                        public String getName() {
                            return Objects.requireNonNull(filter.getItemName());
                        }
                    });
                }
            }
            if (filter.getOrdering() == Ordering.DESCENDING) {
                Collections.reverse(results);
            }
            Stream<HistoricItem> stream = results.stream();
            if (filter.getPageNumber() > 0) {
                stream = stream.skip(filter.getPageSize() * filter.getPageNumber());
            }

            if (filter.getPageSize() != Integer.MAX_VALUE) {
                stream = stream.limit(filter.getPageSize());
            }
            return stream.toList();
        } else {
            int startValue = HISTORIC_START;
            int endValue = FUTURE_END;

            ZonedDateTime beginDate = filter.getBeginDate();
            if (beginDate != null && beginDate.getYear() >= startValue) {
                startValue = beginDate.getYear();
            }
            ZonedDateTime endDate = filter.getEndDate();
            if (endDate != null && endDate.getYear() <= endValue) {
                endValue = endDate.getYear();
            }

            if (endValue <= startValue) {
                return List.of();
            }

            List<HistoricItem> results = new ArrayList<>(endValue - startValue);
            for (int i = startValue; i <= endValue; i++) {
                if (i > HISTORIC_END && i < FUTURE_START) {
                    continue;
                }
                final int year = i;
                results.add(new HistoricItem() {
                    @Override
                    public ZonedDateTime getTimestamp() {
                        return ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
                    }

                    @Override
                    public State getState() {
                        Item item = itemRegistry.get(Objects.requireNonNull(filter.getItemName()));
                        Item baseItem = item;
                        if (baseItem instanceof GroupItem groupItem) {
                            baseItem = groupItem.getBaseItem();
                        }
                        Unit<?> unit = baseItem instanceof NumberItem ni ? ni.getUnit() : null;
                        return unit == null ? new DecimalType(year) : QuantityType.valueOf(year, unit);
                    }

                    @Override
                    public String getName() {
                        return Objects.requireNonNull(filter.getItemName());
                    }
                });
            }
            if (filter.getOrdering() == Ordering.DESCENDING) {
                Collections.reverse(results);
            }
            Stream<HistoricItem> stream = results.stream();
            if (filter.getPageNumber() > 0) {
                stream = stream.skip(filter.getPageSize() * filter.getPageNumber());
            }

            if (filter.getPageSize() != Integer.MAX_VALUE) {
                stream = stream.limit(filter.getPageSize());
            }
            return stream.toList();
        }
    }

    @Override
    public Set<PersistenceItemInfo> getItemInfo() {
        return Set.of();
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return "Test Label";
    }

    @Override
    public List<PersistenceStrategy> getDefaultStrategies() {
        return List.of();
    }

    static OnOffType switchValue(int hour) {
        return (hour >= SWITCH_ON_1 && hour < SWITCH_OFF_1) || (hour >= SWITCH_ON_2 && hour < SWITCH_OFF_2)
                || (hour >= SWITCH_ON_3 && hour < SWITCH_OFF_3) ? OnOffType.ON : OnOffType.OFF;
    }

    static DecimalType value(long year) {
        if (year < HISTORIC_START) {
            return DecimalType.ZERO;
        } else if (year <= HISTORIC_END) {
            return new DecimalType(year);
        } else if (year < FUTURE_START) {
            return new DecimalType(HISTORIC_END);
        } else if (year <= FUTURE_END) {
            return new DecimalType(year);
        } else {
            return new DecimalType(FUTURE_END);
        }
    }

    static double average(@Nullable Integer beginYear, @Nullable Integer endYear) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime beginDate = beginYear != null
                ? ZonedDateTime.of(beginYear >= HISTORIC_START ? beginYear : HISTORIC_START, 1, 1, 0, 0, 0, 0,
                        ZoneId.systemDefault())
                : now;
        ZonedDateTime endDate = endYear != null ? ZonedDateTime.of(endYear, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())
                : now;
        int begin = beginYear != null ? beginYear : now.getYear() + 1;
        int end = endYear != null ? endYear : now.getYear();
        long sum = LongStream.range(begin, end).map(y -> value(y).longValue() * Duration
                .between(ZonedDateTime.of(Long.valueOf(y).intValue(), 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                        ZonedDateTime.of(Long.valueOf(y + 1).intValue(), 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
                .toMillis()).sum();
        sum += beginYear == null ? value(now.getYear()).longValue() * Duration
                .between(now, ZonedDateTime.of(now.getYear() + 1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())).toMillis()
                : 0;
        sum += endYear == null ? value(now.getYear()).longValue() * Duration
                .between(ZonedDateTime.of(now.getYear(), 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), now).toMillis() : 0;
        long duration = Duration.between(beginDate, endDate).toMillis();
        return 1.0 * sum / duration;
    }

    static double median(@Nullable Integer beginYear, @Nullable Integer endYear) {
        ZonedDateTime now = ZonedDateTime.now();
        int begin = beginYear != null ? beginYear : now.getYear() + 1;
        int end = endYear != null ? endYear : now.getYear();
        long[] values = LongStream.range(begin, end + 1)
                .filter(v -> ((v >= HISTORIC_START && v <= HISTORIC_END) || (v >= FUTURE_START && v <= FUTURE_END)))
                .sorted().toArray();
        int length = values.length;
        if (length % 2 == 1) {
            return values[values.length / 2];
        } else {
            return 0.5 * (values[values.length / 2] + values[values.length / 2 - 1]);
        }
    }
}
