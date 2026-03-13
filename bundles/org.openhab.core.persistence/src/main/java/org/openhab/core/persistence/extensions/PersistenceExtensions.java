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

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.FilterCriteria.Ordering;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.ModifiablePersistenceService;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.PersistenceServiceRegistry;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.persistence.registry.PersistenceServiceConfiguration;
import org.openhab.core.persistence.registry.PersistenceServiceConfigurationRegistry;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.TypeParser;
import org.openhab.core.util.Statistics;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.LoggerFactory;

/**
 * This class provides static methods that can be used in automation rules
 * for using persistence services
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Chris Jackson - Initial contribution
 * @author Gaël L'hopital - Add deltaSince, lastUpdate, evolutionRate
 * @author Jan N. Klug - Added sumSince
 * @author John Cocula - Added sumSince
 * @author Jan N. Klug - Added interval methods and refactoring
 * @author Mark Herwege - Changed return types to State for some interval methods to also return unit
 * @author Mark Herwege - Extended for future dates
 * @author Mark Herwege - lastChange and nextChange methods
 * @author Mark Herwege - handle persisted GroupItem with QuantityType
 * @author Mark Herwege - add median methods
 * @author Mark Herwege - use item lastChange and lastUpdate methods if not in peristence
 * @author Mark Herwege - add Riemann sum methods
 * @author Jörg Sautter - use Instant instead of ZonedDateTime in Riemann sum methods
 * @author Mark Herwege - use base unit for calculations and results
 */
@Component(immediate = true)
@NonNullByDefault
public class PersistenceExtensions {

    private static @Nullable PersistenceServiceRegistry registry;
    private static @Nullable PersistenceServiceConfigurationRegistry configRegistry;
    private static @Nullable TimeZoneProvider timeZoneProvider;

    public static enum RiemannType {
        LEFT,
        MIDPOINT,
        RIGHT,
        TRAPEZOIDAL
    }

    @Activate
    public PersistenceExtensions(@Reference PersistenceServiceRegistry registry,
            @Reference PersistenceServiceConfigurationRegistry configRegistry,
            @Reference TimeZoneProvider timeZoneProvider) {
        PersistenceExtensions.registry = registry;
        PersistenceExtensions.configRegistry = configRegistry;
        PersistenceExtensions.timeZoneProvider = timeZoneProvider;
    }

    /**
     * Persists the state of a given <code>item</code> through the default persistence service.
     *
     * @param item the item to store
     */
    public static void persist(Item item) {
        internalPersist(item, null);
    }

    /**
     * Persists the state of a given <code>item</code> through a {@link PersistenceService} identified
     * by the <code>serviceId</code>.
     *
     * @param item the item to store
     * @param serviceId the name of the {@link PersistenceService} to use
     */
    public static void persist(Item item, @Nullable String serviceId) {
        internalPersist(item, serviceId);
    }

    private static void internalPersist(Item item, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return;
        }
        PersistenceService service = getService(effectiveServiceId);
        if (service != null) {
            service.store(item, getAlias(item, effectiveServiceId));
            return;
        }
        LoggerFactory.getLogger(PersistenceExtensions.class)
                .warn("There is no persistence service registered with the id '{}'", effectiveServiceId);
    }

    /**
     * Persists a <code>state</code> at a given <code>timestamp</code> of an <code>item</code> through the default
     * persistence service.
     *
     * @param item the item to store
     * @param timestamp the date for the item state to be stored
     * @param state the state to be stored
     */
    public static void persist(Item item, ZonedDateTime timestamp, State state) {
        internalPersist(item, timestamp, state, null);
    }

    /**
     * Persists a <code>state</code> at a given <code>timestamp</code> of an <code>item</code> through a
     * {@link PersistenceService} identified by the <code>serviceId</code>.
     *
     * @param item the item
     * @param timestamp the date for the item state to be stored
     * @param state the state to be stored
     * @param serviceId the name of the {@link PersistenceService} to use
     */
    public static void persist(Item item, ZonedDateTime timestamp, State state, @Nullable String serviceId) {
        internalPersist(item, timestamp, state, serviceId);
    }

    private static void internalPersist(Item item, ZonedDateTime timestamp, State state, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return;
        }
        PersistenceService service = getService(effectiveServiceId);
        if (service instanceof ModifiablePersistenceService modifiableService) {
            modifiableService.store(item, timestamp, state, getAlias(item, effectiveServiceId));
            return;
        }
        LoggerFactory.getLogger(PersistenceExtensions.class)
                .warn("There is no modifiable persistence service registered with the id '{}'", effectiveServiceId);
    }

    /**
     * Persists a <code>state</code> at a given <code>timestamp</code> of an <code>item</code> through the default
     * persistence service.
     *
     * @param item the item to store
     * @param timestamp the date for the item state to be stored
     * @param stateString the state to be stored
     */
    public static void persist(Item item, ZonedDateTime timestamp, String stateString) {
        internalPersist(item, timestamp, stateString, null);
    }

    /**
     * Persists a <code>state</code> at a given <code>timestamp</code> of an <code>item</code> through a
     * {@link PersistenceService} identified by the <code>serviceId</code>.
     *
     * @param item the item
     * @param timestamp the date for the item state to be stored
     * @param stateString the state to be stored
     * @param serviceId the name of the {@link PersistenceService} to use
     */
    public static void persist(Item item, ZonedDateTime timestamp, String stateString, @Nullable String serviceId) {
        internalPersist(item, timestamp, stateString, serviceId);
    }

    private static void internalPersist(Item item, ZonedDateTime timestamp, String stateString,
            @Nullable String serviceId) {
        State state = TypeParser.parseState(item.getAcceptedDataTypes(), stateString);
        if (state != null) {
            internalPersist(item, timestamp, state, serviceId);
        } else {
            LoggerFactory.getLogger(PersistenceExtensions.class).warn("State '{}' cannot be parsed for item '{}'.",
                    stateString, item.getName());
        }
    }

    /**
     * Persists a <code>timeSeries</code> of an <code>item</code> through the default persistence service.
     *
     * @param item the item to store
     * @param timeSeries the timeSeries of states to be stored
     */
    public static void persist(Item item, TimeSeries timeSeries) {
        internalPersist(item, timeSeries, null);
    }

    /**
     * Persists a <code>timeSeries</code> of an <code>item</code> through a {@link PersistenceService} identified by the
     * <code>serviceId</code>.
     *
     * @param item the item
     * @param timeSeries the timeSeries of states to be stored
     * @param serviceId the name of the {@link PersistenceService} to use
     */
    public static void persist(Item item, TimeSeries timeSeries, @Nullable String serviceId) {
        internalPersist(item, timeSeries, serviceId);
    }

    private static void internalPersist(Item item, TimeSeries timeSeries, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null || timeSeries.size() == 0) {
            return;
        }
        PersistenceService service = getService(effectiveServiceId);
        TimeZoneProvider tzProvider = timeZoneProvider;
        ZoneId timeZone = tzProvider != null ? tzProvider.getTimeZone() : ZoneId.systemDefault();
        if (service instanceof ModifiablePersistenceService modifiableService) {
            if (timeSeries.getPolicy() == TimeSeries.Policy.REPLACE) {
                internalRemoveAllStatesBetween(item, timeSeries.getBegin().atZone(timeZone),
                        timeSeries.getEnd().atZone(timeZone), serviceId);
            }
            String alias = getAlias(item, effectiveServiceId);
            timeSeries.getStates()
                    .forEach(s -> modifiableService.store(item, s.timestamp().atZone(timeZone), s.state(), alias));
            return;
        }
        LoggerFactory.getLogger(PersistenceExtensions.class)
                .warn("There is no modifiable persistence service registered with the id '{}'", effectiveServiceId);
    }

    /**
     * Retrieves the historic item for a given <code>item</code> at a certain point in time through the default
     * persistence service.
     *
     * This method has been deprecated and {@link #persistedState(Item, ZonedDateTime)} should be used instead.
     *
     * @param item the item for which to retrieve the historic item
     * @param timestamp the point in time for which the historic item should be retrieved
     * @return the historic item at the given point in time, or <code>null</code> if no historic item could be found,
     *         the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    @Deprecated
    public static @Nullable HistoricItem historicState(Item item, ZonedDateTime timestamp) {
        LoggerFactory.getLogger(PersistenceExtensions.class).info(
                "The historicState method has been deprecated and will be removed in a future version, use persistedState instead.");
        return internalPersistedState(item, timestamp, null);
    }

    /**
     * Retrieves the historic item for a given <code>item</code> at a certain point in time through a
     * {@link PersistenceService} identified by the <code>serviceId</code>.
     *
     * This method has been deprecated and {@link #persistedState(Item, ZonedDateTime, String)} should be used instead.
     *
     * @param item the item for which to retrieve the historic item
     * @param timestamp the point in time for which the historic item should be retrieved
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the historic item at the given point in time, or <code>null</code> if no historic item could be found or
     *         if the provided <code>serviceId</code> does not refer to an available
     *         {@link QueryablePersistenceService}
     */
    @Deprecated
    public static @Nullable HistoricItem historicState(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        LoggerFactory.getLogger(PersistenceExtensions.class).info(
                "The historicState method has been deprecated and will be removed in a future version, use persistedState instead.");
        return internalPersistedState(item, timestamp, serviceId);
    }

    /**
     * Retrieves the persisted item for a given <code>item</code> at a certain point in time through the default
     * persistence service.
     *
     * @param item the item for which to retrieve the persisted item
     * @param timestamp the point in time for which the persisted item should be retrieved
     * @return the historic item at the given point in time, or <code>null</code> if no persisted item could be found,
     *         the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem persistedState(Item item, ZonedDateTime timestamp) {
        return internalPersistedState(item, timestamp, null);
    }

    /**
     * Retrieves the persisted item for a given <code>item</code> at a certain point in time through a
     * {@link PersistenceService} identified by the <code>serviceId</code>.
     *
     * @param item the item for which to retrieve the persisted item
     * @param timestamp the point in time for which the persisted item should be retrieved
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the persisted item at the given point in time, or <code>null</code> if no persisted item could be found
     *         or if the provided <code>serviceId</code> does not refer to an available
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem persistedState(Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return internalPersistedState(item, timestamp, serviceId);
    }

    private static @Nullable HistoricItem internalPersistedState(Item item, @Nullable ZonedDateTime timestamp,
            @Nullable String serviceId) {
        if (timestamp == null) {
            return null;
        }
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        PersistenceService service = getService(effectiveServiceId);
        if (service instanceof QueryablePersistenceService qService) {
            FilterCriteria filter = new FilterCriteria();
            filter.setEndDate(timestamp);
            String alias = getAlias(item, effectiveServiceId);
            filter.setItemName(item.getName());
            filter.setPageSize(1);
            filter.setOrdering(Ordering.DESCENDING);
            Iterable<HistoricItem> result = qService.query(filter, alias);
            if (result.iterator().hasNext()) {
                return result.iterator().next();
            }
        } else {
            LoggerFactory.getLogger(PersistenceExtensions.class)
                    .warn("There is no queryable persistence service registered with the id '{}'", effectiveServiceId);
        }
        return null;
    }

    /**
     * Query the last historic update time of a given <code>item</code>. The default persistence service is used.
     * Note the {@link Item#getLastStateUpdate()} is generally preferred to get the last update time of an item.
     *
     * @param item the item for which the last historic update time is to be returned
     * @return point in time of the last historic update to <code>item</code>, <code>null</code> if there are no
     *         historic persisted updates, the state has changed since the last update or the default persistence
     *         service is not available or not a {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime lastUpdate(Item item) {
        return internalAdjacentUpdate(item, false, null);
    }

    /**
     * Query for the last historic update time of a given <code>item</code>.
     * Note the {@link Item#getLastStateUpdate()} is generally preferred to get the last update time of an item.
     *
     * @param item the item for which the last historic update time is to be returned
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return point in time of the last historic update to <code>item</code>, <code>null</code> if there are no
     *         historic persisted updates, the state has changed since the last update or if persistence service given
     *         by <code>serviceId</code> does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime lastUpdate(Item item, @Nullable String serviceId) {
        return internalAdjacentUpdate(item, false, serviceId);
    }

    /**
     * Query the first future update time of a given <code>item</code>. The default persistence service is used.
     *
     * @param item the item for which the first future update time is to be returned
     * @return point in time of the first future update to <code>item</code>, or <code>null</code> if there are no
     *         future persisted updates or the default persistence service is not available or not a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime nextUpdate(Item item) {
        return internalAdjacentUpdate(item, true, null);
    }

    /**
     * Query for the first future update time of a given <code>item</code>.
     *
     * @param item the item for which the first future update time is to be returned
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return point in time of the first future update to <code>item</code>, or <code>null</code> if there are no
     *         future persisted updates or if persistence service given by <code>serviceId</code> does not refer to an
     *         available {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime nextUpdate(Item item, @Nullable String serviceId) {
        return internalAdjacentUpdate(item, true, serviceId);
    }

    private static @Nullable ZonedDateTime internalAdjacentUpdate(Item item, boolean forward,
            @Nullable String serviceId) {
        return internalAdjacent(item, forward, false, serviceId);
    }

    /**
     * Query the last historic change time of a given <code>item</code>. The default persistence service is used.
     * Note the {@link Item#getLastStateChange()} is generally preferred to get the last state change time of an item.
     *
     * @param item the item for which the last historic change time is to be returned
     * @return point in time of the last historic change to <code>item</code>, <code>null</code> if there are no
     *         historic persisted changes, the state has changed since the last update or the default persistence
     *         service is not available or not a {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime lastChange(Item item) {
        return internalAdjacentChange(item, false, null);
    }

    /**
     * Query for the last historic change time of a given <code>item</code>.
     * Note the {@link Item#getLastStateChange()} is generally preferred to get the last state change time of an item.
     *
     * @param item the item for which the last historic change time is to be returned
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return point in time of the last historic change to <code>item</code> <code>null</code> if there are no
     *         historic persisted changes, the state has changed since the last update or if persistence service given
     *         by <code>serviceId</code> does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime lastChange(Item item, @Nullable String serviceId) {
        return internalAdjacentChange(item, false, serviceId);
    }

    /**
     * Query the first future change time of a given <code>item</code>. The default persistence service is used.
     *
     * @param item the item for which the first future change time is to be returned
     * @return point in time of the first future change to <code>item</code>, or <code>null</code> if there are no
     *         future persisted changes or the default persistence service is not available or not a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime nextChange(Item item) {
        return internalAdjacentChange(item, true, null);
    }

    /**
     * Query for the first future change time of a given <code>item</code>.
     *
     * @param item the item for which the first future change time is to be returned
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return point in time of the first future change to <code>item</code>, or <code>null</code> if there are no
     *         future persisted changes or if persistence service given by <code>serviceId</code> does not refer to an
     *         available {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime nextChange(Item item, @Nullable String serviceId) {
        return internalAdjacentChange(item, true, serviceId);
    }

    private static @Nullable ZonedDateTime internalAdjacentChange(Item item, boolean forward,
            @Nullable String serviceId) {
        return internalAdjacent(item, forward, true, serviceId);
    }

    private static @Nullable ZonedDateTime internalAdjacent(Item item, boolean forward, boolean skipEqual,
            @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        PersistenceService service = getService(effectiveServiceId);
        if (service instanceof QueryablePersistenceService qService) {
            FilterCriteria filter = new FilterCriteria();
            String alias = getAlias(item, effectiveServiceId);
            filter.setItemName(item.getName());
            if (forward) {
                filter.setBeginDate(ZonedDateTime.now());
            } else {
                filter.setEndDate(ZonedDateTime.now());
            }
            filter.setOrdering(forward ? Ordering.ASCENDING : Ordering.DESCENDING);

            filter.setPageSize(skipEqual ? 1000 : 1);
            int startPage = 0;
            filter.setPageNumber(startPage);

            Iterator<HistoricItem> itemIterator = qService.query(filter, alias).iterator();
            if (!itemIterator.hasNext()) {
                return null;
            }
            int itemCount = 0;
            State state = item.getState();
            if (!skipEqual) {
                HistoricItem historicItem = itemIterator.next();
                if (!forward && !historicItem.getState().equals(state)) {
                    // Last persisted state value different from current state value, so it must have updated
                    // since last persist. We do not know when from persistence, so get it from the item.
                    return item.getLastStateUpdate();
                }
                return historicItem.getTimestamp();
            } else {
                HistoricItem historicItem = itemIterator.next();
                if (!historicItem.getState().equals(state)) {
                    // Persisted state value different from current state value, so it must have changed, but we
                    // do not know when looking backward in persistence. Get it from the item.
                    return forward ? historicItem.getTimestamp() : item.getLastStateChange();
                }
                while (historicItem.getState().equals(state) && itemIterator.hasNext()) {
                    HistoricItem nextHistoricItem = itemIterator.next();
                    itemCount++;
                    if (!nextHistoricItem.getState().equals(state)) {
                        return forward ? nextHistoricItem.getTimestamp() : historicItem.getTimestamp();
                    }
                    historicItem = nextHistoricItem;
                    if (itemCount == filter.getPageSize()) {
                        itemCount = 0;
                        filter.setPageNumber(++startPage);
                        itemIterator = qService.query(filter, alias).iterator();
                    }
                }
            }
        } else {
            LoggerFactory.getLogger(PersistenceExtensions.class)
                    .warn("There is no queryable persistence service registered with the id '{}'", effectiveServiceId);
        }
        return null;
    }

    /**
     * Returns the previous state of a given <code>item</code>.
     * Note the {@link Item#getLastState()} is generally preferred to get the previous state of an item.
     *
     * @param item the item to get the previous state value for
     * @return the previous state or <code>null</code> if no previous state could be found, or if the default
     *         persistence service is not configured or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem previousState(Item item) {
        return internalAdjacentState(item, false, false, null);
    }

    /**
     * Returns the previous state of a given <code>item</code>.
     * Note the {@link Item#getLastState()} is generally preferred to get the previous state of an item.
     *
     * @param item the item to get the previous state value for
     * @param skipEqual if true, skips equal state values and searches the first state not equal the current state
     * @return the previous state or <code>null</code> if no previous state could be found, or if the default
     *         persistence service is not configured or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem previousState(Item item, boolean skipEqual) {
        return internalAdjacentState(item, skipEqual, false, null);
    }

    /**
     * Returns the previous state of a given <code>item</code>.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     * Note the {@link Item#getLastState()} is generally preferred to get the previous state of an item.
     *
     * @param item the item to get the previous state value for
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the previous state or <code>null</code> if no previous state could be found, or if the default
     *         persistence service is not configured or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem previousState(Item item, @Nullable String serviceId) {
        return internalAdjacentState(item, false, false, serviceId);
    }

    /**
     * Returns the previous state of a given <code>item</code>.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     * Note the {@link Item#getLastState()} is generally preferred to get the previous state of an item.
     *
     * @param item the item to get the previous state value for
     * @param skipEqual if <code>true</code>, skips equal state values and searches the first state not equal the
     *            current state
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the previous state or <code>null</code> if no previous state could be found, or if the given
     *         <code>serviceId</code> is not available or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem previousState(Item item, boolean skipEqual, @Nullable String serviceId) {
        return internalAdjacentState(item, skipEqual, false, serviceId);
    }

    /**
     * Returns the next state of a given <code>item</code>.
     *
     * @param item the item to get the next state value for
     * @return the next state or <code>null</code> if no next state could be found, or if the default
     *         persistence service is not configured or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem nextState(Item item) {
        return internalAdjacentState(item, false, true, null);
    }

    /**
     * Returns the next state of a given <code>item</code>.
     *
     * @param item the item to get the next state value for
     * @param skipEqual if true, skips equal state values and searches the first state not equal the current state
     * @return the next state or <code>null</code> if no next state could be found, or if the default
     *         persistence service is not configured or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem nextState(Item item, boolean skipEqual) {
        return internalAdjacentState(item, skipEqual, true, null);
    }

    /**
     * Returns the next state of a given <code>item</code>.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the next state value for
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the next state or <code>null</code> if no next state could be found, or if the default
     *         persistence service is not configured or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem nextState(Item item, @Nullable String serviceId) {
        return internalAdjacentState(item, false, true, serviceId);
    }

    /**
     * Returns the next state of a given <code>item</code>.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the next state value for
     * @param skipEqual if <code>true</code>, skips equal state values and searches the first state not equal the
     *            current state
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the next state or <code>null</code> if no next state could be found, or if the given
     *         <code>serviceId</code> is not available or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem nextState(Item item, boolean skipEqual, @Nullable String serviceId) {
        return internalAdjacentState(item, skipEqual, true, serviceId);
    }

    private static @Nullable HistoricItem internalAdjacentState(Item item, boolean skipEqual, boolean forward,
            @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        PersistenceService service = getService(effectiveServiceId);
        if (service instanceof QueryablePersistenceService qService) {
            FilterCriteria filter = new FilterCriteria();
            String alias = getAlias(item, effectiveServiceId);
            filter.setItemName(item.getName());
            if (forward) {
                filter.setBeginDate(ZonedDateTime.now());
            } else {
                filter.setEndDate(ZonedDateTime.now());
            }
            filter.setOrdering(forward ? Ordering.ASCENDING : Ordering.DESCENDING);

            filter.setPageSize(skipEqual ? 1000 : 1);
            int startPage = 0;
            filter.setPageNumber(startPage);

            Iterator<HistoricItem> itemIterator = qService.query(filter, alias).iterator();
            int itemCount = 0;
            while (itemIterator.hasNext()) {
                HistoricItem historicItem = itemIterator.next();
                itemCount++;
                if (!skipEqual || !historicItem.getState().equals(item.getState())) {
                    return historicItem;
                }
                if (itemCount == filter.getPageSize()) {
                    itemCount = 0;
                    filter.setPageNumber(++startPage);
                    itemIterator = qService.query(filter, alias).iterator();
                }
            }
        } else {
            LoggerFactory.getLogger(PersistenceExtensions.class)
                    .warn("There is no queryable persistence service registered with the id '{}'", effectiveServiceId);
        }
        return null;
    }

    /**
     * Checks if the state of a given <code>item</code> has changed since a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item to check for state changes
     * @param timestamp the point in time to start the check
     * @return <code>true</code> if item state has changed, <code>false</code> if it has not changed, <code>null</code>
     *         if <code>timestamp</code> is in the future, if the default persistence service is not available or does
     *         not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean changedSince(Item item, ZonedDateTime timestamp) {
        return internalChangedBetween(item, timestamp, null, null);
    }

    /**
     * Checks if the state of a given <code>item</code> will change by a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item to check for state changes
     * @param timestamp the point in time to end the check
     * @return <code>true</code> if item state will change, <code>false</code> if it will not change, <code>null</code>
     *         if <code>timestamp></code> is in the past, if the default persistence service is not available or does
     *         not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean changedUntil(Item item, ZonedDateTime timestamp) {
        return internalChangedBetween(item, null, timestamp, null);
    }

    /**
     * Checks if the state of a given <code>item</code> changes between two points in time.
     * The default persistence service is used.
     *
     * @param item the item to check for state changes
     * @return <code>true</code> if item state changes, <code>false</code> if the item does not change in
     *         the given interval, <code>null</code> if <code>begin</code> is after <code>end</code>, if the default
     *         persistence does not refer to a {@link QueryablePersistenceService}, or <code>null</code> if the default
     *         persistence service is not available
     */
    public static @Nullable Boolean changedBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return internalChangedBetween(item, begin, end, null);
    }

    /**
     * Checks if the state of a given <code>item</code> has changed since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to check for state changes
     * @param timestamp the point in time to start the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return <code>true</code> if item state has changed, or <code>false</code> if it has not changed,
     *         <code>null</code> if <code>timestamp</code> is in the future, if the provided <code>serviceId</code> does
     *         not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean changedSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalChangedBetween(item, timestamp, null, serviceId);
    }

    /**
     * Checks if the state of a given <code>item</code> will change by a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to check for state changes
     * @param timestamp the point in time to end the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return <code>true</code> if item state will change, or <code>false</code> if it will not change,
     *         <code>null</code> if <code>timestamp</code> is in the past, if the provided <code>serviceId</code> does
     *         not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean changedUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalChangedBetween(item, null, timestamp, serviceId);
    }

    /**
     * Checks if the state of a given <code>item</code> changes between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to check for state changes
     * @param begin the point in time to start the check
     * @param end the point in time to stop the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return <code>true</code> if item state changed or <code>false</code> if the item does not change
     *         in the given interval, <code>null</code> if <code>begin</code> is after <code>end</code>, if the given
     *         <code>serviceId</code> does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean changedBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return internalChangedBetween(item, begin, end, serviceId);
    }

    private static @Nullable Boolean internalChangedBetween(Item item, @Nullable ZonedDateTime begin,
            @Nullable ZonedDateTime end, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        Iterable<HistoricItem> result = internalGetAllStatesBetween(item, begin, end, effectiveServiceId);
        if (result != null) {
            Iterator<HistoricItem> it = result.iterator();
            HistoricItem itemThen = internalPersistedState(item, begin, effectiveServiceId);
            if (itemThen == null) {
                // Can't get the state at the start time
                // If we've got results more recent than this, it must have changed
                return it.hasNext();
            }

            State state = itemThen.getState();
            while (it.hasNext()) {
                HistoricItem hItem = it.next();
                if (!hItem.getState().equals(state)) {
                    return true;
                }
                state = hItem.getState();
            }
            return false;
        }
        return null;
    }

    /**
     * Checks if the state of a given <code>item</code> has been updated since a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item to check for state updates
     * @param timestamp the point in time to start the check
     * @return <code>true</code> if item state was updated, <code>false</code> if the item has not been updated since
     *         <code>timestamp</code>, <code>null</code> if <code>timestamp</code> is in the future, if the default
     *         persistence does not refer to a {@link QueryablePersistenceService}, or <code>null</code> if the default
     *         persistence service is not available
     */
    public static @Nullable Boolean updatedSince(Item item, ZonedDateTime timestamp) {
        return internalUpdatedBetween(item, timestamp, null, null);
    }

    /**
     * Checks if the state of a given <code>item</code> will be updated until a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item to check for state updates
     * @param timestamp the point in time to end the check
     * @return <code>true</code> if item state is updated, <code>false</code> if the item is not updated until
     *         <code>timestamp</code>, <code>null</code> if <code>timestamp</code> is in the past, if the default
     *         persistence does not refer to a {@link QueryablePersistenceService}, or <code>null</code> if the default
     *         persistence service is not available
     */
    public static @Nullable Boolean updatedUntil(Item item, ZonedDateTime timestamp) {
        return internalUpdatedBetween(item, null, timestamp, null);
    }

    /**
     * Checks if the state of a given <code>item</code> has been updated between two points in time.
     * The default persistence service is used.
     *
     * @param item the item to check for state updates
     * @param begin the point in time to start the check
     * @param end the point in time to stop the check
     * @return <code>true</code> if item state was updated, <code>false</code> if the item has not been updated in
     *         the given interval, <code>null</code> if <code>begin</code> is after <code>end</code>, if the default
     *         persistence does not refer to a {@link QueryablePersistenceService}, or <code>null</code> if the default
     *         persistence service is not available
     */
    public static @Nullable Boolean updatedBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return internalUpdatedBetween(item, begin, end, null);
    }

    /**
     * Checks if the state of a given <code>item</code> has been updated since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to check for state changes
     * @param timestamp the point in time to start the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return <code>true</code> if item state was updated or <code>false</code> if the item has not been updated
     *         since <code>timestamp</code>, <code>null</code> if <code>timestamp</code> is in the future, if the given
     *         <code>serviceId</code> does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean updatedSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalUpdatedBetween(item, timestamp, null, serviceId);
    }

    /**
     * Checks if the state of a given <code>item</code> will be updated until a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to check for state changes
     * @param timestamp the point in time to end the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return <code>true</code> if item state was updated or <code>false</code> if the item is not updated
     *         since <code>timestamp</code>, <code>null</code> if <code>timestamp</code> is in the past, if the given
     *         <code>serviceId</code> does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean updatedUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalUpdatedBetween(item, null, timestamp, serviceId);
    }

    /**
     * Checks if the state of a given <code>item</code> is updated between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to check for state changes
     * @param begin the point in time to start the check
     * @param end the point in time to stop the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return <code>true</code> if item state was updated or <code>false</code> if the item is not updated
     *         in the given interval, <code>null</code> if <code>begin</code> is after <code>end</code>, if the given
     *         <code>serviceId</code> does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean updatedBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return internalUpdatedBetween(item, begin, end, serviceId);
    }

    private static @Nullable Boolean internalUpdatedBetween(Item item, @Nullable ZonedDateTime begin,
            @Nullable ZonedDateTime end, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        Iterable<HistoricItem> result = internalGetAllStatesBetween(item, begin, end, effectiveServiceId);
        if (result != null) {
            return result.iterator().hasNext();
        }
        return null;
    }

    /**
     * Gets the historic item with the maximum value of the state of a given <code>item</code> since
     * a certain point in time. The default persistence service is used.
     *
     * @param item the item to get the maximum state value for
     * @param timestamp the point in time to start the check
     * @return a historic item with the maximum state value since the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         maximum value, <code>null</code> if <code>timestamp</code> is in the future or if the default
     *         persistence service does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem maximumSince(Item item, ZonedDateTime timestamp) {
        return internalMaximumBetween(item, timestamp, null, null);
    }

    /**
     * Gets the historic item with the maximum value of the state of a given <code>item</code> until
     * a certain point in time. The default persistence service is used.
     *
     * @param item the item to get the maximum state value for
     * @param timestamp the point in time to end the check
     * @return a historic item with the maximum state value until the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         maximum value, <code>null</code> if <code>timestamp</code> is in the past or if the default
     *         persistence service does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem maximumUntil(Item item, ZonedDateTime timestamp) {
        return internalMaximumBetween(item, null, timestamp, null);
    }

    /**
     * Gets the historic item with the maximum value of the state of a given <code>item</code> between two points in
     * time. The default persistence service is used.
     *
     * @param item the item to get the maximum state value for
     * @param begin the point in time to start the check
     * @param end the point in time to stop the check
     * @return a {@link HistoricItem} with the maximum state value between two points in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if no persisted states found, or
     *         <code>null</code> if <code>begin</code> is after <code>end</end> or if the default persistence service
     *         does not refer to an available{@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem maximumBetween(final Item item, ZonedDateTime begin, ZonedDateTime end) {
        return internalMaximumBetween(item, begin, end, null);
    }

    /**
     * Gets the historic item with the maximum value of the state of a given <code>item</code> since
     * a certain point in time. The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the maximum state value for
     * @param timestamp the point in time to start the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return a {@link HistoricItem} with the maximum state value since the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         maximum value, <code>null</code> if <code>timestamp</code> is in the future or if the given
     *         <code>serviceId</code> does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem maximumSince(final Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return internalMaximumBetween(item, timestamp, null, serviceId);
    }

    /**
     * Gets the historic item with the maximum value of the state of a given <code>item</code> until
     * a certain point in time. The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the maximum state value for
     * @param timestamp the point in time to end the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return a {@link HistoricItem} with the maximum state value until the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         maximum value, <code>null</code> if <code>timestamp</code> is in the past or if the given
     *         <code>serviceId</code> does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem maximumUntil(final Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return internalMaximumBetween(item, null, timestamp, serviceId);
    }

    /**
     * Gets the historic item with the maximum value of the state of a given <code>item</code> between two points in
     * time. The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the maximum state value for
     * @param begin the point in time to start the check
     * @param end the point in time to stop the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return a {@link HistoricItem} with the maximum state value between two points in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if no persisted states found, or
     *         <code>null</code> if <code>begin</code> is after <code>end</end> or if the given <code>serviceId</code>
     *         does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem maximumBetween(final Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return internalMaximumBetween(item, begin, end, serviceId);
    }

    private static @Nullable HistoricItem internalMaximumBetween(final Item item, @Nullable ZonedDateTime begin,
            @Nullable ZonedDateTime end, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        Iterable<HistoricItem> result = getAllStatesBetweenWithBoundaries(item, begin, end, effectiveServiceId);
        if (result == null) {
            return null;
        }
        Iterator<HistoricItem> it = result.iterator();
        HistoricItem maximumHistoricItem = null;

        Item baseItem = item instanceof GroupItem groupItem ? groupItem.getBaseItem() : item;
        Unit<?> unit = (baseItem instanceof NumberItem numberItem) ? numberItem.getUnit() : null;

        DecimalType maximum = null;
        while (it.hasNext()) {
            HistoricItem historicItem = it.next();
            DecimalType value = getPersistedValue(historicItem, unit);
            if (value != null) {
                if (maximum == null || value.compareTo(maximum) > 0) {
                    maximum = value;
                    maximumHistoricItem = historicItem;
                }
            }
        }
        return historicItemOrCurrentState(item, maximumHistoricItem);
    }

    /**
     * Gets the historic item with the minimum value of the state of a given <code>item</code> since
     * a certain point in time. The default persistence service is used.
     *
     * @param item the item to get the minimum state value for
     * @param timestamp the point in time from which to search for the minimum state value
     * @return a historic item with the minimum state value since the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         minimum value, <code>null</code> if <code>timestamp</code> is in the future or if the default
     *         persistence service does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem minimumSince(Item item, ZonedDateTime timestamp) {
        return internalMinimumBetween(item, timestamp, null, null);
    }

    /**
     * Gets the historic item with the minimum value of the state of a given <code>item</code> until
     * a certain point in time. The default persistence service is used.
     *
     * @param item the item to get the minimum state value for
     * @param timestamp the point in time to which to search for the minimum state value
     * @return a historic item with the minimum state value until the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         minimum value, <code>null</code> if <code>timestamp</code> is in the past or if the default
     *         persistence service does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem minimumUntil(Item item, ZonedDateTime timestamp) {
        return internalMinimumBetween(item, null, timestamp, null);
    }

    /**
     * Gets the historic item with the minimum value of the state of a given <code>item</code> between
     * two certain points in time. The default persistence service is used.
     *
     * @param item the item to get the minimum state value for
     * @param begin the beginning point in time
     * @param end the ending point in time to
     * @return a {@link HistoricItem} with the minimum state value between two points in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if no persisted states found, or
     *         <code>null</code> if <code>begin</code> is after <code>end</end> or if the default persistence service
     *         does not refer to an available{@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem minimumBetween(final Item item, ZonedDateTime begin, ZonedDateTime end) {
        return internalMinimumBetween(item, begin, end, null);
    }

    /**
     * Gets the historic item with the minimum value of the state of a given <code>item</code> since
     * a certain point in time. The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the minimum state value for
     * @param timestamp the point in time from which to search for the minimum state value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return a {@link HistoricItem} with the minimum state value since the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         minimum value, <code>null</code> if <code>timestamp</code> is in the future or if the given
     *         <code>serviceId</code> does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem minimumSince(final Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return internalMinimumBetween(item, timestamp, null, serviceId);
    }

    /**
     * Gets the historic item with the minimum value of the state of a given <code>item</code> until
     * a certain point in time. The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the minimum state value for
     * @param timestamp the point in time to which to search for the minimum state value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return a {@link HistoricItem} with the minimum state value until the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         minimum value, <code>null</code> if <code>timestamp</code> is in the past or if the given
     *         <code>serviceId</code> does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem minimumUntil(final Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return internalMinimumBetween(item, null, timestamp, serviceId);
    }

    /**
     * Gets the historic item with the minimum value of the state of a given <code>item</code> between
     * two certain points in time. The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the minimum state value for
     * @param begin the beginning point in time
     * @param end the end point in time to
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return a {@link HistoricItem} with the minimum state value between two points in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if no persisted states found, or
     *         <code>null</code> if <code>begin</code> is after <code>end</end> or if the given <code>serviceId</code>
     *         does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem minimumBetween(final Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return internalMinimumBetween(item, begin, end, serviceId);
    }

    private static @Nullable HistoricItem internalMinimumBetween(final Item item, @Nullable ZonedDateTime begin,
            @Nullable ZonedDateTime end, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        Iterable<HistoricItem> result = getAllStatesBetweenWithBoundaries(item, begin, end, effectiveServiceId);
        if (result == null) {
            return null;
        }
        Iterator<HistoricItem> it = result.iterator();
        HistoricItem minimumHistoricItem = null;

        Item baseItem = item instanceof GroupItem groupItem ? groupItem.getBaseItem() : item;
        Unit<?> unit = (baseItem instanceof NumberItem numberItem) ? numberItem.getUnit() : null;

        DecimalType minimum = null;
        while (it.hasNext()) {
            HistoricItem historicItem = it.next();
            DecimalType value = getPersistedValue(historicItem, unit);
            if (value != null) {
                if (minimum == null || value.compareTo(minimum) < 0) {
                    minimum = value;
                    minimumHistoricItem = historicItem;
                }
            }
        }
        return historicItemOrCurrentState(item, minimumHistoricItem);
    }

    /**
     * Gets the variance of the state of the given {@link Item} since a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time from which to compute the variance
     * @return the variance between then and now, or <code>null</code> if <code>timestamp</code> is in the future, if
     *         there is no default persistence service available, or it is not a {@link QueryablePersistenceService}, or
     *         if there is no persisted state for the given <code>item</code> at the given <code>timestamp</code>
     */
    public static @Nullable State varianceSince(Item item, ZonedDateTime timestamp) {
        return internalVarianceBetween(item, timestamp, null, null, null);
    }

    /**
     * Gets the variance of the state of the given {@link Item} since a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time from which to compute the variance
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the variance between then and now, or <code>null</code> if <code>timestamp</code> is in the future, if
     *         there is no default persistence service available, or it is not a {@link QueryablePersistenceService}, or
     *         if there is no persisted state for the given <code>item</code> at the given <code>timestamp</code>
     */
    public static @Nullable State varianceSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return internalVarianceBetween(item, timestamp, null, type, null);
    }

    /**
     * Gets the variance of the state of the given {@link Item} until a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time to which to compute the variance
     * @return the variance between now and then, or <code>null</code> if <code>timestamp</code> is in the past, if
     *         there is no default persistence service available, or it is not a {@link QueryablePersistenceService}, or
     *         if there is no persisted state for the given <code>item</code> at the given <code>timestamp</code>
     */
    public static @Nullable State varianceUntil(Item item, ZonedDateTime timestamp) {
        return internalVarianceBetween(item, null, timestamp, null, null);
    }

    /**
     * Gets the variance of the state of the given {@link Item} until a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time to which to compute the variance
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the variance between now and then, or <code>null</code> if <code>timestamp</code> is in the past, if
     *         there is no default persistence service available, or it is not a {@link QueryablePersistenceService}, or
     *         if there is no persisted state for the given <code>item</code> at the given <code>timestamp</code>
     */
    public static @Nullable State varianceUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return internalVarianceBetween(item, null, timestamp, type, null);
    }

    /**
     * Gets the variance of the state of the given {@link Item} between two points in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the variance for
     * @param begin the point in time from which to compute
     * @param end the end time for the computation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the variance between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if the persistence service given by
     *         <code>serviceId</code> is not available, or it is not a {@link QueryablePersistenceService}, or it is not
     *         a {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State varianceBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return internalVarianceBetween(item, begin, end, null, serviceId);
    }

    /**
     * Gets the variance of the state of the given {@link Item} between two certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the variance for
     * @param begin the point in time from which to compute the variance
     * @param end the end time for the computation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the variance between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State varianceBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type) {
        return internalVarianceBetween(item, begin, end, type, null);
    }

    /**
     * Gets the variance of the state of the given {@link Item} since a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time from which to compute the variance
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the variance between then and now, or <code>null</code> if <code>timestamp</code> is in the future, if
     *         the persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State varianceSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalVarianceBetween(item, timestamp, null, null, serviceId);
    }

    /**
     * Gets the variance of the state of the given {@link Item} since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time from which to compute the variance
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the variance between then and now, or <code>null</code> if <code>timestamp</code> is in the future, if
     *         the persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State varianceSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return internalVarianceBetween(item, timestamp, null, type, serviceId);
    }

    /**
     * Gets the variance of the state of the given {@link Item} until a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time to which to compute the variance
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the variance between now and then, or <code>null</code> if <code>timestamp</code> is in the past, if the
     *         persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State varianceUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalVarianceBetween(item, null, timestamp, null, serviceId);
    }

    /**
     * Gets the variance of the state of the given {@link Item} until a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time to which to compute the variance
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the variance between now and then, or <code>null</code> if <code>timestamp</code> is in the past, if the
     *         persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State varianceUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return internalVarianceBetween(item, null, timestamp, type, serviceId);
    }

    /**
     * Gets the variance of the state of the given {@link Item} between two certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the variance for
     * @param begin the point in time from which to compute the variance
     * @param end the end time for the computation
     * @return the variance between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State varianceBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return internalVarianceBetween(item, begin, end, null, null);
    }

    /**
     * Gets the variance of the state of the given {@link Item} between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the variance for
     * @param begin the point in time from which to compute
     * @param end the end time for the computation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the variance between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if the persistence service given by
     *         <code>serviceId</code> is not available, or it is not a {@link QueryablePersistenceService}, or it is not
     *         a {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State varianceBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type, @Nullable String serviceId) {
        return internalVarianceBetween(item, begin, end, type, serviceId);
    }

    private static @Nullable State internalVarianceBetween(Item item, @Nullable ZonedDateTime begin,
            @Nullable ZonedDateTime end, @Nullable RiemannType type, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime beginTime = Objects.requireNonNullElse(begin, now);
        ZonedDateTime endTime = Objects.requireNonNullElse(end, now);

        Iterable<HistoricItem> result = getAllStatesBetweenWithBoundaries(item, begin, end, effectiveServiceId);
        if (result == null) {
            return null;
        }
        Iterator<HistoricItem> it = result.iterator();
        // Remove initial part of history that does not have any values persisted
        if (beginTime.isBefore(now)) {
            if (it.hasNext()) {
                beginTime = it.next().getTimestamp();
            }
            it = result.iterator();
        }
        Item baseItem = item instanceof GroupItem groupItem ? groupItem.getBaseItem() : item;
        Unit<?> unit = (baseItem instanceof NumberItem numberItem) ? numberItem.getUnit() : null;

        BigDecimal average = average(beginTime, endTime, it, unit, type);
        if (average != null) {
            int count = 0;
            BigDecimal sum = BigDecimal.ZERO;

            it = result.iterator();
            while (it.hasNext()) {
                HistoricItem historicItem = it.next();
                DecimalType value = getPersistedValue(historicItem, unit);
                if (value != null) {
                    count++;
                    sum = sum.add(value.toBigDecimal().subtract(average, MathContext.DECIMAL64).pow(2,
                            MathContext.DECIMAL64));
                }
            }

            // avoid division by zero
            if (count > 0) {
                BigDecimal variance = sum.divide(BigDecimal.valueOf(count), MathContext.DECIMAL64);
                if (unit != null) {
                    return new QuantityType<>(variance, unit.multiply(unit));
                }
                return new DecimalType(variance);
            }
        }
        return null;
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} since a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time from which to compute the standard deviation
     * @return the standard deviation between then and now, or <code>null</code> if <code>timestamp</code> is in the
     *         future, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationSince(Item item, ZonedDateTime timestamp) {
        return internalDeviationBetween(item, timestamp, null, null, null);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} since a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time from which to compute the standard deviation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the standard deviation between then and now, or <code>null</code> if <code>timestamp</code> is in the
     *         future, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return internalDeviationBetween(item, timestamp, null, type, null);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} until a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time to which to compute the standard deviation
     * @return the standard deviation between now and then, or <code>null</code> if <code>timestamp</code> is in the
     *         past, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationUntil(Item item, ZonedDateTime timestamp) {
        return internalDeviationBetween(item, timestamp, null, null, null);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} until a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time to which to compute the standard deviation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the standard deviation between now and then, or <code>null</code> if <code>timestamp</code> is in the
     *         past, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return internalDeviationBetween(item, timestamp, null, type, null);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} between two points in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param begin the point in time from which to compute
     * @param end the end time for the computation
     * @return the standard deviation between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State deviationBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return internalDeviationBetween(item, begin, end, null, null);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} between two points in time.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param begin the point in time from which to compute
     * @param end the end time for the computation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the standard deviation between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State deviationBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type) {
        return internalDeviationBetween(item, begin, end, type, null);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} since a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time from which to compute the standard deviation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the standard deviation between then and now, or <code>null</code> if <code>timestamp</code> is in the
     *         future, if the persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalDeviationBetween(item, timestamp, null, null, serviceId);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time from which to compute the standard deviation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the standard deviation between then and now, or <code>null</code> if <code>timestamp</code> is in the
     *         future, if the persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return internalDeviationBetween(item, timestamp, null, type, serviceId);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} until a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time to which to compute the standard deviation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the standard deviation between now and then, or <code>null</code> if <code>timestamp</code> is in the
     *         past, if the persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalDeviationBetween(item, null, timestamp, null, serviceId);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} until a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time to which to compute the standard deviation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the standard deviation between now and then, or <code>null</code> if <code>timestamp</code> is in the
     *         past, if the persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return internalDeviationBetween(item, null, timestamp, type, serviceId);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} between two points in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param begin the point in time from which to compute
     * @param end the end time for the computation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the standard deviation between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if the persistence service given by
     *         <code>serviceId</code> is not available, or it is not a {@link QueryablePersistenceService}, or it is not
     *         a {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State deviationBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return internalDeviationBetween(item, begin, end, null, serviceId);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param begin the point in time from which to compute
     * @param end the end time for the computation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the standard deviation between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if the persistence service given by
     *         <code>serviceId</code> is not available, or it is not a {@link QueryablePersistenceService}, or it is not
     *         a {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State deviationBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type, @Nullable String serviceId) {
        return internalDeviationBetween(item, begin, end, type, serviceId);
    }

    private static @Nullable State internalDeviationBetween(Item item, @Nullable ZonedDateTime begin,
            @Nullable ZonedDateTime end, @Nullable RiemannType type, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        State variance = internalVarianceBetween(item, begin, end, type, effectiveServiceId);
        if (variance == null) {
            return null;
        }

        Unit<?> varianceUnit = (variance instanceof QuantityType<?> quantity) ? quantity.getUnit() : null;
        DecimalType dt = variance.as(DecimalType.class);

        // avoid ArithmeticException if variance is less than zero
        if (dt != null && DecimalType.ZERO.compareTo(dt) <= 0) {
            BigDecimal deviation = dt.toBigDecimal().sqrt(MathContext.DECIMAL64);

            Item baseItem = item instanceof GroupItem groupItem ? groupItem.getBaseItem() : item;
            Unit<?> unit = baseItem instanceof NumberItem numberItem ? numberItem.getUnit() : null;
            if (varianceUnit != null && unit != null) {
                return (new QuantityType<>(deviation, varianceUnit.root(2))).toUnit(unit);
            } else {
                return new DecimalType(deviation);
            }
        }
        return null;
    }

    /**
     * Gets the average value of the state of a given {@link Item} since a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time from which to search for the average value
     * @return the average value since <code>timestamp</code> or <code>null</code> if no
     *         previous states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State averageSince(Item item, ZonedDateTime timestamp) {
        return internalAverageBetween(item, timestamp, null, null, null);
    }

    /**
     * Gets the average value of the state of a given {@link Item} since a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time from which to search for the average value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the average value since <code>timestamp</code> or <code>null</code> if no
     *         previous states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State averageSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return internalAverageBetween(item, timestamp, null, type, null);
    }

    /**
     * Gets the average value of the state of a given {@link Item} until a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time to which to search for the average value
     * @return the average value until <code>timestamp</code> or <code>null</code> if no
     *         future states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State averageUntil(Item item, ZonedDateTime timestamp) {
        return internalAverageBetween(item, null, timestamp, null, null);
    }

    /**
     * Gets the average value of the state of a given {@link Item} until a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time to which to search for the average value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the average value until <code>timestamp</code> or <code>null</code> if no
     *         future states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State averageUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return internalAverageBetween(item, null, timestamp, type, null);
    }

    /**
     * Gets the average value of the state of a given {@link Item} between two certain points in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @return the average value between <code>begin</code> and <code>end</code> or <code>null</code> if no
     *         states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}.
     */
    public static @Nullable State averageBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return internalAverageBetween(item, begin, end, null, null);
    }

    /**
     * Gets the average value of the state of a given {@link Item} between two certain points in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the average value between <code>begin</code> and <code>end</code> or <code>null</code> if no
     *         states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}.
     */
    public static @Nullable State averageBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type) {
        return internalAverageBetween(item, begin, end, type, null);
    }

    /**
     * Gets the average value of the state of a given {@link Item} since a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time from which to search for the average value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the average value since <code>timestamp</code>, or <code>null</code> if no
     *         previous states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State averageSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalAverageBetween(item, timestamp, null, null, serviceId);
    }

    /**
     * Gets the average value of the state of a given {@link Item} since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time from which to search for the average value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the average value since <code>timestamp</code>, or <code>null</code> if no
     *         previous states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State averageSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return internalAverageBetween(item, timestamp, null, type, serviceId);
    }

    /**
     * Gets the average value of the state of a given {@link Item} until a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time to which to search for the average value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the average value until <code>timestamp</code>, or <code>null</code> if no
     *         future states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State averageUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalAverageBetween(item, null, timestamp, null, serviceId);
    }

    /**
     * Gets the average value of the state of a given {@link Item} until a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time to which to search for the average value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the average value until <code>timestamp</code>, or <code>null</code> if no
     *         future states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State averageUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return internalAverageBetween(item, null, timestamp, type, serviceId);
    }

    /**
     * Gets the average value of the state of a given {@link Item} between two certain points in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the average value between <code>begin</code> and <code>end</code>, or <code>null</code> if no
     *         states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable State averageBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return internalAverageBetween(item, begin, end, null, serviceId);
    }

    /**
     * Gets the average value of the state of a given {@link Item} between two certain points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the average value between <code>begin</code> and <code>end</code>, or <code>null</code> if no
     *         states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable State averageBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type, @Nullable String serviceId) {
        return internalAverageBetween(item, begin, end, type, serviceId);
    }

    private static @Nullable State internalAverageBetween(Item item, @Nullable ZonedDateTime begin,
            @Nullable ZonedDateTime end, @Nullable RiemannType type, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime beginTime = Objects.requireNonNullElse(begin, now);
        ZonedDateTime endTime = Objects.requireNonNullElse(end, now);

        if (beginTime.isEqual(endTime)) {
            HistoricItem historicItem = internalPersistedState(item, beginTime, effectiveServiceId);
            return historicItem != null ? historicItem.getState() : null;
        }

        Iterable<HistoricItem> result = getAllStatesBetweenWithBoundaries(item, begin, end, effectiveServiceId);
        if (result == null) {
            return null;
        }
        Iterator<HistoricItem> it = result.iterator();
        // Remove initial part of history that does not have any values persisted
        if (beginTime.isBefore(now)) {
            if (it.hasNext()) {
                beginTime = it.next().getTimestamp();
            }
            it = result.iterator();
        }

        Item baseItem = item instanceof GroupItem groupItem ? groupItem.getBaseItem() : item;
        Unit<?> unit = baseItem instanceof NumberItem numberItem ? numberItem.getUnit() : null;

        BigDecimal average = average(beginTime, endTime, it, unit, type);
        if (average == null) {
            return null;
        }
        if (unit != null) {
            return new QuantityType<>(average, unit);
        }
        return new DecimalType(average);
    }

    private static @Nullable BigDecimal average(ZonedDateTime begin, ZonedDateTime end, Iterator<HistoricItem> it,
            @Nullable Unit<?> unit, @Nullable RiemannType type) {
        BigDecimal sum = riemannSum(begin.toInstant(), end.toInstant(), it, unit, type);
        BigDecimal totalDuration = BigDecimal.valueOf(Duration.between(begin, end).toMillis());
        if (totalDuration.signum() == 0) {
            return null;
        }
        return sum.divide(totalDuration, MathContext.DECIMAL64);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} since a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * A left approximation type is used for the Riemann sum.
     *
     * <b>Note:</b> The time dimension in the result is in seconds, therefore if you do not use QuantityType results,
     * you may have to
     * multiply or divide to get the result in the expected scale.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time from which to search for the riemannSum value
     * @return the Riemann sum since <code>timestamp</code> or <code>null</code> if no
     *         previous states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State riemannSumSince(Item item, ZonedDateTime timestamp) {
        return internalRiemannSumBetween(item, timestamp, null, null, null);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} since a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     *
     * <b>Note:</b> The time dimension in the result is in seconds, therefore if you do not use QuantityType results,
     * you may have to
     * multiply or divide to get the result in the expected scale.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time from which to search for the riemannSum value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the Riemann sum since <code>timestamp</code> or <code>null</code> if no
     *         previous states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State riemannSumSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return internalRiemannSumBetween(item, timestamp, null, type, null);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} until a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * A left approximation type is used for the Riemann sum.
     *
     * <b>Note:</b> The time dimension in the result is in seconds, therefore if you do not use QuantityType results,
     * you may have to
     * multiply or divide to get the result in the expected scale.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time to which to search for the riemannSum value
     * @return the Riemann sum until <code>timestamp</code> or <code>null</code> if no
     *         future states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State riemannSumUntil(Item item, ZonedDateTime timestamp) {
        return internalRiemannSumBetween(item, null, timestamp, null, null);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} until a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     *
     * <b>Note:</b> The time dimension in the result is in seconds, therefore if you do not use QuantityType results,
     * you may have to
     * multiply or divide to get the result in the expected scale.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time to which to search for the riemannSum value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the Riemann sum until <code>timestamp</code> or <code>null</code> if no
     *         future states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State riemannSumUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return internalRiemannSumBetween(item, null, timestamp, type, null);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} between two certain points in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * A left approximation type is used for the Riemann sum.
     *
     * <b>Note:</b> The time dimension in the result is in seconds, therefore if you do not use QuantityType results,
     * you may have to
     * multiply or divide to get the result in the expected scale.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @return the Riemann sum between <code>begin</code> and <code>end</code> or <code>null</code> if no
     *         states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}.
     */
    public static @Nullable State riemannSumBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return internalRiemannSumBetween(item, begin, end, null, null);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} between two certain points in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     *
     * <b>Note:</b> The time dimension in the result is in seconds, therefore if you do not use QuantityType results,
     * you may have to
     * multiply or divide to get the result in the expected scale.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the Riemann sum between <code>begin</code> and <code>end</code> or <code>null</code> if no
     *         states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}.
     */
    public static @Nullable State riemannSumBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type) {
        return internalRiemannSumBetween(item, begin, end, type, null);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} since a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * A left approximation type is used for the Riemann sum.
     *
     * <b>Note:</b> The time dimension in the result is in seconds, therefore if you do not use QuantityType results,
     * you may have to
     * multiply or divide to get the result in the expected scale.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time from which to search for the riemannSum value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the Riemann sum since <code>timestamp</code>, or <code>null</code> if no
     *         previous states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State riemannSumSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalRiemannSumBetween(item, timestamp, null, null, serviceId);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} since a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     *
     * <b>Note:</b> The time dimension in the result is in seconds, therefore if you do not use QuantityType results,
     * you may have to
     * multiply or divide to get the result in the expected scale.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time from which to search for the riemannSum value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the Riemann sum since <code>timestamp</code>, or <code>null</code> if no
     *         previous states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State riemannSumSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return internalRiemannSumBetween(item, timestamp, null, type, serviceId);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} until a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * A left approximation type is used for the Riemann sum.
     *
     * <b>Note:</b> The time dimension in the result is in seconds, therefore if you do not use QuantityType results,
     * you may have to
     * multiply or divide to get the result in the expected scale.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time to which to search for the riemannSum value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the Riemann sum until <code>timestamp</code>, or <code>null</code> if no
     *         future states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State riemannSumUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalRiemannSumBetween(item, null, timestamp, null, serviceId);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} until a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     *
     * <b>Note:</b> The time dimension in the result is in seconds, therefore if you do not use QuantityType results,
     * you may have to
     * multiply or divide to get the result in the expected scale.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time to which to search for the riemannSum value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the Riemann sum until <code>timestamp</code>, or <code>null</code> if no
     *         future states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State riemannSumUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return internalRiemannSumBetween(item, null, timestamp, type, serviceId);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} between two certain points in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * A left approximation type is used for the Riemann sum.
     *
     * <b>Note:</b> The time dimension in the result is in seconds, therefore if you do not use QuantityType results,
     * you may have to
     * multiply or divide to get the result in the expected scale.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the Riemann sum between <code>begin</code> and <code>end</code>, or <code>null</code> if no
     *         states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable State riemannSumBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return internalRiemannSumBetween(item, begin, end, null, serviceId);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} between two certain points in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     *
     * <b>Note:</b> The time dimension in the result is in seconds, therefore if you do not use QuantityType results,
     * you may have to
     * multiply or divide to get the result in the expected scale.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the Riemann sum between <code>begin</code> and <code>end</code>, or <code>null</code> if no
     *         states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable State riemannSumBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type, @Nullable String serviceId) {
        return internalRiemannSumBetween(item, begin, end, type, serviceId);
    }

    private static @Nullable State internalRiemannSumBetween(Item item, @Nullable ZonedDateTime begin,
            @Nullable ZonedDateTime end, @Nullable RiemannType type, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime beginTime = Objects.requireNonNullElse(begin, now);
        ZonedDateTime endTime = Objects.requireNonNullElse(end, now);

        Iterable<HistoricItem> result = getAllStatesBetweenWithBoundaries(item, begin, end, effectiveServiceId);
        if (result == null) {
            return null;
        }
        Iterator<HistoricItem> it = result.iterator();
        // Remove initial part of history that does not have any values persisted
        if (beginTime.isBefore(now)) {
            if (it.hasNext()) {
                beginTime = it.next().getTimestamp();
            }
            it = result.iterator();
        }

        Item baseItem = item instanceof GroupItem groupItem ? groupItem.getBaseItem() : item;
        Unit<?> unit = (baseItem instanceof NumberItem numberItem) ? numberItem.getUnit() : null;
        BigDecimal sum = riemannSum(beginTime.toInstant(), endTime.toInstant(), it, unit, type).scaleByPowerOfTen(-3);
        if (unit != null) {
            return new QuantityType<>(sum, unit.multiply(Units.SECOND));
        }
        return new DecimalType(sum);
    }

    private static BigDecimal riemannSum(Instant begin, Instant end, Iterator<HistoricItem> it, @Nullable Unit<?> unit,
            @Nullable RiemannType type) {
        RiemannType riemannType = type == null ? RiemannType.LEFT : type;

        BigDecimal sum = BigDecimal.ZERO;
        HistoricItem prevItem = null;
        HistoricItem nextItem;
        DecimalType prevState = null;
        DecimalType nextState;
        Instant prevInstant = null;
        Instant nextInstant;
        Duration prevDuration = Duration.ZERO;
        Duration nextDuration;

        boolean midpointStartBucket = true; // The start and end buckets for the midpoint calculation should be
                                            // considered for the full length, this flag is used to find the start
                                            // bucket
        if ((riemannType == RiemannType.MIDPOINT) && it.hasNext()) {
            prevItem = it.next();
            prevInstant = prevItem.getInstant();
            prevState = getPersistedValue(prevItem, unit);
        }

        while (it.hasNext()) {
            nextItem = it.next();
            nextInstant = nextItem.getInstant();
            BigDecimal weight = BigDecimal.ZERO;
            BigDecimal value = BigDecimal.ZERO;
            switch (riemannType) {
                case LEFT:
                    if (prevItem != null) {
                        prevState = getPersistedValue(prevItem, unit);
                        if (prevState != null) {
                            value = prevState.toBigDecimal();
                            weight = BigDecimal.valueOf(Duration.between(prevInstant, nextInstant).toMillis());
                        }
                    }
                    prevItem = nextItem;
                    prevInstant = nextInstant;
                    break;
                case RIGHT:
                    nextState = getPersistedValue(nextItem, unit);
                    if (nextState != null) {
                        value = nextState.toBigDecimal();
                        if (prevItem == null) {
                            weight = BigDecimal.valueOf(Duration.between(begin, nextInstant).toMillis());
                        } else {
                            weight = BigDecimal.valueOf(Duration.between(prevInstant, nextInstant).toMillis());
                        }
                    }
                    prevItem = nextItem;
                    prevInstant = nextInstant;
                    break;
                case TRAPEZOIDAL:
                    if (prevItem != null) {
                        prevState = getPersistedValue(prevItem, unit);
                        nextState = getPersistedValue(nextItem, unit);
                        if (prevState != null && nextState != null) {
                            value = prevState.toBigDecimal().add(nextState.toBigDecimal())
                                    .divide(BigDecimal.valueOf(2));
                            weight = BigDecimal.valueOf(Duration.between(prevInstant, nextInstant).toMillis());
                        }
                    }
                    prevItem = nextItem;
                    prevInstant = nextInstant;
                    break;
                case MIDPOINT:
                    if (prevItem != null) {
                        DecimalType currentState = getPersistedValue(prevItem, unit);
                        if (currentState != null) {
                            value = currentState.toBigDecimal();
                            if (midpointStartBucket && !prevDuration.isZero() && prevState != null) {
                                // Add half of the start bucket with the start value (left approximation)
                                sum = sum.add(prevState.toBigDecimal()
                                        .multiply(BigDecimal.valueOf(prevDuration.toMillis() / 2)));
                                midpointStartBucket = false;
                            }
                            nextDuration = Duration.between(prevInstant, nextInstant);
                            weight = prevDuration.isZero() || nextDuration.isZero() ? BigDecimal.ZERO
                                    : BigDecimal.valueOf(prevDuration.plus(nextDuration).toMillis() / 2);
                            if (!nextDuration.isZero()) {
                                prevDuration = nextDuration;
                            }
                            prevState = currentState;
                        }
                    }
                    prevItem = nextItem;
                    prevInstant = nextInstant;
                    break;
            }
            sum = sum.add(value.multiply(weight));
        }

        if ((riemannType == RiemannType.MIDPOINT) && (prevItem != null)) {
            // Add half of the end bucket with the end value (right approximation)
            DecimalType dtState = getPersistedValue(prevItem, unit);
            if (dtState != null) {
                BigDecimal value = dtState.toBigDecimal();
                BigDecimal weight = BigDecimal.valueOf(prevDuration.toMillis() / 2);
                sum = sum.add(value.multiply(weight));
            }
        }

        return sum;
    }

    /**
     * Gets the median value of the state of a given {@link Item} since a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the median value for
     * @param timestamp the point in time from which to search for the median value
     * @return the median value since <code>timestamp</code> or <code>null</code> if no
     *         previous states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State medianSince(Item item, ZonedDateTime timestamp) {
        return internalMedianBetween(item, timestamp, null, null);
    }

    /**
     * Gets the median value of the state of a given {@link Item} until a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the median value for
     * @param timestamp the point in time to which to search for the median value
     * @return the median value until <code>timestamp</code> or <code>null</code> if no
     *         future states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State medianUntil(Item item, ZonedDateTime timestamp) {
        return internalMedianBetween(item, null, timestamp, null);
    }

    /**
     * Gets the median value of the state of a given {@link Item} between two certain points in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the median value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @return the median value between <code>begin</code> and <code>end</code> or <code>null</code> if no
     *         states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}.
     */
    public static @Nullable State medianBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return internalMedianBetween(item, begin, end, null);
    }

    /**
     * Gets the median value of the state of a given {@link Item} since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the median value for
     * @param timestamp the point in time from which to search for the median value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the median value since <code>timestamp</code>, or <code>null</code> if no
     *         previous states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State medianSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalMedianBetween(item, timestamp, null, serviceId);
    }

    /**
     * Gets the median value of the state of a given {@link Item} until a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the median value for
     * @param timestamp the point in time to which to search for the median value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the median value until <code>timestamp</code>, or <code>null</code> if no
     *         future states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State medianUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalMedianBetween(item, null, timestamp, serviceId);
    }

    /**
     * Gets the median value of the state of a given {@link Item} between two certain points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the median value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the median value between <code>begin</code> and <code>end</code>, or <code>null</code> if no
     *         states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable State medianBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return internalMedianBetween(item, begin, end, serviceId);
    }

    private static @Nullable State internalMedianBetween(Item item, @Nullable ZonedDateTime begin,
            @Nullable ZonedDateTime end, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime beginTime = Objects.requireNonNullElse(begin, now);
        ZonedDateTime endTime = Objects.requireNonNullElse(end, now);

        if (beginTime.isEqual(endTime)) {
            HistoricItem historicItem = internalPersistedState(item, beginTime, effectiveServiceId);
            return historicItem != null ? historicItem.getState() : null;
        }

        Iterable<HistoricItem> result = internalGetAllStatesBetween(item, beginTime, endTime, effectiveServiceId);
        if (result == null) {
            return null;
        }

        Item baseItem = item instanceof GroupItem groupItem ? groupItem.getBaseItem() : item;
        Unit<?> unit = baseItem instanceof NumberItem numberItem ? numberItem.getUnit() : null;

        List<BigDecimal> resultList = new ArrayList<>();
        result.forEach(hi -> {
            DecimalType dtState = getPersistedValue(hi, unit);
            if (dtState != null) {
                resultList.add(dtState.toBigDecimal());
            }
        });

        BigDecimal median = Statistics.median(resultList);
        if (median != null) {
            if (unit != null) {
                return new QuantityType<>(median, unit);
            } else {
                return new DecimalType(median);
            }
        }
        return null;
    }

    /**
     * Gets the sum of the state of a given <code>item</code> since a certain point in time.
     * This method does not calculate a Riemann sum and therefore cannot be used as an approximation for the integral
     * value.
     * The default persistence service is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the item for which we will sum its persisted state values since <code>timestamp</code>
     * @param timestamp the point in time from which to start the summation
     * @return the sum of the state values since <code>timestamp</code>, or null if <code>timestamp</code> is in the
     *         future or the default persistence service does not refer to a {@link QueryablePersistenceService}
     */
    public @Nullable static State sumSince(Item item, ZonedDateTime timestamp) {
        return internalSumBetween(item, timestamp, null, null);
    }

    /**
     * Gets the sum of the state of a given <code>item</code> until a certain point in time.
     * This method does not calculate a Riemann sum and therefore cannot be used as an approximation for the integral
     * value.
     * The default persistence service is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the item for which we will sum its persisted state values to <code>timestamp</code>
     * @param timestamp the point in time to which to start the summation
     * @return the sum of the state values until <code>timestamp</code>, or null if <code>timestamp</code> is in the
     *         past or the default persistence service does not refer to a {@link QueryablePersistenceService}
     */
    public @Nullable static State sumUntil(Item item, ZonedDateTime timestamp) {
        return internalSumBetween(item, null, timestamp, null);
    }

    /**
     * Gets the sum of the state of a given <code>item</code> between two certain points in time.
     * This method does not calculate a Riemann sum and therefore cannot be used as an approximation for the integral
     * value.
     * The default persistence service is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the item for which we will sum its persisted state values between <code>begin</code> and
     *            <code>end</code>
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @return the sum of the state values between the given points in time, or null if <code>begin</code> is after
     *         <code>end</code> or if the default persistence service does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public @Nullable static State sumBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return internalSumBetween(item, begin, end, null);
    }

    /**
     * Gets the sum of the state of a given <code>item</code> since a certain point in time.
     * This method does not calculate a Riemann sum and therefore cannot be used as an approximation for the integral
     * value.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the item for which we will sum its persisted state values since <code>timestamp</code>
     * @param timestamp the point in time from which to start the summation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the sum of the state values since <code>timestamp</code>, or null if <code>timestamp</code> is in the
     *         future or <code>serviceId</code> does not refer to a {@link QueryablePersistenceService}
     */
    public @Nullable static State sumSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalSumBetween(item, timestamp, null, serviceId);
    }

    /**
     * Gets the sum of the state of a given <code>item</code> until a certain point in time.
     * This method does not calculate a Riemann sum and therefore cannot be used as an approximation for the integral
     * value.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the item for which we will sum its persisted state values to <code>timestamp</code>
     * @param timestamp the point in time to which to start the summation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the sum of the state values until <code>timestamp</code>, or null if <code>timestamp</code> is in the
     *         past or <code>serviceId</code> does not refer to a {@link QueryablePersistenceService}
     */
    public @Nullable static State sumUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalSumBetween(item, null, timestamp, serviceId);
    }

    /**
     * Gets the sum of the state of a given <code>item</code> between two certain points in time.
     * This method does not calculate a Riemann sum and therefore cannot be used as an approximation for the integral
     * value.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the item for which we will sum its persisted state values between <code>begin</code> and
     *            <code>end</code>
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the sum of the state values between the given points in time, or null if <code>begin</code> is after
     *         <code>end</code> or <code>serviceId</code> does not refer to a {@link QueryablePersistenceService}
     */
    public @Nullable static State sumBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return internalSumBetween(item, begin, end, serviceId);
    }

    private @Nullable static State internalSumBetween(Item item, @Nullable ZonedDateTime begin,
            @Nullable ZonedDateTime end, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        Iterable<HistoricItem> result = internalGetAllStatesBetween(item, begin, end, effectiveServiceId);
        if (result != null) {
            Iterator<HistoricItem> it = result.iterator();

            Item baseItem = item instanceof GroupItem groupItem ? groupItem.getBaseItem() : item;
            Unit<?> unit = baseItem instanceof NumberItem numberItem ? numberItem.getUnit() : null;
            BigDecimal sum = BigDecimal.ZERO;
            while (it.hasNext()) {
                HistoricItem historicItem = it.next();
                DecimalType value = getPersistedValue(historicItem, unit);
                if (value != null) {
                    sum = sum.add(value.toBigDecimal());
                }
            }
            if (unit != null) {
                return new QuantityType<>(sum, unit);
            }
            return new DecimalType(sum);
        }
        return null;
    }

    /**
     * Gets the difference value of the state of a given <code>item</code> since a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item to get the delta state value for
     * @param timestamp the point in time from which to compute the delta
     * @return the difference between now and then, or <code>null</code> if there is no default persistence
     *         service available, the default persistence service is not a {@link QueryablePersistenceService}, or if
     *         there is no persisted state for the given <code>item</code> at the given <code>timestamp</code> available
     *         in the default persistence service
     */
    public static @Nullable State deltaSince(Item item, ZonedDateTime timestamp) {
        return internalDeltaBetween(item, timestamp, null, null);
    }

    /**
     * Gets the difference value of the state of a given <code>item</code> until a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item to get the delta state value for
     * @param timestamp the point in time to which to compute the delta
     * @return the difference between then and now, or <code>null</code> if there is no default persistence
     *         service available, the default persistence service is not a {@link QueryablePersistenceService}, or if
     *         there is no persisted state for the given <code>item</code> at the given <code>timestamp</code> available
     *         in the default persistence service
     */
    public static @Nullable State deltaUntil(Item item, ZonedDateTime timestamp) {
        return internalDeltaBetween(item, null, timestamp, null);
    }

    /**
     * Gets the difference value of the state of a given <code>item</code> between two points in time.
     * The default persistence service is used.
     *
     * @param item the item to get the delta for
     * @param begin the beginning point in time
     * @param end the end point in time
     * @return the difference between end and begin, or <code>null</code> if the default persistence service does not
     *         refer to an available {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> for the given points in time
     */
    public static @Nullable State deltaBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return internalDeltaBetween(item, begin, end, null);
    }

    /**
     * Gets the difference value of the state of a given <code>item</code> since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the delta for
     * @param timestamp the point in time from which to compute the delta
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the difference between now and then, or <code>null</code> if the given serviceId does not refer to an
     *         available {@link QueryablePersistenceService}, or if there is no persisted state for the given
     *         <code>item</code> at the given <code>timestamp</code> using the persistence service named
     *         <code>serviceId</code>
     */
    public static @Nullable State deltaSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalDeltaBetween(item, timestamp, null, serviceId);
    }

    /**
     * Gets the difference value of the state of a given <code>item</code> until a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the delta for
     * @param timestamp the point in time to which to compute the delta
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the difference between then and now, or <code>null</code> if the given serviceId does not refer to an
     *         available {@link QueryablePersistenceService}, or if there is no persisted state for the given
     *         <code>item</code> at the given <code>timestamp</code> using the persistence service named
     *         <code>serviceId</code>
     */
    public static @Nullable State deltaUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalDeltaBetween(item, null, timestamp, serviceId);
    }

    /**
     * Gets the difference value of the state of a given <code>item</code> between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the delta for
     * @param begin the beginning point in time
     * @param end the end point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the difference between end and begin, or <code>null</code> if the given serviceId does not refer to an
     *         available {@link QueryablePersistenceService}, or if there is no persisted state for the given
     *         <code>item</code> at the given points in time
     */
    public static @Nullable State deltaBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return internalDeltaBetween(item, begin, end, serviceId);
    }

    private static @Nullable State internalDeltaBetween(Item item, @Nullable ZonedDateTime begin,
            @Nullable ZonedDateTime end, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        HistoricItem itemStart = internalPersistedState(item, begin, effectiveServiceId);
        HistoricItem itemStop = internalPersistedState(item, end, effectiveServiceId);

        Item baseItem = item instanceof GroupItem groupItem ? groupItem.getBaseItem() : item;
        Unit<?> unit = baseItem instanceof NumberItem numberItem ? numberItem.getUnit() : null;

        DecimalType valueStart = null;
        if (itemStart != null) {
            valueStart = getPersistedValue(itemStart, unit);
        }
        DecimalType valueStop = null;
        if (itemStop != null) {
            valueStop = getPersistedValue(itemStop, unit);
        }

        if (begin == null && end != null && end.isAfter(ZonedDateTime.now())) {
            valueStart = getItemValue(item, unit);
        }
        if (begin != null && end == null && begin.isBefore(ZonedDateTime.now())) {
            valueStop = getItemValue(item, unit);
        }

        if (valueStart != null && valueStop != null) {
            BigDecimal delta = valueStop.toBigDecimal().subtract(valueStart.toBigDecimal());
            return (unit != null) ? new QuantityType<>(delta, unit) : new DecimalType(delta);
        }
        return null;
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} since a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * This method has been deprecated and {@link #evolutionRateSince(Item, ZonedDateTime)} should be used instead.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the item to get the evolution rate value for
     * @param timestamp the point in time from which to compute the evolution rate
     * @return the evolution rate in percent (positive and negative) between now and then, or <code>null</code> if
     *         there is no default persistence service available, the default persistence service is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>, or if there is a state but it is zero (which would cause a
     *         divide-by-zero error)
     */
    @Deprecated
    public static @Nullable DecimalType evolutionRate(Item item, ZonedDateTime timestamp) {
        LoggerFactory.getLogger(PersistenceExtensions.class).info(
                "The evolutionRate method has been deprecated and will be removed in a future version, use evolutionRateSince instead.");
        return internalEvolutionRateBetween(item, timestamp, null, null);
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} since a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the item to get the evolution rate value for
     * @param timestamp the point in time from which to compute the evolution rate
     * @return the evolution rate in percent (positive and negative) between now and then, or <code>null</code> if
     *         there is no default persistence service available, the default persistence service is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>, or if there is a state but it is zero (which would cause a
     *         divide-by-zero error)
     */
    public static @Nullable DecimalType evolutionRateSince(Item item, ZonedDateTime timestamp) {
        return internalEvolutionRateBetween(item, timestamp, null, null);
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} until a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the item to get the evolution rate value for
     * @param timestamp the point in time to which to compute the evolution rate
     * @return the evolution rate in percent (positive and negative) between then and now, or <code>null</code> if
     *         there is no default persistence service available, the default persistence service is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>, or if there is a state but it is zero (which would cause a
     *         divide-by-zero error)
     */
    public static @Nullable DecimalType evolutionRateUntil(Item item, ZonedDateTime timestamp) {
        return internalEvolutionRateBetween(item, null, timestamp, null);
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} between two points in time.
     * The default {@link PersistenceService} is used.
     *
     * This method has been deprecated and {@link #evolutionRateBetween(Item, ZonedDateTime, ZonedDateTime)} should be
     * used instead.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the item to get the evolution rate value for
     * @param begin the beginning point in time
     * @param end the end point in time
     * @return the evolution rate in percent (positive and negative) in the given interval, or <code>null</code> if
     *         there is no default persistence service available, the default persistence service is not a
     *         {@link QueryablePersistenceService}, or if there are no persisted state for the given <code>item</code>
     *         at the given interval, or if there is a state but it is zero (which would cause a
     *         divide-by-zero error)
     */
    @Deprecated
    public static @Nullable DecimalType evolutionRate(Item item, ZonedDateTime begin, ZonedDateTime end) {
        LoggerFactory.getLogger(PersistenceExtensions.class).info(
                "The evolutionRate method has been deprecated and will be removed in a future version, use evolutionRateBetween instead.");
        return internalEvolutionRateBetween(item, begin, end, null);
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} between two points in time.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the item to get the evolution rate value for
     * @param begin the beginning point in time
     * @param end the end point in time
     * @return the evolution rate in percent (positive and negative) in the given interval, or <code>null</code> if
     *         there is no default persistence service available, the default persistence service is not a
     *         {@link QueryablePersistenceService}, or if there are no persisted state for the given <code>item</code>
     *         at the given interval, or if there is a state but it is zero (which would cause a
     *         divide-by-zero error)
     */
    public static @Nullable DecimalType evolutionRateBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return internalEvolutionRateBetween(item, begin, end, null);
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * This method has been deprecated and {@link #evolutionRateSince(Item, ZonedDateTime, String)} should be used
     * instead.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the evolution rate value for
     * @param timestamp the point in time from which to compute the evolution rate
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the evolution rate in percent (positive and negative) between now and then, or <code>null</code> if
     *         the persistence service given by <code>serviceId</code> is not available or is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given
     *         <code>item</code> at the given <code>timestamp</code> using the persistence service given by
     *         <code>serviceId</code>, or if there is a state but it is zero (which would cause a divide-by-zero
     *         error)
     */
    @Deprecated
    public static @Nullable DecimalType evolutionRate(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        LoggerFactory.getLogger(PersistenceExtensions.class).info(
                "The evolutionRate method has been deprecated and will be removed in a future version, use evolutionRateSince instead.");
        return internalEvolutionRateBetween(item, timestamp, null, serviceId);
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the evolution rate value for
     * @param timestamp the point in time from which to compute the evolution rate
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the evolution rate in percent (positive and negative) between now and then, or <code>null</code> if
     *         the persistence service given by <code>serviceId</code> is not available or is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given
     *         <code>item</code> at the given <code>timestamp</code> using the persistence service given by
     *         <code>serviceId</code>, or if there is a state but it is zero (which would cause a divide-by-zero
     *         error)
     */
    public static @Nullable DecimalType evolutionRateSince(Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return internalEvolutionRateBetween(item, timestamp, null, serviceId);
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} until a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the evolution rate value for
     * @param timestamp the point in time to which to compute the evolution rate
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the evolution rate in percent (positive and negative) between then and now, or <code>null</code> if
     *         the persistence service given by <code>serviceId</code> is not available or is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given
     *         <code>item</code> at the given <code>timestamp</code> using the persistence service given by
     *         <code>serviceId</code>, or if there is a state but it is zero (which would cause a divide-by-zero
     *         error)
     */
    public static @Nullable DecimalType evolutionRateUntil(Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return internalEvolutionRateBetween(item, null, timestamp, serviceId);
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * This method has been deprecated and {@link #evolutionRateBetween(Item, ZonedDateTime, ZonedDateTime, String)}
     * should be used instead.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the evolution rate value for
     * @param begin the beginning point in time
     * @param end the end point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the evolution rate in percent (positive and negative) in the given interval, or <code>null</code> if
     *         the persistence service given by <code>serviceId</code> is not available or is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given
     *         <code>item</code> at the given <code>begin</code> and <code>end</code> using the persistence service
     *         given by <code>serviceId</code>, or if there is a state but it is zero (which would cause a
     *         divide-by-zero error)
     */
    @Deprecated
    public static @Nullable DecimalType evolutionRate(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        LoggerFactory.getLogger(PersistenceExtensions.class).info(
                "The evolutionRate method has been deprecated and will be removed in a future version, use evolutionRateBetween instead.");
        return internalEvolutionRateBetween(item, begin, end, serviceId);
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If the {@link Item} has a dimension, the calculation will be done using the {@link Item}'s
     * configured unit.
     * For temperatures, this will give different results for different configured units.
     *
     * @param item the {@link Item} to get the evolution rate value for
     * @param begin the beginning point in time
     * @param end the end point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the evolution rate in percent (positive and negative) in the given interval, or <code>null</code> if
     *         the persistence service given by <code>serviceId</code> is not available or is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given
     *         <code>item</code> at the given <code>begin</code> and <code>end</code> using the persistence service
     *         given by <code>serviceId</code>, or if there is a state but it is zero (which would cause a
     *         divide-by-zero error)
     */
    public static @Nullable DecimalType evolutionRateBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return internalEvolutionRateBetween(item, begin, end, serviceId);
    }

    private static @Nullable DecimalType internalEvolutionRateBetween(Item item, @Nullable ZonedDateTime begin,
            @Nullable ZonedDateTime end, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }

        Item baseItem = item instanceof GroupItem groupItem ? groupItem.getBaseItem() : item;
        Unit<?> unit = baseItem instanceof NumberItem numberItem ? numberItem.getUnit() : null;

        HistoricItem itemStart = internalPersistedState(item, begin, effectiveServiceId);
        HistoricItem itemStop = internalPersistedState(item, end, effectiveServiceId);

        DecimalType valueStart = null;
        if (itemStart != null) {
            valueStart = getPersistedValue(itemStart, unit);
        }
        DecimalType valueStop = null;
        if (itemStop != null) {
            valueStop = getPersistedValue(itemStop, unit);
        }

        if (begin == null && end != null && end.isAfter(ZonedDateTime.now())) {
            valueStart = getItemValue(item, unit);
        }
        if (begin != null && end == null && begin.isBefore(ZonedDateTime.now())) {
            valueStop = getItemValue(item, unit);
        }

        if (valueStart != null && valueStop != null && !valueStart.equals(DecimalType.ZERO)) {
            return new DecimalType(valueStop.toBigDecimal().subtract(valueStart.toBigDecimal())
                    .divide(valueStart.toBigDecimal(), MathContext.DECIMAL64).movePointRight(2));
        }
        return null;
    }

    /**
     * Gets the number of available historic data points of a given {@link Item} from a point in time until now.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to query
     * @param timestamp the beginning point in time
     * @return the number of values persisted for this item, <code>null</code> if <code>timestamp</code> is in the
     *         future, if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countSince(Item item, ZonedDateTime timestamp) {
        return internalCountBetween(item, timestamp, null, null);
    }

    /**
     * Gets the number of available data points of a given {@link Item} from now to a point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to query
     * @param timestamp the ending point in time
     * @return the number of values persisted for this item, <code>null</code> if <code>timestamp</code> is in the
     *         past, if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countUntil(Item item, ZonedDateTime timestamp) {
        return internalCountBetween(item, null, timestamp, null);
    }

    /**
     * Gets the number of available data points of a given {@link Item} between two points in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to query
     * @param begin the beginning point in time
     * @param end the end point in time
     * @return the number of values persisted for this item, <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return internalCountBetween(item, begin, end, null);
    }

    /**
     * Gets the number of available historic data points of a given {@link Item} from a point in time until now.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to query
     * @param begin the beginning point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the number of values persisted for this item, <code>null</code> if <code>timestamp</code> is in the
     *         future, if the persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countSince(Item item, ZonedDateTime begin, @Nullable String serviceId) {
        return internalCountBetween(item, begin, null, serviceId);
    }

    /**
     * Gets the number of available data points of a given {@link Item} from now to a point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to query
     * @param timestamp the ending point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the number of values persisted for this item, <code>null</code> if <code>timestamp</code> is in the
     *         past, if the persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return internalCountBetween(item, null, timestamp, serviceId);
    }

    /**
     * Gets the number of available data points of a given {@link Item} between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to query
     * @param begin the beginning point in time
     * @param end the end point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the number of values persisted for this item, <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if the persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return internalCountBetween(item, begin, end, serviceId);
    }

    private static @Nullable Long internalCountBetween(Item item, @Nullable ZonedDateTime begin,
            @Nullable ZonedDateTime end, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        Iterable<HistoricItem> result = internalGetAllStatesBetween(item, begin, end, effectiveServiceId);
        if (result != null) {
            if (result instanceof Collection<?> collection) {
                return Long.valueOf(collection.size());
            } else {
                return StreamSupport.stream(result.spliterator(), false).count();
            }
        }
        return null;
    }

    /**
     * Gets the number of changes in historic data points of a given {@link Item} from a point in time until now.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to query
     * @param timestamp the beginning point in time
     * @return the number of state changes for this item, <code>null</code>
     *         if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countStateChangesSince(Item item, ZonedDateTime timestamp) {
        return internalCountStateChangesBetween(item, timestamp, null, null);
    }

    /**
     * Gets the number of changes in data points of a given {@link Item} from now until a point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to query
     * @param timestamp the ending point in time
     * @return the number of state changes for this item, <code>null</code>
     *         if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countStateChangesUntil(Item item, ZonedDateTime timestamp) {
        return internalCountStateChangesBetween(item, null, timestamp, null);
    }

    /**
     * Gets the number of changes in data points of a given {@link Item} between two points in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to query
     * @param begin the beginning point in time
     * @param end the end point in time
     * @return the number of state changes for this item, <code>null</code>
     *         if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countStateChangesBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return internalCountStateChangesBetween(item, begin, end, null);
    }

    /**
     * Gets the number of changes in historic data points of a given {@link Item} from a point in time until now.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to query
     * @param timestamp the beginning point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the number of state changes for this item, <code>null</code>
     *         if the persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countStateChangesSince(Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return internalCountStateChangesBetween(item, timestamp, null, serviceId);
    }

    /**
     * Gets the number of changes in data points of a given {@link Item} from now until a point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to query
     * @param timestamp the ending point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the number of state changes for this item, <code>null</code>
     *         if the persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countStateChangesUntil(Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return internalCountStateChangesBetween(item, null, timestamp, serviceId);
    }

    /**
     * Gets the number of changes in data points of a given {@link Item} between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to query
     * @param begin the beginning point in time
     * @param end the end point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the number of state changes for this item, <code>null</code>
     *         if the persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countStateChangesBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return internalCountStateChangesBetween(item, begin, end, serviceId);
    }

    private static @Nullable Long internalCountStateChangesBetween(Item item, @Nullable ZonedDateTime begin,
            @Nullable ZonedDateTime end, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        Iterable<HistoricItem> result = internalGetAllStatesBetween(item, begin, end, effectiveServiceId);
        if (result != null) {
            Iterator<HistoricItem> it = result.iterator();

            if (!it.hasNext()) {
                return Long.valueOf(0);
            }

            long count = 0;
            State previousState = it.next().getState();
            while (it.hasNext()) {
                HistoricItem historicItem = it.next();
                State state = historicItem.getState();
                if (!state.equals(previousState)) {
                    previousState = state;
                    count++;
                }
            }
            return count;
        }
        return null;
    }

    /**
     * Retrieves the historic items for a given <code>item</code> since a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item for which to retrieve the historic item
     * @param timestamp the point in time from which to retrieve the states
     * @return the historic items since the given point in time, or <code>null</code>
     *         if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     *
     */
    public static @Nullable Iterable<HistoricItem> getAllStatesSince(Item item, ZonedDateTime timestamp) {
        return internalGetAllStatesBetween(item, timestamp, null, null);
    }

    /**
     * Retrieves the future items for a given <code>item</code> until a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item for which to retrieve the future item
     * @param timestamp the point in time to which to retrieve the states
     * @return the future items to the given point in time, or <code>null</code>
     *         if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Iterable<HistoricItem> getAllStatesUntil(Item item, ZonedDateTime timestamp) {
        return internalGetAllStatesBetween(item, null, timestamp, null);
    }

    /**
     * Retrieves the historic items for a given <code>item</code> between two points in time.
     * The default persistence service is used.
     *
     * @param item the item for which to retrieve the historic item
     * @param begin the point in time from which to retrieve the states
     * @param end the point in time to which to retrieve the states
     * @return the historic items between the given points in time, or <code>null</code>
     *         if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Iterable<HistoricItem> getAllStatesBetween(Item item, ZonedDateTime begin,
            ZonedDateTime end) {
        return internalGetAllStatesBetween(item, begin, end, null);
    }

    /**
     * Retrieves the historic items for a given <code>item</code> since a certain point in time
     * through a {@link PersistenceService} identified by the <code>serviceId</code>.
     *
     * @param item the item for which to retrieve the historic item
     * @param timestamp the point in time from which to retrieve the states
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the historic items since the given point in time, or <code>null</code>
     *         if the provided <code>serviceId</code> does not refer to an available
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Iterable<HistoricItem> getAllStatesSince(Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return internalGetAllStatesBetween(item, timestamp, null, serviceId);
    }

    /**
     * Retrieves the future items for a given <code>item</code> until a certain point in time
     * through a {@link PersistenceService} identified by the <code>serviceId</code>.
     *
     * @param item the item for which to retrieve the future item
     * @param timestamp the point in time to which to retrieve the states
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the future items to the given point in time, or <code>null</code>
     *         if the provided <code>serviceId</code> does not refer to an available
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Iterable<HistoricItem> getAllStatesUntil(Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return internalGetAllStatesBetween(item, null, timestamp, serviceId);
    }

    /**
     * Retrieves the historic items for a given <code>item</code> between two points in time
     * through a {@link PersistenceService} identified by the <code>serviceId</code>.
     *
     * @param item the item for which to retrieve the historic item
     * @param begin the point in time from which to retrieve the states
     * @param end the point in time to which to retrieve the states
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the historic items between the given points in time, or <code>null</code>
     *         if the provided <code>serviceId</code> does not refer to an available
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Iterable<HistoricItem> getAllStatesBetween(Item item, ZonedDateTime begin,
            ZonedDateTime end, @Nullable String serviceId) {
        return internalGetAllStatesBetween(item, begin, end, serviceId);
    }

    private static @Nullable Iterable<HistoricItem> internalGetAllStatesBetween(Item item,
            @Nullable ZonedDateTime begin, @Nullable ZonedDateTime end, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return null;
        }
        PersistenceService service = getService(effectiveServiceId);
        if (service instanceof QueryablePersistenceService qService) {
            FilterCriteria filter = new FilterCriteria();
            ZonedDateTime now = ZonedDateTime.now();
            if ((begin == null && end == null) || (begin != null && end == null && begin.isAfter(now))
                    || (begin == null && end != null && end.isBefore(now))) {
                LoggerFactory.getLogger(PersistenceExtensions.class).warn(
                        "Querying persistence service with open begin and/or end not allowed: begin {}, end {}, now {}",
                        begin, end, now);
                return null;
            }
            if (begin != null) {
                filter.setBeginDate(begin);
            } else {
                filter.setBeginDate(now);
            }
            if (end != null) {
                filter.setEndDate(end);
            } else {
                filter.setEndDate(now);
            }
            String alias = getAlias(item, effectiveServiceId);
            filter.setItemName(item.getName());
            filter.setOrdering(Ordering.ASCENDING);

            return qService.query(filter, alias);
        } else {
            LoggerFactory.getLogger(PersistenceExtensions.class)
                    .warn("There is no queryable persistence service registered with the id '{}'", effectiveServiceId);
        }
        return null;
    }

    /**
     * Removes from persistence the historic items for a given <code>item</code> since a certain point in time.
     * The default persistence service is used.
     * This will only have effect if the p{@link PersistenceService} is a {@link ModifiablePersistenceService}.
     *
     * @param item the item for which to remove the historic item
     * @param timestamp the point in time from which to remove the states
     */
    public static void removeAllStatesSince(Item item, ZonedDateTime timestamp) {
        internalRemoveAllStatesBetween(item, timestamp, null, null);
    }

    /**
     * Removes from persistence the future items for a given <code>item</code> until a certain point in time.
     * The default persistence service is used.
     * This will only have effect if the p{@link PersistenceService} is a {@link ModifiablePersistenceService}.
     *
     * @param item the item for which to remove the future item
     * @param timestamp the point in time to which to remove the states
     */
    public static void removeAllStatesUntil(Item item, ZonedDateTime timestamp) {
        internalRemoveAllStatesBetween(item, null, timestamp, null);
    }

    /**
     * Removes from persistence the historic items for a given <code>item</code> between two points in time.
     * The default persistence service is used.
     * This will only have effect if the p{@link PersistenceService} is a {@link ModifiablePersistenceService}.
     *
     * @param item the item for which to remove the historic item
     * @param begin the point in time from which to remove the states
     * @param end the point in time to which to remove the states
     */
    public static void removeAllStatesBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        internalRemoveAllStatesBetween(item, begin, end, null);
    }

    /**
     * Removes from persistence the historic items for a given <code>item</code> since a certain point in time
     * through a {@link PersistenceService} identified by the <code>serviceId</code>.
     * This will only have effect if the p{@link PersistenceService} is a {@link ModifiablePersistenceService}.
     *
     * @param item the item for which to remove the historic item
     * @param timestamp the point in time from which to remove the states
     * @param serviceId the name of the {@link PersistenceService} to use
     */
    public static void removeAllStatesSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        internalRemoveAllStatesBetween(item, timestamp, null, serviceId);
    }

    /**
     * Removes from persistence the future items for a given <code>item</code> until a certain point in time
     * through a {@link PersistenceService} identified by the <code>serviceId</code>.
     * This will only have effect if the p{@link PersistenceService} is a {@link ModifiablePersistenceService}.
     *
     * @param item the item for which to remove the future item
     * @param timestamp the point in time to which to remove the states
     * @param serviceId the name of the {@link PersistenceService} to use
     */
    public static void removeAllStatesUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        internalRemoveAllStatesBetween(item, null, timestamp, serviceId);
    }

    /**
     * Removes from persistence the historic items for a given <code>item</code> beetween two points in time
     * through a {@link PersistenceService} identified by the <code>serviceId</code>.
     * This will only have effect if the p{@link PersistenceService} is a {@link ModifiablePersistenceService}.
     *
     * @param item the item for which to remove the historic item
     * @param begin the point in time from which to remove the states
     * @param end the point in time to which to remove the states
     * @param serviceId the name of the {@link PersistenceService} to use
     */
    public static void removeAllStatesBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        internalRemoveAllStatesBetween(item, begin, end, serviceId);
    }

    private static void internalRemoveAllStatesBetween(Item item, @Nullable ZonedDateTime begin,
            @Nullable ZonedDateTime end, @Nullable String serviceId) {
        String effectiveServiceId = serviceId == null ? getDefaultServiceId() : serviceId;
        if (effectiveServiceId == null) {
            return;
        }
        PersistenceService service = getService(effectiveServiceId);
        if (service instanceof ModifiablePersistenceService mService) {
            FilterCriteria filter = new FilterCriteria();
            ZonedDateTime now = ZonedDateTime.now();
            if ((begin == null && end == null) || (begin != null && end == null && begin.isAfter(now))
                    || (begin == null && end != null && end.isBefore(now))) {
                LoggerFactory.getLogger(PersistenceExtensions.class).warn(
                        "Querying persistence service with open begin and/or end not allowed: begin {}, end {}, now {}",
                        begin, end, now);
                return;
            }
            if (begin != null) {
                filter.setBeginDate(begin);
            } else {
                filter.setBeginDate(now);
            }
            if (end != null) {
                filter.setEndDate(end);
            } else {
                filter.setEndDate(now);
            }
            String alias = getAlias(item, effectiveServiceId);
            filter.setItemName(item.getName());
            filter.setOrdering(Ordering.ASCENDING);

            mService.remove(filter, alias);
        } else {
            LoggerFactory.getLogger(PersistenceExtensions.class)
                    .warn("There is no modifiable persistence service registered with the id '{}'", effectiveServiceId);
        }
        return;
    }

    private static @Nullable Iterable<HistoricItem> getAllStatesBetweenWithBoundaries(Item item,
            @Nullable ZonedDateTime begin, @Nullable ZonedDateTime end, @Nullable String serviceId) {
        Iterable<HistoricItem> betweenItems = internalGetAllStatesBetween(item, begin, end, serviceId);

        ZonedDateTime now = ZonedDateTime.now();
        if ((begin == null && end == null) || (begin != null && end == null && begin.isAfter(now))
                || (begin == null && end != null && end.isBefore(now))
                || (begin != null && end != null && end.isBefore(begin))) {
            return null;
        }

        ZonedDateTime beginTime = Objects.requireNonNullElse(begin, now);
        ZonedDateTime endTime = Objects.requireNonNullElse(end, now);

        List<HistoricItem> betweenItemsList = new ArrayList<>();
        if (betweenItems != null) {
            for (HistoricItem historicItem : betweenItems) {
                betweenItemsList.add(historicItem);
            }
        }

        // add HistoricItem at begin
        if (betweenItemsList.isEmpty() || !betweenItemsList.getFirst().getTimestamp().equals(begin)) {
            HistoricItem first = beginTime.equals(now) ? historicItemOrCurrentState(item, null)
                    : internalPersistedState(item, beginTime, serviceId);
            if (first != null) {
                first = new RetimedHistoricItem(first, beginTime);
            }
            if (first != null) {
                betweenItemsList.addFirst(first);
            }
        }

        // add HistoricItem at end
        if (betweenItemsList.isEmpty() || !betweenItemsList.getLast().getTimestamp().equals(end)) {
            HistoricItem last = endTime.equals(now) ? historicItemOrCurrentState(item, null)
                    : internalPersistedState(item, endTime, serviceId);
            if (last != null) {
                last = new RetimedHistoricItem(last, endTime);
            }
            if (last != null) {
                betweenItemsList.add(last);
            }
        }
        return !betweenItemsList.isEmpty() ? betweenItemsList : null;
    }

    private static @Nullable PersistenceService getService(String serviceId) {
        PersistenceServiceRegistry reg = registry;
        return reg != null ? reg.get(serviceId) : null;
    }

    private static @Nullable String getDefaultServiceId() {
        PersistenceServiceRegistry reg = registry;
        if (reg != null) {
            String id = reg.getDefaultId();
            if (id != null) {
                return id;
            } else {
                LoggerFactory.getLogger(PersistenceExtensions.class)
                        .warn("There is no default persistence service configured!");
            }
        } else {
            LoggerFactory.getLogger(PersistenceExtensions.class)
                    .warn("PersistenceServiceRegistryImpl is not available!");
        }
        return null;
    }

    private static @Nullable String getAlias(Item item, String serviceId) {
        PersistenceServiceConfigurationRegistry reg = configRegistry;
        if (reg != null) {
            PersistenceServiceConfiguration config = reg.get(serviceId);
            return config != null ? config.getAliases().get(item.getName()) : null;
        }
        return null;
    }

    private static @Nullable DecimalType getItemValue(Item item, @Nullable Unit<?> unit) {
        if (unit != null) {
            QuantityType<?> qt = item.getStateAs(QuantityType.class);
            qt = (qt != null) ? qt.toUnit(unit) : qt;
            if (qt != null) {
                return new DecimalType(qt.toBigDecimal());
            }
        }
        return item.getStateAs(DecimalType.class);
    }

    private static @Nullable DecimalType getPersistedValue(HistoricItem historicItem, @Nullable Unit<?> unit) {
        State state = historicItem.getState();
        if (unit != null) {
            if (state instanceof QuantityType<?> qtState) {
                qtState = qtState.toUnit(unit);
                if (qtState != null) {
                    state = qtState;
                } else {
                    LoggerFactory.getLogger(PersistenceExtensions.class).warn(
                            "Unit of state {} at time {} retrieved from persistence not compatible with item unit {} for item {}",
                            state, historicItem.getTimestamp(), unit, historicItem.getName());
                    return null;
                }
            } else {
                LoggerFactory.getLogger(PersistenceExtensions.class).warn(
                        "Item {} is QuantityType but state {} at time {} retrieved from persistence has no unit",
                        historicItem.getName(), historicItem.getState(), historicItem.getTimestamp());
                return null;
            }
        }
        return state.as(DecimalType.class);
    }

    private static @Nullable HistoricItem historicItemOrCurrentState(Item item, @Nullable HistoricItem historicItem) {
        if (historicItem == null) {
            // there are no historic states we couldn't determine a value, construct a HistoricItem from the current
            // state
            return new HistoricItem() {
                @Override
                public ZonedDateTime getTimestamp() {
                    return ZonedDateTime.now();
                }

                @Override
                public State getState() {
                    return item.getState();
                }

                @Override
                public String getName() {
                    return item.getName();
                }
            };
        } else {
            return historicItem;
        }
    }

    private static class RetimedHistoricItem implements HistoricItem {

        private final HistoricItem originItem;
        private final ZonedDateTime timestamp;

        public RetimedHistoricItem(HistoricItem originItem, ZonedDateTime timestamp) {
            this.originItem = originItem;
            this.timestamp = timestamp;
        }

        @Override
        public ZonedDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public State getState() {
            return originItem.getState();
        }

        @Override
        public String getName() {
            return originItem.getName();
        }

        @Override
        public String toString() {
            return "RetimedHistoricItem [originItem=" + originItem + ", timestamp=" + timestamp + "]";
        }
    }
}
