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
package org.eclipse.smarthome.core.thing.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.common.registry.RegistryChangeListener;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemFactory;
import org.eclipse.smarthome.core.items.ItemProvider;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.RegistryHook;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class dynamically provides items for all links that point to non-existing items.
 *
 * @author Kai Kreuzer
 * @author Markus Rathgeb - Add locale provider support
 * @author Thomas Höfer - Added modified operation
 */
@Component(configurationPid = "org.eclipse.smarthome.channelitemprovider", immediate = true)
public class ChannelItemProvider implements ItemProvider {

    private final Logger logger = LoggerFactory.getLogger(ChannelItemProvider.class);

    private final long INITIALIZATION_DELAY_NANOS = TimeUnit.SECONDS.toNanos(2);

    private final Set<ProviderChangeListener<Item>> listeners = new HashSet<>();

    private LocaleProvider localeProvider;
    private ThingRegistry thingRegistry;
    private ItemChannelLinkRegistry linkRegistry;
    private ItemRegistry itemRegistry;
    private final Set<ItemFactory> itemFactories = new HashSet<>();
    private Map<String, Item> items = null;
    private ChannelTypeRegistry channelTypeRegistry;

    private boolean enabled = true;
    private volatile boolean initialized = false;
    private volatile long lastUpdate = System.nanoTime();
    private ScheduledExecutorService executor;

    @Override
    public Collection<Item> getAll() {
        if (!enabled || !initialized) {
            return Collections.emptySet();
        } else {
            initializeItems();
            return new HashSet<>(items.values());
        }
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<Item> listener) {
        listeners.add(listener);
        for (Item item : getAll()) {
            listener.added(this, item);
        }
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<Item> listener) {
        listeners.remove(listener);
    }

    @Reference
    protected void setLocaleProvider(final LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    protected void unsetLocaleProvider(final LocaleProvider localeProvider) {
        this.localeProvider = null;
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void addItemFactory(ItemFactory itemFactory) {
        this.itemFactories.add(itemFactory);
    }

    protected void removeItemFactory(ItemFactory itemFactory) {
        this.itemFactories.remove(itemFactory);
    }

    @Reference
    protected void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    protected void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    @Reference
    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Reference
    protected void setItemChannelLinkRegistry(ItemChannelLinkRegistry linkRegistry) {
        this.linkRegistry = linkRegistry;
    }

    protected void unsetItemChannelLinkRegistry(ItemChannelLinkRegistry linkRegistry) {
        this.linkRegistry = null;
    }

    @Reference
    protected void setChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = channelTypeRegistry;
    }

    protected void unsetChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = null;
    }

    @Activate
    protected void activate(Map<String, Object> properties) {
        modified(properties);
    }

    @Modified
    protected synchronized void modified(Map<String, Object> properties) {
        if (properties != null) {
            String enabled = (String) properties.get("enabled");
            if ("false".equalsIgnoreCase(enabled)) {
                this.enabled = false;
            } else {
                this.enabled = true;
            }
        }

        if (enabled) {
            addRegistryChangeListeners();

            boolean initialDelay = properties == null
                    || !"false".equalsIgnoreCase((String) properties.get("initialDelay"));
            if (initialDelay) {
                executor = Executors.newSingleThreadScheduledExecutor();
                delayedInitialize();
            } else {
                initialize();
            }
        } else {
            logger.debug("Disabling channel item provider.");
            disableChannelItemProvider();
        }
    }

    private synchronized void disableChannelItemProvider() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        for (ProviderChangeListener<Item> listener : listeners) {
            for (Item item : getAll()) {
                listener.removed(this, item);
            }
        }
        removeRegistryChangeListeners();

        initialized = false;
        items = null;
    }

    private synchronized void delayedInitialize() {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        // we wait until no further new links or items are announced in order to avoid creation of
        // items which then must be removed again immediately.
        final long diff = System.nanoTime() - lastUpdate - INITIALIZATION_DELAY_NANOS;
        if (diff < 0) {
            executor.schedule(() -> delayedInitialize(), -diff, TimeUnit.NANOSECONDS);
        } else {
            executor.shutdown();
            executor = null;

            initialize();
        }
    }

    private void initialize() {
        initializeItems();
        initialized = true;
    }

    private synchronized void initializeItems() {
        if (items != null) {
            return;
        }
        items = new ConcurrentHashMap<>();
        for (ItemChannelLink link : linkRegistry.getAll()) {
            createItemForLink(link);
        }
    }

    @Deactivate
    protected void deactivate() {
        disableChannelItemProvider();
    }

    private void addRegistryChangeListeners() {
        this.linkRegistry.addRegistryChangeListener(linkRegistryListener);
        this.itemRegistry.addRegistryHook(itemRegistryListener);
        this.thingRegistry.addRegistryChangeListener(thingRegistryListener);
    }

    private void removeRegistryChangeListeners() {
        this.itemRegistry.removeRegistryHook(itemRegistryListener);
        this.linkRegistry.removeRegistryChangeListener(linkRegistryListener);
        this.thingRegistry.removeRegistryChangeListener(thingRegistryListener);
    }

    private void createItemForLink(ItemChannelLink link) {
        if (!enabled) {
            return;
        }
        if (itemRegistry.get(link.getItemName()) != null) {
            // there is already an item, we do not need to create one
            return;
        }
        Channel channel = thingRegistry.getChannel(link.getLinkedUID());
        if (channel != null) {
            Item item = null;
            // Only create an item for state channels
            if (channel.getKind() == ChannelKind.STATE) {
                for (ItemFactory itemFactory : itemFactories) {
                    item = itemFactory.createItem(channel.getAcceptedItemType(), link.getItemName());
                    if (item != null) {
                        break;
                    }
                }
            }
            if (item instanceof GenericItem) {
                GenericItem gItem = (GenericItem) item;
                gItem.setLabel(getLabel(channel));
                gItem.setCategory(getCategory(channel));
                gItem.addTags(channel.getDefaultTags());
            }
            if (item != null) {
                logger.trace("Created virtual item '{}'", item.getName());
                items.put(item.getName(), item);
                for (ProviderChangeListener<Item> listener : listeners) {
                    listener.added(this, item);
                }
            }
        }
    }

    private String getCategory(Channel channel) {
        if (channel.getChannelTypeUID() != null) {
            ChannelType channelType = channelTypeRegistry.getChannelType(channel.getChannelTypeUID(),
                    localeProvider.getLocale());
            if (channelType != null) {
                return channelType.getCategory();
            }
        }
        return null;
    }

    private String getLabel(Channel channel) {
        if (channel.getLabel() != null) {
            return channel.getLabel();
        } else {
            final Locale locale = localeProvider.getLocale();
            if (channel.getChannelTypeUID() != null) {
                final ChannelType channelType = channelTypeRegistry.getChannelType(channel.getChannelTypeUID(), locale);
                if (channelType != null) {
                    return channelType.getLabel();
                }
            }
        }
        return null;
    }

    private void removeItem(String key) {
        if (!enabled) {
            return;
        }
        Item item = items.get(key);
        if (item != null) {
            for (ProviderChangeListener<Item> listener : listeners) {
                listener.removed(this, item);
            }
            items.remove(key);
            logger.trace("Removed virtual item '{}'", item.getName());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    RegistryChangeListener<Thing> thingRegistryListener = new RegistryChangeListener<Thing>() {

        @Override
        public void added(Thing element) {
            if (!initialized) {
                return;
            }
            for (Channel channel : element.getChannels()) {
                for (ItemChannelLink link : linkRegistry.getLinks(channel.getUID())) {
                    createItemForLink(link);
                }
            }
        }

        @Override
        public void removed(Thing element) {
            if (!initialized) {
                return;
            }
            removeItem(element.getUID().toString());
        }

        @Override
        public void updated(Thing oldElement, Thing element) {
            removed(oldElement);
            added(element);
        }
    };

    RegistryChangeListener<ItemChannelLink> linkRegistryListener = new RegistryChangeListener<ItemChannelLink>() {

        @Override
        public void added(ItemChannelLink element) {
            if (!initialized) {
                lastUpdate = System.nanoTime();
                return;
            }
            createItemForLink(element);
        }

        @Override
        public void removed(ItemChannelLink element) {
            if (!initialized) {
                return;
            }
            removeItem(element.getItemName());
        }

        @Override
        public void updated(ItemChannelLink oldElement, ItemChannelLink element) {
            removed(oldElement);
            added(element);
        }
    };

    RegistryHook<Item> itemRegistryListener = new RegistryHook<Item>() {

        @Override
        public void beforeAdding(Item element) {
            if (!initialized) {
                lastUpdate = System.nanoTime();
                return;
            }
            // check, if it is our own item
            for (Item item : items.values()) {
                if (item == element) {
                    return;
                }
            }
            // it is from some other provider, so remove ours, if we have one
            Item oldElement = items.get(element.getName());
            if (oldElement != null) {
                for (ProviderChangeListener<Item> listener : listeners) {
                    listener.removed(ChannelItemProvider.this, oldElement);
                }
                items.remove(element.getName());
            }
        }

        @Override
        public void afterRemoving(Item element) {
            if (!initialized) {
                return;
            }
            // check, if it is our own item
            for (Item item : items.values()) {
                if (item == element) {
                    return;
                }
            }
            // it is from some other provider, so create one ourselves if needed
            for (ChannelUID uid : linkRegistry.getBoundChannels(element.getName())) {
                for (ItemChannelLink link : linkRegistry.getLinks(uid)) {
                    if (itemRegistry.get(link.getItemName()) == null) {
                        createItemForLink(link);
                    }
                }
            }
        }

    };

}
