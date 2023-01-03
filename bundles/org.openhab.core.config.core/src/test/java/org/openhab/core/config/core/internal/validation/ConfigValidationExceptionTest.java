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
package org.openhab.core.config.core.internal.validation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openhab.core.config.core.validation.ConfigValidationException;
import org.openhab.core.config.core.validation.ConfigValidationMessage;
import org.openhab.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;

/**
 * Testing the {@link ConfigValidationException}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@NonNullByDefault
public class ConfigValidationExceptionTest {

    private static final String PARAM1 = "param1";
    private static final String PARAM2 = "param2";

    private static final Locale DE = new Locale("de");
    private static final Locale EN = new Locale("en");

    private static final int MAX = 3;
    private static final String TXT_DE1 = "German 1";
    private static final String TXT_DE2 = MessageFormat.format("German 2 with some {0} parameter", MAX);

    private static final String TXT_EN1 = "English 1";
    private static final String TXT_EN2 = MessageFormat.format("English 2 with some {0} parameter", MAX);

    private static final String TXT_DEFAULT1 = MessageKey.PARAMETER_REQUIRED.defaultMessage;
    private static final String TXT_DEFAULT2 = MessageFormat.format(MessageKey.MAX_VALUE_TXT_VIOLATED.defaultMessage,
            MAX);

    private static final ConfigValidationMessage MSG1 = createMessage(PARAM1, TXT_DEFAULT1,
            MessageKey.PARAMETER_REQUIRED.key, List.of());
    private static final ConfigValidationMessage MSG2 = createMessage(PARAM2, TXT_DEFAULT2,
            MessageKey.MAX_VALUE_TXT_VIOLATED.key, List.of(MAX));

    private static final List<ConfigValidationMessage> ALL = List.of(MSG1, MSG2);

    private static final Bundle BUNDLE = Mockito.mock(Bundle.class);

    private static final TranslationProvider TRANSLATION_PROVIDER = new TranslationProvider() {
        @Override
        public @Nullable String getText(@Nullable Bundle bundle, @Nullable String key, @Nullable String defaultText,
                @Nullable Locale locale, @Nullable Object @Nullable... arguments) {
            return getText(bundle, key, defaultText, locale);
        }

        @Override
        public @Nullable String getText(@Nullable Bundle bundle, @Nullable String key, @Nullable String defaultText,
                @Nullable Locale locale) {
            if (MessageKey.PARAMETER_REQUIRED.key.equals(key)) {
                if (DE.equals(locale)) {
                    return TXT_DE1;
                } else {
                    return TXT_EN1;
                }
            } else if (MessageKey.MAX_VALUE_TXT_VIOLATED.key.equals(key)) {
                if (DE.equals(locale)) {
                    return TXT_DE2;
                } else {
                    return TXT_EN2;
                }
            }
            return null;
        }
    };

    @Test
    public void assertThatDefaultMessagesAreProvided() {
        ConfigValidationException configValidationException = new ConfigValidationException(BUNDLE,
                TRANSLATION_PROVIDER, ALL);

        Map<String, String> messages = configValidationException.getValidationMessages();

        assertThat(messages.size(), is(2));
        assertThat(messages.get(PARAM1), is(TXT_DEFAULT1));
        assertThat(messages.get(PARAM2), is(TXT_DEFAULT2));
    }

    @Test
    public void assertThatInternationalizedMessagesAreProvided() {
        ConfigValidationException configValidationException = new ConfigValidationException(BUNDLE,
                TRANSLATION_PROVIDER, ALL);

        Map<String, String> messages = configValidationException.getValidationMessages(DE);

        assertThat(messages.size(), is(2));
        assertThat(messages.get(PARAM1), is(TXT_DE1));
        assertThat(messages.get(PARAM2), is(TXT_DE2));

        messages = configValidationException.getValidationMessages(EN);

        assertThat(messages.size(), is(2));
        assertThat(messages.get(PARAM1), is(TXT_EN1));
        assertThat(messages.get(PARAM2), is(TXT_EN2));
    }

    @Test
    public void assertThatDefaultMessagesAreProvidedIfNoI18NproviderIsAvailable() {
        ConfigValidationException configValidationException = new ConfigValidationException(BUNDLE, null, ALL);

        Map<String, String> messages = configValidationException.getValidationMessages(DE);

        assertThat(messages.size(), is(2));
        assertThat(messages.get(PARAM1), is(TXT_DEFAULT1));
        assertThat(messages.get(PARAM2), is(TXT_DEFAULT2));

        messages = configValidationException.getValidationMessages(EN);

        assertThat(messages.size(), is(2));
        assertThat(messages.get(PARAM1), is(TXT_DEFAULT1));
        assertThat(messages.get(PARAM2), is(TXT_DEFAULT2));
    }

    @Test
    public void assertThatNPEIsThrownForNullConfigValidationMessages() {
        assertThrows(NullPointerException.class,
                () -> new ConfigValidationException(BUNDLE, TRANSLATION_PROVIDER, null));
    }

    static ConfigValidationMessage createMessage(String parameterName, String defaultMessage, String messageKey,
            Collection<Object> content) {
        return new ConfigValidationMessage(parameterName, defaultMessage, messageKey, content);
    }
}
