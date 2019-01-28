/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.model.thing.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.registry.AbstractProvider;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkProvider;
import org.eclipse.smarthome.model.item.BindingConfigParseException;
import org.eclipse.smarthome.model.item.BindingConfigReader;
import org.osgi.service.component.annotations.Component;

/**
 * {@link GenericItemChannelLinkProvider} link items to channel by reading bindings with type "channel".
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Alex Tugarev - Added parsing of multiple Channel UIDs
 *
 */
@Component(immediate = true, service = { ItemChannelLinkProvider.class, BindingConfigReader.class })
public class GenericItemChannelLinkProvider extends AbstractProvider<ItemChannelLink>
        implements BindingConfigReader, ItemChannelLinkProvider {

    /** caches binding configurations. maps itemNames to {@link BindingConfig}s */
    protected Map<String, Set<ItemChannelLink>> itemChannelLinkMap = new ConcurrentHashMap<>();

    /**
     * stores information about the context of items. The map has this content
     * structure: context -> Set of Item names
     */
    protected Map<String, Set<String>> contextMap = new ConcurrentHashMap<>();

    private Set<String> previousItemNames;

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
        ChannelUID channelUIDObject = null;
        try {
            channelUIDObject = new ChannelUID(channelUID);
        } catch (IllegalArgumentException e) {
            throw new BindingConfigParseException(e.getMessage());
        }
        ItemChannelLink itemChannelLink = new ItemChannelLink(itemName, channelUIDObject, configuration);

        Set<String> itemNames = contextMap.get(context);
        if (itemNames == null) {
            itemNames = new HashSet<>();
            contextMap.put(context, itemNames);
        }
        itemNames.add(itemName);
        if (previousItemNames != null) {
            previousItemNames.remove(itemName);
        }

        Set<ItemChannelLink> links = itemChannelLinkMap.get(itemName);
        if (links == null) {
            itemChannelLinkMap.put(itemName, links = new HashSet<>());
        }
        if (!links.contains(itemChannelLink)) {
            links.add(itemChannelLink);
            notifyListenersAboutAddedElement(itemChannelLink);
        } else {
            notifyListenersAboutUpdatedElement(itemChannelLink, itemChannelLink);
        }
    }

    @Override
    public void startConfigurationUpdate(String context) {
        if (previousItemNames != null) {
            logger.warn("There already is an update transaction for generic item channel links. Continuing anyway.");
        }
        Set<String> previous = contextMap.get(context);
        previousItemNames = previous != null ? new HashSet<>(previous) : Collections.emptySet();
    }

    @Override
    public void stopConfigurationUpdate(String context) {
        if (previousItemNames != null) {
            for (String itemName : previousItemNames) {
                // we remove all binding configurations that were not processed
                Set<ItemChannelLink> links = itemChannelLinkMap.remove(itemName);
                if (links != null) {
                    for (ItemChannelLink removedItemChannelLink : links) {
                        notifyListenersAboutRemovedElement(removedItemChannelLink);
                    }
                }
            }
            if (contextMap.get(context) != null) {
                contextMap.get(context).removeAll(previousItemNames);
            }
            previousItemNames = null;
        }
    }

    @Override
    public Collection<ItemChannelLink> getAll() {
        return itemChannelLinkMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

}
