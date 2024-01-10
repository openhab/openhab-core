/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
import static org.openhab.core.persistence.extensions.TestPersistenceService.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.DoubleStream;
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
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.PersistenceServiceRegistry;
import org.openhab.core.types.State;

/**
 * @author Kai Kreuzer - Initial contribution
 * @author Chris Jackson - Initial contribution
 * @author Jan N. Klug - Fix averageSince calculation
 * @author Jan N. Klug - Interval method tests and refactoring
 * @author Mark Herwege - Changed return types to State for some interval methods to also return unit
 * @author Mark Herwege - Extended for future dates
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
    private @Mock @NonNullByDefault({}) TimeZoneProvider timeZoneProviderMock;

    private @NonNullByDefault({}) GenericItem numberItem, quantityItem, switchItem;

    @BeforeEach
    public void setUp() {
        when(unitProviderMock.getUnit(Temperature.class)).thenReturn(SIUnits.CELSIUS);

        CoreItemFactory itemFactory = new CoreItemFactory(unitProviderMock);
        numberItem = itemFactory.createItem(CoreItemFactory.NUMBER, TEST_NUMBER);
        quantityItem = itemFactory.createItem(CoreItemFactory.NUMBER + ItemUtil.EXTENSION_SEPARATOR + "Temperature",
                TEST_QUANTITY_NUMBER);
        switchItem = itemFactory.createItem(CoreItemFactory.SWITCH, TEST_SWITCH);

        numberItem.setState(STATE);
        quantityItem.setState(new QuantityType<Temperature>(STATE, SIUnits.CELSIUS));
        switchItem.setState(SWITCH_STATE);

        when(itemRegistryMock.get(TEST_NUMBER)).thenReturn(numberItem);
        when(itemRegistryMock.get(TEST_QUANTITY_NUMBER)).thenReturn(quantityItem);
        when(itemRegistryMock.get(TEST_SWITCH)).thenReturn(switchItem);

        when(timeZoneProviderMock.getTimeZone()).thenReturn(ZoneId.systemDefault());

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
                return TestPersistenceService.SERVICE_ID.equals(serviceId) ? testPersistenceService : null;
            }
        }, timeZoneProviderMock);
    }

    @Test
    public void testPersistedStateDecimalType() {
        HistoricItem historicItem = PersistenceExtensions.persistedState(numberItem,
                ZonedDateTime.of(HISTORIC_END, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertEquals(value(HISTORIC_END), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 12, 31, 0, 0, 0, 0, ZoneId.systemDefault()),
                SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(value(HISTORIC_INTERMEDIATE_VALUE_1), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(value(HISTORIC_INTERMEDIATE_VALUE_1), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(numberItem, ZonedDateTime.now(), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(value(TestPersistenceService.HISTORIC_END), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_NOVALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(value(HISTORIC_END), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(numberItem,
                ZonedDateTime.of(FUTURE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(value(FUTURE_START), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(value(FUTURE_INTERMEDIATE_VALUE_3), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(numberItem,
                ZonedDateTime.of(FUTURE_END, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(value(FUTURE_END), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(numberItem,
                ZonedDateTime.of(AFTER_END, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(value(FUTURE_END), historicItem.getState());

        // default persistence service
        historicItem = PersistenceExtensions.persistedState(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);
    }

    @Test
    public void testPersistedStateQuantityType() {
        HistoricItem historicItem = PersistenceExtensions.persistedState(quantityItem,
                ZonedDateTime.of(HISTORIC_END, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertEquals(new QuantityType<>(value(HISTORIC_END), SIUnits.CELSIUS), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 12, 31, 0, 0, 0, 0, ZoneId.systemDefault()),
                SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(new QuantityType<>(value(HISTORIC_INTERMEDIATE_VALUE_1), SIUnits.CELSIUS),
                historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(new QuantityType<>(value(HISTORIC_INTERMEDIATE_VALUE_1), SIUnits.CELSIUS),
                historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(quantityItem, ZonedDateTime.now(), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(new QuantityType<>(value(HISTORIC_END), SIUnits.CELSIUS), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(quantityItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_NOVALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(new QuantityType<>(value(HISTORIC_END), SIUnits.CELSIUS), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(quantityItem,
                ZonedDateTime.of(FUTURE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(new QuantityType<>(value(FUTURE_START), SIUnits.CELSIUS), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(quantityItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(new QuantityType<>(value(FUTURE_INTERMEDIATE_VALUE_3), SIUnits.CELSIUS), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(quantityItem,
                ZonedDateTime.of(FUTURE_END, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(new QuantityType<>(value(FUTURE_END), SIUnits.CELSIUS), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(quantityItem,
                ZonedDateTime.of(AFTER_END, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(new QuantityType<>(value(FUTURE_END), SIUnits.CELSIUS), historicItem.getState());

        // default persistence service
        historicItem = PersistenceExtensions.persistedState(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);
    }

    @Test
    public void testPersistedStateOnOffType() {
        ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).minusMinutes(1);
        HistoricItem historicItem = PersistenceExtensions.persistedState(switchItem, now.plusHours(SWITCH_START),
                SERVICE_ID);
        assertNull(historicItem);

        historicItem = PersistenceExtensions.persistedState(switchItem, now.plusHours(SWITCH_ON_INTERMEDIATE_1),
                SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(switchValue(SWITCH_ON_INTERMEDIATE_1), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(switchItem, now.plusHours(SWITCH_OFF_INTERMEDIATE_1),
                SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(switchValue(SWITCH_OFF_INTERMEDIATE_1), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(switchItem, now.plusHours(SWITCH_OFF_INTERMEDIATE_2),
                SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(switchValue(SWITCH_OFF_INTERMEDIATE_2), historicItem.getState());

        historicItem = PersistenceExtensions.persistedState(switchItem, now.plusHours(SWITCH_ON_INTERMEDIATE_3),
                SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(switchValue(SWITCH_ON_INTERMEDIATE_3), historicItem.getState());
    }

    @Test
    public void testMaximumSinceDecimalType() {
        HistoricItem historicItem = PersistenceExtensions.maximumSince(numberItem,
                ZonedDateTime.of(HISTORIC_END, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertEquals(value(HISTORIC_END), historicItem.getState());

        historicItem = PersistenceExtensions.maximumSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(value(HISTORIC_END), historicItem.getState());
        assertEquals(ZonedDateTime.of(HISTORIC_END, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                historicItem.getTimestamp());

        // default persistence service
        historicItem = PersistenceExtensions.maximumSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);
    }

    @Test
    public void testMaximumTillDecimalType() {
        HistoricItem historicItem = PersistenceExtensions.maximumTill(numberItem,
                ZonedDateTime.of(FUTURE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertEquals(value(FUTURE_START), historicItem.getState());

        historicItem = PersistenceExtensions.maximumTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(value(FUTURE_INTERMEDIATE_VALUE_3), historicItem.getState());
        assertEquals(ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                historicItem.getTimestamp());

        // default persistence service
        historicItem = PersistenceExtensions.maximumTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);
    }

    @Test
    public void testMaximumBetweenDecimalType() {
        HistoricItem historicItem = PersistenceExtensions.maximumBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertThat(historicItem.getState(), is(value(HISTORIC_INTERMEDIATE_VALUE_2)));

        historicItem = PersistenceExtensions.maximumBetween(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertThat(historicItem.getState(), is(value(FUTURE_INTERMEDIATE_VALUE_4)));

        historicItem = PersistenceExtensions.maximumBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertThat(historicItem.getState(), is(value(FUTURE_INTERMEDIATE_VALUE_4)));

        // default persistence service
        historicItem = PersistenceExtensions.maximumBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);
    }

    @Test
    public void testMaximumSinceQuantityType() {
        HistoricItem historicItem = PersistenceExtensions.maximumSince(quantityItem,
                ZonedDateTime.of(HISTORIC_END, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertThat(historicItem.getState(), is(new QuantityType<>(value(HISTORIC_END), SIUnits.CELSIUS)));

        historicItem = PersistenceExtensions.maximumSince(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(new QuantityType<>(value(HISTORIC_END), SIUnits.CELSIUS)));
        assertThat(historicItem.getTimestamp(),
                is(ZonedDateTime.of(HISTORIC_END, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())));

        // default persistence service
        historicItem = PersistenceExtensions.maximumSince(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);

        // test with alternative unit
        quantityItem.setState(QuantityType.valueOf(5000, Units.KELVIN));
        historicItem = PersistenceExtensions.maximumSince(quantityItem,
                ZonedDateTime.of(HISTORIC_END, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertThat(historicItem.getState(), is(new QuantityType<>(4726.85, SIUnits.CELSIUS)));
    }

    @Test
    public void testMaximumTillQuantityType() {
        HistoricItem historicItem = PersistenceExtensions.maximumTill(quantityItem,
                ZonedDateTime.of(FUTURE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertEquals(new QuantityType<>(value(FUTURE_START), SIUnits.CELSIUS), historicItem.getState());

        historicItem = PersistenceExtensions.maximumTill(quantityItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(new QuantityType<>(value(FUTURE_INTERMEDIATE_VALUE_3), SIUnits.CELSIUS), historicItem.getState());
        assertEquals(ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                historicItem.getTimestamp());

        // default persistence service
        historicItem = PersistenceExtensions.maximumTill(quantityItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);
    }

    @Test
    public void testMaximumBetweenQuantityType() {
        HistoricItem historicItem = PersistenceExtensions.maximumBetween(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertThat(historicItem.getState(),
                is(new QuantityType<>(value(HISTORIC_INTERMEDIATE_VALUE_2), SIUnits.CELSIUS)));

        historicItem = PersistenceExtensions.maximumBetween(quantityItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertThat(historicItem.getState(),
                is(new QuantityType<>(value(FUTURE_INTERMEDIATE_VALUE_4), SIUnits.CELSIUS)));

        historicItem = PersistenceExtensions.maximumBetween(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertThat(historicItem.getState(),
                is(new QuantityType<>(value(FUTURE_INTERMEDIATE_VALUE_4), SIUnits.CELSIUS)));

        // default persistence service
        historicItem = PersistenceExtensions.maximumBetween(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);
    }

    @Test
    public void testMaximumSinceOnOffType() {
        ZonedDateTime now = ZonedDateTime.now();
        HistoricItem historicItem = PersistenceExtensions.maximumSince(switchItem, now.plusHours(SWITCH_START),
                SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(switchValue(SWITCH_ON_1), historicItem.getState());

        historicItem = PersistenceExtensions.maximumSince(switchItem, now.plusHours(SWITCH_OFF_INTERMEDIATE_1),
                SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(switchValue(SWITCH_ON_2), historicItem.getState());

        historicItem = PersistenceExtensions.maximumSince(switchItem, now.plusHours(SWITCH_ON_INTERMEDIATE_21),
                SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(switchValue(SWITCH_ON_2), historicItem.getState());

        historicItem = PersistenceExtensions.maximumSince(switchItem, now, SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(switchValue(SWITCH_ON_2), historicItem.getState());

        historicItem = PersistenceExtensions.maximumSince(switchItem, now.plusHours(SWITCH_ON_INTERMEDIATE_22),
                SERVICE_ID);
        assertNull(historicItem);
    }

    @Test
    public void testMaximumTillOnOffType() {
        ZonedDateTime now = ZonedDateTime.now();
        HistoricItem historicItem = PersistenceExtensions.maximumTill(switchItem,
                now.plusHours(SWITCH_OFF_INTERMEDIATE_2), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(switchValue(SWITCH_ON_2), historicItem.getState());

        historicItem = PersistenceExtensions.maximumTill(switchItem, now.plusHours(SWITCH_ON_INTERMEDIATE_3),
                SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(switchValue(SWITCH_ON_3), historicItem.getState());

        historicItem = PersistenceExtensions.maximumTill(switchItem, now.plusHours(SWITCH_END), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(switchValue(SWITCH_ON_3), historicItem.getState());

        historicItem = PersistenceExtensions.maximumTill(switchItem, now.plusHours(SWITCH_ON_INTERMEDIATE_21),
                SERVICE_ID);
        assertNull(historicItem);
    }

    @Test
    public void testMinimumSinceDecimalType() {
        HistoricItem historicItem = PersistenceExtensions.minimumSince(numberItem,
                ZonedDateTime.of(BEFORE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertEquals(value(HISTORIC_START), historicItem.getState());

        historicItem = PersistenceExtensions.minimumSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(value(HISTORIC_INTERMEDIATE_VALUE_1), historicItem.getState());
        assertEquals(ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                historicItem.getTimestamp());

        // default persistence service
        historicItem = PersistenceExtensions.minimumSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);
    }

    @Test
    public void testMinimumTillDecimalType() {
        HistoricItem historicItem = PersistenceExtensions.minimumTill(numberItem,
                ZonedDateTime.of(FUTURE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertEquals(value(HISTORIC_END), historicItem.getState());

        historicItem = PersistenceExtensions.minimumTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(value(HISTORIC_END), historicItem.getState());

        // default persistence service
        historicItem = PersistenceExtensions.minimumTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);
    }

    @Test
    public void testMinimumBetweenDecimalType() {
        HistoricItem historicItem = PersistenceExtensions.minimumBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertThat(historicItem.getState(), is(value(HISTORIC_INTERMEDIATE_VALUE_1)));

        historicItem = PersistenceExtensions.minimumBetween(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertThat(historicItem.getState(), is(value(FUTURE_INTERMEDIATE_VALUE_3)));

        historicItem = PersistenceExtensions.minimumBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(DecimalType.class)));
        assertThat(historicItem.getState(), is(value(HISTORIC_INTERMEDIATE_VALUE_1)));

        // default persistence service
        historicItem = PersistenceExtensions.minimumBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);
    }

    @Test
    public void testMinimumSinceQuantityType() {
        HistoricItem historicItem = PersistenceExtensions.minimumSince(quantityItem,
                ZonedDateTime.of(BEFORE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertEquals(new QuantityType<>(value(HISTORIC_START), SIUnits.CELSIUS), historicItem.getState());

        historicItem = PersistenceExtensions.minimumSince(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(new QuantityType<>(value(HISTORIC_INTERMEDIATE_VALUE_1), SIUnits.CELSIUS),
                historicItem.getState());
        assertEquals(ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                historicItem.getTimestamp());

        // default persistence service
        historicItem = PersistenceExtensions.minimumSince(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);

        // test with alternative unit
        quantityItem.setState(QuantityType.valueOf(273.15, Units.KELVIN));
        historicItem = PersistenceExtensions.minimumSince(quantityItem,
                ZonedDateTime.of(HISTORIC_END, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertThat(historicItem.getState(), is(new QuantityType<>(0, SIUnits.CELSIUS)));
    }

    @Test
    public void testMinimumTillQuantityType() {
        HistoricItem historicItem = PersistenceExtensions.minimumTill(quantityItem,
                ZonedDateTime.of(FUTURE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertEquals(new QuantityType<>(value(HISTORIC_END), SIUnits.CELSIUS), historicItem.getState());

        historicItem = PersistenceExtensions.minimumTill(quantityItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(new QuantityType<>(value(HISTORIC_END), SIUnits.CELSIUS), historicItem.getState());

        // default persistence service
        historicItem = PersistenceExtensions.minimumTill(quantityItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);
    }

    @Test
    public void testMinimumBetweenQuantityType() {
        HistoricItem historicItem = PersistenceExtensions.minimumBetween(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertThat(historicItem.getState(),
                is(new QuantityType<>(value(HISTORIC_INTERMEDIATE_VALUE_1), SIUnits.CELSIUS)));

        historicItem = PersistenceExtensions.minimumBetween(quantityItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertThat(historicItem.getState(),
                is(new QuantityType<>(value(FUTURE_INTERMEDIATE_VALUE_3), SIUnits.CELSIUS)));

        historicItem = PersistenceExtensions.minimumBetween(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(historicItem);
        assertThat(historicItem.getState(), is(instanceOf(QuantityType.class)));
        assertThat(historicItem.getState(),
                is(new QuantityType<>(value(HISTORIC_INTERMEDIATE_VALUE_1), SIUnits.CELSIUS)));

        // default persistence service
        historicItem = PersistenceExtensions.minimumBetween(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(historicItem);
    }

    @Test
    public void testMinimumSinceOnOffType() {
        ZonedDateTime now = ZonedDateTime.now();
        HistoricItem historicItem = PersistenceExtensions.minimumSince(switchItem, now.plusHours(SWITCH_START),
                SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(switchValue(SWITCH_OFF_1), historicItem.getState());

        historicItem = PersistenceExtensions.minimumSince(switchItem, now.plusHours(SWITCH_OFF_INTERMEDIATE_1),
                SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(switchValue(SWITCH_OFF_INTERMEDIATE_1), historicItem.getState());

        historicItem = PersistenceExtensions.minimumSince(switchItem, now.plusHours(SWITCH_ON_INTERMEDIATE_21),
                SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(switchValue(SWITCH_ON_INTERMEDIATE_21), historicItem.getState());

        historicItem = PersistenceExtensions.minimumSince(switchItem, now, SERVICE_ID);
        assertNotNull(historicItem);
        assertEquals(switchValue(SWITCH_ON_INTERMEDIATE_22), historicItem.getState());

        historicItem = PersistenceExtensions.minimumSince(switchItem, now.plusHours(SWITCH_ON_INTERMEDIATE_22),
                SERVICE_ID);
        assertNull(historicItem);
    }

    @Test
    public void testVarianceSinceDecimalType() {
        ZonedDateTime startStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        double expectedAverage = average(HISTORIC_INTERMEDIATE_VALUE_1, null);

        double expected = DoubleStream
                .concat(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_END)
                        .mapToDouble(i -> Double.valueOf(i)), DoubleStream.of(STATE.doubleValue()))
                .map(d -> Math.pow(d - expectedAverage, 2)).sum()
                / (HISTORIC_END + 1 - HISTORIC_INTERMEDIATE_VALUE_1 + 1);
        State variance = PersistenceExtensions.varianceSince(numberItem, startStored, SERVICE_ID);
        assertNotNull(variance);
        DecimalType dt = variance.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(expected, dt.doubleValue(), 0.01);

        // default persistence service
        variance = PersistenceExtensions.varianceSince(numberItem, startStored);
        assertNull(variance);
    }

    @Test
    public void testVarianceTillDecimalType() {
        ZonedDateTime endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        double expectedAverage = average(null, FUTURE_INTERMEDIATE_VALUE_3);

        double expected = DoubleStream
                .concat(DoubleStream.of(STATE.doubleValue()),
                        IntStream.rangeClosed(FUTURE_START, FUTURE_INTERMEDIATE_VALUE_3)
                                .mapToDouble(i -> Double.valueOf(i)))
                .map(d -> Math.pow(d - expectedAverage, 2)).sum()
                / (1 + FUTURE_INTERMEDIATE_VALUE_3 - FUTURE_START + 1);
        State variance = PersistenceExtensions.varianceTill(numberItem, endStored, SERVICE_ID);
        assertNotNull(variance);
        DecimalType dt = variance.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(expected, dt.doubleValue(), 0.01);

        // default persistence service
        variance = PersistenceExtensions.varianceTill(numberItem, endStored);
        assertNull(variance);
    }

    @Test
    public void testVarianceBetweenDecimalType() {
        ZonedDateTime startStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        double expectedAverage1 = average(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_INTERMEDIATE_VALUE_2);

        double expected = IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_INTERMEDIATE_VALUE_2)
                .mapToDouble(i -> Double.valueOf(i)).map(d -> Math.pow(d - expectedAverage1, 2)).sum()
                / (HISTORIC_INTERMEDIATE_VALUE_2 - HISTORIC_INTERMEDIATE_VALUE_1 + 1);

        State variance = PersistenceExtensions.varianceBetween(numberItem, startStored, endStored, SERVICE_ID);
        assertNotNull(variance);
        DecimalType dt = variance.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(expected, 0.01)));

        startStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        double expectedAverage2 = average(FUTURE_INTERMEDIATE_VALUE_3, FUTURE_INTERMEDIATE_VALUE_4);

        expected = IntStream.rangeClosed(FUTURE_INTERMEDIATE_VALUE_3, FUTURE_INTERMEDIATE_VALUE_4)
                .mapToDouble(i -> Double.valueOf(i)).map(d -> Math.pow(d - expectedAverage2, 2)).sum()
                / (FUTURE_INTERMEDIATE_VALUE_4 - FUTURE_INTERMEDIATE_VALUE_3 + 1);

        variance = PersistenceExtensions.varianceBetween(numberItem, startStored, endStored, SERVICE_ID);
        assertNotNull(variance);
        dt = variance.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(expected, 0.01)));

        startStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        double expectedAverage3 = average(HISTORIC_INTERMEDIATE_VALUE_1, FUTURE_INTERMEDIATE_VALUE_3);

        expected = IntStream
                .concat(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_END),
                        IntStream.rangeClosed(FUTURE_START, FUTURE_INTERMEDIATE_VALUE_3))
                .mapToDouble(i -> Double.valueOf(i)).map(d -> Math.pow(d - expectedAverage3, 2)).sum()
                / (FUTURE_INTERMEDIATE_VALUE_3 - FUTURE_START + 1 + HISTORIC_END - HISTORIC_INTERMEDIATE_VALUE_1 + 1);

        variance = PersistenceExtensions.varianceBetween(numberItem, startStored, endStored, SERVICE_ID);
        assertNotNull(variance);
        dt = variance.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(expected, 0.01)));

        // default persistence service
        variance = PersistenceExtensions.varianceBetween(numberItem, startStored, endStored);
        assertNull(variance);
    }

    @Test
    public void testVarianceSinceQuantityType() {
        ZonedDateTime startStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        double expectedAverage = average(HISTORIC_INTERMEDIATE_VALUE_1, null);

        double expected = DoubleStream
                .concat(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_END)
                        .mapToDouble(i -> Double.valueOf(i)), DoubleStream.of(STATE.doubleValue()))
                .map(d -> Math.pow(d - expectedAverage, 2)).sum()
                / (HISTORIC_END + 1 - HISTORIC_INTERMEDIATE_VALUE_1 + 1);
        State variance = PersistenceExtensions.varianceSince(quantityItem, startStored, SERVICE_ID);
        assertNotNull(variance);
        QuantityType<?> qt = variance.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(expected, qt.doubleValue(), 0.01);
        assertEquals(SIUnits.CELSIUS.multiply(SIUnits.CELSIUS), qt.getUnit());

        // default persistence service
        variance = PersistenceExtensions.varianceSince(quantityItem, startStored);
        assertNull(variance);
    }

    @Test
    public void testVarianceTillQuantityType() {
        ZonedDateTime endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        double expectedAverage = average(null, FUTURE_INTERMEDIATE_VALUE_3);

        double expected = DoubleStream
                .concat(DoubleStream.of(STATE.doubleValue()),
                        IntStream.rangeClosed(FUTURE_START, FUTURE_INTERMEDIATE_VALUE_3)
                                .mapToDouble(i -> Double.valueOf(i)))
                .map(d -> Math.pow(d - expectedAverage, 2)).sum()
                / (1 + FUTURE_INTERMEDIATE_VALUE_3 - FUTURE_START + 1);
        State variance = PersistenceExtensions.varianceTill(quantityItem, endStored, SERVICE_ID);
        assertNotNull(variance);
        QuantityType<?> qt = variance.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(expected, qt.doubleValue(), 0.01);
        assertEquals(SIUnits.CELSIUS.multiply(SIUnits.CELSIUS), qt.getUnit());

        // default persistence service
        variance = PersistenceExtensions.varianceTill(quantityItem, endStored);
        assertNull(variance);
    }

    @Test
    public void testVarianceBetweenQuantityType() {
        ZonedDateTime startStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        double expectedAverage1 = average(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_INTERMEDIATE_VALUE_2);

        double expected = IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_INTERMEDIATE_VALUE_2)
                .mapToDouble(i -> Double.valueOf(i)).map(d -> Math.pow(d - expectedAverage1, 2)).sum()
                / (HISTORIC_INTERMEDIATE_VALUE_2 - HISTORIC_INTERMEDIATE_VALUE_1 + 1);

        State variance = PersistenceExtensions.varianceBetween(quantityItem, startStored, endStored, SERVICE_ID);
        assertNotNull(variance);
        QuantityType<?> qt = variance.as(QuantityType.class);
        assertNotNull(qt);
        assertThat(qt.doubleValue(), is(closeTo(expected, 0.01)));
        assertEquals(SIUnits.CELSIUS.multiply(SIUnits.CELSIUS), qt.getUnit());

        startStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        double expectedAverage2 = average(FUTURE_INTERMEDIATE_VALUE_3, FUTURE_INTERMEDIATE_VALUE_4);

        expected = IntStream.rangeClosed(FUTURE_INTERMEDIATE_VALUE_3, FUTURE_INTERMEDIATE_VALUE_4)
                .mapToDouble(i -> Double.valueOf(i)).map(d -> Math.pow(d - expectedAverage2, 2)).sum()
                / (FUTURE_INTERMEDIATE_VALUE_4 - FUTURE_INTERMEDIATE_VALUE_3 + 1);

        variance = PersistenceExtensions.varianceBetween(quantityItem, startStored, endStored, SERVICE_ID);
        assertNotNull(variance);
        qt = variance.as(QuantityType.class);
        assertNotNull(qt);
        assertThat(qt.doubleValue(), is(closeTo(expected, 0.01)));
        assertEquals(SIUnits.CELSIUS.multiply(SIUnits.CELSIUS), qt.getUnit());

        startStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        double expectedAverage3 = average(HISTORIC_INTERMEDIATE_VALUE_1, FUTURE_INTERMEDIATE_VALUE_3);

        expected = IntStream
                .concat(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_END),
                        IntStream.rangeClosed(FUTURE_START, FUTURE_INTERMEDIATE_VALUE_3))
                .mapToDouble(i -> Double.valueOf(i)).map(d -> Math.pow(d - expectedAverage3, 2)).sum()
                / (FUTURE_INTERMEDIATE_VALUE_3 - FUTURE_START + 1 + HISTORIC_END - HISTORIC_INTERMEDIATE_VALUE_1 + 1);

        variance = PersistenceExtensions.varianceBetween(quantityItem, startStored, endStored, SERVICE_ID);
        assertNotNull(variance);
        qt = variance.as(QuantityType.class);
        assertNotNull(qt);
        assertThat(qt.doubleValue(), is(closeTo(expected, 0.01)));
        assertEquals(SIUnits.CELSIUS.multiply(SIUnits.CELSIUS), qt.getUnit());

        // default persistence service
        variance = PersistenceExtensions.varianceBetween(quantityItem, startStored, endStored);
        assertNull(variance);
    }

    @Test
    public void testDeviationSinceDecimalType() {
        ZonedDateTime startStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        double expectedAverage = average(HISTORIC_INTERMEDIATE_VALUE_1, null);

        double expected = Math.sqrt(DoubleStream
                .concat(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_END)
                        .mapToDouble(i -> Double.valueOf(i)), DoubleStream.of(STATE.doubleValue()))
                .map(d -> Math.pow(d - expectedAverage, 2)).sum()
                / (HISTORIC_END + 1 - HISTORIC_INTERMEDIATE_VALUE_1 + 1));
        State deviation = PersistenceExtensions.deviationSince(numberItem, startStored, SERVICE_ID);
        assertNotNull(deviation);
        DecimalType dt = deviation.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(expected, dt.doubleValue(), 0.01);

        // default persistence service
        deviation = PersistenceExtensions.deviationSince(numberItem, startStored);
        assertNull(deviation);
    }

    @Test
    public void testDeviationTillDecimalType() {
        ZonedDateTime endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        double expectedAverage = average(null, FUTURE_INTERMEDIATE_VALUE_3);

        double expected = Math.sqrt(DoubleStream
                .concat(DoubleStream.of(STATE.doubleValue()),
                        IntStream.rangeClosed(FUTURE_START, FUTURE_INTERMEDIATE_VALUE_3)
                                .mapToDouble(i -> Double.valueOf(i)))
                .map(d -> Math.pow(d - expectedAverage, 2)).sum()
                / (1 + FUTURE_INTERMEDIATE_VALUE_3 - FUTURE_START + 1));
        State deviation = PersistenceExtensions.deviationTill(numberItem, endStored, SERVICE_ID);
        assertNotNull(deviation);
        DecimalType dt = deviation.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(expected, dt.doubleValue(), 0.01);

        // default persistence service
        deviation = PersistenceExtensions.deviationTill(numberItem, endStored);
        assertNull(deviation);
    }

    @Test
    public void testDeviationBetweenDecimalType() {
        ZonedDateTime startStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        double expectedAverage = average(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_INTERMEDIATE_VALUE_2);

        double expected = Math.sqrt(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_INTERMEDIATE_VALUE_2)
                .mapToDouble(i -> Double.parseDouble(Integer.toString(i))).map(d -> Math.pow(d - expectedAverage, 2))
                .sum() / (HISTORIC_INTERMEDIATE_VALUE_2 - HISTORIC_INTERMEDIATE_VALUE_1 + 1));
        State deviation = PersistenceExtensions.deviationBetween(numberItem, startStored, endStored, SERVICE_ID);
        assertNotNull(deviation);
        DecimalType dt = deviation.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(expected, 0.01)));

        startStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        double expectedAverage2 = average(FUTURE_INTERMEDIATE_VALUE_3, FUTURE_INTERMEDIATE_VALUE_4);

        expected = Math.sqrt(IntStream.rangeClosed(FUTURE_INTERMEDIATE_VALUE_3, FUTURE_INTERMEDIATE_VALUE_4)
                .mapToDouble(i -> Double.valueOf(i)).map(d -> Math.pow(d - expectedAverage2, 2)).sum()
                / (FUTURE_INTERMEDIATE_VALUE_4 - FUTURE_INTERMEDIATE_VALUE_3 + 1));

        deviation = PersistenceExtensions.deviationBetween(numberItem, startStored, endStored, SERVICE_ID);
        assertNotNull(deviation);
        dt = deviation.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(expected, 0.01)));

        startStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        double expectedAverage3 = average(HISTORIC_INTERMEDIATE_VALUE_1, FUTURE_INTERMEDIATE_VALUE_3);

        expected = Math.sqrt(IntStream
                .concat(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_END),
                        IntStream.rangeClosed(FUTURE_START, FUTURE_INTERMEDIATE_VALUE_3))
                .mapToDouble(i -> Double.valueOf(i)).map(d -> Math.pow(d - expectedAverage3, 2)).sum()
                / (FUTURE_INTERMEDIATE_VALUE_3 - FUTURE_START + 1 + HISTORIC_END - HISTORIC_INTERMEDIATE_VALUE_1 + 1));

        deviation = PersistenceExtensions.deviationBetween(numberItem, startStored, endStored, SERVICE_ID);
        assertNotNull(deviation);
        dt = deviation.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(expected, 0.01)));

        // default persistence service
        deviation = PersistenceExtensions.deviationBetween(numberItem, startStored, endStored);
        assertNull(deviation);
    }

    @Test
    public void testDeviationSinceQuantityType() {
        ZonedDateTime startStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        double expectedAverage = average(HISTORIC_INTERMEDIATE_VALUE_1, null);

        double expected = Math.sqrt(DoubleStream
                .concat(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_END)
                        .mapToDouble(i -> Double.valueOf(i)), DoubleStream.of(STATE.doubleValue()))
                .map(d -> Math.pow(d - expectedAverage, 2)).sum()
                / (HISTORIC_END + 1 - HISTORIC_INTERMEDIATE_VALUE_1 + 1));
        State deviation = PersistenceExtensions.deviationSince(quantityItem, startStored, SERVICE_ID);
        assertNotNull(deviation);
        QuantityType<?> qt = deviation.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(expected, qt.doubleValue(), 0.01);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        // default persistence service
        deviation = PersistenceExtensions.deviationSince(quantityItem, startStored);
        assertNull(deviation);
    }

    @Test
    public void testDeviationTillQuantityType() {
        ZonedDateTime endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        double expectedAverage = average(null, FUTURE_INTERMEDIATE_VALUE_3);

        double expected = Math.sqrt(DoubleStream
                .concat(DoubleStream.of(STATE.doubleValue()),
                        IntStream.rangeClosed(FUTURE_START, FUTURE_INTERMEDIATE_VALUE_3)
                                .mapToDouble(i -> Double.valueOf(i)))
                .map(d -> Math.pow(d - expectedAverage, 2)).sum()
                / (1 + FUTURE_INTERMEDIATE_VALUE_3 - FUTURE_START + 1));
        State deviation = PersistenceExtensions.deviationTill(quantityItem, endStored, SERVICE_ID);
        assertNotNull(deviation);
        QuantityType<?> qt = deviation.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(expected, qt.doubleValue(), 0.01);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        // default persistence service
        deviation = PersistenceExtensions.deviationTill(quantityItem, endStored);
        assertNull(deviation);
    }

    @Test
    public void testDeviationBetweenQuantityType() {
        ZonedDateTime startStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        double expectedAverage = average(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_INTERMEDIATE_VALUE_2);

        double expected = Math.sqrt(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_INTERMEDIATE_VALUE_2)
                .mapToDouble(i -> Double.parseDouble(Integer.toString(i))).map(d -> Math.pow(d - expectedAverage, 2))
                .sum() / (HISTORIC_INTERMEDIATE_VALUE_2 - HISTORIC_INTERMEDIATE_VALUE_1 + 1));
        State deviation = PersistenceExtensions.deviationBetween(quantityItem, startStored, endStored, SERVICE_ID);
        assertNotNull(deviation);
        QuantityType<?> qt = deviation.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(expected, qt.doubleValue(), 0.01);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        startStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        double expectedAverage2 = average(FUTURE_INTERMEDIATE_VALUE_3, FUTURE_INTERMEDIATE_VALUE_4);

        expected = Math.sqrt(IntStream.rangeClosed(FUTURE_INTERMEDIATE_VALUE_3, FUTURE_INTERMEDIATE_VALUE_4)
                .mapToDouble(i -> Double.valueOf(i)).map(d -> Math.pow(d - expectedAverage2, 2)).sum()
                / (FUTURE_INTERMEDIATE_VALUE_4 - FUTURE_INTERMEDIATE_VALUE_3 + 1));

        deviation = PersistenceExtensions.deviationBetween(quantityItem, startStored, endStored, SERVICE_ID);
        assertNotNull(deviation);
        qt = deviation.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(expected, qt.doubleValue(), 0.01);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        startStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        double expectedAverage3 = average(HISTORIC_INTERMEDIATE_VALUE_1, FUTURE_INTERMEDIATE_VALUE_3);

        expected = Math.sqrt(IntStream
                .concat(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_END),
                        IntStream.rangeClosed(FUTURE_START, FUTURE_INTERMEDIATE_VALUE_3))
                .mapToDouble(i -> Double.valueOf(i)).map(d -> Math.pow(d - expectedAverage3, 2)).sum()
                / (FUTURE_INTERMEDIATE_VALUE_3 - FUTURE_START + 1 + HISTORIC_END - HISTORIC_INTERMEDIATE_VALUE_1 + 1));

        deviation = PersistenceExtensions.deviationBetween(quantityItem, startStored, endStored, SERVICE_ID);
        assertNotNull(deviation);
        qt = deviation.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(expected, qt.doubleValue(), 0.01);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        // default persistence service
        deviation = PersistenceExtensions.deviationBetween(quantityItem, startStored, endStored);
        assertNull(deviation);
    }

    @Test
    public void testAverageSinceDecimalType() {
        ZonedDateTime start = ZonedDateTime.of(BEFORE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        double expected = average(BEFORE_START, null);
        State average = PersistenceExtensions.averageSince(numberItem, start, SERVICE_ID);
        assertNotNull(average);
        DecimalType dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(expected, dt.doubleValue(), 0.01);

        start = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        expected = average(HISTORIC_INTERMEDIATE_VALUE_1, null);
        average = PersistenceExtensions.averageSince(numberItem, start, SERVICE_ID);
        assertNotNull(average);
        dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(expected, dt.doubleValue(), 0.01);

        // default persistence service
        average = PersistenceExtensions.averageSince(numberItem, start);
        assertNull(average);
    }

    @Test
    public void testAverageTillDecimalType() {
        ZonedDateTime end = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        double expected = average(null, FUTURE_INTERMEDIATE_VALUE_3);
        State average = PersistenceExtensions.averageTill(numberItem, end, SERVICE_ID);
        assertNotNull(average);
        DecimalType dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(expected, dt.doubleValue(), 0.01);

        // default persistence service
        average = PersistenceExtensions.averageTill(numberItem, end);
        assertNull(average);
    }

    @Test
    public void testAverageBetweenDecimalType() {
        ZonedDateTime beginStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());

        double expected = average(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_INTERMEDIATE_VALUE_2);
        State average = PersistenceExtensions.averageBetween(numberItem, beginStored, endStored, SERVICE_ID);
        assertNotNull(average);
        DecimalType dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(expected, dt.doubleValue(), 0.01);

        beginStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        expected = average(FUTURE_INTERMEDIATE_VALUE_3, FUTURE_INTERMEDIATE_VALUE_4);

        average = PersistenceExtensions.averageBetween(numberItem, beginStored, endStored, SERVICE_ID);
        assertNotNull(average);
        dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(expected, 0.01)));

        beginStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        expected = average(HISTORIC_INTERMEDIATE_VALUE_1, FUTURE_INTERMEDIATE_VALUE_3);

        average = PersistenceExtensions.averageBetween(numberItem, beginStored, endStored, SERVICE_ID);
        assertNotNull(average);
        dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(expected, 0.01)));

        // default persistence service
        average = PersistenceExtensions.averageBetween(quantityItem, beginStored, endStored);
        assertNull(average);
    }

    @Test
    public void testAverageSinceQuantityType() {
        ZonedDateTime start = ZonedDateTime.of(BEFORE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        double expected = average(BEFORE_START, null);
        State average = PersistenceExtensions.averageSince(quantityItem, start, SERVICE_ID);
        assertNotNull(average);
        QuantityType<?> qt = average.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(expected, qt.doubleValue(), 0.01);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        start = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        expected = average(HISTORIC_INTERMEDIATE_VALUE_1, null);
        average = PersistenceExtensions.averageSince(quantityItem, start, SERVICE_ID);
        assertNotNull(average);
        qt = average.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(expected, qt.doubleValue(), 0.01);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        // default persistence service
        average = PersistenceExtensions.averageSince(quantityItem, start);
        assertNull(average);
    }

    @Test
    public void testAverageTillQuantityType() {
        ZonedDateTime end = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        double expected = average(null, FUTURE_INTERMEDIATE_VALUE_3);
        State average = PersistenceExtensions.averageTill(quantityItem, end, SERVICE_ID);
        assertNotNull(average);
        QuantityType<?> qt = average.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(expected, qt.doubleValue(), 0.01);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        // default persistence service
        average = PersistenceExtensions.averageTill(quantityItem, end);
        assertNull(average);
    }

    @Test
    public void testAverageBetweenQuantityType() {
        ZonedDateTime beginStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        ZonedDateTime endStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0,
                ZoneId.systemDefault());
        double expected = average(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_INTERMEDIATE_VALUE_2);
        State average = PersistenceExtensions.averageBetween(quantityItem, beginStored, endStored, SERVICE_ID);

        assertNotNull(average);
        QuantityType<?> qt = average.as(QuantityType.class);
        assertNotNull(qt);
        assertThat(qt.doubleValue(), is(closeTo(expected, 0.01)));
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        beginStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        expected = average(FUTURE_INTERMEDIATE_VALUE_3, FUTURE_INTERMEDIATE_VALUE_4);

        average = PersistenceExtensions.averageBetween(quantityItem, beginStored, endStored, SERVICE_ID);
        assertNotNull(average);
        qt = average.as(QuantityType.class);
        assertNotNull(qt);
        assertThat(qt.doubleValue(), is(closeTo(expected, 0.01)));
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        beginStored = ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        endStored = ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        expected = average(HISTORIC_INTERMEDIATE_VALUE_1, FUTURE_INTERMEDIATE_VALUE_3);

        average = PersistenceExtensions.averageBetween(quantityItem, beginStored, endStored, SERVICE_ID);
        assertNotNull(average);
        qt = average.as(QuantityType.class);
        assertNotNull(qt);
        assertThat(qt.doubleValue(), is(closeTo(expected, 0.01)));
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        // default persistence service
        average = PersistenceExtensions.averageBetween(quantityItem, beginStored, endStored);
        assertNull(average);
    }

    @Test
    public void testAverageSinceOnOffType() {
        // switch is 5h ON, 5h OFF, and 5h ON (until now)

        ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        State average = PersistenceExtensions.averageSince(switchItem, now.plusHours(SWITCH_START), SERVICE_ID);
        assertNotNull(average);
        DecimalType dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(),
                is(closeTo((SWITCH_OFF_1 - SWITCH_ON_1 - SWITCH_ON_2) / (-1.0 * SWITCH_START), 0.01)));

        average = PersistenceExtensions.averageSince(switchItem, now.plusHours(SWITCH_OFF_INTERMEDIATE_1), SERVICE_ID);
        assertNotNull(average);
        dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(-SWITCH_ON_2 / (-1.0 * SWITCH_OFF_INTERMEDIATE_1), 0.01)));

        average = PersistenceExtensions.averageSince(switchItem, now.plusHours(SWITCH_ON_2), SERVICE_ID);
        assertNotNull(average);
        dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(-SWITCH_ON_2 / (-1.0 * SWITCH_ON_2), 0.01)));

        average = PersistenceExtensions.averageSince(switchItem, now.plusHours(SWITCH_ON_INTERMEDIATE_21), SERVICE_ID);
        assertNotNull(average);
        dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(),
                is(closeTo(-SWITCH_ON_INTERMEDIATE_21 / (-1.0 * SWITCH_ON_INTERMEDIATE_21), 0.01)));

        average = PersistenceExtensions.averageSince(switchItem, now, SERVICE_ID);
        assertNotNull(average);
        dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(1d, 0.01)));

        average = PersistenceExtensions.averageSince(switchItem, now.plusHours(1), SERVICE_ID);
        assertNull(average);
    }

    @Test
    public void testAverageTillOnOffType() {
        // switch is 5h ON, 5h OFF, and 5h ON (from now)

        ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        State average = PersistenceExtensions.averageTill(switchItem, now.plusHours(SWITCH_END), SERVICE_ID);
        assertNotNull(average);
        DecimalType dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(),
                is(closeTo((SWITCH_OFF_3 - SWITCH_ON_3 + SWITCH_OFF_2) / (1.0 * SWITCH_END), 0.01)));

        average = PersistenceExtensions.averageTill(switchItem, now.plusHours(SWITCH_OFF_INTERMEDIATE_2), SERVICE_ID);
        assertNotNull(average);
        dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(SWITCH_OFF_2 / (1.0 * SWITCH_OFF_INTERMEDIATE_2), 0.01)));

        average = PersistenceExtensions.averageTill(switchItem, now.plusHours(SWITCH_ON_3), SERVICE_ID);
        assertNotNull(average);
        dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(SWITCH_OFF_2 / (1.0 * SWITCH_ON_3), 0.01)));

        average = PersistenceExtensions.averageTill(switchItem, now.plusHours(SWITCH_ON_INTERMEDIATE_22), SERVICE_ID);
        assertNotNull(average);
        dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(SWITCH_ON_INTERMEDIATE_22 / (1.0 * SWITCH_ON_INTERMEDIATE_22), 0.01)));

        average = PersistenceExtensions.averageTill(switchItem, now.plusMinutes(1), SERVICE_ID);
        assertNotNull(average);
        dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(1d, 0.01)));

        average = PersistenceExtensions.averageTill(switchItem, now.minusHours(1), SERVICE_ID);
        assertNull(average);
    }

    @Test
    public void testAverageSinceDecimalTypeIrregularTimespans() {
        TestCachedValuesPersistenceService persistenceService = new TestCachedValuesPersistenceService();
        new PersistenceExtensions(new PersistenceServiceRegistry() {

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
                return Set.of(persistenceService);
            }

            @Override
            public @Nullable PersistenceService get(@Nullable String serviceId) {
                return TestCachedValuesPersistenceService.ID.equals(serviceId) ? persistenceService : null;
            }
        }, timeZoneProviderMock);

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime beginStored = now.minusHours(27);

        persistenceService.addHistoricItem(beginStored, new DecimalType(0), TEST_NUMBER);
        persistenceService.addHistoricItem(beginStored.plusHours(1), new DecimalType(100), TEST_NUMBER);
        persistenceService.addHistoricItem(beginStored.plusHours(2), new DecimalType(0), TEST_NUMBER);
        persistenceService.addHistoricItem(beginStored.plusHours(25), new DecimalType(50), TEST_NUMBER);
        persistenceService.addHistoricItem(beginStored.plusHours(26), new DecimalType(0), TEST_NUMBER);
        numberItem.setState(new DecimalType(0));

        State average = PersistenceExtensions.averageSince(numberItem, beginStored,
                TestCachedValuesPersistenceService.ID);
        assertNotNull(average);
        DecimalType dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo((100.0 + 50.0) / 27.0, 0.01)));

        average = PersistenceExtensions.averageSince(numberItem, beginStored.plusHours(3),
                TestCachedValuesPersistenceService.ID);
        assertNotNull(average);
        dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(50.0 / 24.0, 0.01)));

        average = PersistenceExtensions.averageSince(numberItem, now.minusMinutes(30),
                TestCachedValuesPersistenceService.ID);
        assertNotNull(average);
        dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(0, 0.01)));
    }

    @Test
    public void testAverageTillDecimalTypeIrregularTimespans() {
        TestCachedValuesPersistenceService persistenceService = new TestCachedValuesPersistenceService();
        new PersistenceExtensions(new PersistenceServiceRegistry() {

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
                return Set.of(persistenceService);
            }

            @Override
            public @Nullable PersistenceService get(@Nullable String serviceId) {
                return TestCachedValuesPersistenceService.ID.equals(serviceId) ? persistenceService : null;
            }
        }, timeZoneProviderMock);

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime beginStored = now.plusHours(1);

        persistenceService.addHistoricItem(beginStored, new DecimalType(0), TEST_NUMBER);
        persistenceService.addHistoricItem(beginStored.plusHours(1), new DecimalType(0), TEST_NUMBER);
        persistenceService.addHistoricItem(beginStored.plusHours(2), new DecimalType(50), TEST_NUMBER);
        persistenceService.addHistoricItem(beginStored.plusHours(3), new DecimalType(0), TEST_NUMBER);
        persistenceService.addHistoricItem(beginStored.plusHours(25), new DecimalType(100), TEST_NUMBER);
        numberItem.setState(new DecimalType(0));

        State average = PersistenceExtensions.averageTill(numberItem, beginStored.plusHours(26),
                TestCachedValuesPersistenceService.ID);
        assertNotNull(average);
        DecimalType dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo((100.0 + 50.0) / 27.0, 0.01)));

        average = PersistenceExtensions.averageTill(numberItem, beginStored.plusHours(24),
                TestCachedValuesPersistenceService.ID);
        assertNotNull(average);
        dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(50.0 / 25.0, 0.01)));

        average = PersistenceExtensions.averageTill(numberItem, now.plusMinutes(30),
                TestCachedValuesPersistenceService.ID);
        assertNotNull(average);
        dt = average.as(DecimalType.class);
        assertNotNull(dt);
        assertThat(dt.doubleValue(), is(closeTo(0, 0.01)));
    }

    @Test
    public void testAverageBetweenZeroDuration() {
        ZonedDateTime now = ZonedDateTime.now();
        State state = PersistenceExtensions.averageBetween(quantityItem, now, now, SERVICE_ID);
        assertNotNull(state);
        QuantityType<?> qt = state.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(HISTORIC_END, qt.doubleValue(), 0.01);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());
    }

    @Test
    public void testSumSinceDecimalType() {
        State sum = PersistenceExtensions.sumSince(numberItem,
                ZonedDateTime.of(BEFORE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(sum);
        DecimalType dt = sum.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(IntStream.rangeClosed(HISTORIC_START, HISTORIC_END).sum(), dt.doubleValue(), 0.001);

        sum = PersistenceExtensions.sumSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(sum);
        dt = sum.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_END).sum(), dt.doubleValue(), 0.001);

        // default persistence service
        sum = PersistenceExtensions.sumSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(sum);
    }

    @Test
    public void testSumTillDecimalType() {
        State sum = PersistenceExtensions.sumTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(sum);
        DecimalType dt = sum.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(IntStream.rangeClosed(FUTURE_START, FUTURE_INTERMEDIATE_VALUE_3).sum(), dt.doubleValue(), 0.001);

        // default persistence service
        sum = PersistenceExtensions.sumTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(sum);
    }

    @Test
    public void testSumBetweenDecimalType() {
        State sum = PersistenceExtensions.sumBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(sum);
        DecimalType dt = sum.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_INTERMEDIATE_VALUE_2).sum(),
                dt.doubleValue(), 0.001);

        sum = PersistenceExtensions.sumBetween(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(sum);
        dt = sum.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(IntStream.rangeClosed(FUTURE_INTERMEDIATE_VALUE_3, FUTURE_INTERMEDIATE_VALUE_4).sum(),
                dt.doubleValue(), 0.001);

        sum = PersistenceExtensions.sumBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(sum);
        dt = sum.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(
                IntStream.concat(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_END),
                        IntStream.rangeClosed(FUTURE_START, FUTURE_INTERMEDIATE_VALUE_3)).sum(),
                dt.doubleValue(), 0.001);

        // default persistence service
        sum = PersistenceExtensions.sumBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(sum);
    }

    @Test
    public void testSumSinceQuantityType() {
        State sum = PersistenceExtensions.sumSince(quantityItem,
                ZonedDateTime.of(BEFORE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(sum);
        QuantityType<?> qt = sum.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(IntStream.rangeClosed(HISTORIC_START, HISTORIC_END).sum(), qt.doubleValue(), 0.001);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        sum = PersistenceExtensions.sumSince(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(sum);
        qt = sum.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_END).sum(), qt.doubleValue(), 0.001);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        // default persistence service
        sum = PersistenceExtensions.sumSince(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(sum);
    }

    @Test
    public void testSumTillQuantityType() {
        State sum = PersistenceExtensions.sumTill(quantityItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(sum);
        QuantityType<?> qt = sum.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(IntStream.rangeClosed(FUTURE_START, FUTURE_INTERMEDIATE_VALUE_3).sum(), qt.doubleValue(), 0.001);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        // default persistence service
        sum = PersistenceExtensions.sumSince(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(sum);
    }

    @Test
    public void testSumBetweenQuantityType() {
        State sum = PersistenceExtensions.sumBetween(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(sum);
        QuantityType<?> qt = sum.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_INTERMEDIATE_VALUE_2).sum(),
                qt.doubleValue(), 0.001);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        sum = PersistenceExtensions.sumBetween(quantityItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(sum);
        qt = sum.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(IntStream.rangeClosed(FUTURE_INTERMEDIATE_VALUE_3, FUTURE_INTERMEDIATE_VALUE_4).sum(),
                qt.doubleValue(), 0.001);

        sum = PersistenceExtensions.sumBetween(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(sum);
        qt = sum.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(
                IntStream.concat(IntStream.rangeClosed(HISTORIC_INTERMEDIATE_VALUE_1, HISTORIC_END),
                        IntStream.rangeClosed(FUTURE_START, FUTURE_INTERMEDIATE_VALUE_3)).sum(),
                qt.doubleValue(), 0.001);

        // default persistence service
        sum = PersistenceExtensions.sumBetween(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));

        assertNull(sum);
    }

    @Test
    public void testLastUpdate() {
        ZonedDateTime lastUpdate = PersistenceExtensions.lastUpdate(numberItem, SERVICE_ID);
        assertNotNull(lastUpdate);
        assertEquals(ZonedDateTime.of(HISTORIC_END, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), lastUpdate);

        // default persistence service
        lastUpdate = PersistenceExtensions.lastUpdate(numberItem);
        assertNull(lastUpdate);
    }

    @Test
    public void testNextUpdate() {
        ZonedDateTime nextUpdate = PersistenceExtensions.nextUpdate(numberItem, SERVICE_ID);
        assertNotNull(nextUpdate);
        assertEquals(ZonedDateTime.of(FUTURE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), nextUpdate);

        // default persistence service
        nextUpdate = PersistenceExtensions.lastUpdate(numberItem);
        assertNull(nextUpdate);
    }

    @Test
    public void testDeltaSince() {
        State delta = PersistenceExtensions.deltaSince(numberItem,
                ZonedDateTime.of(BEFORE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNull(delta);

        delta = PersistenceExtensions.deltaSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(delta);
        DecimalType dt = delta.as(DecimalType.class);
        assertNotNull(dt);
        DecimalType dtState = numberItem.getState().as(DecimalType.class);
        assertNotNull(dtState);
        assertEquals(dtState.doubleValue() - HISTORIC_INTERMEDIATE_VALUE_1, dt.doubleValue(), 0.001);

        delta = PersistenceExtensions.deltaSince(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(delta);
        QuantityType<?> qt = delta.as(QuantityType.class);
        assertNotNull(qt);
        QuantityType<?> qtState = quantityItem.getState().as(QuantityType.class);
        assertNotNull(qtState);
        assertEquals(qtState.doubleValue() - HISTORIC_INTERMEDIATE_VALUE_1, qt.doubleValue(), 0.001);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        // default persistence service
        delta = PersistenceExtensions.deltaSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(delta);
    }

    @Test
    public void testDeltaTill() {
        State delta = PersistenceExtensions.deltaTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(delta);
        DecimalType dt = delta.as(DecimalType.class);
        assertNotNull(dt);
        DecimalType dtState = numberItem.getState().as(DecimalType.class);
        assertNotNull(dtState);
        assertEquals(FUTURE_INTERMEDIATE_VALUE_3 - dtState.doubleValue(), dt.doubleValue(), 0.001);

        delta = PersistenceExtensions.deltaTill(quantityItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(delta);
        QuantityType<?> qt = delta.as(QuantityType.class);
        assertNotNull(qt);
        QuantityType<?> qtState = quantityItem.getState().as(QuantityType.class);
        assertNotNull(qtState);
        assertEquals(FUTURE_INTERMEDIATE_VALUE_3 - dtState.doubleValue(), qt.doubleValue(), 0.001);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        // default persistence service
        delta = PersistenceExtensions.deltaTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(delta);
    }

    @Test
    public void testDeltaBetween() {
        State delta = PersistenceExtensions.deltaBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(delta);
        DecimalType dt = delta.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(HISTORIC_INTERMEDIATE_VALUE_2 - HISTORIC_INTERMEDIATE_VALUE_1, dt.doubleValue(), 0.001);

        delta = PersistenceExtensions.deltaBetween(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(delta);
        QuantityType<?> qt = delta.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(HISTORIC_INTERMEDIATE_VALUE_2 - HISTORIC_INTERMEDIATE_VALUE_1, qt.doubleValue(), 0.001);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        delta = PersistenceExtensions.deltaBetween(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(delta);
        dt = delta.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(FUTURE_INTERMEDIATE_VALUE_4 - FUTURE_INTERMEDIATE_VALUE_3, dt.doubleValue(), 0.001);

        delta = PersistenceExtensions.deltaBetween(quantityItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(delta);
        qt = delta.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(FUTURE_INTERMEDIATE_VALUE_4 - FUTURE_INTERMEDIATE_VALUE_3, qt.doubleValue(), 0.001);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        delta = PersistenceExtensions.deltaBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(delta);
        dt = delta.as(DecimalType.class);
        assertNotNull(dt);
        assertEquals(FUTURE_INTERMEDIATE_VALUE_3 - HISTORIC_INTERMEDIATE_VALUE_1, dt.doubleValue(), 0.001);

        delta = PersistenceExtensions.deltaBetween(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(delta);
        qt = delta.as(QuantityType.class);
        assertNotNull(qt);
        assertEquals(FUTURE_INTERMEDIATE_VALUE_3 - HISTORIC_INTERMEDIATE_VALUE_1, qt.doubleValue(), 0.001);
        assertEquals(SIUnits.CELSIUS, qt.getUnit());

        // default persistence service
        delta = PersistenceExtensions.deltaBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(delta);
    }

    @Test
    public void testEvolutionRateSince() {
        DecimalType rate = PersistenceExtensions.evolutionRateSince(numberItem,
                ZonedDateTime.of(BEFORE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertThat(rate, is(nullValue()));

        rate = PersistenceExtensions.evolutionRateSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(rate);
        // ((now - then) / then) * 100
        assertThat(rate.doubleValue(),
                is(closeTo(
                        100.0 * (STATE.doubleValue() - HISTORIC_INTERMEDIATE_VALUE_1) / HISTORIC_INTERMEDIATE_VALUE_1,
                        0.001)));

        rate = PersistenceExtensions.evolutionRateSince(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(rate);
        // ((now - then) / then) * 100
        assertThat(rate.doubleValue(),
                is(closeTo(
                        100.0 * (STATE.doubleValue() - HISTORIC_INTERMEDIATE_VALUE_1) / HISTORIC_INTERMEDIATE_VALUE_1,
                        0.001)));

        // default persistence service
        rate = PersistenceExtensions.evolutionRateSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(rate);
    }

    @Test
    public void testEvolutionRateTill() {
        DecimalType rate = PersistenceExtensions.evolutionRateTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(rate);
        // ((then - now) / now) * 100
        assertThat(rate.doubleValue(),
                is(closeTo(100.0 * (FUTURE_INTERMEDIATE_VALUE_3 - STATE.doubleValue()) / STATE.doubleValue(), 0.001)));

        rate = PersistenceExtensions.evolutionRateTill(quantityItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(rate);
        // ((then - now) / now) * 100
        assertThat(rate.doubleValue(),
                is(closeTo(100.0 * (FUTURE_INTERMEDIATE_VALUE_3 - STATE.doubleValue()) / STATE.doubleValue(), 0.001)));

        // default persistence service
        rate = PersistenceExtensions.evolutionRateTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(rate);
    }

    @Test
    public void testEvolutionRateBetween() {
        DecimalType rate = PersistenceExtensions.evolutionRateBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(rate);
        // ((now - then) / then) * 100
        assertThat(rate.doubleValue(), is(closeTo(
                100.0 * (HISTORIC_INTERMEDIATE_VALUE_2 - HISTORIC_INTERMEDIATE_VALUE_1) / HISTORIC_INTERMEDIATE_VALUE_1,
                0.001)));

        rate = PersistenceExtensions.evolutionRateBetween(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(rate);
        // ((now - then) / then) * 100
        assertThat(rate.doubleValue(), is(closeTo(
                100.0 * (HISTORIC_INTERMEDIATE_VALUE_2 - HISTORIC_INTERMEDIATE_VALUE_1) / HISTORIC_INTERMEDIATE_VALUE_1,
                0.001)));

        rate = PersistenceExtensions.evolutionRateBetween(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(rate);
        // ((now - then) / then) * 100
        assertThat(rate.doubleValue(), is(closeTo(
                100.0 * (FUTURE_INTERMEDIATE_VALUE_4 - FUTURE_INTERMEDIATE_VALUE_3) / FUTURE_INTERMEDIATE_VALUE_3,
                0.001)));

        rate = PersistenceExtensions.evolutionRateBetween(quantityItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(rate);
        // ((now - then) / then) * 100
        assertThat(rate.doubleValue(), is(closeTo(
                100.0 * (FUTURE_INTERMEDIATE_VALUE_4 - FUTURE_INTERMEDIATE_VALUE_3) / FUTURE_INTERMEDIATE_VALUE_3,
                0.001)));

        rate = PersistenceExtensions.evolutionRateBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(rate);
        // ((now - then) / then) * 100
        assertThat(rate.doubleValue(), is(closeTo(
                100.0 * (FUTURE_INTERMEDIATE_VALUE_3 - HISTORIC_INTERMEDIATE_VALUE_1) / HISTORIC_INTERMEDIATE_VALUE_1,
                0.001)));

        rate = PersistenceExtensions.evolutionRateBetween(quantityItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertNotNull(rate);
        // ((now - then) / then) * 100
        assertThat(rate.doubleValue(), is(closeTo(
                100.0 * (FUTURE_INTERMEDIATE_VALUE_3 - HISTORIC_INTERMEDIATE_VALUE_1) / HISTORIC_INTERMEDIATE_VALUE_1,
                0.001)));

        // default persistence service
        rate = PersistenceExtensions.evolutionRateBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(rate);
    }

    @Test
    public void testPreviousStateDecimalTypeNoSkip() {
        HistoricItem prevStateItem = PersistenceExtensions.previousState(numberItem, false, SERVICE_ID);
        assertNotNull(prevStateItem);
        assertThat(prevStateItem.getState(), is(instanceOf(DecimalType.class)));
        assertEquals(value(HISTORIC_END), prevStateItem.getState());

        numberItem.setState(new DecimalType(4321));
        prevStateItem = PersistenceExtensions.previousState(numberItem, false, SERVICE_ID);
        assertNotNull(prevStateItem);
        assertEquals(value(HISTORIC_END), prevStateItem.getState());

        numberItem.setState(new DecimalType(HISTORIC_END));
        prevStateItem = PersistenceExtensions.previousState(numberItem, false, SERVICE_ID);
        assertNotNull(prevStateItem);
        assertEquals(value(HISTORIC_END), prevStateItem.getState());

        numberItem.setState(new DecimalType(3025));
        prevStateItem = PersistenceExtensions.previousState(numberItem, false, SERVICE_ID);
        assertNotNull(prevStateItem);
        assertEquals(value(HISTORIC_END), prevStateItem.getState());

        // default persistence service
        prevStateItem = PersistenceExtensions.previousState(numberItem, false);
        assertNull(prevStateItem);
    }

    @Test
    public void testPreviousStateQuantityTypeNoSkip() {
        HistoricItem prevStateItem = PersistenceExtensions.previousState(quantityItem, false, SERVICE_ID);
        assertNotNull(prevStateItem);
        assertThat(prevStateItem.getState(), is(instanceOf(QuantityType.class)));
        assertEquals(new QuantityType<>(value(HISTORIC_END), SIUnits.CELSIUS), prevStateItem.getState());

        quantityItem.setState(QuantityType.valueOf(4321, SIUnits.CELSIUS));
        prevStateItem = PersistenceExtensions.previousState(quantityItem, false, SERVICE_ID);
        assertNotNull(prevStateItem);
        assertEquals(new QuantityType<>(value(HISTORIC_END), SIUnits.CELSIUS), prevStateItem.getState());

        quantityItem.setState(QuantityType.valueOf(HISTORIC_END, SIUnits.CELSIUS));
        prevStateItem = PersistenceExtensions.previousState(quantityItem, false, SERVICE_ID);
        assertNotNull(prevStateItem);
        assertEquals(new QuantityType<>(value(HISTORIC_END), SIUnits.CELSIUS), prevStateItem.getState());

        quantityItem.setState(QuantityType.valueOf(3025, SIUnits.CELSIUS));
        prevStateItem = PersistenceExtensions.previousState(quantityItem, false, SERVICE_ID);
        assertNotNull(prevStateItem);
        assertEquals(new QuantityType<>(value(HISTORIC_END), SIUnits.CELSIUS), prevStateItem.getState());

        // default persistence service
        prevStateItem = PersistenceExtensions.previousState(quantityItem, false);
        assertNull(prevStateItem);
    }

    @Test
    public void testPreviousStateDecimalTypeSkip() {
        numberItem.setState(new DecimalType(HISTORIC_END));
        HistoricItem prevStateItem = PersistenceExtensions.previousState(numberItem, true, SERVICE_ID);
        assertNotNull(prevStateItem);
        assertEquals(value(HISTORIC_END - 1), prevStateItem.getState());

        // default persistence service
        prevStateItem = PersistenceExtensions.previousState(numberItem, true);
        assertNull(prevStateItem);
    }

    @Test
    public void testPreviousStateQuantityTypeSkip() {
        quantityItem.setState(QuantityType.valueOf(HISTORIC_END, SIUnits.CELSIUS));
        HistoricItem prevStateItem = PersistenceExtensions.previousState(quantityItem, true, SERVICE_ID);
        assertNotNull(prevStateItem);
        assertEquals(new QuantityType<>(value(HISTORIC_END - 1), SIUnits.CELSIUS), prevStateItem.getState());

        // default persistence service
        prevStateItem = PersistenceExtensions.previousState(quantityItem, true);
        assertNull(prevStateItem);
    }

    @Test
    public void testNextStateDecimalTypeNoSkip() {
        HistoricItem nextStateItem = PersistenceExtensions.nextState(numberItem, false, SERVICE_ID);
        assertNotNull(nextStateItem);
        assertThat(nextStateItem.getState(), is(instanceOf(DecimalType.class)));
        assertEquals(value(FUTURE_START), nextStateItem.getState());

        numberItem.setState(new DecimalType(4321));
        nextStateItem = PersistenceExtensions.nextState(numberItem, false, SERVICE_ID);
        assertNotNull(nextStateItem);
        assertEquals(value(FUTURE_START), nextStateItem.getState());

        numberItem.setState(new DecimalType(FUTURE_START));
        nextStateItem = PersistenceExtensions.nextState(numberItem, false, SERVICE_ID);
        assertNotNull(nextStateItem);
        assertEquals(value(FUTURE_START), nextStateItem.getState());

        numberItem.setState(new DecimalType(3025));
        nextStateItem = PersistenceExtensions.nextState(numberItem, false, SERVICE_ID);
        assertNotNull(nextStateItem);
        assertEquals(value(FUTURE_START), nextStateItem.getState());

        // default persistence service
        nextStateItem = PersistenceExtensions.nextState(numberItem, false);
        assertNull(nextStateItem);
    }

    @Test
    public void testNextStateQuantityTypeNoSkip() {
        HistoricItem nextStateItem = PersistenceExtensions.nextState(quantityItem, false, SERVICE_ID);
        assertNotNull(nextStateItem);
        assertThat(nextStateItem.getState(), is(instanceOf(QuantityType.class)));
        assertEquals(new QuantityType<>(value(FUTURE_START), SIUnits.CELSIUS), nextStateItem.getState());

        quantityItem.setState(QuantityType.valueOf(4321, SIUnits.CELSIUS));
        nextStateItem = PersistenceExtensions.nextState(quantityItem, false, SERVICE_ID);
        assertNotNull(nextStateItem);
        assertEquals(new QuantityType<>(value(FUTURE_START), SIUnits.CELSIUS), nextStateItem.getState());

        quantityItem.setState(QuantityType.valueOf(FUTURE_START, SIUnits.CELSIUS));
        nextStateItem = PersistenceExtensions.nextState(quantityItem, false, SERVICE_ID);
        assertNotNull(nextStateItem);
        assertEquals(new QuantityType<>(value(FUTURE_START), SIUnits.CELSIUS), nextStateItem.getState());

        quantityItem.setState(QuantityType.valueOf(3025, SIUnits.CELSIUS));
        nextStateItem = PersistenceExtensions.nextState(quantityItem, false, SERVICE_ID);
        assertNotNull(nextStateItem);
        assertEquals(new QuantityType<>(value(FUTURE_START), SIUnits.CELSIUS), nextStateItem.getState());

        // default persistence service
        nextStateItem = PersistenceExtensions.nextState(quantityItem, false);
        assertNull(nextStateItem);
    }

    @Test
    public void testNextStateDecimalTypeSkip() {
        numberItem.setState(new DecimalType(FUTURE_START));
        HistoricItem nextStateItem = PersistenceExtensions.nextState(numberItem, true, SERVICE_ID);
        assertNotNull(nextStateItem);
        assertEquals(value(FUTURE_START + 1), nextStateItem.getState());

        // default persistence service
        nextStateItem = PersistenceExtensions.nextState(numberItem, true);
        assertNull(nextStateItem);
    }

    @Test
    public void testNextStateQuantityTypeSkip() {
        quantityItem.setState(QuantityType.valueOf(FUTURE_START, SIUnits.CELSIUS));
        HistoricItem nextStateItem = PersistenceExtensions.nextState(quantityItem, true, SERVICE_ID);
        assertNotNull(nextStateItem);
        assertEquals(new QuantityType<>(value(FUTURE_START + 1), SIUnits.CELSIUS), nextStateItem.getState());

        // default persistence service
        nextStateItem = PersistenceExtensions.nextState(quantityItem, true);
        assertNull(nextStateItem);
    }

    @Test
    public void testChangedSince() {
        Boolean changed = PersistenceExtensions.changedSince(numberItem,
                ZonedDateTime.of(BEFORE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(changed, true);

        changed = PersistenceExtensions.changedSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(changed, true);

        // default persistence service
        changed = PersistenceExtensions.changedSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(changed);
    }

    @Test
    public void testChangedTill() {
        Boolean changed = PersistenceExtensions.changedTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(changed, true);

        // default persistence service
        changed = PersistenceExtensions.changedTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(changed);
    }

    @Test
    public void testChangedBetween() {
        Boolean changed = PersistenceExtensions.changedBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 5, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(changed, false);

        changed = PersistenceExtensions.changedBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(changed, true);

        changed = PersistenceExtensions.changedBetween(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 5, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(changed, false);

        changed = PersistenceExtensions.changedBetween(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(changed, true);

        // default persistence service
        changed = PersistenceExtensions.changedBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(changed);
    }

    @Test
    public void testUpdatedSince() {
        Boolean updated = PersistenceExtensions.updatedSince(numberItem,
                ZonedDateTime.of(BEFORE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(updated, true);

        updated = PersistenceExtensions.updatedSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(updated, true);

        // default persistence service
        updated = PersistenceExtensions.updatedSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(updated);
    }

    @Test
    public void testUpdatedTill() {
        Boolean updated = PersistenceExtensions.updatedTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(updated, true);

        // default persistence service
        updated = PersistenceExtensions.updatedTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(updated);
    }

    @Test
    public void testUpdatedBetween() {
        Boolean updated = PersistenceExtensions.updatedBetween(numberItem,
                ZonedDateTime.of(BEFORE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(updated, true);

        updated = PersistenceExtensions.updatedBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(updated, true);

        updated = PersistenceExtensions.updatedBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_NOVALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_NOVALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                SERVICE_ID);
        assertEquals(updated, false);

        updated = PersistenceExtensions.updatedBetween(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_NOVALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_NOVALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(updated, false);

        updated = PersistenceExtensions.updatedBetween(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(updated, true);

        // default persistence service
        updated = PersistenceExtensions.updatedBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(updated);
    }

    @Test
    public void testCountSince() {
        Long counts = PersistenceExtensions.countSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(HISTORIC_END - HISTORIC_INTERMEDIATE_VALUE_1 + 1, counts);

        counts = PersistenceExtensions.countSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(HISTORIC_END - HISTORIC_INTERMEDIATE_VALUE_2 + 1, counts);

        counts = PersistenceExtensions.countSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_NOVALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                SERVICE_ID);
        assertEquals(0, counts);

        // default persistence service
        counts = PersistenceExtensions.countSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(counts);
    }

    @Test
    public void testCountTill() {
        Long counts = PersistenceExtensions.countTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_NOVALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(0, counts);

        counts = PersistenceExtensions.countTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(FUTURE_INTERMEDIATE_VALUE_3 - FUTURE_START + 1, counts);

        counts = PersistenceExtensions.countTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(FUTURE_INTERMEDIATE_VALUE_4 - FUTURE_START + 1, counts);

        // default persistence service
        counts = PersistenceExtensions.countTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(counts);
    }

    @Test
    public void testCountBetween() {
        Long counts = PersistenceExtensions.countBetween(numberItem,
                ZonedDateTime.of(BEFORE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(HISTORIC_INTERMEDIATE_VALUE_1 - HISTORIC_START + 1, counts);

        counts = PersistenceExtensions.countBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(HISTORIC_INTERMEDIATE_VALUE_2 - HISTORIC_INTERMEDIATE_VALUE_1 + 1, counts);

        counts = PersistenceExtensions.countBetween(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(FUTURE_INTERMEDIATE_VALUE_4 - FUTURE_INTERMEDIATE_VALUE_3 + 1, counts);

        counts = PersistenceExtensions.countBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(FUTURE_INTERMEDIATE_VALUE_3 - FUTURE_START + 1 + HISTORIC_END - HISTORIC_INTERMEDIATE_VALUE_1 + 1,
                counts);

        // default persistence service
        counts = PersistenceExtensions.countBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(counts);
    }

    @Test
    public void testCountStateChangesSince() {
        Long counts = PersistenceExtensions.countStateChangesSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(HISTORIC_END - HISTORIC_INTERMEDIATE_VALUE_1, counts);

        counts = PersistenceExtensions.countStateChangesSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(HISTORIC_END - HISTORIC_INTERMEDIATE_VALUE_2, counts);

        counts = PersistenceExtensions.countStateChangesSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_NOVALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                SERVICE_ID);
        assertEquals(0, counts);

        // default persistence service
        counts = PersistenceExtensions.countStateChangesSince(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(counts);
    }

    @Test
    public void testCountStateChangesTill() {
        Long counts = PersistenceExtensions.countStateChangesTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_NOVALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(0, counts);

        counts = PersistenceExtensions.countStateChangesTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(FUTURE_INTERMEDIATE_VALUE_3 - FUTURE_START, counts);

        counts = PersistenceExtensions.countStateChangesTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(FUTURE_INTERMEDIATE_VALUE_4 - FUTURE_START, counts);

        // default persistence service
        counts = PersistenceExtensions.countStateChangesTill(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(counts);
    }

    @Test
    public void testCountStateChangesBetween() {
        Long counts = PersistenceExtensions.countStateChangesBetween(numberItem,
                ZonedDateTime.of(BEFORE_START, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(HISTORIC_INTERMEDIATE_VALUE_1 - HISTORIC_START, counts);

        counts = PersistenceExtensions.countStateChangesBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(HISTORIC_INTERMEDIATE_VALUE_2 - HISTORIC_INTERMEDIATE_VALUE_1, counts);

        counts = PersistenceExtensions.countBetween(numberItem,
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_4, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(FUTURE_INTERMEDIATE_VALUE_4 - FUTURE_INTERMEDIATE_VALUE_3 + 1, counts);

        counts = PersistenceExtensions.countBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(FUTURE_INTERMEDIATE_VALUE_3, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()), SERVICE_ID);
        assertEquals(FUTURE_INTERMEDIATE_VALUE_3 - FUTURE_START + 1 + HISTORIC_END - HISTORIC_INTERMEDIATE_VALUE_1 + 1,
                counts);

        // default persistence service
        counts = PersistenceExtensions.countStateChangesBetween(numberItem,
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_1, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
                ZonedDateTime.of(HISTORIC_INTERMEDIATE_VALUE_2, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        assertNull(counts);
    }
}
