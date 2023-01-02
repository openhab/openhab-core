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
package org.openhab.core.internal.i18n;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.test.java.JavaOSGiTest;
import org.osgi.framework.Bundle;

/**
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
public class TranslationProviderOSGiTest extends JavaOSGiTest {

    private static final String KEY_HELLO = "HELLO";
    private static final String KEY_HELLO_SINGLE_NAME = "HELLO_SINGLE_NAME";
    private static final String KEY_HELLO_MULTIPLE_NAMES = "HELLO_MULTIPLE_NAMES";

    private static final String KEY_BYE = "BYE";

    private static final String HELLO_WORLD_DEFAULT = "Hallo Welt!";
    private static final String HELLO_WORLD_EN = "Hello World!";
    private static final String HELLO_WORLD_FR = "Bonjour le monde!";

    private static final String[] NAMES = new String[] { "openHAB", "thing", "rule" };
    private static final String DEFAULT_SINGLE_NAME_TEXT = "Hi {0}!";

    private static final String HELLO_SINGLE_NAME_DEFAULT = "Hallo openHAB!";
    private static final String HELLO_SINGLE_NAME_EN = "Hello openHAB!";
    private static final String HELLO_SINGLE_NAME_FR = "Bonjour openHAB!";

    private static final String HELLO_MULTIPLE_NAMES_DEFAULT = "Hallo openHAB, Hallo thing, Hallo rule!";
    private static final String HELLO_MULTIPLE_NAMES_EN = "Hello openHAB, Hello thing, Hello rule!";
    private static final String HELLO_MULTIPLE_NAMES_FR = "Bonjour openHAB, Bonjour thing, Bonjour rule!";

    private static final String BYE_DE = "Tschüß!";
    private static final String BYE_EN = "Bye!";

    private @NonNullByDefault({}) TranslationProvider translationProvider;

    @BeforeEach
    public void setup() {
        translationProvider = getService(TranslationProvider.class);

        LocaleProvider localeProvider = getService(LocaleProvider.class);
        Map<String, Object> localeCfg = new HashMap<>();
        localeCfg.put("language", "de");
        localeCfg.put("region", "DE");
        ((I18nProviderImpl) localeProvider).modified(localeCfg);
    }

    private static void assertTextEquals(@Nullable String actual, @Nullable String expected) {
        if (expected != null) {
            assertThat(actual, is(notNullValue()));
            assertThat(actual, is(equalTo(expected)));
        } else {
            assertThat(actual, is(nullValue()));
        }
    }

    @Test
    public void assertThatGetTextWithoutBundleIsWorkingProperly() {
        String text = translationProvider.getText(null, null, null, null);
        assertTextEquals(text, null);

        text = translationProvider.getText(null, null, "default", null);
        assertTextEquals(text, "default");
    }

    @Test
    public void assertThatGetTextViaBundleIsWorkingProperly() {
        Bundle bundle = bundleContext.getBundle();
        String text = translationProvider.getText(bundle, null, null, null);
        assertTextEquals(text, null);

        text = translationProvider.getText(bundle, null, "default", null);
        assertTextEquals(text, "default");

        text = translationProvider.getText(bundle, "UNKNOWN_HELLO", "default", null);
        assertTextEquals(text, "default");

        text = translationProvider.getText(bundle, "UNKNOWN_HELLO", null, null);
        assertTextEquals(text, null);

        text = translationProvider.getText(bundle, KEY_HELLO, "default", null);
        assertTextEquals(text, HELLO_WORLD_DEFAULT);

        text = translationProvider.getText(bundle, KEY_HELLO, "default", Locale.FRENCH);
        assertTextEquals(text, HELLO_WORLD_FR);

        text = translationProvider.getText(bundle, KEY_HELLO, "default", Locale.ENGLISH);
        assertTextEquals(text, HELLO_WORLD_EN);

        text = translationProvider.getText(bundle, KEY_BYE, "default", null);
        assertTextEquals(text, "default");

        text = translationProvider.getText(bundle, KEY_BYE, "default", new Locale("de", "AT"));
        assertTextEquals(text, BYE_DE);

        text = translationProvider.getText(bundle, KEY_BYE, "default", Locale.ENGLISH);
        assertTextEquals(text, BYE_EN);
    }

    @Test
    public void assertThatGetTextWithArgumentsDelegatesProperlyToGetTextWithoutArguments() {
        Bundle bundle = bundleContext.getBundle();

        String text = translationProvider.getText(bundle, null, null, null, (Object[]) null);
        assertTextEquals(text, null);

        text = translationProvider.getText(bundle, null, "default", null, (Object[]) null);
        assertTextEquals(text, "default");

        text = translationProvider.getText(bundle, "UNKNOWN_HELLO", "default", null, (Object[]) null);
        assertTextEquals(text, "default");

        text = translationProvider.getText(bundle, "UNKNOWN_HELLO", null, null, (Object[]) null);
        assertTextEquals(text, null);

        text = translationProvider.getText(bundle, KEY_HELLO, "default", null, (Object[]) null);
        assertTextEquals(text, HELLO_WORLD_DEFAULT);

        text = translationProvider.getText(bundle, KEY_HELLO, "default", Locale.FRENCH, (Object[]) null);
        assertTextEquals(text, HELLO_WORLD_FR);

        text = translationProvider.getText(bundle, KEY_HELLO, "default", Locale.ENGLISH, (Object[]) null);
        assertTextEquals(text, HELLO_WORLD_EN);

        text = translationProvider.getText(bundle, KEY_BYE, "default", null, null, null);
        assertTextEquals(text, "default");

        text = translationProvider.getText(bundle, KEY_BYE, "default", new Locale("de", "AT"), (Object[]) null);
        assertTextEquals(text, BYE_DE);

        text = translationProvider.getText(bundle, KEY_BYE, "default", Locale.ENGLISH, (Object[]) null);
        assertTextEquals(text, BYE_EN);
    }

    @Test
    public void assertThatGetTextWithArgumentsViaBundleIsWorkingProperly() {
        Bundle bundle = bundleContext.getBundle();

        String text = translationProvider.getText(bundle, KEY_HELLO_SINGLE_NAME, null, null, (Object[]) NAMES);
        assertTextEquals(text, HELLO_SINGLE_NAME_DEFAULT);

        text = translationProvider.getText(bundle, KEY_HELLO_SINGLE_NAME, DEFAULT_SINGLE_NAME_TEXT, null,
                (Object[]) NAMES);
        assertTextEquals(text, HELLO_SINGLE_NAME_DEFAULT);

        text = translationProvider.getText(bundle, null, DEFAULT_SINGLE_NAME_TEXT, null, (Object[]) NAMES);
        assertTextEquals(text, "Hi openHAB!");

        text = translationProvider.getText(bundle, null, DEFAULT_SINGLE_NAME_TEXT, null, (Object[]) null);
        assertTextEquals(text, DEFAULT_SINGLE_NAME_TEXT);

        text = translationProvider.getText(bundle, null, DEFAULT_SINGLE_NAME_TEXT, null, new Object[0]);
        assertTextEquals(text, DEFAULT_SINGLE_NAME_TEXT);

        text = translationProvider.getText(bundle, KEY_HELLO_SINGLE_NAME, null, Locale.ENGLISH, (Object[]) NAMES);
        assertTextEquals(text, HELLO_SINGLE_NAME_EN);

        text = translationProvider.getText(bundle, KEY_HELLO_SINGLE_NAME, null, Locale.FRENCH, (Object[]) NAMES);
        assertTextEquals(text, HELLO_SINGLE_NAME_FR);

        text = translationProvider.getText(bundle, KEY_HELLO_MULTIPLE_NAMES, null, null, (Object[]) NAMES);
        assertTextEquals(text, HELLO_MULTIPLE_NAMES_DEFAULT);

        text = translationProvider.getText(bundle, KEY_HELLO_MULTIPLE_NAMES, null, Locale.ENGLISH, (Object[]) NAMES);
        assertTextEquals(text, HELLO_MULTIPLE_NAMES_EN);

        text = translationProvider.getText(bundle, KEY_HELLO_MULTIPLE_NAMES, null, Locale.FRANCE, (Object[]) NAMES);
        assertTextEquals(text, HELLO_MULTIPLE_NAMES_FR);

        text = translationProvider.getText(bundle, KEY_HELLO_MULTIPLE_NAMES, null, null,
                (Object[]) new String[] { "openHAB", "thing", "rule", "config" });
        assertTextEquals(text, HELLO_MULTIPLE_NAMES_DEFAULT);

        text = translationProvider.getText(bundle, null, "Hallo {2}, Hallo {1}, Hallo {0}!", null, (Object[]) NAMES);
        assertTextEquals(text, "Hallo rule, Hallo thing, Hallo openHAB!");
    }
}
