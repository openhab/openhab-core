/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import static org.eclipse.jdt.annotation.Checks.requireNonNull;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.test.java.JavaTest;

/**
 * Tests {@link ConfigDescriptionRegistry}.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ConfigDescriptionRegistryTest extends JavaTest {

    private @NonNullByDefault({}) URI uriDummy;
    private @NonNullByDefault({}) URI uriDummy1;
    private @NonNullByDefault({}) URI uriAliases;
    private @NonNullByDefault({}) ConfigDescriptionRegistry configDescriptionRegistry;
    private @NonNullByDefault({}) ConfigDescription configDescription;
    private @NonNullByDefault({}) ConfigDescription configDescription1;
    private @NonNullByDefault({}) ConfigDescription configDescription2;
    private @NonNullByDefault({}) ConfigDescription configDescriptionAliased;

    private @Mock @NonNullByDefault({}) ConfigDescriptionProvider configDescriptionProviderMock;
    private @Mock @NonNullByDefault({}) ConfigDescriptionProvider configDescriptionProviderMock1;
    private @Mock @NonNullByDefault({}) ConfigDescriptionProvider configDescriptionProviderMock2;
    private @Mock @NonNullByDefault({}) ConfigDescriptionProvider configDescriptionProviderAliased;
    private @Mock @NonNullByDefault({}) ConfigDescriptionAliasProvider aliasProvider;
    private @Mock @NonNullByDefault({}) ConfigOptionProvider configOptionsProviderMockAliased;
    private @Mock @NonNullByDefault({}) ConfigOptionProvider configOptionsProviderMock;

    @BeforeEach
    public void setUp() throws Exception {
        uriDummy = new URI("config:Dummy");
        uriDummy1 = new URI("config:Dummy1");
        uriAliases = new URI("config:Aliased");

        configDescriptionRegistry = new ConfigDescriptionRegistry();
        ConfigDescriptionParameter param1 = ConfigDescriptionParameterBuilder
                .create("param1", ConfigDescriptionParameter.Type.INTEGER).build();

        configDescription = ConfigDescriptionBuilder.create(uriDummy).withParameter(param1).build();
        when(configDescriptionProviderMock.getConfigDescriptions(any())).thenReturn(Set.of(configDescription));
        when(configDescriptionProviderMock.getConfigDescription(eq(uriDummy), any())).thenReturn(configDescription);

        configDescription1 = ConfigDescriptionBuilder.create(uriDummy1).build();
        when(configDescriptionProviderMock1.getConfigDescriptions(any())).thenReturn(Set.of(configDescription1));
        when(configDescriptionProviderMock1.getConfigDescription(eq(uriDummy1), any())).thenReturn(configDescription1);

        configDescriptionAliased = ConfigDescriptionBuilder.create(uriAliases).withParameter(
                ConfigDescriptionParameterBuilder.create("instanceId", ConfigDescriptionParameter.Type.INTEGER).build())
                .build();
        when(configDescriptionProviderAliased.getConfigDescriptions(any()))
                .thenReturn(Set.of(configDescriptionAliased));
        when(configDescriptionProviderAliased.getConfigDescription(eq(uriAliases), any()))
                .thenReturn(configDescriptionAliased);

        ConfigDescriptionParameter param2 = ConfigDescriptionParameterBuilder
                .create("param2", ConfigDescriptionParameter.Type.INTEGER).build();
        configDescription2 = ConfigDescriptionBuilder.create(uriDummy).withParameter(param2).build();
        when(configDescriptionProviderMock2.getConfigDescriptions(any())).thenReturn(Set.of(configDescription2));
        when(configDescriptionProviderMock2.getConfigDescription(eq(uriDummy), any())).thenReturn(configDescription2);

        when(aliasProvider.getAlias(eq(uriAliases))).thenReturn(uriDummy);

        when(configOptionsProviderMockAliased.getParameterOptions(eq(uriAliases), anyString(), any(), any()))
                .thenReturn(List.of(new ParameterOption("Option", "Aliased")));
        when(configOptionsProviderMockAliased.getParameterOptions(eq(uriDummy), anyString(), any(), any()))
                .thenReturn(null);

        when(configOptionsProviderMock.getParameterOptions(eq(uriDummy), anyString(), any(), any()))
                .thenReturn(List.of(new ParameterOption("Option", "Original")));
        when(configOptionsProviderMock.getParameterOptions(eq(uriAliases), anyString(), any(), any())).thenReturn(null);
    }

    @Test
    public void testGetConfigDescription() throws Exception {
        configDescriptionRegistry.addConfigDescriptionProvider(configDescriptionProviderMock);

        ConfigDescription configDescription = requireNonNull(configDescriptionRegistry.getConfigDescription(uriDummy));
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

        ConfigDescription res = requireNonNull(configDescriptionRegistry.getConfigDescription(uriAliases));
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

        ConfigDescription res = requireNonNull(configDescriptionRegistry.getConfigDescription(uriAliases));
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

        ConfigDescription res = requireNonNull(configDescriptionRegistry.getConfigDescription(uriAliases));
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

        ConfigDescription res1 = requireNonNull(configDescriptionRegistry.getConfigDescription(uriAliases));
        assertThat(res1.getParameters().size(), is(1));

        configDescriptionRegistry.addConfigDescriptionProvider(configDescriptionProviderAliased);

        ConfigDescription res2 = requireNonNull(configDescriptionRegistry.getConfigDescription(uriAliases));
        assertThat(res2.getParameters().size(), is(2));

        configDescriptionRegistry.removeConfigDescriptionProvider(configDescriptionProviderAliased);
        configDescriptionRegistry.removeConfigDescriptionProvider(configDescriptionProviderMock);
        assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(0));
    }
}
