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
package org.openhab.core.automation.converter;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.converter.RuleSerializer.RuleSerializationOption;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.converter.ObjectSerializer;
import org.openhab.core.converter.SerializabilityResult;
import org.openhab.core.io.dto.SerializationException;

/**
 * {@link RuleTemplateSerializer} is the interface to implement by any file generator for {@link Rule} object.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public interface RuleTemplateSerializer extends ObjectSerializer<RuleTemplate> {

    /**
     * Checks if the specified rule templates are serializable with this {@link RuleTemplateSerializer}. Returned
     * results are in the same order as the specified rule templates.
     *
     * @param templates the {@link List} of {@link RuleTemplate}s to check.
     * @return The resulting {@link List} of {@link SerializabilityResult}s.
     */
    List<SerializabilityResult<String>> checkSerializability(Collection<RuleTemplate> templates);

    /**
     * Specify the {@link List} of {@link RuleTemplate}s to be serialized and associate them with an identifier.
     *
     * @param id the identifier of the {@link RuleTemplate} format generation.
     * @param templates the {@link List} of {@link RuleTemplate}s to serialize.
     * @param option the option that determines how to serialize the {@link RuleTemplate}s.
     * @throws SerializationException If one or more of the rule templates can't be serialized.
     */
    void setTemplatesToBeSerialized(String id, List<RuleTemplate> templates, RuleTemplateSerializationOption option)
            throws SerializationException;

    /**
     * An enum representing the different rule template serialization options.
     */
    public enum RuleTemplateSerializationOption {

        /** Empty collections and normally irrelevant fields are hidden */
        NORMAL("Normal"),

        /** Everything is serialized, including empty collections */
        INCLUDE_ALL("Include all");

        private final String friendlyName;

        private RuleTemplateSerializationOption(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        public RuleSerializationOption toRuleSerializationOption() {
            switch (this) {
                case INCLUDE_ALL:
                    return RuleSerializationOption.INCLUDE_ALL;
                case NORMAL:
                    return RuleSerializationOption.NORMAL;
                default:
                    throw new UnsupportedOperationException(
                            "Missing toRuleSerializationOption() implementation for " + name());
            }
        }

        @Override
        public String toString() {
            return friendlyName;
        }

        public static @Nullable RuleTemplateSerializationOption fromString(@Nullable String id) {
            if (id == null || id.isBlank()) {
                return null;
            }
            String upId = id.toUpperCase(Locale.ROOT).trim();
            for (RuleTemplateSerializationOption option : values()) {
                if (upId.equals(option.name()) || upId.equalsIgnoreCase(option.friendlyName)
                        || upId.equalsIgnoreCase(option.friendlyName.replace(" ", ""))
                        || upId.equalsIgnoreCase(option.friendlyName.replace(" ", "-"))) {
                    return option;
                }
            }
            return null;
        }
    }
}
