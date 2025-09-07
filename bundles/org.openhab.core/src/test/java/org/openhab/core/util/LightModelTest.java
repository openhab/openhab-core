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
import org.openhab.core.util.LightModel.RgbDataType;
import org.openhab.core.util.LightModel.RgbcwMath;
import org.openhab.core.util.LightModel.WhiteLED;

/**
 * Unit tests for {@link LightModel}.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class LightModelTest {

    private static final double EPSILON = 1e-6;
    private final WhiteLED coolWhiteLed = new WhiteLED(153);
    private final WhiteLED warmWhiteLed = new WhiteLED(500);

    @Test
    public void testFullColor() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.DEFAULT);
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
        LightModel lsm = new LightModel(true, false, true, RgbDataType.DEFAULT);
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
        LightModel lsm = new LightModel(true, true, false, RgbDataType.DEFAULT);
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
        LightModel lsm = new LightModel(true, false, false, RgbDataType.DEFAULT);
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
        LightModel lsm = new LightModel(false, false, false, RgbDataType.DEFAULT);
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
        LightModel lsm = new LightModel(false, false, false, RgbDataType.RGB_C_W, 2.0, 501.0, 154.0, 11.0, null, null);
        assertFalse(lsm.configGetSupportsColor());
        assertFalse(lsm.configGetSupportsBrightness());
        assertFalse(lsm.configGetSupportsColorTemperature());
        assertEquals(RgbDataType.RGB_C_W, lsm.configGetRgbDataType());
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
                () -> new LightModel(false, false, false, RgbDataType.DEFAULT, 0.0, null, null, null, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, RgbDataType.DEFAULT, 11.0, null, null, null, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, RgbDataType.DEFAULT, null, 99.0, null, null, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, RgbDataType.DEFAULT, null, 1001.0, null, null, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, RgbDataType.DEFAULT, null, null, 99.0, null, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, RgbDataType.DEFAULT, null, null, 1001.0, null, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, RgbDataType.DEFAULT, null, 300.0, 300.0, null, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, RgbDataType.DEFAULT, null, null, null, 0.0, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, RgbDataType.DEFAULT, null, null, null, 51.0, null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, RgbDataType.DEFAULT, null, null, null, null, 99.0, null));

        assertThrows(IllegalArgumentException.class,
                () -> new LightModel(false, false, false, RgbDataType.DEFAULT, null, null, null, null, null, 10001.0));
    }

    @Test
    public void testRgbIgnoreBrightness() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_NO_BRIGHTNESS);
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
        LightModel lsm = new LightModel(true, true, true, RgbDataType.DEFAULT);
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
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_W);
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
        lsm = new LightModel(true, false, true, RgbDataType.DEFAULT);
        lsm.handleCommand(HSBType.RED);

        assertEquals(HSBType.RED, lsm.getColor());
        assertNull(lsm.getBrightness());
        assertNull(lsm.getOnOff());
        assertNotNull(lsm.getBrightness(true));
        assertNotNull(lsm.getOnOff(true));

        // supports brightness
        lsm = new LightModel(true, false, false, RgbDataType.DEFAULT);
        lsm.handleCommand(HSBType.RED);

        assertNull(lsm.getColor());
        assertNotNull(lsm.getBrightness());
        assertNull(lsm.getOnOff());
        assertNotNull(lsm.getOnOff(true));

        // supports on/off
        lsm = new LightModel(false, false, false, RgbDataType.DEFAULT);
        lsm.handleCommand(HSBType.RED);

        assertNull(lsm.getColor());
        assertNull(lsm.getBrightness());
        assertNotNull(lsm.getOnOff());
    }

    @Test
    public void testRgbToRgbcwToRgb_RoundTrip() {
        double[] originalRgb = { 0.8, 0.7, 0.6 };
        double[] rgbcw = RgbcwMath.rgb2rgbcw(originalRgb, coolWhiteLed.getProfile(), warmWhiteLed.getProfile());
        double[] reconstructedRgb = RgbcwMath.rgbcw2rgb(rgbcw, coolWhiteLed.getProfile(), warmWhiteLed.getProfile());
        assertArrayEquals(originalRgb, reconstructedRgb, EPSILON, "RGB → RGBCW → RGB should match original");
    }

    @Test
    public void testRgbcwToRgbToRgbcw_RoundTrip() {
        double[] originalRgbcw = { 0.5, 0.4, 0.3, 0.2, 0.1 };
        double[] rgb = RgbcwMath.rgbcw2rgb(originalRgbcw, coolWhiteLed.getProfile(), warmWhiteLed.getProfile());
        double[] reconstructedRgbcw = RgbcwMath.rgb2rgbcw(rgb, coolWhiteLed.getProfile(), warmWhiteLed.getProfile());
        double[] rgb2 = RgbcwMath.rgbcw2rgb(reconstructedRgbcw, coolWhiteLed.getProfile(), warmWhiteLed.getProfile());
        assertArrayEquals(rgb, rgb2, EPSILON, "RGB reconstructed from RGBCW should remain visually consistent");
    }

    @Test
    public void testEdgeCase_FullWhite() {
        double[] white = { 1.0, 1.0, 1.0 };
        double[] rgbcw = RgbcwMath.rgb2rgbcw(white, coolWhiteLed.getProfile(), warmWhiteLed.getProfile());
        double[] reconstructed = RgbcwMath.rgbcw2rgb(rgbcw, coolWhiteLed.getProfile(), warmWhiteLed.getProfile());
        assertArrayEquals(white, reconstructed, EPSILON, "Full white RGB should round-trip cleanly");
    }

    @Test
    public void testEdgeCase_Black() {
        double[] black = { 0.0, 0.0, 0.0 };
        double[] rgbcw = RgbcwMath.rgb2rgbcw(black, coolWhiteLed.getProfile(), warmWhiteLed.getProfile());
        double[] reconstructed = RgbcwMath.rgbcw2rgb(rgbcw, coolWhiteLed.getProfile(), warmWhiteLed.getProfile());
        assertArrayEquals(black, reconstructed, EPSILON, "Black RGB should round-trip cleanly");
    }

    @Test
    public void testRgbcw() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);

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
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
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
     * Expected HSB: Depends on implementation. Expect it to match the color temperature of the warm white LED.
     */
    @Test
    public void testRgbcwBrightWhiteWarm() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        HSBType warm = ColorUtil.xyToHsb(ColorUtil.kelvinToXY(1000000 / lsm.configGetMiredWarmWhiteLed()));
        lsm.setRGBx(new double[] { 0, 0, 0, 0, 255 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(30.0, hsb.getHue().doubleValue(), 30); // in the warm spectrum (e.g., ~30)
        assertEquals(warm.getHue().doubleValue(), hsb.getHue().doubleValue(), 1); // same as warm LED
        assertEquals(warm.getSaturation().doubleValue(), hsb.getSaturation().doubleValue(), 1); // same as warm LED
        assertEquals(warm.getBrightness().doubleValue(), hsb.getBrightness().doubleValue(), 1); // same as warm LED
    }

    /**
     * Case: Bright White (warm)
     * Input (RGBCW): (0, 0, 0, 255, 0)
     * Expected HSB: Depends on implementation. Expect it to match the color temperature of the cool white LED.
     */
    @Test
    public void testRgbcwBrightWhiteCool() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        HSBType cool = ColorUtil.xyToHsb(ColorUtil.kelvinToXY(1000000 / lsm.configGetMiredCoolWhiteLed()));
        lsm.setRGBx(new double[] { 0, 0, 0, 255, 0 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(240.0, hsb.getHue().doubleValue(), 30); // in the cool spectrum (e.g., ~240)
        assertEquals(cool.getHue().doubleValue(), hsb.getHue().doubleValue(), 5); // same as cool LED
        assertEquals(cool.getSaturation().doubleValue(), hsb.getSaturation().doubleValue(), 5); // same as cool LED
        assertEquals(cool.getBrightness().doubleValue(), hsb.getBrightness().doubleValue(), 5); // same as cool LED
    }

    /**
     * Case: Mixed White (neutral)
     * Input (RGBCW): (0, 0, 0, 255, 255)
     * Expected HSB: The combined effect of cool and warm white should yield a neutral white. The resulting
     * hue is not really relevant, so long as the saturation is very low, and the point in HSB space lies
     * between the cool and warm white levels.
     */
    @Test
    public void testRgbcwMixedWhiteNeutral() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        /*
         * for the purposes of this test we force
         * - the cool white led must have a blue hue and ~20% saturation
         * - the cool warm led must have a yellow hue and ~20% saturation
         * - so the mix should have ~0% saturation (and the hue is undefined)
         */
        lsm.configSetMiredCoolWhiteLED(100); // 10'000 K
        lsm.configSetMiredWarmWhiteLED(230); // 4'347 K

        lsm.setRGBx(new double[] { 0, 0, 0, 255, 255 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(180.0, hsb.getHue().doubleValue(), 180); // hue is not relevant
        assertEquals(0.0, hsb.getSaturation().doubleValue(), 5); // saturation very low
        assertEquals(100.0, hsb.getBrightness().doubleValue(), 1); // 100% brightness
    }

    /**
     * Case: Black
     * Input (RGBCW): (0, 0, 0, 0, 0)
     * Expected HSB: Black. The hue and saturation are undefined, and the brightness shall be zero.
     */
    @Test
    public void testRgbcwBlack() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        lsm.setRGBx(new double[] { 0, 0, 0, 0, 0 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(180.0, hsb.getHue().doubleValue(), 180); // hue is not relevant
        assertEquals(180.0, hsb.getSaturation().doubleValue(), 180); // saturation not relevant
        assertEquals(0.0, hsb.getBrightness().doubleValue(), 1); // 0% brightness (black)
    }

    /**
     * Case: All channels max
     * Input (RGBCW): (255, 255, 255, 255, 255)
     * Expected HSB: White. The combination of all channels at full brightness should produce the brightest possible
     * white, with zero saturation.
     */
    @Test
    public void testRgbcwAllChannelsMax() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        lsm.setRGBx(new double[] { 255, 255, 255, 255, 255 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(180.0, hsb.getHue().doubleValue(), 180); // hue is not relevant
        assertEquals(0.0, hsb.getSaturation().doubleValue(), 1); // zero saturation
        assertEquals(100.0, hsb.getBrightness().doubleValue(), 1); // white at full brightness
    }

    /**
     * Case: Maximum RGB, zero white
     * Input (RGBCW): (255, 255, 255, 0, 0)
     * Expected HSB: White. The RGB channels alone should produce white at full brightness.
     */
    @Test
    public void testRgbcwMaxRgbZeroWhite() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        lsm.setRGBx(new double[] { 255, 255, 255, 0, 0 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(180.0, hsb.getHue().doubleValue(), 180); // hue is not relevant
        assertEquals(0.0, hsb.getSaturation().doubleValue(), 1); // zero saturation
        assertEquals(100.0, hsb.getBrightness().doubleValue(), 1); // white at full brightness
    }

    /**
     * Case: Mixed color with white
     * Input (RGBCW): (255, 0, 0, 0, 100)
     * Expected HSB: A less saturated, warmer red. The Hue is in the positive red sector, saturation is lower,
     * and brightness is 100%.
     */
    @Test
    public void testRgbcwMixedColorWithWhite() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        lsm.setRGBx(new double[] { 255, 0, 0, 0, 100 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(15.0, hsb.getHue().doubleValue(), 15); // positive red
        assertTrue(100.0 > hsb.getSaturation().doubleValue()); // less saturated
        assertEquals(100.0, hsb.getBrightness().doubleValue()); // 100%
    }

    /**
     * Case: Non-zero RGB with CW only
     * Input (RGBCW): (255, 0, 0, 100, 0)
     * Expected HSB: A less saturated, cooler red. Hue is in the negative red sector, saturation is lower,
     * and brightness is 100%.
     */
    @Test
    public void testRgbcwNonZeroRgbWithCoolWhiteOnly() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        // force cool white led for this test
        lsm.configSetMiredCoolWhiteLED(100); // 10'000 K
        lsm.setRGBx(new double[] { 255, 0, 0, 100, 0 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(345.0, hsb.getHue().doubleValue(), 15); // negative red
        assertTrue(100.0 > hsb.getSaturation().doubleValue()); // less saturated
        assertEquals(100.0, hsb.getBrightness().doubleValue()); // 100%
    }

    /**
     * Case: Primary Blue
     * Input (HSB): (240, 100, 100)
     * Expected RGBCW: (0, 0, 255, 0, 0). A pure, saturated color should only use the RGB channels.
     */
    @Test
    public void testRgbcwPrimaryBlue() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        lsm.handleCommand(new HSBType("240,100,100"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(0.0, rgbx[0], 1); // no red
        assertEquals(0.0, rgbx[1], 1); // no green
        assertEquals(255.0, rgbx[2], 1); // full blue
        assertEquals(0.0, rgbx[3], 1); // no cool white
        assertEquals(0.0, rgbx[4], 1); // no warm white
    }

    /**
     * Case: Black #2
     * Input (HSB): (0, 0, 0)
     * Expected RGBCW: Black should result in all channels off.
     */
    @Test
    public void testRgbcwBlack2() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        lsm.handleCommand(new HSBType("0,0,0"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(0.0, rgbx[0], 1); // no red
        assertEquals(0.0, rgbx[1], 1); // no green
        assertEquals(0.0, rgbx[2], 1); // no blue
        assertEquals(0.0, rgbx[3], 1); // no cool white
        assertEquals(0.0, rgbx[4], 1); // no warm white
    }

    /**
     * Case: Low Brightness, High Saturation
     * Input (HSB): (60, 100, 25)
     * Expected RGBCW: The low brightness should mean the white channels are not used at all, and only the RGB channels
     * are used at a scaled-down value.
     */
    @Test
    public void testRgbcwLowBrightnessHighSaturation() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        lsm.handleCommand(new HSBType("60,100,25"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(64.0, rgbx[0], 1); // 25% of 255 red => yellow
        assertEquals(64.0, rgbx[1], 1); // 25% of 255 green => yellow
        assertEquals(0.0, rgbx[2], 1); // no blue
        assertEquals(0.0, rgbx[3], 1); // no cool white
        assertEquals(0.0, rgbx[4], 1); // no warm white
    }

    /**
     * Case: Gray
     * Input (HSB): (0, 0, 50)
     * Expected RGBCW: The gray should be produced by the cool or white LED, with minimal RGB to fine tune the color.
     */
    @Test
    public void testRgbcwGray() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        lsm.handleCommand(new HSBType("0,0,50"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(0.0, rgbx[0], 10); // red fine tuning up to 10.0
        assertEquals(0.0, rgbx[1], 10); // green fine tuning up to 10.0
        assertEquals(0.0, rgbx[2], 10); // blue fine tuning up to 10.0
        assertEquals(128.0, rgbx[3], 10); // cool white close to 50%
        assertEquals(0.0, rgbx[4], 1); // warm white off
    }

    /**
     * Case: Pastel Green
     * Input (HSB): (120, 50, 75)
     * Expected RGBCW: The conversion should calculate a mix of green and white to achieve the desired brightness and
     * saturation. The green channel should be prominent, with some contribution from the cool white LED to reduce
     * saturation and increase brightness.
     */
    @Test
    public void testRgbcwPastelGreen() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        lsm.handleCommand(new HSBType("120,50,75"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(0.0, rgbx[0], 10); // red channel minimal
        assertTrue(rgbx[1] > Math.max(rgbx[0], rgbx[2])); // green channel dominant
        assertEquals(0.0, rgbx[2], 10); // blue channel minimal
        assertEquals(0.0, Math.min(rgbx[0], Math.min(rgbx[2], rgbx[2])), 1); // red or blue should be zero
        assertEquals(0.0, rgbx[4], 0); // warm white zero
        assertEquals(191.0, rgbx[1] + rgbx[3], 1); // green + cool white should be ~75% brightness
    }

    /**
     * Case: Pastel Color (yellow, low saturation)
     * Input (RGBCW): (100, 100, 0, 100, 100)
     * Expected HSB: The high CW and WW values should lead to a desaturated, brighter version of the yellow from the RGB
     * components. The saturation will be lower and the brightness higher than for a pure RGB yellow.
     */
    @Test
    public void testRgbcwPastelYellowLowSaturation() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        lsm.setRGBx(new double[] { 100, 100, 0, 100, 100 });
        HSBType hsb = lsm.getColor();
        assertNotNull(hsb);
        assertEquals(60.0, hsb.getHue().doubleValue(), 1); // hue 60 (yellow)
        assertEquals(50.0, hsb.getSaturation().doubleValue(), 10); // lower saturation than full yellow
        assertEquals(100.0, hsb.getBrightness().doubleValue(), 1); // 100% brightness
    }

    /**
     * Case: HSB with Cool White preference
     * Input (HSB): A color with a blue tint, e.g., (240, 50, 75).
     * Expected RGBCW: The conversion should prioritize the cool white LED to create a cooler white component, resulting
     * in a higher CW value and lower WW.
     */
    @Test
    public void testRgbcwHsbWithCoolWhitePreference() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        lsm.handleCommand(new HSBType("240,50,75"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(0.0, rgbx[0], 10); // red channel fine tuning up to 10.0
        assertEquals(0.0, rgbx[1], 10); // green channel fine tuning up to 10.0
        assertEquals(192.0, rgbx[2] + rgbx[3], 5); // blue and cool white channel shall be ~75% brightness
        assertEquals(96.0, rgbx[2], 5); // blue channel shall contribute ~37.5% brightness
        assertEquals(96.0, rgbx[3], 5); // cool white channel shall contribute ~37.5% brightness
        assertEquals(0.0, rgbx[4], 1); // warm white channel should zero
    }

    /**
     * Case: HSB with Warm White preference
     * Input (HSB): A color with a yellow/red tint, e.g., (30, 70, 75).
     * Expected RGBCW: The conversion should prioritize the warm white LED to maintain the warmer color temperature,
     * resulting in a higher WW value and lower CW.
     */
    @Test
    public void testRgbcwHsbWithWarmWhitePreference() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        lsm.handleCommand(new HSBType("30,70,75"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(0.0, rgbx[0], 15); // red channel fine tuning up to 15.0
        assertEquals(0.0, rgbx[1], 15); // green channel fine tuning up to 15.0
        assertEquals(0.0, rgbx[2], 15); // blue channel fine tuning up to 15.0
        assertEquals(0.0, rgbx[3], 10); // cool white channel should be zero
        assertTrue(rgbx[0] > Math.max(rgbx[1], rgbx[2])); // red channel dominant
        assertEquals(192.0, rgbx[0] + rgbx[4], 10); // red + warm white channel should be ~75% brightness
    }

    /**
     * Case: Full Bright White
     * Input (HSB): (0, 0, 100)
     * Expected RGBCW: (0, 0, 0, 255, 255). Maximum brightness and zero saturation should be achieved by using mainly
     * the white channels, with small RGB values for color fine tuning.
     */
    @Test
    public void testRgbcwFullBrightWhite() {
        LightModel lsm = new LightModel(true, true, true, RgbDataType.RGB_C_W);
        lsm.handleCommand(new HSBType("0,0,100"));
        double[] rgbx = lsm.getRGBx();
        assertEquals(5, rgbx.length);
        assertEquals(0.0, rgbx[0], 15); // red channel fine tuning up to 15.0
        assertEquals(0.0, rgbx[1], 15); // green channel fine tuning up to 15.0
        assertEquals(0.0, rgbx[2], 15); // blue channel fine tuning up to 15.0
        assertEquals(255.0, rgbx[3], 10); // cool white channel should be maximal
        assertEquals(0.0, rgbx[4], 10); // warm white channel should be zero
    }
}
