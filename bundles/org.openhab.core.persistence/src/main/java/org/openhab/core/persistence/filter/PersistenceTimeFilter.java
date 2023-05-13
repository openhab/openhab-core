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
package org.openhab.core.persistence.filter;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;

/**
 * The {@link PersistenceTimeFilter} is a filter to prevent persistence base on intervals.
 *
 * The filter returns {@link false} if the time between now and the time of the last persisted value is less than
 * {@link #duration} {@link #unit}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class PersistenceTimeFilter extends PersistenceFilter {
    private final int value;
    private final String unit;

    private transient @Nullable Duration duration;
    private final transient Map<String, ZonedDateTime> nextPersistenceTimes = new HashMap<>();

    public PersistenceTimeFilter(String name, int value, String unit) {
        super(name);
        this.value = value;
        this.unit = unit;
    }

    public int getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public boolean apply(Item item) {
        String itemName = item.getName();

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime nextPersistenceTime = nextPersistenceTimes.get(itemName);

        return nextPersistenceTime == null || !now.isBefore(nextPersistenceTime);
    }

    @Override
    public void persisted(Item item) {
        Duration duration = this.duration;
        if (duration == null) {
            duration = switch (unit) {
                case "m" -> Duration.of(value, ChronoUnit.MINUTES);
                case "h" -> Duration.of(value, ChronoUnit.HOURS);
                case "d" -> Duration.of(value, ChronoUnit.DAYS);
                default -> Duration.of(value, ChronoUnit.SECONDS);
            };

            this.duration = duration;
        }
        nextPersistenceTimes.put(item.getName(), ZonedDateTime.now().plus(duration));
    }

    @Override
    public String toString() {
        return String.format("%s [name=%s, value=%s, unit=%s]", getClass().getSimpleName(), getName(), value, unit);
    }
}
