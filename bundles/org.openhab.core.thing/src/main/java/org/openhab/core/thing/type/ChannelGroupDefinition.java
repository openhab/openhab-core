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
package org.openhab.core.thing.type;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;

/**
 * The {@link ChannelGroupDefinition} class defines a {@link ChannelGroupType} of a {@link ThingType}.
 * <p>
 * A {@link ChannelGroupType} is part of a {@link Thing} that represents a set of channels (functionalities) of it.
 * <p>
 * <b>Hint:</b> This class is immutable.
 *
 * @author Michael Grammling - Initial contribution
 * @author Dennis Nobel - Introduced ChannelTypeRegistry and channel type references
 */
@NonNullByDefault
public class ChannelGroupDefinition {

    private String id;
    private ChannelGroupTypeUID typeUID;
    private final @Nullable String label;
    private final @Nullable String description;

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param id the identifier of the channel group (must not be empty)
     * @param typeUID the type UID of the channel group
     * @param label the label for the channel group to override ChannelGroupType
     * @param description the description for the channel group to override ChannelGroupType
     * @throws IllegalArgumentException if the ID is empty
     */
    public ChannelGroupDefinition(String id, ChannelGroupTypeUID typeUID, @Nullable String label,
            @Nullable String description) throws IllegalArgumentException {
        if (id.isEmpty()) {
            throw new IllegalArgumentException("The ID must not be empty!");
        }

        this.id = id;
        this.typeUID = typeUID;
        this.label = label;
        this.description = description;
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param id the identifier of the channel group (must not be empty)
     * @param typeUID the type UID of the channel group
     * @throws IllegalArgumentException if the ID is empty
     */
    public ChannelGroupDefinition(String id, ChannelGroupTypeUID typeUID) throws IllegalArgumentException {
        this(id, typeUID, null, null);
    }

    /**
     * Returns the identifier of the channel group.
     *
     * @return the identifier of the channel group (not empty)
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the type UID of the channel group.
     *
     * @return the type UID of the channel group
     */
    public ChannelGroupTypeUID getTypeUID() {
        return typeUID;
    }

    /**
     * Returns the label (if set).
     * If no label is set, getLabel will return null and the default label for the {@link ChannelGroupType} is used.
     *
     * @return the label for the channel group.
     */
    public @Nullable String getLabel() {
        return label;
    }

    /**
     * Returns the description (if set).
     * If no description is set, getDescription will return null and the default description for the
     * {@link ChannelGroupType} is used.
     *
     * @return the description for the channel group.
     */
    public @Nullable String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "ChannelGroupDefinition [id=" + id + ", typeUID=" + typeUID + "]";
    }
}
