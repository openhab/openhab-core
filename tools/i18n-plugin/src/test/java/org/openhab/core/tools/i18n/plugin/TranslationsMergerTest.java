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
package org.openhab.core.tools.i18n.plugin;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.openhab.core.tools.i18n.plugin.Translations.TranslationsEntry.entry;
import static org.openhab.core.tools.i18n.plugin.Translations.TranslationsGroup.group;
import static org.openhab.core.tools.i18n.plugin.Translations.TranslationsSection.section;
import static org.openhab.core.tools.i18n.plugin.Translations.translations;

import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link TranslationsMerger}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class TranslationsMergerTest {

    @Test
    public void mergeEmptyTranslations() {
        Translations mainTranslations = translations();
        Translations missingTranslations = translations();

        TranslationsMerger merger = new TranslationsMerger();
        merger.merge(mainTranslations, missingTranslations);

        assertThat(mainTranslations.hasTranslations(), is(false));
        assertThat(mainTranslations.keysStream().count(), is(0L));

        assertThat(missingTranslations.hasTranslations(), is(false));
        assertThat(missingTranslations.keysStream().count(), is(0L));
    }

    @Test
    public void mergeDifferentTranslations() {
        Translations mainTranslations = Translations.translations( //
                section("main section 1", group( //
                        entry("key1", "mainValue1"), //
                        entry("key2", "mainValue2"))),
                section("main section 2", group( //
                        entry("key3", "mainValue3"), //
                        entry("key4", "mainValue4"))));

        Translations missingTranslations = Translations.translations( //
                section("missing section 1", group( //
                        entry("key1", "missingValue1"), //
                        entry("key2", "missingValue2"))),
                section("missing section 3", group( //
                        entry("key5", "missingValue5"), //
                        entry("key6", "missingValue6"))));

        TranslationsMerger merger = new TranslationsMerger();
        merger.merge(mainTranslations, missingTranslations);

        assertThat(mainTranslations.hasTranslations(), is(true));
        assertThat(mainTranslations.keysStream().count(), is(6L));
        assertThat(mainTranslations.sections.size(), is(3));

        String lines = mainTranslations.linesStream().collect(Collectors.joining(System.lineSeparator()));
        assertThat(lines, containsString("# main section 1"));
        assertThat(lines, containsString("key1 = mainValue1"));
        assertThat(lines, containsString("key2 = mainValue2"));
        assertThat(lines, containsString("# main section 2"));
        assertThat(lines, containsString("key3 = mainValue3"));
        assertThat(lines, containsString("key4 = mainValue4"));
        assertThat(lines, containsString("# missing section 3"));
        assertThat(lines, containsString("key5 = missingValue5"));
        assertThat(lines, containsString("key6 = missingValue6"));
    }
}
