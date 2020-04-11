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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.openhab.core.io.http.servlet.SmartHomeServlet;
import org.openhab.core.ui.icon.IconProvider;
import org.openhab.core.ui.icon.IconSet.Format;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers a servlet that serves icons through {@link IconProvider}s.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component
public class IconServlet extends SmartHomeServlet {

    private static final long serialVersionUID = 2880642275858634578L;

    private final Logger logger = LoggerFactory.getLogger(IconServlet.class);

    private static final String SERVLET_NAME = "/icon";
    static final String PARAM_ICONSET = "iconset";
    static final String PARAM_FORMAT = "format";
    static final String PARAM_ANY_FORMAT = "anyFormat";
    static final String PARAM_STATE = "state";

    private long startupTime;

    protected HttpService httpService;

    protected String defaultIconSetId = "classic";

    private List<IconProvider> iconProvider = new ArrayList<>();

    @Override
    @Reference
    public void setHttpService(HttpService httpService) {
        super.setHttpService(httpService);
    }

    @Override
    public void unsetHttpService(HttpService httpService) {
        super.unsetHttpService(httpService);
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
        Format format = getFormat(req);
        String state = getState(req);
        String iconSetId = getIconSetId(req);
        Format otherFormat = null;
        if ("true".equalsIgnoreCase(req.getParameter(PARAM_ANY_FORMAT))) {
            otherFormat = (format == Format.PNG) ? Format.SVG : Format.PNG;
        }

        IconProvider provider = getIconProvider(category, iconSetId, format);
        IconProvider provider2 = getIconProvider(category, iconSetId, otherFormat);
        if (provider2 != null) {
            if (provider == null) {
                provider = provider2;
                format = otherFormat;
            } else if (provider2 != provider) {
                Integer prio = provider.hasIcon(category, iconSetId, format);
                Integer prio2 = provider2.hasIcon(category, iconSetId, otherFormat);
                if (prio != null && prio2 != null && prio < prio2) {
                    provider = provider2;
                    format = otherFormat;
                }
            }
        }
        if (provider == null) {
            logger.debug("Requested icon category {} provided by no icon provider;", category);
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
            IOUtils.copy(is, os);
            resp.flushBuffer();
        } catch (IOException e) {
            logger.error("Failed sending the icon byte stream as a response: {}", e.getMessage());
            resp.sendError(500, e.getMessage());
        }
    }

    private String getCategory(HttpServletRequest req) {
        String category = StringUtils.substringAfterLast(req.getRequestURI(), "/");
        category = StringUtils.substringBeforeLast(category, ".");
        return StringUtils.substringBeforeLast(category, "-");
    }

    private Format getFormat(HttpServletRequest req) {
        String format = req.getParameter(PARAM_FORMAT);
        if (format == null) {
            String filename = StringUtils.substringAfterLast(req.getRequestURI(), "/");
            format = StringUtils.substringAfterLast(filename, ".");
        }
        try {
            Format f = Format.valueOf(format.toUpperCase());
            return f;
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

    private String getState(HttpServletRequest req) {
        String state = req.getParameter(PARAM_STATE);
        if (state != null) {
            return state;
        } else {
            String filename = StringUtils.substringAfterLast(req.getRequestURI(), "/");
            state = StringUtils.substringAfterLast(filename, "-");
            state = StringUtils.substringBeforeLast(state, ".");
            if (StringUtils.isNotEmpty(state)) {
                return state;
            } else {
                return null;
            }
        }
    }

    private IconProvider getIconProvider(String category, String iconSetId, Format format) {
        IconProvider topProvider = null;
        if (format != null) {
            int maxPrio = Integer.MIN_VALUE;
            for (IconProvider provider : iconProvider) {
                Integer prio = provider.hasIcon(category, iconSetId, format);
                if (prio != null && prio > maxPrio) {
                    maxPrio = prio;
                    topProvider = provider;
                }
            }
        }
        return topProvider;
    }
}
