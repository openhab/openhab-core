/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
import static org.junit.jupiter.api.Assertions.*;
import static org.openhab.core.config.discovery.addon.mdns.MDNSAddonFinder.MDNS_SERVICE_TYPE;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoRegistry;
import org.openhab.core.addon.AddonMatchProperty;
import org.openhab.core.addon.AddonParameter;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.core.ParameterOption;
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

            List<AddonDiscoveryMethod> discoveryMethods = addonInfo.getDiscoveryMethods();
            assertNotNull(discoveryMethods);
            assertEquals(2, discoveryMethods.size());

            AddonDiscoveryMethod discoveryMethod = discoveryMethods.get(0);
            assertNotNull(discoveryMethod);
            assertEquals("mdns", discoveryMethod.getServiceType());
            List<AddonParameter> parameters = discoveryMethod.getParameters();
            assertNotNull(parameters);
            assertEquals(1, parameters.size());
            AddonParameter parameter = parameters.get(0);
            assertNotNull(parameter);
            assertEquals(MDNS_SERVICE_TYPE, parameter.getName());
            assertEquals("_hue._tcp.local.", parameter.getValue());
            List<AddonMatchProperty> properties = discoveryMethod.getMatchProperties();
            assertNotNull(properties);
            assertEquals(0, properties.size());

            discoveryMethod = discoveryMethods.get(1);
            assertNotNull(discoveryMethod);
            assertEquals("upnp", discoveryMethod.getServiceType());
            parameters = discoveryMethod.getParameters();
            assertNotNull(parameters);
            assertEquals(0, parameters.size());
            properties = discoveryMethod.getMatchProperties();
            assertNotNull(properties);
            assertEquals(1, properties.size());
            AddonMatchProperty property = properties.get(0);
            assertNotNull(property);
            assertEquals("modelName", property.getName());
            assertEquals("Philips hue bridge", property.getRegex());
            assertTrue(property.getPattern().matcher("Philips hue bridge").matches());
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
            assertThat(
                    listParameter.getOptions().stream().map(ParameterOption::toString)
                            .collect(Collectors.joining(", ")),
                    is("ParameterOption [value=\"key1\", label=\"label1\"], ParameterOption [value=\"key2\", label=\"label2\"]"));

            ConfigDescriptionParameter lightParameter = parameters.stream()
                    .filter(p -> "color-alarming-light".equals(p.getName())).findFirst().get();
            assertThat(lightParameter, is(notNullValue()));
            assertThat(
                    lightParameter.getFilterCriteria().stream().map(FilterCriteria::toString)
                            .collect(Collectors.joining(", ")),
                    is("FilterCriteria [name=\"tags\", value=\"alarm, light\"], FilterCriteria [name=\"type\", value=\"color\"], FilterCriteria [name=\"binding-id\", value=\"hue\"]"));
        });
    }
}
