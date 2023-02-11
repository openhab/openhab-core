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

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.tools.i18n.plugin.Translations.TranslationsEntry;
import org.openhab.core.tools.i18n.plugin.Translations.TranslationsGroup;
import org.openhab.core.tools.i18n.plugin.Translations.TranslationsSection;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Converts JSON based {@link BundleInfo} to {@link Translations}.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class JsonToTranslationsConverter {

    private static final Pattern OPTION_ESCAPE_PATTERN = Pattern.compile("([ :=])");

    public Translations convert(BundleInfo bundleInfo) {
        return new Translations(Stream.of( //
                moduleTypeSection(bundleInfo), ruleTemplateSection(bundleInfo)//
        ).flatMap(Function.identity()).collect(Collectors.toList()));
    }

    private Stream<TranslationsSection> moduleTypeSection(BundleInfo bundleInfo) {
        Builder<TranslationsSection> sectionBuilder = Stream.builder();

        bundleInfo.getModuleTypesJson().stream().flatMap(this::getModuleType).forEach(sectionBuilder::add);
        return sectionBuilder.build().sorted(Comparator.comparing(s -> s.header));
    }

    private Stream<TranslationsSection> ruleTemplateSection(BundleInfo bundleInfo) {
        Builder<TranslationsSection> sectionBuilder = Stream.builder();

        bundleInfo.getRuleTemplateJson().stream().flatMap(this::getRuleTemplate).forEach(sectionBuilder::add);
        return sectionBuilder.build().sorted(Comparator.comparing(s -> s.header));
    }

    private Stream<TranslationsSection> getModuleType(JsonObject moduleType) {
        String uid = getAsString(moduleType, "uid").orElse("");
        if (uid.isBlank()) {
            return Stream.of();
        }

        String globalPrefix = "module-type." + uid + ".";
        Builder<TranslationsGroup> groupBuilder = Stream.builder();

        // global entries
        Builder<TranslationsEntry> entriesBuilder = Stream.builder();
        getAsString(moduleType, "label").ifPresent(label -> entriesBuilder.add(entry(globalPrefix + "label", label)));
        getAsString(moduleType, "description")
                .ifPresent(description -> entriesBuilder.add(entry(globalPrefix + "description", description)));
        groupBuilder.add(group(entriesBuilder.build()));

        // configDescriptions
        JsonElement configDescriptionsElement = moduleType.get("configDescriptions");
        if (configDescriptionsElement != null) {
            String prefix = globalPrefix + "config.";
            groupBuilder.add(getGroupFromArray(configDescriptionsElement, prefix));
        }

        // inputs
        JsonElement inputsElement = moduleType.get("inputs");
        if (inputsElement != null) {
            String prefix = globalPrefix + "input.";
            groupBuilder.add(getGroupFromArray(inputsElement, prefix));
        }

        // outputs
        JsonElement outputsElement = moduleType.get("outputs");
        if (outputsElement != null) {
            String prefix = globalPrefix + "output.";
            groupBuilder.add(getGroupFromArray(outputsElement, prefix));
        }

        return Stream.of(new TranslationsSection(uid, groupBuilder.build().collect(Collectors.toList())));
    }

    private Stream<TranslationsSection> getRuleTemplate(JsonObject ruleTemplate) {
        String uid = getAsString(ruleTemplate, "uid").orElse("");
        if (uid.isBlank()) {
            return Stream.of();
        }

        String globalPrefix = "rule-template." + uid + ".";
        Builder<TranslationsGroup> groupBuilder = Stream.builder();

        // global entries
        Builder<TranslationsEntry> entriesBuilder = Stream.builder();
        getAsString(ruleTemplate, "label").ifPresent(label -> entriesBuilder.add(entry(globalPrefix + "label", label)));
        getAsString(ruleTemplate, "description")
                .ifPresent(description -> entriesBuilder.add(entry(globalPrefix + "description", description)));
        groupBuilder.add(group(entriesBuilder.build()));

        return Stream.of(new TranslationsSection(uid, groupBuilder.build().collect(Collectors.toList())));
    }

    private TranslationsGroup getGroupFromArray(JsonElement parentElement, String prefix) {
        Builder<TranslationsEntry> entriesBuilder = Stream.builder();
        for (JsonElement jsonElement : parentElement.getAsJsonArray()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            getAsString(jsonObject, "name").ifPresent(name -> {
                String namePrefix = prefix + name + ".";
                getAsString(jsonObject, "label")
                        .ifPresent(label -> entriesBuilder.add(entry(namePrefix + "label", label)));
                getAsString(jsonObject, "description")
                        .ifPresent(description -> entriesBuilder.add(entry(namePrefix + "description", description)));
                JsonElement optionsElement = jsonObject.get("options");
                if (optionsElement != null) {
                    for (JsonElement optionElement : optionsElement.getAsJsonArray()) {
                        JsonObject optionObject = optionElement.getAsJsonObject();
                        getAsString(optionObject, "value")
                                .ifPresent(value -> getAsString(optionObject, "label").ifPresent(label -> {
                                    String optionKey = namePrefix + "option."
                                            + OPTION_ESCAPE_PATTERN.matcher(value).replaceAll("\\\\$1");
                                    entriesBuilder.add(entry(optionKey, label));
                                }));
                    }
                }
            });
        }
        return group(entriesBuilder.build());
    }

    private Optional<String> getAsString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null ? Optional.empty() : Optional.of(element.getAsString());
    }
}
