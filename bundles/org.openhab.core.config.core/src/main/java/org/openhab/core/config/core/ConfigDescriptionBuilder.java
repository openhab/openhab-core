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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link ConfigDescriptionBuilder} class provides a builder for the {@link ConfigDescription} class.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ConfigDescriptionBuilder {

    private final URI uri;
    private List<ConfigDescriptionParameter> parameters = new ArrayList<>();
    private List<ConfigDescriptionParameterGroup> parameterGroups = new ArrayList<>();

    private ConfigDescriptionBuilder(URI uri) {
        this.uri = uri;
    }

    /**
     * Creates a config description builder
     *
     * @param @param uri the URI of this description within the {@link ConfigDescriptionRegistry}
     * @return the config description builder instance
     */
    public static ConfigDescriptionBuilder create(URI uri) {
        return new ConfigDescriptionBuilder(uri);
    }

    /**
     * Adds a {@link ConfigDescriptionParameter}s.
     *
     * @return the updated builder instance
     */
    public ConfigDescriptionBuilder withParameter(ConfigDescriptionParameter parameter) {
        parameters.add(parameter);
        return this;
    }

    /**
     * Adds a list of {@link ConfigDescriptionParameter}s.
     *
     * @return the updated builder instance
     */
    public ConfigDescriptionBuilder withParameters(List<ConfigDescriptionParameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    /**
     * Adds a {@link ConfigDescriptionParameterGroup} associated with the {@link ConfigDescriptionParameter}s.
     *
     * @return the updated builder instance
     */
    public ConfigDescriptionBuilder withParameterGroup(ConfigDescriptionParameterGroup parameterGroup) {
        parameterGroups.add(parameterGroup);
        return this;
    }

    /**
     * Adds a list of {@link ConfigDescriptionParameterGroup} associated with the {@link ConfigDescriptionParameter}s.
     *
     * @return the updated builder instance
     */
    public ConfigDescriptionBuilder withParameterGroups(List<ConfigDescriptionParameterGroup> parameterGroups) {
        this.parameterGroups = parameterGroups;
        return this;
    }

    /**
     * Builds a {@link ConfigDescription} with the settings of this builder.
     *
     * @return the desired result
     * @throws IllegalArgumentException if the URI is invalid
     */
    @SuppressWarnings("deprecation")
    public ConfigDescription build() throws IllegalArgumentException {
        return new ConfigDescription(uri, parameters, parameterGroups);
    }
}
