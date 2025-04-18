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
package org.openhab.core.ui.internal.chart.defaultchartprovider;

import java.awt.Color;
import java.awt.Font;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Implementation of the dark {@link ChartTheme chart theme}.
 *
 * @author Holger Reichert - Initial contribution
 */
@NonNullByDefault
public class ChartThemeDark implements ChartTheme {

    private static final String THEME_NAME = "dark";

    private static final Color[] LINECOLORS = new Color[] { //
            new Color(244, 67, 54), // red
            new Color(76, 175, 80), // green
            new Color(63, 81, 181), // blue
            new Color(156, 39, 176), // magenta/purple
            new Color(255, 152, 0), // orange
            new Color(0, 188, 212).darker(), // cyan
            new Color(233, 30, 99).darker(), // pink
            Color.WHITE, // white
            new Color(255, 235, 59) // yellow
    };

    private static final String FONT_NAME = "SansSerif";

    @Override
    public String getThemeName() {
        return THEME_NAME;
    }

    @Override
    public Color getPlotBackgroundColor() {
        return new Color(40, 40, 40);
    }

    @Override
    public Color getPlotGridLinesColor() {
        return Color.WHITE.darker();
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
        return new Color(30, 30, 30, 160);
    }

    @Override
    public Color getChartBackgroundColor() {
        return new Color(48, 48, 48);
    }

    @Override
    public Color getChartFontColor() {
        return new Color(255, 255, 255, 255);
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
