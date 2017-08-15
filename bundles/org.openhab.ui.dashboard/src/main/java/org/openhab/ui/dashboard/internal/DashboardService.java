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
import java.net.URL;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.io.IOUtils;
import org.eclipse.smarthome.core.net.HttpServiceUtil;
import org.eclipse.smarthome.core.net.NetworkAddressService;
import org.openhab.ui.dashboard.DashboardTile;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This component registers the dashboard resources.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(service = DashboardService.class, immediate = true)
public class DashboardService {

    public static final String DASHBOARD_ALIAS = "/start";

    /** the name of the servlet to be used in the URL */
    public static final String SERVLET_NAME = "index";

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    protected HttpService httpService;
    protected ConfigurationAdmin configurationAdmin;
    protected NetworkAddressService networkAddressService;

    protected Set<DashboardTile> tiles = new CopyOnWriteArraySet<>();

    private BundleContext bundleContext;

    @Activate
    protected void activate(ComponentContext componentContext) {
        try {
            bundleContext = componentContext.getBundleContext();
            Hashtable<String, String> props = new Hashtable<>();
            httpService.registerServlet(DASHBOARD_ALIAS + "/" + SERVLET_NAME, createServlet(), props,
                    httpService.createDefaultHttpContext());
            httpService.registerResources(DASHBOARD_ALIAS, "web", null);

            if (HttpServiceUtil.getHttpServicePort(bundleContext) > 0) {
                logger.info("Started dashboard at http://{}:{}", networkAddressService.getPrimaryIpv4HostAddress(),
                        HttpServiceUtil.getHttpServicePort(bundleContext));
            }
            if (HttpServiceUtil.getHttpServicePortSecure(bundleContext) > 0) {
                logger.info("Started dashboard at https://{}:{}", networkAddressService.getPrimaryIpv4HostAddress(),
                        HttpServiceUtil.getHttpServicePortSecure(bundleContext));
            }
        } catch (NamespaceException | ServletException e) {
            logger.error("Error during dashboard startup: {}", e.getMessage());
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        httpService.unregister(DASHBOARD_ALIAS);
        logger.info("Stopped dashboard");
    }

    @Reference
    protected void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = null;
    }

    @Reference
    protected void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    protected void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }

    @Reference
    protected void setNetworkAddressService(NetworkAddressService networkAddressService) {
        this.networkAddressService = networkAddressService;
    }

    protected void unsetNetworkAddressService(NetworkAddressService networkAddressService) {
        this.networkAddressService = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addDashboardTile(DashboardTile tile) {
        tiles.add(tile);
    }

    protected void removeDashboardTile(DashboardTile tile) {
        tiles.remove(tile);
    }

    protected HttpServlet createServlet() {
        String indexTemplate;
        String entryTemplate;
        String warnTemplate;
        String setupTemplate;

        URL index = bundleContext.getBundle().getEntry("templates/index.html");
        if (index != null) {
            try {
                indexTemplate = IOUtils.toString(index.openStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Cannot find index.html - failed to initialize dashboard servlet");
        }

        URL entry = bundleContext.getBundle().getEntry("templates/entry.html");
        if (entry != null) {
            try {
                entryTemplate = IOUtils.toString(entry.openStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Cannot find entry.html - failed to initialize dashboard servlet");
        }

        URL warn = bundleContext.getBundle().getEntry("templates/warn.html");
        if (warn != null) {
            try {
                warnTemplate = IOUtils.toString(warn.openStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Cannot find warn.html - failed to initialize dashboard servlet");
        }

        URL setup = bundleContext.getBundle().getEntry("templates/setup.html");
        if (setup != null) {
            try {
                setupTemplate = IOUtils.toString(setup.openStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Cannot find setup.html - failed to initialize dashboard servlet");
        }

        return new DashboardServlet(configurationAdmin, indexTemplate, entryTemplate, warnTemplate, setupTemplate,
                tiles);
    }
}
