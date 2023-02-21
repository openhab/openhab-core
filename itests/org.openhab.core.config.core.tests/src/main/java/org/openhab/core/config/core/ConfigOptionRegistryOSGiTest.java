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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
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
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * Test the ConfigOptionRegistry
 *
 * @author Chris Jackson - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class ConfigOptionRegistryOSGiTest extends JavaOSGiTest {

    private static final URI DUMMY_URI = URI.create("config:Dummy");

    private @NonNullByDefault({}) ConfigDescriptionRegistry configDescriptionRegistry;

    private @Mock @NonNullByDefault({}) ConfigDescriptionProvider configDescriptionProviderMock;
    private @Mock @NonNullByDefault({}) ConfigOptionProvider configOptionsProviderMock;

    @BeforeEach
    public void setUp() {
        // Register config registry
        configDescriptionRegistry = getService(ConfigDescriptionRegistry.class);
        ConfigDescriptionParameter param1 = ConfigDescriptionParameterBuilder
                .create("Param1", ConfigDescriptionParameter.Type.INTEGER).build();
        ConfigDescription configDescription = ConfigDescriptionBuilder.create(DUMMY_URI).withParameter(param1).build();

        // Create config option list
        List<ParameterOption> oList1 = new ArrayList<>();
        ParameterOption parameterOption = new ParameterOption("Option1", "Option1");
        oList1.add(parameterOption);
        parameterOption = new ParameterOption("Option2", "Option2");
        oList1.add(parameterOption);

        when(configOptionsProviderMock.getParameterOptions(any(), any(), any(), any())).thenReturn(oList1);

        when(configDescriptionProviderMock.getConfigDescriptions(any())).thenReturn(Set.of(configDescription));
        when(configDescriptionProviderMock.getConfigDescription(any(), any())).thenReturn(configDescription);
    }

    @Test
    public void assertConfigDescriptionRegistryMergesOptions() {
        int preAddSize = configDescriptionRegistry.getConfigDescriptions().size();

        configDescriptionRegistry.addConfigDescriptionProvider(configDescriptionProviderMock);
        assertThat("Config description added ok", configDescriptionRegistry.getConfigDescriptions(),
                hasSize(preAddSize + 1));

        configDescriptionRegistry.addConfigOptionProvider(configOptionsProviderMock);

        ConfigDescription configDescriptions = configDescriptionRegistry.getConfigDescription(DUMMY_URI);
        assertThat(configDescriptions, is(not(nullValue())));
        assertThat("Config is found", configDescriptions.getUID(), is(DUMMY_URI));
        assertThat("Config contains parameter", configDescriptions.getParameters(), hasSize(1));
        assertThat("Config parameter found", configDescriptions.getParameters().get(0).getName(),
                is(equalTo("Param1")));
        assertThat("Config parameter contains options", configDescriptions.getParameters().get(0).getOptions(),
                hasSize(2));

        configDescriptionRegistry.removeConfigOptionProvider(configOptionsProviderMock);

        configDescriptionRegistry.removeConfigDescriptionProvider(configDescriptionProviderMock);
        assertThat("Description registery is empty to finish", configDescriptionRegistry.getConfigDescriptions(),
                hasSize(preAddSize));
    }
}
