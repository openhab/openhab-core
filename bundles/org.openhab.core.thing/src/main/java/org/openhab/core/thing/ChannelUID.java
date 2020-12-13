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
package org.openhab.core.thing;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * {@link ChannelUID} represents a unique identifier for channels.
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Jochen Hiller - Bugfix 455434: added default constructor
 * @author Dennis Nobel - Added channel group id
 * @author Kai Kreuzer - Changed creation of channels to not require a thing type
 * @author Christoph Weitkamp - Changed pattern for validating last segment to contain either a single `#` or none
 */
@NonNullByDefault
public class ChannelUID extends UID {

    public static final String CHANNEL_SEGMENT_PATTERN = "[\\w-]*|[\\w-]*#[\\w-]*";
    public static final String CHANNEL_GROUP_SEPARATOR = "#";

    /**
     * Default constructor in package scope only. Will allow to instantiate this
     * class by reflection. Not intended to be used for normal instantiation.
     */
    ChannelUID() {
        super();
    }

    /**
     * Parses a {@link ChannelUID} for a given string. The UID must be in the format
     * 'bindingId:segment:segment:...'.
     *
     * @param channelUid uid in form a string
     */
    public ChannelUID(String channelUid) {
        super(channelUid);
    }

    /**
     * @param thingUID the unique identifier of the thing the channel belongs to
     * @param id the channel's id
     */
    public ChannelUID(ThingUID thingUID, String id) {
        super(toSegments(thingUID, null, id));
    }

    /**
     * @param channelGroupUID the unique identifier of the channel group the channel belongs to
     * @param id the channel's id
     */
    public ChannelUID(ChannelGroupUID channelGroupUID, String id) {
        super(toSegments(channelGroupUID.getThingUID(), channelGroupUID.getId(), id));
    }

    /**
     * @param thingUID the unique identifier of the thing the channel belongs to
     * @param groupId the channel's group id
     * @param id the channel's id
     */
    public ChannelUID(ThingUID thingUID, String groupId, String id) {
        super(toSegments(thingUID, groupId, id));
    }

    private static List<String> toSegments(ThingUID thingUID, @Nullable String groupId, String id) {
        List<String> ret = new ArrayList<>(thingUID.getAllSegments());
        ret.add(getChannelId(groupId, id));
        return ret;
    }

    private static String getChannelId(@Nullable String groupId, String id) {
        return groupId != null ? groupId + CHANNEL_GROUP_SEPARATOR + id : id;
    }

    /**
     * Returns the id.
     *
     * @return id
     */
    public String getId() {
        List<String> segments = getAllSegments();
        return segments.get(segments.size() - 1);
    }

    /**
     * Returns the id without the group id.
     *
     * @return id id without group id
     */
    public String getIdWithoutGroup() {
        if (!isInGroup()) {
            return getId();
        } else {
            return getId().split(CHANNEL_GROUP_SEPARATOR)[1];
        }
    }

    public boolean isInGroup() {
        return getId().contains(CHANNEL_GROUP_SEPARATOR);
    }

    /**
     * Returns the group id.
     *
     * @return group id or null if channel is not in a group
     */
    public @Nullable String getGroupId() {
        return isInGroup() ? getId().split(CHANNEL_GROUP_SEPARATOR)[0] : null;
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 4;
    }

    @Override
    protected void validateSegment(String segment, int index, int length) {
        if (index < length - 1) {
            super.validateSegment(segment, index, length);
        } else {
            if (!segment.matches(CHANNEL_SEGMENT_PATTERN)) {
                throw new IllegalArgumentException(String.format(
                        "UID segment '%s' contains invalid characters. The last segment of the channel UID must match the pattern '%s'.",
                        segment, CHANNEL_SEGMENT_PATTERN));
            }
        }
    }

    /**
     * Returns the thing UID
     *
     * @return the thing UID
     */
    public ThingUID getThingUID() {
        List<String> allSegments = getAllSegments();
        return new ThingUID(allSegments.subList(0, allSegments.size() - 1).toArray(new String[allSegments.size() - 1]));
    }
}
