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
 * Implementation of the default bright {@link ChartTheme chart theme}.
 *
 * @author Holger Reichert - Initial contribution
 */
public class ChartThemeBright implements ChartTheme {

    private static final String THEME_NAME = "bright";

    private static final Color[] LINECOLORS = new Color[] { Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA,
            Color.ORANGE, Color.CYAN, Color.PINK, Color.DARK_GRAY, Color.YELLOW };

    private static final String FONT_NAME = "SansSerif";

    @Override
    public String getThemeName() {
        return THEME_NAME;
    }

    @Override
    public Color getPlotBackgroundColor() {
        return new Color(254, 254, 254);
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
        return new Color(224, 224, 224, 160);
    }

    @Override
    public Color getChartBackgroundColor() {
        return new Color(224, 224, 224, 224);
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
