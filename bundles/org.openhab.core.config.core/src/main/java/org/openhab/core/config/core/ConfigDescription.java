/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;

/**
 * The {@link ConfigDescription} class contains a description for a concrete
 * configuration of e.g. a {@code Thing}, a {@code Bridge} or other specific
 * configurable services. This class <i>does not</i> contain the configuration
 * data itself and is usually used for data validation of the concrete
 * configuration or for supporting user interfaces.
 * <p>
 * The {@link ConfigDescriptionParameterGroup} provides a method to group parameters to allow the UI to better display
 * the parameter information. This can be left blank for small devices where there are only a few parameters, however
 * devices with larger numbers of parameters can set the group member in the {@link ConfigDescriptionParameter} and then
 * provide group information as part of the {@link ConfigDescription} class.
 * <p>
 * The description is stored within the {@link ConfigDescriptionRegistry} under the given URI. The URI has to follow the
 * syntax {@code '<scheme>:<token>[:<token>]'} (e.g. {@code "binding:hue:bridge"}).
 * <p>
 * <b>Hint:</b> This class is immutable.
 *
 * @author Michael Grammling - Initial contribution
 * @author Dennis Nobel - Initial contribution
 * @author Chris Jackson - Added parameter groups
 * @author Thomas Höfer - Added convenient operation to get config description parameters in a map
 */
@NonNullByDefault
public class ConfigDescription implements Identifiable<URI> {

    private final URI uri;
    private final List<ConfigDescriptionParameter> parameters;
    private final List<ConfigDescriptionParameterGroup> parameterGroups;

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param uri the URI of this description within the {@link ConfigDescriptionRegistry}
     * @param parameters the list of configuration parameters that belong to the given URI
     * @param groups the list of groups associated with the parameters
     * @throws IllegalArgumentException if the URI is null or invalid
     * @deprecated Use the {@link ConfigDescriptionBuilder} instead.
     */
    @Deprecated
    ConfigDescription(URI uri, @Nullable List<ConfigDescriptionParameter> parameters,
            @Nullable List<ConfigDescriptionParameterGroup> groups) {
        if (uri == null) {
            throw new IllegalArgumentException("The URI must not be null!");
        }
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("The scheme is missing!");
        }
        if (!uri.isOpaque()) {
            throw new IllegalArgumentException("The scheme specific part (token) must not start with a slash ('/')!");
        }

        this.uri = uri;
        this.parameters = parameters == null ? Collections.emptyList() : Collections.unmodifiableList(parameters);
        this.parameterGroups = groups == null ? Collections.emptyList() : Collections.unmodifiableList(groups);
    }

    /**
     * Returns the URI of this description within the {@link ConfigDescriptionRegistry}.
     * The URI follows the syntax {@code '<scheme>:<token>[:<token>]'} (e.g. {@code "binding:hue:bridge"}).
     *
     * @return the URI of this description
     */
    @Override
    public URI getUID() {
        return uri;
    }

    /**
     * Returns the corresponding {@link ConfigDescriptionParameter}s.
     * <p>
     * The returned list is immutable.
     *
     * @return the corresponding configuration description parameters (could be empty)
     */
    public List<ConfigDescriptionParameter> getParameters() {
        return parameters;
    }

    /**
     * Returns a map representation of the {@link ConfigDescriptionParameter}s. The map will use the name of the
     * parameter as key and the parameter as value.
     *
     * @return the unmodifiable map of configuration description parameters which uses the name as key and the parameter
     *         as value (could be empty)
     */
    public Map<String, ConfigDescriptionParameter> toParametersMap() {
        Map<String, ConfigDescriptionParameter> map = new HashMap<>();
        for (ConfigDescriptionParameter parameter : parameters) {
            map.put(parameter.getName(), parameter);
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Returns the list of configuration parameter groups associated with the parameters.
     * <p>
     * The returned list is immutable.
     *
     * @return the list of parameter groups parameter (could be empty)
     */
    public List<ConfigDescriptionParameterGroup> getParameterGroups() {
        return parameterGroups;
    }

    @Override
    public String toString() {
        return "ConfigDescription [uri=" + uri + ", parameters=" + parameters + ", groups=" + parameterGroups + "]";
    }
}
