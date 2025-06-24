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
package org.openhab.core.automation.module.script.providersupport.shared;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.providersupport.internal.ProviderRegistry;
import org.openhab.core.common.registry.Registry;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;

/**
 * The {@link ProviderItemChannelLinkRegistry} is implementing a {@link Registry} to provide a comfortable way to
 * provide {@link ItemChannelLink}s from scripts without worrying about the need to remove them again when the script is
 * unloaded.
 * Nonetheless, using the {@link #addPermanent(ItemChannelLink)} method it is still possible to them permanently.
 * <p>
 * Use a new instance of this class for each {@link javax.script.ScriptEngine}.
 * <p>
 * ATTENTION: This class does not provide the same methods as {@link ItemChannelLinkRegistry}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class ProviderItemChannelLinkRegistry implements Registry<ItemChannelLink, String>, ProviderRegistry {
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;

    private final Set<String> uids = new HashSet<>();

    private final ScriptedItemChannelLinkProvider scriptedProvider;

    public ProviderItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry,
            ScriptedItemChannelLinkProvider scriptedProvider) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.scriptedProvider = scriptedProvider;
    }

    @Override
    public void addRegistryChangeListener(RegistryChangeListener<ItemChannelLink> listener) {
        itemChannelLinkRegistry.addRegistryChangeListener(listener);
    }

    @Override
    public Collection<ItemChannelLink> getAll() {
        return itemChannelLinkRegistry.getAll();
    }

    @Override
    public Stream<ItemChannelLink> stream() {
        return itemChannelLinkRegistry.stream();
    }

    @Override
    public @Nullable ItemChannelLink get(String key) {
        return itemChannelLinkRegistry.get(key);
    }

    @Override
    public void removeRegistryChangeListener(RegistryChangeListener<ItemChannelLink> listener) {
        itemChannelLinkRegistry.removeRegistryChangeListener(listener);
    }

    @Override
    public ItemChannelLink add(ItemChannelLink element) {
        String uid = element.getUID();
        // Check for item->channel link already existing here because the item->channel link might exist in a different
        // provider, so we need to
        // check the registry and not only the provider itself
        if (get(uid) != null) {
            throw new IllegalArgumentException(
                    "Cannot add item->channel link, because an item->channel link with same UID (" + uid
                            + ") already exists.");
        }

        itemChannelLinkRegistry.add(element);
        uids.add(uid);

        return element;
    }

    /**
     * Adds an {@link ItemChannelLink} permanently to the registry.
     * This {@link ItemChannelLink} will be kept in the registry even if the script is unloaded
     * 
     * @param element the {@link ItemChannelLink} to be added (must not be null)
     * @return the added {@link ItemChannelLink}
     */
    public ItemChannelLink addPermanent(ItemChannelLink element) {
        return itemChannelLinkRegistry.add(element);
    }

    @Override
    public @Nullable ItemChannelLink update(ItemChannelLink element) {
        if (uids.contains(element.getUID())) {
            return scriptedProvider.update(element);
        }
        return itemChannelLinkRegistry.update(element);
    }

    @Override
    public @Nullable ItemChannelLink remove(String key) {
        if (uids.contains(key)) {
            return scriptedProvider.remove(key);
        }
        return itemChannelLinkRegistry.remove(key);
    }

    public int removeLinksForThing(ThingUID thingUID) {
        int removedLinks = 0;
        Collection<ItemChannelLink> itemChannelLinks = getAll();
        for (ItemChannelLink itemChannelLink : itemChannelLinks) {
            if (itemChannelLink.getLinkedUID().getThingUID().equals(thingUID)) {
                this.remove(itemChannelLink.getUID());
                removedLinks++;
            }
        }
        return removedLinks;
    }

    public int removeLinksForItem(String itemName) {
        int removedLinks = 0;
        Collection<ItemChannelLink> itemChannelLinks = getAll();
        for (ItemChannelLink itemChannelLink : itemChannelLinks) {
            if (itemChannelLink.getItemName().equals(itemName)) {
                this.remove(itemChannelLink.getUID());
                removedLinks++;
            }
        }
        return removedLinks;
    }

    public int purge() {
        List<String> toRemove = itemChannelLinkRegistry.getOrphanLinks().keySet().stream().map(ItemChannelLink::getUID)
                .filter(i -> scriptedProvider.get(i) != null).toList();

        toRemove.forEach(this::remove);

        return toRemove.size() + itemChannelLinkRegistry.purge();
    }

    @Override
    public void removeAllAddedByScript() {
        for (String uid : uids) {
            scriptedProvider.remove(uid);
        }
        uids.clear();
    }
}
