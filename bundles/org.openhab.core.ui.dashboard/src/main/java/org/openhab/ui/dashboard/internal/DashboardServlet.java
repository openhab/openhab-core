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
package org.openhab.ui.dashboard.internal;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openhab.core.OpenHAB;
import org.openhab.ui.dashboard.DashboardTile;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet constructs the main HTML page for the dashboard, listing all DashboardTiles
 * that are registered as a service.
 *
 * @author Kai Kreuzer
 * @author Laurent Garnier - internationalization
 * @author Hilbrand Bouwkamp - internationalization
 *
 */
public class DashboardServlet extends HttpServlet {

    private static final long serialVersionUID = -5154582000538034381L;

    private static final Pattern MESSAGE_KEY_PATTERN = Pattern.compile("\\$\\{([^\\}]+)\\}");

    private final Logger logger = LoggerFactory.getLogger(DashboardServlet.class);

    private ConfigurationAdmin configurationAdmin;

    private String indexTemplate;

    private String entryTemplate;

    private String warnTemplate;

    private String setupTemplate;

    private Set<DashboardTile> tiles;

    private Function<String, String> localizeFunction;

    public DashboardServlet(ConfigurationAdmin configurationAdmin, String indexTemplate, String entryTemplate,
            String warnTemplate, String setupTemplate, Set<DashboardTile> tiles,
            Function<String, String> localizeFunction) {
        this.configurationAdmin = configurationAdmin;
        this.indexTemplate = indexTemplate;
        this.entryTemplate = entryTemplate;
        this.warnTemplate = warnTemplate;
        this.setupTemplate = setupTemplate;
        this.tiles = tiles;
        this.localizeFunction = localizeFunction;
        isExposed(null);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (isSetup()) {
            serveDashboard(req, resp);
        } else {
            serveSetup(req, resp);
        }
    }

    private void serveDashboard(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        StringBuilder entries = new StringBuilder();
        for (DashboardTile tile : tiles) {
            Map<String, String> entryMap = new HashMap<>();
            entryMap.put("name", tile.getName());
            String overlay = tile.getOverlay() == null ? "none" : tile.getOverlay();

            entryMap.put("url", tile.getUrl());
            entryMap.put("overlay", overlay);
            entryMap.put("icon", tile.getImageUrl());
            entries.append(replaceKeysFromMap(entryTemplate, entryMap));
        }
        if (tiles.isEmpty()) {
            if ("minimal".equals(getPackage())) {
                entries.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
                entries.append("${entry.no-ui-installed}");
            } else {
                entries.append(
                        "&nbsp;&nbsp;&nbsp;&nbsp;<div class=\"spinner spinner--steps\"><img src=\"img/spinner.svg\"></div>&nbsp;&nbsp;");
                entries.append("${entry.install-running}");
            }
        }
        Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put("version", OpenHAB.getVersion() + " " + OpenHAB.buildString());
        replaceMap.put("entries", entries.toString());
        replaceMap.put("warn", isExposed(req) ? warnTemplate : "");
        // Set the messages in the session
        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().append(replaceKeysWithLocaleFunction(replaceKeysFromMap(indexTemplate, replaceMap)));
        resp.getWriter().close();
    }

    private void serveSetup(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getParameter("type") != null) {
            setPackage(req.getParameter("type"));
            resp.sendRedirect(req.getRequestURI());
        } else {
            Map<String, String> replaceMap = new HashMap<>();
            replaceMap.put("version", OpenHAB.getVersion() + " " + OpenHAB.buildString());
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().append(replaceKeysWithLocaleFunction(replaceKeysFromMap(setupTemplate, replaceMap)));
            resp.getWriter().close();
        }
    }

    private void setPackage(String parameter) {
        try {
            Configuration cfg = configurationAdmin.getConfiguration(OpenHAB.ADDONS_SERVICE_PID, null);
            Dictionary<String, Object> props = cfg.getProperties();
            if (props == null) {
                props = new Hashtable<>();
            }
            props.put(OpenHAB.CFG_PACKAGE, parameter);
            cfg.setBundleLocation(null);
            cfg.update(props);
        } catch (IOException e) {
            logger.error("Error while accessing the configuration admin: {}", e.getMessage());
        }
    }

    private boolean isExposed(HttpServletRequest req) {
        if (req != null) {
            if ("ihavelearnedmylesson".equals(req.getParameter("warn"))) {
                setExposed(false);
            } else if ("activate".equals(req.getParameter("warn"))) {
                setExposed(true);
            }
        }
        try {
            Configuration cfg = configurationAdmin.getConfiguration("org.openhab.dashboard");
            if (cfg != null && cfg.getProperties() != null && cfg.getProperties().get("exposed") != null) {
                if (cfg.getProperties().get("nowarning") == null) {
                    boolean value = cfg.getProperties().get("exposed").toString().equals(Boolean.TRUE.toString());
                    if (value) {
                        logger.error(
                                "WARNING - YOUR HOME IS EXPOSED! It is accessible from the Internet without authentication - please take immediate action!");
                    }
                    return value;
                }
            }
        } catch (IOException e) {
            logger.error("Error while accessing the configuration admin: {}", e.getMessage());
        }
        return false;
    }

    private void setExposed(boolean value) {
        try {
            Configuration cfg = configurationAdmin.getConfiguration("org.openhab.dashboard");
            Dictionary<String, Object> props = cfg.getProperties();
            if (props == null) {
                props = new Hashtable<>();
            }
            props.put("exposed", value);
            cfg.update(props);
        } catch (IOException e) {
            logger.error("Error while accessing the configuration admin: {}", e.getMessage());
        }
    }

    private boolean isSetup() {
        return getPackage() != null;
    }

    private String getPackage() {
        try {
            Configuration cfg = configurationAdmin.getConfiguration(OpenHAB.ADDONS_SERVICE_PID, null);
            if (cfg != null && cfg.getProperties() != null && cfg.getProperties().get(OpenHAB.CFG_PACKAGE) != null) {
                return cfg.getProperties().get(OpenHAB.CFG_PACKAGE).toString();
            }
        } catch (IOException e) {
            logger.error("Error while accessing the configuration admin: {}", e.getMessage());
        }
        return null;
    }

    private String replaceKeysWithLocaleFunction(String template) {
        return replaceKeysWithFunction(template, (key) -> localizeFunction.apply(key));
    }

    private String replaceKeysFromMap(String template, Map<String, String> map) {
        return replaceKeysWithFunction(template,
                (key) -> Matcher.quoteReplacement(map.getOrDefault(key, "${" + key + '}')));
    }

    private String replaceKeysWithFunction(String template, Function<String, String> getMessage) {
        Matcher m = MESSAGE_KEY_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            try {
                String key = m.group(1);
                m.appendReplacement(sb, getMessage.apply(key));
            } catch (Exception e) {
                logger.debug("Error occurred during template filling, cause ", e);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
