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
package org.eclipse.smarthome.core.thing.link;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.UID;
import org.eclipse.smarthome.core.thing.link.events.LinkEventFactory;
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
 *
 */
@Component(immediate = true, service = ItemChannelLinkRegistry.class)
public class ItemChannelLinkRegistry extends AbstractLinkRegistry<ItemChannelLink, ItemChannelLinkProvider> {

    private ThingRegistry thingRegistry;
    private ItemRegistry itemRegistry;

    public ItemChannelLinkRegistry() {
        super(ItemChannelLinkProvider.class);
    }

    /**
     * Returns a set of bound channels for the given item name.
     *
     * @param itemName item name
     * @return an unmodifiable set of bound channels for the given item name
     */
    public Set<ChannelUID> getBoundChannels(final String itemName) {
        return getLinks(itemName).parallelStream().map(link -> link.getLinkedUID()).collect(Collectors.toSet());
    }

    @Override
    public Set<String> getLinkedItemNames(final UID uid) {
        return super.getLinkedItemNames(uid).parallelStream().filter(itemName -> itemRegistry.get(itemName) != null)
                .collect(Collectors.toSet());
    }

    public Set<Item> getLinkedItems(final UID uid) {
        return super.getLinkedItemNames(uid).parallelStream().map(itemName -> itemRegistry.get(itemName))
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * Returns a set of bound things for the given item name.
     *
     * @param itemName item name
     * @return an unmodifiable set of bound things for the given item name
     */
    public Set<Thing> getBoundThings(final String itemName) {
        return getBoundChannels(itemName).parallelStream()
                .map(channelUID -> thingRegistry.get(channelUID.getThingUID())).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Reference
    protected void setThingRegistry(final ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    protected void unsetThingRegistry(final ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    @Reference
    protected void setItemRegistry(final ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    protected void unsetItemRegistry(final ItemRegistry itemRegistry) {
        this.itemRegistry = null;
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
    protected void unsetEventPublisher(final EventPublisher eventPublisher) {
        super.unsetEventPublisher(eventPublisher);
    }

    public void removeLinksForThing(final ThingUID thingUID) {
        ((ManagedItemChannelLinkProvider) getManagedProvider()
                .orElseThrow(() -> new IllegalStateException("ManagedProvider is not available")))
                        .removeLinksForThing(thingUID);
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
