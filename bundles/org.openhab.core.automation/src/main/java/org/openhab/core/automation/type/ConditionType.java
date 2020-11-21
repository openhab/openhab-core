/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.automation.type;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Visibility;
import org.openhab.core.config.core.ConfigDescriptionParameter;

/**
 * This class provides common functionality for creating {@link Condition} instances by supplying their
 * meta-information. Each {@link ConditionType} is uniquely identifiable in scope of the {@link ModuleTypeRegistry} and
 * defines {@link ConfigDescriptionParameter}s that are meta-information for configuration of the future
 * {@link Condition} instances and meta-information for {@link Input}s used from these {@link Condition} instances.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - Initial contribution
 * @author Vasil Ilchev - Initial contribution
 */
@NonNullByDefault
public class ConditionType extends ModuleType {

    private final List<Input> inputs;

    /**
     * Creates an instance of {@link ConditionType} with base properties - UID, a {@link List} of configuration
     * descriptions and a {@link List} of {@link Input} descriptions.
     *
     * @param UID the {@link ConditionType}'s identifier, or {@code null} if a random identifier should
     *            be generated.
     * @param configDescriptions describing meta-data for the configuration of the future {@link Condition} instances.
     * @param inputs a {@link List} with {@link Input} meta-information descriptions of the future
     *            {@link Condition} instances.
     */
    public ConditionType(@Nullable String UID, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            @Nullable List<Input> inputs) {
        this(UID, configDescriptions, null, null, null, null, inputs);
    }

    /**
     * Creates an instance of {@link ConditionType} with UID, label, description, a {@link Set} of tags, visibility,
     * a {@link List} of configuration descriptions and a {@link List} of {@link Input} descriptions.
     *
     * @param UID the {@link ConditionType}'s identifier, or {@code null} if a random identifier should
     *            be generated.
     * @param configDescriptions describing meta-data for the configuration of the future {@link Condition} instances.
     * @param label a short and accurate, human-readable label of the {@link ConditionType}.
     * @param description a detailed, human-readable description of usage of {@link ConditionType} and its
     *            benefits.
     * @param tags defines categories that fit the {@link ConditionType} and which can serve as criteria
     *            for searching or filtering it.
     * @param visibility determines whether the {@link ConditionType} can be used by anyone if it is
     *            {@link Visibility#VISIBLE} or only by its creator if it is {@link Visibility#HIDDEN}.
     * @param inputs a {@link List} with {@link Input} meta-information descriptions of the future
     *            {@link Condition} instances.
     */
    public ConditionType(@Nullable String UID, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            @Nullable String label, @Nullable String description, @Nullable Set<String> tags,
            @Nullable Visibility visibility, @Nullable List<Input> inputs) {
        super(UID, configDescriptions, label, description, tags, visibility);
        this.inputs = inputs != null ? Collections.unmodifiableList(inputs) : List.of();
    }

    /**
     * Gets the meta-information descriptions of {@link Input}s defined by this {@link ConditionType}.
     *
     * @return a {@link List} of {@link Input} meta-information descriptions.
     */
    public List<Input> getInputs() {
        return inputs;
    }
}
