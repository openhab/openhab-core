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
package org.openhab.core.binding;

import java.net.URI;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;

/**
 * The {@link BindingInfo} class contains general information about a binding.
 * <p>
 * Any binding information are provided by a {@link BindingInfoProvider} and can also be retrieved through the
 * {@link BindingInfoRegistry}.
 * <p>
 * <b>Hint:</b> This class is immutable.
 *
 * @author Michael Grammling - Initial contribution
 * @author Andre Fuechsel - Made author tag optional
 */
@NonNullByDefault
public class BindingInfo implements Identifiable<String> {

    /**
     * The default service ID prefix.
     */
    public static final String DEFAULT_SERVICE_ID_PREFIX = "binding.";

    private String id;
    private String name;
    private @Nullable String description;
    private @Nullable String author;
    private @Nullable URI configDescriptionURI;
    private String serviceId;

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param id the identifier for the binding (must neither be null, nor empty)
     * @param name a human readable name for the binding (must neither be null, nor empty)
     * @param description a human readable description for the binding (could be null or empty)
     * @param author the author of the binding (could be null or empty)
     * @param serviceId the service id of the main service of the binding (can be null)
     * @param configDescriptionURI the link to a concrete ConfigDescription (could be null)
     * @throws IllegalArgumentException if the identifier or the name are null or empty
     */
    public BindingInfo(String id, String name, @Nullable String description, @Nullable String author,
            @Nullable String serviceId, @Nullable URI configDescriptionURI) throws IllegalArgumentException {
        if ((id == null) || (id.isEmpty())) {
            throw new IllegalArgumentException("The ID must neither be null nor empty!");
        }

        if ((name == null) || (name.isEmpty())) {
            throw new IllegalArgumentException("The name must neither be null nor empty!");
        }

        this.id = id;
        this.name = name;
        this.description = description;
        this.author = author;
        this.serviceId = serviceId != null ? serviceId : DEFAULT_SERVICE_ID_PREFIX + id;
        this.configDescriptionURI = configDescriptionURI;
    }

    /**
     * Returns an identifier for the binding (e.g. "hue").
     *
     * @return an identifier for the binding (neither null, nor empty)
     */
    @Override
    public String getUID() {
        return id;
    }

    /**
     * Returns a human readable name for the binding (e.g. "HUE Binding").
     *
     * @return a human readable name for the binding (neither null, nor empty)
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a human readable description for the binding
     * (e.g. "Discovers and controls HUE bulbs").
     *
     * @return a human readable description for the binding (could be null or empty)
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * Returns the author of the binding (e.g. "Max Mustermann").
     *
     * @return the author of the binding (could be null or empty)
     */
    public @Nullable String getAuthor() {
        return author;
    }

    /**
     * Returns the service ID of the bindings main service, that can be configured.
     *
     * @return service ID
     */
    public String getServiceId() {
        return serviceId;
    }

    /**
     * Returns the link to a concrete {@link ConfigDescription}.
     *
     * @return the link to a concrete ConfigDescription (could be null)
     */
    public @Nullable URI getConfigDescriptionURI() {
        return configDescriptionURI;
    }

    @Override
    public String toString() {
        return "BindingInfo [id=" + id + ", name=" + name + ", description=" + description + ", author=" + author
                + ", configDescriptionURI=" + configDescriptionURI + "]";
    }
}
