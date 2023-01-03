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

import static org.openhab.core.tools.i18n.plugin.Translations.TranslationsEntry.entry;
import static org.openhab.core.tools.i18n.plugin.Translations.TranslationsGroup.group;
import static org.openhab.core.tools.i18n.plugin.Translations.TranslationsSection.section;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.tools.i18n.plugin.Translations.TranslationsEntry;
import org.openhab.core.tools.i18n.plugin.Translations.TranslationsGroup;
import org.openhab.core.tools.i18n.plugin.Translations.TranslationsSection;

/**
 * Converts the translation key/value pairs of properties files to {@link Translations}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class PropertiesToTranslationsConverter {

    private static final String HASH = "#";

    private final Log log;

    public PropertiesToTranslationsConverter(Log log) {
        this.log = log;
    }

    public Translations convert(Path propertiesPath) {
        if (!Files.exists(propertiesPath)) {
            log.debug("Properties file '" + propertiesPath + "' does not exist");
            return Translations.translations();
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(propertiesPath);
        } catch (IOException e) {
            log.warn("Exception while converting properties to translations: " + e.getMessage());
            return Translations.translations();
        }

        Builder<TranslationsSection> sectionsBuilder = Stream.builder();
        Builder<TranslationsGroup> groupsBuilder = null;
        Builder<TranslationsEntry> entriesBuilder = null;

        boolean appendHeader = false;

        String header = "";

        for (String line : lines) {
            line = line.trim();
            if (HASH.equals(line)) {
                line = "";
            }

            if (line.startsWith(HASH)) {
                if (!appendHeader) {
                    if (groupsBuilder != null) {
                        sectionsBuilder.add(section(header, groupsBuilder.build()));
                    }
                    header = "";
                    groupsBuilder = Stream.builder();
                }

                if (line.length() > 1) {
                    if (!header.isEmpty()) {
                        header += System.lineSeparator();
                    }
                    header += line.substring(1).trim().toLowerCase();
                }
                appendHeader = true;
                continue;
            }

            appendHeader = false;

            if (!line.isBlank()) {
                int index = line.indexOf("=");
                if (index == -1) {
                    log.warn("Ignoring invalid translation key/value pair: " + line);
                } else if (!(index > 0 && line.charAt(index - 1) == '\\')) { // ignore escaped =
                    if (entriesBuilder == null) {
                        entriesBuilder = Stream.builder();
                    }
                    String key = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    entriesBuilder.add(entry(key, value));
                }
            } else if (entriesBuilder != null) {
                if (groupsBuilder == null) {
                    groupsBuilder = Stream.builder();
                }
                groupsBuilder.add(group(entriesBuilder.build()));
                entriesBuilder = null;
            }
        }

        if (entriesBuilder != null) {
            if (groupsBuilder == null) {
                groupsBuilder = Stream.builder();
            }
            groupsBuilder.add(group(entriesBuilder.build()));
        }

        if (groupsBuilder != null) {
            sectionsBuilder.add(section(header, groupsBuilder.build()));
        }

        return Translations.translations(sectionsBuilder.build());
    }
}
