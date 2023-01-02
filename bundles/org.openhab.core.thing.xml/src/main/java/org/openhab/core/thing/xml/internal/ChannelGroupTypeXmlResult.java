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
package org.openhab.core.thing.xml.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeBuilder;
import org.openhab.core.thing.type.ChannelGroupTypeUID;

import com.thoughtworks.xstream.converters.ConversionException;

/**
 * The {@link ChannelGroupTypeXmlResult} is an intermediate XML conversion result object which
 * contains all parts of a {@link ChannelGroupType} object.
 * <p>
 * To create a concrete {@link ChannelGroupType} object, the method {@link #toChannelGroupType(Map)} must be called.
 *
 * @author Michael Grammling - Initial contribution
 * @author Chris Jackson - Updated to support channel properties
 * @author Christoph Weitkamp - Removed "advanced" attribute
 */
@NonNullByDefault
public class ChannelGroupTypeXmlResult {

    private final ChannelGroupTypeUID channelGroupTypeUID;
    private final String label;
    private final @Nullable String description;
    private final @Nullable String category;
    private final @Nullable List<ChannelXmlResult> channelTypeReferences;

    public ChannelGroupTypeXmlResult(ChannelGroupTypeUID channelGroupTypeUID, String label,
            @Nullable String description, @Nullable String category,
            @Nullable List<ChannelXmlResult> channelTypeReferences) {
        this.channelGroupTypeUID = channelGroupTypeUID;
        this.label = label;
        this.description = description;
        this.category = category;
        this.channelTypeReferences = channelTypeReferences;
    }

    public ChannelGroupTypeUID getUID() {
        return channelGroupTypeUID;
    }

    protected @Nullable List<ChannelDefinition> toChannelDefinitions(
            @Nullable List<ChannelXmlResult> channelTypeReferences) throws ConversionException {
        List<ChannelDefinition> channelTypeDefinitions = null;

        if (channelTypeReferences != null && !channelTypeReferences.isEmpty()) {
            channelTypeDefinitions = new ArrayList<>(channelTypeReferences.size());
            for (ChannelXmlResult channelTypeReference : channelTypeReferences) {
                channelTypeDefinitions
                        .add(channelTypeReference.toChannelDefinition(channelGroupTypeUID.getBindingId()));
            }
        }

        return channelTypeDefinitions;
    }

    public ChannelGroupType toChannelGroupType() throws ConversionException {
        ChannelGroupTypeBuilder builder = ChannelGroupTypeBuilder.instance(channelGroupTypeUID, label);
        String description = this.description;
        if (description != null) {
            builder.withDescription(description);
        }
        String category = this.category;
        if (category != null) {
            builder.withCategory(category);
        }
        List<ChannelDefinition> channelDefinitions = toChannelDefinitions(channelTypeReferences);
        if (channelDefinitions != null) {
            builder.withChannelDefinitions(channelDefinitions);
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return "ChannelGroupTypeXmlResult [channelGroupTypeUID=" + channelGroupTypeUID + ", label=" + label
                + ", description=" + description + ", category=" + category + ", channelTypeReferences="
                + channelTypeReferences + "]";
    }
}
