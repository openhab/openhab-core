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
package org.openhab.core.thing.internal;

import java.net.URI;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionAliasProvider;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Provides a proxy for thing & channel configuration descriptions.
 *
 * If a thing config description is requested, the provider will look up the thing/channel type
 * to get the configURI and the config description for it. If there is a corresponding {@link ConfigOptionProvider}, it
 * will be used to get updated options.
 *
 * @author Chris Jackson - Initial contribution
 * @author Chris Jackson - Updated to separate thing type from thing name
 * @author Simon Kaufmann - Added support for channel config descriptions, turned into alias handler
 */
@Component
@NonNullByDefault
public class ThingConfigDescriptionAliasProvider implements ConfigDescriptionAliasProvider {

    private @NonNullByDefault({}) ThingRegistry thingRegistry;
    private @NonNullByDefault({}) ThingTypeRegistry thingTypeRegistry;
    private @NonNullByDefault({}) ChannelTypeRegistry channelTypeRegistry;

    @Reference
    protected void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    protected void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    @Reference
    protected void setThingTypeRegistry(ThingTypeRegistry thingTypeRegistry) {
        this.thingTypeRegistry = thingTypeRegistry;
    }

    protected void unsetThingTypeRegistry(ThingTypeRegistry thingTypeRegistry) {
        this.thingTypeRegistry = null;
    }

    @Reference
    protected void setChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = channelTypeRegistry;
    }

    protected void unsetChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = null;
    }

    @Override
    public @Nullable URI getAlias(URI uri) {
        // If this is not a concrete thing, then return
        if (uri.getScheme() == null) {
            return null;
        }

        switch (uri.getScheme()) {
            case "thing":
                return getThingConfigDescriptionURI(uri);
            case "channel":
                return getChannelConfigDescriptionURI(uri);
            default:
                return null;
        }
    }

    private @Nullable URI getThingConfigDescriptionURI(URI uri) {
        // First, get the thing type so we get the generic config descriptions
        ThingUID thingUID = new ThingUID(uri.getSchemeSpecificPart());
        Thing thing = thingRegistry.get(thingUID);
        if (thing == null) {
            return null;
        }

        ThingType thingType = thingTypeRegistry.getThingType(thing.getThingTypeUID());
        if (thingType == null) {
            return null;
        }

        // Get the config description URI for this thing type
        URI configURI = thingType.getConfigDescriptionURI();
        return configURI;
    }

    private @Nullable URI getChannelConfigDescriptionURI(URI uri) {
        String stringUID = uri.getSchemeSpecificPart();
        if (uri.getFragment() != null) {
            stringUID = stringUID + "#" + uri.getFragment();
        }
        ChannelUID channelUID = new ChannelUID(stringUID);
        ThingUID thingUID = channelUID.getThingUID();

        // First, get the thing so we get access to the channel type via the channel
        Thing thing = thingRegistry.get(thingUID);
        if (thing == null) {
            return null;
        }

        Channel channel = thing.getChannel(channelUID.getId());
        if (channel == null) {
            return null;
        }

        ChannelType channelType = channelTypeRegistry.getChannelType(channel.getChannelTypeUID());
        if (channelType == null) {
            return null;
        }

        // Get the config description URI for this channel type
        URI configURI = channelType.getConfigDescriptionURI();
        return configURI;
    }

}
