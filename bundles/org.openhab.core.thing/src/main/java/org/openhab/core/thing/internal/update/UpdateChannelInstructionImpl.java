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
package org.openhab.core.thing.internal.update;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.internal.ThingFactoryHelper;
import org.openhab.core.thing.internal.update.dto.XmlAddChannel;
import org.openhab.core.thing.internal.update.dto.XmlUpdateChannel;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UpdateChannelInstructionImpl} implements a {@link ThingUpdateInstruction} that updates a channel of a
 * thing.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class UpdateChannelInstructionImpl implements ThingUpdateInstruction {
    private final Logger logger = LoggerFactory.getLogger(UpdateChannelInstructionImpl.class);
    private final boolean removeOldChannel;
    private final int thingTypeVersion;
    private final boolean preserveConfig;
    private final List<String> groupIds;
    private final String channelId;
    private final ChannelTypeUID channelTypeUid;
    private final @Nullable String label;
    private final @Nullable String description;
    private final @Nullable List<String> tags;
    private final ChannelTypeRegistry channelTypeRegistry;
    private final ConfigDescriptionRegistry configDescriptionRegistry;

    UpdateChannelInstructionImpl(int thingTypeVersion, XmlUpdateChannel updateChannel,
            ChannelTypeRegistry channelTypeRegistry, ConfigDescriptionRegistry configDescriptionRegistry) {
        this.removeOldChannel = true;
        this.thingTypeVersion = thingTypeVersion;
        this.channelId = updateChannel.getId();
        this.channelTypeUid = new ChannelTypeUID(updateChannel.getType());
        String rawGroupIds = updateChannel.getGroupIds();
        this.groupIds = rawGroupIds != null ? Arrays.asList(rawGroupIds.split(",")) : List.of();
        this.label = updateChannel.getLabel();
        this.description = updateChannel.getDescription();
        this.tags = updateChannel.getTags();
        this.preserveConfig = updateChannel.isPreserveConfiguration();
        this.channelTypeRegistry = channelTypeRegistry;
        this.configDescriptionRegistry = configDescriptionRegistry;
    }

    UpdateChannelInstructionImpl(int thingTypeVersion, XmlAddChannel addChannel,
            ChannelTypeRegistry channelTypeRegistry, ConfigDescriptionRegistry configDescriptionRegistry) {
        this.removeOldChannel = false;
        this.thingTypeVersion = thingTypeVersion;
        this.channelId = addChannel.getId();
        this.channelTypeUid = new ChannelTypeUID(addChannel.getType());
        String rawGroupIds = addChannel.getGroupIds();
        this.groupIds = rawGroupIds != null ? Arrays.asList(rawGroupIds.split(",")) : List.of();
        this.label = addChannel.getLabel();
        this.description = addChannel.getDescription();
        this.tags = addChannel.getTags();
        this.preserveConfig = false;
        this.channelTypeRegistry = channelTypeRegistry;
        this.configDescriptionRegistry = configDescriptionRegistry;
    }

    @Override
    public int getThingTypeVersion() {
        return thingTypeVersion;
    }

    @Override
    public void perform(Thing thing, ThingBuilder thingBuilder) {
        logger.debug("Applying {} to thing '{}'", this, thing.getUID());
        if (groupIds.isEmpty()) {
            doChannel(thing, thingBuilder, new ChannelUID(thing.getUID(), channelId));
        } else {
            groupIds.forEach(groupId -> doChannel(thing, thingBuilder,
                    new ChannelUID(thing.getUID(), groupId + ChannelUID.CHANNEL_GROUP_SEPARATOR + channelId)));
        }
    }

    private void doChannel(Thing thing, ThingBuilder thingBuilder, ChannelUID affectedChannelUid) {
        if (removeOldChannel) {
            thingBuilder.withoutChannel(affectedChannelUid);
        }

        ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUid);
        if (channelType == null) {
            logger.error("Failed to create channel '{}' because channel type '{}' could not be found.",
                    affectedChannelUid, channelTypeUid);
            return;
        }

        ChannelBuilder channelBuilder = ThingFactoryHelper.createChannelBuilder(affectedChannelUid, channelType,
                configDescriptionRegistry);

        if (preserveConfig) {
            Channel oldChannel = thing.getChannel(affectedChannelUid);
            if (oldChannel != null) {
                channelBuilder.withConfiguration(oldChannel.getConfiguration());
                channelBuilder.withDefaultTags(oldChannel.getDefaultTags());
            }
        }

        if (label != null) {
            channelBuilder.withLabel(Objects.requireNonNull(label));
        }
        if (description != null) {
            channelBuilder.withDescription(Objects.requireNonNull(description));
        }
        if (tags != null) {
            channelBuilder.withDefaultTags(Set.copyOf(Objects.requireNonNull(tags)));
        }

        thingBuilder.withChannel(channelBuilder.build());
    }

    @Override
    public String toString() {
        return "UpdateChannelInstructionImpl{" + "removeOldChannel=" + removeOldChannel + ", thingTypeVersion="
                + thingTypeVersion + ", preserveConfig=" + preserveConfig + ", groupIds=" + groupIds + ", channelId='"
                + channelId + "'" + ", channelTypeUid=" + channelTypeUid + ", label='" + label + "'" + ", description='"
                + description + "'" + ", tags=" + tags + "}";
    }
}
