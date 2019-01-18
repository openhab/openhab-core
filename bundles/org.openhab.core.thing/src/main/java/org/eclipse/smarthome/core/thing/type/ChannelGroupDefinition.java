/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.thing.type;

import org.eclipse.smarthome.core.thing.Thing;

/**
 * The {@link ChannelGroupDefinition} class defines a {@link ChannelGroupType} of a {@link ThingType}.
 * <p>
 * A {@link ChannelGroupType} is part of a {@link Thing} that represents a set of channels (functionalities) of it.
 * <p>
 * <b>Hint:</b> This class is immutable.
 *
 * @author Michael Grammling - Initial Contribution
 * @author Dennis Nobel - Introduced ChannelTypeRegistry and channel type references
 */
public class ChannelGroupDefinition {

    private String id;
    private ChannelGroupTypeUID typeUID;
    private final String label;
    private final String description;

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param id the identifier of the channel group (must neither be null nor empty)
     * @param typeUID the type UID of the channel group (must not be null)
     * @param label the label for the channel group to override ChannelGroupType (could be null)
     * @param description the description for the channel group to override ChannelGroupType (could be null)
     * @throws IllegalArgumentException if the ID is null or empty, or the type is null
     */
    public ChannelGroupDefinition(String id, ChannelGroupTypeUID typeUID, String label, String description)
            throws IllegalArgumentException {
        if ((id == null) || (id.isEmpty())) {
            throw new IllegalArgumentException("The ID must neither be null nor empty!");
        }

        if (typeUID == null) {
            throw new IllegalArgumentException("The channel group type UID must not be null");
        }

        this.id = id;
        this.typeUID = typeUID;
        this.label = label;
        this.description = description;
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param id the identifier of the channel group (must neither be null nor empty)
     * @param typeUID the type UID of the channel group (must not be null)
     * @throws IllegalArgumentException if the ID is null or empty, or the type is null
     */
    public ChannelGroupDefinition(String id, ChannelGroupTypeUID typeUID) throws IllegalArgumentException {
        this(id, typeUID, null, null);
    }

    /**
     * Returns the identifier of the channel group.
     *
     * @return the identifier of the channel group (neither null, nor empty)
     */
    public String getId() {
        return this.id;
    }

    /**
     * Returns the type UID of the channel group.
     *
     * @return the type UID of the channel group (not null)
     */
    public ChannelGroupTypeUID getTypeUID() {
        return this.typeUID;
    }

    /**
     * Returns the label (if set).
     * If no label is set, getLabel will return null and the default label for the {@link ChannelGroupType} is used.
     *
     * @return the label for the channel group. Can be null.
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * Returns the description (if set).
     * If no description is set, getDescription will return null and the default description for the
     * {@link ChannelGroupType} is used.
     *
     * @return the description for the channel group. Can be null.
     */
    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return "ChannelGroupDefinition [id=" + id + ", typeUID=" + typeUID + "]";
    }

}
