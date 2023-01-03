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
package org.openhab.core.automation.integration.test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.storage.StorageService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test class tests the Host - Fragment support for automation and localization resources
 *
 * @author Ana Dimova - Initial contribution
 * @author Kai Kreuzer - Refactored to Java
 */
@SuppressWarnings("deprecation")
@NonNullByDefault
public class HostFragmentSupportTest extends JavaOSGiTest {

    private static final Locale BULGARIAN = new Locale("bg");
    private static final Locale DEFAULT = Locale.ENGLISH;
    private static final Locale GERMAN = Locale.GERMANY;

    private final Logger logger = LoggerFactory.getLogger(HostFragmentSupportTest.class);
    private @NonNullByDefault({}) ModuleTypeRegistry registry;
    private @NonNullByDefault({}) PackageAdmin pkgAdmin;

    private static final String EXT = ".jar";
    private static final String PATH = "/";
    private static final String RESOURCES_TEST_BUNDLE_1 = "host-tb1";
    private static final String RESOURCES_TEST_BUNDLE_2 = "host-tb2";
    private static final String RESOURCES_TEST_BUNDLE_3 = "fragment-tb1";
    private static final String RESOURCES_TEST_BUNDLE_4 = "fragment-tb2";

    private static final String TRIGGER1 = "Trigger1";
    private static final String TRIGGER2 = "Trigger2";
    private static final String CONDITION1 = "Condition1";
    private static final String CONDITION2 = "Condition2";
    private static final String ACTION1 = "Action1";

    private static final Map<Locale, String> TRIGGER1_LABEL = Map.of( //
            BULGARIAN, "Тригер 1 Етикет", //
            GERMAN, "Abzugshahn 1 Etikette");
    private static final Map<Locale, String> TRIGGER1_LABEL_UPDATED = Map.of( //
            BULGARIAN, "Тригер 1 Обновен Етикет", //
            GERMAN, "Abzugshahn 1 Aktualisiert Etikette");

    private static final Map<Locale, String> TRIGGER2_LABEL = Map.of( //
            BULGARIAN, "Тригер 2 Етикет", //
            GERMAN, "Abzugshahn 2 Etikette");
    private static final Map<Locale, String> TRIGGER2_LABEL_UPDATED = Map.of( //
            BULGARIAN, "Тригер 2 Обновен Етикет", //
            GERMAN, "Abzugshahn 2 Aktualisiert Etikette");

    private static final Map<Locale, String> CONDITION1_LABEL = Map.of( //
            BULGARIAN, "Условие 1 Етикет", //
            GERMAN, "Bedingung 1 Etikette");
    private static final Map<Locale, String> CONDITION1_LABEL_UPDATED = Map.of( //
            BULGARIAN, "Условие 1 Обновен Етикет", //
            GERMAN, "Bedingung 1 Aktualisiert Etikette");

    private static final Map<Locale, String> CONDITION2_LABEL = Map.of( //
            BULGARIAN, "Условие 2 Етикет", //
            GERMAN, "Bedingung 2 Etikette");
    private static final Map<Locale, String> CONDITION2_LABEL_UPDATED = Map.of( //
            BULGARIAN, "Условие 2 Обновен Етикет", //
            GERMAN, "Bedingung 2 Aktualisiert Etikette");

    private static final Map<Locale, String> ACTION1_LABEL = Map.of( //
            BULGARIAN, "Действие 1 Етикет", //
            GERMAN, "Aktion 1 Etikette");
    private static final Map<Locale, String> ACTION1_LABEL_UPDATED = Map.of( //
            BULGARIAN, "Действие 1 Обновен Етикет", //
            GERMAN, "Aktion 1 Aktualisiert Etikette");

    private boolean waiting = true;

    @BeforeEach
    public void before() {
        logger.info("@Before.begin");

        Locale.setDefault(Locale.ENGLISH);
        registerVolatileStorageService();

        StorageService storageService = getService(StorageService.class);
        registry = getService(ModuleTypeRegistry.class);
        pkgAdmin = getService(PackageAdmin.class);

        assertThat(storageService, is(notNullValue()));
        assertThat(registry, is(notNullValue()));
        assertThat(pkgAdmin, is(notNullValue()));

        logger.info("@Before.finish");
    }

    @AfterEach
    public void after() {
        logger.info("@After");
    }

    @Override
    protected void registerVolatileStorageService() {
        registerService(AutomationIntegrationJsonTest.VOLATILE_STORAGE_SERVICE);
    }

    @SuppressWarnings("null")
    private void assertThatModuleTypeLabelIsLocalized(String uid, Map<Locale, String> labelLocalizations) {
        ModuleType moduleType = registry.get(uid);
        assertThat(moduleType, is(notNullValue()));

        ModuleType defaultModuleType = registry.get(uid, DEFAULT);
        assertThat(defaultModuleType, is(notNullValue()));

        assertThat(defaultModuleType.getLabel(), is(moduleType.getLabel()));

        for (Entry<Locale, String> entry : labelLocalizations.entrySet()) {
            moduleType = registry.get(uid, entry.getKey());
            assertThat(moduleType, is(notNullValue()));
            assertThat(moduleType.getLabel(), is(entry.getValue()));
        }
    }

    @SuppressWarnings("null")
    @Test
    public void assertThatTheInstallationOfTheFragmentHostProvidesTheResourcesCorrectly() throws BundleException {
        logger.info("asserting that the installation of the fragment-host provides the resources correctly");

        FrameworkListener listener = new FrameworkListener() {
            @Override
            public void frameworkEvent(@NonNullByDefault({}) FrameworkEvent event) {
                waiting = false;
            }
        };

        // first install the fragment
        Bundle fragment = bundleContext.installBundle(RESOURCES_TEST_BUNDLE_3,
                getClass().getClassLoader().getResourceAsStream(PATH + RESOURCES_TEST_BUNDLE_3 + EXT));
        assertThat(fragment, is(notNullValue()));

        // assert that the host and fragment resources are not loaded
        waitForAssert(() -> {
            assertThat(fragment.getState(), is(Bundle.INSTALLED));
            assertThat(registry.get(TRIGGER2), is(nullValue()));
            assertThat(registry.get(CONDITION2), is(nullValue()));
        });

        // then install the host
        Bundle host = bundleContext.installBundle(RESOURCES_TEST_BUNDLE_1,
                getClass().getClassLoader().getResourceAsStream(PATH + RESOURCES_TEST_BUNDLE_1 + EXT));
        assertThat(host, is(notNullValue()));
        host.start();

        // assert that the host and fragment resources are loaded
        waitForAssert(() -> {
            assertThat(host.getState(), is(Bundle.ACTIVE));
            assertThat(fragment.getState(), is(Bundle.RESOLVED));

            assertThatModuleTypeLabelIsLocalized(TRIGGER1, TRIGGER1_LABEL);
            assertThatModuleTypeLabelIsLocalized(CONDITION1, CONDITION1_LABEL);
            assertThatModuleTypeLabelIsLocalized(ACTION1, ACTION1_LABEL);
            assertThatModuleTypeLabelIsLocalized(TRIGGER2, TRIGGER2_LABEL);
            assertThatModuleTypeLabelIsLocalized(CONDITION2, CONDITION2_LABEL);
        });

        // first uninstall the fragment
        fragment.uninstall();
        assertThat(fragment.getState(), is(Bundle.UNINSTALLED));

        waiting = true;
        bundleContext.addFrameworkListener(listener);
        Bundle[] bundles = { host, fragment };
        pkgAdmin.refreshPackages(bundles);
        waitForAssert(() -> assertFalse(waiting), 3000, 100);
        bundleContext.removeFrameworkListener(listener);

        waitForAssert(() -> {
            // assert that the host is updated and only its resources are available
            assertThatModuleTypeLabelIsLocalized(TRIGGER1, TRIGGER1_LABEL);
            assertThatModuleTypeLabelIsLocalized(CONDITION1, CONDITION1_LABEL);
            assertThatModuleTypeLabelIsLocalized(ACTION1, ACTION1_LABEL);

            assertThat(registry.get(TRIGGER2), is(nullValue()));
            assertThat(registry.get(CONDITION2), is(nullValue()));
        });

        // then uninstall the host
        host.uninstall();

        // assert that the host resources also are removed
        waitForAssert(() -> {
            assertThat(host.getState(), is(Bundle.UNINSTALLED));
            assertThat(registry.get(TRIGGER1), is(nullValue()));
            assertThat(registry.get(CONDITION1), is(nullValue()));
            assertThat(registry.get(ACTION1), is(nullValue()));
        });
    }

    @SuppressWarnings("null")
    @Test
    public void assertThatTheUpdateOfTheFragmentHostProvidesTheResourcesCorrectly() throws BundleException {
        logger.info("asserting that the update of the fragment-host provides the resources correctly");

        waiting = true;
        FrameworkListener listener = new FrameworkListener() {
            @Override
            public void frameworkEvent(@NonNullByDefault({}) FrameworkEvent event) {
                waiting = false;
            }
        };

        // first install the fragment
        Bundle fragment = bundleContext.installBundle(RESOURCES_TEST_BUNDLE_3,
                getClass().getClassLoader().getResourceAsStream(PATH + RESOURCES_TEST_BUNDLE_3 + EXT));
        assertThat(fragment, is(notNullValue()));

        // assert that the host and fragment resources are not loaded
        waitForAssert(() -> {
            assertThat(fragment.getState(), is(Bundle.INSTALLED));
            assertThat(registry.get(TRIGGER2), is(nullValue()));
            assertThat(registry.get(CONDITION2), is(nullValue()));
        });

        // then install the host
        Bundle host = bundleContext.installBundle(RESOURCES_TEST_BUNDLE_1,
                getClass().getClassLoader().getResourceAsStream(PATH + RESOURCES_TEST_BUNDLE_1 + EXT));
        assertThat(host, is(notNullValue()));
        host.start();

        // assert that the host and fragment resources are loaded
        waitForAssert(() -> {
            assertThat(host.getState(), is(Bundle.ACTIVE));
            assertThat(fragment.getState(), is(Bundle.RESOLVED));

            assertThatModuleTypeLabelIsLocalized(TRIGGER1, TRIGGER1_LABEL);
            assertThatModuleTypeLabelIsLocalized(CONDITION1, CONDITION1_LABEL);
            assertThatModuleTypeLabelIsLocalized(ACTION1, ACTION1_LABEL);
            assertThatModuleTypeLabelIsLocalized(TRIGGER2, TRIGGER2_LABEL);
            assertThatModuleTypeLabelIsLocalized(CONDITION2, CONDITION2_LABEL);
        });

        // first update the fragment
        fragment.update(getClass().getClassLoader().getResourceAsStream(PATH + RESOURCES_TEST_BUNDLE_4 + EXT));

        waiting = true;
        bundleContext.addFrameworkListener(listener);
        Bundle[] bundles = { host, fragment };
        pkgAdmin.refreshPackages(bundles);
        waitForAssert(() -> assertFalse(waiting), 3000, 100);
        bundleContext.removeFrameworkListener(listener);

        waitForAssert(() -> {
            assertThat(host.getState(), is(Bundle.ACTIVE));
            assertThat(fragment.getState(), is(Bundle.RESOLVED));

            assertThatModuleTypeLabelIsLocalized(TRIGGER1, TRIGGER1_LABEL);
            assertThatModuleTypeLabelIsLocalized(CONDITION1, CONDITION1_LABEL);
            assertThatModuleTypeLabelIsLocalized(ACTION1, ACTION1_LABEL);
            assertThatModuleTypeLabelIsLocalized(TRIGGER2, TRIGGER2_LABEL_UPDATED);
            assertThatModuleTypeLabelIsLocalized(CONDITION2, CONDITION2_LABEL_UPDATED);
        });

        // then update the host
        host.update(getClass().getClassLoader().getResourceAsStream(PATH + RESOURCES_TEST_BUNDLE_2 + EXT));

        waiting = true;
        bundleContext.addFrameworkListener(listener);
        pkgAdmin.refreshPackages(bundles);
        waitForAssert(() -> assertFalse(waiting), 3000, 100);
        bundleContext.removeFrameworkListener(listener);

        waitForAssert(() -> {
            assertThat(host.getState(), is(Bundle.ACTIVE));
            assertThat(fragment.getState(), is(Bundle.RESOLVED));

            assertThatModuleTypeLabelIsLocalized(TRIGGER1, TRIGGER1_LABEL_UPDATED);
            assertThatModuleTypeLabelIsLocalized(CONDITION1, CONDITION1_LABEL_UPDATED);
            assertThatModuleTypeLabelIsLocalized(ACTION1, ACTION1_LABEL_UPDATED);
            assertThatModuleTypeLabelIsLocalized(TRIGGER2, TRIGGER2_LABEL_UPDATED);
            assertThatModuleTypeLabelIsLocalized(CONDITION2, CONDITION2_LABEL_UPDATED);
        });

        // first uninstall the fragment
        fragment.uninstall();
        assertThat(fragment.getState(), is(Bundle.UNINSTALLED));

        waiting = true;
        bundleContext.addFrameworkListener(listener);
        pkgAdmin.refreshPackages(bundles);
        waitForAssert(() -> assertFalse(waiting), 3000, 100);
        bundleContext.removeFrameworkListener(listener);

        // assert that the host is updated and only its resources are available
        waitForAssert(() -> {
            assertThatModuleTypeLabelIsLocalized(TRIGGER1, TRIGGER1_LABEL_UPDATED);
            assertThatModuleTypeLabelIsLocalized(CONDITION1, CONDITION1_LABEL_UPDATED);
            assertThatModuleTypeLabelIsLocalized(ACTION1, ACTION1_LABEL_UPDATED);

            assertThat(registry.get(TRIGGER2), is(nullValue()));
            assertThat(registry.get(CONDITION2), is(nullValue()));
        });

        // then uninstall the host
        host.uninstall();
        assertThat(host.getState(), is(Bundle.UNINSTALLED));

        // assert that the host resources also are removed
        waitForAssert(() -> {
            assertThat(registry.get(TRIGGER1), is(nullValue()));
            assertThat(registry.get(CONDITION1), is(nullValue()));
            assertThat(registry.get(ACTION1), is(nullValue()));
        });
    }
}
