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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A {@link ChannelGroupType} builder.
 *
 * @author Christoph Weitkamp - Initial contribution
 * @author Christoph Weitkamp - Removed "advanced" attribute
 */
@NonNullByDefault
public class ChannelGroupTypeBuilder {

    private @Nullable List<ChannelDefinition> channelDefinitions;
    private @Nullable String category;
    private @Nullable String description;

    private final ChannelGroupTypeUID channelGroupTypeUID;
    private final String label;

    /**
     * Create an instance of a ChannelGroupTypeBuilder for {@link ChannelGroupType}s
     *
     * @param channelGroupTypeUID UID of the {@link ChannelGroupType}
     * @param label Label for the {@link ChannelGroupType}
     * @return ChannelGroupTypeBuilder for {@link ChannelGroupType}s
     */
    public static ChannelGroupTypeBuilder instance(ChannelGroupTypeUID channelGroupTypeUID, String label) {
        if (channelGroupTypeUID == null) {
            throw new IllegalArgumentException("ChannelGroupTypeUID must be set.");
        }

        if (StringUtils.isEmpty(label)) {
            throw new IllegalArgumentException("Label for a ChannelGroupType must not be empty.");
        }

        return new ChannelGroupTypeBuilder(channelGroupTypeUID, label);
    }

    private ChannelGroupTypeBuilder(ChannelGroupTypeUID channelGroupTypeUID, String label) {
        this.channelGroupTypeUID = channelGroupTypeUID;
        this.label = label;
    }

    /**
     * Build the {@link ChannelGroupType} with the given values
     *
     * @return the created {@link ChannelGroupType}
     */
    public ChannelGroupType build() {
        return new ChannelGroupType(channelGroupTypeUID, label, description, category, channelDefinitions);
    }

    /**
     * Sets the description for the {@link ChannelGroupType}
     *
     * @param description Description for the {@link ChannelGroupType}
     * @return this Builder
     */
    public ChannelGroupTypeBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the category for the {@link ChannelGroupType}
     *
     * @param category Category for the {@link ChannelGroupType}
     * @return this Builder
     */
    public ChannelGroupTypeBuilder withCategory(String category) {
        this.category = category;
        return this;
    }

    /**
     * Sets the channels for the {@link ChannelGroupType}
     *
     * @param channelDefinitions The channels this {@link ChannelGroupType} provides (could be null or empty)
     * @return this Builder
     */
    public ChannelGroupTypeBuilder withChannelDefinitions(List<ChannelDefinition> channelDefinitions) {
        this.channelDefinitions = channelDefinitions;
        return this;
    }
}
