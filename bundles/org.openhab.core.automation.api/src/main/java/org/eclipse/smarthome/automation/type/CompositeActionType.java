/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.automation.type;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Visibility;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;

/**
 * This class is as {@link ActionType} which logically combines {@link Action} instances. The composite action hides
 * internal logic and inner connections between participating {@link Action}s and it can be used as a regular
 * {@link Action} module.
 *
 * @author Yordan Mihaylov - Initial Contribution
 * @author Ana Dimova - Initial Contribution
 * @author Vasil Ilchev - Initial Contribution
 */
@NonNullByDefault
public class CompositeActionType extends ActionType {

    private final List<Action> children;

    /**
     * Creates an instance of {@code CompositeActionType} with list of {@link Action}s. It initializes only base
     * properties of the {@code CompositeActionType}.
     *
     * @param UID                the {@link ActionType}'s identifier, or {@code null} if a random identifier should be
     *                           generated.
     * @param configDescriptions describing meta-data for the configuration of the future {@link Action} instances.
     * @param children           is a {@link List} of {@link Action}s.
     * @param inputs             a {@link List} with {@link Input} meta-information descriptions of the future
     *                           {@link Action} instances.
     * @param outputs            a {@link List} with {@link Output} meta-information descriptions of the future
     *                           {@link Action} instances.
     * @param children           is a {@link List} of {@link Action}s.
     */
    public CompositeActionType(@Nullable String UID, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            @Nullable List<Input> inputs, @Nullable List<Output> outputs, @Nullable List<Action> children) {
        super(UID, configDescriptions, inputs, outputs);
        this.children = children != null ? Collections.unmodifiableList(children) : Collections.emptyList();
    }

    /**
     * Creates an instance of {@code CompositeActionType} with list of {@link Action}s. It initializes all properties of
     * the {@code CompositeActionType}.
     *
     * @param UID                the {@link ActionType}'s identifier, or {@code null} if a random identifier should be
     *                           generated.
     * @param configDescriptions describing meta-data for the configuration of the future {@link Action} instances.
     * @param label              a short and accurate, human-readable label of the {@link ActionType}.
     * @param description        a detailed, human-readable description of usage of {@link ActionType} and its benefits.
     * @param tags               defines categories that fit the {@link ActionType} and which can serve as criteria for
     *                           searching or filtering it.
     * @param visibility         determines whether the {@link ActionType} can be used by anyone if it is
     *                           {@link Visibility#VISIBLE} or only by its creator if it is {@link Visibility#HIDDEN}.
     * @param inputs             a {@link List} with {@link Input} meta-information descriptions of the future
     *                           {@link Action} instances.
     * @param outputs            a {@link List} with {@link Output} meta-information descriptions of the future
     *                           {@link Action} instances.
     * @param children           is a {@link List} of {@link Action}s.
     */
    public CompositeActionType(@Nullable String UID, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            @Nullable String label, @Nullable String description, @Nullable Set<String> tags,
            @Nullable Visibility visibility, @Nullable List<Input> inputs, @Nullable List<Output> outputs,
            @Nullable List<Action> children) {
        super(UID, configDescriptions, label, description, tags, visibility, inputs, outputs);
        this.children = children != null ? Collections.unmodifiableList(children) : Collections.emptyList();
    }

    /**
     * Gets the {@link Action} modules of the {@code CompositeActionType}.
     *
     * @return a {@link List} of the {@link Action} modules of this {@code CompositeActionType}.
     */
    public List<Action> getChildren() {
        return children;
    }

}
