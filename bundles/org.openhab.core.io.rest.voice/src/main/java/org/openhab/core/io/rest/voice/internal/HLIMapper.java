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
package org.openhab.core.io.rest.voice.internal;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.voice.text.HumanLanguageInterpreter;

/**
 * Mapper class that maps {@link HumanLanguageInterpreter} instanced to their respective DTOs.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class HLIMapper {

    /**
     * Maps a {@link HumanLanguageInterpreter} to an {@link HumanLanguageInterpreterDTO}.
     *
     * @param hli the human language interpreter
     * @param locale the locale to use for the DTO
     *
     * @return the corresponding DTO
     */
    public static HumanLanguageInterpreterDTO map(HumanLanguageInterpreter hli, Locale locale) {
        HumanLanguageInterpreterDTO dto = new HumanLanguageInterpreterDTO();
        dto.id = hli.getId();
        dto.label = hli.getLabel(locale);
        final Set<Locale> supportedLocales = hli.getSupportedLocales();
        if (supportedLocales != null) {
            dto.locales = new HashSet<>(supportedLocales.size());
            for (final Locale supportedLocale : supportedLocales) {
                dto.locales.add(supportedLocale.toString());
            }
        }
        return dto;
    }
}
