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
package org.openhab.core.sitemap.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.sitemap.Chart;
import org.openhab.core.sitemap.Parent;

/**
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class ChartImpl extends NonLinkableWidgetImpl implements Chart {

    private @Nullable String service;
    private @Nullable Integer refresh;
    private String period = "";
    private @Nullable Boolean legend;
    private @Nullable Boolean forceAsItem;
    private @Nullable String yAxisDecimalPattern;
    private @Nullable String interpolation;

    public ChartImpl() {
        super();
    }

    public ChartImpl(Parent parent) {
        super(parent);
    }

    @Override
    public @Nullable String getService() {
        return service;
    }

    @Override
    public void setService(String service) {
        this.service = service;
    }

    @Override
    public int getRefresh() {
        return refresh != null ? refresh : 0;
    }

    @Override
    public void setRefresh(@Nullable Integer refresh) {
        this.refresh = refresh;
    }

    @Override
    public String getPeriod() {
        return period;
    }

    @Override
    public void setPeriod(String period) {
        this.period = period;
    }

    @Override
    public boolean hasLegend() {
        return legend != null ? legend : false;
    }

    @Override
    public void setLegend(@Nullable Boolean legend) {
        this.legend = legend;
    }

    @Override
    public boolean forceAsItem() {
        return forceAsItem != null ? forceAsItem : false;
    }

    @Override
    public void setForceAsItem(@Nullable Boolean forceAsItem) {
        this.forceAsItem = forceAsItem;
    }

    @Override
    public @Nullable String getYAxisDecimalPattern() {
        return yAxisDecimalPattern;
    }

    @Override
    public void setYAxisDecimalPattern(@Nullable String yAxisDecimalPattern) {
        this.yAxisDecimalPattern = yAxisDecimalPattern;
    }

    @Override
    public @Nullable String getInterpolation() {
        return interpolation;
    }

    @Override
    public void setInterpolation(@Nullable String interpolation) {
        this.interpolation = interpolation;
    }
}
