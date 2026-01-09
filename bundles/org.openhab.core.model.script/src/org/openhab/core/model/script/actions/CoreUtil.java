/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.script.actions;

import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.util.ColorUtil;
import org.openhab.core.util.ColorUtil.Gamut;

/**
 * This class provides static methods mapping methods from package org.openhab.core.util
 *
 * @author Laurent Garnier - Initial contribution
 */
public class CoreUtil {

    public static int[] hsbToRgb(HSBType hsb) {
        return ColorUtil.hsbToRgb(hsb);
    }

    public static PercentType[] hsbToRgbPercent(HSBType hsb) {
        return ColorUtil.hsbToRgbPercent(hsb);
    }

    public static int hsbTosRgb(HSBType hsb) {
        return ColorUtil.hsbTosRgb(hsb);
    }

    public static int[] hsbToRgbw(HSBType hsb) {
        return ColorUtil.hsbToRgbw(hsb);
    }

    public static PercentType[] hsbToRgbwPercent(HSBType hsb) {
        return ColorUtil.hsbToRgbwPercent(hsb);
    }

    public static double[] hsbToXY(HSBType hsb) {
        return ColorUtil.hsbToXY(hsb);
    }

    public static double[] hsbToXY(HSBType hsb, double[] gamutR, double[] gamutG, double[] gamutB) {
        Gamut gamut = new Gamut(gamutR, gamutG, gamutB);
        return ColorUtil.hsbToXY(hsb, gamut);
    }

    public static HSBType rgbToHsb(int[] rgb) throws IllegalArgumentException {
        return ColorUtil.rgbToHsb(rgb);
    }

    public static HSBType rgbToHsb(PercentType[] rgb) throws IllegalArgumentException {
        return ColorUtil.rgbToHsb(rgb);
    }

    public static HSBType xyToHsb(double[] xy) throws IllegalArgumentException {
        return ColorUtil.xyToHsb(xy);
    }

    public static HSBType xyToHsb(double[] xy, double[] gamutR, double[] gamutG, double[] gamutB)
            throws IllegalArgumentException {
        Gamut gamut = new Gamut(gamutR, gamutG, gamutB);
        return ColorUtil.xyToHsb(xy, gamut);
    }

    public static double xyToDuv(double[] xy) throws IllegalArgumentException {
        return ColorUtil.xyToDuv(xy);
    }

    public static double[] kelvinToXY(double kelvin) throws IndexOutOfBoundsException {
        return ColorUtil.kelvinToXY(kelvin);
    }

    public static double xyToKelvin(double[] xy) throws IllegalArgumentException {
        return ColorUtil.xyToKelvin(xy);
    }
}
