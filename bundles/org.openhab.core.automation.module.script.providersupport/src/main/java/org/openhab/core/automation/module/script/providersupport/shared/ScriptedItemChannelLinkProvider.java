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
package org.openhab.core.automation.module.script.providersupport.shared;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link ItemChannelLinkProvider} keeps {@link ItemChannelLink}s provided by scripts during runtime.
 * This ensures that {@link ItemChannelLink}s are not kept on reboot, but have to be provided by the scripts again.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { ScriptedItemChannelLinkProvider.class, ItemChannelLinkProvider.class })
public class ScriptedItemChannelLinkProvider extends AbstractProvider<ItemChannelLink>
        implements ItemChannelLinkProvider, ManagedProvider<ItemChannelLink, String> {
    private final Logger logger = LoggerFactory.getLogger(ScriptedItemChannelLinkProvider.class);
    private final Map<String, ItemChannelLink> itemChannelLinks = new HashMap<>();

    @Override
    public Collection<ItemChannelLink> getAll() {
        return itemChannelLinks.values();
    }

    @Override
    public @Nullable ItemChannelLink get(String key) {
        return itemChannelLinks.get(key);
    }

    @Override
    public void add(ItemChannelLink itemChannelLink) {
        if (get(itemChannelLink.getUID()) != null) {
            throw new IllegalArgumentException(
                    "Cannot add item->channel link, because an item->channel link with same UID ("
                            + itemChannelLink.getUID() + ") already exists.");
        }
        itemChannelLinks.put(itemChannelLink.getUID(), itemChannelLink);

        notifyListenersAboutAddedElement(itemChannelLink);
    }

    @Override
    public @Nullable ItemChannelLink update(ItemChannelLink itemChannelLink) {
        ItemChannelLink oldItemChannelLink = itemChannelLinks.get(itemChannelLink.getUID());
        if (oldItemChannelLink != null) {
            itemChannelLinks.put(itemChannelLink.getUID(), itemChannelLink);
            notifyListenersAboutUpdatedElement(oldItemChannelLink, itemChannelLink);
        } else {
            logger.warn("Cannot update item->channel link with UID '{}', because it does not exist.",
                    itemChannelLink.getUID());
        }
        return oldItemChannelLink;
    }

    @Override
    public @Nullable ItemChannelLink remove(String key) {
        ItemChannelLink itemChannelLink = itemChannelLinks.remove(key);
        if (itemChannelLink != null) {
            notifyListenersAboutRemovedElement(itemChannelLink);
        }
        return itemChannelLink;
    }
}
