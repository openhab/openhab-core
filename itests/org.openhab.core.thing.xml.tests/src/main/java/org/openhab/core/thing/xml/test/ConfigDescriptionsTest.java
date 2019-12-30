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
package org.openhab.core.thing.xml.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterGroup;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.xml.test.LoadedTestBundle.StuffAddition;

/**
 * @author Henning Treu - Initial contribution
 */
public class ConfigDescriptionsTest extends JavaOSGiTest {

    private LoadedTestBundle loadedTestBundle() throws Exception {
        return new LoadedTestBundle("ConfigDescriptionsTest.bundle", bundleContext, this::getService,
                new StuffAddition().configDescriptions(3));
    }

    private ConfigDescriptionRegistry configDescriptionRegistry;

    @Before
    public void before() {
        configDescriptionRegistry = getService(ConfigDescriptionRegistry.class);
        assertThat(configDescriptionRegistry, is(notNullValue()));
    }

    @Test
    public void syntheticBundleShouldLoadFromTestResource() throws Exception {
        try (final AutoCloseable unused = loadedTestBundle()) {
        }
    }

    @Test
    public void configDescriptionsShouldLoadProperly() throws Exception {
        try (final AutoCloseable unused = loadedTestBundle()) {
            URI bridgeURI = new URI("thing-type:hue:bridge");
            ConfigDescription bridgeConfigDescription = configDescriptionRegistry.getConfigDescriptions().stream()
                    .filter(it -> it.getUID().equals(bridgeURI)).findFirst().get();

            assertThat(bridgeConfigDescription, is(notNullValue()));

            Collection<ConfigDescriptionParameter> parameters = bridgeConfigDescription.getParameters();
            assertThat(parameters.size(), is(2));

            ConfigDescriptionParameter ipParameter = parameters.stream().filter(it -> it.getName().equals("ip"))
                    .findFirst().get();
            assertThat(ipParameter, is(notNullValue()));
            assertThat(ipParameter.getType(), is(Type.TEXT));
            assertThat(ipParameter.getContext(), is("network-address"));
            assertThat(ipParameter.getLabel(), is("Network Address"));
            assertThat(ipParameter.getDescription(), is("Network address of the hue bridge."));
            assertThat(ipParameter.isRequired(), is(true));

            ConfigDescriptionParameter userNameParameter = parameters.stream()
                    .filter(it -> it.getName().equals("username")).findFirst().get();
            assertThat(userNameParameter, is(notNullValue()));

            assertThat(userNameParameter.isAdvanced(), is(true));

            assertThat(userNameParameter.getType(), is(Type.TEXT));
            assertThat(userNameParameter.getContext(), is("password"));
            assertThat(userNameParameter.getLabel(), is("Username"));
            assertThat(userNameParameter.getDescription(),
                    is("Name of a registered hue bridge user, that allows to access the API."));

            URI colorURI = new URI("channel-type:hue:color");
            ConfigDescription colorConfigDescription = configDescriptionRegistry.getConfigDescriptions().stream()
                    .filter(it -> it.getUID().equals(colorURI)).findFirst().get();

            assertThat(colorConfigDescription, is(notNullValue()));

            parameters = colorConfigDescription.getParameters();
            assertThat(parameters.size(), is(1));

            ConfigDescriptionParameter lastDimValueParameter = parameters.stream()
                    .filter(it -> it.getName().equals("lastDimValue")).findFirst().get();
            assertThat(lastDimValueParameter, is(notNullValue()));
            assertThat(lastDimValueParameter.getType(), is(Type.BOOLEAN));

            Collection<ConfigDescriptionParameterGroup> groups = bridgeConfigDescription.getParameterGroups();
            assertThat(groups.size(), is(2));

            ConfigDescriptionParameterGroup group1 = groups.stream().filter(it -> it.getName().equals("group1"))
                    .findFirst().get();
            assertThat(group1, is(notNullValue()));
            assertThat(group1.getContext(), is("Group1-context"));
            assertThat(group1.getLabel(), is("Group Label 1"));
            assertThat(group1.getDescription(), is("Group description 1"));
        }
    }

    @Test
    public void parametersWithOptionsAndFiltersShouldLoadProperly() throws Exception {
        try (final AutoCloseable unused = loadedTestBundle()) {
            URI dummyURI = new URI("thing-type:hue:dummy");
            ConfigDescription bridgeConfigDescription = configDescriptionRegistry.getConfigDescriptions().stream()
                    .filter(it -> it.getUID().equals(dummyURI)).findFirst().get();
            assertThat(bridgeConfigDescription, is(notNullValue()));

            Collection<ConfigDescriptionParameter> parameters = bridgeConfigDescription.getParameters();
            assertThat(parameters.size(), is(2));

            ConfigDescriptionParameter unitParameter = parameters.stream().filter(it -> it.getName().equals("unit"))
                    .findFirst().get();
            assertThat(unitParameter, is(notNullValue()));
            assertThat(join(unitParameter.getOptions(), ","), is(
                    "ParameterOption [value=\"us\", label=\"US\"],ParameterOption [value=\"metric\", label=\"Metric\"]"));

            ConfigDescriptionParameter lightParameter = parameters.stream()
                    .filter(it -> it.getName().equals("color-alarming-light")).findFirst().get();
            assertThat(lightParameter, is(notNullValue()));
            assertThat(join(lightParameter.getFilterCriteria(), ","), is(
                    "FilterCriteria [name=\"tags\", value=\"alarm, light\"],FilterCriteria [name=\"type\", value=\"color\"],FilterCriteria [name=\"binding-id\", value=\"hue\"]"));
        }
    }

    private String join(Collection<?> elements, String separator) {
        StringBuilder sb = new StringBuilder();
        for (Object element : elements) {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            if (element != null) {
                sb.append(element.toString());
            }
        }

        return sb.toString();
    }
}
