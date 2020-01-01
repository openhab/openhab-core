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

import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.ArrayList;
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
    private ConfigDescription configDescription;
    private ConfigDescriptionProvider configDescriptionProviderMock;
    private ConfigOptionProvider configOptionsProviderMock;
    private ParameterOption parameterOption;

    @Before
    public void setUp() throws Exception {
        // Register config registry
        configDescriptionRegistry = getService(ConfigDescriptionRegistry.class);
        ConfigDescriptionParameter parm1 = new ConfigDescriptionParameter("Parm1",
                ConfigDescriptionParameter.Type.INTEGER);
        List<ConfigDescriptionParameter> pList1 = new ArrayList<>();
        pList1.add(parm1);
        configDescription = new ConfigDescription(new URI("config:Dummy"), pList1);

        // Create config option list
        List<ParameterOption> oList1 = new ArrayList<>();
        parameterOption = new ParameterOption("Option1", "Option1");
        oList1.add(parameterOption);
        parameterOption = new ParameterOption("Option2", "Option2");
        oList1.add(parameterOption);

        configOptionsProviderMock = mock(ConfigOptionProvider.class);
        when(configOptionsProviderMock.getParameterOptions(any(), any(), any(), any())).thenReturn(oList1);

        configDescriptionProviderMock = mock(ConfigDescriptionProvider.class);
        when(configDescriptionProviderMock.getConfigDescriptions(any())).thenReturn(singleton(configDescription));
        when(configDescriptionProviderMock.getConfigDescription(any(), any())).thenReturn(configDescription);
    }

    @Test
    public void assertConfigDescriptionRegistryMergesOptions() throws Exception {
        assertThat("Registery is empty to start", configDescriptionRegistry.getConfigDescriptions().size(), is(0));

        configDescriptionRegistry.addConfigDescriptionProvider(configDescriptionProviderMock);
        assertThat("Config description added ok", configDescriptionRegistry.getConfigDescriptions().size(), is(1));

        configDescriptionRegistry.addConfigOptionProvider(configOptionsProviderMock);

        ConfigDescription configDescriptions = configDescriptionRegistry.getConfigDescription(new URI("config:Dummy"));
        assertThat("Config is found", configDescriptions.getUID(), is(equalTo(new URI("config:Dummy"))));

        assertThat("Config contains parameter", configDescriptions.getParameters().size(), is(1));
        assertThat("Config parameter found", configDescriptions.getParameters().get(0).getName(), is(equalTo("Parm1")));
        assertThat("Config parameter contains options", configDescriptions.getParameters().get(0).getOptions().size(),
                is(2));

        configDescriptionRegistry.removeConfigOptionProvider(configOptionsProviderMock);

        configDescriptionRegistry.removeConfigDescriptionProvider(configDescriptionProviderMock);
        assertThat("Description registery is empty to finish", configDescriptionRegistry.getConfigDescriptions().size(),
                is(0));
    }
}
