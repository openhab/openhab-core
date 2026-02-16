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
import org.openhab.core.converter.ObjectSerializer;
import org.openhab.core.converter.SerializabilityResult;
import org.openhab.core.io.dto.SerializationException;

/**
 * {@link RuleSerializer} is the interface to implement by any file generator for {@link Rule} object.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public interface RuleSerializer extends ObjectSerializer<Rule> {

    /**
     * Checks if the specified rules are serializable with this {@link RuleSerializer}. Returned results are in the same
     * order as the specified rules.
     *
     * @param rules the {@link List} of {@link Rule}s to check.
     * @return The resulting {@link List} of {@link SerializabilityResult}s.
     */
    List<SerializabilityResult<String>> checkSerializability(Collection<Rule> rules);

    /**
     * Specify the {@link List} of {@link Rule}s to be serialized and associate them with an identifier.
     *
     * @param id the identifier of the {@link Rule} format generation.
     * @param rules the {@link List} of {@link Rule}s to serialize.
     * @param the option that determines how to serialize the {@link Rule}s.
     * @throws SerializationException If one or more of the rules can't be serialized.
     */
    void setRulesToBeSerialized(String id, List<Rule> rules, RuleSerializationOption option)
            throws SerializationException;

    /**
     * An enum representing the different rule serialization options.
     */
    public enum RuleSerializationOption {

        /** Empty collections and normally irrelevant fields are hidden */
        NORMAL("Normal"),

        /** Everything is serialized, including empty collections */
        INCLUDE_ALL("Include all"),

        /** Only the fields required in a rule stub to be used with a template is serialized */
        STUB_ONLY("Stub only"),

        /** Template information is stripped, otherwise like {@link #NORMAL} */
        STRIP_TEMPLATE("Strip template");

        private final String friendlyName;

        private RuleSerializationOption(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        @Override
        public String toString() {
            return friendlyName;
        }

        public static @Nullable RuleSerializationOption fromString(@Nullable String id) {
            if (id == null || id.isBlank()) {
                return null;
            }
            String upId = id.toUpperCase(Locale.ROOT).trim();
            for (RuleSerializationOption option : values()) {
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
