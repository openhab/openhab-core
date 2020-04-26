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
package org.openhab.core.config.core;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ConfigDescriptionParameterGroupBuilder} class provides a builder for the
 * {@link ConfigDescriptionParameterGroup} class.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ConfigDescriptionParameterGroupBuilder {

    private String name;
    private @Nullable String context;
    private @Nullable Boolean advanced;
    private @Nullable String label;
    private @Nullable String description;

    private ConfigDescriptionParameterGroupBuilder(String name) {
        this.name = name;
    }

    /**
     * Creates a parameter group builder
     *
     * @param name configuration parameter name
     * @return parameter group builder
     */
    public static ConfigDescriptionParameterGroupBuilder create(String name) {
        return new ConfigDescriptionParameterGroupBuilder(name);
    }

    /**
     * Set. the context of the group.
     *
     * @param context group context as a string
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterGroupBuilder withContext(@Nullable String context) {
        this.context = context;
        return this;
    }

    /**
     * Sets the advanced flag for this group.
     *
     * @param advanced - true if the group contains advanced properties
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterGroupBuilder withAdvanced(@Nullable Boolean advanced) {
        this.advanced = advanced;
        return this;
    }

    /**
     * Sets the human readable label of the group.
     *
     * @param label as a string
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterGroupBuilder withLabel(@Nullable String label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the human readable description of the parameter group.
     *
     * @param description as a string
     * @return the updated builder instance
     */
    public ConfigDescriptionParameterGroupBuilder withDescription(@Nullable String description) {
        this.description = description;
        return this;
    }

    /**
     * Builds a result with the settings of this builder.
     *
     * @return the desired result
     */
    @SuppressWarnings("deprecation")
    public ConfigDescriptionParameterGroup build() throws IllegalArgumentException {
        return new ConfigDescriptionParameterGroup(name, context, advanced != null ? advanced : false, label,
                description);
    }
}
