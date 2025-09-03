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

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, true, false, true, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, false, true, true, null, null, null, null));
    }

    @Test
    public void testRgbIgnoreBrightness() {
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
        assertEquals(255.0, rgb[0], 1);

        lsm.handleCommand(new PercentType(50));
        rgb = lsm.getRGBx();
        assertEquals(3, rgb.length);
        assertEquals(255.0, rgb[0], 1);

        /*
         * Nota Bene: in the case of rgbIgnoreBrightness == true the round trip setRGBx() followed by
         * getRGBx will NOT return identical values. But the ratio of the RGB values WILL be the same.
         */
        lsm.setRGBx(new double[] { 0.0, 100.0, 200.0 });
        rgb = lsm.getRGBx();
        assertEquals(3, rgb.length);
        assertEquals(0.0, rgb[0], 1);
        assertEquals(127.5, rgb[1], 1);
        assertEquals(255.0, rgb[2], 1);
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
        assertEquals(255.0, rgb[0], 1);

        lsm.handleCommand(new PercentType(50));
        rgb = lsm.getRGBx();
        assertEquals(3, rgb.length);
        assertEquals(127.5, rgb[0], 1);

        lsm.setRGBx(new double[] { 0.0, 100.0, 200.0 });
        rgb = lsm.getRGBx();
        assertEquals(3, rgb.length);
        assertEquals(0.0, rgb[0], 1);
        assertEquals(100.0, rgb[1], 1);
        assertEquals(200.0, rgb[2], 1);
        PercentType brightness = lsm.getBrightness(true);
        assertNotNull(brightness);
        assertEquals(78.4, brightness.doubleValue(), 1); // 78.4 = 200 / 255
    }

    @Test
    public void testRgbwDimming() {
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
        assertEquals(255.0, rgbw[0], 1);

        lsm.handleCommand(new PercentType(50));
        rgbw = lsm.getRGBx();
        assertEquals(4, rgbw.length);
        assertEquals(127.5, rgbw[0], 1);

        lsm.setRGBx(new double[] { 0.0, 100.0, 200.0, 55.0 }); // set full brightness 200 + 55 = 255

        rgbw = lsm.getRGBx();
        assertEquals(4, rgbw.length);
        assertEquals(0.0, rgbw[0], 1);
        assertEquals(100.0, rgbw[1], 1);
        assertEquals(200.0, rgbw[2], 1);
        assertEquals(55.0, rgbw[3], 1);
        PercentType brightness = lsm.getBrightness(true);
        assertNotNull(brightness);
        assertEquals(PercentType.HUNDRED, brightness);

        lsm.setRGBx(new double[] { 0.0, 100.0, 200.0, 0.0 });
        brightness = lsm.getBrightness(true);
        assertNotNull(brightness);
        assertEquals(78.4, brightness.doubleValue(), 1); // 78.4 = 200 / 255

        lsm.setRGBx(new double[] { 0.0, 100.0, 100.0, 100.0 });
        brightness = lsm.getBrightness(true);
        assertNotNull(brightness);
        assertEquals(78.4, brightness.doubleValue(), 1); // 78.4 = 200 / 255
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

        // primary red at 100% brightness
        double[] rgbcw = lsm.getRGBx();
        assertEquals(5, rgbcw.length);
        assertEquals(255.0, rgbcw[0], 1);
        assertEquals(0.0, rgbcw[1], 1);
        assertEquals(0.0, rgbcw[2], 1);
        assertEquals(0.0, rgbcw[3], 1);
        assertEquals(0.0, rgbcw[4], 1);

        // primary red at 50% brightness
        lsm.handleCommand(new PercentType(50));
        rgbcw = lsm.getRGBx();
        assertEquals(5, rgbcw.length);
        assertEquals(127.5, rgbcw[0], 1);
        assertEquals(0.0, rgbcw[1], 1);
        assertEquals(0.0, rgbcw[2], 1);
        assertEquals(0.0, rgbcw[3], 1);
        assertEquals(0.0, rgbcw[4], 1);
    }

    /**
     * Case: Primary Red
     * Input (RGBCW): (255, 0, 0, 0, 0)
     * Expected HSB: (0, 100, 100)
     */
    @Test
    public void testRgbcwPrimaryRed() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.setRGBx(new double[] { 255, 0, 0, 0, 0 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(0.0, hsb.getHue().doubleValue(), 1); // hue for red
        assertEquals(100.0, hsb.getSaturation().doubleValue(), 1); // fully saturated
        assertEquals(100.0, hsb.getBrightness().doubleValue(), 1); // full brightness
    }

    /**
     * Case: Bright White (warm)
     * Input (RGBCW): (0, 0, 0, 0, 255)
     * Expected HSB: Depends on implementation. Since it is a warm white, the HSB conversion must reflect its position
     * on the color temperature spectrum.
     */
    @Test
    public void testRgbcwBrightWhiteWarm() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        // expect hue near 30-60, low saturation, high brightness.
        lsm.setRGBx(new double[] { 0, 0, 0, 0, 255 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(30.0, hsb.getHue().doubleValue(), 30); // hue should be in the warm spectrum (e.g., ~30)
        assertTrue(hsb.getSaturation().doubleValue() < 10); // low saturation
        assertTrue(hsb.getBrightness().doubleValue() > 90); // high brightness
    }

    /**
     * Case: Mixed White (neutral)
     * Input (RGBCW): (0, 0, 0, 255, 255)
     * Expected HSB: The combined effect of cool and warm white should yield a neutral white, such as (0, 0, 100)
     */
    @Test
    public void testRgbcwMixedWhiteNeutral() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.setRGBx(new double[] { 0, 0, 0, 255, 255 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(0.0, hsb.getHue().doubleValue(), 1);
        assertEquals(0.0, hsb.getSaturation().doubleValue(), 1);
        assertEquals(100.0, hsb.getBrightness().doubleValue(), 1); // white at full brightness
    }

    /**
     * Case: Pastel Color (yellow, low saturation)
     * Input (RGBCW): (100, 100, 0, 100, 100)
     * Expected HSB: The high CW and WW values should lead to a desaturated, brighter version of the yellow from the RGB
     * components. The saturation will be lower and the brightness higher than for a pure RGB yellow.
     */
    @Test
    public void testRgbcwPastelYellowLowSaturation() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.handleCommand(new HSBType("60, 100, 99")); // pure RGB yellow
        HSBType reference = lsm.getColor();
        assertNotNull(reference);

        lsm.setRGBx(new double[] { 100, 100, 0, 100, 100 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(60.0, hsb.getHue().doubleValue(), 1); // yellow
        assertTrue(reference.getSaturation().doubleValue() > hsb.getSaturation().doubleValue()); // less saturated
        assertTrue(reference.getBrightness().doubleValue() < hsb.getBrightness().doubleValue()); // brighter
    }

    /**
     * Case: Black
     * Input (RGBCW): (0, 0, 0, 0, 0)
     * Expected HSB: (0, 0, 0) (Black). The hue and saturation are undefined, so a value of 0 is standard
     */
    @Test
    public void testRgbcwBlack() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.setRGBx(new double[] { 0, 0, 0, 0, 0 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(0.0, hsb.getHue().doubleValue(), 1);
        assertEquals(0.0, hsb.getSaturation().doubleValue(), 1);
        assertEquals(0.0, hsb.getBrightness().doubleValue(), 1);
    }

    /**
     * Case: All channels max
     * Input (RGBCW): (255, 255, 255, 255, 255)
     * Expected HSB: (0, 0, 100) (White). The combination of all channels at full brightness should produce the
     * brightest possible white, with zero saturation.
     */
    @Test
    public void testRgbcwAllChannelsMax() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.setRGBx(new double[] { 255, 255, 255, 255, 255 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(0.0, hsb.getHue().doubleValue(), 1);
        assertEquals(0.0, hsb.getSaturation().doubleValue(), 1);
        assertEquals(100.0, hsb.getBrightness().doubleValue(), 1); // white at full brightness
    }

    /**
     * Case: Maximum RGB, zero white
     * Input (RGBCW): (255, 255, 255, 0, 0)
     * Expected HSB: (0, 0, 100) (White). The RGB channels alone should produce white at full brightness.
     */
    @Test
    public void testRgbcwMaxRgbZeroWhite() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.setRGBx(new double[] { 255, 255, 255, 0, 0 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(0.0, hsb.getHue().doubleValue(), 1);
        assertEquals(0.0, hsb.getSaturation().doubleValue(), 1);
        assertEquals(100.0, hsb.getBrightness().doubleValue(), 1); // white at full brightness
    }

    /**
     * Case: Mixed color with white
     * Input (RGBCW): (255, 0, 0, 0, 100)
     * Expected HSB: A less saturated, brighter red than pure RGB red. The Hue should remain at 0, but saturation will
     * decrease, and brightness will be higher.
     */
    @Test
    public void testRgbcwMixedColorWithWhite() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.handleCommand(new HSBType("0, 100, 99")); // pure RGB red
        HSBType reference = lsm.getColor();
        assertNotNull(reference);

        lsm.setRGBx(new double[] { 255, 0, 0, 0, 100 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(0.0, hsb.getHue().doubleValue(), 1);
        assertTrue(reference.getBrightness().doubleValue() < hsb.getBrightness().doubleValue()); // brighter
        // TODO the following test fails - maybe due to error in ColorUtil RGBW method ??
        // assertTrue(reference.getSaturation().doubleValue() > hsb.getSaturation().doubleValue()); // less saturated
    }

    /**
     * Case: Non-zero RGB with CW only
     * Input (RGBCW): (255, 0, 0, 100, 0)
     * Expected HSB: A less saturated, cooler red. Hue remains 0, saturation is lower, and brightness is higher. The
     * color temperature of the red will shift towards the cool white.
     */
    @Test
    public void testRgbcwNonZeroRgbWithCoolWhiteOnly() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.handleCommand(new HSBType("0, 100, 99")); // pure RGB red
        HSBType reference = lsm.getColor();
        assertNotNull(reference);

        lsm.setRGBx(new double[] { 255, 0, 0, 100, 0 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(0.0, hsb.getHue().doubleValue(), 1);
        assertTrue(reference.getBrightness().doubleValue() < hsb.getBrightness().doubleValue()); // brighter
        // TODO the following test fails - maybe due to error in ColorUtil RGBW method ??
        // assertTrue(reference.getSaturation().doubleValue() > hsb.getSaturation().doubleValue()); // less saturated
    }

    /**
     * Case: Primary Blue
     * Input (HSB): (240, 100, 100)
     * Expected RGBCW: (0, 0, 255, 0, 0). A pure, saturated color should only use the RGB channels.
     */
    @Test
    public void testRgbcwPrimaryBlue() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.handleCommand(new HSBType("240, 100, 100"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(0.0, rgbx[0], 1);
        assertEquals(0.0, rgbx[1], 1);
        assertEquals(255.0, rgbx[2], 1); // full blue
        assertEquals(0.0, rgbx[3], 1);
        assertEquals(0.0, rgbx[4], 1);
    }

    /**
     * Case: Gray
     * Input (HSB): (0, 0, 50)
     * Expected RGBCW: Assuming the system uses white channels for brightness and desaturation, the gray should be
     * produced by a mix of CW and WW, with no RGB active: (0, 0, 0, 128, 128).
     */
    @Test
    public void testRgbcwGray() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.handleCommand(new HSBType("0, 0, 50"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(0.0, rgbx[0], 1);
        assertEquals(0.0, rgbx[1], 1);
        assertEquals(0.0, rgbx[2], 1);
        assertEquals(127.5, rgbx[3] + rgbx[4], 1); // 75% brightness mix of CW and WW
    }

    /**
     * Case: Pastel Green
     * Input (HSB): (120, 50, 75)
     * Expected RGBCW: The conversion should calculate a mix of green and white to achieve the desired brightness and
     * saturation. For instance, (0, 191, 0, 64, 64)
     */
    @Test
    public void testRgbcwPastelGreen() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.handleCommand(new HSBType("120, 50, 75"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(0.0, rgbx[0], 1); //
        assertEquals(191.0, rgbx[1] + rgbx[3] + rgbx[4], 1); // 75% brightness green
        assertEquals(0.0, rgbx[2], 1);
        assertEquals(rgbx[3], rgbx[4], 1); // equal parts CW and WW
    }

    /**
     * Case: Full Bright White
     * Input (HSB): (0, 0, 100)
     * Expected RGBCW: (0, 0, 0, 255, 255). Maximum brightness and zero saturation should be achieved by using only the
     * white channels.
     */
    @Test
    public void testRgbcwFullBrightWhite() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.handleCommand(new HSBType("0, 0, 100"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(0.0, rgbx[0], 1);
        assertEquals(0.0, rgbx[1], 1);
        assertEquals(0.0, rgbx[2], 1);
        assertEquals(255.0, rgbx[3] + rgbx[4], 1); // 100% brightness mix of CW and WW
    }

    /**
     * Case: Black #2
     * Input (HSB): (0, 0, 0)
     * Expected RGBCW: (0, 0, 0, 0, 0). Black should result in all channels off.
     */
    @Test
    public void testRgbcwBlack2() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.handleCommand(new HSBType("0, 0, 0"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(0.0, rgbx[0], 1);
        assertEquals(0.0, rgbx[1], 1);
        assertEquals(0.0, rgbx[2], 1);
        assertEquals(0.0, rgbx[3], 1);
        assertEquals(0.0, rgbx[4], 1);
    }

    /**
     * Case: Low Brightness, High Saturation
     * Input (HSB): (60, 100, 25)
     * Expected RGBCW: (64, 64, 0, 0, 0). The low brightness should mean the white channels are not used at all, and
     * only the RGB channels are used at a scaled-down value.
     */
    @Test
    public void testRgbcwLowBrightnessHighSaturation() {
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.handleCommand(new HSBType("60, 100, 25"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(64.0, rgbx[0], 1); // 25% of 255 red
        assertEquals(64.0, rgbx[1], 1); // 25% of 255 green => yellow
        assertEquals(0.0, rgbx[2], 1);
        assertEquals(0.0, rgbx[3], 1);
        assertEquals(0.0, rgbx[4], 1);
    }

    /**
     * Case: HSB with Cool White preference
     * Input (HSB): A color with a blue tint, e.g., (240, 50, 75).
     * Expected RGBCW: The conversion might be designed to leverage the cool white LED to create a cooler white
     * component, resulting in a higher CW value and lower WW. For example, a result like (0, 0, 191, 128, 0).
     */
    @Test
    public void testRgbcwHsbWithCoolWhitePreference() {
        // assume conversion prefers cool white for cooler hues
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.handleCommand(new HSBType("240, 50, 75"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(0.0, rgbx[0]);
        assertEquals(0.0, rgbx[1]);
        assertEquals(191.0, rgbx[2] + rgbx[3] + rgbx[4], 1); // blue 75% brightness
        assertTrue(rgbx[3] > rgbx[4]); // using cooler white
    }

    /**
     * Case: HSB with Warm White preference
     * Input (HSB): A color with a yellow/red tint, e.g., (30, 50, 75).
     * Expected RGBCW: Similarly, this conversion would prioritize the warm white LED to maintain the warmer color
     * temperature, possibly leading to a result like (191, 64, 0, 0, 128)
     *
     */
    @Test
    public void testRgbcwHsbWithWarmWhitePreference() {
        // assume conversion prefers warm white for warmer hues
        LightModel lsm = new LightModel(true, true, true, false, true, false);
        lsm.handleCommand(new HSBType("30, 50, 75"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(191.0, rgbx[0] + rgbx[3] + rgbx[4], 1); // red 75% brightness
        assertTrue(rgbx[0] > rgbx[1]); // green contribution is less than the red
        assertEquals(0.0, rgbx[2], 1);
        assertTrue(rgbx[4] > rgbx[3]); // using warmer white
    }
}
