/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.thing.events;

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEvent;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.StateDescription;

/**
 * {@link ChannelDescriptionChangedEvent}s will be delivered through the openHAB event bus if the
 * {@link CommandDescription} or {@link StateDescription} of a channel has changed. Instances must be created with the
 * {@link ThingEventFactory}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ChannelDescriptionChangedEvent extends AbstractEvent {

    public enum CommonChannelDescriptionField {
        ALL,
        COMMAND_OPTIONS,
        PATTERN,
        STATE_OPTIONS
    };

    /**
     * The channel description changed event type.
     */
    public static final String TYPE = ChannelDescriptionChangedEvent.class.getSimpleName();

    /**
     * The changed field.
     */
    private CommonChannelDescriptionField field;

    /**
     * The channel which triggered the event.
     */
    private final ChannelUID channelUID;

    /**
     * A {@link Set} of linked item names.
     */
    private final Set<String> linkedItemNames;

    /**
     * The new value (represented as JSON string).
     */
    private final String value;

    /**
     * The old value (represented as JSON string).
     */
    private final @Nullable String oldValue;

    /**
     * Creates a new instance.
     *
     * @param topic the topic
     * @param payload the payload
     * @param field the changed field
     * @param channelUID the {@link ChannelUID}
     * @param linkedItemNames a {@link Set} of linked item names
     * @param value the new value (represented as JSON string)
     * @param oldValue the old value represented as JSON string)
     */
    protected ChannelDescriptionChangedEvent(String topic, String payload, CommonChannelDescriptionField field,
            ChannelUID channelUID, Set<String> linkedItemNames, String value, @Nullable String oldValue) {
        super(topic, payload, null);
        this.field = field;
        this.channelUID = channelUID;
        this.linkedItemNames = linkedItemNames;
        this.value = value;
        this.oldValue = oldValue;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Gets the changed field.
     *
     * @return the changed field
     */
    public CommonChannelDescriptionField getField() {
        return field;
    }

    /**
     * Gets the {@link ChannelUID}.
     *
     * @return the {@link ChannelUID}
     */
    public ChannelUID getChannelUID() {
        return channelUID;
    }

    /**
     * Gets the linked item names.
     *
     * @return a {@link Set} of linked item names
     */
    public Set<String> getLinkedItemNames() {
        return linkedItemNames;
    }

    /**
     * Gets the new value (represented as JSON string).
     *
     * @return the new value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Gets the old value (represented as JSON string).
     *
     * @return the old value.
     */
    public @Nullable String getOldValue() {
        return oldValue;
    }

    @Override
    public String toString() {
        return String.format(
                "Description for field '%s' of channel '%s' changed from '%s' to '%s' for linked items: %s", field,
                channelUID, oldValue, value, linkedItemNames.stream().collect(Collectors.joining(",", "[", "]")));
    }
}
