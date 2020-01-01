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
package org.openhab.core.config.discovery.mdns.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.openhab.core.config.discovery.DiscoveryService.CONFIG_PROPERTY_BACKGROUND_DISCOVERY;

import java.io.IOException;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Integration tests for the {@link MDNSDiscoveryService}.
 *
 * @author Henning Sudbrock - Initial contribution
 */
public class MDNSDiscoveryServiceOSGiTest extends JavaOSGiTest {

    private MDNSDiscoveryService mdnsDiscoveryService;

    @Before
    public void setup() {
        mdnsDiscoveryService = getService(DiscoveryService.class, MDNSDiscoveryService.class);
        assertThat(mdnsDiscoveryService, is(notNullValue()));
    }

    /**
     * Test that configuring the background discovery dynamically via config admin is effective.
     */
    @Test
    public void testDynamicConfigurationOfBackgroundDiscovery() throws IOException {
        waitForAssert(() -> assertThat(mdnsDiscoveryService.isBackgroundDiscoveryEnabled(), is(true)), 5000, 100);

        setBackgroundDiscoveryViaConfigAdmin(false);
        waitForAssert(() -> assertThat(mdnsDiscoveryService.isBackgroundDiscoveryEnabled(), is(false)), 5000, 100);

        setBackgroundDiscoveryViaConfigAdmin(true);
        waitForAssert(() -> assertThat(mdnsDiscoveryService.isBackgroundDiscoveryEnabled(), is(true)), 5000, 100);
    }

    private void setBackgroundDiscoveryViaConfigAdmin(boolean status) throws IOException {
        ConfigurationAdmin configAdmin = getService(ConfigurationAdmin.class);
        assertThat(configAdmin, is(notNullValue()));

        Configuration configuration = configAdmin.getConfiguration("discovery.mdns");
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(CONFIG_PROPERTY_BACKGROUND_DISCOVERY, Boolean.valueOf(status));

        configuration.update(properties);
    }

}
