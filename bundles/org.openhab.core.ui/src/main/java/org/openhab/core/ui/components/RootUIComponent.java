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
package org.openhab.core.ui.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.config.core.dto.ConfigDescriptionDTO;

/**
 * A root component is a special type of {@link Component} at the root of the hierarchy.
 * It has a number of specific parameters, a set of tags, a timestamp, some configurable
 * parameters ("props") and is identifiable by its UID (generally a GUID).
 *
 * @author Yannick Schaus - Initial contribution
 */
@NonNullByDefault
public class RootUIComponent extends UIComponent implements Identifiable<String> {
    String uid;

    Set<String> tags = new HashSet<String>();

    ConfigDescriptionDTO props;

    @Nullable
    Date timestamp;

    /**
     * Constructs a root component.
     *
     * @param name the name of the UI component to render the card on client frontends, ie. "HbCard"
     */
    public RootUIComponent(String name) {
        super(name);
        this.uid = UUID.randomUUID().toString();
        this.props = new ConfigDescriptionDTO(null, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Constructs a root component with a specific UID.
     *
     * @param uid the UID of the new card
     * @param name the name of the UI component to render the card on client frontends, ie. "HbCard"
     */
    public RootUIComponent(String uid, String name) {
        super(name);
        this.uid = uid;
        this.props = new ConfigDescriptionDTO(null, new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public String getUID() {
        return uid;
    }

    /**
     * Gets the set of tags attached to the component
     *
     * @return the card tags
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Gets the timestamp of the component
     *
     * @return the timestamp
     */
    public @Nullable Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the specified timestamp of the component
     *
     * @param date the timestamp
     */
    public void setTimestamp(Date date) {
        this.timestamp = date;
    }

    /**
     * Updates the timestamp of the component to the current date & time.
     */
    public void updateTimestamp() {
        this.timestamp = new Date();
    }

    /**
     * Returns whether the component has a certain tag
     *
     * @param tag the tag to check
     * @return true if the component is tagged with the specified tag
     */
    public boolean hasTag(String tag) {
        return (tags != null && tags.contains(tag));
    }

    /**
     * Adds a tag to the component
     *
     * @param tag the tag to add
     */
    public void addTag(String tag) {
        this.tags.add(tag);
    }

    /**
     * Adds several tags to the component
     *
     * @param tags the tags to add
     */
    public void addTags(Collection<String> tags) {
        this.tags.addAll(tags);
    }

    /**
     * Adds several tags to the component
     *
     * @param tags the tags to add
     */
    public void addTags(String... tags) {
        this.tags.addAll(Arrays.asList(tags));
    }

    /**
     * Removes a tag on a component
     *
     * @param tag the tag to remove
     */
    public void removeTag(String tag) {
        this.tags.remove(tag);
    }

    /**
     * Removes all tags on the component
     */
    public void removeAllTags() {
        this.tags.clear();
    }

    /**
     * Gets the configurable parameters ("props") of the component
     *
     * @return the configurable parameters
     */
    public ConfigDescriptionDTO getProps() {
        return props;
    }

    /**
     * Sets the configurable parameters ("props") of the component
     *
     * @param props the configurable parameters
     */
    public void setProps(ConfigDescriptionDTO props) {
        this.props = props;
    }
}
