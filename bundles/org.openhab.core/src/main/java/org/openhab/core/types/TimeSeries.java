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
package org.openhab.core.types;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link TimeSeries} is used to transport a set of states together with their timestamp.
 * It can be used for persisting historic state or forecasts.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TimeSeries {
    private final TreeSet<Entry> states = new TreeSet<>(Comparator.comparing(e -> e.timestamp));
    private final Policy policy;

    public TimeSeries(Policy policy) {
        this.policy = policy;
    }

    /**
     * Get the persistence policy of this series.
     * <p/>
     * {@link Policy#ADD} add the content to the persistence, {@link Policy#REPLACE} first removes all persisted
     * elements in the timespan given by {@link #getBegin()} and {@link #getEnd()}.
     *
     * @return
     */
    public Policy getPolicy() {
        return policy;
    }

    /**
     * Get the timestamp of the first element in this series.
     *
     * @return the {@link Instant} of the first element
     */
    public Instant getBegin() {
        return states.isEmpty() ? Instant.MAX : states.first().timestamp();
    }

    /**
     * Get the timestamp of the last element in this series.
     *
     * @return the {@link Instant} of the last element
     */
    public Instant getEnd() {
        return states.isEmpty() ? Instant.MIN : states.last().timestamp();
    }

    /**
     * Get the number of elements in this series.
     *
     * @return the number of elements
     */
    public int size() {
        return states.size();
    }

    /**
     * Add a new element to this series.
     * <p/>
     * Elements can be added in an arbitrary order and are sorted chronologically.
     *
     * @param timestamp an {@link Instant} for the given state
     * @param state the {@link State} at the given timestamp
     */
    public void add(Instant timestamp, State state) {
        states.add(new Entry(timestamp, state));
    }

    /**
     * Get the content of this series.
     * <p/>
     * The entries are returned in chronological order, earlier entries before later entries.
     *
     * @return a {@link <Stream<Entry>} with the content of this series.
     */
    public Stream<Entry> getStates() {
        return List.copyOf(states).stream();
    }

    public record Entry(Instant timestamp, State state) {
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TimeSeries that = (TimeSeries) o;
        return Objects.equals(states, that.states) && policy == that.policy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(states, policy);
    }

    public enum Policy {
        ADD,
        REPLACE
    }
}
