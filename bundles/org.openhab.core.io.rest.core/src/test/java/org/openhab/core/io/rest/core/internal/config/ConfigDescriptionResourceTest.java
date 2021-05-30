/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.io.rest.core.internal.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.io.rest.LocaleService;

/**
 * @author Christoph Weitkamp - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ConfigDescriptionResourceTest {

    private static final String PARAM_NAME = "name";
    private static final String PARAMETER_NAME_DEFAULT_VALUE = "test";
    private static final String PARAM_COUNTRY = "country";

    private static final String CONFIG_DESCRIPTION_SYSTEM_I18N_URI = "system:i18n";

    private ConfigDescriptionResource resource;

    private @Mock ConfigDescriptionRegistry mockedConfigDescriptionRegistry;
    private @Mock LocaleService mockedLocaleService;

    @BeforeEach
    public void beforeEach() throws URISyntaxException {
        final URI systemI18nURI = new URI(CONFIG_DESCRIPTION_SYSTEM_I18N_URI);
        final ConfigDescription systemI18n = ConfigDescriptionBuilder.create(systemI18nURI)
                .withParameter(ConfigDescriptionParameterBuilder.create(PARAM_NAME, Type.TEXT)
                        .withDefault(PARAMETER_NAME_DEFAULT_VALUE).build())
                .build();
        final ConfigDescription systemEphemeris = ConfigDescriptionBuilder.create(new URI("system:ephemeris"))
                .withParameter(ConfigDescriptionParameterBuilder.create(PARAM_COUNTRY, Type.TEXT).build()).build();
        when(mockedConfigDescriptionRegistry.getConfigDescriptions(any()))
                .thenReturn(List.of(systemI18n, systemEphemeris));
        when(mockedConfigDescriptionRegistry.getConfigDescription(eq(systemI18nURI), any())).thenReturn(systemI18n);

        resource = new ConfigDescriptionResource(mockedConfigDescriptionRegistry, mockedLocaleService);
    }

    @Test
    public void shouldReturnAllConfigDescriptions() throws IOException {
        Response response = resource.getAll(null, null);
        assertThat(response.getStatus(), is(200));
        assertThat(new String(((InputStream) response.getEntity()).readAllBytes(), StandardCharsets.UTF_8), is(
                "[{\"uri\":\"system:i18n\",\"parameters\":[{\"default\":\"test\",\"name\":\"name\",\"required\":false,\"type\":\"TEXT\",\"readOnly\":false,\"multiple\":false,\"advanced\":false,\"verify\":false,\"limitToOptions\":true,\"options\":[],\"filterCriteria\":[]}],\"parameterGroups\":[]},{\"uri\":\"system:ephemeris\",\"parameters\":[{\"name\":\"country\",\"required\":false,\"type\":\"TEXT\",\"readOnly\":false,\"multiple\":false,\"advanced\":false,\"verify\":false,\"limitToOptions\":true,\"options\":[],\"filterCriteria\":[]}],\"parameterGroups\":[]}]"));
    }

    @Test
    public void shouldReturnAConfigDescription() throws IOException {
        Response response = resource.getByURI(null, CONFIG_DESCRIPTION_SYSTEM_I18N_URI);
        assertThat(response.getStatus(), is(200));
        assertThat(new String(((InputStream) response.getEntity()).readAllBytes(), StandardCharsets.UTF_8), is(
                "{\"uri\":\"system:i18n\",\"parameters\":[{\"default\":\"test\",\"name\":\"name\",\"required\":false,\"type\":\"TEXT\",\"readOnly\":false,\"multiple\":false,\"advanced\":false,\"verify\":false,\"limitToOptions\":true,\"options\":[],\"filterCriteria\":[]}],\"parameterGroups\":[]}"));
    }

    @Test
    public void shouldReturnStatus404() {
        Response response = resource.getByURI(null, "uri:invalid");
        assertThat(response.getStatus(), is(404));
    }
}
