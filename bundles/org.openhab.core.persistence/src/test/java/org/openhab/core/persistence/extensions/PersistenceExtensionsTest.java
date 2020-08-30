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

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.GenericItem;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.PersistenceServiceRegistry;

/**
 * @author Kai Kreuzer - Initial contribution
 * @author Chris Jackson - Initial contribution
 * @author Jan N. Klug - Fix averageSince calculation
 */
public class PersistenceExtensionsTest {

    private final PersistenceServiceRegistry registry = new PersistenceServiceRegistry() {

        private final PersistenceService testPersistenceService = new TestPersistenceService();

        @Override
        public String getDefaultId() {
            // not available
            return null;
        }

        @Override
        public PersistenceService getDefault() {
            // not available
            return null;
        }

        @Override
        public Set<PersistenceService> getAll() {
            return Set.of(testPersistenceService);
        }

        @Override
        public PersistenceService get(String serviceId) {
            return testPersistenceService;
        }
    };

    private CoreItemFactory itemFactory;
    private GenericItem item;

    @BeforeEach
    public void setUp() {
        new PersistenceExtensions(registry);
        itemFactory = new CoreItemFactory();
        item = itemFactory.createItem(CoreItemFactory.NUMBER, "Test");
        assertEquals(CoreItemFactory.NUMBER, item.getType());
    }

    @Test
    public void testHistoricState() {
        HistoricItem historicItem = PersistenceExtensions.historicState(item,
                ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2012", historicItem.getState().toString());

        historicItem = PersistenceExtensions.historicState(item,
                ZonedDateTime.of(2011, 12, 31, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2011", historicItem.getState().toString());

        historicItem = PersistenceExtensions.historicState(item,
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2011", historicItem.getState().toString());

        historicItem = PersistenceExtensions.historicState(item,
                ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2000", historicItem.getState().toString());

        // default persistence service
        historicItem = PersistenceExtensions.historicState(item,
                ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);
    }

    @Test
    public void testMaximumSince() {
        item.setState(new QuantityType<>(1, SIUnits.CELSIUS));
        HistoricItem historicItem = PersistenceExtensions.maximumSince(item,
                ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("1", historicItem.getState().toString());

        item.setState(new DecimalType(1));
        historicItem = PersistenceExtensions.maximumSince(item,
                ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("1", historicItem.getState().toString());

        historicItem = PersistenceExtensions.maximumSince(item,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2012", historicItem.getState().toString());
        assertEquals(ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), historicItem.getTimestamp());

        // default persistence service
        historicItem = PersistenceExtensions.maximumSince(item,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNotNull(historicItem);
        assertEquals("1", historicItem.getState().toString());
    }

    @Test
    public void testMinimumSince() {
        item.setState(new QuantityType<>(5000, SIUnits.CELSIUS));
        HistoricItem historicItem = PersistenceExtensions.minimumSince(item,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("5000", historicItem.getState().toString());

        item.setState(new DecimalType(5000));
        historicItem = PersistenceExtensions.minimumSince(item,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("5000", historicItem.getState().toString());

        historicItem = PersistenceExtensions.minimumSince(item,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2005", historicItem.getState().toString());
        assertEquals(ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), historicItem.getTimestamp());

        // default persistence service
        historicItem = PersistenceExtensions.minimumSince(item,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNotNull(historicItem);
        assertEquals("5000", historicItem.getState().toString());
    }

    public void testVarianceSince() {
        item.setState(new DecimalType(3025));

        ZonedDateTime startStored = ZonedDateTime.of(2003, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        long storedInterval = endStored.toInstant().toEpochMilli() - startStored.toInstant().toEpochMilli();
        long recentInterval = Instant.now().toEpochMilli() - endStored.toInstant().toEpochMilli();
        double expectedAverage = (2007.4994 * storedInterval + 2518.5 * recentInterval)
                / (storedInterval + recentInterval);
        double expected = IntStream.of(2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012)
                .mapToDouble(i -> Double.parseDouble(Integer.toString(i))).map(d -> Math.pow(d - expectedAverage, 2))
                .sum() / 10d;
        DecimalType variance = PersistenceExtensions.varianceSince(item, startStored, TestPersistenceService.ID);
        assertNotNull(variance);
        assertEquals(expected, variance.doubleValue(), 0.01);

        item.setState(new QuantityType<>(3025, SIUnits.CELSIUS));
        variance = PersistenceExtensions.varianceSince(item, startStored, TestPersistenceService.ID);
        assertNotNull(variance);
        assertEquals(expected, variance.doubleValue(), 0.01);

        // default persistence service
        variance = PersistenceExtensions.varianceSince(item, startStored);
        assertNull(variance);
    }

    public void testDeviationSince() {
        item.setState(new DecimalType(3025));

        ZonedDateTime startStored = ZonedDateTime.of(2003, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        long storedInterval = endStored.toInstant().toEpochMilli() - startStored.toInstant().toEpochMilli();
        long recentInterval = Instant.now().toEpochMilli() - endStored.toInstant().toEpochMilli();
        double expectedAverage = (2007.4994 * storedInterval + 2518.5 * recentInterval)
                / (storedInterval + recentInterval);
        double expected = Math.sqrt(IntStream.of(2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012)
                .mapToDouble(i -> Double.parseDouble(Integer.toString(i))).map(d -> Math.pow(d - expectedAverage, 2))
                .sum() / 10d);
        DecimalType deviation = PersistenceExtensions.deviationSince(item, startStored, TestPersistenceService.ID);
        assertNotNull(deviation);
        assertEquals(expected, deviation.doubleValue(), 0.01);

        item.setState(new QuantityType<>(3025, SIUnits.CELSIUS));
        deviation = PersistenceExtensions.deviationSince(item, startStored, TestPersistenceService.ID);
        assertNotNull(deviation);
        assertEquals(expected, deviation.doubleValue(), 0.01);

        // default persistence service
        deviation = PersistenceExtensions.deviationSince(item, startStored);
        assertNull(deviation);
    }

    @Test
    public void testAverageSince() {
        item.setState(new DecimalType(3025));

        ZonedDateTime startStored = ZonedDateTime.of(2003, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        long storedInterval = endStored.toInstant().toEpochMilli() - startStored.toInstant().toEpochMilli();
        long recentInterval = Instant.now().toEpochMilli() - endStored.toInstant().toEpochMilli();
        double expected = (2007.4994 * storedInterval + 2518.5 * recentInterval) / (storedInterval + recentInterval);
        DecimalType average = PersistenceExtensions.averageSince(item, startStored, TestPersistenceService.ID);
        assertNotNull(average);
        assertEquals(expected, average.doubleValue(), 0.01);

        item.setState(new QuantityType<>(3025, SIUnits.CELSIUS));
        average = PersistenceExtensions.averageSince(item, startStored, TestPersistenceService.ID);
        assertNotNull(average);
        assertEquals(expected, average.doubleValue(), 0.01);

        // default persistence service
        average = PersistenceExtensions.averageSince(item, startStored);
        assertNull(average);
    }

    @Test
    public void testSumSince() {
        DecimalType sum = PersistenceExtensions.sumSince(item,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(sum);
        assertEquals(0.0, sum.doubleValue(), 0.001);

        sum = PersistenceExtensions.sumSince(item, ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                TestPersistenceService.ID);
        assertNotNull(sum);
        assertEquals(IntStream.of(2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012).sum(), sum.doubleValue(), 0.001);

        // default persistence service
        sum = PersistenceExtensions.sumSince(item, ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNotNull(sum);
        assertEquals(0.0, sum.doubleValue(), 0.001);
    }

    @Test
    public void testLastUpdate() {
        item.setState(new DecimalType(2005));
        ZonedDateTime lastUpdate = PersistenceExtensions.lastUpdate(item, TestPersistenceService.ID);
        assertNotNull(lastUpdate);
        assertEquals(ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), lastUpdate);

        // default persistence service
        lastUpdate = PersistenceExtensions.lastUpdate(item);
        assertNull(lastUpdate);
    }

    @Test
    public void testDeltaSince() {
        item.setState(new DecimalType(2012));
        DecimalType delta = PersistenceExtensions.deltaSince(item,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNull(delta);

        delta = PersistenceExtensions.deltaSince(item, ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                TestPersistenceService.ID);
        assertNotNull(delta);
        assertEquals(7, delta.doubleValue(), 0.001);

        item.setState(new QuantityType<>(2012, SIUnits.CELSIUS));
        delta = PersistenceExtensions.deltaSince(item, ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                TestPersistenceService.ID);
        assertNotNull(delta);
        assertEquals(7, delta.doubleValue(), 0.001);

        // default persistence service
        delta = PersistenceExtensions.deltaSince(item,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(delta);
    }

    @Test
    public void testEvolutionRate() {
        item.setState(new DecimalType(2012));
        DecimalType rate = PersistenceExtensions.evolutionRate(item,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNull(rate);

        rate = PersistenceExtensions.evolutionRate(item,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(rate);
        // ((now - then) / then) * 100
        assertEquals(0.349127182, rate.doubleValue(), 0.001);

        item.setState(new QuantityType<>(2012, SIUnits.CELSIUS));
        rate = PersistenceExtensions.evolutionRate(item,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(rate);
        // ((now - then) / then) * 100
        assertEquals(0.349127182, rate.doubleValue(), 0.001);

        // default persistence service
        rate = PersistenceExtensions.evolutionRate(item,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(rate);
    }

    @Test
    public void testPreviousStateNoSkip() {
        item.setState(new DecimalType(4321));
        HistoricItem prevStateItem = PersistenceExtensions.previousState(item, false, TestPersistenceService.ID);
        assertNotNull(prevStateItem);
        assertEquals("2012", prevStateItem.getState().toString());

        item.setState(new DecimalType(2012));
        prevStateItem = PersistenceExtensions.previousState(item, false, TestPersistenceService.ID);
        assertNotNull(prevStateItem);
        assertEquals("2012", prevStateItem.getState().toString());

        // default persistence service
        prevStateItem = PersistenceExtensions.previousState(item, false);
        assertNull(prevStateItem);
    }

    @Test
    public void testPreviousStateSkip() {
        item.setState(new DecimalType(2012));
        HistoricItem prevStateItem = PersistenceExtensions.previousState(item, true, TestPersistenceService.ID);
        assertNotNull(prevStateItem);
        assertEquals("2011", prevStateItem.getState().toString());

        // default persistence service
        prevStateItem = PersistenceExtensions.previousState(item, true);
        assertNull(prevStateItem);
    }

    @Test
    public void testChangedSince() {
        boolean changed = PersistenceExtensions.changedSince(item,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertFalse(changed);

        changed = PersistenceExtensions.changedSince(item,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertTrue(changed);

        // default persistence service
        changed = PersistenceExtensions.changedSince(item,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertFalse(changed);
    }

    @Test
    public void testUpdatedSince() {
        boolean updated = PersistenceExtensions.updatedSince(item,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertFalse(updated);

        updated = PersistenceExtensions.updatedSince(item,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertTrue(updated);

        // default persistence service
        updated = PersistenceExtensions.updatedSince(item,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertFalse(updated);
    }
}
