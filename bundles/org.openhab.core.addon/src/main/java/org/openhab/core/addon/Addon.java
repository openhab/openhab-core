/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.Version;

/**
 * This class defines an add-on.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Yannick Schaus - Add fields
 */
public class Addon {
    public static final Set<String> CODE_MATURITY_LEVELS = Set.of("alpha", "beta", "mature", "stable");
    public static final String ADDON_SEPARATOR = "-";

    private final String uid;

    private final String id;
    private final String label;
    private final @Nullable Version version;
    private final @Nullable String maturity;
    private final boolean compatible;
    private final String contentType;
    private final @Nullable String link;
    private final String author;
    private final boolean verifiedAuthor;
    private volatile boolean installed;
    private final String type;
    private final @Nullable String description;
    private final @Nullable String detailedDescription;
    private final String configDescriptionURI;
    private final String keywords;
    private final List<String> countries;
    private final @Nullable String license;
    private final String connection;
    private final @Nullable String backgroundColor;
    private final @Nullable String imageLink;
    private final Map<String, Object> properties;
    private final List<String> loggerPackages;

    /**
     * Creates a new Addon instance
     *
     * @param uid the id of the add-on (e.g. "binding-dmx", "json:transform-format" or "marketplace:123456")
     * @param type the type id of the add-on (e.g. "automation")
     * @param uid the technical name of the add-on (e.g. "influxdb")
     * @param label the label of the add-on
     * @param version the version of the add-on
     * @param maturity the maturity level of this version
     * @param compatible if this add-on is compatible with the current core version
     * @param contentType the content type of the add-on
     * @param link the link to find more information about the add-on (may be null)
     * @param author the author of the add-on
     * @param verifiedAuthor true, if the author is verified
     * @param installed true, if the add-on is installed, false otherwise
     * @param description the description of the add-on (may be null)
     * @param detailedDescription the detailed description of the add-on (may be null)
     * @param configDescriptionURI the URI to the configuration description for this add-on
     * @param keywords the keywords for this add-on
     * @param countries a list of ISO 3166 codes relevant to this add-on
     * @param license the SPDX license identifier
     * @param connection a string describing the type of connection (local or cloud, push or pull...) this add-on uses,
     *            if applicable.
     * @param backgroundColor for displaying the add-on (may be null)
     * @param imageLink the link to an image (png/svg) (may be null)
     * @param properties a {@link Map} containing addition information
     * @param loggerPackages a {@link List} containing the package names belonging to this add-on
     * @throws IllegalArgumentException when a mandatory parameter is invalid
     */
    Addon(String uid, String type, String id, String label, @Nullable Version version, @Nullable String maturity,
            boolean compatible, String contentType, @Nullable String link, String author, boolean verifiedAuthor,
            boolean installed, @Nullable String description, @Nullable String detailedDescription,
            String configDescriptionURI, String keywords, List<String> countries, @Nullable String license,
            String connection, @Nullable String backgroundColor, @Nullable String imageLink,
            @Nullable Map<String, Object> properties, List<String> loggerPackages) {
        if (uid == null || uid.isBlank()) {
            throw new IllegalArgumentException("uid must not be empty");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be empty");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be empty");
        }

        this.uid = uid;
        this.type = type;
        this.id = id;

        this.label = label;
        this.version = version;
        this.maturity = maturity;
        this.compatible = compatible;
        this.contentType = contentType;
        this.description = description;
        this.detailedDescription = detailedDescription;
        this.configDescriptionURI = configDescriptionURI == null || configDescriptionURI.isBlank() ? ""
                : configDescriptionURI;
        this.keywords = keywords == null || keywords.isBlank() ? "" : keywords;
        this.countries = countries == null ? List.of() : List.copyOf(countries);
        this.license = license;
        this.connection = connection == null || connection.isBlank() ? "" : connection;
        this.backgroundColor = backgroundColor;
        this.link = link;
        this.imageLink = imageLink;
        this.author = author == null || author.isBlank() ? "" : author;
        this.verifiedAuthor = verifiedAuthor;
        this.installed = installed;
        this.properties = properties == null ? Map.of() : Map.copyOf(properties);
        this.loggerPackages = loggerPackages == null ? List.of() : List.copyOf(loggerPackages);
    }

    /**
     * The type of the addon (same as id of {@link AddonType})
     */
    public String getType() {
        return type;
    }

    /**
     * The uid of the add-on (e.g. "binding-dmx", "json:transform-format" or "marketplace:123456")
     */
    public String getUid() {
        return uid;
    }

    /**
     * The id of the add-on (e.g. "influxdb")
     */
    public String getId() {
        return id;
    }

    /**
     * The label of the add-on
     */
    public String getLabel() {
        return label;
    }

    /**
     * The (optional) link to find more information about the add-on
     */
    public @Nullable String getLink() {
        return link;
    }

    /**
     * The author of the add-on
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Whether the add-on author is verified or not
     */
    public boolean isVerifiedAuthor() {
        return verifiedAuthor;
    }

    /**
     * The version of the add-on
     */
    public @Nullable Version getVersion() {
        return version;
    }

    /**
     * The maturity level of this version
     */
    public @Nullable String getMaturity() {
        return maturity;
    }

    /**
     * The (expected) compatibility of this add-on
     */
    public boolean getCompatible() {
        return compatible;
    }

    /**
     * The content type of the add-on
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * The description of the add-on
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * The detailed description of the add-on
     */
    public @Nullable String getDetailedDescription() {
        return detailedDescription;
    }

    /**
     * The URI to the configuration description for this add-on
     */
    public String getConfigDescriptionURI() {
        return configDescriptionURI;
    }

    /**
     * The keywords for this add-on
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * A list of ISO 3166 codes relevant to this add-on
     */
    public List<String> getCountries() {
        return countries;
    }

    /**
     * The SPDX License identifier for this addon
     */
    public @Nullable String getLicense() {
        return license;
    }

    /**
     * A string describing the type of connection (local, cloud, cloudDiscovery) this add-on uses, if applicable.
     */
    public String getConnection() {
        return connection;
    }

    /**
     * A set of additional properties relative to this add-on
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * true, if the add-on is installed, false otherwise
     */
    public boolean isInstalled() {
        return installed;
    }

    /**
     * Sets the installed state
     */
    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    /**
     * The background color for rendering the add-on
     */
    public @Nullable String getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * A link to an image (png/svg) for the add-on
     */
    public @Nullable String getImageLink() {
        return imageLink;
    }

    /**
     * The package names that are associated with this add-on
     */
    public List<String> getLoggerPackages() {
        return loggerPackages;
    }

    /**
     * Create a builder for an {@link Addon}
     *
     * @param uid the UID of the add-on (e.g. "binding-dmx", "json:transform-format" or "marketplace:123456")
     * @return the builder
     */
    public static Builder create(String uid) {
        return new Builder(uid);
    }

    public static Builder create(Addon addon) {
        return new Builder(addon);
    }

    public static class Builder {
        private final @NonNull String uid;
        private @Nullable String id;
        private @Nullable String label;
        private @Nullable Version version;
        private @Nullable String maturity;
        private boolean compatible = true;
        private @Nullable String contentType;
        private @Nullable String link;
        private @Nullable String author;
        private boolean verifiedAuthor = false;
        private boolean installed = false;
        private @Nullable String type;
        private @Nullable String description;
        private @Nullable String detailedDescription;
        private @Nullable String configDescriptionURI;
        private @Nullable String keywords;
        private @Nullable List<@NonNull String> countries = List.of();
        private @Nullable String license;
        private @Nullable String connection;
        private @Nullable String backgroundColor;
        private @Nullable String imageLink;
        private @Nullable Map<@NonNull String, @NonNull Object> properties;
        private @Nullable List<@NonNull String> loggerPackages = List.of();

        private Builder(@NonNull String uid) {
            this.uid = uid;
        }

        private Builder(Addon addon) {
            this.uid = addon.uid;
            this.id = addon.id;
            this.label = addon.label;
            this.version = addon.version;
            this.maturity = addon.maturity;
            this.compatible = addon.compatible;
            this.contentType = addon.contentType;
            this.link = addon.link;
            this.author = addon.author;
            this.verifiedAuthor = addon.verifiedAuthor;
            this.installed = addon.installed;
            this.type = addon.type;
            this.description = addon.description;
            this.detailedDescription = addon.detailedDescription;
            this.configDescriptionURI = addon.configDescriptionURI;
            this.keywords = addon.keywords;
            this.countries = addon.countries;
            this.license = addon.license;
            this.connection = addon.connection;
            this.backgroundColor = addon.backgroundColor;
            this.imageLink = addon.imageLink;
            this.properties = addon.properties;
            this.loggerPackages = addon.loggerPackages;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withLabel(String label) {
            this.label = label;
            return this;
        }

        public Builder withVersion(@Nullable Version version) {
            this.version = version;
            return this;
        }

        public Builder withMaturity(@Nullable String maturity) {
            this.maturity = maturity;
            return this;
        }

        public Builder withCompatible(boolean compatible) {
            this.compatible = compatible;
            return this;
        }

        public Builder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder withLink(String link) {
            this.link = link;
            return this;
        }

        public Builder withAuthor(@Nullable String author) {
            this.author = Objects.requireNonNullElse(author, "");
            return this;
        }

        public Builder withAuthor(String author, boolean verifiedAuthor) {
            this.author = author;
            this.verifiedAuthor = verifiedAuthor;
            return this;
        }

        public Builder withInstalled(boolean installed) {
            this.installed = installed;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withDetailedDescription(String detailedDescription) {
            this.detailedDescription = detailedDescription;
            return this;
        }

        public Builder withConfigDescriptionURI(@Nullable String configDescriptionURI) {
            this.configDescriptionURI = Objects.requireNonNullElse(configDescriptionURI, "");
            return this;
        }

        public Builder withKeywords(String keywords) {
            this.keywords = keywords;
            return this;
        }

        public @Nullable List<@NonNull String> getCountries() {
            return countries;
        }

        public Builder withCountries(List<@NonNull String> countries) {
            this.countries = countries;
            return this;
        }

        public Builder withLicense(@Nullable String license) {
            this.license = license;
            return this;
        }

        public Builder withConnection(String connection) {
            this.connection = connection;
            return this;
        }

        public Builder withBackgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder withImageLink(@Nullable String imageLink) {
            this.imageLink = imageLink;
            return this;
        }

        public Builder withProperty(@NonNull String key, @NonNull Object value) {
            Map<@NonNull String, @NonNull Object> props = this.properties;
            if (props == null) {
                props = new HashMap<>();
            }
            props.put(key, value);
            this.properties = props;
            return this;
        }

        public Builder withProperties(@Nullable Map<@NonNull String, @NonNull Object> properties) {
            this.properties = properties;
            return this;
        }

        public @Nullable List<@NonNull String> getLoggerPackages() {
            return loggerPackages;
        }

        public Builder withLoggerPackages(@Nullable List<@NonNull String> loggerPackages) {
            this.loggerPackages = loggerPackages;
            return this;
        }

        public Addon build() {
            return new Addon(uid, type, id, label, version, maturity, compatible, contentType, link, author,
                    verifiedAuthor, installed, description, detailedDescription, configDescriptionURI, keywords,
                    countries, license, connection, backgroundColor, imageLink, properties, loggerPackages);
        }
    }
}
