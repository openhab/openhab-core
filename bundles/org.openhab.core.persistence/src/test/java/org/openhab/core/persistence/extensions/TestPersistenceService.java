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
import org.openhab.core.persistence.extensions.PersistenceExtensions.RiemannType;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.openhab.core.types.State;

/**
 * A simple persistence service used for unit tests
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Mark Herwege - Allow future values
 * @author Mark Herwege - Adapt test expected value logic for Riemann sums
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

    static final int BASE_VALUE = ZonedDateTime.now().getYear(); // For reference, if year is 2025
    static final int BEFORE_START = BASE_VALUE - 85; // 1940
    static final int HISTORIC_START = BASE_VALUE - 75; // 1950
    static final int HISTORIC_INTERMEDIATE_VALUE_1 = BASE_VALUE - 20; // 2005
    static final int HISTORIC_INTERMEDIATE_VALUE_2 = BASE_VALUE - 14; // 2011
    static final int HISTORIC_END = BASE_VALUE - 13; // 2012
    static final int HISTORIC_INTERMEDIATE_NOVALUE_3 = BASE_VALUE - 6; // 2019
    static final int HISTORIC_INTERMEDIATE_NOVALUE_4 = BASE_VALUE - 4; // 2021
    static final int FUTURE_INTERMEDIATE_NOVALUE_1 = BASE_VALUE + 21; // 2051
    static final int FUTURE_INTERMEDIATE_NOVALUE_2 = BASE_VALUE + 31; // 2056
    static final int FUTURE_START = BASE_VALUE + 35; // 2060
    static final int FUTURE_INTERMEDIATE_VALUE_3 = BASE_VALUE + 45; // 2070
    static final int FUTURE_INTERMEDIATE_VALUE_4 = BASE_VALUE + 52; // 2077
    static final int FUTURE_END = BASE_VALUE + 75; // 2100
    static final int AFTER_END = BASE_VALUE + 85; // 2110
    static final DecimalType STATE = new DecimalType(HISTORIC_END);

    static final double KELVIN_OFFSET = 273.15;

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
        return value(year, false);
    }

    private static DecimalType value(long year, boolean kelvinOffset) {
        if (year < HISTORIC_START) {
            return DecimalType.ZERO;
        } else if (year <= HISTORIC_END) {
            return new DecimalType(year + (kelvinOffset ? KELVIN_OFFSET : 0));
        } else if (year < FUTURE_START) {
            return new DecimalType(HISTORIC_END + (kelvinOffset ? KELVIN_OFFSET : 0));
        } else if (year <= FUTURE_END) {
            return new DecimalType(year + (kelvinOffset ? KELVIN_OFFSET : 0));
        } else {
            return new DecimalType(FUTURE_END + (kelvinOffset ? KELVIN_OFFSET : 0));
        }
    }

    static double testRiemannSum(@Nullable Integer beginYear, @Nullable Integer endYear, RiemannType type) {
        return testRiemannSum(beginYear, endYear, type, false);
    }

    static double testRiemannSumCelsius(@Nullable Integer beginYear, @Nullable Integer endYear, RiemannType type) {
        return testRiemannSum(beginYear, endYear, type, true);
    }

    private static double testRiemannSum(@Nullable Integer beginYear, @Nullable Integer endYear, RiemannType type,
            boolean kelvinOffset) {
        ZonedDateTime now = ZonedDateTime.now();
        int begin = beginYear != null ? (beginYear < HISTORIC_START ? HISTORIC_START : beginYear) : now.getYear() + 1;
        int end = endYear != null ? endYear : now.getYear();
        double sum = 0;
        int index = begin;
        long duration = 0;
        long nextDuration = 0;
        switch (type) {
            case LEFT:
                if (beginYear == null) {
                    duration = Duration
                            .between(now, ZonedDateTime.of(now.getYear() + 1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
                            .toSeconds();
                }
                while (index < end) {
                    int bucketStart = index;
                    double value = value(index, kelvinOffset).doubleValue();
                    while ((index < end - 1) && (value(index).longValue() == value(index + 1).longValue())) {
                        index++;
                    }
                    index++;
                    duration += Duration
                            .between(ZonedDateTime.of(bucketStart, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                                    ZonedDateTime.of(index, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
                            .toSeconds();
                    if (endYear == null && index == end) {
                        duration += Duration
                                .between(ZonedDateTime.of(now.getYear(), 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), now)
                                .toSeconds();
                    }
                    sum += value * duration;
                    duration = 0;
                }
                break;
            case RIGHT:
                if (beginYear == null) {
                    duration = Duration
                            .between(now, ZonedDateTime.of(now.getYear() + 1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
                            .toSeconds();
                }
                while (index < end) {
                    int bucketStart = index;
                    while ((index < end - 1) && (value(index).longValue() == value(index + 1).longValue())) {
                        index++;
                    }
                    index++;
                    double value = value(index, kelvinOffset).doubleValue();
                    duration += Duration
                            .between(ZonedDateTime.of(bucketStart, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                                    ZonedDateTime.of(index, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
                            .toSeconds();
                    if (endYear == null && index == end) {
                        duration += Duration
                                .between(ZonedDateTime.of(now.getYear(), 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), now)
                                .toSeconds();
                    }
                    sum += value * duration;
                    duration = 0;
                }
                break;
            case TRAPEZOIDAL:
                if (beginYear == null) {
                    duration = Duration
                            .between(now, ZonedDateTime.of(now.getYear() + 1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
                            .toSeconds();
                }
                while (index < end) {
                    int bucketStart = index;
                    double value = value(index, kelvinOffset).doubleValue();
                    while ((index < end - 1) && (value(index).longValue() == value(index + 1).longValue())) {
                        index++;
                    }
                    index++;
                    value = (value + value(index, kelvinOffset).doubleValue()) / 2.0;
                    duration += Duration
                            .between(ZonedDateTime.of(bucketStart, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                                    ZonedDateTime.of(index, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
                            .toSeconds();
                    if (endYear == null && index == end) {
                        duration += Duration
                                .between(ZonedDateTime.of(now.getYear(), 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), now)
                                .toSeconds();
                    }
                    sum += value * duration;
                    duration = 0;
                }
                break;
            case MIDPOINT:
                int nextIndex = begin;
                boolean startBucket = true;
                double startValue = value(begin, kelvinOffset).doubleValue();
                if (beginYear == null) {
                    duration = Duration.between(now, ZonedDateTime.of(begin, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
                            .toSeconds();
                }
                while (index < end - 1 && nextIndex < end) {
                    int bucketStart = index;
                    while ((index < end - 1) && (value(index).longValue() == value(index + 1).longValue())) {
                        index++;
                    }
                    index++;
                    double value = value(index, kelvinOffset).doubleValue();
                    duration += Duration
                            .between(ZonedDateTime.of(bucketStart, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                                    ZonedDateTime.of(index, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
                            .toSeconds();
                    if (startBucket) {
                        sum += startValue * duration / 2.0;
                        startBucket = false;
                    }
                    bucketStart = index;
                    nextIndex = index;
                    while ((nextIndex < end - 1)
                            && (value(nextIndex).longValue() == value(nextIndex + 1).longValue())) {
                        nextIndex++;
                    }
                    nextIndex++;
                    nextDuration = Duration
                            .between(ZonedDateTime.of(bucketStart, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                                    ZonedDateTime.of(nextIndex, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
                            .toSeconds();
                    if (endYear == null && nextIndex == end) {
                        nextDuration += Duration
                                .between(ZonedDateTime.of(now.getYear(), 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), now)
                                .toSeconds();
                    }
                    sum += value * (duration + nextDuration) / 2.0;
                    duration = 0;
                }
                double endValue = value(end, kelvinOffset).doubleValue();
                long endDuration = nextDuration;
                sum += endValue * endDuration / 2.0;
                break;
        }
        return sum;
    }

    static double testAverage(@Nullable Integer beginYear, @Nullable Integer endYear) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime beginDate = beginYear != null
                ? ZonedDateTime.of(beginYear >= HISTORIC_START ? beginYear : HISTORIC_START, 1, 1, 0, 0, 0, 0,
                        ZoneId.systemDefault())
                : now;
        ZonedDateTime endDate = endYear != null ? ZonedDateTime.of(endYear, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())
                : now;
        double sum = testRiemannSum(beginYear, endYear, RiemannType.LEFT);
        long duration = Duration.between(beginDate, endDate).toSeconds();
        return 1.0 * sum / duration;
    }

    static double testMedian(@Nullable Integer beginYear, @Nullable Integer endYear) {
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

    public static final ZonedDateTime now() {
        ZonedDateTime now = ZonedDateTime.now();
        return now;
    }
}
