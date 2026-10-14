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
package org.openhab.core.io.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.i18n.LocaleProvider;

/**
 * Tests for {@link LocaleServiceImpl}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class LocaleServiceImplTest {

    private @NonNullByDefault({}) LocaleServiceImpl localeService;
    private @NonNullByDefault({}) LocaleProvider localeProviderMock;

    @BeforeEach
    public void setUp() {
        localeService = new LocaleServiceImpl();
        localeProviderMock = mock(LocaleProvider.class);
        when(localeProviderMock.getLocale()).thenReturn(Locale.ENGLISH);
        localeService.setLocaleProvider(localeProviderMock);
    }

    @Test
    public void testNullHeaderReturnsDefaultLocale() {
        assertEquals(Locale.ENGLISH, localeService.getLocale(null));
    }

    @Test
    public void testEmptyHeaderReturnsDefaultLocale() {
        assertEquals(Locale.ENGLISH, localeService.getLocale(""));
        assertEquals(Locale.ENGLISH, localeService.getLocale("   "));
    }

    @Test
    public void testSingleLanguage() {
        assertEquals(Locale.ENGLISH, localeService.getLocale("en"));
        assertEquals(Locale.GERMAN, localeService.getLocale("de"));
        assertEquals(Locale.FRENCH, localeService.getLocale("fr"));
    }

    @Test
    public void testLanguageAndRegion() {
        assertEquals(Locale.GERMANY, localeService.getLocale("de-DE"));
        assertEquals(Locale.US, localeService.getLocale("en-US"));
        assertEquals(Locale.of("de", "AT"), localeService.getLocale("de-AT"));
    }

    @Test
    public void testLanguageWithQualityValue() {
        assertEquals(Locale.GERMAN, localeService.getLocale("de;q=0.8"));
        assertEquals(Locale.GERMAN, localeService.getLocale("de;q=8"));
        assertEquals(Locale.GERMANY, localeService.getLocale("de-DE;q=0.9"));
    }

    @Test
    public void testMultipleLanguagesWithQualityValues() {
        assertEquals(Locale.GERMAN, localeService.getLocale("de;q=0.8, en-US;q=0.6, en;q=0.4"));
        assertEquals(Locale.GERMANY, localeService.getLocale("de-DE;q=0.9, en;q=0.8"));
        assertEquals(Locale.GERMAN, localeService.getLocale("de, en-US;q=0.6"));
    }

    @Test
    public void testUnderscoreRegionFormat() {
        assertEquals(Locale.GERMANY, localeService.getLocale("de_DE"));
        assertEquals(Locale.GERMANY, localeService.getLocale("de_DE;q=0.9, en;q=0.8"));
    }

    @Test
    public void testWhitespaceHandling() {
        assertEquals(Locale.GERMANY, localeService.getLocale("  de-DE ; q=0.8 , en;q=0.5 "));
    }

    @Test
    public void testWithoutLocaleProviderFallback() {
        localeService.unsetLocaleProvider(localeProviderMock);
        assertEquals(Locale.US, localeService.getLocale(null));
    }
}
