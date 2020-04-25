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
package org.openhab.core.ui.internal.chart.defaultchartprovider;

import java.awt.Color;
import java.awt.Font;

/**
 * Implementation of the white {@link ChartTheme chart theme}.
 *
 * @author Holger Reichert - Initial contribution
 */
public class ChartThemeWhite implements ChartTheme {

    private static final String THEME_NAME = "white";

    private static final Color[] LINECOLORS = new Color[] { //
            new Color(244, 67, 54), // red
            new Color(76, 175, 80), // green
            new Color(63, 81, 181), // blue
            new Color(156, 39, 176), // magenta/purple
            new Color(255, 152, 0), // orange
            new Color(0, 188, 212), // cyan
            new Color(233, 30, 99), // pink
            new Color(50, 50, 50), // dark grey
            new Color(205, 220, 57) // yellow/lime
    };

    private static final String FONT_NAME = "SansSerif";

    @Override
    public String getThemeName() {
        return THEME_NAME;
    }

    @Override
    public Color getPlotBackgroundColor() {
        return Color.WHITE;
    }

    @Override
    public Color getPlotGridLinesColor() {
        return new Color(216, 216, 216);
    }

    @Override
    public double getPlotGridLinesWidth(int dpi) {
        return Math.max(1.0, dpi / 64.0);
    }

    @Override
    public double getPlotGridLinesDash(int dpi) {
        return Math.max(3.0f, dpi / 32.0);
    }

    @Override
    public Color getLegendBackgroundColor() {
        return new Color(255, 255, 255, 160);
    }

    @Override
    public Color getChartBackgroundColor() {
        return Color.WHITE;
    }

    @Override
    public Color getChartFontColor() {
        return Color.BLACK;
    }

    @Override
    public Color getLineColor(int series) {
        return LINECOLORS[series % LINECOLORS.length];
    }

    @Override
    public double getLineWidth(int dpi) {
        return Math.max(1.0, dpi / 64.0);
    }

    @Override
    public Color getAxisTickLabelsColor() {
        return getChartFontColor();
    }

    @Override
    public Font getAxisTickLabelsFont(int dpi) {
        int fontsize = (int) Math.max(8, Math.round(dpi / 8.5));
        return new Font(FONT_NAME, Font.PLAIN, fontsize);
    }

    @Override
    public Font getLegendFont(int dpi) {
        int fontsize = (int) Math.max(8, Math.round(dpi / 9.6));
        return new Font(FONT_NAME, Font.PLAIN, fontsize);
    }

    @Override
    public int getChartPadding(int dpi) {
        return (int) Math.max(5, Math.round(dpi / 19.0));
    }

    @Override
    public int getLegendSeriesLineLength(int dpi) {
        return (int) Math.max(10, Math.round(dpi / 12.0));
    }
}
