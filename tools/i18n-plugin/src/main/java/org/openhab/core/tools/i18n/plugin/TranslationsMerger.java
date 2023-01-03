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

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.tools.i18n.plugin.Translations.TranslationsSection;

/**
 * Merges multiple {@link Translations} into one.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class TranslationsMerger {

    /**
     * Adds any missing translations from <code>missingTranslations</code> to <code>mainTranslations</code>.
     */
    public void merge(Translations mainTranslations, Translations missingTranslations) {
        Set<String> mainEntryKeys = mainTranslations.keysStream().collect(Collectors.toSet());
        missingTranslations.removeEntries(entry -> mainEntryKeys.contains(entry.key));
        missingTranslations.sections.stream() //
                .filter(TranslationsSection::hasTranslations) //
                .forEach(mainTranslations::addSection);
    }
}
