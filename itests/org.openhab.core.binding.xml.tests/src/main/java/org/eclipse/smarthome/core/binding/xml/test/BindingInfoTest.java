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
package org.eclipse.smarthome.core.binding.xml.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionRegistry;
import org.eclipse.smarthome.core.binding.BindingInfo;
import org.eclipse.smarthome.core.binding.BindingInfoRegistry;
import org.eclipse.smarthome.test.SyntheticBundleInstaller;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author Alex Tugarev - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class BindingInfoTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "BundleInfoTest.bundle";
    private static final String TEST_BUNDLE_NAME2 = "BundleInfoTestNoAuthor.bundle";

    private BindingInfoRegistry bindingInfoRegistry;
    private ConfigDescriptionRegistry configDescriptionRegistry;

    @Before
    public void setUp() {
        bindingInfoRegistry = getService(BindingInfoRegistry.class);
        assertThat(bindingInfoRegistry, is(notNullValue()));
        configDescriptionRegistry = getService(ConfigDescriptionRegistry.class);
        assertThat(configDescriptionRegistry, is(notNullValue()));
    }

    @After
    public void tearDown() throws BundleException {
        SyntheticBundleInstaller.uninstall(bundleContext, TEST_BUNDLE_NAME);
    }

    @Test
    public void assertThatBindingInfoIsReadProperly() throws Exception {
        int initialNumberOfBindingInfos = bindingInfoRegistry.getBindingInfos().size();

        // install test bundle
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(bundle, is(notNullValue()));

        Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos();
        assertThat(bindingInfos.size(), is(initialNumberOfBindingInfos + 1));
        BindingInfo bindingInfo = bindingInfos.iterator().next();
        assertThat(bindingInfo.getUID(), is("hue"));
        assertThat(bindingInfo.getConfigDescriptionURI(), is(URI.create("binding:hue")));
        assertThat(bindingInfo.getDescription(),
                is("The hue Binding integrates the Philips hue system. It allows to control hue lights."));
        assertThat(bindingInfo.getName(), is("hue Binding"));
        assertThat(bindingInfo.getAuthor(), is("Deutsche Telekom AG"));

        // uninstall test bundle
        bundle.uninstall();
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
    }

    @Test
    public void assertThatBindingInfoWithoutAuthorIsReadProperly() throws Exception {
        int initialNumberOfBindingInfos = bindingInfoRegistry.getBindingInfos().size();

        // install test bundle
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME2);
        assertThat(bundle, is(notNullValue()));

        Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos();
        assertThat(bindingInfos.size(), is(initialNumberOfBindingInfos + 1));
        BindingInfo bindingInfo = bindingInfos.iterator().next();
        assertThat(bindingInfo.getUID(), is("hue"));
        assertThat(bindingInfo.getConfigDescriptionURI(), is(URI.create("binding:hue")));
        assertThat(bindingInfo.getDescription(),
                is("The hue Binding integrates the Philips hue system. It allows to control hue lights."));
        assertThat(bindingInfo.getName(), is("hue Binding"));
        assertThat(bindingInfo.getAuthor(), is((String) null));

        // uninstall test bundle
        bundle.uninstall();
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
    }

    @Test
    public void assertThatBindingInfoIsRemovedAfterTheBundleWasUninstalled() throws Exception {
        int initialNumberOfBindingInfos = bindingInfoRegistry.getBindingInfos().size();

        // install test bundle
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(bundle, is(notNullValue()));

        Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos();
        assertThat(bindingInfos.size(), is(initialNumberOfBindingInfos + 1));
        BindingInfo bindingInfo = bindingInfos.iterator().next();

        // uninstall test bundle
        bundle.uninstall();
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));

        bindingInfos = bindingInfoRegistry.getBindingInfos();
        assertThat(bindingInfos.size(), is(initialNumberOfBindingInfos));

        if (initialNumberOfBindingInfos > 0) {
            for (BindingInfo bindingInfo_ : bindingInfos) {
                assertThat(bindingInfo_.getUID(), is(not(bindingInfo.getUID())));
            }
        }
    }

    @Test
    public void assertThatConfigWithOptionsAndFilterAreProperlyRead() throws Exception {
        int initialNumberOfBindingInfos = bindingInfoRegistry.getBindingInfos().size();

        // install test bundle
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(bundle, is(notNullValue()));

        Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos();
        assertThat(bindingInfos.size(), is(initialNumberOfBindingInfos + 1));
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
        assertThat(lightParameter.getFilterCriteria().stream().map(p -> p.toString()).collect(Collectors.joining(", ")),
                is("FilterCriteria [name=\"tags\", value=\"alarm, light\"], FilterCriteria [name=\"type\", value=\"color\"], FilterCriteria [name=\"binding-id\", value=\"hue\"]"));
    }
}
