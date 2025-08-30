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
package org.openhab.core.util;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.UnDefType;

/**
 * Test for {@link LightStateMachine}.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class LightStateMachineTest {

    @Test
    public void testFullColor() {
        LightStateMachine lsm = new LightStateMachine(true, true, true);
        assertTrue(lsm.supportsColor());
        assertTrue(lsm.supportsBrightness());
        assertTrue(lsm.supportsColorTemperature());

        lsm.handleCommand(HSBType.RED);
        assertEquals(HSBType.RED, lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness());
        assertEquals(OnOffType.ON, lsm.getOnOff());

        lsm.handleCommand(new PercentType(50));
        assertEquals(new PercentType(50), lsm.getBrightness());
        assertEquals(OnOffType.ON, lsm.getOnOff());

        lsm.handleCommand(PercentType.ZERO);
        assertEquals(PercentType.ZERO, lsm.getBrightness());
        assertEquals(OnOffType.OFF, lsm.getOnOff());

        lsm.handleCommand(IncreaseDecreaseType.INCREASE);
        assertEquals(new PercentType(10), lsm.getBrightness());
        assertEquals(OnOffType.ON, lsm.getOnOff());

        lsm.handleCommand(OnOffType.OFF);
        assertEquals(OnOffType.OFF, lsm.getOnOff());
        assertEquals(PercentType.ZERO, lsm.getBrightness());

        lsm.handleCommand(OnOffType.OFF);
        assertEquals(OnOffType.OFF, lsm.getOnOff());
        assertEquals(PercentType.ZERO, lsm.getBrightness());

        lsm.handleCommand(OnOffType.ON);
        assertEquals(OnOffType.ON, lsm.getOnOff());
        assertEquals(new PercentType(10), lsm.getBrightness());

        lsm.handleCommand(OnOffType.ON);
        assertEquals(OnOffType.ON, lsm.getOnOff());
        assertEquals(new PercentType(10), lsm.getBrightness());

        lsm.handleCommand(QuantityType.valueOf(500, Units.MIRED));
        assertEquals(OnOffType.ON, lsm.getOnOff());
        assertEquals(new PercentType(10), lsm.getBrightness());
        assertEquals(QuantityType.valueOf(500, Units.MIRED), lsm.getColorTemperature());
        assertEquals(PercentType.HUNDRED, lsm.getColorTemperaturePercent());

        lsm.handleColorTemperatureCommand(QuantityType.valueOf(500, Units.MIRED));
        assertEquals(OnOffType.ON, lsm.getOnOff());
        assertEquals(new PercentType(10), lsm.getBrightness());
        assertEquals(QuantityType.valueOf(500, Units.MIRED), lsm.getColorTemperature());
        assertEquals(PercentType.HUNDRED, lsm.getColorTemperaturePercent());

        lsm.handleColorTemperatureCommand(PercentType.ZERO);
        assertEquals(OnOffType.ON, lsm.getOnOff());
        assertEquals(new PercentType(10), lsm.getBrightness());
        assertEquals(QuantityType.valueOf(153, Units.MIRED), lsm.getColorTemperature());
        assertEquals(PercentType.ZERO, lsm.getColorTemperaturePercent());
    }

    @Test
    public void testColorWithoutColorTemperature() {
        LightStateMachine lsm = new LightStateMachine(true, false, true);
        assertTrue(lsm.supportsColor());
        assertTrue(lsm.supportsBrightness());
        assertFalse(lsm.supportsColorTemperature());

        lsm.handleCommand(HSBType.RED);
        assertEquals(HSBType.RED, lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness());
        assertEquals(OnOffType.ON, lsm.getOnOff());

        lsm.handleCommand(QuantityType.valueOf(500, Units.MIRED));
        assertEquals(OnOffType.ON, lsm.getOnOff());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness());
        assertNull(lsm.getColorTemperature());
        assertNull(lsm.getColorTemperaturePercent());
        assertEquals(UnDefType.UNDEF, LightStateMachine.requireNonNull(lsm.getColorTemperature()));
        assertEquals(UnDefType.UNDEF, LightStateMachine.requireNonNull(lsm.getColorTemperaturePercent()));

        lsm.handleCommand(PercentType.ZERO);
        assertEquals(PercentType.ZERO, lsm.getBrightness());
        assertEquals(OnOffType.OFF, lsm.getOnOff());
    }

    @Test
    public void testBrightnessAndColorTemperature() {
        LightStateMachine lsm = new LightStateMachine(true, true, false);
        assertFalse(lsm.supportsColor());
        assertTrue(lsm.supportsBrightness());
        assertTrue(lsm.supportsColorTemperature());

        lsm.handleCommand(HSBType.RED);
        assertNull(lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness());
        assertEquals(OnOffType.ON, lsm.getOnOff());
        assertEquals(UnDefType.UNDEF, LightStateMachine.requireNonNull(lsm.getColor()));

        lsm.handleCommand(QuantityType.valueOf(500, Units.MIRED));
        assertEquals(OnOffType.ON, lsm.getOnOff());
        assertEquals(QuantityType.valueOf(500, Units.MIRED), lsm.getColorTemperature());
        assertEquals(PercentType.HUNDRED, lsm.getColorTemperaturePercent());

        lsm.handleCommand(PercentType.ZERO);
        assertEquals(PercentType.ZERO, lsm.getBrightness());
        assertEquals(OnOffType.OFF, lsm.getOnOff());
    }

    @Test
    public void testBrightnessOnly() {
        LightStateMachine lsm = new LightStateMachine(true, false, false);
        assertFalse(lsm.supportsColor());
        assertTrue(lsm.supportsBrightness());
        assertFalse(lsm.supportsColorTemperature());

        lsm.handleCommand(HSBType.RED);
        assertNull(lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness());
        assertEquals(OnOffType.ON, lsm.getOnOff());
        assertEquals(UnDefType.UNDEF, LightStateMachine.requireNonNull(lsm.getColor()));

        lsm.handleCommand(QuantityType.valueOf(500, Units.MIRED));
        assertEquals(OnOffType.ON, lsm.getOnOff());
        assertNull(lsm.getColorTemperature());
        assertNull(lsm.getColorTemperaturePercent());

        lsm.handleCommand(PercentType.ZERO);
        assertEquals(PercentType.ZERO, lsm.getBrightness());
        assertEquals(OnOffType.OFF, lsm.getOnOff());
    }

    @Test
    public void testOnOffOnly() {
        LightStateMachine lsm = new LightStateMachine(false, false, false);
        assertFalse(lsm.supportsColor());
        assertFalse(lsm.supportsBrightness());
        assertFalse(lsm.supportsColorTemperature());

        lsm.handleCommand(HSBType.RED);
        assertNull(lsm.getColor());
        assertNull(lsm.getBrightness());
        assertEquals(OnOffType.ON, lsm.getOnOff());
        assertEquals(UnDefType.UNDEF, LightStateMachine.requireNonNull(lsm.getColor()));
        assertEquals(UnDefType.UNDEF, LightStateMachine.requireNonNull(lsm.getBrightness()));

        lsm.handleCommand(QuantityType.valueOf(500, Units.MIRED));
        assertEquals(OnOffType.ON, lsm.getOnOff());
        assertNull(lsm.getColorTemperature());
        assertNull(lsm.getColorTemperaturePercent());

        lsm.handleCommand(PercentType.ZERO);
        assertNull(lsm.getBrightness());
        assertEquals(OnOffType.OFF, lsm.getOnOff());
    }

    @Test
    public void testSimpleConstructor() {
        LightStateMachine lsm = new LightStateMachine();
        assertFalse(lsm.supportsColor());
        assertFalse(lsm.supportsBrightness());
        assertFalse(lsm.supportsColorTemperature());
        assertEquals(1.0, lsm.getMinimumOnBrightness());
        assertEquals(500.0, lsm.getWarmestMired());
        assertEquals(153.0, lsm.getCoolestMired());
        assertEquals(10.0, lsm.getIncreaseDecreaseStep());
    }

    @Test
    public void testComplexConstructor() {
        LightStateMachine lsm = new LightStateMachine(false, false, false, 2.0, 501.0, 154.0, 11.0);
        assertFalse(lsm.supportsColor());
        assertFalse(lsm.supportsBrightness());
        assertFalse(lsm.supportsColorTemperature());
        assertEquals(2.0, lsm.getMinimumOnBrightness());
        assertEquals(501.0, lsm.getWarmestMired());
        assertEquals(154.0, lsm.getCoolestMired());
        assertEquals(11.0, lsm.getIncreaseDecreaseStep());
    }

    @Test
    public void testCapabilitySetters() {
        LightStateMachine lsm = new LightStateMachine();
        lsm.setSupportsColor(true);
        lsm.setSupportsBrightness(true);
        lsm.setSupportsColorTemperature(true);

        assertTrue(lsm.supportsColor());
        assertTrue(lsm.supportsBrightness());
        assertTrue(lsm.supportsColorTemperature());
    }

    @Test
    public void testParameterSetters() {
        LightStateMachine lsm = new LightStateMachine();
        assertThrows(IllegalArgumentException.class, () -> lsm.setMinimumOnBrightness(0.0));
        lsm.setMinimumOnBrightness(2.0);
        lsm.setWarmestMired(501.0);
        lsm.setCoolestMired(154.0);
        lsm.setIncreaseDecreaseStep(11.0);

        assertEquals(2.0, lsm.getMinimumOnBrightness());
        assertEquals(501.0, lsm.getWarmestMired());
        assertEquals(154.0, lsm.getCoolestMired());
        assertEquals(11.0, lsm.getIncreaseDecreaseStep());
    }

    @Test
    public void testParameterSettersBad() {
        LightStateMachine lsm = new LightStateMachine();
        assertThrows(IllegalArgumentException.class, () -> lsm.setMinimumOnBrightness(0.0));
        assertThrows(IllegalArgumentException.class, () -> lsm.setMinimumOnBrightness(11.0));

        assertThrows(IllegalArgumentException.class, () -> lsm.setWarmestMired(153.0));
        assertThrows(IllegalArgumentException.class, () -> lsm.setWarmestMired(99.0));
        assertThrows(IllegalArgumentException.class, () -> lsm.setWarmestMired(1001.0));

        assertThrows(IllegalArgumentException.class, () -> lsm.setCoolestMired(501.0));
        assertThrows(IllegalArgumentException.class, () -> lsm.setCoolestMired(99.0));
        assertThrows(IllegalArgumentException.class, () -> lsm.setCoolestMired(1001.0));

        assertThrows(IllegalArgumentException.class, () -> lsm.setIncreaseDecreaseStep(0.0));
        assertThrows(IllegalArgumentException.class, () -> lsm.setIncreaseDecreaseStep(51.0));
    }

    @Test
    public void testCommandsBad() {
        LightStateMachine lsm = new LightStateMachine();
        assertThrows(IllegalArgumentException.class, () -> lsm.handleCommand(DecimalType.ZERO));
        assertThrows(IllegalArgumentException.class, () -> lsm.handleCommand(QuantityType.valueOf(5, Units.AMPERE)));
        assertThrows(IllegalArgumentException.class, () -> lsm.handleColorTemperatureCommand(OnOffType.ON));
        assertThrows(IllegalArgumentException.class,
                () -> lsm.handleColorTemperatureCommand(QuantityType.valueOf(5, Units.AMPERE)));
    }

    @Test
    public void testComplexConstructorBad() {
        assertThrows(IllegalArgumentException.class,
                () -> new LightStateMachine(false, false, false, 0.0, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new LightStateMachine(false, false, false, 11.0, null, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightStateMachine(false, false, false, null, 99.0, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new LightStateMachine(false, false, false, null, 1001.0, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightStateMachine(false, false, false, null, null, 99.0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new LightStateMachine(false, false, false, null, null, 1001.0, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightStateMachine(false, false, false, null, 300.0, 300.0, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightStateMachine(false, false, false, null, null, null, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> new LightStateMachine(false, false, false, null, null, null, 51.0));
    }
}
