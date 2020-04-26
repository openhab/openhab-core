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
 * This class is as {@link ConditionType} which logically combines {@link Condition} modules. The composite condition
 * hides internal logic between participating conditions and it can be used as a regular {@link Condition} module.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - Initial contribution
 * @author Vasil Ilchev - Initial contribution
 */
@NonNullByDefault
public class CompositeConditionType extends ConditionType {

    private final List<Condition> children;

    /**
     * Creates an instance of {@code CompositeConditionType} with ordered set of {@link Condition}s. It initializes
     * only base properties of the {@code CompositeConditionType}.
     *
     * @param UID is the {@link ConditionType}'s identifier, or {@code null} if a random identifier
     *            should be generated.
     * @param configDescriptions is a {@link List} of configuration descriptions describing meta-data for the
     *            configuration of the future {@link Condition} instances.
     * @param inputs is a {@link List} with {@link Input}'s meta-information descriptions of the future
     *            {@link Condition} instances.
     * @param children is a {@link List} of {@link Condition}s.
     */
    public CompositeConditionType(@Nullable String UID, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            @Nullable List<Input> inputs, @Nullable List<Condition> children) {
        super(UID, configDescriptions, inputs);
        this.children = children != null ? Collections.unmodifiableList(children) : Collections.emptyList();
    }

    /**
     * Creates an instance of {@code CompositeConditionType} with ordered set of {@link Condition}s. It initializes
     * all properties of the {@code CompositeConditionType}.
     *
     * @param UID is the {@link ConditionType}'s identifier, or {@code null} if a random identifier
     *            should be generated.
     * @param configDescriptions is a {@link List} of configuration descriptions describing meta-data for the
     *            configuration of the future {@link Condition} instances.
     * @param label a short and accurate, human-readable label of the {@code CompositeConditionType}.
     * @param description a detailed, human-readable description of usage of {@code CompositeConditionType} and
     *            its benefits.
     * @param tags defines categories that fit the {@code CompositeConditionType} and which can serve as
     *            criteria for searching or filtering it.
     * @param visibility determines whether the {@code CompositeConditionType} can be used by anyone if it is
     *            {@link Visibility#VISIBLE} or only by its creator if it is {@link Visibility#HIDDEN}.
     *            If {@code null} is provided the default visibility {@link Visibility#VISIBLE} will be
     *            used.
     * @param inputs is a {@link List} with {@link Input}'s meta-information descriptions of the future
     *            {@link Condition} instances.
     * @param children is a {@link List} of {@link Condition}s.
     */
    public CompositeConditionType(@Nullable String UID, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            @Nullable String label, @Nullable String description, @Nullable Set<String> tags,
            @Nullable Visibility visibility, @Nullable List<Input> inputs, @Nullable List<Condition> children) {
        super(UID, configDescriptions, label, description, tags, visibility, inputs);
        this.children = children != null ? Collections.unmodifiableList(children) : Collections.emptyList();
    }

    /**
     * Gets the {@link Condition} modules of the {@code CompositeConditionType}.
     *
     * @return a {@link List} of the {@link Condition} modules of this {@code CompositeConditionType}.
     */
    public List<Condition> getChildren() {
        return children;
    }
}
