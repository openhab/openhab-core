/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.voice.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Unit tests for {@link VoiceI18nProvider}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class VoiceI18nProviderTest {

    private final TranslationProvider translationProvider = mock(TranslationProvider.class);
    private final BundleContext bundleContext = mock(BundleContext.class);
    private final Bundle bundle = mock(Bundle.class);

    private VoiceI18nProvider provider = new VoiceI18nProvider(translationProvider, bundleContext);

    @BeforeEach
    public void setUp() {
        when(bundleContext.getBundle()).thenReturn(bundle);
        provider = new VoiceI18nProvider(translationProvider, bundleContext);
    }

    @Test
    public void getTextDelegatesToTranslationProvider() {
        when(translationProvider.getText(bundle, "test.key", null, Locale.ENGLISH)).thenReturn("Translated Text");

        String result = provider.getText("test.key", Locale.ENGLISH);

        assertEquals("Translated Text", result);
        verify(translationProvider).getText(bundle, "test.key", null, Locale.ENGLISH);
    }

    @Test
    public void getTextWithDefaultDelegatesToTranslationProvider() {
        when(translationProvider.getText(bundle, "test.key", "Default Text", Locale.ENGLISH))
                .thenReturn("Default Text");

        String result = provider.getText("test.key", "Default Text", Locale.ENGLISH);

        assertEquals("Default Text", result);
        verify(translationProvider).getText(bundle, "test.key", "Default Text", Locale.ENGLISH);
    }

    @Test
    public void getFormattedTextDelegatesToTranslationProviderWithArguments() {
        when(translationProvider.getText(eq(bundle), eq("error.ks-error"), isNull(), eq(Locale.ENGLISH),
                eq(new Object[] { "Device timeout" })))
                .thenReturn("Encountered error while spotting keywords, Device timeout");

        String result = provider.getFormattedText("error.ks-error", Locale.ENGLISH, "Device timeout");

        assertEquals("Encountered error while spotting keywords, Device timeout", result);
    }

    @Test
    public void getFormattedTextFallsBackToFirstArgumentWhenKeyNotFound() {
        when(translationProvider.getText(any(), eq("missing.key"), isNull(), any(), any())).thenReturn(null);

        String result = provider.getFormattedText("missing.key", Locale.ENGLISH, "Raw error message", "Second arg");

        assertEquals("Raw error message", result);
    }

    @Test
    public void getFormattedTextReturnsNullWhenKeyNotFoundAndNoArguments() {
        when(translationProvider.getText(any(), eq("missing.key"), isNull(), any(), any())).thenReturn(null);

        String result = provider.getFormattedText("missing.key", Locale.ENGLISH);

        assertNull(result);
    }

    @Test
    public void getTextReturnsDefaultWhenBundleIsNull() {
        BundleContext emptyContext = mock(BundleContext.class);
        when(emptyContext.getBundle()).thenReturn(null);
        VoiceI18nProvider providerWithoutBundle = new VoiceI18nProvider(translationProvider, emptyContext);

        assertEquals("Fallback", providerWithoutBundle.getText("any.key", "Fallback", Locale.ENGLISH));
        assertEquals("ArgFallback", providerWithoutBundle.getFormattedText("any.key", Locale.ENGLISH, "ArgFallback"));
        assertNull(providerWithoutBundle.getFormattedText("any.key", Locale.ENGLISH));
    }
}
