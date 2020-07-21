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
package org.openhab.core.addon;

/**
 * This class defines an add-on.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class Addon {

    private final String id;
    private final String label;
    private final String version;
    private final String link;
    private boolean installed;
    private final String type;
    private final String description;
    private final String backgroundColor;
    private final String imageLink;

    /**
     * Creates a new Addon instance
     *
     * @param id the id of the add-on
     * @param type the type id of the add-on
     * @param label the label of the add-on
     * @param version the version of the add-on
     * @param link the link to find more information about the add-on (can be null)
     * @param installed true, if the add-on is installed, false otherwise
     */
    public Addon(String id, String type, String label, String version, String link, boolean installed) {
        this(id, type, label, version, link, installed, null, null, null);
    }

    /**
     * Creates a new Addon instance
     *
     * @param id the id of the add-on
     * @param type the type id of the add-on
     * @param label the label of the add-on
     * @param version the version of the add-on
     * @param description the detailed description of the add-on (may be null)
     * @param backgroundColor for displaying the add-on (may be null)
     * @param link the link to find more information about the add-on (may be null)
     * @param imageLink the link to an image (png/svg) (may be null)
     * @param installed true, if the add-on is installed, false otherwise
     */
    public Addon(String id, String type, String label, String version, String link, boolean installed,
            String description, String backgroundColor, String imageLink) {
        this.id = id;
        this.label = label;
        this.version = version;
        this.description = description;
        this.backgroundColor = backgroundColor;
        this.link = link;
        this.imageLink = imageLink;
        this.installed = installed;
        this.type = type;
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
     * The version of the add-on
     */
    public String getVersion() {
        return version;
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
     * The description of the add-on
     */
    public String getDescription() {
        return description;
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
