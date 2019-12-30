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
package org.openhab.core.binding.xml.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.binding.BindingInfo;
import org.openhab.core.binding.BindingInfoRegistry;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * @author Alex Tugarev - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class BindingInfoTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "BundleInfoTest.bundle";
    private static final String TEST_BUNDLE_NAME2 = "BundleInfoTestNoAuthor.bundle";

    private BindingInfoRegistry bindingInfoRegistry;
    private ConfigDescriptionRegistry configDescriptionRegistry;
    private BindingInstaller bindingInstaller;

    @Before
    public void setUp() {
        bindingInfoRegistry = getService(BindingInfoRegistry.class);
        assertThat(bindingInfoRegistry, is(notNullValue()));
        configDescriptionRegistry = getService(ConfigDescriptionRegistry.class);
        assertThat(configDescriptionRegistry, is(notNullValue()));
        bindingInstaller = new BindingInstaller(this::waitForAssert, bindingInfoRegistry, bundleContext);
    }

    @Test
    public void assertThatBindingInfoIsReadProperly() throws Exception {
        bindingInstaller.exec(TEST_BUNDLE_NAME, () -> {
            Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos();
            BindingInfo bindingInfo = bindingInfos.iterator().next();
            assertThat(bindingInfo.getUID(), is("hue"));
            assertThat(bindingInfo.getConfigDescriptionURI(), is(URI.create("binding:hue")));
            assertThat(bindingInfo.getDescription(),
                    is("The hue Binding integrates the Philips hue system. It allows to control hue lights."));
            assertThat(bindingInfo.getName(), is("hue Binding"));
            assertThat(bindingInfo.getAuthor(), is("Deutsche Telekom AG"));
        });
    }

    @Test
    public void assertThatBindingInfoWithoutAuthorIsReadProperly() throws Exception {
        bindingInstaller.exec(TEST_BUNDLE_NAME2, () -> {
            Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos();
            BindingInfo bindingInfo = bindingInfos.iterator().next();
            assertThat(bindingInfo.getUID(), is("hue"));
            assertThat(bindingInfo.getConfigDescriptionURI(), is(URI.create("binding:hue")));
            assertThat(bindingInfo.getDescription(),
                    is("The hue Binding integrates the Philips hue system. It allows to control hue lights."));
            assertThat(bindingInfo.getName(), is("hue Binding"));
            assertThat(bindingInfo.getAuthor(), is((String) null));
        });
    }

    @Test
    public void assertThatConfigWithOptionsAndFilterAreProperlyRead() throws Exception {
        bindingInstaller.exec(TEST_BUNDLE_NAME, () -> {
            Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos();
            BindingInfo bindingInfo = bindingInfos.iterator().next();

            URI configDescriptionURI = bindingInfo.getConfigDescriptionURI();
            ConfigDescription configDescription = configDescriptionRegistry.getConfigDescription(configDescriptionURI);
            List<ConfigDescriptionParameter> parameters = configDescription.getParameters();
            assertThat(parameters.size(), is(2));

            ConfigDescriptionParameter listParameter = parameters.stream().filter(p -> p.getName().equals("list"))
                    .findFirst().get();
            assertThat(listParameter, is(notNullValue()));
            assertThat(listParameter.getOptions().stream().map(p -> p.toString()).collect(Collectors.joining(", ")), is(
                    "ParameterOption [value=\"key1\", label=\"label1\"], ParameterOption [value=\"key2\", label=\"label2\"]"));

            ConfigDescriptionParameter lightParameter = parameters.stream()
                    .filter(p -> p.getName().equals("color-alarming-light")).findFirst().get();
            assertThat(lightParameter, is(notNullValue()));
            assertThat(
                    lightParameter.getFilterCriteria().stream().map(p -> p.toString())
                            .collect(Collectors.joining(", ")),
                    is("FilterCriteria [name=\"tags\", value=\"alarm, light\"], FilterCriteria [name=\"type\", value=\"color\"], FilterCriteria [name=\"binding-id\", value=\"hue\"]"));
        });
    }
}
