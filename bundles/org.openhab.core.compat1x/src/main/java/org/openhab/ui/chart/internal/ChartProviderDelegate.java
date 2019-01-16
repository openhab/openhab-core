/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.ui.chart.internal;

import java.awt.image.BufferedImage;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.openhab.ui.chart.ChartProvider;

/**
 * This class serves as a mapping from the "old" org.openhab namespace to the new org.eclipse.smarthome
 * namespace for the action service. It wraps an instance with the old interface
 * into a class with the new interface.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
public class ChartProviderDelegate implements org.eclipse.smarthome.ui.chart.ChartProvider {

    private ChartProvider provider;

    public ChartProviderDelegate(ChartProvider chartProvider) {
        this.provider = chartProvider;
    }

    @Override
    public String getName() {
        return provider.getName();
    }

    @Override
    public BufferedImage createChart(String service, String theme, Date startTime, Date endTime, int height, int width,
            String items, String groups, Integer dpi, Boolean legend) throws ItemNotFoundException {
        try {
            return provider.createChart(service, theme, startTime, endTime, height, width, items, groups);
        } catch (org.openhab.core.items.ItemNotFoundException e) {
            throw new ItemNotFoundException(StringUtils.substringBetween(e.getMessage(), "'"));
        }
    }

    @Override
    public ImageType getChartType() {
        return ImageType.valueOf(provider.getChartType().name());
    }

}
