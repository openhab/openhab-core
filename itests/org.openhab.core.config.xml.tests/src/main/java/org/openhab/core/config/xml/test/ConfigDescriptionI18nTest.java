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
package org.openhab.core.config.xml.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterGroup;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.test.BundleCloseable;
import org.openhab.core.test.SyntheticBundleInstaller;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * The ConfigDescriptionI18nTest is a test for loading of configuration description from XML documents.
 *
 * @author Alex Tugarev - Initial contribution; Extended tests for options and filters
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class ConfigDescriptionI18nTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "yahooweather.bundle";

    private ConfigDescriptionProvider configDescriptionProvider;

    @Before
    public void setUp() {
        configDescriptionProvider = getService(ConfigDescriptionProvider.class,
                serviceReference -> "core.xml.config".equals(serviceReference.getProperty("esh.scope")));
        assertThat(configDescriptionProvider, is(notNullValue()));
    }

    @Test
    public void assertConfigDescriptionsAreLocalized() throws Exception {
        try (BundleCloseable bundle = new BundleCloseable(
                SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME))) {
            assertThat(bundle, is(notNullValue()));

            Collection<ConfigDescription> configDescriptions = configDescriptionProvider
                    .getConfigDescriptions(Locale.GERMAN);

            ConfigDescription config = findDescription(configDescriptions, "config:Dummy");
            assertThat(config, is(notNullValue()));

            String expected = "location.label = Ort\n" + //
                    "location.description = Ort der Wetterinformation.\n" + //
                    "unit.label = Einheit\n" + //
                    "unit.description = Spezifiziert die Einheit der Daten. Valide Werte sind 'us' und 'metric'\n" + //
                    "refresh.label = Aktualisierungsintervall\n" + //
                    "refresh.description = Spezifiziert das Aktualisierungsintervall in Sekunden\n" + //
                    "question.pattern = Wie ist das Wetter in [\\w]*?\n" + //
                    "question.options = München, Köln\n" + //
                    "group.label = Group 1 German Label\n" + //
                    "group.description = Group 1 German Description";

            assertEquals(expected, asString(config));
        }
    }

    private static String asString(ConfigDescription description) {
        ConfigDescriptionParameter location = findParameter(description, "location");
        ConfigDescriptionParameter unit = findParameter(description, "unit");
        ConfigDescriptionParameter refresh = findParameter(description, "refresh");
        ConfigDescriptionParameter question = findParameter(description, "question");
        ConfigDescriptionParameterGroup group = findParameterGroup(description, "group1");

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("location.label = %s\n", location.getLabel()));
        sb.append(String.format("location.description = %s\n", location.getDescription()));
        sb.append(String.format("unit.label = %s\n", unit.getLabel()));
        sb.append(String.format("unit.description = %s\n", unit.getDescription()));
        sb.append(String.format("refresh.label = %s\n", refresh.getLabel()));
        sb.append(String.format("refresh.description = %s\n", refresh.getDescription()));
        sb.append(String.format("question.pattern = %s\n", question.getPattern()));
        sb.append(String.format("question.options = %s\n",
                question.getOptions().stream().map(o -> o.getLabel()).collect(Collectors.joining(", "))));
        sb.append(String.format("group.label = %s\n", group.getLabel()));
        sb.append(String.format("group.description = %s", group.getDescription()));

        return sb.toString();
    }

    private static ConfigDescription findDescription(Collection<ConfigDescription> descriptions, String uri) {
        try {
            return findDescription(descriptions, new URI(uri));
        } catch (URISyntaxException e) {
            return null;
        }
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
