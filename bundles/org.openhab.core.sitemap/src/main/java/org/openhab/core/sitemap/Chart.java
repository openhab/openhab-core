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
package org.openhab.core.sitemap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A representation of a sitemap Chart widget.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface Chart extends NonLinkableWidget {

    /**
     * Get the configured persistence service, if no service is configured, the default service should be used.
     *
     * @return service
     */
    @Nullable
    String getService();

    /**
     * Set the persistence service.
     *
     * @param service
     */
    void setService(String service);

    /**
     * Get the chart refresh interval in s. If no interval is set, 0 should be returned.
     *
     * @return refresh
     */
    int getRefresh();

    /**
     * Set the chart refresh interval in s.
     *
     * @param refresh
     */
    void setRefresh(@Nullable Integer refresh);

    /**
     * Get the configured chart time period. See {@link #setPeriod(String)}.
     *
     * @return period
     */
    String getPeriod();

    /**
     * Set the chart time axis scale.The time axis can be either entirely in the past ending at the present time,
     * entirely in the future starting at the present time, or partly in the past and partly in the future around the
     * present time. To do this, the value can be composed of two parts separated by the "-" character, the value before
     * the "-" is then the scale in the past and the value after the "-" is the scale in the future. Valid values before
     * and after the central character "-" are h, 2h, 3h, ..., D, 2D, 3D, ..., W, 2W, 3W, ..., M, 2M, 3M, ..., Y, 2Y,
     * ... and any valid duration following the ISO8601 duration notation such as P1Y6M for the last year and a half or
     * PT1H30M for the last hour and a half. If only a period is provided, i.e. without the final "-" character or
     * without anything after the "-" character, only a period in the past is taken into account.
     *
     * @param period
     */
    void setPeriod(String period);

    /**
     * Return true if legend should be shown.
     *
     * @return legend
     */
    boolean hasLegend();

    /**
     * Set to true if legend should be shown. If not set, the legend will not be shown if there is only a single series
     * in the chart.
     *
     * @param legend
     */
    void setLegend(@Nullable Boolean legend);

    /**
     * Return true if the group item will be shown instead of items in the group.
     *
     * @return forceAsItem
     */
    boolean forceAsItem();

    /**
     * Set to true if group item should be shown in the chart instead of items in the group (default).
     *
     * @param forceAsItem
     */
    void setForceAsItem(@Nullable Boolean forceAsItem);

    /**
     * Get the y axis value format pattern.
     *
     * @return yAxisDecimalPattern
     */
    @Nullable
    String getYAxisDecimalPattern();

    /**
     * Set the format for values on the y axis. It accepts {@link java.text.DecimalFormat}. For example with #.## a
     * number has 2 decimals.
     *
     * @param yAxisDecimalPattern
     */
    void setYAxisDecimalPattern(@Nullable String yAxisDecimalPattern);

    /**
     * Gets the interpolation parameter. See {@link #setInterpolation(String)}.
     *
     * @return interpolation
     */
    @Nullable
    String getInterpolation();

    /**
     * Sets the interpolation parameter. The interpolation parameter is used to change how the line is drawn between 2
     * datapoints. By default, a horizontal line (step) will be drawn between 2 datapoints of Switch or Contact items.
     * All other item types will have a line (linear) connecting the datapoints. With the "linear" or "step" value for
     * this parameter, this default behaviour can be changed.
     *
     * @param interpolation
     */
    void setInterpolation(@Nullable String interpolation);
}
