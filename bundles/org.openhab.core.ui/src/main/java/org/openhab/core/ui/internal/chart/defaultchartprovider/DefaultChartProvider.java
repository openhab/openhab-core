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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knowm.xchart.Chart;
import org.knowm.xchart.ChartBuilder;
import org.knowm.xchart.Series;
import org.knowm.xchart.SeriesMarker;
import org.knowm.xchart.StyleManager.LegendPosition;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
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
 */
@Component(immediate = true)
public class DefaultChartProvider implements ChartProvider {

    private final Logger logger = LoggerFactory.getLogger(DefaultChartProvider.class);

    private final TimeZoneProvider timeZoneProvider;
    protected final ItemUIRegistry itemUIRegistry;
    private final PersistenceServiceRegistry persistenceServiceRegistry;

    private int legendPosition = 0;

    private static final ChartTheme[] CHART_THEMES_AVAILABLE = { new ChartThemeWhite(), new ChartThemeBright(),
            new ChartThemeDark(), new ChartThemeBlack() };
    public static final String CHART_THEME_DEFAULT_NAME = "bright";
    private Map<String, ChartTheme> chartThemes = null;

    public static final int DPI_DEFAULT = 96;

    @Activate
    public DefaultChartProvider(final @Reference TimeZoneProvider timeZoneProvider,
            final @Reference ItemUIRegistry itemUIRegistry,
            final @Reference PersistenceServiceRegistry persistenceServiceRegistry) {
        this.timeZoneProvider = timeZoneProvider;
        this.itemUIRegistry = itemUIRegistry;
        this.persistenceServiceRegistry = persistenceServiceRegistry;
    }

    @Activate
    protected void activate() {
        logger.debug("Starting up default chart provider.");
        String themeNames = Arrays.stream(CHART_THEMES_AVAILABLE) //
                .map(t -> t.getThemeName()) //
                .collect(Collectors.joining(", "));
        logger.debug("Available themes for default chart provider: {}", themeNames);
    }

    @Override
    public String getName() {
        return "default";
    }

    @Override
    public BufferedImage createChart(String serviceId, String theme, Date startTime, Date endTime, int height,
            int width, String items, String groups, Integer dpiValue, Boolean legend)
            throws ItemNotFoundException, IllegalArgumentException {
        logger.debug(
                "Rendering chart: service: '{}', theme: '{}', startTime: '{}', endTime: '{}', width: '{}', height: '{}', items: '{}', groups: '{}', dpi: '{}', legend: '{}'",
                serviceId, theme, startTime, endTime, width, height, items, groups, dpiValue, legend);

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
        ChartTheme chartTheme = getChartTheme(theme);

        // get DPI
        int dpi;
        if (dpiValue != null && dpiValue > 0) {
            dpi = dpiValue;
        } else {
            dpi = DPI_DEFAULT;
        }

        // Create Chart
        Chart chart = new ChartBuilder().width(width).height(height).build();

        // Define the time axis - the defaults are not very nice
        long period = (endTime.getTime() - startTime.getTime()) / 1000;
        String pattern = "HH:mm";
        if (period <= 600) { // 10 minutes
            pattern = "mm:ss";
        } else if (period <= 86400) { // 1 day
            pattern = "HH:mm";
        } else if (period <= 604800) { // 1 week
            pattern = "EEE d";
        } else {
            pattern = "d MMM";
        }

        chart.getStyleManager().setDatePattern(pattern);
        // axis
        chart.getStyleManager().setAxisTickLabelsFont(chartTheme.getAxisTickLabelsFont(dpi));
        chart.getStyleManager().setAxisTickLabelsColor(chartTheme.getAxisTickLabelsColor());
        chart.getStyleManager().setXAxisMin(startTime.getTime());
        chart.getStyleManager().setXAxisMax(endTime.getTime());
        int yAxisSpacing = Math.max(height / 10, chartTheme.getAxisTickLabelsFont(dpi).getSize());
        chart.getStyleManager().setYAxisTickMarkSpacingHint(yAxisSpacing);
        // chart
        chart.getStyleManager().setChartBackgroundColor(chartTheme.getChartBackgroundColor());
        chart.getStyleManager().setChartFontColor(chartTheme.getChartFontColor());
        chart.getStyleManager().setChartPadding(chartTheme.getChartPadding(dpi));
        chart.getStyleManager().setPlotBackgroundColor(chartTheme.getPlotBackgroundColor());
        float plotGridLinesDash = (float) chartTheme.getPlotGridLinesDash(dpi);
        float[] plotGridLinesDashArray = { plotGridLinesDash, plotGridLinesDash };
        chart.getStyleManager().setPlotGridLinesStroke(
                new BasicStroke((float) chartTheme.getPlotGridLinesWidth(dpi), 0, 2, 10, plotGridLinesDashArray, 0));
        chart.getStyleManager().setPlotGridLinesColor(chartTheme.getPlotGridLinesColor());
        // legend
        chart.getStyleManager().setLegendBackgroundColor(chartTheme.getLegendBackgroundColor());
        chart.getStyleManager().setLegendFont(chartTheme.getLegendFont(dpi));
        chart.getStyleManager().setLegendSeriesLineLength(chartTheme.getLegendSeriesLineLength(dpi));

        // Loop through all the items
        if (items != null) {
            String[] itemNames = items.split(",");
            for (String itemName : itemNames) {
                Item item = itemUIRegistry.getItem(itemName);
                if (addItem(chart, persistenceService, startTime, endTime, item, seriesCounter, chartTheme, dpi)) {
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
                                dpi)) {
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

            xData.add(startTime);
            yData.add(0);
            xData.add(endTime);
            yData.add(0);

            Series series = chart.addSeries("NONE", xData, yData);
            series.setMarker(SeriesMarker.NONE);
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
            if (legendPosition < 0) {
                chart.getStyleManager().setLegendPosition(LegendPosition.InsideNW);
            } else {
                chart.getStyleManager().setLegendPosition(LegendPosition.InsideSW);
            }
        } else { // hide the whole legend
            chart.getStyleManager().setLegendVisible(false);
        }

        // Write the chart as a PNG image
        BufferedImage lBufferedImage = new BufferedImage(chart.getWidth(), chart.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D lGraphics2D = lBufferedImage.createGraphics();
        chart.paint(lGraphics2D);
        return lBufferedImage;
    }

    double convertData(State state) {
        if (state instanceof DecimalType) {
            return ((DecimalType) state).doubleValue();
        } else if (state instanceof OnOffType) {
            return (state == OnOffType.OFF) ? 0 : 1;
        } else if (state instanceof OpenClosedType) {
            return (state == OpenClosedType.CLOSED) ? 0 : 1;
        } else {
            logger.debug("Unsupported item type in chart: {}", state.getClass().toString());
            return 0;
        }
    }

    boolean addItem(Chart chart, QueryablePersistenceService service, Date timeBegin, Date timeEnd, Item item,
            int seriesCounter, ChartTheme chartTheme, int dpi) {
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
        filter.setEndDate(ZonedDateTime.ofInstant(timeBegin.toInstant(), timeZoneProvider.getTimeZone()));
        filter.setItemName(item.getName());
        filter.setPageSize(1);
        filter.setOrdering(Ordering.DESCENDING);
        result = service.query(filter);
        if (result.iterator().hasNext()) {
            HistoricItem historicItem = result.iterator().next();

            state = historicItem.getState();
            xData.add(timeBegin);
            yData.add(convertData(state));
        }

        // Now, get all the data between the start and end time
        filter.setBeginDate(ZonedDateTime.ofInstant(timeBegin.toInstant(), timeZoneProvider.getTimeZone()));
        filter.setEndDate(ZonedDateTime.ofInstant(timeEnd.toInstant(), timeZoneProvider.getTimeZone()));
        filter.setPageSize(Integer.MAX_VALUE);
        filter.setOrdering(Ordering.ASCENDING);

        // Get the data from the persistence store
        result = service.query(filter);
        Iterator<HistoricItem> it = result.iterator();

        // Iterate through the data
        while (it.hasNext()) {
            HistoricItem historicItem = it.next();

            // For 'binary' states, we need to replicate the data
            // to avoid diagonal lines
            if (state instanceof OnOffType || state instanceof OpenClosedType) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(historicItem.getTimestamp());
                cal.add(Calendar.MILLISECOND, -1);
                xData.add(cal.getTime());
                yData.add(convertData(state));
            }

            state = historicItem.getState();
            xData.add(historicItem.getTimestamp());
            yData.add(convertData(state));
        }

        // Lastly, add the final state at the endtime
        if (state != null) {
            xData.add(timeEnd);
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

        Series series = chart.addSeries(label, xData, yData);
        float lineWidth = (float) chartTheme.getLineWidth(dpi);
        series.setLineStyle(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
        series.setMarker(SeriesMarker.NONE);
        series.setLineColor(color);

        // If the start value is below the median, then count legend position down
        // Otherwise count up.
        // We use this to decide whether to put the legend in the top or bottom corner.
        if (yData.iterator().next().floatValue() > ((series.getYMax() - series.getYMin()) / 2 + series.getYMin())) {
            legendPosition++;
        } else {
            legendPosition--;
        }

        return true;
    }

    @Override
    public ImageType getChartType() {
        return (ImageType.png);
    }

    /**
     * Retrieve a chart theme by it's name. If no name is given or no theme with the given name exists, the
     * {@link DefaultChartProvider#CHART_THEME_DEFAULT_NAME default theme} gets returned.
     *
     * @param name the {@link ChartTheme#getThemeName() theme name}
     * @return {@link ChartTheme}
     */
    private ChartTheme getChartTheme(String name) {
        // if the static chartThemes hashmap is nul, we have to fill it first with all available themes,
        // based on the theme name
        if (chartThemes == null) {
            chartThemes = new HashMap<>();
            for (ChartTheme theme : CHART_THEMES_AVAILABLE) {
                chartThemes.put(theme.getThemeName(), theme);
            }
        }
        String chartThemeName = name;
        // no theme name -> default theme
        if (name == null || name.isBlank()) {
            chartThemeName = CHART_THEME_DEFAULT_NAME;
        }
        ChartTheme chartTheme = chartThemes.get(chartThemeName);
        if (chartTheme == null) {
            // no theme with the given name found -> default theme
            chartTheme = chartThemes.get(CHART_THEME_DEFAULT_NAME);
        }
        return chartTheme;
    }
}
