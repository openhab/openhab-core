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
package org.openhab.core.config.core.internal.i18n;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URI;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.ParameterOption;

/**
 * Tests for {@link I18nConfigOptionsProvider}
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public class I18nConfigOptionsProviderTest {

    private final I18nConfigOptionsProvider provider = new I18nConfigOptionsProvider();
    private final URI uriI18N = URI.create("system:i18n");

    private final ParameterOption empty = new ParameterOption("", "");
    private final ParameterOption expectedLangEN = new ParameterOption("en", "English");
    private final ParameterOption expectedLangFR = new ParameterOption("en", "anglais");
    private final ParameterOption expectedCntryEN = new ParameterOption("US", "United States");
    private final ParameterOption expectedCntryFRJava8 = new ParameterOption("US", "Etats-Unis");
    private final ParameterOption expectedCntryFRJava9 = new ParameterOption("US", "États-Unis");

    @Test
    public void testLanguage() {
        assertThat(provider.getParameterOptions(uriI18N, "language", null, Locale.US), hasItem(expectedLangEN));
        assertThat(provider.getParameterOptions(uriI18N, "language", null, Locale.US), not(hasItem(empty)));

        assertThat(provider.getParameterOptions(uriI18N, "language", null, Locale.FRENCH), hasItem(expectedLangFR));
        assertThat(provider.getParameterOptions(uriI18N, "language", null, Locale.FRENCH), not(hasItem(empty)));

        assertThat(provider.getParameterOptions(uriI18N, "language", null, null), not(IsEmptyCollection.empty()));
    }

    @Test
    public void testRegion() {
        assertThat(provider.getParameterOptions(uriI18N, "region", null, Locale.US), hasItem(expectedCntryEN));
        assertThat(provider.getParameterOptions(uriI18N, "region", null, Locale.US), not(hasItem(empty)));

        assertThat(provider.getParameterOptions(uriI18N, "region", null, Locale.FRENCH),
                anyOf(hasItem(expectedCntryFRJava8), hasItem(expectedCntryFRJava9)));
        assertThat(provider.getParameterOptions(uriI18N, "region", null, Locale.FRENCH), not(hasItem(empty)));

        assertThat(provider.getParameterOptions(uriI18N, "region", null, null), not(IsEmptyCollection.empty()));
    }

    @Test
    public void testUnknownParameter() {
        assertThat(provider.getParameterOptions(uriI18N, "unknown", null, Locale.US), nullValue());
    }
}
