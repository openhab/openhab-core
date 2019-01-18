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
package org.eclipse.smarthome.config.core.internal.i18n;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.net.URI;
import java.util.Locale;

import org.eclipse.smarthome.config.core.ParameterOption;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link I18nConfigOptionsProvider}
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
public class I18nConfigOptionsProviderTest {

    private I18nConfigOptionsProvider provider;
    private final ParameterOption expectedLangEN = new ParameterOption("en", "English");
    private final ParameterOption expectedLangFR = new ParameterOption("en", "anglais");
    private final ParameterOption expectedCntryEN = new ParameterOption("US", "United States");
    private final ParameterOption expectedCntryFRJava8 = new ParameterOption("US", "Etats-Unis");
    private final ParameterOption expectedCntryFRJava9 = new ParameterOption("US", "États-Unis");
    private URI uriI18N;

    @Before
    public void setup() throws Exception {
        provider = new I18nConfigOptionsProvider();
        uriI18N = new URI("system:i18n");
    }

    @Test
    public void testLanguage() throws Exception {
        assertTrue(provider.getParameterOptions(uriI18N, "language", Locale.US).contains(expectedLangEN));
        assertTrue(provider.getParameterOptions(uriI18N, "language", Locale.FRENCH).contains(expectedLangFR));
        assertFalse(provider.getParameterOptions(uriI18N, "language", null).isEmpty());
    }

    @Test
    public void testRegion() throws Exception {
        assertTrue(provider.getParameterOptions(uriI18N, "region", Locale.US).contains(expectedCntryEN));
        assertThat(provider.getParameterOptions(uriI18N, "region", Locale.FRENCH),
                anyOf(hasItem(expectedCntryFRJava8), hasItem(expectedCntryFRJava9)));
        assertFalse(provider.getParameterOptions(uriI18N, "region", null).isEmpty());
    }

    @Test
    public void testUnknownParameter() throws Exception {
        assertNull(provider.getParameterOptions(uriI18N, "unknown", Locale.US));
    }

}
