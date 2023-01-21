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
package org.openhab.core.addon.marketplace.internal.json.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link AddonEntryDTO} is a DTO for encapsulating a single addon information.
 *
 * @author Jan N. Klug - Initial contribution
 */
public class AddonEntryDTO {
    public String uid = "";
    public String id = "";
    public String type = "";
    public String description = "";
    public String title = "";
    public String link = "";
    public String version = "";
    @SerializedName("compatible_versions")
    public String compatibleVersions = "";
    public String author = "";
    public String configDescriptionURI = "";
    public String maturity = "unstable";
    @SerializedName("content_type")
    public String contentType = "";
    @SerializedName("image_url")
    public String imageUrl;
    public String url = "";
    @SerializedName("logger_packages")
    public List<String> loggerPackages = List.of();
}
