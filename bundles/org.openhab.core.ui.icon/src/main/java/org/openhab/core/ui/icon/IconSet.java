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
package org.openhab.core.ui.icon;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This is a bean that provides some meta-information about available icon sets.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class IconSet {

    /**
     * Data format of icons
     */
    public enum Format {
        PNG,
        SVG
    }

    private String id;
    private String label;
    private String description;
    private Set<Format> formats;

    /**
     * Construct a new pojo.
     *
     * @param id the icon set id
     * @param label a short label text
     * @param description a description text
     * @param formats a set of formats that are supported
     */
    public IconSet(String id, String label, String description, Set<Format> formats) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.formats = new HashSet<>(formats);
    }

    /**
     * Returns the id of the icon set.
     *
     * @return id of the icon set
     */
    public String getId() {
        return id;
    }

    /**
     * Returns a label for the icon set.
     *
     * @return a short label text
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns a description for the icon set.
     *
     * @return a description text
     */
    public String getDescription() {
        return description;
    }

    /**
     * Provides information about supported file formats of this set.
     *
     * @return a set of formats
     */
    public Set<Format> getFormats() {
        return Collections.unmodifiableSet(formats);
    }

}
