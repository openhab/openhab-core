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
package org.openhab.core.ui.internal.chart;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serial;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.ui.chart.ChartProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletName;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet generates time-series charts for a given set of items. It
 * accepts the following HTTP parameters:
 * <ul>
 * <li>w: width in pixels of image to generate</li>
 * <li>h: height in pixels of image to generate</li>
 * <li>period: the time span for the x-axis. Value can be h,4h,8h,12h,D,3D,W,2W,M,2M,4M,Y</li>
 * <li>items: A comma separated list of item names to display</li>
 * <li>groups: A comma separated list of group names, whose members should be displayed</li>
 * <li>service: The persistence service name. If not supplied the first service found will be used.</li>
 * <li>theme: The chart theme to use. If not supplied the chart provider uses a default theme.</li>
 * <li>dpi: The DPI (dots per inch) value. If not supplied, a default is used.</code></li>
 * <li>legend: Show the legend? If not supplied, the ChartProvider should make his own decision.</li>
 * </ul>
 *
 * @author Chris Jackson - Initial contribution
 * @author Holger Reichert - Support for themes, DPI, legend hiding
 * @author Laurent Garnier - Extend support to ISO8601 format for chart period parameter
 * @author Laurent Garnier - Extend support to past and future for chart period parameter
 */
@Component(immediate = true, service = { ChartServlet.class, Servlet.class }, configurationPid = "org.openhab.chart", //
        property = Constants.SERVICE_PID + "=org.openhab.chart")
@ConfigurableService(category = "system", label = "Charts", description_uri = ChartServlet.CONFIG_URI)
@HttpWhiteboardServletName(ChartServlet.SERVLET_PATH)
@HttpWhiteboardServletPattern(ChartServlet.SERVLET_PATH + "/*")
@NonNullByDefault
public class ChartServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 7700873790924746422L;

    protected static final String CONFIG_URI = "system:chart";
    private static final int CHART_HEIGHT = 240;
    private static final int CHART_WIDTH = 480;
    private static final String DATE_FORMAT = "yyyyMMddHHmm";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    private final Logger logger = LoggerFactory.getLogger(ChartServlet.class);

    private final TimeZoneProvider timeZoneProvider;

    private String providerName = "default";
    private int defaultHeight = CHART_HEIGHT;
    private int defaultWidth = CHART_WIDTH;
    private double scale = 1.0;
    private int maxWidth = -1;

    // The URI of this servlet
    public static final String SERVLET_PATH = "/chart";

    protected static final Duration DEFAULT_PERIOD = Duration.ofDays(1);

    protected static final Map<String, ChartProvider> CHART_PROVIDERS = new ConcurrentHashMap<>();

    @Activate
    public ChartServlet(final @Reference TimeZoneProvider timeZoneProvider) {
        this.timeZoneProvider = timeZoneProvider;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addChartProvider(ChartProvider provider) {
        CHART_PROVIDERS.put(provider.getName(), provider);
    }

    public void removeChartProvider(ChartProvider provider) {
        CHART_PROVIDERS.remove(provider.getName());
    }

    public static Map<String, ChartProvider> getChartProviders() {
        return CHART_PROVIDERS;
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        applyConfig(config);
    }

    @Modified
    protected void modified(@Nullable Map<String, Object> config) {
        applyConfig(config);
    }

    /**
     * Handle the initial or a changed configuration.
     *
     * @param config the configuration
     */
    private void applyConfig(@Nullable Map<String, Object> config) {
        if (config == null) {
            return;
        }

        final String providerNameString = Objects.toString(config.get("provider"), null);
        if (providerNameString != null) {
            providerName = providerNameString;
        }

        final String defaultHeightString = Objects.toString(config.get("defaultHeight"), null);
        if (defaultHeightString != null) {
            try {
                defaultHeight = Integer.parseInt(defaultHeightString);
            } catch (NumberFormatException e) {
                logger.warn("'{}' is not a valid integer value for the defaultHeight parameter.", defaultHeightString);
            }
        }

        final String defaultWidthString = Objects.toString(config.get("defaultWidth"), null);
        if (defaultWidthString != null) {
            try {
                defaultWidth = Integer.parseInt(defaultWidthString);
            } catch (NumberFormatException e) {
                logger.warn("'{}' is not a valid integer value for the defaultWidth parameter.", defaultWidthString);
            }
        }

        final String scaleString = Objects.toString(config.get("scale"), null);
        if (scaleString != null) {
            try {
                scale = Double.parseDouble(scaleString);
                // Set scale to normal if the custom value is unrealistically low
                if (scale < 0.1) {
                    scale = 1.0;
                }
            } catch (NumberFormatException e) {
                logger.warn("'{}' is not a valid number value for the scale parameter.", scaleString);
            }
        }

        final String maxWidthString = Objects.toString(config.get("maxWidth"), null);
        if (maxWidthString != null) {
            try {
                maxWidth = Integer.parseInt(maxWidthString);
            } catch (NumberFormatException e) {
                logger.warn("'{}' is not a valid integer value for the maxWidth parameter.", maxWidthString);
            }
        }
    }

    @SuppressWarnings("null")
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        logger.debug("Received incoming chart request: {}", req);

        int width = defaultWidth;
        String w = req.getParameter("w");
        if (w != null) {
            try {
                width = Integer.parseInt(w);
            } catch (NumberFormatException e) {
                logger.debug("Ignoring invalid value '{}' for HTTP request parameter 'w'", w);
            }
        }
        int height = defaultHeight;
        String h = req.getParameter("h");
        if (h != null) {
            try {
                Double d = Double.parseDouble(h) * scale;
                height = d.intValue();
            } catch (NumberFormatException e) {
                logger.debug("Ignoring invalid value '{}' for HTTP request parameter 'h'", h);
            }
        }

        String periodParam = req.getParameter("period");
        String timeBeginParam = req.getParameter("begin");
        String timeEndParam = req.getParameter("end");

        // To avoid ambiguity you are not allowed to specify period, begin and end time at the same time.
        if (periodParam != null && timeBeginParam != null && timeEndParam != null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Do not specify the three parameters period, begin and end at the same time.");
            return;
        }

        ZonedDateTime timeBegin = null;
        ZonedDateTime timeEnd = null;

        if (timeBeginParam != null) {
            try {
                timeBegin = LocalDateTime.parse(timeBeginParam, FORMATTER).atZone(timeZoneProvider.getTimeZone());
            } catch (DateTimeParseException e) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Begin and end must have this format: " + DATE_FORMAT + ".");
                return;
            }
        }

        if (timeEndParam != null) {
            try {
                timeEnd = LocalDateTime.parse(timeEndParam, FORMATTER).atZone(timeZoneProvider.getTimeZone());
            } catch (DateTimeParseException e) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Begin and end must have this format: " + DATE_FORMAT + ".");
                return;
            }
        }

        PeriodPastFuture period = getPeriodPastFuture(periodParam);
        PeriodBeginEnd beginEnd = getPeriodBeginEnd(timeBegin, timeEnd, period,
                ZonedDateTime.now(timeZoneProvider.getTimeZone()));

        if (beginEnd.begin() != null && beginEnd.end() != null && beginEnd.end().isBefore(beginEnd.begin())) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "The end is before the begin.");
            return;
        }

        // If a persistence service is specified, find the provider
        String serviceName = req.getParameter("service");

        ChartProvider provider = getChartProviders().get(providerName);
        if (provider == null) {
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not get chart provider.");
            return;
        }

        // Read out the parameter 'dpi'
        Integer dpi = null;
        String dpiString = req.getParameter("dpi");
        if (dpiString != null) {
            try {
                dpi = Integer.valueOf(dpiString);
            } catch (NumberFormatException e) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "dpi parameter is invalid");
                return;
            }
            if (dpi <= 0) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "dpi parameter is <= 0");
                return;
            }
        }

        String yAxisDecimalPattern = req.getParameter("yAxisDecimalPattern");
        String interpolation = req.getParameter("interpolation");

        // Read out parameter 'legend'
        String legendParam = req.getParameter("legend");
        Boolean legend = null;
        if (legendParam != null) {
            legend = Boolean.valueOf(legendParam);
        }

        if (maxWidth > 0 && width > maxWidth) {
            height = Math.round((float) height / (float) width * maxWidth);
            if (dpi != null) {
                dpi = Math.round((float) dpi / (float) width * maxWidth);
            }
            width = maxWidth;
        }

        logger.debug("chart building with width {} height {} dpi {}", width, height, dpi);
        try {
            BufferedImage chart = provider.createChart(serviceName, req.getParameter("theme"), beginEnd.begin(),
                    beginEnd.end(), height, width, req.getParameter("items"), req.getParameter("groups"), dpi,
                    yAxisDecimalPattern, interpolation, legend);
            // Set the content type to that provided by the chart provider
            res.setContentType("image/" + provider.getChartType());
            try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(res.getOutputStream())) {
                ImageIO.write(chart, provider.getChartType().toString(), imageOutputStream);
                logger.debug("Chart successfully generated and written to the response.");
            }
        } catch (ItemNotFoundException e) {
            logger.debug("{}", e.getMessage());
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("Illegal argument in chart: {}", e.getMessage());
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Illegal argument in chart: " + e.getMessage());
        } catch (IOException e) {
            // this can happen if the request is terminated while the image is streamed, see
            // https://github.com/openhab/openhab-distro/issues/684
            logger.debug("Failed writing image to response stream", e);
        } catch (RuntimeException e) {
            logger.warn("Chart generation failed: {}", e.getMessage(), logger.isDebugEnabled() ? e : null);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public void init(@Nullable ServletConfig config) throws ServletException {
    }

    @Override
    public @Nullable ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public @Nullable String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {
    }

    protected static @Nullable TemporalAmount convertToTemporalAmount(@Nullable String periodParam,
            @Nullable TemporalAmount defaultPeriod) {
        TemporalAmount period = defaultPeriod;
        String convertedPeriod = convertPeriodToISO8601(periodParam);
        if (convertedPeriod != null) {
            boolean failed = false;
            try {
                period = Period.parse(convertedPeriod);
            } catch (DateTimeParseException e) {
                failed = true;
            }
            if (failed) {
                try {
                    period = Duration.parse(convertedPeriod);
                } catch (DateTimeParseException e) {
                    // Ignored
                }
            }
        }
        return period;
    }

    private static @Nullable String convertPeriodToISO8601(@Nullable String period) {
        if (period == null || period.startsWith("P") || !(period.endsWith("h") || period.endsWith("D")
                || period.endsWith("W") || period.endsWith("M") || period.endsWith("Y"))) {
            return period;
        }
        String newPeriod = period.length() == 1 ? "1" + period : period;
        if (newPeriod.endsWith("h")) {
            newPeriod = "T" + newPeriod.replace("h", "H");
        }
        return "P" + newPeriod;
    }

    protected static PeriodPastFuture getPeriodPastFuture(@Nullable String periodParam) {
        String periodParamPast = null;
        String periodParamFuture = null;
        TemporalAmount defaultPeriodPast = DEFAULT_PERIOD;
        TemporalAmount defaultPeriodFuture = null;
        if (periodParam != null) {
            int idx = periodParam.indexOf("-");
            if (idx < 0) {
                periodParamPast = periodParam;
            } else {
                if (idx > 0) {
                    periodParamPast = periodParam.substring(0, idx);
                } else {
                    defaultPeriodPast = null;
                    defaultPeriodFuture = DEFAULT_PERIOD;
                }
                periodParamFuture = periodParam.substring(idx + 1);
            }
        }
        TemporalAmount periodPast = convertToTemporalAmount(periodParamPast, defaultPeriodPast);
        TemporalAmount periodFuture = convertToTemporalAmount(periodParamFuture, defaultPeriodFuture);
        return new PeriodPastFuture(periodPast, periodFuture);
    }

    protected static PeriodBeginEnd getPeriodBeginEnd(@Nullable ZonedDateTime begin, @Nullable ZonedDateTime end,
            PeriodPastFuture period, ZonedDateTime now) {
        ZonedDateTime timeBegin = begin;
        ZonedDateTime timeEnd = end;
        TemporalAmount periodPast = period.past();
        TemporalAmount periodFuture = period.future();

        if (timeBegin == null && timeEnd == null) {
            timeBegin = timeEnd = now;
            if (periodPast != null) {
                timeBegin = timeBegin.minus(periodPast);
            }
            if (periodFuture != null) {
                timeEnd = timeEnd.plus(periodFuture);
            }
        } else if (timeBegin != null && timeEnd == null) {
            timeEnd = timeBegin;
            if (periodPast != null) {
                timeEnd = timeEnd.plus(periodPast);
            }
            if (periodFuture != null) {
                timeEnd = timeEnd.plus(periodFuture);
            }
        } else if (timeBegin == null && timeEnd != null) {
            timeBegin = timeEnd;
            if (periodFuture != null) {
                timeBegin = timeBegin.minus(periodFuture);
            }
            if (periodPast != null) {
                timeBegin = timeBegin.minus(periodPast);
            }
        }

        return new PeriodBeginEnd(Objects.requireNonNull(timeBegin), Objects.requireNonNull(timeEnd));
    }

    record PeriodPastFuture(@Nullable TemporalAmount past, @Nullable TemporalAmount future) {
    }

    record PeriodBeginEnd(ZonedDateTime begin, ZonedDateTime end) {
    }
}
