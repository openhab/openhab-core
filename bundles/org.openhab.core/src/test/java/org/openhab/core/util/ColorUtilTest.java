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
package org.openhab.core.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.library.types.HSBType;

/**
 * The {@link ColorUtilTest} is a test class for the color conversion
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ColorUtilTest {
    private static Stream<Arguments> colors() {
        return Stream.of(HSBType.BLACK, HSBType.BLUE, HSBType.GREEN, HSBType.RED, HSBType.WHITE,
                HSBType.fromRGB(127, 94, 19)).map(Arguments::of);
    }

    private static Stream<Arguments> invalids() {
        return Stream.of(new double[] { 0.0 }, new double[] { -1.0, 0.5 }, new double[] { 1.5, 0.5 },
                new double[] { 0.5, -1.0 }, new double[] { 0.5, 1.5 }, new double[] { 0.5, 0.5, -1.0 },
                new double[] { 0.5, 0.5, 1.5 }, new double[] { 0.0, 1.0, 0.0, 1.0 }).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("colors")
    public void inversionTest(HSBType hsb) {
        HSBType hsb2 = ColorUtil.xyToHsv(ColorUtil.hsbToXY(hsb));

        double deltaHue = Math.abs(hsb.getHue().doubleValue() - hsb2.getHue().doubleValue());
        deltaHue = deltaHue > 180.0 ? Math.abs(deltaHue - 360) : deltaHue; // if deltaHue > 180, the "other direction"
        // is shorter
        double deltaSat = Math.abs(hsb.getSaturation().doubleValue() - hsb2.getSaturation().doubleValue());
        double deltaBri = Math.abs(hsb.getBrightness().doubleValue() - hsb2.getBrightness().doubleValue());

        assertThat(deltaHue, is(lessThan(5.0)));
        assertThat(deltaSat, is(lessThanOrEqualTo(1.0)));
        assertThat(deltaBri, is(lessThanOrEqualTo(1.0)));
    }

    @ParameterizedTest
    @MethodSource("invalids")
    public void invalidXyValues(double[] xy) {
        assertThrows(IllegalArgumentException.class, () -> ColorUtil.xyToHsv(xy));
    }
}
