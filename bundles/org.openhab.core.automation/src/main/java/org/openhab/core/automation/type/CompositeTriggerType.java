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
 * This class is as {@link TriggerType} which logically combines {@link Trigger} modules. The composite trigger hides
 * internal logic between participating {@link Trigger}s and it can be used as a regular {@link Trigger} module.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - Initial contribution
 * @author Vasil Ilchev - Initial contribution
 */
@NonNullByDefault
public class CompositeTriggerType extends TriggerType {

    private final List<Trigger> children;

    /**
     * Creates an instance of {@code CompositeTriggerType} with ordered set of {@link Trigger} modules. It initializes
     * only base properties of the {@code CompositeTriggerType}.
     *
     * @param UID the {@link TriggerType}'s identifier, or {@code null} if a random identifier should be
     *            generated.
     * @param configDescriptions describing meta-data for the configuration of the future {@link Trigger} instances.
     * @param outputs a {@link List} with {@link Output} meta-information descriptions of the future
     *            {@link Trigger} instances.
     * @param children is a {@link List} of {@link Trigger} modules.
     *
     */
    public CompositeTriggerType(@Nullable String UID, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            @Nullable List<Output> outputs, @Nullable List<Trigger> children) {
        super(UID, configDescriptions, outputs);
        this.children = children != null ? Collections.unmodifiableList(children) : Collections.emptyList();
    }

    /**
     * Creates an instance of {@code CompositeTriggerType} with ordered set of {@link Trigger} modules. It initializes
     * all properties of the {@code CompositeTriggerType}.
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
     * @param outputs a {@link List} with {@link Output} meta-information descriptions of the future
     *            {@link Trigger} instances.
     * @param children is a {@link List} of {@link Trigger} modules.
     */
    public CompositeTriggerType(@Nullable String UID, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            @Nullable String label, @Nullable String description, @Nullable Set<String> tags,
            @Nullable Visibility visibility, @Nullable List<Output> outputs, @Nullable List<Trigger> children) {
        super(UID, configDescriptions, label, description, tags, visibility, outputs);
        this.children = children != null ? Collections.unmodifiableList(children) : Collections.emptyList();
    }

    /**
     * Gets the {@link Trigger} modules of the {@code CompositeTriggerType}.
     *
     * @return a {@link List} of the {@link Trigger} modules of this {@code CompositeTriggerType}.
     */
    public List<Trigger> getChildren() {
        return children;
    }
}
