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

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Visibility;
import org.openhab.core.config.core.ConfigDescriptionParameter;

/**
 * This class provides common functionality for creating {@link Action} instances by supplying their meta-information.
 * Each {@link ActionType} is uniquely identifiable in scope of the {@link ModuleTypeRegistry} and defines
 * {@link ConfigDescriptionParameter}s that are meta-information for configuration of the future {@link Action}
 * instances and meta-information for {@link Input}s and {@link Output}s used from these {@link Action} instances.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - Initial contribution
 * @author Vasil Ilchev - Initial contribution
 */
public class ActionType extends ModuleType {

    /**
     * Contains meta-information describing the incoming connections of the {@link Action} module to the other
     * {@link Module}s.
     */
    private final List<Input> inputs;

    /**
     * Contains meta-information describing the outgoing connections of the {@link Action} module to the other
     * {@link Action}s.
     */
    private final List<Output> outputs;

    /**
     * Creates an instance of {@link ActionType} with base properties - UID, a {@link List} of configuration
     * descriptions and a {@link List} of {@link Input} definitions.
     *
     * @param UID the {@link ActionType}'s identifier, or {@code null} if a random identifier should be
     *            generated.
     * @param configDescriptions describing meta-data for the configuration of the future {@link Action} instances.
     * @param inputs a {@link List} with {@link Input} meta-information descriptions of the future
     *            {@link Action} instances.
     */
    public ActionType(@Nullable String UID, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            List<Input> inputs) {
        this(UID, configDescriptions, inputs, null);
    }

    /**
     * Creates an instance of the {@link ActionType} with UID, a {@link List} of configuration descriptions,
     * a {@link List} of {@link Input} definitions and a {@link List} of {@link Output} descriptions.
     *
     * @param UID the {@link ActionType}'s identifier, or {@code null} if a random identifier should be
     *            generated.
     * @param configDescriptions describing meta-data for the configuration of the future {@link Action} instances.
     * @param inputs a {@link List} with {@link Input} meta-information descriptions of the future
     *            {@link Action} instances.
     * @param outputs a {@link List} with {@link Output} meta-information descriptions of the future
     *            {@link Action} instances.
     */
    public ActionType(@Nullable String UID, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            @Nullable List<Input> inputs, @Nullable List<Output> outputs) {
        this(UID, configDescriptions, null, null, null, null, inputs, outputs);
    }

    /**
     * Creates an instance of {@link ActionType} with UID, label, description, a {@link Set} of tags, visibility,
     * a {@link List} of configuration descriptions, a {@link List} of {@link Input} descriptions and a {@link List}
     * of {@link Output} descriptions.
     *
     * @param UID the {@link ActionType}'s identifier, or {@code null} if a random identifier should be
     *            generated.
     * @param configDescriptions describing meta-data for the configuration of the future {@link Action} instances.
     * @param label is a short and accurate name of the {@link ActionType}.
     * @param description is a short and understandable description of which can be used the {@link ActionType}.
     * @param tags defines categories that fit the {@link ActionType} and which can serve as criteria for
     *            searching or filtering it.
     * @param visibility determines whether the {@link ActionType} can be used by anyone if it is
     *            {@link Visibility#VISIBLE} or only by its creator if it is {@link Visibility#HIDDEN}.
     * @param inputs a {@link List} with {@link Input} meta-information descriptions of the future
     *            {@link Action} instances.
     * @param outputs a {@link List} with {@link Output} meta-information descriptions of the future
     *            {@link Action} instances.
     */
    public ActionType(@Nullable String UID, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            @Nullable String label, @Nullable String description, @Nullable Set<String> tags,
            @Nullable Visibility visibility, @Nullable List<Input> inputs, @Nullable List<Output> outputs) {
        super(UID, configDescriptions, label, description, tags, visibility);
        this.inputs = inputs != null ? Collections.unmodifiableList(inputs) : Collections.emptyList();
        this.outputs = outputs != null ? Collections.unmodifiableList(outputs) : Collections.emptyList();
    }

    /**
     * Gets the meta-information descriptions of {@link Input}s defined by this type.
     *
     * @return a {@link List} with {@link Input} definitions.
     */
    public List<Input> getInputs() {
        return inputs;
    }

    /**
     * Gets the meta-information descriptions of {@link Output}s defined by this type.
     *
     * @return a {@link List} with {@link Output} definitions.
     */
    public List<Output> getOutputs() {
        return outputs;
    }

}
