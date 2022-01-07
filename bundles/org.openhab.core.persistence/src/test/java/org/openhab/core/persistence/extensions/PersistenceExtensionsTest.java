/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.IntStream;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
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
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class PersistenceExtensionsTest {

    public static final String TEST_NUMBER = "testNumber";
    public static final String TEST_QUANTITY_NUMBER = "testQuantityNumber";
    public static final String TEST_SWITCH = "testSwitch";

    private @Mock UnitProvider unitProvider;
    private @Mock ItemRegistry mockedItemRegistry;

    private GenericItem numberItem, quantityItem, switchItem;

    @BeforeEach
    public void setUp() {
        when(unitProvider.getUnit(Temperature.class)).thenReturn(SIUnits.CELSIUS);

        CoreItemFactory itemFactory = new CoreItemFactory();
        numberItem = itemFactory.createItem(CoreItemFactory.NUMBER, TEST_NUMBER);
        quantityItem = itemFactory.createItem(CoreItemFactory.NUMBER + ItemUtil.EXTENSION_SEPARATOR + "Temperature",
                TEST_QUANTITY_NUMBER);
        quantityItem.setUnitProvider(unitProvider);
        switchItem = itemFactory.createItem(CoreItemFactory.SWITCH, TEST_SWITCH);

        when(mockedItemRegistry.get(TEST_NUMBER)).thenReturn(numberItem);
        when(mockedItemRegistry.get(TEST_QUANTITY_NUMBER)).thenReturn(quantityItem);
        when(mockedItemRegistry.get(TEST_SWITCH)).thenReturn(switchItem);

        new PersistenceExtensions(new PersistenceServiceRegistry() {
            private final PersistenceService testPersistenceService = new TestPersistenceService(mockedItemRegistry);

            @Override
            public @Nullable String getDefaultId() {
                // not available
                return null;
            }

            @Override
            public @Nullable PersistenceService getDefault() {
                // not available
                return null;
            }

            @Override
            public Set<PersistenceService> getAll() {
                return Set.of(testPersistenceService);
            }

            @Override
            public @Nullable PersistenceService get(@Nullable String serviceId) {
                return TestPersistenceService.ID.equals(serviceId) ? testPersistenceService : null;
            }
        });
    }

    @Test
    public void testHistoricStateDecimalType() {
        HistoricItem historicItem = PersistenceExtensions.historicState(numberItem,
                ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertEquals("2012", historicItem.getState().toString());

        historicItem = PersistenceExtensions.historicState(numberItem,
                ZonedDateTime.of(2011, 12, 31, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2011", historicItem.getState().toString());

        historicItem = PersistenceExtensions.historicState(numberItem,
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2011", historicItem.getState().toString());

        historicItem = PersistenceExtensions.historicState(numberItem,
                ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2000", historicItem.getState().toString());

        // default persistence service
        historicItem = PersistenceExtensions.historicState(numberItem,
                ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);
    }

    @Test
    public void testHistoricStateQuantityType() {
        HistoricItem historicItem = PersistenceExtensions.historicState(quantityItem,
                ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertEquals("2012 °C", historicItem.getState().toString());

        historicItem = PersistenceExtensions.historicState(quantityItem,
                ZonedDateTime.of(2011, 12, 31, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2011 °C", historicItem.getState().toString());

        historicItem = PersistenceExtensions.historicState(quantityItem,
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2011 °C", historicItem.getState().toString());

        historicItem = PersistenceExtensions.historicState(quantityItem,
                ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2000 °C", historicItem.getState().toString());

        // default persistence service
        historicItem = PersistenceExtensions.historicState(quantityItem,
                ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);
    }

    @Test
    public void testHistoricSwitchState() {
        ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
        HistoricItem historicItem = PersistenceExtensions.historicState(switchItem, now.minusHours(15),
                TestPersistenceService.ID);
        assertNull(historicItem);

        historicItem = PersistenceExtensions.historicState(switchItem, now.minusHours(14), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals(OnOffType.ON, historicItem.getState());

        historicItem = PersistenceExtensions.historicState(switchItem, now.minusHours(4), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals(OnOffType.OFF, historicItem.getState());
    }

    @Test
    public void testMaximumSinceDecimalType() {
        numberItem.setState(new DecimalType(1));
        HistoricItem historicItem = PersistenceExtensions.maximumSince(numberItem,
                ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertEquals("1", historicItem.getState().toString());

        historicItem = PersistenceExtensions.maximumSince(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2012", historicItem.getState().toString());
        assertEquals(ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), historicItem.getTimestamp());

        // default persistence service
        historicItem = PersistenceExtensions.maximumSince(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNotNull(historicItem);
        assertEquals("1", historicItem.getState().toString());
    }

    @Test
    public void testMaximumSinceQuantityType() {
        quantityItem.setState(QuantityType.valueOf(1, SIUnits.CELSIUS));
        HistoricItem historicItem = PersistenceExtensions.maximumSince(quantityItem,
                ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertEquals("1 °C", historicItem.getState().toString());

        historicItem = PersistenceExtensions.maximumSince(quantityItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2012 °C", historicItem.getState().toString());
        assertEquals(ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), historicItem.getTimestamp());

        // default persistence service
        historicItem = PersistenceExtensions.maximumSince(quantityItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNotNull(historicItem);
        assertEquals("1 °C", historicItem.getState().toString());
    }

    @Test
    public void testMaximumSinceSwitch() {
        switchItem.setState(OnOffType.OFF);

        ZonedDateTime now = ZonedDateTime.now();
        HistoricItem historicItem = PersistenceExtensions.maximumSince(switchItem, now.minusHours(15),
                TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals(OnOffType.ON, historicItem.getState());

        historicItem = PersistenceExtensions.maximumSince(switchItem, now.minusHours(6), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals(OnOffType.ON, historicItem.getState());

        historicItem = PersistenceExtensions.maximumSince(switchItem, now.minusHours(1), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals(OnOffType.ON, historicItem.getState());

        historicItem = PersistenceExtensions.maximumSince(switchItem, now, TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals(OnOffType.OFF, historicItem.getState());

        historicItem = PersistenceExtensions.maximumSince(switchItem, now.plusHours(1), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals(OnOffType.OFF, historicItem.getState());
    }

    @Test
    public void testMinimumSinceDecimalType() {
        numberItem.setState(new DecimalType(5000));
        HistoricItem historicItem = PersistenceExtensions.minimumSince(numberItem,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertEquals("5000", historicItem.getState().toString());

        historicItem = PersistenceExtensions.minimumSince(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2005", historicItem.getState().toString());
        assertEquals(ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), historicItem.getTimestamp());

        // default persistence service
        historicItem = PersistenceExtensions.minimumSince(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNotNull(historicItem);
        assertEquals("5000", historicItem.getState().toString());
    }

    @Test
    public void testMinimumSinceQuantityType() {
        quantityItem.setState(QuantityType.valueOf(5000, SIUnits.CELSIUS));
        HistoricItem historicItem = PersistenceExtensions.minimumSince(quantityItem,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertEquals("5000 °C", historicItem.getState().toString());

        historicItem = PersistenceExtensions.minimumSince(quantityItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals("2005 °C", historicItem.getState().toString());
        assertEquals(ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), historicItem.getTimestamp());

        // default persistence service
        historicItem = PersistenceExtensions.minimumSince(quantityItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNotNull(historicItem);
        assertEquals("5000 °C", historicItem.getState().toString());
    }

    @Test
    public void testMinimumSinceSwitch() {
        switchItem.setState(OnOffType.ON);

        ZonedDateTime now = ZonedDateTime.now();
        HistoricItem historicItem = PersistenceExtensions.minimumSince(switchItem, now.minusHours(15),
                TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals(OnOffType.OFF, historicItem.getState());

        historicItem = PersistenceExtensions.minimumSince(switchItem, now.minusHours(6), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals(OnOffType.OFF, historicItem.getState());

        historicItem = PersistenceExtensions.minimumSince(switchItem, now.minusHours(1), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals(OnOffType.ON, historicItem.getState());

        historicItem = PersistenceExtensions.minimumSince(switchItem, now, TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals(OnOffType.ON, historicItem.getState());

        historicItem = PersistenceExtensions.minimumSince(switchItem, now.plusHours(1), TestPersistenceService.ID);
        assertNotNull(historicItem);
        assertEquals(OnOffType.ON, historicItem.getState());
    }

    @Test
    public void testVarianceSince() {
        numberItem.setState(new DecimalType(3025));

        ZonedDateTime startStored = ZonedDateTime.of(2003, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        long storedInterval = endStored.toInstant().toEpochMilli() - startStored.toInstant().toEpochMilli();
        long recentInterval = Instant.now().toEpochMilli() - endStored.toInstant().toEpochMilli();
        double expectedAverage = (2007.4994 * storedInterval + 2518.5 * recentInterval)
                / (storedInterval + recentInterval);
        double expected = IntStream.of(2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012)
                .mapToDouble(i -> Double.parseDouble(Integer.toString(i))).map(d -> Math.pow(d - expectedAverage, 2))
                .sum() / 10d;
        DecimalType variance = PersistenceExtensions.varianceSince(numberItem, startStored, TestPersistenceService.ID);
        assertNotNull(variance);
        assertEquals(expected, variance.doubleValue(), 0.01);

        // default persistence service
        variance = PersistenceExtensions.varianceSince(numberItem, startStored);
        assertNull(variance);
    }

    @Test
    public void testDeviationSinceDecimalType() {
        numberItem.setState(new DecimalType(3025));

        ZonedDateTime startStored = ZonedDateTime.of(2003, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        long storedInterval = endStored.toInstant().toEpochMilli() - startStored.toInstant().toEpochMilli();
        long recentInterval = Instant.now().toEpochMilli() - endStored.toInstant().toEpochMilli();
        double expectedAverage = (2007.4994 * storedInterval + 2518.5 * recentInterval)
                / (storedInterval + recentInterval);
        double expected = Math.sqrt(IntStream.of(2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012)
                .mapToDouble(i -> Double.parseDouble(Integer.toString(i))).map(d -> Math.pow(d - expectedAverage, 2))
                .sum() / 10d);
        DecimalType deviation = PersistenceExtensions.deviationSince(numberItem, startStored,
                TestPersistenceService.ID);
        assertNotNull(deviation);
        assertEquals(expected, deviation.doubleValue(), 0.01);

        // default persistence service
        deviation = PersistenceExtensions.deviationSince(numberItem, startStored);
        assertNull(deviation);
    }

    @Test
    public void testDeviationSinceQuantityType() {
        quantityItem.setState(QuantityType.valueOf(3025, SIUnits.CELSIUS));

        ZonedDateTime startStored = ZonedDateTime.of(2003, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        long storedInterval = endStored.toInstant().toEpochMilli() - startStored.toInstant().toEpochMilli();
        long recentInterval = Instant.now().toEpochMilli() - endStored.toInstant().toEpochMilli();
        double expectedAverage = (2007.4994 * storedInterval + 2518.5 * recentInterval)
                / (storedInterval + recentInterval);
        double expected = Math.sqrt(IntStream.of(2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012)
                .mapToDouble(i -> Double.parseDouble(Integer.toString(i))).map(d -> Math.pow(d - expectedAverage, 2))
                .sum() / 10d);
        DecimalType deviation = PersistenceExtensions.deviationSince(quantityItem, startStored,
                TestPersistenceService.ID);
        assertNotNull(deviation);
        assertEquals(expected, deviation.doubleValue(), 0.01);

        // default persistence service
        deviation = PersistenceExtensions.deviationSince(quantityItem, startStored);
        assertNull(deviation);
    }

    @Test
    public void testAverageSinceDecimalType() {
        numberItem.setState(new DecimalType(3025));

        ZonedDateTime startStored = ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        DecimalType average = PersistenceExtensions.averageSince(numberItem, startStored, TestPersistenceService.ID);
        assertNull(average);

        startStored = ZonedDateTime.of(2003, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        Instant endStored = ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant();
        long storedInterval = endStored.toEpochMilli() - startStored.toInstant().toEpochMilli();
        long recentInterval = Instant.now().toEpochMilli() - endStored.toEpochMilli();
        double expected = (2007.4994 * storedInterval + 2518.5 * recentInterval) / (storedInterval + recentInterval);
        average = PersistenceExtensions.averageSince(numberItem, startStored, TestPersistenceService.ID);
        assertNotNull(average);
        assertEquals(expected, average.doubleValue(), 0.01);

        // default persistence service
        average = PersistenceExtensions.averageSince(numberItem, startStored);
        assertNull(average);
    }

    @Test
    public void testAverageSinceQuantityType() {
        quantityItem.setState(QuantityType.valueOf(3025, SIUnits.CELSIUS));

        ZonedDateTime startStored = ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        DecimalType average = PersistenceExtensions.averageSince(quantityItem, startStored, TestPersistenceService.ID);
        assertNull(average);

        startStored = ZonedDateTime.of(2003, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        Instant endStored = ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant();
        long storedInterval = endStored.toEpochMilli() - startStored.toInstant().toEpochMilli();
        long recentInterval = Instant.now().toEpochMilli() - endStored.toEpochMilli();
        double expected = (2007.4994 * storedInterval + 2518.5 * recentInterval) / (storedInterval + recentInterval);
        average = PersistenceExtensions.averageSince(quantityItem, startStored, TestPersistenceService.ID);
        assertNotNull(average);
        assertEquals(expected, average.doubleValue(), 0.01);

        // default persistence service
        average = PersistenceExtensions.averageSince(quantityItem, startStored);
        assertNull(average);
    }

    @Test
    public void testAverageSinceSwitch() {
        switchItem.setState(OnOffType.ON);

        ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        DecimalType average = PersistenceExtensions.averageSince(switchItem, now.minusHours(15),
                TestPersistenceService.ID);
        assertNotNull(average);
        assertEquals(0.625, average.doubleValue(), 0.04);

        average = PersistenceExtensions.averageSince(switchItem, now.minusHours(7), TestPersistenceService.ID);
        assertNotNull(average);
        assertEquals(0.714, average.doubleValue(), 0.1);

        average = PersistenceExtensions.averageSince(switchItem, now.minusHours(6), TestPersistenceService.ID);
        assertNotNull(average);
        assertEquals(0.833, average.doubleValue(), 0.2);

        average = PersistenceExtensions.averageSince(switchItem, now.minusHours(5), TestPersistenceService.ID);
        assertNotNull(average);
        assertEquals(1d, average.doubleValue(), 0.2);

        average = PersistenceExtensions.averageSince(switchItem, now.minusHours(1), TestPersistenceService.ID);
        assertNotNull(average);
        assertEquals(1d, average.doubleValue(), 0.001);

        average = PersistenceExtensions.averageSince(switchItem, now, TestPersistenceService.ID);
        assertEquals(1d, average.doubleValue(), 0.001);

        average = PersistenceExtensions.averageSince(switchItem, now.plusHours(1), TestPersistenceService.ID);
        assertNull(average);
    }

    @Test
    public void testSumSinceDecimalType() {
        DecimalType sum = PersistenceExtensions.sumSince(numberItem,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(sum);
        assertEquals(0.0, sum.doubleValue(), 0.001);

        sum = PersistenceExtensions.sumSince(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(sum);
        assertEquals(IntStream.of(2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012).sum(), sum.doubleValue(), 0.001);

        // default persistence service
        sum = PersistenceExtensions.sumSince(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNotNull(sum);
        assertEquals(0.0, sum.doubleValue(), 0.001);
    }

    @Test
    public void testSumSinceQuantityType() {
        DecimalType sum = PersistenceExtensions.sumSince(quantityItem,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(sum);
        assertEquals(0.0, sum.doubleValue(), 0.001);

        sum = PersistenceExtensions.sumSince(quantityItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(sum);
        assertEquals(IntStream.of(2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012).sum(), sum.doubleValue(), 0.001);

        // default persistence service
        sum = PersistenceExtensions.sumSince(quantityItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNotNull(sum);
        assertEquals(0.0, sum.doubleValue(), 0.001);
    }

    @Test
    public void testLastUpdate() {
        numberItem.setState(new DecimalType(2005));
        ZonedDateTime lastUpdate = PersistenceExtensions.lastUpdate(numberItem, TestPersistenceService.ID);
        assertNotNull(lastUpdate);
        assertEquals(ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), lastUpdate);

        // default persistence service
        lastUpdate = PersistenceExtensions.lastUpdate(numberItem);
        assertNull(lastUpdate);
    }

    @Test
    public void testDeltaSince() {
        numberItem.setState(new DecimalType(2012));
        DecimalType delta = PersistenceExtensions.deltaSince(numberItem,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNull(delta);

        delta = PersistenceExtensions.deltaSince(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(delta);
        assertEquals(7, delta.doubleValue(), 0.001);

        numberItem.setState(new QuantityType<>(2012, SIUnits.CELSIUS));
        delta = PersistenceExtensions.deltaSince(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(delta);
        assertEquals(7, delta.doubleValue(), 0.001);

        // default persistence service
        delta = PersistenceExtensions.deltaSince(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(delta);
    }

    @Test
    public void testEvolutionRate() {
        numberItem.setState(new DecimalType(2012));
        DecimalType rate = PersistenceExtensions.evolutionRate(numberItem,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNull(rate);

        rate = PersistenceExtensions.evolutionRate(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(rate);
        // ((now - then) / then) * 100
        assertEquals(0.349127182, rate.doubleValue(), 0.001);

        numberItem.setState(new QuantityType<>(2012, SIUnits.CELSIUS));
        rate = PersistenceExtensions.evolutionRate(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertNotNull(rate);
        // ((now - then) / then) * 100
        assertEquals(0.349127182, rate.doubleValue(), 0.001);

        // default persistence service
        rate = PersistenceExtensions.evolutionRate(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(rate);
    }

    @Test
    public void testPreviousStateDecimalTypeNoSkip() {
        HistoricItem prevStateItem = PersistenceExtensions.previousState(numberItem, false, TestPersistenceService.ID);
        assertNotNull(prevStateItem);
        assertThat(prevStateItem.getState(), is(instanceOf(DecimalType.class)));
        assertEquals("2012", prevStateItem.getState().toString());

        numberItem.setState(new DecimalType(4321));
        prevStateItem = PersistenceExtensions.previousState(numberItem, false, TestPersistenceService.ID);
        assertNotNull(prevStateItem);
        assertEquals("2012", prevStateItem.getState().toString());

        numberItem.setState(new DecimalType(2012));
        prevStateItem = PersistenceExtensions.previousState(numberItem, false, TestPersistenceService.ID);
        assertNotNull(prevStateItem);
        assertEquals("2012", prevStateItem.getState().toString());

        numberItem.setState(new DecimalType(3025));
        prevStateItem = PersistenceExtensions.previousState(numberItem, false, TestPersistenceService.ID);
        assertNotNull(prevStateItem);
        assertEquals("2012", prevStateItem.getState().toString());

        // default persistence service
        prevStateItem = PersistenceExtensions.previousState(numberItem, false);
        assertNull(prevStateItem);
    }

    @Test
    public void testPreviousStateQuantityTypeNoSkip() {
        HistoricItem prevStateItem = PersistenceExtensions.previousState(quantityItem, false,
                TestPersistenceService.ID);
        assertNotNull(prevStateItem);
        assertThat(prevStateItem.getState(), is(instanceOf(QuantityType.class)));
        assertEquals("2012 °C", prevStateItem.getState().toString());

        quantityItem.setState(QuantityType.valueOf(4321, SIUnits.CELSIUS));
        prevStateItem = PersistenceExtensions.previousState(quantityItem, false, TestPersistenceService.ID);
        assertNotNull(prevStateItem);
        assertEquals("2012 °C", prevStateItem.getState().toString());

        quantityItem.setState(QuantityType.valueOf(2012, SIUnits.CELSIUS));
        prevStateItem = PersistenceExtensions.previousState(quantityItem, false, TestPersistenceService.ID);
        assertNotNull(prevStateItem);
        assertEquals("2012 °C", prevStateItem.getState().toString());

        quantityItem.setState(QuantityType.valueOf(3025, SIUnits.CELSIUS));
        prevStateItem = PersistenceExtensions.previousState(quantityItem, false, TestPersistenceService.ID);
        assertNotNull(prevStateItem);
        assertEquals("2012 °C", prevStateItem.getState().toString());

        // default persistence service
        prevStateItem = PersistenceExtensions.previousState(quantityItem, false);
        assertNull(prevStateItem);
    }

    @Test
    public void testPreviousStateDecimalTypeSkip() {
        numberItem.setState(new DecimalType(2012));
        HistoricItem prevStateItem = PersistenceExtensions.previousState(numberItem, true, TestPersistenceService.ID);
        assertNotNull(prevStateItem);
        assertEquals("2011", prevStateItem.getState().toString());

        // default persistence service
        prevStateItem = PersistenceExtensions.previousState(numberItem, true);
        assertNull(prevStateItem);
    }

    @Test
    public void testPreviousStateQuantityTypeSkip() {
        quantityItem.setState(QuantityType.valueOf(2012, SIUnits.CELSIUS));
        HistoricItem prevStateItem = PersistenceExtensions.previousState(quantityItem, true, TestPersistenceService.ID);
        assertNotNull(prevStateItem);
        assertEquals("2011 °C", prevStateItem.getState().toString());

        // default persistence service
        prevStateItem = PersistenceExtensions.previousState(quantityItem, true);
        assertNull(prevStateItem);
    }

    @Test
    public void testChangedSince() {
        boolean changed = PersistenceExtensions.changedSince(numberItem,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertFalse(changed);

        changed = PersistenceExtensions.changedSince(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertTrue(changed);

        // default persistence service
        changed = PersistenceExtensions.changedSince(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertFalse(changed);
    }

    @Test
    public void testUpdatedSince() {
        boolean updated = PersistenceExtensions.updatedSince(numberItem,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertFalse(updated);

        updated = PersistenceExtensions.updatedSince(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertTrue(updated);

        // default persistence service
        updated = PersistenceExtensions.updatedSince(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertFalse(updated);
    }
}
