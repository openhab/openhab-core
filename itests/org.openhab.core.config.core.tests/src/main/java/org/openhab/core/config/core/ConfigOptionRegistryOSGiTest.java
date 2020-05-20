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
package org.openhab.core.config.core;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * Test the ConfigOptionRegistry
 *
 * @author Chris Jackson - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class ConfigOptionRegistryOSGiTest extends JavaOSGiTest {

    private ConfigDescriptionRegistry configDescriptionRegistry;
    private ConfigDescriptionProvider configDescriptionProviderMock;
    private ConfigOptionProvider configOptionsProviderMock;
    private URI dummyURI;

    @Before
    public void setUp() throws URISyntaxException {
        // Register config registry
        configDescriptionRegistry = getService(ConfigDescriptionRegistry.class);
        dummyURI = new URI("config:Dummy");
        ConfigDescriptionParameter param1 = ConfigDescriptionParameterBuilder
                .create("Param1", ConfigDescriptionParameter.Type.INTEGER).build();
        ConfigDescription configDescription = ConfigDescriptionBuilder.create(dummyURI).withParameter(param1).build();

        // Create config option list
        List<ParameterOption> oList1 = new ArrayList<>();
        ParameterOption parameterOption = new ParameterOption("Option1", "Option1");
        oList1.add(parameterOption);
        parameterOption = new ParameterOption("Option2", "Option2");
        oList1.add(parameterOption);

        configOptionsProviderMock = mock(ConfigOptionProvider.class);
        when(configOptionsProviderMock.getParameterOptions(any(), any(), any(), any())).thenReturn(oList1);

        configDescriptionProviderMock = mock(ConfigDescriptionProvider.class);
        when(configDescriptionProviderMock.getConfigDescriptions(any()))
                .thenReturn(Collections.singleton(configDescription));
        when(configDescriptionProviderMock.getConfigDescription(any(), any())).thenReturn(configDescription);
    }

    @Test
    public void assertConfigDescriptionRegistryMergesOptions() {
        assertThat("Registery is empty to start", configDescriptionRegistry.getConfigDescriptions(), hasSize(0));

        configDescriptionRegistry.addConfigDescriptionProvider(configDescriptionProviderMock);
        assertThat("Config description added ok", configDescriptionRegistry.getConfigDescriptions(), hasSize(1));

        configDescriptionRegistry.addConfigOptionProvider(configOptionsProviderMock);

        ConfigDescription configDescriptions = configDescriptionRegistry.getConfigDescription(dummyURI);
        assertThat(configDescriptions, is(not(nullValue())));
        assertThat("Config is found", configDescriptions.getUID(), is(dummyURI));
        assertThat("Config contains parameter", configDescriptions.getParameters(), hasSize(1));
        assertThat("Config parameter found", configDescriptions.getParameters().get(0).getName(),
                is(equalTo("Param1")));
        assertThat("Config parameter contains options", configDescriptions.getParameters().get(0).getOptions(),
                hasSize(2));

        configDescriptionRegistry.removeConfigOptionProvider(configOptionsProviderMock);

        configDescriptionRegistry.removeConfigDescriptionProvider(configDescriptionProviderMock);
        assertThat("Description registery is empty to finish", configDescriptionRegistry.getConfigDescriptions(),
                hasSize(0));
    }
}
