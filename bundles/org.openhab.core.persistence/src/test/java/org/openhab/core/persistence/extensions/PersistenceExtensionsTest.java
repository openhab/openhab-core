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
package org.openhab.core.persistence.extensions;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.IntStream;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
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
 * @author Jan N. Klug - Interval method tests and refactoring
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class PersistenceExtensionsTest {

    public static final String TEST_NUMBER = "testNumber";
    public static final String TEST_QUANTITY_NUMBER = "testQuantityNumber";
    public static final String TEST_SWITCH = "testSwitch";

    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;
    private @Mock @NonNullByDefault({}) UnitProvider unitProviderMock;

    private @NonNullByDefault({}) GenericItem numberItem, quantityItem, switchItem;

    @BeforeEach
    public void setUp() {
        when(unitProviderMock.getUnit(Temperature.class)).thenReturn(SIUnits.CELSIUS);

        CoreItemFactory itemFactory = new CoreItemFactory();
        numberItem = itemFactory.createItem(CoreItemFactory.NUMBER, TEST_NUMBER);
        quantityItem = itemFactory.createItem(CoreItemFactory.NUMBER + ItemUtil.EXTENSION_SEPARATOR + "Temperature",
                TEST_QUANTITY_NUMBER);
        quantityItem.setUnitProvider(unitProviderMock);
        switchItem = itemFactory.createItem(CoreItemFactory.SWITCH, TEST_SWITCH);

        when(itemRegistryMock.get(TEST_NUMBER)).thenReturn(numberItem);
        when(itemRegistryMock.get(TEST_QUANTITY_NUMBER)).thenReturn(quantityItem);
        when(itemRegistryMock.get(TEST_SWITCH)).thenReturn(switchItem);

        new PersistenceExtensions(new PersistenceServiceRegistry() {
            private final PersistenceService testPersistenceService = new TestPersistenceService(itemRegistryMock);

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
        ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).minusMinutes(1);
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
    public void testMaximumBetweenDecimalType() {
        HistoricItem historicItem = PersistenceExtensions.maximumBetween(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertThat(historicItem, is(notNullValue()));
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertThat(historicItem.getState().toString(), is("2011"));

        historicItem = PersistenceExtensions.maximumBetween(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertThat(historicItem, is(nullValue()));
    }

    @Test
    public void testMaximumSinceQuantityType() {
        quantityItem.setState(QuantityType.valueOf(1, SIUnits.CELSIUS));
        HistoricItem historicItem = PersistenceExtensions.maximumSince(quantityItem,
                ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertThat(historicItem, is(notNullValue()));
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertThat(historicItem.getState().toString(), is("1 °C"));

        historicItem = PersistenceExtensions.maximumSince(quantityItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertThat(historicItem, is(notNullValue()));
        assertThat(historicItem.getState().toString(), is("2012 °C"));
        assertThat(historicItem.getTimestamp(), is(ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())));

        // default persistence service
        historicItem = PersistenceExtensions.maximumSince(quantityItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertThat(historicItem, is(notNullValue()));
        assertThat(historicItem.getState().toString(), is("1 °C"));
    }

    @Test
    public void testMaximumBetweenQuantityType() {
        HistoricItem historicItem = PersistenceExtensions.maximumBetween(quantityItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertThat(historicItem, is(notNullValue()));
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertThat(historicItem.getState().toString(), is("2011 °C"));

        historicItem = PersistenceExtensions.maximumBetween(quantityItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertThat(historicItem, is(nullValue()));
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
    public void testMinimumBetweenDecimalType() {
        HistoricItem historicItem = PersistenceExtensions.minimumBetween(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertThat(historicItem, is(notNullValue()));
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertThat(historicItem.getState().toString(), is("2005"));

        historicItem = PersistenceExtensions.minimumBetween(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertThat(historicItem, is(nullValue()));
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
    public void testMinimumBetweenQuantityType() {
        HistoricItem historicItem = PersistenceExtensions.minimumBetween(quantityItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertThat(historicItem, is(notNullValue()));
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertThat(historicItem.getState().toString(), is("2005 °C"));

        historicItem = PersistenceExtensions.minimumBetween(quantityItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertThat(historicItem, is(nullValue()));
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
    public void testVarianceBetween() {
        ZonedDateTime startStored = ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        DecimalType variance = PersistenceExtensions.varianceBetween(numberItem, startStored, endStored,
                TestPersistenceService.ID);
        assertThat(variance, is(notNullValue()));
        assertThat(variance.doubleValue(), is(closeTo(4, 0.01)));

        // default persistence service
        variance = PersistenceExtensions.varianceBetween(numberItem, startStored, endStored);
        assertThat(variance, is(nullValue()));
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
    public void testDeviationBetweenDecimalType() {
        ZonedDateTime startStored = ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        DecimalType deviation = PersistenceExtensions.deviationBetween(numberItem, startStored, endStored,
                TestPersistenceService.ID);
        assertThat(deviation, is(notNullValue()));
        assertThat(deviation.doubleValue(), is(closeTo(2, 0.01)));

        // default persistence service
        deviation = PersistenceExtensions.deviationBetween(numberItem, startStored, endStored);
        assertThat(deviation, is(nullValue()));
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
    public void testDeviationBetweenQuantityType() {
        ZonedDateTime startStored = ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        DecimalType deviation = PersistenceExtensions.deviationBetween(quantityItem, startStored, endStored,
                TestPersistenceService.ID);
        assertThat(deviation, is(notNullValue()));
        assertThat(deviation.doubleValue(), is(closeTo(2, 0.01)));

        // default persistence service
        deviation = PersistenceExtensions.deviationBetween(quantityItem, startStored, endStored);
        assertThat(deviation, is(nullValue()));
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
    public void testAverageBetweenDecimalType() {
        ZonedDateTime beginStored = ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        DecimalType average = PersistenceExtensions.averageBetween(numberItem, beginStored, endStored,
                TestPersistenceService.ID);

        assertThat(average, is(notNullValue()));
        assertThat(average.doubleValue(), is(closeTo(2008, 0.01)));

        // default persistence service
        average = PersistenceExtensions.averageBetween(quantityItem, beginStored, endStored);
        assertThat(average, is(nullValue()));
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
    public void testAverageBetweenQuantityType() {
        ZonedDateTime beginStored = ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        DecimalType average = PersistenceExtensions.averageBetween(quantityItem, beginStored, endStored,
                TestPersistenceService.ID);

        assertThat(average, is(notNullValue()));
        assertThat(average.doubleValue(), is(closeTo(2008, 0.01)));

        // default persistence service
        average = PersistenceExtensions.averageBetween(quantityItem, beginStored, endStored);
        assertThat(average, is(nullValue()));
    }

    @Test
    public void testAverageSinceSwitch() {
        switchItem.setState(OnOffType.ON);

        ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        DecimalType average = PersistenceExtensions.averageSince(switchItem, now.minusHours(15),
                TestPersistenceService.ID);
        assertThat(average, is(notNullValue()));
        assertThat(average.doubleValue(), is(closeTo(0.625, 0.04)));

        average = PersistenceExtensions.averageSince(switchItem, now.minusHours(7), TestPersistenceService.ID);
        assertThat(average, is(notNullValue()));
        assertThat(average.doubleValue(), is(closeTo(0.714, 0.1)));

        average = PersistenceExtensions.averageSince(switchItem, now.minusHours(6), TestPersistenceService.ID);
        assertThat(average, is(notNullValue()));
        assertThat(average.doubleValue(), is(closeTo(0.833, 0.2)));

        average = PersistenceExtensions.averageSince(switchItem, now.minusHours(5), TestPersistenceService.ID);
        assertThat(average, is(notNullValue()));
        assertThat(average.doubleValue(), is(closeTo(1d, 0.2)));

        average = PersistenceExtensions.averageSince(switchItem, now.minusHours(1), TestPersistenceService.ID);
        assertThat(average, is(notNullValue()));
        assertThat(average.doubleValue(), is(closeTo(1d, 0.001)));

        average = PersistenceExtensions.averageSince(switchItem, now, TestPersistenceService.ID);
        assertThat(average, is(notNullValue()));
        assertThat(average.doubleValue(), is(closeTo(1d, 0.001)));

        average = PersistenceExtensions.averageSince(switchItem, now.plusHours(1), TestPersistenceService.ID);
        assertThat(average, is(nullValue()));
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
    public void testSumBetweenDecimalType() {
        DecimalType sum = PersistenceExtensions.sumBetween(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertThat(sum, is(notNullValue()));
        assertThat(sum.doubleValue(), is(closeTo(14056.0, 0.1)));

        sum = PersistenceExtensions.sumBetween(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));

        assertThat(sum, is(notNullValue()));
        assertThat(sum.doubleValue(), is(closeTo(0.0, 0.1)));
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
    public void testDeltaBetween() {
        DecimalType delta = PersistenceExtensions.deltaBetween(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertThat(delta, is(notNullValue()));
        assertThat(delta.doubleValue(), is(closeTo(6, 0.001)));

        delta = PersistenceExtensions.deltaBetween(quantityItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertThat(delta, is(notNullValue()));
        assertThat(delta.doubleValue(), is(closeTo(6, 0.001)));

        // default persistence service
        delta = PersistenceExtensions.deltaBetween(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertThat(delta, is(nullValue()));
    }

    @Test
    public void testEvolutionRate() {
        numberItem.setState(new DecimalType(2012));
        DecimalType rate = PersistenceExtensions.evolutionRate(numberItem,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertThat(rate, is(nullValue()));

        rate = PersistenceExtensions.evolutionRate(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertThat(rate, is(notNullValue()));
        // ((now - then) / then) * 100
        assertThat(rate.doubleValue(), is(closeTo(0.349, 0.001)));

        rate = PersistenceExtensions.evolutionRate(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertThat(rate, is(notNullValue()));
        // ((now - then) / then) * 100
        assertThat(rate.doubleValue(), is(closeTo(0.299, 0.001)));

        numberItem.setState(new QuantityType<>(2012, SIUnits.CELSIUS));
        rate = PersistenceExtensions.evolutionRate(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertThat(rate, is(notNullValue()));
        // ((now - then) / then) * 100
        assertThat(rate.doubleValue(), is(closeTo(0.349, 0.001)));

        rate = PersistenceExtensions.evolutionRate(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertThat(rate, is(notNullValue()));
        // ((now - then) / then) * 100
        assertThat(rate.doubleValue(), is(closeTo(0.299, 0.001)));

        // default persistence service
        rate = PersistenceExtensions.evolutionRate(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertThat(rate, is(nullValue()));

        rate = PersistenceExtensions.evolutionRate(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertThat(rate, is(nullValue()));
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
    public void testChangedBetween() {
        boolean changed = PersistenceExtensions.changedBetween(numberItem,
                ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2019, 5, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertFalse(changed);

        changed = PersistenceExtensions.changedBetween(numberItem,
                ZonedDateTime.of(2006, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2008, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertTrue(changed);

        // default persistence service
        changed = PersistenceExtensions.changedBetween(numberItem,
                ZonedDateTime.of(2004, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
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

    @Test
    public void testUpdatedBetween() {
        boolean updated = PersistenceExtensions.updatedBetween(numberItem,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertFalse(updated);

        updated = PersistenceExtensions.updatedBetween(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertTrue(updated);

        updated = PersistenceExtensions.updatedBetween(numberItem,
                ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertTrue(updated);

        // default persistence service
        updated = PersistenceExtensions.updatedBetween(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertFalse(updated);
    }

    @Test
    public void testCountBetween() {
        long counts = PersistenceExtensions.countBetween(numberItem,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertEquals(0, counts);

        counts = PersistenceExtensions.countBetween(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertEquals(7, counts);

        counts = PersistenceExtensions.countBetween(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertEquals(0, counts);
    }

    @Test
    public void testCountSince() {
        long counts = PersistenceExtensions.countSince(numberItem,
                ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertEquals(33, counts);

        counts = PersistenceExtensions.countSince(numberItem,
                ZonedDateTime.of(2007, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertEquals(6, counts);

        counts = PersistenceExtensions.countSince(numberItem,
                ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertEquals(0, counts);

        counts = PersistenceExtensions.countSince(numberItem,
                ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertEquals(0, counts);
    }

    @Test
    public void testCountStateChangesSince() {
        long counts = PersistenceExtensions.countStateChangesSince(numberItem,
                ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertEquals(32, counts);

        counts = PersistenceExtensions.countStateChangesSince(numberItem,
                ZonedDateTime.of(2007, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertEquals(5, counts);

        counts = PersistenceExtensions.countStateChangesSince(numberItem,
                ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertEquals(0, counts);

        counts = PersistenceExtensions.countStateChangesSince(numberItem,
                ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertEquals(0, counts);
    }

    @Test
    public void testCountStateChangesBetween() {
        long counts = PersistenceExtensions.countStateChangesBetween(numberItem,
                ZonedDateTime.of(1940, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertEquals(0, counts);

        counts = PersistenceExtensions.countStateChangesBetween(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), TestPersistenceService.ID);
        assertEquals(6, counts);

        counts = PersistenceExtensions.countStateChangesBetween(numberItem,
                ZonedDateTime.of(2005, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(2011, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertEquals(0, counts);
    }
}
