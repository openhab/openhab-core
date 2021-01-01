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
package org.openhab.core.thing.link;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.Item;
import org.openhab.core.thing.ChannelUID;

/**
 * {@link ItemChannelLink} defines a link between an {@link Item} and a {@link Channel}.
 *
 * @author Dennis Nobel - Initial contribution, Added getIDFor method
 * @author Jochen Hiller - Bugfix 455434: added default constructor, object is now mutable
 * @author Simon Kaufmann - added configuration
 */
@NonNullByDefault
public class ItemChannelLink extends AbstractLink {

    private final @NonNullByDefault({}) ChannelUID channelUID;
    private final Configuration configuration;

    /**
     * Default constructor in package scope only. Will allow to instantiate this
     * class by reflection. Not intended to be used for normal instantiation.
     */
    ItemChannelLink() {
        super();
        this.channelUID = null;
        this.configuration = new Configuration();
    }

    public ItemChannelLink(String itemName, ChannelUID channelUID) {
        this(itemName, channelUID, new Configuration());
    }

    public ItemChannelLink(String itemName, ChannelUID channelUID, Configuration configuration) {
        super(itemName);
        this.channelUID = channelUID;
        this.configuration = configuration;
    }

    @Override
    public ChannelUID getLinkedUID() {
        return channelUID;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof ItemChannelLink) {
            ItemChannelLink link = (ItemChannelLink) obj;
            return super.equals(obj) && configuration.equals(link.getConfiguration());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() * configuration.hashCode();
    }
}
