/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.config.discovery.mdns.internal;

import static org.eclipse.smarthome.config.discovery.DiscoveryService.CONFIG_PROPERTY_BACKGROUND_DISCOVERY;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Hashtable;

import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Integration tests for the {@link MDNSDiscoveryService}.
 *
 * @author Henning Sudbrock - initial contribution
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
        setBackgroundDiscoveryViaConfigAdmin(true);
        waitForAssert(() -> assertThat(mdnsDiscoveryService.isBackgroundDiscoveryEnabled(), is(true)), 2000, 100);

        setBackgroundDiscoveryViaConfigAdmin(false);
        waitForAssert(() -> assertThat(mdnsDiscoveryService.isBackgroundDiscoveryEnabled(), is(false)), 2000, 100);
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
