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

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link Translations} of a bundle consisting of {@link TranslationsSection}s having {@link TranslationsGroup}s of
 * {@link TranslationsEntry}s (key/value pairs).
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class Translations {

    public static class TranslationsEntry {
        public final String key;
        final String value;

        public TranslationsEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public boolean hasTranslation() {
            return !value.isBlank() && !value.startsWith("@text/");
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public static TranslationsEntry entry(String key, @Nullable String value) {
            return new TranslationsEntry(key, value == null ? "" : value);
        }
    }

    public static class TranslationsGroup implements Comparable<TranslationsGroup> {
        final List<TranslationsEntry> entries;

        public TranslationsGroup(List<TranslationsEntry> entries) {
            this.entries = entries;
        }

        @Override
        public int compareTo(TranslationsGroup other) {
            if (entries.isEmpty()) {
                return -1;
            } else if (other.entries.isEmpty()) {
                return 1;
            }

            return entries.get(0).getKey().compareTo(other.entries.get(0).getKey());
        }

        public boolean hasTranslations() {
            return !entries.isEmpty() && entries.stream().anyMatch(TranslationsEntry::hasTranslation);
        }

        public Stream<String> keysStream() {
            return entries.stream() //
                    .filter(TranslationsEntry::hasTranslation) //
                    .map(TranslationsEntry::getKey);
        }

        public Stream<String> linesStream() {
            return entries.stream() //
                    .filter(TranslationsEntry::hasTranslation) //
                    .map(entry -> String.format("%s = %s", entry.key, entry.value));
        }

        public void removeEntries(Predicate<? super TranslationsEntry> filter) {
            entries.removeIf(filter);
        }

        public static TranslationsGroup group(Stream<TranslationsEntry> entries) {
            return new TranslationsGroup(entries.collect(Collectors.toList()));
        }

        public static TranslationsGroup group(TranslationsEntry... entries) {
            return group(Arrays.stream(entries));
        }
    }

    public static class TranslationsSection {
        final String header;
        final List<TranslationsGroup> groups;

        public TranslationsSection(List<TranslationsGroup> groups) {
            this("", groups);
        }

        public TranslationsSection(String header, List<TranslationsGroup> groups) {
            this.header = header;
            this.groups = groups;
        }

        public boolean hasTranslations() {
            return groups.stream().anyMatch(TranslationsGroup::hasTranslations);
        }

        public Stream<String> keysStream() {
            return groups.stream() //
                    .map(TranslationsGroup::keysStream) //
                    .flatMap(Function.identity());
        }

        public Stream<String> linesStream() {
            Builder<String> builder = Stream.builder();
            if (!header.isBlank()) {
                Arrays.stream(header.split(System.lineSeparator())) //
                        .map(line -> "# " + line) //
                        .forEach(builder::add);
                builder.add("");
            }
            groups.stream() //
                    .filter(TranslationsGroup::hasTranslations) //
                    .map(TranslationsGroup::linesStream) //
                    .flatMap(Function.identity()) //
                    .forEach(builder::add);
            builder.add("");
            return builder.build();
        }

        public void removeEntries(Predicate<? super TranslationsEntry> filter) {
            groups.forEach(group -> group.removeEntries(filter));
        }

        public static TranslationsSection section(Stream<TranslationsGroup> groups) {
            return section("", groups);
        }

        public static TranslationsSection section(String header, Stream<TranslationsGroup> groups) {
            return new TranslationsSection(header, groups.sorted().collect(Collectors.toList()));
        }

        public static TranslationsSection section(TranslationsGroup... groups) {
            return section("", groups);
        }

        public static TranslationsSection section(String header, TranslationsGroup... groups) {
            return section(header, Arrays.stream(groups));
        }
    }

    final List<TranslationsSection> sections;

    public Translations(List<TranslationsSection> sections) {
        this.sections = sections;
    }

    boolean hasTranslations() {
        return sections.stream().anyMatch(TranslationsSection::hasTranslations);
    }

    public void addSection(TranslationsSection section) {
        sections.add(section);
    }

    public Stream<String> keysStream() {
        return sections.stream() //
                .map(TranslationsSection::keysStream) //
                .flatMap(Function.identity());
    }

    public Stream<String> linesStream() {
        return sections.stream() //
                .filter(TranslationsSection::hasTranslations) //
                .map(TranslationsSection::linesStream) //
                .flatMap(Function.identity());
    }

    public void removeEntries(Predicate<? super TranslationsEntry> filter) {
        sections.forEach(section -> section.removeEntries(filter));
    }

    static Translations translations(Stream<TranslationsSection> sections) {
        return new Translations(sections.collect(Collectors.toList()));
    }

    static Translations translations(TranslationsSection... sections) {
        return translations(Arrays.stream(sections));
    }
}
