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
package org.openhab.core.model.script.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemProvider;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.SmartHomeUnits;
import org.openhab.core.model.script.ScriptServiceUtil;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.types.State;

/**
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class ScriptEngineOSGiTest extends JavaOSGiTest {

    private static final String ITEM_NAME = "Switch1";
    private static final String NUMBER_ITEM_TEMPERATURE = "NumberA";
    private static final String NUMBER_ITEM_DECIMAL = "NumberB";
    private static final String NUMBER_ITEM_LENGTH = "NumberC";

    private @NonNullByDefault({}) ItemProvider itemProvider;
    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @NonNullByDefault({}) ScriptEngine scriptEngine;

    @Before
    public void setup() {
        registerVolatileStorageService();

        EventPublisher eventPublisher = event -> {
        };

        registerService(eventPublisher);

        itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);

        itemProvider = new ItemProvider() {

            @Override
            public void addProviderChangeListener(ProviderChangeListener<Item> listener) {
            }

            @Override
            public Collection<Item> getAll() {
                return Arrays.asList(new SwitchItem(ITEM_NAME),
                        createNumberItem(NUMBER_ITEM_TEMPERATURE, Temperature.class),
                        createNumberItem(NUMBER_ITEM_LENGTH, Length.class), new NumberItem(NUMBER_ITEM_DECIMAL));
            }

            @Override
            public void removeProviderChangeListener(ProviderChangeListener<Item> listener) {
            }
        };

        registerService(itemProvider);

        ScriptServiceUtil scriptServiceUtil = getService(ScriptServiceUtil.class);
        assertNotNull(scriptServiceUtil);
        scriptEngine = ScriptServiceUtil.getScriptEngine();
    }

    @After
    public void tearDown() {
        unregisterService(itemProvider);
    }

    @Test
    public void testInterpreter() throws ScriptParsingException, ScriptExecutionException {
        OnOffType switch1State = runScript("Switch1.state = ON;Switch1.state = OFF;Switch1.state = ON;Switch1.state");

        assertNotNull(switch1State);
        assertEquals("org.openhab.core.library.types.OnOffType", switch1State.getClass().getName());
        assertEquals("ON", switch1State.toString());
    }

    @SuppressWarnings("null")
    @Test
    public void testAssignQuantityType() throws ScriptParsingException, ScriptExecutionException {
        runScript("NumberA.state = 20.0|°C as org.openhab.core.types.State");

        State numberState = itemRegistry.get(NUMBER_ITEM_TEMPERATURE).getState();
        assertNotNull(numberState);
        assertEquals("org.openhab.core.library.types.QuantityType", numberState.getClass().getName());
        assertEquals("20.0 °C", numberState.toString());
    }

    @SuppressWarnings("null")
    @Test
    public void testGreaterThanWithItemState() throws ScriptExecutionException, ScriptParsingException {
        Item numberItem = itemRegistry.get(NUMBER_ITEM_TEMPERATURE);
        ((NumberItem) numberItem).setState(new QuantityType<>("20 °C"));

        assertTrue(runScript("NumberA.state > 20|°F"));
    }

    @Test
    public void testSpacesDoNotMatter() throws ScriptExecutionException, ScriptParsingException {
        assertTrue(runScript("20|°C == 20 | °C"));
        assertTrue(runScript("20|\"°C\" == 20 | \"°C\""));
    }

    @SuppressWarnings("null")
    @Test
    public void testGreaterEqualsWithItemState() throws ScriptExecutionException, ScriptParsingException {
        Item numberItem = itemRegistry.get(NUMBER_ITEM_TEMPERATURE);
        ((NumberItem) numberItem).setState(new QuantityType<>("20 °C"));

        assertTrue(runScript("NumberA.state >= 20|°C"));
    }

    @SuppressWarnings("null")
    @Test
    public void testLessThanWithItemState() throws ScriptExecutionException, ScriptParsingException {
        Item numberItem = itemRegistry.get(NUMBER_ITEM_TEMPERATURE);
        ((NumberItem) numberItem).setState(new QuantityType<>("20 °F"));

        assertTrue(runScript("NumberA.state < 20|°C"));
    }

    @SuppressWarnings("null")
    @Test
    public void testLessEqualsWithItemState() throws ScriptExecutionException, ScriptParsingException {
        Item numberItem = itemRegistry.get(NUMBER_ITEM_TEMPERATURE);
        ((NumberItem) numberItem).setState(new QuantityType<>("19 °F"));

        assertTrue(runScript("NumberA.state <= 20|°F"));
    }

    @SuppressWarnings("null")
    @Test
    public void testEqualsWithItemState() throws ScriptExecutionException, ScriptParsingException {
        Item numberItem = itemRegistry.get(NUMBER_ITEM_TEMPERATURE);
        ((NumberItem) numberItem).setState(new QuantityType<>("20 °C"));

        assertTrue(runScript("NumberA.state == 20|°C"));
    }

    @SuppressWarnings("null")
    @Test
    public void testNotEqualsWithItemState() throws ScriptExecutionException, ScriptParsingException {
        Item numberItem = itemRegistry.get(NUMBER_ITEM_TEMPERATURE);
        ((NumberItem) numberItem).setState(new QuantityType<>("20 °C"));

        assertTrue(runScript("NumberA.state != 10|°C"));
    }

    @SuppressWarnings("null")
    @Test
    public void testGreaterThanNumberNumber() throws ScriptParsingException, ScriptExecutionException {
        Item numberItem = itemRegistry.get(NUMBER_ITEM_DECIMAL);
        ((NumberItem) numberItem).setState(new DecimalType(20));

        assertTrue(runScript("NumberB.state > new DecimalType(19)"));
    }

    @Test
    public void testCompareGreaterThanQuantityType() throws ScriptParsingException, ScriptExecutionException {
        assertTrue(runScript("20.0|°C > 20|°F"));
    }

    @Test
    public void testCompareGreaterThanQuantityTypeFalse() throws ScriptParsingException, ScriptExecutionException {
        assertFalse(runScript("20.0|°F > 20|°C"));
    }

    @Test
    public void testCompareGreaterEqualsThanQuantityType() throws ScriptParsingException, ScriptExecutionException {
        assertTrue(runScript("1|m >= 100|cm"));
    }

    @Test
    public void testCompareLessThanQuantityTypeFalse() throws ScriptParsingException, ScriptExecutionException {
        assertFalse(runScript("20.0|°C < 20|°F"));
    }

    @Test
    public void testCompareLessThanQuantityType() throws ScriptParsingException, ScriptExecutionException {
        assertTrue(runScript("20.0|°F < 20|°C"));
    }

    @Test
    public void testCompareLessEqualsThanQuantityType() throws ScriptParsingException, ScriptExecutionException {
        assertTrue(runScript("100|cm <= 1|m"));
    }

    @Test
    public void testpostUpdateQuantityType() throws ScriptParsingException, ScriptExecutionException {
        scriptEngine.newScriptFromString("postUpdate(NumberA, 20.0|°C)").execute();
        scriptEngine.newScriptFromString("sendCommand(NumberA, 20.0|°F)").execute();
    }

    @Test
    public void testAssignAndCompareQuantityType() throws ScriptParsingException, ScriptExecutionException {
        assertFalse(runScript("NumberA.state = 20.0|°C as org.openhab.core.types.State; NumberA.state < 20|°F"));
    }

    @Test
    public void testAddQuantityType() throws ScriptParsingException, ScriptExecutionException {
        assertThat((QuantityType<?>) runScript("1|m + 20|cm"), is(QuantityType.valueOf("1.2 m")));
    }

    @Test
    public void testSubtractQuantityType() throws ScriptParsingException, ScriptExecutionException {
        assertThat((QuantityType<?>) runScript("1|m - 20|cm"), is(QuantityType.valueOf("0.8 m")));
    }

    @Test
    public void testMultiplyQuantityType() throws ScriptParsingException, ScriptExecutionException {
        assertThat((QuantityType<?>) runScript("1|m * 20|cm"), is(QuantityType.valueOf("2000 cm^2")));
    }

    @Test
    public void testMultiplyQuantityTypeNumber() throws ScriptParsingException, ScriptExecutionException {
        assertThat((QuantityType<?>) runScript("1|m * 20"), is(QuantityType.valueOf("20 m")));
    }

    @Test
    public void testDivideQuantityType() throws ScriptParsingException, ScriptExecutionException {
        assertThat((QuantityType<?>) runScript("1|m / 2|cm"), is(QuantityType.valueOf("50")));
    }

    @SuppressWarnings("null")
    @Test
    public void testDivideItemStateQuantityType() throws ScriptParsingException, ScriptExecutionException {
        Item numberItem = itemRegistry.get(NUMBER_ITEM_LENGTH);
        ((NumberItem) numberItem).setState(new QuantityType<>("1 m"));

        assertThat((QuantityType<?>) runScript("val length = NumberC.state as QuantityType; return length / 2|cm;"),
                is(QuantityType.valueOf("50")));
    }

    @Test
    public void testDivideQuantityTypeNumber() throws ScriptParsingException, ScriptExecutionException {
        assertThat((QuantityType<?>) runScript("1|m / 2"), is(QuantityType.valueOf("0.5 m")));
    }

    @Test
    public void testDivideNumberQuantityType() throws ScriptParsingException, ScriptExecutionException {
        assertThat((QuantityType<?>) runScript("1 / 2|m"), is(new QuantityType<>("0.5 one/m")));
    }

    @Test
    public void testDivideNumberQuantityType1() throws ScriptParsingException, ScriptExecutionException {
        assertThat((QuantityType<?>) runScript("0.5|\"one/m\" + 0.5|\"one/m\""), is(new QuantityType<>("1 one/m")));
    }

    @Test
    public void testDivideLengthTime() throws ScriptParsingException, ScriptExecutionException {
        assertThat((QuantityType<?>) runScript("100|km / 1|h"), is(new QuantityType<>("100 km/h")));
    }

    @Test
    public void testToUnitQuantityType() throws ScriptParsingException, ScriptExecutionException {
        assertThat(runScript("20|°C.toUnit(\"°F\")"), is(new QuantityType<>("68 °F")));
    }

    @Test
    public void testToUnitQuantityType2() throws ScriptParsingException, ScriptExecutionException {
        assertThat(runScript("new QuantityType(20, CELSIUS).toUnit('°F').doubleValue"), is(Double.valueOf(68)));
        assertThat(runScript("new QuantityType(68, FAHRENHEIT).toUnit('°C').doubleValue"), is(Double.valueOf(20)));
    }

    @Test
    public void testToUnitQuantityType3() throws ScriptParsingException, ScriptExecutionException {
        assertThat(runScript("new QuantityType(1, KELVIN)"), is(new QuantityType<>(1, SmartHomeUnits.KELVIN)));
        assertThat(runScript("new QuantityType(1, MICRO(KELVIN))"),
                is(new QuantityType<>(1, MetricPrefix.MICRO(SmartHomeUnits.KELVIN))));
    }

    @Test
    public void testEqualsQuantityTypeNumber() throws ScriptParsingException, ScriptExecutionException {
        assertThat(runScript("20|m.equals(20)"), is(false));
    }

    @Test
    public void testQuantityTypeUnitSymbols() throws ScriptParsingException, ScriptExecutionException {
        assertThat(runScript("20|m²"), is(new QuantityType<>(20, SIUnits.SQUARE_METRE)));
        assertThat(runScript("20|\"m**2\""), is(new QuantityType<>(20, SIUnits.SQUARE_METRE)));
        assertThat(runScript("20|m³"), is(new QuantityType<>(20, SIUnits.SQUARE_METRE.multiply(SIUnits.METRE))));
        assertThat(runScript("20|\"m**3\""), is(new QuantityType<>(20, SIUnits.SQUARE_METRE.multiply(SIUnits.METRE))));
        assertThat(runScript("1|\"µm\""), is(new QuantityType<>(1, MetricPrefix.MICRO(SIUnits.METRE))));
    }

    @Test
    public void testCompareQuantityTypeOneNumber() throws ScriptParsingException, ScriptExecutionException {
        assertThat(runScript("1 == 1|one"), is(true));
        assertThat(runScript("1|one == 1"), is(true));

        assertThat(runScript("1 != 2|one"), is(true));
        assertThat(runScript("2|one != 1"), is(true));

        assertThat(runScript("1 < 2|one"), is(true));
        assertThat(runScript("1|one < 2"), is(true));

        assertThat(runScript("1 <= 1|one"), is(true));
        assertThat(runScript("1|one <= 1"), is(true));

        assertThat(runScript("2 > 1|one"), is(true));
        assertThat(runScript("2|one > 1"), is(true));

        assertThat(runScript("1 >= 1|one"), is(true));
        assertThat(runScript("1|one >= 1"), is(true));
    }

    @Test
    public void testNoXbaseConflicts() throws ScriptParsingException, ScriptExecutionException {
        assertEquals(42, (int) runScript("(1..3).forEach[x |println(x)]; return 42;"));
        assertEquals(42, (int) runScript("92 % 50"));
        assertTrue((boolean) runScript("1 == 1 || 1 != 2"));
        assertEquals("\\", runScript("return \"\\\\\""));
    }

    private Item createNumberItem(String numberItemName, Class<?> dimension) {
        return new NumberItem("Number:" + dimension.getSimpleName(), numberItemName);
    }

    @SuppressWarnings("unchecked")
    private <T> T runScript(String script) throws ScriptExecutionException, ScriptParsingException {
        return (T) scriptEngine.newScriptFromString(script).execute();
    }
}
