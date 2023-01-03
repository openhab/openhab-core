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
package org.openhab.core.ui.internal.chart.defaultchartprovider;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.markers.None;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.FilterCriteria.Ordering;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.PersistenceServiceRegistry;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.types.State;
import org.openhab.core.ui.chart.ChartProvider;
import org.openhab.core.ui.internal.chart.ChartServlet;
import org.openhab.core.ui.items.ItemUIRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This default chart provider generates time-series charts for a given set of items.
 *
 * See {@link ChartProvider} and {@link ChartServlet} for further details.
 *
 * @author Chris Jackson - Initial contribution
 * @author Holger Reichert - Support for themes, DPI, legend hiding
 * @author Christoph Weitkamp - Consider default persistence service
 * @author Jan N. Klug - Add y-axis label formatter
 */
@NonNullByDefault
@Component(immediate = true)
public class DefaultChartProvider implements ChartProvider {

    private static class LegendPositionDecider {
        private int counter = 0;

        private void addData(XYSeries series, List<Number> yData) {
            // If the start value is below the median, then count legend position down
            // Otherwise count up.
            // We use this to decide whether to put the legend in the top or bottom corner.
            if (yData.iterator().next().floatValue() > ((series.getYMax() - series.getYMin()) / 2 + series.getYMin())) {
                counter++;
            } else {
                counter--;
            }
        }

        private LegendPosition getLegendPosition() {
            return counter < 0 ? LegendPosition.InsideNW : LegendPosition.InsideSW;
        }
    }

    private static final Duration TEN_MINUTES = Duration.ofMinutes(10);
    private static final Duration ONE_DAY = Duration.ofDays(1);
    private static final Duration ONE_WEEK = Duration.ofDays(7);

    private static final ChartTheme CHART_THEME_DEFAULT = new ChartThemeBright();
    private static final Map<String, ChartTheme> CHART_THEMES = Stream
            .of(CHART_THEME_DEFAULT, new ChartThemeBrightTransparent(), //
                    new ChartThemeWhite(), new ChartThemeWhiteTransparent(), //
                    new ChartThemeDark(), new ChartThemeDarkTransparent(), //
                    new ChartThemeBlack(), new ChartThemeBlackTransparent()) //
            .collect(Collectors.toMap(ChartTheme::getThemeName, Function.identity()));

    private static final int DPI_DEFAULT = 96;

    private final Logger logger = LoggerFactory.getLogger(DefaultChartProvider.class);

    private final ItemUIRegistry itemUIRegistry;
    private final PersistenceServiceRegistry persistenceServiceRegistry;

    @Activate
    public DefaultChartProvider(final @Reference ItemUIRegistry itemUIRegistry,
            final @Reference PersistenceServiceRegistry persistenceServiceRegistry) {
        this.itemUIRegistry = itemUIRegistry;
        this.persistenceServiceRegistry = persistenceServiceRegistry;

        if (logger.isDebugEnabled()) {
            logger.debug("Available themes for default chart provider: {}", String.join(", ", CHART_THEMES.keySet()));
        }
    }

    @Override
    public String getName() {
        return "default";
    }

    @Override
    public BufferedImage createChart(@Nullable String serviceId, @Nullable String theme, ZonedDateTime startTime,
            ZonedDateTime endTime, int height, int width, @Nullable String items, @Nullable String groups,
            @Nullable Integer dpiValue, @Nullable Boolean legend)
            throws ItemNotFoundException, IllegalArgumentException {
        return createChart(serviceId, theme, startTime, endTime, height, width, items, groups, dpiValue, null, legend);
    }

    @Override
    public BufferedImage createChart(@Nullable String serviceId, @Nullable String theme, ZonedDateTime startTime,
            ZonedDateTime endTime, int height, int width, @Nullable String items, @Nullable String groups,
            @Nullable Integer dpiValue, @Nullable String yAxisDecimalPattern, @Nullable Boolean legend)
            throws ItemNotFoundException, IllegalArgumentException {
        logger.debug(
                "Rendering chart: service: '{}', theme: '{}', startTime: '{}', endTime: '{}', width: '{}', height: '{}', items: '{}', groups: '{}', dpi: '{}', yAxisDecimalPattern: '{}', legend: '{}'",
                serviceId, theme, startTime, endTime, width, height, items, groups, dpiValue, yAxisDecimalPattern,
                legend);

        // If a persistence service is specified, find the provider, or use the default provider
        PersistenceService service = (serviceId == null) ? persistenceServiceRegistry.getDefault()
                : persistenceServiceRegistry.get(serviceId);

        // Did we find a service?
        QueryablePersistenceService persistenceService = (service instanceof QueryablePersistenceService)
                ? (QueryablePersistenceService) service
                : (QueryablePersistenceService) persistenceServiceRegistry.getAll() //
                        .stream() //
                        .filter(it -> it instanceof QueryablePersistenceService) //
                        .findFirst() //
                        .orElseThrow(() -> new IllegalArgumentException("No Persistence service found."));

        int seriesCounter = 0;

        // get theme
        ChartTheme chartTheme = theme == null ? CHART_THEME_DEFAULT
                : CHART_THEMES.getOrDefault(theme, CHART_THEME_DEFAULT);

        // get DPI
        int dpi = dpiValue != null && dpiValue > 0 ? dpiValue : DPI_DEFAULT;

        // Create Chart
        XYChart chart = new XYChartBuilder().width(width).height(height).build();

        // Define the time axis - the defaults are not very nice
        Duration period = Duration.between(startTime, endTime);
        String pattern;

        if (period.compareTo(TEN_MINUTES) <= 0) {
            pattern = "mm:ss";
        } else if (period.compareTo(ONE_DAY) <= 0) {
            pattern = "HH:mm";
        } else if (period.compareTo(ONE_WEEK) <= 0) {
            pattern = "EEE d";
        } else {
            pattern = "d MMM";
        }

        XYStyler styler = chart.getStyler();
        styler.setDatePattern(pattern);
        // axis
        styler.setAxisTickLabelsFont(chartTheme.getAxisTickLabelsFont(dpi));
        styler.setAxisTickLabelsColor(chartTheme.getAxisTickLabelsColor());
        styler.setXAxisMin((double) startTime.toInstant().toEpochMilli());
        styler.setXAxisMax((double) endTime.toInstant().toEpochMilli());
        int yAxisSpacing = Math.max(height / 10, chartTheme.getAxisTickLabelsFont(dpi).getSize());
        if (yAxisDecimalPattern != null) {
            styler.setYAxisDecimalPattern(yAxisDecimalPattern);
        }
        styler.setYAxisTickMarkSpacingHint(yAxisSpacing);
        styler.setYAxisLabelAlignment(Styler.TextAlignment.Right);
        // chart
        styler.setChartBackgroundColor(chartTheme.getChartBackgroundColor());
        styler.setChartFontColor(chartTheme.getChartFontColor());
        styler.setChartPadding(chartTheme.getChartPadding(dpi));
        styler.setPlotBackgroundColor(chartTheme.getPlotBackgroundColor());
        float plotGridLinesDash = (float) chartTheme.getPlotGridLinesDash(dpi);
        float[] plotGridLinesDashArray = { plotGridLinesDash, plotGridLinesDash };
        styler.setPlotGridLinesStroke(new BasicStroke((float) chartTheme.getPlotGridLinesWidth(dpi),
                BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, plotGridLinesDashArray, 0));
        styler.setPlotGridLinesColor(chartTheme.getPlotGridLinesColor());
        // legend
        styler.setLegendBackgroundColor(chartTheme.getLegendBackgroundColor());
        styler.setLegendFont(chartTheme.getLegendFont(dpi));
        styler.setLegendSeriesLineLength(chartTheme.getLegendSeriesLineLength(dpi));

        LegendPositionDecider legendPositionDecider = new LegendPositionDecider();

        // Loop through all the items
        if (items != null) {
            String[] itemNames = items.split(",");
            for (String itemName : itemNames) {
                Item item = itemUIRegistry.getItem(itemName);
                if (addItem(chart, persistenceService, startTime, endTime, item, seriesCounter, chartTheme, dpi,
                        legendPositionDecider)) {
                    seriesCounter++;
                }
            }
        }

        // Loop through all the groups and add each item from each group
        if (groups != null) {
            String[] groupNames = groups.split(",");
            for (String groupName : groupNames) {
                Item item = itemUIRegistry.getItem(groupName);
                if (item instanceof GroupItem) {
                    GroupItem groupItem = (GroupItem) item;
                    for (Item member : groupItem.getMembers()) {
                        if (addItem(chart, persistenceService, startTime, endTime, member, seriesCounter, chartTheme,
                                dpi, legendPositionDecider)) {
                            seriesCounter++;
                        }
                    }
                } else {
                    throw new ItemNotFoundException("Item '" + item.getName() + "' defined in groups is not a group.");
                }
            }
        }

        Boolean showLegend = null;

        // If there are no series, render a blank chart
        if (seriesCounter == 0) {
            // always hide the legend
            showLegend = false;

            List<Date> xData = new ArrayList<>();
            List<Number> yData = new ArrayList<>();

            xData.add(Date.from(startTime.toInstant()));
            yData.add(0);
            xData.add(Date.from(endTime.toInstant()));
            yData.add(0);

            XYSeries series = chart.addSeries("NONE", xData, yData);
            series.setMarker(new None());
            series.setLineStyle(new BasicStroke(0f));
        }

        // if the legend is not already hidden, check if legend parameter is supplied, or calculate a sensible value
        if (showLegend == null) {
            if (legend == null) {
                // more than one series, show the legend. otherwise hide it.
                showLegend = seriesCounter > 1;
            } else {
                // take value from supplied legend parameter
                showLegend = legend;
            }
        }

        // Legend position (top-left or bottom-left) is dynamically selected based on the data
        // This won't be perfect, but it's a good compromise
        if (showLegend) {
            styler.setLegendPosition(legendPositionDecider.getLegendPosition());
        } else { // hide the whole legend
            styler.setLegendVisible(false);
        }

        // Write the chart as a PNG image
        BufferedImage lBufferedImage = new BufferedImage(chart.getWidth(), chart.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D lGraphics2D = lBufferedImage.createGraphics();
        chart.paint(lGraphics2D, chart.getWidth(), chart.getHeight());
        return lBufferedImage;
    }

    private double convertData(State state) {
        if (state instanceof DecimalType) {
            return ((DecimalType) state).doubleValue();
        } else if (state instanceof QuantityType) {
            return ((QuantityType<?>) state).doubleValue();
        } else if (state instanceof OnOffType) {
            return state == OnOffType.OFF ? 0 : 1;
        } else if (state instanceof OpenClosedType) {
            return state == OpenClosedType.CLOSED ? 0 : 1;
        } else {
            logger.debug("Unsupported item type in chart: {}", state.getClass().toString());
            return 0;
        }
    }

    private boolean addItem(XYChart chart, QueryablePersistenceService service, ZonedDateTime timeBegin,
            ZonedDateTime timeEnd, Item item, int seriesCounter, ChartTheme chartTheme, int dpi,
            LegendPositionDecider legendPositionDecider) {
        Color color = chartTheme.getLineColor(seriesCounter);

        // Get the item label
        String label = itemUIRegistry.getLabel(item.getName());
        if (label == null) {
            label = item.getName();
        } else if (label.contains("[") && label.contains("]")) {
            label = label.substring(0, label.indexOf('['));
        }

        Iterable<HistoricItem> result;
        FilterCriteria filter;

        // Generate data collections
        List<Date> xData = new ArrayList<>();
        List<Number> yData = new ArrayList<>();

        // Declare state here so it will hold the last value at the end of the process
        State state = null;

        // First, get the value at the start time.
        // This is necessary for values that don't change often otherwise data will start
        // after the start of the graph (or not at all if there's no change during the graph period)
        filter = new FilterCriteria();
        filter.setEndDate(timeBegin);
        filter.setItemName(item.getName());
        filter.setPageSize(1);
        filter.setOrdering(Ordering.DESCENDING);
        result = service.query(filter);
        if (result.iterator().hasNext()) {
            HistoricItem historicItem = result.iterator().next();

            state = historicItem.getState();
            xData.add(Date.from(timeBegin.toInstant()));
            yData.add(convertData(state));
        }

        // Now, get all the data between the start and end time
        filter.setBeginDate(timeBegin);
        filter.setEndDate(timeEnd);
        filter.setPageSize(Integer.MAX_VALUE);
        filter.setOrdering(Ordering.ASCENDING);

        // Get the data from the persistence store
        result = service.query(filter);

        // Iterate through the data
        for (HistoricItem historicItem : result) {
            // For 'binary' states, we need to replicate the data
            // to avoid diagonal lines
            if (state instanceof OnOffType || state instanceof OpenClosedType) {
                xData.add(Date.from(historicItem.getTimestamp().toInstant().minus(1, ChronoUnit.MILLIS)));
                yData.add(convertData(state));
            }

            state = historicItem.getState();
            xData.add(Date.from(historicItem.getTimestamp().toInstant()));
            yData.add(convertData(state));
        }

        // Lastly, add the final state at the endtime
        if (state != null) {
            xData.add(Date.from(timeEnd.toInstant()));
            yData.add(convertData(state));
        }

        // Add the new series to the chart - only if there's data elements to display
        // The chart engine will throw an exception if there's no data
        if (xData.isEmpty()) {
            return false;
        }

        // If there's only 1 data point, plot it again!
        if (xData.size() == 1) {
            xData.add(xData.iterator().next());
            yData.add(yData.iterator().next());
        }

        XYSeries series = chart.addSeries(label, xData, yData);
        float lineWidth = (float) chartTheme.getLineWidth(dpi);
        series.setLineStyle(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
        series.setMarker(new None());
        series.setLineColor(color);

        legendPositionDecider.addData(series, yData);
        return true;
    }

    @Override
    public ImageType getChartType() {
        return ImageType.png;
    }
}
