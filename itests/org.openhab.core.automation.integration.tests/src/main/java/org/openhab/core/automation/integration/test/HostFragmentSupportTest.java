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
package org.openhab.core.automation.integration.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
public class HostFragmentSupportTest extends JavaOSGiTest {

    private static final Locale BULGARIAN = new Locale("bg");
    private static final Locale DEFAULT = Locale.getDefault();
    private static final Locale GERMAN = Locale.GERMANY;

    private static final List<Locale> LOCALES = Arrays.asList(BULGARIAN, DEFAULT, GERMAN);

    private final Logger logger = LoggerFactory.getLogger(HostFragmentSupportTest.class);
    private ModuleTypeRegistry registry;
    private PackageAdmin pkgAdmin;

    private static final String EXT = ".jar";
    private static final String PATH = "/";
    private static final String RESOURCES_TEST_BUNDLE_1 = "host-tb1";
    private static final String RESOURCES_TEST_BUNDLE_2 = "host-tb2";
    private static final String RESOURCES_TEST_BUNDLE_3 = "fragment-tb1";
    private static final String RESOURCES_TEST_BUNDLE_4 = "fragment-tb2";

    private final String trigger1 = "Trigger1";
    private final String trigger2 = "Trigger2";
    private final String condition1 = "Condition1";
    private final String condition2 = "Condition2";
    private final String action1 = "Action1";

    private final String trigger1LabelBG = "Тригер 1 Етикет";
    private final String trigger1LabelDE = "Abzugshahn 1 Etikette";
    private final String trigger1LabelUpdatedBG = "Тригер 1 Обновен Етикет";
    private final String trigger1LabelUpdatedDE = "Abzugshahn 1 Aktualisiert Etikette";

    private final String trigger2LabelBG = "Тригер 2 Етикет";
    private final String trigger2LabelDE = "Abzugshahn 2 Etikette";
    private final String trigger2LabelUpdatedBG = "Тригер 2 Обновен Етикет";
    private final String trigger2LabelUpdatedDE = "Abzugshahn 2 Aktualisiert Etikette";

    private final String condition1LabelBG = "Условие 1 Етикет";
    private final String condition1LabelDE = "Bedingung 1 Etikette";
    private final String condition1LabelUpdatedBG = "Условие 1 Обновен Етикет";
    private final String condition1LabelUpdatedDE = "Bedingung 1 Aktualisiert Etikette";

    private final String condition2LabelBG = "Условие 2 Етикет";
    private final String condition2LabelDE = "Bedingung 2 Etikette";
    private final String condition2LabelUpdatedBG = "Условие 2 Обновен Етикет";
    private final String condition2LabelUpdatedDE = "Bedingung 2 Aktualisiert Etikette";

    private final String action1LabelBG = "Действие 1 Етикет";
    private final String action1LabelDE = "Aktion 1 Etikette";
    private final String action1LabelUpdatedBG = "Действие 1 Обновен Етикет";
    private final String action1LabelUpdatedDE = "Aktion 1 Aktualisiert Etikette";

    boolean waiting = true;

    @Before
    public void before() {
        logger.info("@Before.begin");

        Locale.setDefault(Locale.ENGLISH);
        registerVolatileStorageService();

        StorageService storageService = getService(StorageService.class);
        registry = getService(ModuleTypeRegistry.class);
        pkgAdmin = getService(PackageAdmin.class);
        waitForAssert(() -> {
            assertThat(storageService, is(notNullValue()));
            assertThat(registry, is(notNullValue()));
            assertThat(pkgAdmin, is(notNullValue()));
        }, 9000, 1000);

        logger.info("@Before.finish");
    }

    @After
    public void after() {
        logger.info("@After");
    }

    @Override
    protected void registerVolatileStorageService() {
        registerService(AutomationIntegrationJsonTest.VOLATILE_STORAGE_SERVICE);
    }

    private void assertThatModuleAndLocalizationsAreNotNull(String uid) {
        assertThat(registry.get(uid), is(notNullValue()));
        for (Locale locale : LOCALES) {
            assertThat(registry.get(uid, locale), is(notNullValue()));
        }
    }

    @SuppressWarnings("null")
    @Test
    public void assertThatTheInstallationOfTheFragmentHostProvidesTheResourcesCorrectly() throws BundleException {
        logger.info("asserting that the installation of the fragment-host provides the resources correctly");

        FrameworkListener listener = new FrameworkListener() {
            @Override
            public void frameworkEvent(FrameworkEvent event) {
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
            assertThat(registry.get(trigger2), is(nullValue()));
            assertThat(registry.get(condition2), is(nullValue()));
        }, 3000, 200);

        // then install the host
        Bundle host = bundleContext.installBundle(RESOURCES_TEST_BUNDLE_1,
                getClass().getClassLoader().getResourceAsStream(PATH + RESOURCES_TEST_BUNDLE_1 + EXT));
        assertThat(host, is(notNullValue()));
        host.start();

        // assert that the host and fragment resources are loaded
        waitForAssert(() -> {
            assertThat(host.getState(), is(Bundle.ACTIVE));
            assertThat(fragment.getState(), is(Bundle.RESOLVED));

            assertThatModuleAndLocalizationsAreNotNull(trigger1);
            assertThat(registry.get(trigger1, DEFAULT).getLabel(), is(registry.get(trigger1).getLabel()));
            assertThat(registry.get(trigger1, BULGARIAN).getLabel(), is(trigger1LabelBG));
            assertThat(registry.get(trigger1, GERMAN).getLabel(), is(trigger1LabelDE));

            assertThatModuleAndLocalizationsAreNotNull(condition1);
            assertThat(registry.get(condition1, DEFAULT).getLabel(), is(registry.get(condition1).getLabel()));
            assertThat(registry.get(condition1, BULGARIAN).getLabel(), is(condition1LabelBG));
            assertThat(registry.get(condition1, GERMAN).getLabel(), is(condition1LabelDE));

            assertThatModuleAndLocalizationsAreNotNull(action1);
            assertThat(registry.get(action1, DEFAULT).getLabel(), is(registry.get(action1).getLabel()));
            assertThat(registry.get(action1, BULGARIAN).getLabel(), is(action1LabelBG));
            assertThat(registry.get(action1, GERMAN).getLabel(), is(action1LabelDE));

            assertThatModuleAndLocalizationsAreNotNull(trigger2);
            assertThat(registry.get(trigger2, DEFAULT).getLabel(), is(registry.get(trigger2).getLabel()));
            assertThat(registry.get(trigger2, BULGARIAN).getLabel(), is(trigger2LabelBG));
            assertThat(registry.get(trigger2, GERMAN).getLabel(), is(trigger2LabelDE));

            assertThatModuleAndLocalizationsAreNotNull(condition2);
            assertThat(registry.get(condition2, DEFAULT).getLabel(), is(registry.get(condition2).getLabel()));
            assertThat(registry.get(condition2, BULGARIAN).getLabel(), is(condition2LabelBG));
            assertThat(registry.get(condition2, GERMAN).getLabel(), is(condition2LabelDE));
        }, 10000, 200);

        // first uninstall the fragment
        fragment.uninstall();
        assertThat(fragment.getState(), is(Bundle.UNINSTALLED));
        ;

        waiting = true;
        bundleContext.addFrameworkListener(listener);
        Bundle[] bundles = new Bundle[] { host, fragment };
        pkgAdmin.refreshPackages(bundles);
        waitForAssert(() -> assertFalse(waiting), 3000, 100);
        bundleContext.removeFrameworkListener(listener);

        waitForAssert(() -> {
            // assert that the host is updated and only its resources are available
            assertThatModuleAndLocalizationsAreNotNull(trigger1);
            assertThat(registry.get(trigger1, DEFAULT).getLabel(), is(registry.get(trigger1).getLabel()));
            assertThat(registry.get(trigger1, BULGARIAN).getLabel(), is(trigger1LabelBG));
            assertThat(registry.get(trigger1, GERMAN).getLabel(), is(trigger1LabelDE));

            assertThatModuleAndLocalizationsAreNotNull(condition1);
            assertThat(registry.get(condition1, DEFAULT).getLabel(), is(registry.get(condition1).getLabel()));
            assertThat(registry.get(condition1, BULGARIAN).getLabel(), is(condition1LabelBG));
            assertThat(registry.get(condition1, GERMAN).getLabel(), is(condition1LabelDE));

            assertThatModuleAndLocalizationsAreNotNull(action1);
            assertThat(registry.get(action1, DEFAULT).getLabel(), is(registry.get(action1).getLabel()));
            assertThat(registry.get(action1, BULGARIAN).getLabel(), is(action1LabelBG));
            assertThat(registry.get(action1, GERMAN).getLabel(), is(action1LabelDE));

            assertThat(registry.get(trigger2), is(nullValue()));
            assertThat(registry.get(condition2), is(nullValue()));
        }, 3000, 200);

        // then uninstall the host
        host.uninstall();

        // assert that the host resources also are removed
        waitForAssert(() -> {
            assertThat(host.getState(), is(Bundle.UNINSTALLED));
            assertThat(registry.get(trigger1), is(nullValue()));
            assertThat(registry.get(condition1), is(nullValue()));
            assertThat(registry.get(action1), is(nullValue()));
        }, 3000, 200);
    }

    @SuppressWarnings("null")
    @Test
    public void assertThatTheUpdateOfTheFragmentHostProvidesTheResourcesCorrectly() throws BundleException {
        logger.info("asserting that the update of the fragment-host provides the resources correctly");

        waiting = true;
        FrameworkListener listener = new FrameworkListener() {
            @Override
            public void frameworkEvent(FrameworkEvent event) {
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
            assertThat(registry.get(trigger2), is(nullValue()));
            assertThat(registry.get(condition2), is(nullValue()));
        }, 3000, 200);

        // then install the host
        Bundle host = bundleContext.installBundle(RESOURCES_TEST_BUNDLE_1,
                getClass().getClassLoader().getResourceAsStream(PATH + RESOURCES_TEST_BUNDLE_1 + EXT));
        assertThat(host, is(notNullValue()));
        host.start();

        // assert that the host and fragment resources are loaded
        waitForAssert(() -> {
            assertThat(host.getState(), is(Bundle.ACTIVE));
            assertThat(fragment.getState(), is(Bundle.RESOLVED));

            assertThatModuleAndLocalizationsAreNotNull(trigger1);
            assertThat(registry.get(trigger1, DEFAULT).getLabel(), is(registry.get(trigger1).getLabel()));
            assertThat(registry.get(trigger1, BULGARIAN).getLabel(), is(trigger1LabelBG));
            assertThat(registry.get(trigger1, GERMAN).getLabel(), is(trigger1LabelDE));

            assertThatModuleAndLocalizationsAreNotNull(condition1);
            assertThat(registry.get(condition1, DEFAULT).getLabel(), is(registry.get(condition1).getLabel()));
            assertThat(registry.get(condition1, BULGARIAN).getLabel(), is(condition1LabelBG));
            assertThat(registry.get(condition1, GERMAN).getLabel(), is(condition1LabelDE));

            assertThatModuleAndLocalizationsAreNotNull(action1);
            assertThat(registry.get(action1, DEFAULT).getLabel(), is(registry.get(action1).getLabel()));
            assertThat(registry.get(action1, BULGARIAN).getLabel(), is(action1LabelBG));
            assertThat(registry.get(action1, GERMAN).getLabel(), is(action1LabelDE));

            assertThatModuleAndLocalizationsAreNotNull(trigger2);
            assertThat(registry.get(trigger2, DEFAULT).getLabel(), is(registry.get(trigger2).getLabel()));
            assertThat(registry.get(trigger2, BULGARIAN).getLabel(), is(trigger2LabelBG));
            assertThat(registry.get(trigger2, GERMAN).getLabel(), is(trigger2LabelDE));

            assertThatModuleAndLocalizationsAreNotNull(condition2);
            assertThat(registry.get(condition2, DEFAULT).getLabel(), is(registry.get(condition2).getLabel()));
            assertThat(registry.get(condition2, BULGARIAN).getLabel(), is(condition2LabelBG));
            assertThat(registry.get(condition2, GERMAN).getLabel(), is(condition2LabelDE));
        }, 3000, 200);

        // first update the fragment
        fragment.update(getClass().getClassLoader().getResourceAsStream(PATH + RESOURCES_TEST_BUNDLE_4 + EXT));

        waiting = true;
        bundleContext.addFrameworkListener(listener);
        Bundle[] bundles = new Bundle[] { host, fragment };
        pkgAdmin.refreshPackages(bundles);
        waitForAssert(() -> assertFalse(waiting), 3000, 100);
        bundleContext.removeFrameworkListener(listener);

        waitForAssert(() -> {
            assertThat(host.getState(), is(Bundle.ACTIVE));
            assertThat(fragment.getState(), is(Bundle.RESOLVED));

            assertThatModuleAndLocalizationsAreNotNull(trigger1);
            assertThat(registry.get(trigger1, DEFAULT).getLabel(), is(registry.get(trigger1).getLabel()));
            assertThat(registry.get(trigger1, BULGARIAN).getLabel(), is(trigger1LabelBG));
            assertThat(registry.get(trigger1, GERMAN).getLabel(), is(trigger1LabelDE));

            assertThatModuleAndLocalizationsAreNotNull(condition1);
            assertThat(registry.get(condition1, DEFAULT).getLabel(), is(registry.get(condition1).getLabel()));
            assertThat(registry.get(condition1, BULGARIAN).getLabel(), is(condition1LabelBG));
            assertThat(registry.get(condition1, GERMAN).getLabel(), is(condition1LabelDE));

            assertThatModuleAndLocalizationsAreNotNull(action1);
            assertThat(registry.get(action1, DEFAULT).getLabel(), is(registry.get(action1).getLabel()));
            assertThat(registry.get(action1, BULGARIAN).getLabel(), is(action1LabelBG));
            assertThat(registry.get(action1, GERMAN).getLabel(), is(action1LabelDE));

            assertThatModuleAndLocalizationsAreNotNull(trigger2);
            assertThat(registry.get(trigger2, DEFAULT).getLabel(), is(registry.get(trigger2).getLabel()));
            assertThat(registry.get(trigger2, BULGARIAN).getLabel(), is(trigger2LabelUpdatedBG));
            assertThat(registry.get(trigger2, GERMAN).getLabel(), is(trigger2LabelUpdatedDE));

            assertThatModuleAndLocalizationsAreNotNull(condition2);
            assertThat(registry.get(condition2, DEFAULT).getLabel(), is(registry.get(condition2).getLabel()));
            assertThat(registry.get(condition2, BULGARIAN).getLabel(), is(condition2LabelUpdatedBG));
            assertThat(registry.get(condition2, GERMAN).getLabel(), is(condition2LabelUpdatedDE));
        }, 3000, 200);

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

            assertThatModuleAndLocalizationsAreNotNull(trigger1);
            assertThat(registry.get(trigger1, DEFAULT).getLabel(), is(registry.get(trigger1).getLabel()));
            assertThat(registry.get(trigger1, BULGARIAN).getLabel(), is(trigger1LabelUpdatedBG));
            assertThat(registry.get(trigger1, GERMAN).getLabel(), is(trigger1LabelUpdatedDE));

            assertThatModuleAndLocalizationsAreNotNull(condition1);
            assertThat(registry.get(condition1, DEFAULT).getLabel(), is(registry.get(condition1).getLabel()));
            assertThat(registry.get(condition1, BULGARIAN).getLabel(), is(condition1LabelUpdatedBG));
            assertThat(registry.get(condition1, GERMAN).getLabel(), is(condition1LabelUpdatedDE));

            assertThatModuleAndLocalizationsAreNotNull(action1);
            assertThat(registry.get(action1, DEFAULT).getLabel(), is(registry.get(action1).getLabel()));
            assertThat(registry.get(action1, BULGARIAN).getLabel(), is(action1LabelUpdatedBG));
            assertThat(registry.get(action1, GERMAN).getLabel(), is(action1LabelUpdatedDE));

            assertThatModuleAndLocalizationsAreNotNull(trigger2);
            assertThat(registry.get(trigger2, DEFAULT).getLabel(), is(registry.get(trigger2).getLabel()));
            assertThat(registry.get(trigger2, BULGARIAN).getLabel(), is(trigger2LabelUpdatedBG));
            assertThat(registry.get(trigger2, GERMAN).getLabel(), is(trigger2LabelUpdatedDE));

            assertThatModuleAndLocalizationsAreNotNull(condition2);
            assertThat(registry.get(condition2, DEFAULT).getLabel(), is(registry.get(condition2).getLabel()));
            assertThat(registry.get(condition2, BULGARIAN).getLabel(), is(condition2LabelUpdatedBG));
            assertThat(registry.get(condition2, GERMAN).getLabel(), is(condition2LabelUpdatedDE));
        }, 3000, 200);

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
            assertThatModuleAndLocalizationsAreNotNull(trigger1);
            assertThat(registry.get(trigger1, DEFAULT).getLabel(), is(registry.get(trigger1).getLabel()));
            assertThat(registry.get(trigger1, BULGARIAN).getLabel(), is(trigger1LabelUpdatedBG));
            assertThat(registry.get(trigger1, GERMAN).getLabel(), is(trigger1LabelUpdatedDE));

            assertThatModuleAndLocalizationsAreNotNull(condition1);
            assertThat(registry.get(condition1, DEFAULT).getLabel(), is(registry.get(condition1).getLabel()));
            assertThat(registry.get(condition1, BULGARIAN).getLabel(), is(condition1LabelUpdatedBG));
            assertThat(registry.get(condition1, GERMAN).getLabel(), is(condition1LabelUpdatedDE));

            assertThatModuleAndLocalizationsAreNotNull(action1);
            assertThat(registry.get(action1, DEFAULT).getLabel(), is(registry.get(action1).getLabel()));
            assertThat(registry.get(action1, BULGARIAN).getLabel(), is(action1LabelUpdatedBG));
            assertThat(registry.get(action1, GERMAN).getLabel(), is(action1LabelUpdatedDE));

            assertThat(registry.get(trigger2), is(nullValue()));
            assertThat(registry.get(condition2), is(nullValue()));
        }, 3000, 200);

        // then uninstall the host
        host.uninstall();
        assertThat(host.getState(), is(Bundle.UNINSTALLED));

        // assert that the host resources also are removed
        waitForAssert(() -> {
            assertThat(registry.get(trigger1), is(nullValue()));
            assertThat(registry.get(condition1), is(nullValue()));
            assertThat(registry.get(action1), is(nullValue()));
        }, 3000, 200);
    }
}
