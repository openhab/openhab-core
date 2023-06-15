/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.service.ReadyService;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.UID;
import org.openhab.core.thing.link.events.LinkEventFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * {@link ItemChannelLinkRegistry} tracks all {@link ItemChannelLinkProvider}s
 * and aggregates all {@link ItemChannelLink}s.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Markus Rathgeb - Linked items returns only existing items
 * @author Markus Rathgeb - Rewrite collection handling to improve performance
 */
@NonNullByDefault
@Component(immediate = true, service = ItemChannelLinkRegistry.class)
public class ItemChannelLinkRegistry extends AbstractLinkRegistry<ItemChannelLink, ItemChannelLinkProvider> {

    private final ThingRegistry thingRegistry;
    private final ItemRegistry itemRegistry;

    @Activate
    public ItemChannelLinkRegistry(final @Reference ThingRegistry thingRegistry,
            final @Reference ItemRegistry itemRegistry) {
        super(ItemChannelLinkProvider.class);
        this.thingRegistry = thingRegistry;
        this.itemRegistry = itemRegistry;
    }

    /**
     * Returns a set of bound channels for the given item name.
     *
     * @param itemName item name
     * @return an unmodifiable set of bound channels for the given item name
     */
    public Set<ChannelUID> getBoundChannels(final String itemName) {
        return getLinks(itemName).stream().map(link -> link.getLinkedUID()).collect(Collectors.toSet());
    }

    @Override
    public Set<String> getLinkedItemNames(final UID uid) {
        return super.getLinkedItemNames(uid).stream().filter(itemName -> itemRegistry.get(itemName) != null)
                .collect(Collectors.toSet());
    }

    /**
     * Returns a set of bound items for the given channel UID.
     *
     * @param uid channel UID
     * @return an unmodifiable set of bound items for the given channel UID
     */
    public Set<Item> getLinkedItems(final UID uid) {
        return ((Stream<Item>) super.getLinkedItemNames(uid).stream().map(itemName -> itemRegistry.get(itemName))
                .filter(Objects::nonNull)).collect(Collectors.toSet());
    }

    /**
     * Returns a set of bound things for the given item name.
     *
     * @param itemName item name
     * @return an unmodifiable set of bound things for the given item name
     */
    public Set<Thing> getBoundThings(final String itemName) {
        return ((Stream<Thing>) getBoundChannels(itemName).stream()
                .map(channelUID -> thingRegistry.get(channelUID.getThingUID())).filter(Objects::nonNull))
                .collect(Collectors.toSet());
    }

    @Reference
    protected void setManagedProvider(final ManagedItemChannelLinkProvider provider) {
        super.setManagedProvider(provider);
    }

    protected void unsetManagedProvider(final ManagedItemChannelLinkProvider provider) {
        super.unsetManagedProvider(provider);
    }

    @Override
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setEventPublisher(final EventPublisher eventPublisher) {
        super.setEventPublisher(eventPublisher);
    }

    @Override
    @Reference
    protected void setReadyService(ReadyService readyService) {
        super.setReadyService(readyService);
    }

    @Override
    protected void unsetReadyService(ReadyService readyService) {
        super.unsetReadyService(readyService);
    }

    @Override
    protected void unsetEventPublisher(final EventPublisher eventPublisher) {
        super.unsetEventPublisher(eventPublisher);
    }

    /**
     * Remove all links related to a thing
     *
     * @param thingUID the UID of the thing
     * @return the number of removed links
     */
    public int removeLinksForThing(final ThingUID thingUID) {
        ManagedItemChannelLinkProvider managedProvider = (ManagedItemChannelLinkProvider) getManagedProvider()
                .orElseThrow(() -> new IllegalStateException("ManagedProvider is not available"));
        return managedProvider.removeLinksForThing(thingUID);
    }

    /**
     * Remove all links related to an item
     *
     * @param itemName the name of the item
     * @return the number of removed links
     */
    public int removeLinksForItem(final String itemName) {
        ManagedItemChannelLinkProvider managedProvider = (ManagedItemChannelLinkProvider) getManagedProvider()
                .orElseThrow(() -> new IllegalStateException("ManagedProvider is not available"));
        return managedProvider.removeLinksForItem(itemName);
    }

    /**
     * Remove all orphaned (item or channel missing) links
     *
     * @return the number of removed links
     */
    public int purge() {
        ManagedProvider<ItemChannelLink, String> managedProvider = getManagedProvider()
                .orElseThrow(() -> new IllegalStateException("ManagedProvider is not available"));

        Set<String> allItems = itemRegistry.stream().map(Item::getName).collect(Collectors.toSet());
        Set<ChannelUID> allChannels = thingRegistry.stream().map(Thing::getChannels).flatMap(List::stream)
                .map(Channel::getUID).collect(Collectors.toSet());

        Set<String> toRemove = managedProvider.getAll().stream()
                .filter(link -> !allItems.contains(link.getItemName()) || !allChannels.contains(link.getLinkedUID()))
                .map(ItemChannelLink::getUID).collect(Collectors.toSet());

        toRemove.forEach(managedProvider::remove);

        return toRemove.size();
    }

    @Override
    protected void notifyListenersAboutAddedElement(final ItemChannelLink element) {
        super.notifyListenersAboutAddedElement(element);
        postEvent(LinkEventFactory.createItemChannelLinkAddedEvent(element));
    }

    @Override
    protected void notifyListenersAboutRemovedElement(final ItemChannelLink element) {
        super.notifyListenersAboutRemovedElement(element);
        postEvent(LinkEventFactory.createItemChannelLinkRemovedEvent(element));
    }

    @Override
    protected void notifyListenersAboutUpdatedElement(final ItemChannelLink oldElement, final ItemChannelLink element) {
        super.notifyListenersAboutUpdatedElement(oldElement, element);
        // it is not needed to send an event, because links can not be updated
    }
}
