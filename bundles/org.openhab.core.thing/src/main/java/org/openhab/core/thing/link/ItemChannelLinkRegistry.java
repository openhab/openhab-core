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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ActiveItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemBuilderFactory;
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
import org.osgi.service.component.annotations.Modified;
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
@Component(immediate = true, service = ItemChannelLinkRegistry.class, configurationPid = "org.openhab.ItemChannelLinkRegistry")
public class ItemChannelLinkRegistry extends AbstractLinkRegistry<ItemChannelLink, ItemChannelLinkProvider>
        implements RegistryChangeListener<Item> {

    public static final String USE_TAGS = "useTags";

    private final Logger logger = LoggerFactory.getLogger(ItemChannelLinkRegistry.class);

    private final ThingRegistry thingRegistry;
    private final ItemRegistry itemRegistry;
    private final ItemBuilderFactory itemBuilderFactory;

    private boolean useTagsGlobally = false;

    @Activate
    public ItemChannelLinkRegistry(final @Nullable Map<String, @Nullable Object> configuration,
            final @Reference ThingRegistry thingRegistry, final @Reference ItemRegistry itemRegistry,
            final @Reference ItemBuilderFactory itemBuilderFactory) {
        super(ItemChannelLinkProvider.class);
        this.thingRegistry = thingRegistry;
        this.itemRegistry = itemRegistry;
        this.itemRegistry.addRegistryChangeListener(this);
        this.itemBuilderFactory = itemBuilderFactory;

        modified(configuration);
    }

    @Modified
    protected void modified(@Nullable Map<String, @Nullable Object> configuration) {
        Object entry = configuration != null ? configuration.get(USE_TAGS) : null;
        useTagsGlobally = entry != null ? Boolean.parseBoolean(entry.toString()) : false;
    }

    @Override
    protected void deactivate() {
        itemRegistry.removeRegistryChangeListener(this);
        super.deactivate();
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
     * If the item does not already have a Point and/or a Property tag and if the linked channel has
     * 'useTags=true' then assign the default tags from that channel to the respective item. By contrast
     * if the item does already have a Point and/or a Property tag then we write a warning message in
     * the log. The warning is also logged if the item has more than one linked channel with 'useTags=true'.
     * And in any case if the item has native custom tags then those tags remain.
     */
    private void assignChannelDefaultTags(ItemChannelLink link, ActiveItem activeItem) {
        boolean alreadyHasPointOrPropertyTag = false;

        for (String tag : activeItem.getTags()) {
            Class<? extends Tag> type = SemanticTags.getById(tag);
            if ((type != null) && (Point.class.isAssignableFrom(type) || Property.class.isAssignableFrom(type))) {
                alreadyHasPointOrPropertyTag = true;
                break;
            }
        }

        Set<String> channelDefaultTags = getChannelDefaultTags(link);
        if (!channelDefaultTags.isEmpty()) {
            if (alreadyHasPointOrPropertyTag) {
                if (!useTagsGlobally) {
                    logger.warn("Item '{}' already tagged; forbidden to add tags supplied by channel '{}'.",
                            activeItem.getName(), link.getLinkedUID());
                }
            } else {
                Set<String> newTags = new HashSet<>(activeItem.getTags());
                newTags.addAll(channelDefaultTags);
                logger.info("Item '{}' added tags '{}' supplied by channel '{}'.", activeItem.getName(),
                        channelDefaultTags, link.getLinkedUID());

                link.setTagsLinked(true);

                updateExistingRegistryItemTagsAndNotifyRegistryListeners(activeItem.getName(), newTags);
            }
        }
    }

    private void assignChannelDefaultTags(ItemChannelLink link) {
        try {
            if (itemRegistry.getItem(link.getItemName()) instanceof ActiveItem activeItem) {
                assignChannelDefaultTags(link, activeItem);
            }
        } catch (ItemNotFoundException e) {
        }
    }

    /**
     * if the linked channel is the actual source of the item's tags then remove those tags from
     * the item. If the item had any native custom tags they shall NOT be removed. Finally iterate
     * over any other linked channels so they may eventually provide new tags.
     */
    private void removeChannelDefaultTags(ItemChannelLink oldLink, ActiveItem activeItem) {
        if (oldLink.tagsLinked()) {
            Set<String> newTags = new HashSet<>(activeItem.getTags());

            // remove old link's tags
            Set<String> oldLinkTags = getChannelDefaultTags(oldLink);
            newTags.removeAll(oldLinkTags);
            // on OH shutdown tagsLinked may be true but oldLinkTags is already empty so suppress that log
            if (oldLinkTags.isEmpty()) {
                logger.info("Item '{}' removed tags '{}' supplied by channel '{}'.", activeItem.getName(), oldLinkTags,
                        oldLink.getLinkedUID());
            }

            // iterate over other links in case one may assign new tags
            boolean alreadyHasPointOrPropertyTag = false;
            for (ItemChannelLink otherLink : getLinks(activeItem.getName())) {
                if (!otherLink.getUID().equals(oldLink.getUID())) {
                    Set<String> otherLinkTags = getChannelDefaultTags(otherLink);
                    if (!otherLinkTags.isEmpty()) {
                        if (alreadyHasPointOrPropertyTag) {
                            if (!useTagsGlobally) {
                                logger.warn("Item '{}' already tagged; forbidden to add tags supplied by channel '{}'.",
                                        activeItem.getName(), otherLink.getLinkedUID());
                            }
                            break;
                        } else {
                            alreadyHasPointOrPropertyTag = true;
                            newTags.addAll(otherLinkTags);
                            logger.info("Item '{}' added tags '{}' supplied by channel '{}'.", activeItem.getName(),
                                    otherLinkTags, otherLink.getLinkedUID());
                            otherLink.setTagsLinked(true);
                        }
                    }
                }
            }

            oldLink.setTagsLinked(false);

            updateExistingRegistryItemTagsAndNotifyRegistryListeners(activeItem.getName(), newTags);
        }
    }

    private void removeChannelDefaultTags(ItemChannelLink link) {
        try {
            if (itemRegistry.getItem(link.getItemName()) instanceof ActiveItem activeItem) {
                removeChannelDefaultTags(link, activeItem);
            }
        } catch (ItemNotFoundException e) {
        }
    }

    /**
     * If the linked channel has 'useTags=true' return its default tags, otherwise return an empty set.
     */
    private Set<String> getChannelDefaultTags(ItemChannelLink link) {
        boolean getChannelTags;
        if (useTagsGlobally) {
            getChannelTags = true;
        } else {
            Configuration configuration = link.getConfiguration();
            Object entry = configuration.get(USE_TAGS);
            getChannelTags = entry != null ? Boolean.parseBoolean(entry.toString()) : false;
        }

        if (getChannelTags) {
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

    /**
     * Update the tags on the item instance in item registry and notify the item registry listeners about
     * the change. For items provisioned by an .items file, the item registry is read only so instead of
     * trying to replace the item with a new instance, we just modify the tags on the existing item instance.
     * However since the notification method requires both old and new items so we create a 'fake old item'
     * instance having the prior tags.
     *
     * @param itemName the name of the item in the registry that shall be be updated.
     * @param newTags the new tags that shall be applied to that item instance.
     */
    private void updateExistingRegistryItemTagsAndNotifyRegistryListeners(String itemName, Set<String> newTags) {
        try {
            if (itemRegistry.getItem(itemName) instanceof ActiveItem item) {
                Item fakeOldItem = itemBuilderFactory.newItemBuilder(item).build();
                item.removeAllTags();
                item.addTags(newTags);
                itemRegistry.notifyListenersAboutItemExternalUpdate(fakeOldItem, item);
            }
        } catch (ItemNotFoundException e) {
        }
    }

    /**
     * If a new item is added to the item registry and we already have prior "pre- orphaned"
     * links to it, then update the tags from the default tags of the prior linked channels.
     */
    @Override
    public void added(Item item) {
        if (item instanceof ActiveItem activeItem) {
            getLinks(activeItem.getName()).forEach(link -> assignChannelDefaultTags(link, activeItem));
        }
    }

    /**
     * If the item has gone then clear the 'tagsLinked' flag.
     */
    @Override
    public void removed(Item item) {
        if (item instanceof ActiveItem activeItem) {
            getLinks(activeItem.getName()).forEach(link -> link.setTagsLinked(false));
        }
    }

    /**
     * Either this class applied channel default tags to the item, or something else updated the
     * item (including possibly updating its tags), so in any case do NOT try to re-apply the
     * channel default tags. Since that might cause an infinite loop.
     */
    @Override
    public void updated(Item oldItem, Item item) {
        // do nothing
    }
}
