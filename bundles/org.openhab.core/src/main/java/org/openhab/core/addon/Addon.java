/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

/**
 * This class defines an add-on.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Yannick Schaus - Add fields
 */
public class Addon {
    public static final Set<String> CODE_MATURITY_LEVELS = Set.of("alpha", "beta", "mature", "stable");

    private final String id;
    private final String label;
    private final String version;
    private final @Nullable String maturity;
    private final String contentType;
    private final @Nullable String link;
    private final String author;
    private boolean verifiedAuthor;
    private boolean installed;
    private final String type;
    private final @Nullable String description;
    private final @Nullable String detailedDescription;
    private final String configDescriptionURI;
    private final String keywords;
    private final String countries;
    private final @Nullable String license;
    private final String connection;
    private final @Nullable String backgroundColor;
    private final @Nullable String imageLink;
    private final Map<String, Object> properties;

    /**
     * Creates a new Addon instance
     *
     * @param id the id of the add-on
     * @param type the type id of the add-on
     * @param label the label of the add-on
     * @param version the version of the add-on
     * @param maturity the maturity level of this version
     * @param contentType the content type of the add-on
     * @param link the link to find more information about the add-on (may be null)
     * @param author the author of the add-on
     * @param verifiedAuthor true, if the author is verified
     * @param installed true, if the add-on is installed, false otherwise
     * @param description the description of the add-on (may be null)
     * @param detailedDescription the detailed description of the add-on (may be null)
     * @param configDescriptionURI the URI to the configuration description for this add-on
     * @param keywords the keywords for this add-on
     * @param countries a comma-separated list of ISO 3166 codes relevant to this add-on
     * @param license the SPDX license identifier
     * @param connection a string describing the type of connection (local or cloud, push or pull...) this add-on uses,
     *            if applicable.
     * @param backgroundColor for displaying the add-on (may be null)
     * @param imageLink the link to an image (png/svg) (may be null)
     * @param properties a {@link Map} containing addition information
     */
    private Addon(String id, String type, String label, String version, @Nullable String maturity, String contentType,
            @Nullable String link, String author, boolean verifiedAuthor, boolean installed,
            @Nullable String description, @Nullable String detailedDescription, String configDescriptionURI,
            String keywords, String countries, @Nullable String license, String connection,
            @Nullable String backgroundColor, @Nullable String imageLink, @Nullable Map<String, Object> properties) {
        this.id = id;
        this.label = label;
        this.version = version;
        this.maturity = maturity;
        this.contentType = contentType;
        this.description = description;
        this.detailedDescription = detailedDescription;
        this.configDescriptionURI = configDescriptionURI;
        this.keywords = keywords;
        this.countries = countries;
        this.license = license;
        this.connection = connection;
        this.backgroundColor = backgroundColor;
        this.link = link;
        this.imageLink = imageLink;
        this.author = author;
        this.verifiedAuthor = verifiedAuthor;
        this.installed = installed;
        this.type = type;
        this.properties = properties == null ? Map.of() : properties;
    }

    /**
     * The id of the {@AddonType} of the add-on
     */
    public String getType() {
        return type;
    }

    /**
     * The id of the add-on
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
    public String getVersion() {
        return version;
    }

    /**
     * The maturity level of this version
     */
    public @Nullable String getMaturity() {
        return maturity;
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
     * A comma-separated list of ISO 3166 codes relevant to this add-on
     */
    public String getCountries() {
        return countries;
    }

    /**
     * The SPDX License identifier for this addon
     */
    public @Nullable String getLicense() {
        return license;
    }

    /**
     * A string describing the type of connection (local or cloud, push or pull...) this add-on uses, if applicable.
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

    public static Builder create(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private String id;
        private String label;
        private String version = "";
        private @Nullable String maturity;
        private String contentType;
        private @Nullable String link;
        private String author = "";
        private boolean verifiedAuthor = false;
        private boolean installed = false;
        private String type;
        private @Nullable String description;
        private @Nullable String detailedDescription;
        private String configDescriptionURI = "";
        private String keywords = "";
        private String countries = "";
        private @Nullable String license;
        private String connection = "";
        private @Nullable String backgroundColor;
        private @Nullable String imageLink;
        private Map<String, Object> properties = new HashMap<>();

        private Builder(String id) {
            this.id = id;
        }

        public Builder withLabel(String label) {
            this.label = label;
            return this;
        }

        public Builder withVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder withMaturity(@Nullable String maturity) {
            this.maturity = maturity;
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

        public Builder withAuthor(String author) {
            this.author = author;
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

        public Builder withType(String type) {
            this.type = type;
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

        public Builder withConfigDescriptionURI(String configDescriptionURI) {
            this.configDescriptionURI = configDescriptionURI;
            return this;
        }

        public Builder withKeywords(String keywords) {
            this.keywords = keywords;
            return this;
        }

        public Builder withCountries(String countries) {
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

        public Builder withProperty(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder withProperties(Map<String, Object> properties) {
            this.properties.putAll(properties);
            return this;
        }

        public Addon build() {
            return new Addon(id, type, label, version, maturity, contentType, link, author, verifiedAuthor, installed,
                    description, detailedDescription, configDescriptionURI, keywords, countries, license, connection,
                    backgroundColor, imageLink, properties.isEmpty() ? null : properties);
        }
    }
}
