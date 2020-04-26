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
 * Chart styling theme for the {@link DefaultChartProvider}.
 *
 * @author Holger Reichert - Initial contribution
 */
public interface ChartTheme {

    /**
     * Theme name. Has to be unique across all themes.
     *
     * @return theme name
     */
    public String getThemeName();

    /**
     * Background color, plot area.
     *
     * @return background color, plot area
     */
    public Color getPlotBackgroundColor();

    /**
     * Color for the grid lines.
     *
     * @return color for the grid lines
     */
    public Color getPlotGridLinesColor();

    /**
     * Return the width of the grid lines.
     *
     * @param dpi DPI dots per inch to calculate the width
     * @return width of the grid lines
     */
    public double getPlotGridLinesWidth(int dpi);

    /**
     * Return the dash spacing for the grid lines.
     *
     * @param dpi DPI dots per inch to calculate the width
     * @return dash spacing for the grid lines
     */
    public double getPlotGridLinesDash(int dpi);

    /**
     * Background color, legend area.
     *
     * @return background color, legend area
     */
    public Color getLegendBackgroundColor();

    /**
     * Background color, whole chart
     *
     * @return background color, whole chart
     */
    public Color getChartBackgroundColor();

    /**
     * Font color, legend and general use.
     *
     * @return
     */
    public Color getChartFontColor();

    /**
     * Return a color for the given series number.
     *
     * @param series series number
     * @return color for the given series numer
     */
    public Color getLineColor(int series);

    /**
     * Return the width of the series lines.
     *
     * @param dpi DPI dots per inch to calculate the width
     * @return width of the series lines
     */
    public double getLineWidth(int dpi);

    /**
     * Color for the axis labels.
     *
     * @return
     */
    public Color getAxisTickLabelsColor();

    /**
     * Font for the axis labels.
     * The font size gets calculated with the dpi parameter.
     *
     * @param dpi the DPI to calculate the font size
     * @return {@link Font} for the axis labels.
     */
    public Font getAxisTickLabelsFont(int dpi);

    /**
     * Font for the legend text.
     * The font size gets calculated with the dpi parameter.
     *
     * @param dpi the DPI to calculate the font size
     * @return {@link Font} for the legend text
     */
    public Font getLegendFont(int dpi);

    /**
     * Padding of the chart.
     *
     * @param dpi the DPI to calculate the padding
     * @return padding of the chart
     */
    public int getChartPadding(int dpi);

    /**
     * Length of the line markers in the legend, in px.
     *
     * @param dpi the DPI to calculate the line length
     * @return length of the line markers in the legend, in px
     */
    public int getLegendSeriesLineLength(int dpi);
}
