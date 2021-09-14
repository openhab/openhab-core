/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import java.util.Map;

/**
 * This class defines an add-on.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Yannick Schaus - Add fields
 */
public class Addon {

    private final String id;
    private final String label;
    private final String version;
    private final String contentType;
    private final String link;
    private final String author;
    private boolean verifiedAuthor;
    private boolean installed;
    private final String type;
    private final String description;
    private final String detailedDescription;
    private final String configDescriptionURI;
    private final String keywords;
    private final String countries;
    private final String connection;
    private final String backgroundColor;
    private final String imageLink;
    private final Map<String, Object> properties;

    /**
     * Creates a new Addon instance
     *
     * @param id the id of the add-on
     * @param type the type id of the add-on
     * @param label the label of the add-on
     * @param version the version of the add-on
     * @param contentType the content type of the add-on
     * @param link the link to find more information about the add-on (can be null)
     * @param author the author of the add-on
     * @param verifiedAuthor true, if the author is verified
     * @param installed true, if the add-on is installed, false otherwise
     */
    public Addon(String id, String type, String label, String version, String contentType, String link, String author,
            boolean verifiedAuthor, boolean installed) {
        this(id, type, label, version, contentType, link, author, verifiedAuthor, installed, null, null, null, null,
                null, null, null, null, null);
    }

    /**
     * Creates a new Addon instance
     *
     * @param id the id of the add-on
     * @param type the type id of the add-on
     * @param label the label of the add-on
     * @param version the version of the add-on
     * @param contentType the content type of the add-on
     * @param description the detailed description of the add-on (may be null)
     * @param backgroundColor for displaying the add-on (may be null)
     * @param link the link to find more information about the add-on (may be null)
     * @param author the author of the add-on
     * @param verifiedAuthor true, if the author is verified
     * @param imageLink the link to an image (png/svg) (may be null)
     * @param installed true, if the add-on is installed, false otherwise
     */
    public Addon(String id, String type, String label, String version, String contentType, String link, String author,
            boolean verifiedAuthor, boolean installed, String description, String detailedDescription,
            String configDescriptionURI, String keywords, String countries, String connection, String backgroundColor,
            String imageLink, Map<String, Object> properties) {
        this.id = id;
        this.label = label;
        this.version = version;
        this.contentType = contentType;
        this.description = description;
        this.detailedDescription = detailedDescription;
        this.configDescriptionURI = configDescriptionURI;
        this.keywords = keywords;
        this.countries = countries;
        this.connection = connection;
        this.backgroundColor = backgroundColor;
        this.link = link;
        this.imageLink = imageLink;
        this.author = author;
        this.verifiedAuthor = verifiedAuthor;
        this.installed = installed;
        this.type = type;
        this.properties = properties;
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
    public String getLink() {
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
     * The content type of the add-on
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * The description of the add-on
     */
    public String getDescription() {
        return description;
    }

    /**
     * The detailed description of the add-on
     */
    public String getDetailedDescription() {
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
    public String getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * A link to an image (png/svg) for the add-on
     */
    public String getImageLink() {
        return imageLink;
    }
}
