/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Locale;

import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.i18n.I18nException;
import org.openhab.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;

/**
 * The {@link I18nExceptionTest} tests all the functionalities of the {@link I18nException} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
public class I18nExceptionTest {

    private static final String PARAM1 = "ABC";
    private static final int PARAM2 = 50;

    private static final String MSG = "hardcoded message";

    private static final String KEY1 = "key1";
    private static final String MSG_KEY1 = "@text/" + KEY1;
    private static final String RAW_MSG_KEY1 = MSG_KEY1;
    private static final String MSG_KEY1_EN = "This is a test.";
    private static final String MSG_KEY1_FR = "Ceci est un test.";

    private static final String KEY2 = "key2";
    private static final String MSG_KEY2 = "@text/" + KEY2;
    private static final String RAW_MSG_KEY2 = String.format("@text/%s [ \"%s\", \"%d\" ]", KEY2, PARAM1, PARAM2);
    private static final String MSG_KEY2_EN = String.format("%s: value %d.", PARAM1, PARAM2);
    private static final String MSG_KEY2_FR = String.format("%s: valeur %d.", PARAM1, PARAM2);

    private static final String KEY3 = "key3";
    private static final String MSG_KEY3 = "@text/" + KEY3;
    private static final String RAW_MSG_KEY3 = String.format("@text/%s [ \"%d\" ]", KEY3, PARAM2);
    private static final String MSG_KEY3_EN = String.format("Value %d.", PARAM2);
    private static final String MSG_KEY3_FR = String.format("Valeur %d.", PARAM2);

    private @Mock Bundle bundle;

    TranslationProvider i18nProvider = new TranslationProvider() {
        @Override
        public @Nullable String getText(@Nullable Bundle bundle, @Nullable String key, @Nullable String defaultText,
                @Nullable Locale locale, @Nullable Object... arguments) {
            if (KEY1.equals(key)) {
                return Locale.FRENCH.equals(locale) ? MSG_KEY1_FR : MSG_KEY1_EN;
            } else if (KEY2.equals(key)) {
                return Locale.FRENCH.equals(locale) ? MSG_KEY2_FR : MSG_KEY2_EN;
            } else if (KEY3.equals(key)) {
                return Locale.FRENCH.equals(locale) ? MSG_KEY3_FR : MSG_KEY3_EN;
            }
            return null;
        }

        @Override
        public @Nullable String getText(@Nullable Bundle bundle, @Nullable String key, @Nullable String defaultText,
                @Nullable Locale locale) {
            return null;
        }
    };

    @BeforeEach
    public void setup() {
    }

    @Test
    public void testMessageWithoutKey() {
        I18nException exception = new I18nException(MSG);

        assertThat(exception.getMessage(), is(MSG));
        assertThat(exception.getLocalizedMessage(), is(MSG));
        assertThat(exception.getRawMessage(), is(MSG));

        exception.setupI18n(bundle, i18nProvider, Locale.FRENCH);

        assertThat(exception.getMessage(), is(MSG));
        assertThat(exception.getLocalizedMessage(), is(MSG));
        assertThat(exception.getRawMessage(), is(MSG));
    }

    @Test
    public void testMessageWithKeyNoParam() {
        I18nException exception = new I18nException(MSG_KEY1);

        assertNull(exception.getMessage());
        assertNull(exception.getLocalizedMessage());
        assertThat(exception.getRawMessage(), is(RAW_MSG_KEY1));

        exception.setupI18n(bundle, i18nProvider);

        assertThat(exception.getMessage(), is(MSG_KEY1_EN));
        assertThat(exception.getLocalizedMessage(), is(MSG_KEY1_EN));
        assertThat(exception.getRawMessage(), is(RAW_MSG_KEY1));

        exception.setupI18n(bundle, i18nProvider, Locale.FRENCH);

        assertThat(exception.getMessage(), is(MSG_KEY1_EN));
        assertThat(exception.getLocalizedMessage(), is(MSG_KEY1_FR));
        assertThat(exception.getRawMessage(), is(RAW_MSG_KEY1));
    }

    @Test
    public void testMessageWithKeyTwoParams() {
        I18nException exception = new I18nException(MSG_KEY2, PARAM1, PARAM2);

        assertNull(exception.getMessage());
        assertNull(exception.getLocalizedMessage());
        assertThat(exception.getRawMessage(), is(RAW_MSG_KEY2));

        exception.setupI18n(bundle, i18nProvider, Locale.FRENCH);

        assertThat(exception.getMessage(), is(MSG_KEY2_EN));
        assertThat(exception.getLocalizedMessage(), is(MSG_KEY2_FR));
        assertThat(exception.getRawMessage(), is(RAW_MSG_KEY2));
    }

    @Test
    public void testMessageWithKeyOneParam() {
        I18nException exception = new I18nException(MSG_KEY3, PARAM2);

        assertNull(exception.getMessage());
        assertNull(exception.getLocalizedMessage());
        assertThat(exception.getRawMessage(), is(RAW_MSG_KEY3));

        exception.setupI18n(bundle, i18nProvider, Locale.FRENCH);

        assertThat(exception.getMessage(), is(MSG_KEY3_EN));
        assertThat(exception.getLocalizedMessage(), is(MSG_KEY3_FR));
        assertThat(exception.getRawMessage(), is(RAW_MSG_KEY3));
    }
}
