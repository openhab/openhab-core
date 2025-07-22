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
package org.openhab.core.thing.link;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ActiveItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.semantics.Point;
import org.openhab.core.semantics.Property;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.semantics.Tag;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static final String USE_TAGS = "useTags";

    private final Logger logger = LoggerFactory.getLogger(ItemChannelLinkRegistry.class);

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
        return super.getLinkedItemNames(uid).stream().map(itemName -> itemRegistry.get(itemName))
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * Returns a set of bound things for the given item name.
     *
     * @param itemName item name
     * @return an unmodifiable set of bound things for the given item name
     */
    public Set<Thing> getBoundThings(final String itemName) {
        return getBoundChannels(itemName).stream().map(channelUID -> thingRegistry.get(channelUID.getThingUID()))
                .filter(Objects::nonNull).collect(Collectors.toSet());
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
     * Remove all orphaned (item or channel missing) links that are editable
     *
     * @see #getOrphanLinks()
     * @return the number of removed links
     */
    public int purge() {
        ManagedProvider<ItemChannelLink, String> managedProvider = getManagedProvider()
                .orElseThrow(() -> new IllegalStateException("ManagedProvider is not available"));

        List<String> toRemove = getOrphanLinks().keySet().stream().map(ItemChannelLink::getUID)
                .filter(i -> managedProvider.get(i) != null).toList();

        toRemove.forEach(managedProvider::remove);

        return toRemove.size();
    }

    /**
     * Get all orphan links (item or channel missing)
     *
     * @see #purge()
     *
     * @return a map with orphan links as key and reason they are broken as value
     */
    public Map<ItemChannelLink, ItemChannelLinkProblem> getOrphanLinks() {
        Collection<Item> items = itemRegistry.getItems();
        Collection<Thing> things = thingRegistry.getAll();

        Map<ItemChannelLink, ItemChannelLinkProblem> results = new HashMap<>();

        Collection<ChannelUID> channelUIDS = things.stream().map(Thing::getChannels).flatMap(List::stream)
                .map(Channel::getUID).collect(Collectors.toSet());
        Collection<String> itemNames = items.stream().map(Item::getName).collect(Collectors.toSet());

        getAll().forEach(itemChannelLink -> {
            if (!channelUIDS.contains(itemChannelLink.getLinkedUID())) {
                if (!itemNames.contains(itemChannelLink.getItemName())) {
                    results.put(itemChannelLink, ItemChannelLinkProblem.ITEM_AND_THING_CHANNEL_MISSING);
                } else {
                    results.put(itemChannelLink, ItemChannelLinkProblem.THING_CHANNEL_MISSING);
                }
            } else if (!itemNames.contains(itemChannelLink.getItemName())) {
                results.put(itemChannelLink, ItemChannelLinkProblem.ITEM_MISSING);
            }
        });
        return results;
    }

    @Override
    protected void notifyListenersAboutAddedElement(final ItemChannelLink element) {
        assignChannelDefaultTags(element);
        super.notifyListenersAboutAddedElement(element);
        postEvent(LinkEventFactory.createItemChannelLinkAddedEvent(element));
    }

    @Override
    protected void notifyListenersAboutRemovedElement(final ItemChannelLink element) {
        removeChannelDefaultTags(element);
        super.notifyListenersAboutRemovedElement(element);
        postEvent(LinkEventFactory.createItemChannelLinkRemovedEvent(element));
    }

    @Override
    protected void notifyListenersAboutUpdatedElement(final ItemChannelLink oldElement, final ItemChannelLink element) {
        assignChannelDefaultTags(element);
        super.notifyListenersAboutUpdatedElement(oldElement, element);
        // it is not needed to send an event, because links can not be updated
    }

    public enum ItemChannelLinkProblem {
        THING_CHANNEL_MISSING,
        ITEM_MISSING,
        ITEM_AND_THING_CHANNEL_MISSING;
    }

    /**
     * If the item does not already have a Point and/or a Property tag and if the linked channel
     * has 'useTags=true' then assign the default tags from that channel to the respective item.
     * By contrast if the item does already have a Point and/or a Property tag then we write a warning
     * message in the log. The warning is also logged if the item has more than one linked channel
     * with the 'useTags=true' configuration. If the item has native custom tags then those tags
     * remain.
     */
    private void assignChannelDefaultTags(ItemChannelLink link) {
        Item baseItem;
        try {
            baseItem = itemRegistry.getItem(link.getItemName());
        } catch (ItemNotFoundException e) {
            return;
        }

        if (baseItem instanceof ActiveItem item) {
            boolean alreadyHasPointOrPropertyTag = false;
            for (String tag : item.getTags()) {
                Class<? extends Tag> type = SemanticTags.getById(tag);
                if ((type != null) && (Point.class.isAssignableFrom(type) || Property.class.isAssignableFrom(type))) {
                    alreadyHasPointOrPropertyTag = true;
                    break;
                }
            }

            Set<String> channelDefaultTags = getChannelDefaultTags(link);
            if (!channelDefaultTags.isEmpty()) {
                if (alreadyHasPointOrPropertyTag) {
                    logger.warn("Item '{}' forbidden to assign tags from multiple sources.", item.getName());
                } else {
                    item.addTags(channelDefaultTags);
                    logger.debug("Item '{}' assigned tags '{}' from channel '{}'.", item.getName(), channelDefaultTags,
                            link.getLinkedUID());
                }
            }
        }
    }

    /**
     * If the linked channel has 'useTags=true' and the item's native tag set contains all of the
     * tags of the linked channel then remove those tags from the item. If the item had any native
     * custom tags they shall NOT be removed. Finally iterate over any other linked channels so
     * they may eventually provide new tags.
     */
    private void removeChannelDefaultTags(ItemChannelLink thisLink) {
        Item baseItem;
        try {
            baseItem = itemRegistry.getItem(thisLink.getItemName());
        } catch (ItemNotFoundException e) {
            return;
        }

        if (baseItem instanceof ActiveItem item) {
            Set<String> channelDefaultTags = getChannelDefaultTags(thisLink);
            if (!channelDefaultTags.isEmpty() && item.getTags().containsAll(channelDefaultTags)) {
                // remove the original tags
                channelDefaultTags.forEach(tag -> item.removeTag(tag));
                logger.debug("Item '{}' removed tags '{}' from channel '{}'.", item.getName(), channelDefaultTags,
                        thisLink.getLinkedUID());

                // iterate over other links in case one may provide new tags
                boolean alreadyHasPointOrPropertyTag = false;
                for (ItemChannelLink otherLink : getLinks(item.getName())) {
                    if (!otherLink.getUID().equals(thisLink.getUID())) {
                        channelDefaultTags = getChannelDefaultTags(otherLink);
                        if (!channelDefaultTags.isEmpty()) {
                            if (alreadyHasPointOrPropertyTag) {
                                logger.warn("Item '{}' forbidden to assign tags from multiple sources.",
                                        item.getName());
                                return;
                            } else {
                                alreadyHasPointOrPropertyTag = true;
                                item.addTags(channelDefaultTags);
                                logger.debug("Item '{}' assigned tags '{}' from channel '{}'.", item.getName(),
                                        channelDefaultTags, otherLink.getLinkedUID());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * If the linked channel has 'useTags=true' in its configuration return it's default tags.
     * Otherwise return an empty set.
     */
    private Set<String> getChannelDefaultTags(ItemChannelLink link) {
        Configuration configuration = link.getConfiguration();
        if (Boolean.TRUE.equals(configuration.get(USE_TAGS))) {
            ChannelUID channelUID = link.getLinkedUID();
            Thing thing = thingRegistry.get(channelUID.getThingUID());
            if (thing != null) {
                Channel channel = thing.getChannel(channelUID.getId());
                if (channel != null) {
                    return channel.getDefaultTags();
                }
            }
        }
        return Set.of();
    }
}
