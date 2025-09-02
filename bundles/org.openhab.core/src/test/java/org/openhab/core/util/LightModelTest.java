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
 * Unit tests for {@link LightModel}.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class LightModelTest {

    @Test
    public void testFullColor() {
        LightModel lsm = new LightModel(true, true, true, false, false, false);
        assertTrue(lsm.configGetSupportsColor());
        assertTrue(lsm.configGetSupportsBrightness());
        assertTrue(lsm.configGetSupportsColorTemperature());

        lsm.handleCommand(HSBType.RED);
        assertEquals(HSBType.RED, lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness(true));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));

        lsm.handleCommand(new PercentType(50));
        assertEquals(new PercentType(50), lsm.getBrightness(true));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));

        lsm.handleCommand(PercentType.ZERO);
        assertEquals(PercentType.ZERO, lsm.getBrightness(true));
        assertEquals(OnOffType.OFF, lsm.getOnOff(true));

        lsm.handleCommand(IncreaseDecreaseType.INCREASE);
        assertEquals(new PercentType(10), lsm.getBrightness(true));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));

        lsm.handleCommand(OnOffType.OFF);
        assertEquals(OnOffType.OFF, lsm.getOnOff(true));
        assertEquals(PercentType.ZERO, lsm.getBrightness(true));

        lsm.handleCommand(OnOffType.OFF);
        assertEquals(OnOffType.OFF, lsm.getOnOff(true));
        assertEquals(PercentType.ZERO, lsm.getBrightness(true));

        lsm.handleCommand(OnOffType.ON);
        assertEquals(OnOffType.ON, lsm.getOnOff(true));
        assertEquals(new PercentType(10), lsm.getBrightness(true));

        lsm.handleCommand(OnOffType.ON);
        assertEquals(OnOffType.ON, lsm.getOnOff(true));
        assertEquals(new PercentType(10), lsm.getBrightness(true));

        lsm.handleCommand(QuantityType.valueOf(500, Units.MIRED));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));
        assertEquals(new PercentType(10), lsm.getBrightness(true));
        assertEquals(QuantityType.valueOf(500, Units.MIRED), lsm.getColorTemperature());
        assertEquals(PercentType.HUNDRED, lsm.getColorTemperaturePercent());

        lsm.handleColorTemperatureCommand(QuantityType.valueOf(500, Units.MIRED));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));
        assertEquals(new PercentType(10), lsm.getBrightness(true));
        assertEquals(QuantityType.valueOf(500, Units.MIRED), lsm.getColorTemperature());
        assertEquals(PercentType.HUNDRED, lsm.getColorTemperaturePercent());

        lsm.handleColorTemperatureCommand(PercentType.ZERO);
        assertEquals(OnOffType.ON, lsm.getOnOff(true));
        assertEquals(new PercentType(10), lsm.getBrightness(true));
        assertEquals(QuantityType.valueOf(153, Units.MIRED), lsm.getColorTemperature());
        assertEquals(PercentType.ZERO, lsm.getColorTemperaturePercent());
    }

    @Test
    public void testColorWithoutColorTemperature() {
        LightModel lsm = new LightModel(true, false, true, false, false, false);
        assertTrue(lsm.configGetSupportsColor());
        assertTrue(lsm.configGetSupportsBrightness());
        assertFalse(lsm.configGetSupportsColorTemperature());

        lsm.handleCommand(HSBType.RED);
        assertEquals(HSBType.RED, lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness(true));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));

        lsm.handleCommand(QuantityType.valueOf(500, Units.MIRED));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));
        assertEquals(PercentType.HUNDRED, lsm.getBrightness(true));
        assertNull(lsm.getColorTemperature());
        assertNull(lsm.getColorTemperaturePercent());
        assertEquals(UnDefType.UNDEF, lsm.toNonNull(lsm.getColorTemperature()));
        assertEquals(UnDefType.UNDEF, lsm.toNonNull(lsm.getColorTemperaturePercent()));

        lsm.handleCommand(PercentType.ZERO);
        assertEquals(PercentType.ZERO, lsm.getBrightness(true));
        assertEquals(OnOffType.OFF, lsm.getOnOff(true));
    }

    @Test
    public void testBrightnessAndColorTemperature() {
        LightModel lsm = new LightModel(true, true, false, false, false, false);
        assertFalse(lsm.configGetSupportsColor());
        assertTrue(lsm.configGetSupportsBrightness());
        assertTrue(lsm.configGetSupportsColorTemperature());

        lsm.handleCommand(HSBType.RED);
        assertNull(lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness());
        assertEquals(OnOffType.ON, lsm.getOnOff(true));
        assertEquals(UnDefType.UNDEF, lsm.toNonNull(lsm.getColor()));

        lsm.handleCommand(QuantityType.valueOf(500, Units.MIRED));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));
        assertEquals(QuantityType.valueOf(500, Units.MIRED), lsm.getColorTemperature());
        assertEquals(PercentType.HUNDRED, lsm.getColorTemperaturePercent());

        lsm.handleCommand(PercentType.ZERO);
        assertEquals(PercentType.ZERO, lsm.getBrightness());
        assertEquals(OnOffType.OFF, lsm.getOnOff(true));
    }

    @Test
    public void testBrightnessOnly() {
        LightModel lsm = new LightModel(true, false, false, false, false, false);
        assertFalse(lsm.configGetSupportsColor());
        assertTrue(lsm.configGetSupportsBrightness());
        assertFalse(lsm.configGetSupportsColorTemperature());

        lsm.handleCommand(HSBType.RED);
        assertNull(lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness());
        assertEquals(OnOffType.ON, lsm.getOnOff(true));
        assertEquals(UnDefType.UNDEF, lsm.toNonNull(lsm.getColor()));

        lsm.handleCommand(QuantityType.valueOf(500, Units.MIRED));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));
        assertNull(lsm.getColorTemperature());
        assertNull(lsm.getColorTemperaturePercent());

        lsm.handleCommand(PercentType.ZERO);
        assertEquals(PercentType.ZERO, lsm.getBrightness());
        assertEquals(OnOffType.OFF, lsm.getOnOff(true));
    }

    @Test
    public void testOnOffOnly() {
        LightModel lsm = new LightModel(false, false, false, false, false, false);
        assertFalse(lsm.configGetSupportsColor());
        assertFalse(lsm.configGetSupportsBrightness());
        assertFalse(lsm.configGetSupportsColorTemperature());

        lsm.handleCommand(HSBType.RED);
        assertNull(lsm.getColor());
        assertNull(lsm.getBrightness());
        assertEquals(OnOffType.ON, lsm.getOnOff());
        assertEquals(UnDefType.UNDEF, lsm.toNonNull(lsm.getColor()));
        assertEquals(UnDefType.UNDEF, lsm.toNonNull(lsm.getBrightness()));

        lsm.handleCommand(QuantityType.valueOf(500, Units.MIRED));
        assertEquals(OnOffType.ON, lsm.getOnOff());
        assertNull(lsm.getColorTemperature());
        assertNull(lsm.getColorTemperaturePercent());

        lsm.handleCommand(PercentType.ZERO);
        assertNull(lsm.getBrightness());
        assertEquals(OnOffType.OFF, lsm.getOnOff());
    }

    @Test
    public void testColorTemperatureTracking() {
        LightModel lsm = new LightModel();

        lsm.handleCommand(HSBType.RED);
        assertEquals(HSBType.RED, lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness(true));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));

        lsm.handleCommand(QuantityType.valueOf(500, Units.MIRED));
        assertNotEquals(HSBType.RED, lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness(true));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));
        assertEquals(QuantityType.valueOf(500, Units.MIRED), lsm.getColorTemperature());
        assertEquals(PercentType.HUNDRED, lsm.getColorTemperaturePercent());

        lsm.handleCommand(QuantityType.valueOf(153, Units.MIRED));
        assertEquals(QuantityType.valueOf(153, Units.MIRED), lsm.getColorTemperature());
        assertEquals(PercentType.ZERO, lsm.getColorTemperaturePercent());

        lsm.handleColorTemperatureCommand(PercentType.HUNDRED);
        assertEquals(QuantityType.valueOf(500, Units.MIRED), lsm.getColorTemperature());
        assertEquals(PercentType.HUNDRED, lsm.getColorTemperaturePercent());

        lsm.handleColorTemperatureCommand(PercentType.ZERO);
        assertEquals(QuantityType.valueOf(153, Units.MIRED), lsm.getColorTemperature());
        assertEquals(PercentType.ZERO, lsm.getColorTemperaturePercent());
    }

    @Test
    public void testSimpleConstructor() {
        LightModel lsm = new LightModel();
        assertTrue(lsm.configGetSupportsColor());
        assertTrue(lsm.configGetSupportsBrightness());
        assertTrue(lsm.configGetSupportsColorTemperature());
        assertEquals(1.0, lsm.configGetMinimumOnBrightness());
        assertEquals(500.0, lsm.configGetMiredWarmest());
        assertEquals(153.0, lsm.configGetMiredCoolest());
        assertEquals(10.0, lsm.configGetIncreaseDecreaseStep());
    }

    @Test
    public void testComplexConstructor() {
        LightModel lsm = new LightModel(false, false, false, false, false, false, 2.0, 501.0, 154.0, 11.0);
        assertFalse(lsm.configGetSupportsColor());
        assertFalse(lsm.configGetSupportsBrightness());
        assertFalse(lsm.configGetSupportsColorTemperature());
        assertEquals(2.0, lsm.configGetMinimumOnBrightness());
        assertEquals(501.0, lsm.configGetMiredWarmest());
        assertEquals(154.0, lsm.configGetMiredCoolest());
        assertEquals(11.0, lsm.configGetIncreaseDecreaseStep());
    }

    @Test
    public void testCapabilitySetters() {
        LightModel lsm = new LightModel();
        lsm.configSetSupportsColor(false);
        lsm.configSetSupportsBrightness(false);
        lsm.configSetSupportsColorTemperature(false);

        assertFalse(lsm.configGetSupportsColor());
        assertFalse(lsm.configGetSupportsBrightness());
        assertFalse(lsm.configGetSupportsColorTemperature());
    }

    @Test
    public void testParameterSetters() {
        LightModel lsm = new LightModel();
        assertThrows(IllegalArgumentException.class, () -> lsm.configSetMinimumOnBrightness(0.0));
        lsm.configSetMinimumOnBrightness(2.0);
        lsm.configSetMiredWarmest(501.0);
        lsm.configSetMiredCoolest(154.0);
        lsm.configSetIncreaseDecreaseStep(11.0);

        assertEquals(2.0, lsm.configGetMinimumOnBrightness());
        assertEquals(501.0, lsm.configGetMiredWarmest());
        assertEquals(154.0, lsm.configGetMiredCoolest());
        assertEquals(11.0, lsm.configGetIncreaseDecreaseStep());
    }

    @Test
    public void testParameterSettersBad() {
        LightModel lsm = new LightModel();
        assertThrows(IllegalArgumentException.class, () -> lsm.configSetMinimumOnBrightness(0.0));
        assertThrows(IllegalArgumentException.class, () -> lsm.configSetMinimumOnBrightness(11.0));

        assertThrows(IllegalArgumentException.class, () -> lsm.configSetMiredWarmest(153.0));
        assertThrows(IllegalArgumentException.class, () -> lsm.configSetMiredWarmest(99.0));
        assertThrows(IllegalArgumentException.class, () -> lsm.configSetMiredWarmest(1001.0));

        assertThrows(IllegalArgumentException.class, () -> lsm.configSetMiredCoolest(501.0));
        assertThrows(IllegalArgumentException.class, () -> lsm.configSetMiredCoolest(99.0));
        assertThrows(IllegalArgumentException.class, () -> lsm.configSetMiredCoolest(1001.0));

        assertThrows(IllegalArgumentException.class, () -> lsm.configSetIncreaseDecreaseStep(0.0));
        assertThrows(IllegalArgumentException.class, () -> lsm.configSetIncreaseDecreaseStep(51.0));
    }

    @Test
    public void testCommandsBad() {
        LightModel lsm = new LightModel();
        assertThrows(IllegalArgumentException.class, () -> lsm.handleCommand(DecimalType.ZERO));
        assertThrows(IllegalArgumentException.class, () -> lsm.handleCommand(QuantityType.valueOf(5, Units.AMPERE)));
        assertThrows(IllegalArgumentException.class,
                () -> lsm.handleColorTemperatureCommand(QuantityType.valueOf(5, Units.AMPERE)));
        assertDoesNotThrow(() -> lsm.handleColorTemperatureCommand(OnOffType.ON));
    }

    @Test
    public void testComplexConstructorBad() {
        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, true, false, false, null, null, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, false, true, false, null, null, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, true, false, true, false, null, null, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, true, true, true, true, false, null, null, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, false, false, false, 0.0, null, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, false, false, false, 11.0, null, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, false, false, false, null, 99.0, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, false, false, false, null, 1001.0, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, false, false, false, null, null, 99.0, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, false, false, false, null, null, 1001.0, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, false, false, false, null, 300.0, 300.0, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, false, false, false, null, null, null, 0.0));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, false, false, false, null, null, null, 51.0));
    }

    @Test
    public void testRgb() {
        LightModel lsm = new LightModel(true, true, true, false, false, false);
        assertTrue(lsm.configGetSupportsColor());
        assertTrue(lsm.configGetSupportsBrightness());
        assertTrue(lsm.configGetSupportsColorTemperature());

        lsm.handleCommand(HSBType.RED);
        assertEquals(HSBType.RED, lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness(true));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));

        double[] rgb = lsm.getRGBx();
        assertEquals(3, rgb.length);
        assertEquals(255.0, rgb[0]);

        lsm.handleCommand(new PercentType(50));
        rgb = lsm.getRGBx();
        assertEquals(3, rgb.length);
        assertEquals(255.0, rgb[0]);

        /*
         * Nota Bene: in the case of rgbLinkedToBrightness == false the round trip setRGBx() followed by
         * getRGBx will NOT return identical values. But the ratio of the RGB values WILL be the same.
         */
        lsm.setRGBx(new double[] { 0.0, 100.0, 200.0 });
        rgb = lsm.getRGBx();
        assertEquals(3, rgb.length);
        assertEquals(0.0, rgb[0]);
        assertEquals(127.5, rgb[1]);
        assertEquals(255.0, rgb[2]);
    }

    @Test
    public void testRgbw() {
        LightModel lsm = new LightModel(true, true, true, true, false, false);
        assertTrue(lsm.configGetSupportsColor());
        assertTrue(lsm.configGetSupportsBrightness());
        assertTrue(lsm.configGetSupportsColorTemperature());

        lsm.handleCommand(HSBType.RED);
        assertEquals(HSBType.RED, lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness(true));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));

        double[] rgbw = lsm.getRGBx();
        assertEquals(4, rgbw.length);
        assertEquals(255.0, rgbw[0]);

        lsm.handleCommand(new PercentType(50));
        rgbw = lsm.getRGBx();
        assertEquals(4, rgbw.length);
        assertEquals(255.0, rgbw[0]);

        /*
         * Nota Bene: in this case with rgbLinkedToBrightness == false the round trip setRGBx() followed
         * by getRGBx will NOT return identical values. However the ratio of the RGBW values WILL be
         * the same.
         */
        lsm.setRGBx(new double[] { 0.0, 100.0, 200.0, 55.0 });// set full brightness 200 + 55 = 255
        rgbw = lsm.getRGBx();
        assertEquals(4, rgbw.length);
        assertEquals(0.0, rgbw[0]);
        assertEquals(100.0, rgbw[1], 0.1);
        assertEquals(200.0, rgbw[2], 0.1);
        assertEquals(55.0, rgbw[3], 0.1);
    }

    @Test
    public void testRgbDimming() {
        LightModel lsm = new LightModel(true, true, true, false, false, true);
        assertTrue(lsm.configGetSupportsColor());
        assertTrue(lsm.configGetSupportsBrightness());
        assertTrue(lsm.configGetSupportsColorTemperature());

        lsm.handleCommand(HSBType.RED);
        assertEquals(HSBType.RED, lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness(true));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));

        double[] rgb = lsm.getRGBx();
        assertEquals(3, rgb.length);
        assertEquals(255.0, rgb[0]);

        lsm.handleCommand(new PercentType(50));
        rgb = lsm.getRGBx();
        assertEquals(3, rgb.length);
        assertEquals(127.5, rgb[0]);

        /*
         * Nota Bene: in this case with rgbLinkedToBrightness == true the round trip setRGBx() followed
         * by getRGBx MUST return identical values. And the brightness MUST be adjusted.
         */
        lsm.setRGBx(new double[] { 0.0, 100.0, 200.0 });
        rgb = lsm.getRGBx();
        assertEquals(3, rgb.length);
        assertEquals(0.0, rgb[0]);
        assertEquals(100.0, rgb[1]);
        assertEquals(200.0, rgb[2]);
        PercentType brightness = lsm.getBrightness(true);
        assertNotNull(brightness);
        assertEquals(78.4, brightness.doubleValue(), 0.1); // 78.4 = 200 / 255
    }

    @Test
    public void testRgbwDimming() {
        LightModel lsm = new LightModel(true, true, true, true, false, true);
        assertTrue(lsm.configGetSupportsColor());
        assertTrue(lsm.configGetSupportsBrightness());
        assertTrue(lsm.configGetSupportsColorTemperature());

        lsm.handleCommand(HSBType.RED);
        assertEquals(HSBType.RED, lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness(true));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));

        double[] rgbw = lsm.getRGBx();
        assertEquals(4, rgbw.length);
        assertEquals(255.0, rgbw[0]);

        lsm.handleCommand(new PercentType(50));
        rgbw = lsm.getRGBx();
        assertEquals(4, rgbw.length);
        assertEquals(127.5, rgbw[0]);

        /*
         * Nota Bene: in this case with rgbLinkedToBrightness == true the round trip setRGBx() followed
         * by getRGBx MUST return identical values, and the brightness MUST be adjusted.
         */
        lsm.setRGBx(new double[] { 0.0, 100.0, 200.0, 55.0 }); // set full brightness 200 + 55 = 255

        rgbw = lsm.getRGBx();
        assertEquals(4, rgbw.length);
        assertEquals(0.0, rgbw[0]);
        assertEquals(100.0, rgbw[1], 0.1);
        assertEquals(200.0, rgbw[2], 0.1);
        assertEquals(55.0, rgbw[3], 0.1);
        PercentType brightness = lsm.getBrightness(true);
        assertNotNull(brightness);
        assertEquals(PercentType.HUNDRED, brightness);

        lsm.setRGBx(new double[] { 0.0, 100.0, 200.0, 0.0 });
        brightness = lsm.getBrightness(true);
        assertNotNull(brightness);
        assertEquals(78.4, brightness.doubleValue(), 0.1); // 78.4 = 200 / 255

        lsm.setRGBx(new double[] { 0.0, 100.0, 100.0, 100.0 });
        brightness = lsm.getBrightness(true);
        assertNotNull(brightness);
        assertEquals(78.4, brightness.doubleValue(), 0.1); // 78.4 = 200 / 255
    }

    @Test
    public void testSparseChannelRuleCompliance() {
        LightModel lsm;

        // supports color
        lsm = new LightModel(true, false, true, false, false, false);
        lsm.handleCommand(HSBType.RED);

        assertEquals(HSBType.RED, lsm.getColor());
        assertNull(lsm.getBrightness());
        assertNull(lsm.getOnOff());
        assertNotNull(lsm.getBrightness(true));
        assertNotNull(lsm.getOnOff(true));

        // supports brightness
        lsm = new LightModel(true, false, false, false, false, false);
        lsm.handleCommand(HSBType.RED);

        assertNull(lsm.getColor());
        assertNotNull(lsm.getBrightness());
        assertNull(lsm.getOnOff());
        assertNotNull(lsm.getOnOff(true));

        // supports on/off
        lsm = new LightModel(false, false, false, false, false, false);
        lsm.handleCommand(HSBType.RED);

        assertNull(lsm.getColor());
        assertNull(lsm.getBrightness());
        assertNotNull(lsm.getOnOff());
    }

    @Test
    public void testRgbcw() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);

        lsm.handleCommand(HSBType.RED);
        assertEquals(HSBType.RED, lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness(true));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));

        double[] rgbcw = lsm.getRGBx();
        assertEquals(5, rgbcw.length);
        assertEquals(255.0, rgbcw[0]);

        lsm.handleCommand(new PercentType(50));
        rgbcw = lsm.getRGBx();
        assertEquals(5, rgbcw.length);
        assertEquals(255.0, rgbcw[0]);

        /*
         * Nota Bene: in the case of rgbLinkedToBrightness == false the round trip setRGBx() followed by
         * getRGBx will NOT return identical values. But the ratio of the RGB values WILL be the same.
         */
        lsm.setRGBx(new double[] { 0.0, 100.0, 200.0, 20.0, 35.0 }); // set full brightness 200 + 20 + 35 = 255
        rgbcw = lsm.getRGBx();
        assertEquals(5, rgbcw.length);
        assertEquals(0.0, rgbcw[0], 0.1);
        assertEquals(100.0, rgbcw[1], 0.1);
        assertEquals(200.0, rgbcw[2], 0.1);
        assertEquals(20.0, rgbcw[3], 0.1);
        assertEquals(35.0, rgbcw[4], 0.1);

        PercentType brightness = lsm.getBrightness(true);
        assertNotNull(brightness);
        assertEquals(50.0, brightness.doubleValue(), 0.1);
        assertEquals(374.0, lsm.getMired(), 0.5);
    }

    @Test
    public void testRgbcwDimming() {
        LightModel lsm = new LightModel(true, true, true, false, true, true);

        lsm.handleCommand(HSBType.RED);
        assertEquals(HSBType.RED, lsm.getColor());
        assertEquals(PercentType.HUNDRED, lsm.getBrightness(true));
        assertEquals(OnOffType.ON, lsm.getOnOff(true));

        double[] rgbcw = lsm.getRGBx();
        assertEquals(5, rgbcw.length);
        assertEquals(255.0, rgbcw[0]);

        lsm.handleCommand(new PercentType(50));
        rgbcw = lsm.getRGBx();
        assertEquals(5, rgbcw.length);
        assertEquals(127.5, rgbcw[0]);

        /*
         * Nota Bene: in this case with rgbLinkedToBrightness == true the round trip setRGBx() followed
         * by getRGBx MUST return identical values, and the brightness MUST be adjusted.
         */
        lsm.setRGBx(new double[] { 0.0, 100.0, 200.0, 20.0, 35.0 }); // set full brightness 200 + 20 + 35 = 255
        rgbcw = lsm.getRGBx();
        assertEquals(5, rgbcw.length);
        assertEquals(0.0, rgbcw[0], 0.1);
        assertEquals(100.0, rgbcw[1], 0.1);
        assertEquals(200.0, rgbcw[2], 0.1);
        assertEquals(20.0, rgbcw[3], 0.1);
        assertEquals(35.0, rgbcw[4], 0.1);

        PercentType brightness = lsm.getBrightness(true);
        assertNotNull(brightness);
        assertEquals(100.0, brightness.doubleValue(), 0.1);
        assertEquals(374.0, lsm.getMired(), 0.5);
    }
}
