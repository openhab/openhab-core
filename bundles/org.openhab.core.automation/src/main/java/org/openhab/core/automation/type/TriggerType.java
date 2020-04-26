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
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.Visibility;
import org.openhab.core.config.core.ConfigDescriptionParameter;

/**
 * This class provides common functionality for creating {@link Trigger} instances by supplying their meta-information.
 * Each {@link TriggerType} is uniquely identifiable in scope of the {@link ModuleTypeRegistry} and defines
 * {@link ConfigDescriptionParameter}s that are meta-information for configuration of the future {@link Trigger}
 * instances and meta-information for {@link Output}s used from these {@link Trigger} instances.
 *
 * @author Yordan Mihaylov - Initial contribution
 */
@NonNullByDefault
public class TriggerType extends ModuleType {

    private final List<Output> outputs;

    /**
     * Creates an instance of {@link TriggerType} with base properties - UID, a {@link List} of configuration
     * descriptions and a {@link List} of {@link Output} descriptions.
     *
     * @param UID the {@link TriggerType}'s identifier, or {@code null} if a random identifier should be
     *            generated.
     * @param configDescriptions describing meta-data for the configuration of the future {@link Trigger} instances.
     * @param outputs a {@link List} with {@link Output} meta-information descriptions of the future
     *            {@link Trigger} instances.
     */
    public TriggerType(@Nullable String UID, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            @Nullable List<Output> outputs) {
        super(UID, configDescriptions);
        this.outputs = outputs != null ? Collections.unmodifiableList(outputs) : Collections.emptyList();
    }

    /**
     * Creates an instance of {@link TriggerType} with UID, label, description, a {@link Set} of tags, visibility,
     * a {@link List} of configuration descriptions and a {@link List} of {@link Output} descriptions.
     *
     * @param UID the {@link TriggerType}'s identifier, or {@code null} if a random identifier should be
     *            generated.
     * @param configDescriptions describing meta-data for the configuration of the future {@link Trigger} instances.
     * @param label a short and accurate, human-readable label of the {@link TriggerType}.
     * @param description a detailed, human-readable description of usage of {@link TriggerType} and its
     *            benefits.
     * @param tags defines categories that fit the {@link TriggerType} and which can serve as criteria for
     *            searching or filtering it.
     * @param visibility determines whether the {@link TriggerType} can be used by anyone if it is
     *            {@link Visibility#VISIBLE} or only by its creator if it is {@link Visibility#HIDDEN}.
     *            If {@code null} is provided the default visibility {@link Visibility#VISIBLE} will be
     *            used.
     * @param outputs a {@link List} with {@link Output} meta-information descriptions of the future
     *            {@link Trigger} instances.
     */
    public TriggerType(@Nullable String UID, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            @Nullable String label, @Nullable String description, @Nullable Set<String> tags,
            @Nullable Visibility visibility, @Nullable List<Output> outputs) {
        super(UID, configDescriptions, label, description, tags, visibility);
        this.outputs = outputs != null ? Collections.unmodifiableList(outputs) : Collections.emptyList();
    }

    /**
     * Gets the meta-information descriptions of {@link Output}s defined by this type.<br>
     *
     * @return a {@link List} of {@link Output} definitions.
     */
    public List<Output> getOutputs() {
        return outputs;
    }
}
