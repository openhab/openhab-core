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
package org.openhab.core.io.rest.core.internal.service;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.io.rest.core.service.ConfigurableServiceDTO;
import org.openhab.core.test.AsyncResultWrapper;
import org.openhab.core.test.java.JavaOSGiTest;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * {@link ConfigurableServiceResourceOSGiTest} tests the ConfigurableService REST resource on the OSGi level.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class ConfigurableServiceResourceOSGiTest extends JavaOSGiTest {

    public interface SomeServiceInterface {
    }

    private ConfigurableServiceResource configurableServiceResource;
    private final AsyncResultWrapper<Dictionary<String, Object>> propertiesWrapper = new AsyncResultWrapper<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        Configuration configuration = mock(Configuration.class);
        doAnswer(answer -> propertiesWrapper.getWrappedObject()).when(configuration).getProperties();
        doAnswer(answer -> {
            propertiesWrapper.set(answer.getArgument(0));
            return null;
        }).when(configuration).update(any(Dictionary.class));
        doAnswer(answer -> {
            propertiesWrapper.reset();
            return null;
        }).when(configuration).delete();

        propertiesWrapper.reset();

        ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class);
        when(configAdmin.getConfiguration(any())).thenReturn(configuration);
        when(configAdmin.getConfiguration(any(), nullable(String.class))).thenReturn(configuration);

        registerService(configAdmin);

        configurableServiceResource = getService(ConfigurableServiceResource.class);
        assertThat(configurableServiceResource, is(notNullValue()));
    }

    @Test
    public void assertGetConfigurableServicesWorks() {
        int num = configurableServiceResource.getAll().size();

        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("service.pid", "pid");
        properties.put("service.config.description.uri", "someuri");
        properties.put("service.config.label", "label");
        properties.put("service.config.category", "category");

        registerService(mock(SomeServiceInterface.class), properties);

        List<ConfigurableServiceDTO> configurableServices = configurableServiceResource.getAll();
        assertThat(configurableServices.size(), is(num + 1));

        Optional<ConfigurableServiceDTO> optionalService = configurableServices.stream()
                .filter(configurableService -> "pid".equals(configurableService.id)).findFirst();
        assertThat(optionalService.isPresent(), is(true));

        if (optionalService.isPresent()) {
            ConfigurableServiceDTO service = optionalService.get();
            assertThat(service.id, is(equalTo("pid")));
            assertThat(service.configDescriptionURI, is(equalTo("someuri")));
            assertThat(service.label, is(equalTo("label")));
            assertThat(service.category, is(equalTo("category")));
        }
    }

    @Test
    public void assertComponentNameFallbackWorks() {
        int num = configurableServiceResource.getAll().size();

        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("component.name", "component.name");
        properties.put("service.config.description.uri", "someuri");

        registerService(mock(SomeServiceInterface.class), properties);

        List<ConfigurableServiceDTO> configurableServices = configurableServiceResource.getAll();
        assertThat(configurableServices.size(), is(num + 1));

        Optional<ConfigurableServiceDTO> optionalService = configurableServices.stream()
                .filter(configurableService -> "component.name".equals(configurableService.id)).findFirst();
        assertThat(optionalService.isPresent(), is(true));

        if (optionalService.isPresent()) {
            ConfigurableServiceDTO service = optionalService.get();
            assertThat(service.id, is(equalTo("component.name")));
            assertThat(service.configDescriptionURI, is(equalTo("someuri")));
        }
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void assertConfigurationManagementWorks() {
        Response response = configurableServiceResource.getConfiguration("id");
        assertThat(response.getStatus(), is(200));

        Map<String, Object> newConfiguration = new HashMap<>();
        newConfiguration.put("a", "b");
        response = configurableServiceResource.updateConfiguration("id", newConfiguration);
        assertThat(response.getStatus(), is(204));

        response = configurableServiceResource.getConfiguration("id");
        assertThat(response.getStatus(), is(200));
        assertThat(((Map) response.getEntity()).get("a"), is(equalTo("b")));

        newConfiguration.put("a", "c");
        response = configurableServiceResource.updateConfiguration("id", newConfiguration);
        assertThat(response.getStatus(), is(200));
        assertThat(((Map) response.getEntity()).get("a"), is(equalTo("b")));

        response = configurableServiceResource.deleteConfiguration("id");
        assertThat(response.getStatus(), is(200));
        assertThat(((org.openhab.core.config.core.Configuration) response.getEntity()).getProperties().get("a"),
                is(equalTo("c")));

        response = configurableServiceResource.getConfiguration("id");
        assertThat(response.getStatus(), is(200));
        assertThat(((Map) response.getEntity()).get("a"), nullValue());

        response = configurableServiceResource.deleteConfiguration("id");
        assertThat(response.getStatus(), is(204));
    }
}
