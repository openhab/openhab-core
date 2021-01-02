/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.ui.icon.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.http.servlet.OpenHABServlet;
import org.openhab.core.ui.icon.IconProvider;
import org.openhab.core.ui.icon.IconSet.Format;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers a servlet that serves icons through {@link IconProvider}s.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component
@NonNullByDefault
public class IconServlet extends OpenHABServlet {

    private static final long serialVersionUID = 2880642275858634578L;

    private final Logger logger = LoggerFactory.getLogger(IconServlet.class);

    private static final String SERVLET_NAME = "/icon";
    static final String PARAM_ICONSET = "iconset";
    static final String PARAM_FORMAT = "format";
    static final String PARAM_ANY_FORMAT = "anyFormat";
    static final String PARAM_STATE = "state";

    private long startupTime;

    protected String defaultIconSetId = "classic";

    private final List<IconProvider> iconProvider = new ArrayList<>();

    @Activate
    public IconServlet(final @Reference HttpService httpService, final @Reference HttpContext httpContext) {
        super(httpService, httpContext);
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    public void addIconProvider(IconProvider iconProvider) {
        this.iconProvider.add(iconProvider);
    }

    public void removeIconProvider(IconProvider iconProvider) {
        this.iconProvider.remove(iconProvider);
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        super.activate(SERVLET_NAME);
        startupTime = System.currentTimeMillis();

        modified(config);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate(SERVLET_NAME);
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        Object iconSetId = config.get("default");
        if (iconSetId instanceof String) {
            defaultIconSetId = (String) iconSetId;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getDateHeader("If-Modified-Since") > startupTime) {
            resp.setStatus(304);
            return;
        }

        String category = getCategory(req);
        String state = getState(req);
        String iconSetId = getIconSetId(req);

        Format format = getFormat(req);
        Format otherFormat = null;
        if ("true".equalsIgnoreCase(req.getParameter(PARAM_ANY_FORMAT))) {
            otherFormat = (format == Format.PNG) ? Format.SVG : Format.PNG;
        }

        IconProvider provider = getIconProvider(category, iconSetId, format);

        if (otherFormat != null) {
            IconProvider provider2 = getIconProvider(category, iconSetId, otherFormat);
            if (provider2 != null) {
                if (provider == null) {
                    provider = provider2;
                    format = otherFormat;
                } else if (provider2 != provider) {
                    Integer prio = provider.hasIcon(category, iconSetId, format);
                    Integer prio2 = provider2.hasIcon(category, iconSetId, otherFormat);
                    if ((prio != null && prio2 != null && prio < prio2) || (prio == null && prio2 != null)) {
                        provider = provider2;
                        format = otherFormat;
                    }
                }
            }
        }

        if (provider == null) {
            logger.debug("Requested icon category {} provided by no icon provider", category);
            resp.sendError(404);
            return;
        }

        if (Format.SVG.equals(format)) {
            resp.setContentType("image/svg+xml");
        } else {
            resp.setContentType("image/png");
        }
        resp.setDateHeader("Last-Modified", new Date().getTime());
        ServletOutputStream os = resp.getOutputStream();
        try (InputStream is = provider.getIcon(category, iconSetId, state, format)) {
            if (is == null) {
                logger.debug("Requested icon category {} provided by no icon provider", category);
                resp.sendError(404);
                return;
            }
            is.transferTo(os);
            resp.flushBuffer();
        } catch (IOException e) {
            logger.error("Failed sending the icon byte stream as a response: {}", e.getMessage());
            resp.sendError(500, e.getMessage());
        }
    }

    private String substringAfterLast(@Nullable String str, String separator) {
        if (str == null) {
            return "";
        }
        int index = str.lastIndexOf(separator);
        return index == -1 || index == str.length() - separator.length() ? ""
                : str.substring(index + separator.length());
    }

    private String substringBeforeLast(String str, String separator) {
        int index = str.lastIndexOf(separator);
        return index == -1 ? str : str.substring(0, index);
    }

    private String getCategory(HttpServletRequest req) {
        String category = substringAfterLast(req.getRequestURI(), "/");
        category = substringBeforeLast(category, ".");
        return substringBeforeLast(category, "-");
    }

    private Format getFormat(HttpServletRequest req) {
        String format = req.getParameter(PARAM_FORMAT);
        if (format == null) {
            String requestURI = req.getRequestURI();
            if (requestURI == null) {
                logger.debug("null request URI in HTTP request - falling back to PNG");
                return Format.PNG;
            }

            String filename = substringAfterLast(requestURI, "/");
            format = substringAfterLast(filename, ".");
        }
        try {
            return Format.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.debug("unknown format '{}' in HTTP request - falling back to PNG", format);
            return Format.PNG;
        }
    }

    private String getIconSetId(HttpServletRequest req) {
        String iconSetId = req.getParameter(PARAM_ICONSET);
        if (iconSetId == null || iconSetId.isEmpty()) {
            return defaultIconSetId;
        } else {
            return iconSetId;
        }
    }

    private @Nullable String getState(HttpServletRequest req) {
        String state = req.getParameter(PARAM_STATE);
        if (state != null) {
            return state;
        } else {
            String filename = substringAfterLast(req.getRequestURI(), "/");
            state = substringAfterLast(filename, "-");
            state = substringBeforeLast(state, ".");
            if (!state.isEmpty()) {
                return state;
            } else {
                return null;
            }
        }
    }

    private @Nullable IconProvider getIconProvider(String category, String iconSetId, Format format) {
        IconProvider topProvider = null;
        int maxPrio = Integer.MIN_VALUE;
        for (IconProvider provider : iconProvider) {
            Integer prio = provider.hasIcon(category, iconSetId, format);
            if (prio != null && prio > maxPrio) {
                maxPrio = prio;
                topProvider = provider;
            }
        }
        return topProvider;
    }
}
