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
package org.eclipse.smarthome.config.xml.test;

import static java.util.stream.Collectors.joining;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterGroup;
import org.eclipse.smarthome.config.core.ConfigDescriptionRegistry;
import org.eclipse.smarthome.test.SyntheticBundleInstaller;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * The ConfigDescriptionsTest is a test for loading of configuration description from XML documents.
 *
 * @author Alex Tugarev - Initial contribution; Extended tests for options and filters
 * @author Thomas Höfer - Added unit
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class ConfigDescriptionsTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "ConfigDescriptionsTest.bundle";
    private static final String FRAGMENT_TEST_HOST_NAME = "ConfigDescriptionsFragmentTest.host";
    private static final String FRAGMENT_TEST_FRAGMENT_NAME = "ConfigDescriptionsFragmentTest.fragment";

    private ConfigDescriptionRegistry configDescriptionRegistry;

    @Before
    public void setUp() {
        configDescriptionRegistry = getService(ConfigDescriptionRegistry.class);
        assertThat(configDescriptionRegistry, is(notNullValue()));
    }

    @After
    public void tearDown() throws Exception {
        SyntheticBundleInstaller.uninstall(bundleContext, TEST_BUNDLE_NAME);
        SyntheticBundleInstaller.uninstall(bundleContext, FRAGMENT_TEST_FRAGMENT_NAME);
        SyntheticBundleInstaller.uninstall(bundleContext, FRAGMENT_TEST_HOST_NAME);
    }

    @Test
    public void assertThatConfigDescriptionsAreLoadedProperly() throws Exception {
        int initialNumberOfConfigDescriptions = configDescriptionRegistry.getConfigDescriptions().size();

        // install test bundle
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(bundle, is(notNullValue()));

        Collection<ConfigDescription> englishConfigDescriptions = configDescriptionRegistry
                .getConfigDescriptions(Locale.ENGLISH);
        assertThat(englishConfigDescriptions.size(), is(initialNumberOfConfigDescriptions + 1));

        ConfigDescription englishDescription = findDescription(englishConfigDescriptions,
                new URI("config:dummyConfig"));
        assertThat(englishDescription, is(notNullValue()));

        List<ConfigDescriptionParameter> parameters = englishDescription.getParameters();
        assertThat(parameters.size(), is(14));

        ConfigDescriptionParameter ipParameter = findParameter(englishDescription, "ip");
        assertThat(ipParameter, is(notNullValue()));
        assertThat(ipParameter.getType(), is(Type.TEXT));
        assertThat(ipParameter.getGroupName(), is(nullValue()));
        assertThat(ipParameter.getContext(), is("network-address"));
        assertThat(ipParameter.getLabel(), is("Network Address"));
        assertThat(ipParameter.getDescription(), is("Network address of the hue bridge."));
        assertThat(ipParameter.getPattern(), is("[0-9]{3}.[0-9]{3}.[0-9]{3}.[0-9]{3}"));
        assertThat(ipParameter.isRequired(), is(true));
        assertThat(ipParameter.isMultiple(), is(false));
        assertThat(ipParameter.isReadOnly(), is(true));
        assertThat(ipParameter.getUnit(), is(nullValue()));
        assertThat(ipParameter.getUnitLabel(), is(nullValue()));

        ConfigDescriptionParameter usernameParameter = findParameter(englishDescription, "username");
        assertThat(usernameParameter, is(notNullValue()));
        assertThat(usernameParameter.getType(), is(Type.TEXT));
        assertThat(usernameParameter.getGroupName(), is("user"));
        assertThat(usernameParameter.getContext(), is("password"));
        assertThat(usernameParameter.getLabel(), is("Username"));
        assertThat(usernameParameter.isRequired(), is(false));
        assertThat(usernameParameter.isMultiple(), is(false));
        assertThat(usernameParameter.isReadOnly(), is(false));
        assertThat(usernameParameter.getDescription(),
                is("Name of a registered hue bridge user, that allows to access the API."));

        ConfigDescriptionParameter userPassParameter = findParameter(englishDescription, "user-pass");
        assertThat(userPassParameter, is(notNullValue()));
        assertThat(userPassParameter.getType(), is(Type.TEXT));
        assertThat(userPassParameter.getMinimum(), is(BigDecimal.valueOf(8)));
        assertThat(userPassParameter.getMaximum(), is(BigDecimal.valueOf(16)));
        assertThat(userPassParameter.isRequired(), is(true));
        assertThat(userPassParameter.isVerifyable(), is(true));
        assertThat(userPassParameter.isMultiple(), is(false));
        assertThat(userPassParameter.isReadOnly(), is(false));
        assertThat(userPassParameter.getContext(), is("password"));
        assertThat(userPassParameter.getLabel(), is("Password"));

        ConfigDescriptionParameter colorItemParameter = findParameter(englishDescription, "color-alarming-light");
        assertThat(colorItemParameter, is(notNullValue()));
        assertThat(colorItemParameter.getType(), is(Type.TEXT));
        assertThat(colorItemParameter.isRequired(), is(false));
        assertThat(colorItemParameter.isReadOnly(), is(false));
        assertThat(colorItemParameter.getContext(), is("item"));
        assertThat(colorItemParameter.getFilterCriteria(), is(notNullValue()));
        assertThat(
                colorItemParameter.getFilterCriteria().stream().map(c -> c.toString())
                        .collect(Collectors.joining(", ")),
                is("FilterCriteria [name=\"tags\", value=\"alarm, light\"], FilterCriteria [name=\"type\", value=\"color\"], FilterCriteria [name=\"binding-id\", value=\"hue\"]"));

        ConfigDescriptionParameter listParameter1 = findParameter(englishDescription, "list1");
        assertThat(listParameter1, is(notNullValue()));
        assertThat(listParameter1.getType(), is(Type.TEXT));
        assertThat(listParameter1.isRequired(), is(false));
        assertThat(listParameter1.isMultiple(), is(true));
        assertThat(listParameter1.isReadOnly(), is(false));
        assertThat(listParameter1.getMinimum(), is(BigDecimal.valueOf(2)));
        assertThat(listParameter1.getMaximum(), is(BigDecimal.valueOf(3)));
        assertThat(listParameter1.getOptions(), is(notNullValue()));
        assertThat(listParameter1.isAdvanced(), is(false));
        assertThat(listParameter1.isVerifyable(), is(false));
        assertThat(listParameter1.getLimitToOptions(), is(true));
        assertThat(listParameter1.getMultipleLimit(), is(nullValue()));
        assertThat(listParameter1.getOptions().stream().map(o -> o.toString()).collect(joining(", ")), is(
                "ParameterOption [value=\"key1\", label=\"label1\"], ParameterOption [value=\"key2\", label=\"label2\"]"));

        ConfigDescriptionParameter listParameter2 = findParameter(englishDescription, "list2");
        assertThat(listParameter2, is(notNullValue()));
        assertThat(listParameter2.getType(), is(Type.TEXT));
        assertThat(listParameter2.isRequired(), is(false));
        assertThat(listParameter2.isMultiple(), is(true));
        assertThat(listParameter2.isReadOnly(), is(false));
        assertThat(listParameter2.getOptions(), is(notNullValue()));
        assertThat(listParameter2.isAdvanced(), is(true));
        assertThat(listParameter2.getLimitToOptions(), is(false));
        assertThat(listParameter2.getMultipleLimit(), is(4));

        ConfigDescriptionParameter unitParameter = findParameter(englishDescription, "unit");
        assertThat(unitParameter, is(notNullValue()));
        assertThat(unitParameter.getUnit(), is("m"));
        assertThat(unitParameter.getUnitLabel(), is(nullValue()));

        ConfigDescriptionParameter unitLabelParameter = findParameter(englishDescription, "unit-label");
        assertThat(unitLabelParameter, is(notNullValue()));
        assertThat(unitLabelParameter.getUnit(), is(nullValue()));
        assertThat(unitLabelParameter.getUnitLabel(), is("Runs"));

        ConfigDescriptionParameter unitOhmParameter = findParameter(englishDescription, "unit-ohm");
        assertThat(unitOhmParameter, is(notNullValue()));
        assertThat(unitOhmParameter.getUnit(), is("Ω"));
        assertThat(unitOhmParameter.getUnitLabel(), is(nullValue()));

        ConfigDescriptionParameter unitAccelerationParameter = findParameter(englishDescription, "unit-acceleration");
        assertThat(unitAccelerationParameter, is(notNullValue()));
        assertThat(unitAccelerationParameter.getUnit(), is("m/s2"));
        assertThat(unitAccelerationParameter.getUnitLabel(), is("m/s\u00B2"));

        ConfigDescriptionParameter unitCelcius = findParameter(englishDescription, "unit-celcius");
        assertThat(unitCelcius, is(notNullValue()));
        assertThat(unitCelcius.getUnit(), is("Cel"));
        assertThat(unitCelcius.getUnitLabel(), is("°C"));

        ConfigDescriptionParameter unitSeconds = findParameter(englishDescription, "unit-seconds");
        assertThat(unitSeconds, is(notNullValue()));
        assertThat(unitSeconds.getUnit(), is("s"));
        assertThat(unitSeconds.getUnitLabel(), is("seconds"));

        ConfigDescriptionParameter unitMovements = findParameter(englishDescription, "unit-movements");
        assertThat(unitMovements, is(notNullValue()));
        assertThat(unitMovements.getUnit(), is(nullValue()));
        assertThat(unitMovements.getUnitLabel(), is("Movements"));

        ConfigDescriptionParameter unitKph = findParameter(englishDescription, "unit-kph");
        assertThat(unitKph, is(notNullValue()));
        assertThat(unitKph.getUnit(), is("kph"));
        assertThat(unitKph.getUnitLabel(), is("km/h"));

        assertThat(englishDescription.getParameterGroups().size(), is(2));

        ConfigDescriptionParameterGroup group1 = findParameterGroup(englishDescription, "group1");
        assertThat(group1, is(notNullValue()));
        assertThat(group1.getLabel(), is("Group 1"));
        assertThat(group1.getDescription(), is("Description Group 1"));
        assertThat(group1.isAdvanced(), is(false));
        assertThat(group1.getContext(), is("Context-Group1"));

        ConfigDescriptionParameterGroup group2 = findParameterGroup(englishDescription, "group2");
        assertThat(group2, is(notNullValue()));
        assertThat(group2.getLabel(), is("Group 2"));
        assertThat(group2.getDescription(), is("Description Group 2"));
        assertThat(group2.isAdvanced(), is(true));
        assertThat(group2.getContext(), is("Context-Group2"));

        ConfigDescription germanDescription = findDescription(
                configDescriptionRegistry.getConfigDescriptions(Locale.GERMAN), new URI("config:dummyConfig"));

        unitSeconds = findParameter(germanDescription, "unit-seconds");
        assertThat(unitSeconds, is(notNullValue()));
        assertThat(unitSeconds.getUnit(), is("s"));
        assertThat(unitSeconds.getUnitLabel(), is("Sekunden"));

        unitMovements = findParameter(germanDescription, "unit-movements");
        assertThat(unitMovements, is(notNullValue()));
        assertThat(unitMovements.getUnit(), is(nullValue()));
        assertThat(unitMovements.getUnitLabel(), is("Bewegungen"));

        unitKph = findParameter(germanDescription, "unit-kph");
        assertThat(unitKph, is(notNullValue()));
        assertThat(unitKph.getUnit(), is("kph"));
        assertThat(unitKph.getUnitLabel(), is("km/h"));

        // uninstall test bundle
        bundle.uninstall();
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
    }

    @Test
    public void assertThatConfigDescriptionsOfFragmentHostAreLoadedProperly() throws Exception {
        int initialNumberOfConfigDescriptions = configDescriptionRegistry.getConfigDescriptions().size();

        // install test bundle
        Bundle fragment = SyntheticBundleInstaller.installFragment(bundleContext, FRAGMENT_TEST_FRAGMENT_NAME);
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, FRAGMENT_TEST_HOST_NAME);
        assertThat(bundle, is(notNullValue()));

        Collection<ConfigDescription> configDescriptions = configDescriptionRegistry.getConfigDescriptions();
        assertThat(configDescriptions.size(), is(initialNumberOfConfigDescriptions + 1));

        ConfigDescription description = findDescription(configDescriptions, new URI("config:fragmentConfig"));
        assertThat(description, is(notNullValue()));

        List<ConfigDescriptionParameter> parameters = description.getParameters();
        assertThat(parameters.size(), is(1));

        ConfigDescriptionParameter usernameParameter = findParameter(description, "testParam");
        assertThat(usernameParameter, is(notNullValue()));
        assertThat(usernameParameter.getType(), is(Type.TEXT));
        assertThat(usernameParameter.getLabel(), is("Test"));
        assertThat(usernameParameter.isRequired(), is(false));
        assertThat(usernameParameter.isMultiple(), is(false));
        assertThat(usernameParameter.isReadOnly(), is(false));
        assertThat(usernameParameter.getDescription(), is("Test Parameter."));

        fragment.uninstall();
        assertThat(fragment.getState(), is(Bundle.UNINSTALLED));
        bundle.uninstall();
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
    }

    private static ConfigDescription findDescription(Collection<ConfigDescription> descriptions, URI uri) {
        return descriptions.stream().filter(d -> uri.equals(d.getUID())).findFirst().get();
    }

    private static ConfigDescriptionParameter findParameter(ConfigDescription description, String parameterName) {
        return description.getParameters().stream().filter(p -> parameterName.equals(p.getName())).findFirst().get();
    }

    private static ConfigDescriptionParameterGroup findParameterGroup(ConfigDescription description,
            String parameterGroupName) {
        return description.getParameterGroups().stream().filter(g -> parameterGroupName.equals(g.getName())).findFirst()
                .get();
    }
}
