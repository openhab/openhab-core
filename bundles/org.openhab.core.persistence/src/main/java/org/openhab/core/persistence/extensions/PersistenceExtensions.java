/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Iterator;

import org.openhab.core.items.Item;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.FilterCriteria.Ordering;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.PersistenceServiceRegistry;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.types.State;
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
 */
@Component(immediate = true)
public class PersistenceExtensions {

    private static final BigDecimal BIG_DECIMAL_TWO = BigDecimal.valueOf(2);

    private static PersistenceServiceRegistry registry;

    @Activate
    public PersistenceExtensions(@Reference PersistenceServiceRegistry registry) {
        PersistenceExtensions.registry = registry;
    }

    private static PersistenceService getService(String serviceId) {
        PersistenceService service = null;
        if (registry != null) {
            if (serviceId != null) {
                service = registry.get(serviceId);
            } else {
                service = registry.getDefault();
            }
        }
        return service;
    }

    private static String getDefaultServiceId() {
        if (registry != null) {
            String id = registry.getDefaultId();
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

    /**
     * Persists the state of a given <code>item</code> through a {@link PersistenceService} identified
     * by the <code>serviceId</code>.
     *
     * @param item the item to store
     * @param serviceId the name of the {@link PersistenceService} to use
     */
    public static void persist(Item item, String serviceId) {
        PersistenceService service = getService(serviceId);
        if (service != null) {
            service.store(item);
        } else {
            LoggerFactory.getLogger(PersistenceExtensions.class)
                    .warn("There is no persistence service registered with the id '{}'", serviceId);
        }
    }

    /**
     * Persists the state of a given <code>item</code> through the default persistence service.
     *
     * @param item the item to store
     */
    public static void persist(Item item) {
        persist(item, getDefaultServiceId());
    }

    /**
     * Retrieves the historic item for a given <code>item</code> at a certain point in time through the default
     * persistence service.
     *
     * @param item the item for which to retrieve the historic item
     * @param timestamp the point in time for which the historic item should be retrieved
     * @return the historic item at the given point in time, or <code>null</code> if no historic item could be found,
     *         the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static HistoricItem historicState(Item item, ZonedDateTime timestamp) {
        return historicState(item, timestamp, getDefaultServiceId());
    }

    /**
     * Retrieves the historic item for a given <code>item</code> at a certain point in time through a
     * {@link PersistenceService} identified by the <code>serviceId</code>.
     *
     * @param item the item for which to retrieve the historic item
     * @param timestamp the point in time for which the historic item should be retrieved
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the historic item at the given point in time, or <code>null</code> if no historic item could be found or
     *         if the provided <code>serviceId</code> does not refer to an available
     *         {@link QueryablePersistenceService}
     */
    public static HistoricItem historicState(Item item, ZonedDateTime timestamp, String serviceId) {
        PersistenceService service = getService(serviceId);
        if (service instanceof QueryablePersistenceService) {
            QueryablePersistenceService qService = (QueryablePersistenceService) service;
            FilterCriteria filter = new FilterCriteria();
            filter.setEndDate(timestamp);
            filter.setItemName(item.getName());
            filter.setPageSize(1);
            filter.setOrdering(Ordering.DESCENDING);
            Iterable<HistoricItem> result = qService.query(filter);
            if (result.iterator().hasNext()) {
                return result.iterator().next();
            } else {
                return null;
            }
        } else {
            LoggerFactory.getLogger(PersistenceExtensions.class)
                    .warn("There is no queryable persistence service registered with the id '{}'", serviceId);
            return null;
        }
    }

    /**
     * Checks if the state of a given <code>item</code> has changed since a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item to check for state changes
     * @param timestamp the point in time to start the check
     * @return <code>true</code> if item state has changed, <code>false</code> if it has not changed or if the default
     *         persistence service is not available or does not refer to a {@link QueryablePersistenceService}
     */
    public static Boolean changedSince(Item item, ZonedDateTime timestamp) {
        return changedSince(item, timestamp, getDefaultServiceId());
    }

    /**
     * Checks if the state of a given <code>item</code> has changed since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to check for state changes
     * @param timestamp the point in time to start the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return <code>true</code> if item state has changed, or <code>false</code> if it has not changed or if the
     *         provided <code>serviceId</code> does not refer to an available {@link QueryablePersistenceService}
     */
    public static Boolean changedSince(Item item, ZonedDateTime timestamp, String serviceId) {
        Iterable<HistoricItem> result = getAllStatesSince(item, timestamp, serviceId);
        Iterator<HistoricItem> it = result.iterator();
        HistoricItem itemThen = historicState(item, timestamp);
        if (itemThen == null) {
            // Can't get the state at the start time
            // If we've got results more recent that this, it must have changed
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

    /**
     * Checks if the state of a given <code>item</code> has been updated since a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item to check for state updates
     * @param timestamp the point in time to start the check
     * @return <code>true</code> if item state was updated, <code>false</code> if either item has not been updated since
     *         <code>timestamp</code> or if the default persistence does not refer to a
     *         {@link QueryablePersistenceService}, or <code>null</code> if the default persistence service is not
     *         available
     */
    public static Boolean updatedSince(Item item, ZonedDateTime timestamp) {
        return updatedSince(item, timestamp, getDefaultServiceId());
    }

    /**
     * Checks if the state of a given <code>item</code> has changed since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to check for state changes
     * @param timestamp the point in time to start the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return <code>true</code> if item state was updated or <code>false</code> if either the item has not been updated
     *         since <code>timestamp</code> or if the given <code>serviceId</code> does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static Boolean updatedSince(Item item, ZonedDateTime timestamp, String serviceId) {
        Iterable<HistoricItem> result = getAllStatesSince(item, timestamp, serviceId);
        if (result.iterator().hasNext()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the historic item with the maximum value of the state of a given <code>item</code> since
     * a certain point in time. The default persistence service is used.
     *
     * @param item the item to get the maximum state value for
     * @param timestamp the point in time to start the check
     * @return a historic item with the maximum state value since the given point in time, or a {@link HistoricItem}
     *         constructed from the <code>item</code> if the default persistence service does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static HistoricItem maximumSince(Item item, ZonedDateTime timestamp) {
        return maximumSince(item, timestamp, getDefaultServiceId());
    }

    /**
     * Gets the historic item with the maximum value of the state of a given <code>item</code> since
     * a certain point in time. The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the maximum state value for
     * @param timestamp the point in time to start the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return a {@link HistoricItem} with the maximum state value since the given point in time, or a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         maximum value or if the given <code>serviceId</code> does not refer to an available
     *         {@link QueryablePersistenceService}
     */
    public static HistoricItem maximumSince(final Item item, ZonedDateTime timestamp, String serviceId) {
        Iterable<HistoricItem> result = getAllStatesSince(item, timestamp, serviceId);
        Iterator<HistoricItem> it = result.iterator();
        HistoricItem maximumHistoricItem = null;
        DecimalType maximum = item.getStateAs(DecimalType.class);
        while (it.hasNext()) {
            HistoricItem historicItem = it.next();
            State state = historicItem.getState();
            if (state instanceof DecimalType) {
                DecimalType value = (DecimalType) state;
                if (maximum == null || value.compareTo(maximum) > 0) {
                    maximum = value;
                    maximumHistoricItem = historicItem;
                }
            }
        }
        if (maximumHistoricItem == null && maximum != null) {
            // the maximum state is the current one, so construct a historic item on the fly
            final DecimalType state = maximum;
            return new HistoricItem() {

                @Override
                public ZonedDateTime getTimestamp() {
                    return ZonedDateTime.now();
                }

                @Override
                public State getState() {
                    return state;
                }

                @Override
                public String getName() {
                    return item.getName();
                }
            };
        } else {
            return maximumHistoricItem;
        }
    }

    /**
     * Gets the historic item with the minimum value of the state of a given <code>item</code> since
     * a certain point in time. The default persistence service is used.
     *
     * @param item the item to get the minimum state value for
     * @param timestamp the point in time from which to search for the minimum state value
     * @return the historic item with the minimum state value since the given point in time or a {@link HistoricItem}
     *         constructed from the <code>item</code>'s state if <code>item</code>'s state is the minimum value or if
     *         the default persistence service does not refer to an available {@link QueryablePersistenceService}
     */
    public static HistoricItem minimumSince(Item item, ZonedDateTime timestamp) {
        return minimumSince(item, timestamp, getDefaultServiceId());
    }

    /**
     * Gets the historic item with the minimum value of the state of a given <code>item</code> since
     * a certain point in time. The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the minimum state value for
     * @param timestamp the point in time from which to search for the minimum state value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the historic item with the minimum state value since the given point in time, or a {@link HistoricItem}
     *         constructed from the <code>item</code>'s state if <code>item</code>'s state is the minimum value or if
     *         the given <code>serviceId</code> does not refer to an available {@link QueryablePersistenceService}
     */
    public static HistoricItem minimumSince(final Item item, ZonedDateTime timestamp, String serviceId) {
        Iterable<HistoricItem> result = getAllStatesSince(item, timestamp, serviceId);
        Iterator<HistoricItem> it = result.iterator();
        HistoricItem minimumHistoricItem = null;
        DecimalType minimum = item.getStateAs(DecimalType.class);
        while (it.hasNext()) {
            HistoricItem historicItem = it.next();
            State state = historicItem.getState();
            if (state instanceof DecimalType) {
                DecimalType value = (DecimalType) state;
                if (minimum == null || value.compareTo(minimum) < 0) {
                    minimum = value;
                    minimumHistoricItem = historicItem;
                }
            }
        }
        if (minimumHistoricItem == null && minimum != null) {
            // the minimal state is the current one, so construct a historic item on the fly
            final DecimalType state = minimum;
            return new HistoricItem() {

                @Override
                public ZonedDateTime getTimestamp() {
                    return ZonedDateTime.now();
                }

                @Override
                public State getState() {
                    return state;
                }

                @Override
                public String getName() {
                    return item.getName();
                }
            };
        } else {
            return minimumHistoricItem;
        }
    }

    /**
     * Gets the variance of the state of the given {@link Item} since a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time from which to compute the variance
     * @return the variance between now and then, or <code>null</code> if there is no default persistence service
     *         available or it is not a {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> at the given <code>timestamp</code>
     */
    public static DecimalType varianceSince(Item item, ZonedDateTime timestamp) {
        return varianceSince(item, timestamp, getDefaultServiceId());
    }

    /**
     * Gets the variance of the state of the given {@link Item} since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time from which to compute the variance
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the variance between now and then, or <code>null</code> if the persistence service given by
     *         <code>serviceId</code> is not available or it is not a {@link QueryablePersistenceService}, or if there
     *         is no persisted state for the given <code>item</code> at the given <code>timestamp</code>
     */
    public static DecimalType varianceSince(Item item, ZonedDateTime timestamp, String serviceId) {
        Iterable<HistoricItem> result = getAllStatesSince(item, timestamp, serviceId);
        Iterator<HistoricItem> it = result.iterator();
        DecimalType averageSince = internalAverageSince(item, it);

        if (averageSince != null) {
            BigDecimal average = averageSince.toBigDecimal(), sum = BigDecimal.ZERO;
            int count = 0;

            while (it.hasNext()) {
                State state = it.next().getState();
                if (state instanceof DecimalType) {
                    count++;
                    sum = sum.add(((DecimalType) state).toBigDecimal().subtract(average, MathContext.DECIMAL64).pow(2,
                            MathContext.DECIMAL64));
                }
            }

            // avoid division by zero
            if (count > 0) {
                return new DecimalType(sum.divide(BigDecimal.valueOf(count), MathContext.DECIMAL64));
            }
        }
        return null;
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} since a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time from which to compute the standard deviation
     * @return the standard deviation between now and then, or <code>null</code> if there is no default persistence
     *         service available or it is not a {@link QueryablePersistenceService}, or if there is no persisted state
     *         for the given <code>item</code> at the given <code>timestamp</code>
     */
    public static DecimalType deviationSince(Item item, ZonedDateTime timestamp) {
        return deviationSince(item, timestamp, getDefaultServiceId());
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time from which to compute the standard deviation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the standard deviation between now and then, or <code>null</code> if the persistence service given by
     *         <code>serviceId</code> it is not available or is not a {@link QueryablePersistenceService}, or if there
     *         is no persisted state for the given <code>item</code> at the given <code>timestamp</code>
     */
    public static DecimalType deviationSince(Item item, ZonedDateTime timestamp, String serviceId) {
        DecimalType varianceSince = varianceSince(item, timestamp, serviceId);

        if (varianceSince != null) {
            BigDecimal variance = varianceSince.toBigDecimal();

            // avoid ArithmeticException if variance is less than zero
            if (BigDecimal.ZERO.compareTo(variance) != 1) {
                return new DecimalType(variance.sqrt(MathContext.DECIMAL64));
            }
        }
        return null;
    }

    /**
     * Gets the average value of the state of a given {@link Item} since a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time from which to search for the average value
     * @return the average value since <code>timestamp</code> or the state of the given <code>item</code> if no
     *         previous states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}
     */
    public static DecimalType averageSince(Item item, ZonedDateTime timestamp) {
        return averageSince(item, timestamp, getDefaultServiceId());
    }

    /**
     * Gets the average value of the state of a given {@link Item} since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time from which to search for the average value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the average value since <code>timestamp</code>, or the state of the given <code>item</code> if no
     *         previous states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}
     */
    public static DecimalType averageSince(Item item, ZonedDateTime timestamp, String serviceId) {
        Iterable<HistoricItem> result = getAllStatesSince(item, timestamp, serviceId);
        Iterator<HistoricItem> it = result.iterator();
        return internalAverageSince(item, it);
    }

    private static DecimalType internalAverageSince(Item item, Iterator<HistoricItem> it) {
        BigDecimal total = BigDecimal.ZERO;

        BigDecimal avgValue, timeSpan;
        DecimalType lastState = null, thisState;
        BigDecimal firstTimestamp = null, lastTimestamp = null, thisTimestamp = null;

        while (it.hasNext()) {
            HistoricItem thisItem = it.next();
            State state = thisItem.getState();

            if (state instanceof DecimalType) {
                thisState = (DecimalType) state;
                thisTimestamp = BigDecimal.valueOf(thisItem.getTimestamp().toInstant().toEpochMilli());
                if (firstTimestamp == null || lastState == null) {
                    firstTimestamp = thisTimestamp;
                } else {
                    avgValue = thisState.toBigDecimal().add(lastState.toBigDecimal()).divide(BIG_DECIMAL_TWO,
                            MathContext.DECIMAL64);
                    timeSpan = thisTimestamp.subtract(lastTimestamp, MathContext.DECIMAL64);
                    total = total.add(avgValue.multiply(timeSpan, MathContext.DECIMAL64));
                }
                lastTimestamp = thisTimestamp;
                lastState = thisState;
            }
        }

        if (lastState != null) {
            thisState = item.getStateAs(DecimalType.class);
            if (thisState != null) {
                thisTimestamp = BigDecimal.valueOf(Instant.now().toEpochMilli());
                avgValue = thisState.toBigDecimal().add(lastState.toBigDecimal()).divide(BIG_DECIMAL_TWO,
                        MathContext.DECIMAL64);
                timeSpan = thisTimestamp.subtract(lastTimestamp, MathContext.DECIMAL64);
                total = total.add(avgValue.multiply(timeSpan, MathContext.DECIMAL64));
            }
        }

        if (thisTimestamp != null) {
            timeSpan = thisTimestamp.subtract(firstTimestamp, MathContext.DECIMAL64);
            // avoid ArithmeticException if timeSpan is zero
            if (!BigDecimal.ZERO.equals(timeSpan)) {
                BigDecimal average = total.divide(timeSpan, MathContext.DECIMAL64);
                return new DecimalType(average);
            }
        }
        return null;
    }

    /**
     * Gets the sum of the state of a given <code>item</code> since a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item for which we will sum its persisted state values since <code>timestamp</code>
     * @param timestamp the point in time from which to start the summation
     * @return the sum of the state values since <code>timestamp</code>, or {@link DecimalType.ZERO} if no historic
     *         states could be found or if the default persistence service does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static DecimalType sumSince(Item item, ZonedDateTime timestamp) {
        return sumSince(item, timestamp, getDefaultServiceId());
    }

    /**
     * Gets the sum of the state of a given <code>item</code> since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item for which we will sum its persisted state values since <code>timestamp</code>
     * @param timestamp the point in time from which to start the summation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the sum of the state values since the given point in time, or {@link DecimalType.ZERO} if no historic
     *         states could be found for the <code>item</code> or if <code>serviceId</code> does no refer to a
     *         {@link QueryablePersistenceService}
     */
    public static DecimalType sumSince(Item item, ZonedDateTime timestamp, String serviceId) {
        Iterable<HistoricItem> result = getAllStatesSince(item, timestamp, serviceId);
        Iterator<HistoricItem> it = result.iterator();

        BigDecimal sum = BigDecimal.ZERO;
        while (it.hasNext()) {
            State state = it.next().getState();
            if (state instanceof DecimalType) {
                sum = sum.add(((DecimalType) state).toBigDecimal());
            }
        }

        return new DecimalType(sum);
    }

    private static Iterable<HistoricItem> getAllStatesSince(Item item, ZonedDateTime timestamp, String serviceId) {
        PersistenceService service = getService(serviceId);
        if (service instanceof QueryablePersistenceService) {
            QueryablePersistenceService qService = (QueryablePersistenceService) service;
            FilterCriteria filter = new FilterCriteria();
            filter.setBeginDate(timestamp);
            filter.setItemName(item.getName());
            filter.setOrdering(Ordering.ASCENDING);
            return qService.query(filter);
        } else {
            LoggerFactory.getLogger(PersistenceExtensions.class)
                    .warn("There is no queryable persistence service registered with the id '{}'", serviceId);
            return Collections.emptySet();
        }
    }

    /**
     * Query the last update time of a given <code>item</code>. The default persistence service is used.
     *
     * @param item the item for which the last update time is to be returned
     * @return point in time of the last update to <code>item</code>, or <code>null</code> if there are no previously
     *         persisted updates or the default persistence service is not available or a
     *         {@link QueryablePersistenceService}
     */
    public static ZonedDateTime lastUpdate(Item item) {
        return lastUpdate(item, getDefaultServiceId());
    }

    /**
     * Query for the last update time of a given <code>item</code>.
     *
     * @param item the item for which the last update time is to be returned
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return last time <code>item</code> was updated, or <code>null</code> if there are no previously
     *         persisted updates or if persistence service given by <code>serviceId</code> does not refer to an
     *         available {@link QueryablePersistenceService}
     */
    public static ZonedDateTime lastUpdate(Item item, String serviceId) {
        PersistenceService service = getService(serviceId);
        if (service instanceof QueryablePersistenceService) {
            QueryablePersistenceService qService = (QueryablePersistenceService) service;
            FilterCriteria filter = new FilterCriteria();
            filter.setItemName(item.getName());
            filter.setOrdering(Ordering.DESCENDING);
            filter.setPageSize(1);
            Iterable<HistoricItem> result = qService.query(filter);
            if (result.iterator().hasNext()) {
                return result.iterator().next().getTimestamp();
            } else {
                return null;
            }
        } else {
            LoggerFactory.getLogger(PersistenceExtensions.class)
                    .warn("There is no queryable persistence service registered with the id '{}'", serviceId);
            return null;
        }
    }

    /**
     * Gets the difference value of the state of a given <code>item</code> since a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item to get the average state value for
     * @param timestamp the point in time from which to compute the delta
     * @return the difference between now and then, or <code>null</code> if there is no default persistence
     *         service available, the default persistence service is not a {@link QueryablePersistenceService}, or if
     *         there is no persisted state for the given <code>item</code> at the given <code>timestamp</code> available
     *         in the default persistence service
     */
    public static DecimalType deltaSince(Item item, ZonedDateTime timestamp) {
        return deltaSince(item, timestamp, getDefaultServiceId());
    }

    /**
     * Gets the difference value of the state of a given <code>item</code> since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the average state value for
     * @param timestamp the point in time from which to compute the delta
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the difference between now and then, or <code>null</code> if the given serviceId does not refer to an
     *         available {@link QueryablePersistenceService}, or if there is no persisted state for the given
     *         <code>item</code> at the given <code>timestamp</code> using the persistence service named
     *         <code>serviceId</code>
     */
    public static DecimalType deltaSince(Item item, ZonedDateTime timestamp, String serviceId) {
        HistoricItem itemThen = historicState(item, timestamp, serviceId);
        if (itemThen != null) {
            DecimalType valueThen = (DecimalType) itemThen.getState();
            DecimalType valueNow = item.getStateAs(DecimalType.class);

            if (valueNow != null) {
                return new DecimalType(valueNow.toBigDecimal().subtract(valueThen.toBigDecimal()));
            }
        }
        return null;
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} since a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the item to get the evolution rate value for
     * @param timestamp the point in time from which to compute the evolution rate
     * @return the evolution rate in percent (positive and negative) between now and then, or <code>null</code> if
     *         there is no default persistence service available, the default persistence service is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>, or if there is a state but it is zero (which would cause a
     *         divide-by-zero error)
     */
    public static DecimalType evolutionRate(Item item, ZonedDateTime timestamp) {
        return evolutionRate(item, timestamp, getDefaultServiceId());
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
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
    public static DecimalType evolutionRate(Item item, ZonedDateTime timestamp, String serviceId) {
        HistoricItem itemThen = historicState(item, timestamp, serviceId);
        if (itemThen != null) {
            DecimalType valueThen = (DecimalType) itemThen.getState();
            DecimalType valueNow = item.getStateAs(DecimalType.class);

            if ((valueThen.toBigDecimal().compareTo(BigDecimal.ZERO) != 0) && (valueNow != null)) {
                // ((now - then) / then) * 100
                return new DecimalType(valueNow.toBigDecimal().subtract(valueThen.toBigDecimal())
                        .divide(valueThen.toBigDecimal(), MathContext.DECIMAL64).movePointRight(2));
            }
        }
        return null;
    }

    /**
     * Returns the previous state of a given <code>item</code>.
     *
     * @param item the item to get the previous state value for
     * @return the previous state or <code>null</code> if no previous state could be found, or if the default
     *         persistence service is not configured or does not refer to a {@link QueryablePersistenceService}
     */
    public static HistoricItem previousState(Item item) {
        return previousState(item, false);
    }

    /**
     * Returns the previous state of a given <code>item</code>.
     *
     * @param item the item to get the previous state value for
     * @param skipEqual if true, skips equal state values and searches the first state not equal the current state
     * @return the previous state or <code>null</code> if no previous state could be found, or if the default
     *         persistence service is not configured or does not refer to a {@link QueryablePersistenceService}
     */
    public static HistoricItem previousState(Item item, boolean skipEqual) {
        return previousState(item, skipEqual, getDefaultServiceId());
    }

    /**
     * Returns the previous state of a given <code>item</code>.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the previous state value for
     * @param skipEqual if <code>true</code>, skips equal state values and searches the first state not equal the
     *            current state
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the previous state or <code>null</code> if no previous state could be found, or if the given
     *         <code>serviceId</code> is not available or does not refer to a {@link QueryablePersistenceService}
     */
    public static HistoricItem previousState(Item item, boolean skipEqual, String serviceId) {
        PersistenceService service = getService(serviceId);
        if (service instanceof QueryablePersistenceService) {
            QueryablePersistenceService qService = (QueryablePersistenceService) service;
            FilterCriteria filter = new FilterCriteria();
            filter.setItemName(item.getName());
            filter.setOrdering(Ordering.DESCENDING);

            filter.setPageSize(skipEqual ? 1000 : 1);
            int startPage = 0;
            filter.setPageNumber(startPage);

            Iterable<HistoricItem> items = qService.query(filter);
            while (items != null) {
                Iterator<HistoricItem> itemIterator = items.iterator();
                int itemCount = 0;
                while (itemIterator.hasNext()) {
                    HistoricItem historicItem = itemIterator.next();
                    itemCount++;
                    if (!skipEqual || (skipEqual && !historicItem.getState().equals(item.getState()))) {
                        return historicItem;
                    }
                }
                if (itemCount == filter.getPageSize()) {
                    filter.setPageNumber(++startPage);
                    items = qService.query(filter);
                } else {
                    items = null;
                }
            }
            return null;
        } else {
            LoggerFactory.getLogger(PersistenceExtensions.class)
                    .warn("There is no queryable persistence service registered with the id '{}'", serviceId);
            return null;
        }
    }
}
