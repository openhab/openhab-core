/**
 * Copyright (c) 2015-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.ui.dashboard.internal;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

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
 *
 */
public class DashboardServlet extends HttpServlet {

    private static final long serialVersionUID = -5154582000538034381L;

    private final Logger logger = LoggerFactory.getLogger(DashboardServlet.class);

    private ConfigurationAdmin configurationAdmin;

    private String indexTemplate;

    private String entryTemplate;

    private String warnTemplate;

    private String setupTemplate;

    private Set<DashboardTile> tiles;

    public DashboardServlet(ConfigurationAdmin configurationAdmin, String indexTemplate, String entryTemplate,
            String warnTemplate, String setupTemplate, Set<DashboardTile> tiles) {
        this.configurationAdmin = configurationAdmin;
        this.indexTemplate = indexTemplate;
        this.entryTemplate = entryTemplate;
        this.warnTemplate = warnTemplate;
        this.setupTemplate = setupTemplate;
        this.tiles = tiles;
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

    private void serveDashboard(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String index = indexTemplate.replace("<!--version-->", OpenHAB.getVersion() + " " + OpenHAB.buildString());
        StringBuilder entries = new StringBuilder();
        for (DashboardTile tile : tiles) {
            String entry = entryTemplate.replace("<!--name-->", tile.getName());
            String overlay = tile.getOverlay() == null ? "none" : tile.getOverlay();

            entry = entry.replace("<!--url-->", tile.getUrl());
            entry = entry.replace("<!--overlay-->", overlay);
            entry = entry.replace("<!--icon-->", tile.getImageUrl());
            entries.append(entry);
        }
        resp.setContentType("text/html;charset=UTF-8");
        if (tiles.size() == 0) {
            if ("minimal".equals(getPackage())) {
                entries.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
                entries.append("No user interfaces installed.");
            } else {
                entries.append(
                        "&nbsp;&nbsp;&nbsp;&nbsp;<div class=\"spinner spinner--steps\"><img src=\"img/spinner.svg\"></div>&nbsp;&nbsp;");
                entries.append("Please stand by while UIs are being installed. This can take several minutes.");
            }
        }
        String warn = isExposed(req) ? warnTemplate : "";
        resp.getWriter().append(index.replace("<!--entries-->", entries.toString()).replace("<!--warn-->", warn));
        resp.getWriter().close();
    }

    private void serveSetup(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getParameter("type") != null) {
            setPackage(req.getParameter("type"));
            resp.sendRedirect(req.getRequestURI());
        } else {
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().append(
                    setupTemplate.replace("<!--version-->", OpenHAB.getVersion() + " " + OpenHAB.buildString()));
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
}
