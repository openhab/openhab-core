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
package org.openhab.core.addon.dto;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.addon.Addon;
import org.openhab.core.common.Version;

/**
 * This is a data transfer object that is used to serialize add-ons.
 *
 * @author Ravi Nadahar - Initial contribution
 */
public class AddonDTO {

    public String uid;
    public String id;
    public String label;
    public String version;
    public String maturity;
    public boolean compatible;
    public String contentType;
    public String link;
    public String author;
    public boolean verifiedAuthor;
    public boolean installed;
    public String type;
    public String description;
    public String detailedDescription;
    public String configDescriptionURI;
    public String keywords;
    public List<@NonNull String> countries;
    public String license;
    public String connection;
    public String backgroundColor;
    public String imageLink;
    public Map<@NonNull String, @NonNull Object> properties;
    public List<@NonNull String> loggerPackages;

    /**
     * Create a new, empty {@link AddonDTO} instance.
     */
    public AddonDTO() {
    }

    /**
     * Create a new {@link AddonDTO} instance from the specified {@link Addon}.
     *
     * @param addon the {@link Addon}.
     */
    public AddonDTO(@NonNull Addon addon) {
        this.uid = addon.getUid();
        this.id = addon.getId();
        this.label = addon.getLabel();
        Version v = addon.getVersion();
        if (v != null) {
            this.version = v.toString();
        }
        this.maturity = addon.getMaturity();
        this.compatible = addon.getCompatible();
        this.contentType = addon.getContentType();
        this.link = addon.getLink();
        this.author = addon.getAuthor();
        this.verifiedAuthor = addon.isVerifiedAuthor();
        this.installed = addon.isInstalled();
        this.type = addon.getType();
        this.description = addon.getDescription();
        this.detailedDescription = addon.getDetailedDescription();
        this.configDescriptionURI = addon.getConfigDescriptionURI();
        this.keywords = addon.getKeywords();
        List<@NonNull String> stringList = addon.getCountries();
        if (!stringList.isEmpty()) {
            this.countries = stringList;
        }
        this.license = addon.getLicense();
        this.connection = addon.getConnection();
        this.backgroundColor = addon.getBackgroundColor();
        this.imageLink = addon.getImageLink();
        Map<@NonNull String, @NonNull Object> map = addon.getProperties();
        if (!map.isEmpty()) {
            this.properties = map;
        }
        stringList = addon.getLoggerPackages();
        if (!stringList.isEmpty()) {
            this.loggerPackages = stringList;
        }
    }

    /**
     * Create a new {@link Addon} instance from this {@link AddonDTO}.
     *
     * @return The new {@link Addon} instance.
     */
    public @NonNull Addon toAddon() {
        Addon.Builder b = Addon.create(this.uid);
        if (this.id != null) {
            b.withId(this.id);
        }
        if (this.label != null) {
            b.withLabel(this.label);
        }
        if (this.version != null) {
            b.withVersion(Version.valueOf(this.version));
        }
        if (this.maturity != null) {
            b.withMaturity(this.maturity);
        }
        b.withCompatible(this.compatible);
        if (this.contentType != null) {
            b.withContentType(this.contentType);
        }
        if (this.link != null) {
            b.withLink(this.link);
        }
        if (this.author != null) {
            b.withAuthor(this.author, this.verifiedAuthor);
        }
        b.withInstalled(this.installed);
        if (this.type != null) {
            b.withType(this.type);
        }
        if (this.description != null) {
            b.withDescription(this.description);
        }
        if (this.detailedDescription != null) {
            b.withDetailedDescription(this.detailedDescription);
        }
        if (this.configDescriptionURI != null) {
            b.withConfigDescriptionURI(this.configDescriptionURI);
        }
        if (this.keywords != null) {
            b.withKeywords(this.keywords);
        }
        if (this.countries != null) {
            b.withCountries(this.countries);
        }
        if (this.license != null) {
            b.withLicense(this.license);
        }
        if (this.connection != null) {
            b.withConnection(this.connection);
        }
        if (this.backgroundColor != null) {
            b.withBackgroundColor(this.backgroundColor);
        }
        if (this.imageLink != null) {
            b.withImageLink(this.imageLink);
        }
        if (this.properties != null) {
            b.withProperties(this.properties);
        }
        if (this.loggerPackages != null) {
            b.withLoggerPackages(this.loggerPackages);
        }
        return b.build();
    }
}
