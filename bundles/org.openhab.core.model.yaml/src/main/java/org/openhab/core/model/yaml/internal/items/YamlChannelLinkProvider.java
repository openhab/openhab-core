/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.items;

import static org.openhab.core.model.yaml.YamlModelUtils.isIsolatedModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.ItemProvider;
import org.openhab.core.model.yaml.internal.util.YamlElementUtils;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkProvider;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class serves as a provider for all item channel links that is found within YAML files.
 * It is filled with content by the {@link YamlItemProvider}, which cannot itself implement the
 * {@link ItemChannelLinkProvider} interface as it already implements {@link ItemProvider},
 * which would lead to duplicate methods.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { ItemChannelLinkProvider.class, YamlChannelLinkProvider.class })
public class YamlChannelLinkProvider extends AbstractProvider<ItemChannelLink> implements ItemChannelLinkProvider {

    private final Logger logger = LoggerFactory.getLogger(YamlChannelLinkProvider.class);

    // Map the channel links to each channel UID and then to each item name and finally to each model name
    private Map<String, Map<String, Map<ChannelUID, ItemChannelLink>>> itemsChannelLinksMap = new ConcurrentHashMap<>();

    @Override
    public Collection<ItemChannelLink> getAll() {
        // Ignore isolated models
        return itemsChannelLinksMap.keySet().stream().filter(name -> !isIsolatedModel(name))
                .map(name -> itemsChannelLinksMap.getOrDefault(name, Map.of())).flatMap(m -> m.values().stream())
                .flatMap(m -> m.values().stream()).toList();
    }

    public Collection<ItemChannelLink> getAllFromModel(String modelName) {
        return itemsChannelLinksMap.getOrDefault(modelName, Map.of()).values().stream()
                .flatMap(m -> m.values().stream()).toList();
    }

    public void updateItemChannelLinks(String modelName, String itemName, Map<String, Configuration> channelLinks) {
        Map<String, Map<ChannelUID, ItemChannelLink>> channelLinksMap = Objects
                .requireNonNull(itemsChannelLinksMap.computeIfAbsent(modelName, k -> new ConcurrentHashMap<>()));
        // Create a HashMap with an initial capacity of 2 (the default is 16) to save memory because most items have
        // only one channel. A capacity of 2 is enough to avoid resizing the HashMap in most cases, whereas 1 would
        // trigger a resize as soon as one element is added.
        Map<ChannelUID, ItemChannelLink> links = Objects
                .requireNonNull(channelLinksMap.computeIfAbsent(itemName, k -> new ConcurrentHashMap<>(2)));

        Set<ChannelUID> linksToBeRemoved = new HashSet<>(links.keySet());

        for (Map.Entry<String, Configuration> entry : channelLinks.entrySet()) {
            String channelUID = entry.getKey();
            Configuration configuration = entry.getValue();

            ChannelUID channelUIDObject;
            try {
                channelUIDObject = new ChannelUID(channelUID);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid channel UID '{}' in channel link for item '{}'!", channelUID, itemName, e);
                continue;
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

            linksToBeRemoved.remove(channelUIDObject);
            ItemChannelLink oldLink = links.get(channelUIDObject);
            if (oldLink == null) {
                links.put(channelUIDObject, itemChannelLink);
                logger.debug("model {} added channel link {}", modelName, itemChannelLink.getUID());
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutAddedElement(itemChannelLink);
                }
            } else if (!YamlElementUtils.equalsConfig(configuration.getProperties(),
                    oldLink.getConfiguration().getProperties())) {
                links.put(channelUIDObject, itemChannelLink);
                logger.debug("model {} updated channel link {}", modelName, itemChannelLink.getUID());
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutUpdatedElement(oldLink, itemChannelLink);
                }
            }
        }

        linksToBeRemoved.forEach(uid -> {
            ItemChannelLink link = links.remove(uid);
            if (link != null) {
                logger.debug("model {} removed channel link {}", modelName, link.getUID());
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutRemovedElement(link);
                }
            }
        });
        if (links.isEmpty()) {
            channelLinksMap.remove(itemName);
        }
        if (channelLinksMap.isEmpty()) {
            itemsChannelLinksMap.remove(modelName);
        }
    }
}
