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
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.test.java.JavaTest;

/**
 * Tests {@link ConfigDescriptionRegistry}.
 *
 * @author Simon Kaufmann - Initial contribution
 */
public class ConfigDescriptionRegistryTest extends JavaTest {

    private URI uriDummy;
    private URI uriDummy1;
    private URI uriAliases;
    private ConfigDescriptionRegistry configDescriptionRegistry;
    private ConfigDescription configDescription;
    private @Mock ConfigDescriptionProvider configDescriptionProviderMock;
    private ConfigDescription configDescription1;
    private @Mock ConfigDescriptionProvider configDescriptionProviderMock1;
    private ConfigDescription configDescription2;
    private @Mock ConfigDescriptionProvider configDescriptionProviderMock2;
    private ConfigDescription configDescriptionAliased;
    private @Mock ConfigDescriptionProvider configDescriptionProviderAliased;
    private @Mock ConfigDescriptionAliasProvider aliasProvider;
    private @Mock ConfigOptionProvider configOptionsProviderMockAliased;
    private @Mock ConfigOptionProvider configOptionsProviderMock;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        uriDummy = new URI("config:Dummy");
        uriDummy1 = new URI("config:Dummy1");
        uriAliases = new URI("config:Aliased");

        configDescriptionRegistry = new ConfigDescriptionRegistry();
        ConfigDescriptionParameter param1 = new ConfigDescriptionParameter("param1",
                ConfigDescriptionParameter.Type.INTEGER);
        List<ConfigDescriptionParameter> pList1 = new ArrayList<>();
        pList1.add(param1);

        configDescription = new ConfigDescription(uriDummy, pList1);
        when(configDescriptionProviderMock.getConfigDescriptions(any()))
                .thenReturn(Collections.singleton(configDescription));
        when(configDescriptionProviderMock.getConfigDescription(eq(uriDummy), any())).thenReturn(configDescription);

        configDescription1 = new ConfigDescription(uriDummy1);
        when(configDescriptionProviderMock1.getConfigDescriptions(any()))
                .thenReturn(Collections.singleton(configDescription1));
        when(configDescriptionProviderMock1.getConfigDescription(eq(uriDummy1), any())).thenReturn(configDescription1);

        configDescriptionAliased = new ConfigDescription(uriAliases, Collections
                .singletonList(new ConfigDescriptionParameter("instanceId", ConfigDescriptionParameter.Type.INTEGER)));
        when(configDescriptionProviderAliased.getConfigDescriptions(any()))
                .thenReturn(Collections.singleton(configDescriptionAliased));
        when(configDescriptionProviderAliased.getConfigDescription(eq(uriAliases), any()))
                .thenReturn(configDescriptionAliased);

        ConfigDescriptionParameter param2 = new ConfigDescriptionParameter("param2",
                ConfigDescriptionParameter.Type.INTEGER);
        List<ConfigDescriptionParameter> pList2 = new ArrayList<>();
        pList2.add(param2);
        configDescription2 = new ConfigDescription(uriDummy, pList2);
        when(configDescriptionProviderMock2.getConfigDescriptions(any()))
                .thenReturn(Collections.singleton(configDescription2));
        when(configDescriptionProviderMock2.getConfigDescription(eq(uriDummy), any())).thenReturn(configDescription2);

        when(aliasProvider.getAlias(eq(uriAliases))).thenReturn(uriDummy);

        when(configOptionsProviderMockAliased.getParameterOptions(eq(uriAliases), anyString(), any(), any()))
                .thenReturn(Collections.singletonList(new ParameterOption("Option", "Aliased")));
        when(configOptionsProviderMockAliased.getParameterOptions(eq(uriDummy), anyString(), any(), any()))
                .thenReturn(null);

        when(configOptionsProviderMock.getParameterOptions(eq(uriDummy), anyString(), any(), any()))
                .thenReturn(Collections.singletonList(new ParameterOption("Option", "Original")));
        when(configOptionsProviderMock.getParameterOptions(eq(uriAliases), anyString(), any(), any())).thenReturn(null);
    }

    @Test
    public void testGetConfigDescription() throws Exception {
        configDescriptionRegistry.addConfigDescriptionProvider(configDescriptionProviderMock);

        ConfigDescription configDescription = configDescriptionRegistry.getConfigDescription(uriDummy);
        assertThat(configDescription, is(notNullValue()));
        assertThat(configDescription.getUID(), is(equalTo(uriDummy)));
    }

    @Test
    public void testGetConfigDescriptions() throws Exception {
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(0));

        configDescriptionRegistry.addConfigDescriptionProvider(configDescriptionProviderMock);
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(1));

        List<ConfigDescription> configDescriptions = new ArrayList<>(configDescriptionRegistry.getConfigDescriptions());
        assertThat(configDescriptions.get(0).getUID(), is(equalTo(uriDummy)));
        assertThat(configDescriptions.get(0).toParametersMap().size(), is(1));
        assertThat(configDescriptions.get(0).toParametersMap().get("param1"), notNullValue());

        configDescriptionRegistry.addConfigDescriptionProvider(configDescriptionProviderMock1);
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(2));

        configDescriptionRegistry.removeConfigDescriptionProvider(configDescriptionProviderMock);
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(1));

        configDescriptionRegistry.removeConfigDescriptionProvider(configDescriptionProviderMock1);
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(0));
    }

    @Test
    public void testGetConfigDescriptionsOptions() throws Exception {
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(0));

        configDescriptionRegistry.addConfigDescriptionProvider(configDescriptionProviderMock);
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(1));

        configDescriptionRegistry.addConfigDescriptionProvider(configDescriptionProviderMock2);
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(1));

        List<ConfigDescription> configDescriptions = new ArrayList<>(configDescriptionRegistry.getConfigDescriptions());
        assertThat(configDescriptions.get(0).getUID(), is(equalTo(uriDummy)));

        assertThat(configDescriptions.get(0).getParameters().size(), is(2));
        assertThat(configDescriptions.get(0).getParameters().get(0).getName(), is(equalTo("param1")));
        assertThat(configDescriptions.get(0).getParameters().get(1).getName(), is(equalTo("param2")));

        configDescriptionRegistry.removeConfigDescriptionProvider(configDescriptionProviderMock);
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(1));

        configDescriptionRegistry.removeConfigDescriptionProvider(configDescriptionProviderMock2);
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(0));
    }

    @Test
    public void testGetConfigDescriptionsAliasedOptions() throws Exception {
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(0));

        configDescriptionRegistry.addConfigDescriptionProvider(configDescriptionProviderMock);
        configDescriptionRegistry.addConfigDescriptionAliasProvider(aliasProvider);
        configDescriptionRegistry.addConfigOptionProvider(configOptionsProviderMockAliased);

        ConfigDescription res = configDescriptionRegistry.getConfigDescription(uriAliases);
        assertThat(res, is(notNullValue()));
        assertThat(res.getParameters().get(0).getOptions().size(), is(1));
        assertThat(res.getParameters().get(0).getOptions().get(0).getLabel(), is("Aliased"));
        assertThat(res.getUID(), is(uriAliases));

        configDescriptionRegistry.removeConfigDescriptionProvider(configDescriptionProviderMock);
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(0));
    }

    @Test
    public void testGetConfigDescriptionsAliasedOptionsOriginalWins() throws Exception {
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(0));

        configDescriptionRegistry.addConfigDescriptionProvider(configDescriptionProviderMock);
        configDescriptionRegistry.addConfigDescriptionAliasProvider(aliasProvider);
        configDescriptionRegistry.addConfigOptionProvider(configOptionsProviderMock);
        configDescriptionRegistry.addConfigOptionProvider(configOptionsProviderMockAliased);

        ConfigDescription res = configDescriptionRegistry.getConfigDescription(uriAliases);
        assertThat(res, is(notNullValue()));
        assertThat(res.getParameters().get(0).getOptions().size(), is(1));
        assertThat(res.getParameters().get(0).getOptions().get(0).getLabel(), is("Aliased"));
        assertThat(res.getUID(), is(uriAliases));

        configDescriptionRegistry.removeConfigDescriptionProvider(configDescriptionProviderMock);
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(0));
    }

    @Test
    public void testGetConfigDescriptionsNonAliasOptions() throws Exception {
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(0));

        configDescriptionRegistry.addConfigDescriptionProvider(configDescriptionProviderMock);
        configDescriptionRegistry.addConfigDescriptionAliasProvider(aliasProvider);
        configDescriptionRegistry.addConfigOptionProvider(configOptionsProviderMock);

        ConfigDescription res = configDescriptionRegistry.getConfigDescription(uriAliases);
        assertThat(res, is(notNullValue()));
        assertThat(res.getParameters().get(0).getOptions().size(), is(1));
        assertThat(res.getParameters().get(0).getOptions().get(0).getLabel(), is("Original"));
        assertThat(res.getUID(), is(uriAliases));

        configDescriptionRegistry.removeConfigDescriptionProvider(configDescriptionProviderMock);
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(0));
    }

    @Test
    public void testGetConfigDescriptionsAliasedMixes() throws Exception {
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(0));

        configDescriptionRegistry.addConfigDescriptionProvider(configDescriptionProviderMock);
        configDescriptionRegistry.addConfigDescriptionAliasProvider(aliasProvider);

        ConfigDescription res1 = configDescriptionRegistry.getConfigDescription(uriAliases);
        assertThat(res1, is(notNullValue()));
        assertThat(res1.getParameters().size(), is(1));

        configDescriptionRegistry.addConfigDescriptionProvider(configDescriptionProviderAliased);

        ConfigDescription res2 = configDescriptionRegistry.getConfigDescription(uriAliases);
        assertThat(res2, is(notNullValue()));
        assertThat(res2.getParameters().size(), is(2));

        configDescriptionRegistry.removeConfigDescriptionProvider(configDescriptionProviderAliased);
        configDescriptionRegistry.removeConfigDescriptionProvider(configDescriptionProviderMock);
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(0));
    }
}
