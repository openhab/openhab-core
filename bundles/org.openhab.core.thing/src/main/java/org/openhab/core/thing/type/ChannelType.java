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

import java.net.URI;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.thing.Channel;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.EventDescription;
import org.openhab.core.types.StateDescription;

/**
 * The {@link ChannelType} describes a concrete type of a {@link Channel}.
 * <p>
 * This description is used as template definition for the creation of the according concrete {@link Channel} object.
 * <p>
 * <b>Hint:</b> This class is immutable.
 *
 * @author Michael Grammling - Initial contribution
 * @author Henning Treu - add command options
 */
@NonNullByDefault
public class ChannelType extends AbstractDescriptionType {

    private final boolean advanced;
    private final @Nullable String itemType;
    private final ChannelKind kind;
    private final Set<String> tags;
    private final @Nullable String category;
    private final @Nullable StateDescription state;
    private final @Nullable CommandDescription commandDescription;
    private final @Nullable EventDescription event;
    private final @Nullable URI configDescriptionURI;
    private final @Nullable AutoUpdatePolicy autoUpdatePolicy;

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @deprecated Use the {@link ChannelTypeBuilder#trigger(ChannelTypeUID, String)} instead.
     */
    @Deprecated
    public ChannelType(ChannelTypeUID uid, boolean advanced, String label, @Nullable String description,
            @Nullable String category, @Nullable Set<String> tags, @Nullable EventDescription event,
            @Nullable URI configDescriptionURI) throws IllegalArgumentException {
        this(uid, advanced, null, ChannelKind.TRIGGER, label, description, category, tags, null, null, event,
                configDescriptionURI, null);
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @deprecated Use the {@link ChannelTypeBuilder#state(ChannelTypeUID, String, String)} instead.
     */
    @Deprecated
    public ChannelType(ChannelTypeUID uid, boolean advanced, String itemType, String label,
            @Nullable String description, @Nullable String category, @Nullable Set<String> tags,
            @Nullable StateDescription state, @Nullable CommandDescription commandDescription,
            @Nullable URI configDescriptionURI, @Nullable AutoUpdatePolicy autoUpdatePolicy)
            throws IllegalArgumentException {
        this(uid, advanced, itemType, ChannelKind.STATE, label, description, category, tags, state, commandDescription,
                null, configDescriptionURI, autoUpdatePolicy);
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param uid the unique identifier which identifies this Channel type within
     *            the overall system (must neither be null, nor empty)
     * @param advanced true if this channel type contains advanced features, otherwise false
     * @param itemType the item type of this Channel type, e.g. {@code ColorItem}
     * @param kind the channel kind.
     * @param label the human readable label for the according type
     *            (must neither be null nor empty)
     * @param description the human readable description for the according type
     *            (could be null or empty)
     * @param category the category of this Channel type, e.g. {@code TEMPERATURE} (could be null or empty)
     * @param tags all tags of this {@link ChannelType}, e.g. {@code Alarm} (could be null or empty)
     * @param state a {@link StateDescription} of an items state which gives information how to interpret it.
     * @param commandDescription a {@link CommandDescription} which should be rendered as push-buttons. The command
     *            values will be send to the channel from this {@link ChannelType}.
     * @param configDescriptionURI the link to the concrete ConfigDescription (could be null)
     * @param autoUpdatePolicy the {@link AutoUpdatePolicy} to use.
     * @throws IllegalArgumentException if the UID or the item type is null or empty,
     *             or the meta information is null
     */
    ChannelType(ChannelTypeUID uid, boolean advanced, @Nullable String itemType, ChannelKind kind, String label,
            @Nullable String description, @Nullable String category, @Nullable Set<String> tags,
            @Nullable StateDescription state, @Nullable CommandDescription commandDescription,
            @Nullable EventDescription event, @Nullable URI configDescriptionURI,
            @Nullable AutoUpdatePolicy autoUpdatePolicy) throws IllegalArgumentException {
        super(uid, label, description);

        if (kind == ChannelKind.STATE && (itemType == null || itemType.isBlank())) {
            throw new IllegalArgumentException("If the kind is 'state', the item type must be set!");
        }
        if (kind == ChannelKind.TRIGGER && itemType != null) {
            throw new IllegalArgumentException("If the kind is 'trigger', the item type must not be set!");
        }

        this.itemType = itemType;
        this.kind = kind;
        this.configDescriptionURI = configDescriptionURI;

        this.tags = tags == null ? Set.of() : Set.copyOf(tags);
        this.advanced = advanced;
        this.category = category;
        this.state = state;
        this.commandDescription = commandDescription;
        this.event = event;
        this.autoUpdatePolicy = autoUpdatePolicy;
    }

    @Override
    public ChannelTypeUID getUID() {
        return (ChannelTypeUID) super.getUID();
    }

    /**
     * Returns the item type of this {@link ChannelType}, e.g. {@code ColorItem}.
     *
     * @return the item type of this Channel type, e.g. {@code ColorItem}. Can be null if the channel is a trigger
     *         channel.
     */
    public @Nullable String getItemType() {
        return this.itemType;
    }

    /**
     * Returns the kind of this {@link ChannelType}, e.g. {@code STATE}.
     *
     * @return the kind of this Channel type, e.g. {@code STATE}.
     */
    public ChannelKind getKind() {
        return kind;
    }

    /**
     * Returns all tags of this {@link ChannelType}, e.g. {@code Alarm}.
     *
     * @return all tags of this Channel type, e.g. {@code Alarm}
     */
    public Set<String> getTags() {
        return this.tags;
    }

    @Override
    public String toString() {
        return super.getUID().toString();
    }

    /**
     * Returns the link to a concrete {@link ConfigDescription}.
     *
     * @return the link to a concrete ConfigDescription
     */
    public @Nullable URI getConfigDescriptionURI() {
        return this.configDescriptionURI;
    }

    /**
     * Returns the {@link StateDescription} of an items state which gives information how to interpret it.
     *
     * @return the {@link StateDescription}
     */
    public @Nullable StateDescription getState() {
        return state;
    }

    /**
     * Returns informations about the supported events.
     *
     * @return the event information. Can be null if the channel is a state channel.
     */
    public @Nullable EventDescription getEvent() {
        return event;
    }

    /**
     * Returns {@code true} if this channel type contains advanced functionalities
     * which should be typically not shown in the basic view of user interfaces,
     * otherwise {@code false}.
     *
     * @return true if this channel type contains advanced functionalities, otherwise false
     */
    public boolean isAdvanced() {
        return advanced;
    }

    /**
     * Returns the category of this {@link ChannelType}, e.g. {@code TEMPERATURE}.
     *
     * @return the category of this Channel type, e.g. {@code TEMPERATURE}
     */
    public @Nullable String getCategory() {
        return category;
    }

    /**
     * Returns the {@link AutoUpdatePolicy} of for channels of this type.
     *
     * @return the {@link AutoUpdatePolicy}. Can be null if the channel is a trigger
     *         channel.
     */
    public @Nullable AutoUpdatePolicy getAutoUpdatePolicy() {
        return autoUpdatePolicy;
    }

    /**
     * Returns the {@link CommandDescription} which should be rendered as push-buttons.
     *
     * @return the {@link CommandDescription}
     */
    public @Nullable CommandDescription getCommandDescription() {
        return commandDescription;
    }
}
