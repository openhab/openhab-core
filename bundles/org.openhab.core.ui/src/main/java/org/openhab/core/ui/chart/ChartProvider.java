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
package org.openhab.core.ui.chart;

import java.awt.image.BufferedImage;
import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.ItemNotFoundException;

/**
 * Defines the interface for chart providers. A chart provider interfaces with
 * the persistence store to get the data and receives parameters from the UI
 * chart servlet and returns a chart image object (PNG).
 *
 * @author Chris Jackson - Initial contribution
 * @author Holger Reichert - Support for themes, DPI, legend hiding
 * @author Jan N. Klug - add y-axis label formatter
 */
@NonNullByDefault
public interface ChartProvider {
    /**
     * Gets the name of this chart provider.
     *
     * @return String containing the provider name
     */
    String getName();

    /**
     * Creates a chart object. This sets the initial parameters for the chart
     * before the items are added
     *
     * @param service A string containing the name of the persistence service. May
     *            be null in which case the chart provider can decide itself
     *            which store to use.
     * @param theme A string containing a theme name for the chart. The provider
     *            should store its own themes. May be null to use a default
     *            theme.
     * @param startTime The start time of the chart
     * @param endTime The end time of the chart
     * @param height The height of the chart
     * @param width The width of the chart
     * @param items The items to display on the chart
     * @param groups The groups to display on the chart
     * @param dpi The DPI (dots per inch) value, can be <code>null</code>
     * @param legend Show the legend? If <code>null</code>, the ChartProvider should make his own decision.
     * @return BufferedImage object if the chart is rendered correctly,
     *         otherwise null.
     * @throws ItemNotFoundException if an item or group is not found
     * @throws IllegalArgumentException if an invalid argument is passed
     */
    BufferedImage createChart(@Nullable String service, @Nullable String theme, ZonedDateTime startTime,
            ZonedDateTime endTime, int height, int width, @Nullable String items, @Nullable String groups,
            @Nullable Integer dpi, @Nullable Boolean legend) throws ItemNotFoundException;

    /**
     * Creates a chart object. This sets the initial parameters for the chart
     * before the items are added
     *
     * @param service A string containing the name of the persistence service. May
     *            be null in which case the chart provider can decide itself
     *            which store to use.
     * @param theme A string containing a theme name for the chart. The provider
     *            should store its own themes. May be null to use a default
     *            theme.
     * @param startTime The start time of the chart
     * @param endTime The end time of the chart
     * @param height The height of the chart
     * @param width The width of the chart
     * @param items The items to display on the chart
     * @param groups The groups to display on the chart
     * @param dpi The DPI (dots per inch) value, can be <code>null</code>
     * @param yAxisDecimalPattern The format pattern for the y-axis labels
     * @param legend Show the legend? If <code>null</code>, the ChartProvider should make his own decision.
     * @return BufferedImage object if the chart is rendered correctly,
     *         otherwise null.
     * @throws ItemNotFoundException if an item or group is not found
     * @throws IllegalArgumentException if an invalid argument is passed
     */
    default BufferedImage createChart(@Nullable String service, @Nullable String theme, ZonedDateTime startTime,
            ZonedDateTime endTime, int height, int width, @Nullable String items, @Nullable String groups,
            @Nullable Integer dpi, @Nullable String yAxisDecimalPattern, @Nullable Boolean legend)
            throws ItemNotFoundException {
        return createChart(service, theme, startTime, endTime, height, width, items, groups, dpi, legend);
    }

    /**
     * Gets the type of data that will be written by the chart.
     *
     * @return ImageType
     */
    ImageType getChartType();

    /**
     * Provides a list of image types
     *
     */
    public enum ImageType {
        png,
        jpg,
        gif;
    }
}
