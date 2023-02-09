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
package org.openhab.core.addon;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;

/**
 * The {@link AddonInfo} class contains general information about an add-on.
 * <p>
 * Any add-on information is provided by a {@link AddonInfoProvider} and can also be retrieved through the
 * {@link AddonInfoRegistry}.
 *
 * @author Michael Grammling - Initial contribution
 * @author Andre Fuechsel - Made author tag optional
 * @author Jan N. Klug - Refactored to cover all add-ons
 */
@NonNullByDefault
public class AddonInfo implements Identifiable<String> {
    private static final Set<String> SUPPORTED_ADDON_TYPES = Set.of("automation", "binding", "misc", "persistence",
            "transformation", "ui", "voice");

    private final String id;
    private final String type;
    private final String name;
    private final String description;
    private final @Nullable String connection;
    private final List<String> countries;
    private final @Nullable String configDescriptionURI;
    private final String serviceId;
    private @Nullable String sourceBundle;

    private AddonInfo(String id, String type, String name, String description, @Nullable String author,
            @Nullable String connection, List<String> countries, @Nullable String configDescriptionURI,
            @Nullable String serviceId, @Nullable String sourceBundle) throws IllegalArgumentException {
        // mandatory fields
        if (id.isBlank()) {
            throw new IllegalArgumentException("The ID must neither be null nor empty!");
        }
        if (!SUPPORTED_ADDON_TYPES.contains(type)) {
            throw new IllegalArgumentException(
                    "The type must be one of [" + String.join(", ", SUPPORTED_ADDON_TYPES) + "]");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("The name must neither be null nor empty!");
        }
        if (description.isBlank()) {
            throw new IllegalArgumentException("The description must neither be null nor empty!");
        }
        this.id = id;
        this.type = type;
        this.name = name;
        this.description = description;

        // optional fields
        this.connection = connection;
        this.countries = countries;
        this.configDescriptionURI = configDescriptionURI;
        this.serviceId = Objects.requireNonNullElse(serviceId, type + "." + id);
        this.sourceBundle = sourceBundle;
    }

    /**
     * Returns an unique identifier for the add-on (e.g. "binding-hue").
     *
     * @return an identifier for the add-on
     */
    @Override
    public String getUID() {
        return type + Addon.ADDON_SEPARATOR + id;
    }

    /**
     * Returns the id part of the UID
     *
     * @return the identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Returns a human-readable name for the add-on (e.g. "HUE Binding").
     *
     * @return a human-readable name for the add-on (neither null, nor empty)
     */
    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getServiceId() {
        return serviceId;
    }

    /**
     * Returns a human-readable description for the add-on
     * (e.g. "Discovers and controls HUE bulbs").
     *
     * @return a human-readable description for the add-on
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the link to a concrete {@link org.openhab.core.config.core.ConfigDescription}.
     *
     * @return the link to a concrete ConfigDescription (could be <code>null</code>>)
     */
    public @Nullable String getConfigDescriptionURI() {
        return configDescriptionURI;
    }

    public @Nullable String getSourceBundle() {
        return sourceBundle;
    }

    public List<String> getCountries() {
        return countries;
    }

    public static Builder builder(String id, String type) {
        return new Builder(id, type);
    }

    public static Builder builder(AddonInfo addonInfo) {
        return new Builder(addonInfo);
    }

    public static class Builder {

        private final String id;
        private final String type;
        private String name = "";
        private String description = "";
        private @Nullable String author;
        private @Nullable String connection;
        private List<String> countries = List.of();
        private @Nullable String configDescriptionURI = "";
        private @Nullable String serviceId;
        private @Nullable String sourceBundle;

        private Builder(String id, String type) {
            this.id = id;
            this.type = type;
        }

        private Builder(AddonInfo addonInfo) {
            this.id = addonInfo.id;
            this.type = addonInfo.type;
            this.name = addonInfo.name;
            this.description = addonInfo.description;
            this.connection = addonInfo.connection;
            this.countries = addonInfo.countries;
            this.configDescriptionURI = addonInfo.configDescriptionURI;
            this.serviceId = addonInfo.serviceId;
            this.sourceBundle = addonInfo.sourceBundle;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withAuthor(@Nullable String author) {
            this.author = author;
            return this;
        }

        public Builder withConnection(@Nullable String connection) {
            this.connection = connection;
            return this;
        }

        public Builder withCountries(@Nullable String countries) {
            this.countries = Arrays.asList(Objects.requireNonNullElse(countries, "").split(","));
            return this;
        }

        public Builder withCountries(List<String> countries) {
            this.countries = countries;
            return this;
        }

        public Builder withConfigDescriptionURI(@Nullable String configDescriptionURI) {
            this.configDescriptionURI = configDescriptionURI;
            return this;
        }

        public Builder withServiceId(@Nullable String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder withSourceBundle(@Nullable String sourceBundle) {
            this.sourceBundle = sourceBundle;
            return this;
        }

        /**
         * Build an {@link AddonInfo} from this builder
         *
         * @return the add-on info object
         * @throws IllegalArgumentException if any of the information in this builder is invalid
         */
        public AddonInfo build() throws IllegalArgumentException {
            return new AddonInfo(id, type, name, description, author, connection, countries, configDescriptionURI,
                    serviceId, sourceBundle);
        }
    }
}
