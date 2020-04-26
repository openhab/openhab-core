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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
public class ChannelType extends AbstractDescriptionType {

    private final boolean advanced;
    private final String itemType;
    private final ChannelKind kind;
    private final Set<String> tags;
    private final String category;
    private final StateDescription state;
    private final CommandDescription commandDescription;
    private final EventDescription event;
    private final URI configDescriptionURI;
    private final AutoUpdatePolicy autoUpdatePolicy;

    /**
     * @deprecated Use the {@link ChannelTypeBuilder} instead.
     */
    @Deprecated
    public ChannelType(ChannelTypeUID uid, boolean advanced, String itemType, String label, String description,
            String category, Set<String> tags, StateDescription state, URI configDescriptionURI) {
        this(uid, advanced, itemType, ChannelKind.STATE, label, description, category, tags, state, null,
                configDescriptionURI);
    }

    /**
     * @deprecated Use the {@link ChannelTypeBuilder} instead.
     */
    @Deprecated
    public ChannelType(ChannelTypeUID uid, boolean advanced, String itemType, ChannelKind kind, String label,
            String description, String category, Set<String> tags, StateDescription state, EventDescription event,
            URI configDescriptionURI) throws IllegalArgumentException {
        this(uid, advanced, itemType, kind, label, description, category, tags, state, event, configDescriptionURI,
                null);
    }

    /**
     * Creates a new instance of a "write-only" {@link ChannelType} with command options. The purpose of this
     * {@link ChannelType} is to send command to a device without updating the state of the corresponding channel.
     * E.g. activate a special device mode which is not represented as a definitive state.
     *
     * @param uid the unique identifier which identifies this Channel type within
     *            the overall system (must neither be null, nor empty)
     * @param advanced true if this channel type contains advanced features, otherwise false
     * @param itemType the item type of this Channel type, e.g. {@code ColorItem} (must neither be null nor empty)
     * @param label the human readable label for the according type
     *            (must neither be null nor empty)
     * @param description the human readable description for the according type
     *            (could be null or empty)
     * @param category the category of this Channel type, e.g. {@code TEMPERATURE} (could be null or empty)
     * @param tags all tags of this {@link ChannelType}, e.g. {@code Alarm} (could be null or empty)
     * @param commandDescription a {@link CommandDescription} which should be rendered as push-buttons. The command
     *            values will be send to the channel from this {@link ChannelType}.
     * @param configDescriptionURI the link to the concrete ConfigDescription (could be null)
     * @param autoUpdatePolicy the {@link AutoUpdatePolicy} to use.
     * @throws IllegalArgumentException if the UID or the item type is null or empty,
     *             or the meta information is null
     */
    public ChannelType(ChannelTypeUID uid, boolean advanced, String itemType, String label, String description,
            String category, Set<String> tags, CommandDescription commandDescription, URI configDescriptionURI,
            AutoUpdatePolicy autoUpdatePolicy) {
        this(uid, advanced, itemType, ChannelKind.STATE, label, description, category, tags, null, commandDescription,
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
     * @param state the restrictions of an item state which gives information how to interpret it
     *            (could be null)
     * @param configDescriptionURI the link to the concrete ConfigDescription (could be null)
     * @param autoUpdatePolicy the {@link AutoUpdatePolicy} to use.
     * @throws IllegalArgumentException if the UID or the item type is null or empty,
     *             or the meta information is null
     */
    public ChannelType(ChannelTypeUID uid, boolean advanced, String itemType, ChannelKind kind, String label,
            String description, String category, Set<String> tags, StateDescription state, EventDescription event,
            URI configDescriptionURI, AutoUpdatePolicy autoUpdatePolicy) throws IllegalArgumentException {
        this(uid, advanced, itemType, kind, label, description, category, tags, state, null, event,
                configDescriptionURI, autoUpdatePolicy);
    }

    private ChannelType(ChannelTypeUID uid, boolean advanced, String itemType, ChannelKind kind, String label,
            String description, String category, Set<String> tags, StateDescription state,
            CommandDescription commandDescription, EventDescription event, URI configDescriptionURI,
            AutoUpdatePolicy autoUpdatePolicy) throws IllegalArgumentException {
        super(uid, label, description);

        if (kind == null) {
            throw new IllegalArgumentException("Kind must not be null!");
        }

        if (kind == ChannelKind.STATE && (itemType == null || itemType.isBlank())) {
            throw new IllegalArgumentException("If the kind is 'state', the item type must be set!");
        }
        if (kind == ChannelKind.TRIGGER && itemType != null) {
            throw new IllegalArgumentException("If the kind is 'trigger', the item type must not be set!");
        }

        this.itemType = itemType;
        this.kind = kind;
        this.configDescriptionURI = configDescriptionURI;

        if (tags != null) {
            this.tags = Collections.unmodifiableSet(new HashSet<>(tags));
        } else {
            this.tags = Collections.unmodifiableSet(new HashSet<>(0));
        }

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
    public String getItemType() {
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
     * @return all tags of this Channel type, e.g. {@code Alarm} (not null, could be empty)
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
     * @return the link to a concrete ConfigDescription (could be null)
     */
    public URI getConfigDescriptionURI() {
        return this.configDescriptionURI;
    }

    /**
     * Returns the restrictions of an item state which gives information how to interpret it.
     *
     * @return the restriction of an item state which gives information how to interpret it
     *         (could be null)
     */
    public StateDescription getState() {
        return state;
    }

    /**
     * Returns informations about the supported events.
     *
     * @return the event information
     *         (could be null)
     */
    public EventDescription getEvent() {
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
     * @return the category of this Channel type, e.g. {@code TEMPERATURE} (could be null or empty)
     */
    public String getCategory() {
        return category;
    }

    /**
     * Returns the {@link AutoUpdatePolicy} of for channels of this type.
     *
     * @return the {@link AutoUpdatePolicy}
     */
    public AutoUpdatePolicy getAutoUpdatePolicy() {
        return autoUpdatePolicy;
    }

    public CommandDescription getCommandDescription() {
        return commandDescription;
    }
}
