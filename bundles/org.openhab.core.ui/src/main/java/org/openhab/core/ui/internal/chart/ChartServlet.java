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
package org.openhab.core.ui.internal.chart;

import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.io.http.servlet.SmartHomeServlet;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.ui.chart.ChartProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

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
 */
@Component(immediate = true, service = ChartServlet.class, configurationPid = "org.openhab.chart", property = {
        Constants.SERVICE_PID + "=org.openhab.core.chart", ConfigurableService.SERVICE_PROPERTY_CATEGORY + "=system",
        ConfigurableService.SERVICE_PROPERTY_LABEL + "=Charts",
        ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI + "=" + "system:chart" })
public class ChartServlet extends SmartHomeServlet {

    private static final long serialVersionUID = 7700873790924746422L;
    private static final int CHART_HEIGHT = 240;
    private static final int CHART_WIDTH = 480;
    private static final String DATE_FORMAT = "yyyyMMddHHmm";

    private String providerName = "default";
    private int defaultHeight = CHART_HEIGHT;
    private int defaultWidth = CHART_WIDTH;
    private double scale = 1.0;
    private int maxWidth = -1;

    // The URI of this servlet
    public static final String SERVLET_NAME = "/chart";

    protected static final Map<String, Long> PERIODS = Collections.unmodifiableMap(Stream.of( //
            new SimpleEntry<>("h", 3600000L), new SimpleEntry<>("4h", 14400000L), //
            new SimpleEntry<>("8h", 28800000L), new SimpleEntry<>("12h", 43200000L), //
            new SimpleEntry<>("D", 86400000L), new SimpleEntry<>("2D", 172800000L), //
            new SimpleEntry<>("3D", 259200000L), new SimpleEntry<>("W", 604800000L), //
            new SimpleEntry<>("2W", 1209600000L), new SimpleEntry<>("M", 2592000000L), //
            new SimpleEntry<>("2M", 5184000000L), new SimpleEntry<>("4M", 10368000000L), //
            new SimpleEntry<>("Y", 31536000000L)//
    ).collect(Collectors.toMap(Entry::getKey, Entry::getValue)));

    protected static final Map<String, ChartProvider> CHART_PROVIDERS = new ConcurrentHashMap<>();

    @Activate
    public ChartServlet(final @Reference HttpService httpService, final @Reference HttpContext httpContext) {
        super.setHttpService(httpService);
        super.setHttpContext(httpContext);
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
        super.activate(SERVLET_NAME);
        applyConfig(config);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate(SERVLET_NAME);
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        applyConfig(config);
    }

    /**
     * Handle the initial or a changed configuration.
     *
     * @param config the configuration
     */
    private void applyConfig(Map<String, Object> config) {
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

        // To avoid ambiguity you are not allowed to specify period, begin and end time at the same time.
        if (req.getParameter("period") != null && req.getParameter("begin") != null
                && req.getParameter("end") != null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Do not specify the three parameters period, begin and end at the same time.");
            return;
        }

        // Read out the parameter period, begin and end and save them.
        Date timeBegin = null;
        Date timeEnd = null;

        Long period = PERIODS.get(req.getParameter("period"));
        if (period == null) {
            // use a day as the default period
            period = PERIODS.get("D");
        }

        if (req.getParameter("begin") != null) {
            try {
                timeBegin = new SimpleDateFormat(DATE_FORMAT).parse(req.getParameter("begin"));
            } catch (ParseException e) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Begin and end must have this format: " + DATE_FORMAT + ".");
                return;
            }
        }

        if (req.getParameter("end") != null) {
            try {
                timeEnd = new SimpleDateFormat(DATE_FORMAT).parse(req.getParameter("end"));
            } catch (ParseException e) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Begin and end must have this format: " + DATE_FORMAT + ".");
                return;
            }
        }

        // Set begin and end time and check legality.
        if (timeBegin == null && timeEnd == null) {
            timeEnd = new Date();
            timeBegin = new Date(timeEnd.getTime() - period);
            logger.debug("No begin or end is specified, use now as end and now-period as begin.");
        } else if (timeEnd == null) {
            timeEnd = new Date(timeBegin.getTime() + period);
            logger.debug("No end is specified, use begin + period as end.");
        } else if (timeBegin == null) {
            timeBegin = new Date(timeEnd.getTime() - period);
            logger.debug("No begin is specified, use end-period as begin");
        } else if (timeEnd.before(timeBegin)) {
            throw new ServletException("The end is before the begin.");
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
        if (req.getParameter("dpi") != null) {
            try {
                dpi = Integer.valueOf(req.getParameter("dpi"));
            } catch (NumberFormatException e) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "dpi parameter is invalid");
                return;
            }
            if (dpi <= 0) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "dpi parameter is <= 0");
                return;
            }
        }

        // Read out parameter 'legend'
        Boolean legend = null;
        if (req.getParameter("legend") != null) {
            legend = Boolean.valueOf(req.getParameter("legend"));
        }

        if (maxWidth > 0 && width > maxWidth) {
            height = Math.round((float) height / (float) width * maxWidth);
            if (dpi != null) {
                dpi = Math.round((float) dpi / (float) width * maxWidth);
            }
            width = maxWidth;
        }

        // Set the content type to that provided by the chart provider
        res.setContentType("image/" + provider.getChartType());
        logger.debug("chart building with width {} height {} dpi {}", width, height, dpi);
        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(res.getOutputStream())) {
            BufferedImage chart = provider.createChart(serviceName, req.getParameter("theme"), timeBegin, timeEnd,
                    height, width, req.getParameter("items"), req.getParameter("groups"), dpi, legend);
            ImageIO.write(chart, provider.getChartType().toString(), imageOutputStream);
            logger.debug("Chart successfully generated and written to the response.");
        } catch (ItemNotFoundException e) {
            logger.debug("{}", e.getMessage());
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("Illegal argument in chart: {}", e.getMessage());
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Illegal argument in chart: " + e.getMessage());
        } catch (IIOException | EOFException e) {
            // this can happen if the request is terminated while the image is streamed, see
            // https://github.com/openhab/openhab-distro/issues/684
            logger.debug("Failed writing image to response stream", e);
        } catch (RuntimeException e) {
            if (logger.isDebugEnabled()) {
                // we also attach the stack trace
                logger.warn("Chart generation failed: {}", e.getMessage(), e);
            } else {
                logger.warn("Chart generation failed: {}", e.getMessage());
            }
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {
    }
}
