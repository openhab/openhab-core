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
package org.openhab.core.ui.icon.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.ui.icon.IconProvider;
import org.openhab.core.ui.icon.IconSet.Format;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletAsyncSupported;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletName;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers a servlet that serves icons through {@link IconProvider}s.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(service = Servlet.class)
@HttpWhiteboardServletAsyncSupported(asyncSupported = true)
@HttpWhiteboardServletName(IconServlet.SERVLET_PATH)
@HttpWhiteboardServletPattern(IconServlet.SERVLET_PATH + "/*")
@NonNullByDefault
public class IconServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 2880642275858634578L;

    private final Logger logger = LoggerFactory.getLogger(IconServlet.class);

    static final String SERVLET_PATH = "/icon";
    static final String PARAM_ICONSET = "iconset";
    static final String PARAM_FORMAT = "format";
    static final String PARAM_ANY_FORMAT = "anyFormat";
    static final String PARAM_STATE = "state";

    protected String defaultIconSetId = "classic";

    private final List<IconProvider> iconProvider = new ArrayList<>();

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    public void addIconProvider(IconProvider iconProvider) {
        this.iconProvider.add(iconProvider);
    }

    public void removeIconProvider(IconProvider iconProvider) {
        this.iconProvider.remove(iconProvider);
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        modified(config);
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        Object iconSetId = config.get("default");
        if (iconSetId instanceof String string) {
            defaultIconSetId = string;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String category = getCategory(req);
        if (category.isEmpty()) {
            logger.debug("URI must start with '{}' but is '{}'", SERVLET_PATH, req.getRequestURI());
            resp.sendError(400);
            return;
        }

        String state = req.getParameter(PARAM_STATE);
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
                } else if (!provider2.equals(provider)) {
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

        try (InputStream is = provider.getIcon(category, iconSetId, state, format)) {
            if (is == null) {
                logger.debug("Requested icon category {} provided by no icon provider", category);
                resp.sendError(404);
                return;
            }

            resp.setContentType(Format.SVG.equals(format) ? "image/svg+xml" : "image/png");
            resp.setHeader("Cache-Control", "max-age=31536000");
            is.transferTo(resp.getOutputStream());
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
        return substringBeforeLast(category, ".");
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
