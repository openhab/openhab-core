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
package org.openhab.core.thing.type;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ChannelGroupType} contains a list of {@link ChannelDefinition}s and further meta information such as label
 * and description, which are generally used by user interfaces.
 * <p>
 * This type can be used for Things which offers multiple functionalities which belong all together.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Michael Grammling - Initial contribution
 * @author Christoph Weitkamp - Removed "advanced" attribute
 */
@NonNullByDefault
public class ChannelGroupType extends AbstractDescriptionType {

    private final List<ChannelDefinition> channelDefinitions;
    private final @Nullable String category;

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param uid the unique identifier which identifies this channel group type within the
     * @param label the human readable label for the according type
     * @param description the human readable description for the according type
     * @param category the category of this channel group type, e.g. Temperature
     * @param channelDefinitions the channel definitions this channel group forms
     * @throws IllegalArgumentException if the UID is null, or the label is null or empty
     */
    ChannelGroupType(ChannelGroupTypeUID uid, String label, @Nullable String description, @Nullable String category,
            @Nullable List<ChannelDefinition> channelDefinitions) throws IllegalArgumentException {
        super(uid, label, description);

        this.category = category;
        this.channelDefinitions = channelDefinitions == null ? Collections.emptyList()
                : Collections.unmodifiableList(channelDefinitions);
    }

    /**
     * Returns the channel definitions this {@link ChannelGroupType} provides.
     * <p>
     * The returned list is immutable.
     *
     * @return the channels this Thing type provides
     */
    public List<ChannelDefinition> getChannelDefinitions() {
        return channelDefinitions;
    }

    public @Nullable String getCategory() {
        return category;
    }

    @Override
    public ChannelGroupTypeUID getUID() {
        return (ChannelGroupTypeUID) super.getUID();
    }

    @Override
    public String toString() {
        return super.getUID().toString();
    }
}
