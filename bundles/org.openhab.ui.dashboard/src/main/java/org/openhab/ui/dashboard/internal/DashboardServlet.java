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

    private String setupTemplate;

    private Set<DashboardTile> tiles;

    public DashboardServlet(ConfigurationAdmin configurationAdmin, String indexTemplate, String entryTemplate,
            String setupTemplate, Set<DashboardTile> tiles) {
        this.configurationAdmin = configurationAdmin;
        this.indexTemplate = indexTemplate;
        this.entryTemplate = entryTemplate;
        this.setupTemplate = setupTemplate;
        this.tiles = tiles;
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
        StringBuilder entries = new StringBuilder();
        for (DashboardTile tile : tiles) {
            String entry = entryTemplate.replace("<!--name-->", tile.getName());
            entry = entry.replace("<!--url-->", tile.getUrl());
            entry = entry.replace("<!--overlay-->", tile.getOverlay());
            entry = entry.replace("<!--icon-->", tile.getImageUrl());
            entries.append(entry);
        }
        resp.setContentType("text/html;charset=UTF-8");
        if (tiles.size() == 0) {
            entries.append(
                    "&nbsp;&nbsp;&nbsp;&nbsp;<div class=\"spinner spinner--steps\"><img src=\"img/spinner.svg\"></div>&nbsp;&nbsp;");
            entries.append("Please stand by while UIs are being installed. This can take several minutes.");
        }
        resp.getWriter().append(indexTemplate.replace("<!--entries-->", entries.toString()));
        resp.getWriter().close();
    }

    private void serveSetup(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getParameter("type") != null) {
            setPackage(req.getParameter("type"));
            resp.sendRedirect(req.getRequestURI());
        } else {
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().append(setupTemplate);
            resp.getWriter().close();
        }
    }

    private void setPackage(String parameter) {
        try {
            Configuration cfg = configurationAdmin.getConfiguration(OpenHAB.ADDONS_SERVICE_PID);
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

    private boolean isSetup() {
        try {
            Configuration cfg = configurationAdmin.getConfiguration(OpenHAB.ADDONS_SERVICE_PID);
            if (cfg != null && cfg.getProperties() != null && cfg.getProperties().get(OpenHAB.CFG_PACKAGE) != null) {
                return true;
            }
        } catch (IOException e) {
            logger.error("Error while accessing the configuration admin: {}", e.getMessage());
        }
        return false;
    }
}
