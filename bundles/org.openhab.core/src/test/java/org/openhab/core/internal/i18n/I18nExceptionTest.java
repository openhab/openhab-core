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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.i18n.AbstractI18nException;
import org.openhab.core.i18n.CommunicationException;
import org.openhab.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;

/**
 * The {@link I18nExceptionTest} tests all the functionalities of the {@link AbstractI18nException} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
public class I18nExceptionTest {

    private static final String PARAM1 = "ABC";
    private static final int PARAM2 = 50;

    private static final String MSG = "hardcoded message";

    private static final String KEY1 = "key1";
    private static final String MSG_KEY1 = "@text/" + KEY1;
    private static final String RAW_MSG_KEY1 = MSG_KEY1;
    private static final String MSG_KEY1_EN = "This is an exception.";
    private static final String MSG_KEY1_FR = "Ceci est une exception.";

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

    private static final String CAUSE = "Here is the root cause.";

    private @Nullable @Mock Bundle bundle;

    TranslationProvider i18nProvider = new TranslationProvider() {
        @Override
        public @Nullable String getText(@Nullable Bundle bundle, @Nullable String key, @Nullable String defaultText,
                @Nullable Locale locale, @Nullable Object @Nullable... arguments) {
            if (bundle != null) {
                if (KEY1.equals(key)) {
                    return Locale.FRENCH.equals(locale) ? MSG_KEY1_FR : MSG_KEY1_EN;
                } else if (KEY2.equals(key)) {
                    return Locale.FRENCH.equals(locale) ? MSG_KEY2_FR : MSG_KEY2_EN;
                } else if (KEY3.equals(key)) {
                    return Locale.FRENCH.equals(locale) ? MSG_KEY3_FR : MSG_KEY3_EN;
                }
            }
            return null;
        }

        @Override
        public @Nullable String getText(@Nullable Bundle bundle, @Nullable String key, @Nullable String defaultText,
                @Nullable Locale locale) {
            return null;
        }
    };

    @Test
    public void testMessageWithoutKey() {
        CommunicationException exception = new CommunicationException(MSG);

        assertThat(exception.getMessage(), is(MSG));
        assertThat(exception.getLocalizedMessage(), is(MSG));
        assertThat(exception.getMessage(bundle, i18nProvider), is(MSG));
        assertThat(exception.getLocalizedMessage(bundle, i18nProvider, null), is(MSG));
        assertThat(exception.getLocalizedMessage(bundle, i18nProvider, Locale.FRENCH), is(MSG));
        assertThat(exception.getRawMessage(), is(MSG));
        assertNull(exception.getCause());
    }

    @Test
    public void testMessageWithoutKeyAndWithCause() {
        Exception exception0 = new Exception(CAUSE);
        CommunicationException exception = new CommunicationException(MSG, exception0);

        assertThat(exception.getMessage(), is(MSG));
        assertThat(exception.getLocalizedMessage(), is(MSG));
        assertThat(exception.getMessage(bundle, i18nProvider), is(MSG));
        assertThat(exception.getLocalizedMessage(bundle, i18nProvider, null), is(MSG));
        assertThat(exception.getLocalizedMessage(bundle, i18nProvider, Locale.FRENCH), is(MSG));
        assertThat(exception.getRawMessage(), is(MSG));
        assertNotNull(exception.getCause());
        assertThat(exception.getCause().getMessage(), is(CAUSE));
    }

    @Test
    public void testMessageWithKeyButMissingParams() {
        CommunicationException exception = new CommunicationException(MSG_KEY1);

        assertNull(exception.getMessage());
        assertNull(exception.getLocalizedMessage());
        assertNull(exception.getMessage(bundle, null));
        assertNull(exception.getMessage(null, i18nProvider));
        assertNull(exception.getLocalizedMessage(bundle, null, Locale.FRENCH));
        assertNull(exception.getLocalizedMessage(null, i18nProvider, Locale.FRENCH));
        assertThat(exception.getRawMessage(), is(RAW_MSG_KEY1));
        assertNull(exception.getCause());
    }

    @Test
    public void testMessageWithKeyNoParam() {
        CommunicationException exception = new CommunicationException(MSG_KEY1);

        assertNull(exception.getMessage());
        assertNull(exception.getLocalizedMessage());
        assertThat(exception.getMessage(bundle, i18nProvider), is(MSG_KEY1_EN));
        assertThat(exception.getLocalizedMessage(bundle, i18nProvider, null), is(MSG_KEY1_EN));
        assertThat(exception.getLocalizedMessage(bundle, i18nProvider, Locale.FRENCH), is(MSG_KEY1_FR));
        assertThat(exception.getRawMessage(), is(RAW_MSG_KEY1));
        assertNull(exception.getCause());
    }

    @Test
    public void testMessageWithKeyTwoParams() {
        CommunicationException exception = new CommunicationException(MSG_KEY2, PARAM1, PARAM2);

        assertNull(exception.getMessage());
        assertNull(exception.getLocalizedMessage());
        assertThat(exception.getMessage(bundle, i18nProvider), is(MSG_KEY2_EN));
        assertThat(exception.getLocalizedMessage(bundle, i18nProvider, null), is(MSG_KEY2_EN));
        assertThat(exception.getLocalizedMessage(bundle, i18nProvider, Locale.FRENCH), is(MSG_KEY2_FR));
        assertThat(exception.getRawMessage(), is(RAW_MSG_KEY2));
        assertNull(exception.getCause());
    }

    @Test
    public void testMessageWithKeyOneParam() {
        CommunicationException exception = new CommunicationException(MSG_KEY3, PARAM2);

        assertNull(exception.getMessage());
        assertNull(exception.getLocalizedMessage());
        assertThat(exception.getMessage(bundle, i18nProvider), is(MSG_KEY3_EN));
        assertThat(exception.getLocalizedMessage(bundle, i18nProvider, null), is(MSG_KEY3_EN));
        assertThat(exception.getLocalizedMessage(bundle, i18nProvider, Locale.FRENCH), is(MSG_KEY3_FR));
        assertThat(exception.getRawMessage(), is(RAW_MSG_KEY3));
        assertNull(exception.getCause());
    }

    @Test
    public void testMessageWithKeyAndWithCause() {
        Exception exception0 = new Exception(CAUSE);
        CommunicationException exception = new CommunicationException(MSG_KEY1, exception0);

        assertNull(exception.getMessage());
        assertNull(exception.getLocalizedMessage());
        assertThat(exception.getMessage(bundle, i18nProvider), is(MSG_KEY1_EN));
        assertThat(exception.getLocalizedMessage(bundle, i18nProvider, null), is(MSG_KEY1_EN));
        assertThat(exception.getLocalizedMessage(bundle, i18nProvider, Locale.FRENCH), is(MSG_KEY1_FR));
        assertThat(exception.getRawMessage(), is(RAW_MSG_KEY1));
        assertNotNull(exception.getCause());
        assertThat(exception.getCause().getMessage(), is(CAUSE));
    }

    @Test
    public void testCauseOnly() {
        Exception exception0 = new Exception(CAUSE);
        CommunicationException exception = new CommunicationException(exception0);

        String expectedMsg = String.format("%s: %s", exception0.getClass().getName(), CAUSE);

        assertThat(exception.getMessage(), is(expectedMsg));
        assertThat(exception.getLocalizedMessage(), is(expectedMsg));
        assertThat(exception.getMessage(bundle, i18nProvider), is(expectedMsg));
        assertThat(exception.getLocalizedMessage(bundle, i18nProvider, null), is(expectedMsg));
        assertThat(exception.getLocalizedMessage(bundle, i18nProvider, Locale.FRENCH), is(expectedMsg));
        assertThat(exception.getRawMessage(), is(expectedMsg));
        assertNotNull(exception.getCause());
        assertThat(exception.getCause().getMessage(), is(CAUSE));
    }
}
