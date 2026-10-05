/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.ui.internal.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.StringType;
import org.openhab.core.sitemap.Image;
import org.openhab.core.sitemap.Sitemap;
import org.openhab.core.sitemap.registry.SitemapRegistry;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.ui.items.ItemUIRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

import com.sun.net.httpserver.HttpServer;

/**
 * Verifies that the {@link ProxyServletService} component receives the "org.openhab.sitemap" configuration,
 * which it shares with the item UI registry, and that the "allowedHosts" setting controls whether the URL held by an
 * item is proxied.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class ProxyServletServiceOSGiTest extends JavaOSGiTest {

    private static final String SITEMAP_PID = "org.openhab.sitemap";
    private static final String ALLOWED_HOSTS = "allowedHosts";

    private static final String SITEMAP_NAME = "demo";
    private static final String WIDGET_ID = "00";
    private static final String ITEM_NAME = "Camera";

    private @NonNullByDefault({}) HttpServer targetServer;
    private @NonNullByDefault({}) String targetHostAndPort;

    @BeforeEach
    public void setUp() throws Exception {
        // the component must not find a stale configuration from a previous run
        deleteConfiguration(SITEMAP_PID);

        targetServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        targetServer.createContext("/", exchange -> {
            byte[] body = ("target:" + exchange.getRequestURI().getPath()).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        targetServer.start();
        targetHostAndPort = "127.0.0.1:" + targetServer.getAddress().getPort();

        Sitemap sitemap = mock(Sitemap.class);
        Image image = mock(Image.class);
        when(image.getUrl()).thenReturn("http://" + targetHostAndPort + "/from-sitemap");
        when(image.getItem()).thenReturn(ITEM_NAME);

        ItemUIRegistry itemUIRegistry = mock(ItemUIRegistry.class);
        when(itemUIRegistry.getWidget(sitemap, WIDGET_ID)).thenReturn(image);
        when(itemUIRegistry.getItemState(ITEM_NAME))
                .thenReturn(new StringType("http://" + targetHostAndPort + "/from-item"));
        SitemapRegistry sitemapRegistry = mock(SitemapRegistry.class);
        when(sitemapRegistry.get(SITEMAP_NAME)).thenReturn(sitemap);

        // the real registries are available as well; the mocks must win when the component is (re-)activated
        Dictionary<String, Object> ranking = new Hashtable<>();
        ranking.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
        registerService(itemUIRegistry, ranking);
        registerService(sitemapRegistry, ranking);

        // a configuration change re-creates the component, which then binds to the mocks
        updateConfiguration(SITEMAP_PID, "maxProxyThreads", "4");
    }

    @AfterEach
    public void tearDown() throws Exception {
        deleteConfiguration(SITEMAP_PID);
        targetServer.stop(0);
    }

    @Test
    public void allowedHostsFromSitemapConfigurationReachProxyServlet() throws Exception {
        waitForAssert(() -> assertNotNull(findActiveProxyConfiguration()));

        updateConfiguration(SITEMAP_PID, ALLOWED_HOSTS, "camera.local, 192.168.1.20:8080");

        waitForAssert(() -> {
            ComponentConfigurationDTO config = findActiveProxyConfiguration();
            assertNotNull(config);
            assertEquals("camera.local, 192.168.1.20:8080", config.properties.get(ALLOWED_HOSTS));
        }, 10000, 100);
    }

    @Test
    public void itemStateUrlIsNotProxiedByDefault() throws Exception {
        waitForAssert(() -> assertEquals("target:/from-sitemap", fetchViaProxy()), 10000, 100);
    }

    @Test
    public void itemStateUrlIsNotProxiedForHostNotInAllowlist() throws Exception {
        updateConfiguration(SITEMAP_PID, ALLOWED_HOSTS, "camera.local");

        waitForAssert(() -> {
            ComponentConfigurationDTO config = findActiveProxyConfiguration();
            assertNotNull(config);
            assertEquals("camera.local", config.properties.get(ALLOWED_HOSTS));
        }, 10000, 100);
        assertEquals("target:/from-sitemap", fetchViaProxy());
    }

    @Test
    public void itemStateUrlIsProxiedForAllowedHost() throws Exception {
        updateConfiguration(SITEMAP_PID, ALLOWED_HOSTS, "camera.local, " + targetHostAndPort);

        waitForAssert(() -> assertEquals("target:/from-item", fetchViaProxy()), 10000, 100);
    }

    private String fetchViaProxy() {
        String port = bundleContext.getProperty("org.osgi.service.http.port");
        assertNotNull(port);
        URI uri = URI.create("http://127.0.0.1:" + port + "/" + ProxyServletService.PROXY_ALIAS + "?sitemap="
                + SITEMAP_NAME + "&widgetId=" + WIDGET_ID);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int status = connection.getResponseCode();
            if (status != 200) {
                return "status:" + status;
            }
            return new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "error:" + e.getMessage();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private @Nullable ComponentConfigurationDTO findActiveProxyConfiguration() {
        ServiceComponentRuntime scr = getService(ServiceComponentRuntime.class);
        assertNotNull(scr);
        for (Bundle bundle : bundleContext.getBundles()) {
            if (!"org.openhab.core.ui".equals(bundle.getSymbolicName())) {
                continue;
            }
            for (ComponentDescriptionDTO description : scr.getComponentDescriptionDTOs(bundle)) {
                if (ProxyServletService.class.getName().equals(description.implementationClass)) {
                    for (ComponentConfigurationDTO config : scr.getComponentConfigurationDTOs(description)) {
                        if (config.state == ComponentConfigurationDTO.ACTIVE) {
                            return config;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void updateConfiguration(String pid, String key, String value) throws Exception {
        ConfigurationAdmin configAdmin = getService(ConfigurationAdmin.class);
        assertNotNull(configAdmin);
        Configuration configuration = configAdmin.getConfiguration(pid, null);
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(key, value);
        configuration.update(props);
    }

    private void deleteConfiguration(String pid) throws Exception {
        ConfigurationAdmin configAdmin = getService(ConfigurationAdmin.class);
        if (configAdmin != null) {
            configAdmin.getConfiguration(pid, null).delete();
        }
    }
}
