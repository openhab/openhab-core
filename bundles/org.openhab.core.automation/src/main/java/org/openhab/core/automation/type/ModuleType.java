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
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.Visibility;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.config.core.ConfigDescriptionParameter;

/**
 * This class provides common functionality for creating {@link ModuleType} instances. Each {@link ModuleType} instance
 * defines the meta-information needed for creation of a {@link Module} instance which is a building block for a
 * {@link Rule}. The meta-information describes the {@link Configuration} of a {@link Module} providing list with
 * {@link ConfigDescriptionParameter}s, {@link Input}s and {@link Output}s of a {@link Module}. Each {@link ModuleType}
 * instance owns an unique id which is used as reference in the {@link Module}s, to find their meta-information.
 * <p>
 * Whether the {@link ModuleType}s can be used by anyone, depends from their {@link Visibility} value, but they can be
 * modified only by their creator.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - Initial contribution
 */
@NonNullByDefault
public abstract class ModuleType implements Identifiable<String> {

    /**
     * Holds the {@link ModuleType}'s identifier, specified by its creator or randomly generated, and is used as
     * reference in the {@link Module}s, to find their meta-information.
     */
    private final String uid;

    /**
     * Determines whether the {@link ModuleType}s can be used by anyone if they are {@link Visibility#VISIBLE} or only
     * by their creator if they are {@link Visibility#HIDDEN}.
     */
    private final Visibility visibility;

    /**
     * Defines categories that fit the particular {@link ModuleType} and which can serve as criteria for searching or
     * filtering of {@link ModuleType}s.
     */
    private final Set<String> tags;

    /**
     * Defines short and accurate, human-readable label of the {@link ModuleType}.
     */
    private final @Nullable String label;

    /**
     * Defines detailed, human-readable description of usage of {@link ModuleType} and its benefits.
     */
    private final @Nullable String description;

    /**
     * Describes meta-data for the configuration of the future {@link Module} instances.
     */
    protected List<ConfigDescriptionParameter> configDescriptions;

    /**
     * Creates a {@link ModuleType} instance. This constructor is responsible to initialize common base properties of
     * the {@link ModuleType}s.
     *
     * @param UID the {@link ModuleType}'s identifier, or {@code null} if a random identifier should be
     *            generated.
     * @param configDescriptions describing meta-data for the configuration of the future {@link Module} instances
     */
    public ModuleType(@Nullable String UID, @Nullable List<ConfigDescriptionParameter> configDescriptions) {
        this(UID, configDescriptions, null, null, null, null);
    }

    /**
     * Creates a {@link ModuleType} instance. This constructor is responsible to initialize all common properties of
     * the {@link ModuleType}s.
     *
     * @param UID the {@link ModuleType}'s identifier, or {@code null} if a random identifier should be
     *            generated.
     * @param configDescriptions describing meta-data for the configuration of the future {@link Module} instances.
     * @param label a short and accurate, human-readable label of the {@link ModuleType}.
     * @param description a detailed, human-readable description of usage of {@link ModuleType} and its benefits.
     * @param tags defines categories that fit the {@link ModuleType} and which can serve as criteria for
     *            searching or filtering it.
     * @param visibility determines whether the {@link ModuleType} can be used by anyone if it is
     *            {@link Visibility#VISIBLE} or only by its creator if it is {@link Visibility#HIDDEN}.
     *            If {@code null} is provided the default visibility {@link Visibility#VISIBLE} will be
     *            used.
     */
    public ModuleType(@Nullable String UID, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            @Nullable String label, @Nullable String description, @Nullable Set<String> tags,
            @Nullable Visibility visibility) {
        this.uid = UID == null ? UUID.randomUUID().toString() : UID;
        this.label = label;
        this.description = description;
        this.configDescriptions = configDescriptions == null ? Collections.emptyList()
                : Collections.unmodifiableList(configDescriptions);
        this.tags = tags == null ? Collections.emptySet() : Collections.unmodifiableSet(tags);
        this.visibility = visibility == null ? Visibility.VISIBLE : visibility;
    }

    /**
     * Gets the {@link ModuleType}. It can be specified by the {@link ModuleType}'s creator, or randomly generated.
     *
     * @return an identifier of this {@link ModuleType}. Can't be {@code null}.
     */
    @Override
    public String getUID() {
        return uid;
    }

    /**
     * Gets the meta-data for the configuration of the future {@link Module} instances.
     *
     * @return a {@link Set} of meta-information configuration descriptions.
     */
    public List<ConfigDescriptionParameter> getConfigurationDescriptions() {
        return configDescriptions;
    }

    /**
     * Gets the assigned to the {@link ModuleType} - {@link #tags}.
     *
     * @return a set of tags, assigned to this {@link ModuleType}.
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Gets the label of the {@link ModuleType}. The label is a short and accurate, human-readable label of the
     * {@link ModuleType}.
     *
     * @return the {@link ModuleType}'s {@link #label}. Can be {@code null}.
     */
    public @Nullable String getLabel() {
        return label;
    }

    /**
     * Gets the description of the {@link ModuleType}. The description is a short and understandable description of that
     * for what can be used the {@link ModuleType}.
     *
     * @return the {@link ModuleType}'s {@link #description}. Can be {@code null}.
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * Gets the visibility of the {@link ModuleType}. The visibility determines whether the {@link ModuleType}s can be
     * used by anyone if they are {@link Visibility#VISIBLE} or only by their creator if they are
     * {@link Visibility#HIDDEN}. The default visibility is {@link Visibility#VISIBLE}.
     *
     * @return the {@link #visibility} of the {@link ModuleType}.
     */
    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + uid.hashCode();
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ModuleType other = (ModuleType) obj;
        if (!uid.equals(other.uid)) {
            return false;
        }
        return true;
    }
}
