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
package org.openhab.core.addon.xml.test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoRegistry;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * @author Alex Tugarev - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@NonNullByDefault
public class AddonInfoTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "BundleInfoTest.bundle";

    private @NonNullByDefault({}) AddonInfoRegistry addonInfoRegistry;
    private @NonNullByDefault({}) ConfigDescriptionRegistry configDescriptionRegistry;
    private @NonNullByDefault({}) AddonInstaller addonInstaller;

    @BeforeEach
    public void setUp() {
        addonInfoRegistry = getService(AddonInfoRegistry.class);
        assertThat(addonInfoRegistry, is(notNullValue()));
        configDescriptionRegistry = getService(ConfigDescriptionRegistry.class);
        assertThat(configDescriptionRegistry, is(notNullValue()));
        addonInstaller = new AddonInstaller(this::waitForAssert, addonInfoRegistry, bundleContext);
    }

    @Test
    public void assertThatAddonInfoIsReadProperly() throws Exception {
        addonInstaller.exec(TEST_BUNDLE_NAME, () -> {
            Set<AddonInfo> addonInfos = addonInfoRegistry.getAddonInfos();
            AddonInfo addonInfo = addonInfos.iterator().next();
            assertThat(addonInfo.getId(), is("hue"));
            assertThat(addonInfo.getUID(), is("binding-hue"));
            assertThat(addonInfo.getConfigDescriptionURI(), is("binding:hue"));
            assertThat(addonInfo.getDescription(),
                    is("The hue Binding integrates the Philips hue system. It allows to control hue lights."));
            assertThat(addonInfo.getName(), is("hue Binding"));
        });
    }

    @Test
    public void assertThatConfigWithOptionsAndFilterAreProperlyRead() throws Exception {
        addonInstaller.exec(TEST_BUNDLE_NAME, () -> {
            Set<AddonInfo> bindingInfos = addonInfoRegistry.getAddonInfos();
            AddonInfo bindingInfo = bindingInfos.iterator().next();

            String configDescriptionURI = Objects.requireNonNull(bindingInfo.getConfigDescriptionURI());
            ConfigDescription configDescription = configDescriptionRegistry
                    .getConfigDescription(URI.create(configDescriptionURI));
            List<ConfigDescriptionParameter> parameters = configDescription.getParameters();
            assertThat(parameters.size(), is(2));

            ConfigDescriptionParameter listParameter = parameters.stream().filter(p -> "list".equals(p.getName()))
                    .findFirst().get();
            assertThat(listParameter, is(notNullValue()));
            assertThat(listParameter.getOptions().stream().map(p -> p.toString()).collect(Collectors.joining(", ")), is(
                    "ParameterOption [value=\"key1\", label=\"label1\"], ParameterOption [value=\"key2\", label=\"label2\"]"));

            ConfigDescriptionParameter lightParameter = parameters.stream()
                    .filter(p -> "color-alarming-light".equals(p.getName())).findFirst().get();
            assertThat(lightParameter, is(notNullValue()));
            assertThat(
                    lightParameter.getFilterCriteria().stream().map(p -> p.toString())
                            .collect(Collectors.joining(", ")),
                    is("FilterCriteria [name=\"tags\", value=\"alarm, light\"], FilterCriteria [name=\"type\", value=\"color\"], FilterCriteria [name=\"binding-id\", value=\"hue\"]"));
        });
    }
}
