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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;
import java.util.stream.Collectors;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
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
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class ConfigDescriptionI18nTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "yahooweather.bundle";

    private ConfigDescriptionRegistry configDescriptionRegistry;

    @Before
    public void setUp() {
        configDescriptionRegistry = getService(ConfigDescriptionRegistry.class);
        assertThat(configDescriptionRegistry, is(notNullValue()));
    }

    @After
    public void tearDown() throws Exception {
        SyntheticBundleInstaller.uninstall(bundleContext, TEST_BUNDLE_NAME);
    }

    @Test
    public void assertConfigDescriptionsAreLocalized() throws Exception {
        int initialNumberOfConfigDescriptions = configDescriptionRegistry.getConfigDescriptions().size();

        // install test bundle
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(bundle, is(notNullValue()));

        Collection<ConfigDescription> configDescriptions = configDescriptionRegistry
                .getConfigDescriptions(Locale.GERMAN);
        assertThat(configDescriptions.size(), is(initialNumberOfConfigDescriptions + 1));

        ConfigDescription config = new LinkedList<>(configDescriptions).getFirst();

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

    private static ConfigDescriptionParameter findParameter(ConfigDescription description, String parameterName) {
        return description.getParameters().stream().filter(p -> parameterName.equals(p.getName())).findFirst().get();
    }

    private static ConfigDescriptionParameterGroup findParameterGroup(ConfigDescription description,
            String parameterGroupName) {
        return description.getParameterGroups().stream().filter(g -> parameterGroupName.equals(g.getName())).findFirst()
                .get();
    }

}
