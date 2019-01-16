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
import java.net.URL;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.io.IOUtils;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.eclipse.smarthome.core.net.HttpServiceUtil;
import org.eclipse.smarthome.core.net.NetworkAddressService;
import org.openhab.ui.dashboard.DashboardReady;
import org.openhab.ui.dashboard.DashboardTile;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
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
 * @author Laurent Garnier - internationalization
 * @author Hilbrand Bouwkamp - internationalization
 */
@Component(service = { DashboardService.class, DashboardReady.class }, immediate = true, name = "org.openhab.dashboard")
public class DashboardService implements DashboardReady {

    public static final String DASHBOARD_ALIAS = "/start";

    /** the name of the servlet to be used in the URL */
    public static final String SERVLET_NAME = "index";

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    protected HttpService httpService;
    protected ConfigurationAdmin configurationAdmin;
    protected NetworkAddressService networkAddressService;
    protected TranslationProvider i18nProvider;
    protected LocaleProvider localeProvider;

    protected Set<DashboardTile> tiles = new CopyOnWriteArraySet<>();

    private BundleContext bundleContext;

    private final static String LINK_NAME = "link-name";
    private final static String LINK_URL = "link-url";
    private final static String LINK_IMAGEURL = "link-imageurl";

    @Activate
    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        try {
            bundleContext = componentContext.getBundleContext();
            Hashtable<String, String> props = new Hashtable<>();
            httpService.registerServlet(DASHBOARD_ALIAS + "/" + SERVLET_NAME, createServlet(), props,
                    httpService.createDefaultHttpContext());
            httpService.registerResources(DASHBOARD_ALIAS, "web", null);

            if (HttpServiceUtil.getHttpServicePort(bundleContext) > 0) {
                logger.info("Started Dashboard at http://{}:{}", networkAddressService.getPrimaryIpv4HostAddress(),
                        HttpServiceUtil.getHttpServicePort(bundleContext));
            }
            if (HttpServiceUtil.getHttpServicePortSecure(bundleContext) > 0) {
                logger.info("Started Dashboard at https://{}:{}", networkAddressService.getPrimaryIpv4HostAddress(),
                        HttpServiceUtil.getHttpServicePortSecure(bundleContext));
            }
        } catch (NamespaceException | ServletException e) {
            logger.error("Error during Dashboard startup: {}", e.getMessage());
        }

        addTilesForExternalServices(properties);
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        httpService.unregister(DASHBOARD_ALIAS + "/" + SERVLET_NAME);
        httpService.unregister(DASHBOARD_ALIAS);
        logger.info("Stopped Dashboard");
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

    @Reference
    protected void setLocaleProvider(final LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    protected void unsetLocaleProvider(final LocaleProvider localeProvider) {
        this.localeProvider = null;
    }

    @Reference
    public void setTranslationProvider(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    public void unsetTranslationProvider(TranslationProvider i18nProvider) {
        this.i18nProvider = null;
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
                throw new ComponentException(e);
            }
        } else {
            throw new ComponentException("Cannot find index.html - failed to initialize Dashboard servlet");
        }

        URL entry = bundleContext.getBundle().getEntry("templates/entry.html");
        if (entry != null) {
            try {
                entryTemplate = IOUtils.toString(entry.openStream());
            } catch (IOException e) {
                throw new ComponentException(e);
            }
        } else {
            throw new ComponentException("Cannot find entry.html - failed to initialize Dashboard servlet");
        }

        URL warn = bundleContext.getBundle().getEntry("templates/warn.html");
        if (warn != null) {
            try {
                warnTemplate = IOUtils.toString(warn.openStream());
            } catch (IOException e) {
                throw new ComponentException(e);
            }
        } else {
            throw new RuntimeException("Cannot find warn.html - failed to initialize Dashboard servlet");
        }

        URL setup = bundleContext.getBundle().getEntry("templates/setup.html");
        if (setup != null) {
            try {
                setupTemplate = IOUtils.toString(setup.openStream());
            } catch (IOException e) {
                throw new ComponentException(e);
            }
        } else {
            throw new ComponentException("Cannot find setup.html - failed to initialize Dashboard servlet");
        }

        return new DashboardServlet(configurationAdmin, indexTemplate, entryTemplate, warnTemplate, setupTemplate,
                tiles, this::getLocalizedText);
    }

    private void addTilesForExternalServices(Map<String, Object> properties) {
        for (String key : properties.keySet()) {
            if (key.endsWith(LINK_NAME)) {
                if (key.length() > LINK_NAME.length()) {
                    // get prefix from link name
                    String linkname = key.substring(0, key.length() - LINK_NAME.length());

                    String name = (String) properties.get(linkname + LINK_NAME);
                    String url = (String) properties.get(linkname + LINK_URL);
                    String imageUrl = (String) properties.get(linkname + LINK_IMAGEURL);

                    DashboardTile newTile = new ExternalServiceTile.DashboardTileBuilder().withName(name).withUrl(url)
                            .withImageUrl(imageUrl).build();

                    if (name != null && url != null && !name.isEmpty() && !url.isEmpty()) {
                        addDashboardTile(newTile);
                        logger.debug("Tile added to Dashboard: {}", newTile);
                    } else {
                        logger.warn("Ignore invalid tile '{}': {}", linkname, newTile);
                    }
                }
            }
        }
    }

    /**
     * Returns the localized text for the given key. When the key is 'locale' it returns the locale. If no locale or
     * fall-back would be present it returns the key.
     *
     * @param key key to get locale from
     * @return localized text for the key
     */
    private String getLocalizedText(String key) {
        Locale useLocale = localeProvider.getLocale() == null ? Locale.ENGLISH : localeProvider.getLocale();

        if ("locale".equals(key)) {
            // The return value for "locale" key is an ISO 639-1 language code
            // In case there is no translation for the used locale provided with the dashboard, "en" is returned
            return bundleContext.getBundle()
                    .getEntry("ESH-INF/i18n/dashboard_" + useLocale.getLanguage() + ".properties") != null
                            ? useLocale.getLanguage()
                            : "en";
        } else {
            return i18nProvider.getText(bundleContext.getBundle(), key, key, useLocale);
        }
    }
}
