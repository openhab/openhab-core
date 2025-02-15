/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.model.thing.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.model.item.BindingConfigParseException;
import org.openhab.core.model.item.BindingConfigReader;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkProvider;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link GenericItemChannelLinkProvider} link items to channel by reading bindings with type "channel".
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Alex Tugarev - Added parsing of multiple Channel UIDs
 */
@NonNullByDefault
@Component(immediate = true, service = { ItemChannelLinkProvider.class, BindingConfigReader.class })
public class GenericItemChannelLinkProvider extends AbstractProvider<ItemChannelLink>
        implements BindingConfigReader, ItemChannelLinkProvider {

    private final Logger logger = LoggerFactory.getLogger(GenericItemChannelLinkProvider.class);
    /** caches binding configurations. maps itemNames to {@link ItemChannelLink}s */
    protected Map<String, Map<ChannelUID, ItemChannelLink>> itemChannelLinkMap = new ConcurrentHashMap<>();

    private Map<String, Set<ChannelUID>> addedItemChannels = new ConcurrentHashMap<>();

    /**
     * stores information about the context of items. The map has this content
     * structure: context -> Set of Item names
     */
    protected Map<String, Set<String>> contextMap = new ConcurrentHashMap<>();

    private @Nullable Set<String> previousItemNames;

    @Override
    public String getBindingType() {
        return "channel";
    }

    @Override
    public void validateItemType(String itemType, String bindingConfig) throws BindingConfigParseException {
        // all item types are allowed
    }

    @Override
    public void processBindingConfiguration(String context, String itemType, String itemName, String bindingConfig,
            Configuration configuration) throws BindingConfigParseException {
        String[] uids = bindingConfig.split(",");
        if (uids.length == 0) {
            throw new BindingConfigParseException(
                    "At least one Channel UID should be provided: <bindingID>.<thingTypeId>.<thingId>.<channelId>");
        }
        for (String uid : uids) {
            createItemChannelLink(context, itemName, uid.trim(), configuration);
        }
    }

    private void createItemChannelLink(String context, String itemName, String channelUID, Configuration configuration)
            throws BindingConfigParseException {
        ChannelUID channelUIDObject;
        try {
            channelUIDObject = new ChannelUID(channelUID);
        } catch (IllegalArgumentException e) {
            throw new BindingConfigParseException(e.getMessage());
        }

        // Fix the configuration in case a profile is defined without any scope
        if (configuration.containsKey("profile") && configuration.get("profile") instanceof String profile
                && profile.indexOf(":") == -1) {
            String fullProfile = ProfileTypeUID.SYSTEM_SCOPE + ":" + profile;
            configuration.put("profile", fullProfile);
            logger.info(
                    "Profile '{}' for channel '{}' is missing the scope prefix, assuming the correct UID is '{}'. Check your configuration.",
                    profile, channelUID, fullProfile);
        }

        ItemChannelLink itemChannelLink = new ItemChannelLink(itemName, channelUIDObject, configuration);

        Set<String> itemNames = Objects.requireNonNull(contextMap.computeIfAbsent(context, k -> new HashSet<>()));
        itemNames.add(itemName);
        if (previousItemNames != null) {
            previousItemNames.remove(itemName);
        }

        // Create a HashMap with an initial capacity of 2 (the default is 16) to save memory because most items have
        // only one channel. A capacity of 2 is enough to avoid resizing the HashMap in most cases, whereas 1 would
        // trigger a resize as soon as one element is added.
        Map<ChannelUID, ItemChannelLink> links = Objects
                .requireNonNull(itemChannelLinkMap.computeIfAbsent(itemName, k -> new HashMap<>(2)));

        ItemChannelLink oldLink = links.put(channelUIDObject, itemChannelLink);
        if (oldLink == null) {
            notifyListenersAboutAddedElement(itemChannelLink);
        } else {
            notifyListenersAboutUpdatedElement(oldLink, itemChannelLink);
        }
        addedItemChannels.computeIfAbsent(itemName, k -> new HashSet<>(2)).add(channelUIDObject);
    }

    @Override
    public void startConfigurationUpdate(String context) {
        if (previousItemNames != null) {
            logger.warn("There already is an update transaction for generic item channel links. Continuing anyway.");
        }
        Set<String> previous = contextMap.get(context);
        previousItemNames = previous != null ? new HashSet<>(previous) : new HashSet<>();
    }

    @Override
    public void stopConfigurationUpdate(String context) {
        final Set<String> previousItemNames = this.previousItemNames;
        this.previousItemNames = null;
        if (previousItemNames == null) {
            return;
        }
        for (String itemName : previousItemNames) {
            // we remove all binding configurations that were not processed
            Map<ChannelUID, ItemChannelLink> links = itemChannelLinkMap.remove(itemName);
            if (links != null) {
                links.values().forEach(this::notifyListenersAboutRemovedElement);
            }
        }
        Optional.ofNullable(contextMap.get(context)).ifPresent(ctx -> ctx.removeAll(previousItemNames));

        addedItemChannels.forEach((itemName, addedChannelUIDs) -> {
            Map<ChannelUID, ItemChannelLink> links = itemChannelLinkMap.getOrDefault(itemName, Map.of());
            Set<ChannelUID> removedChannelUIDs = new HashSet<>(links.keySet());
            removedChannelUIDs.removeAll(addedChannelUIDs);
            removedChannelUIDs.forEach(removedChannelUID -> {
                ItemChannelLink link = links.remove(removedChannelUID);
                notifyListenersAboutRemovedElement(link);
            });
        });
        addedItemChannels.clear();
    }

    @Override
    public Collection<ItemChannelLink> getAll() {
        return itemChannelLinkMap.values().stream().flatMap(m -> m.values().stream()).toList();
    }
}
