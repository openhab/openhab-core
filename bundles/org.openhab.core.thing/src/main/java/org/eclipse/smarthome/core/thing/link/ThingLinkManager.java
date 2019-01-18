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

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.common.registry.Provider;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.common.registry.RegistryChangeListener;
import org.eclipse.smarthome.core.events.AbstractTypedEventSubscriber;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ManagedThingProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.events.ThingStatusInfoChangedEvent;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.eclipse.smarthome.core.thing.util.ThingHandlerHelper;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ThingLinkManager} manages links for channels.
 * <p>
 * If a Thing is created, it can automatically create links for its non-advanced channels.
 * Upon a Thing deletion, it removes all links of this Thing.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Markus Rathgeb - Handle item registry's all items changed notification
 * @author Kai Kreuzer - Refactored to make it a service and introduced the auto-linking (as a replacement for the
 *         ThingSetupManager)
 * @author Markus Rathgeb - Send link notification if item and link exists and unlink on the first removal
 */
@Component(immediate = true, configurationPid = "org.eclipse.smarthome.links", service = { ThingLinkManager.class,
        EventSubscriber.class }, property = { "service.config.description.uri:String=system:links",
                "service.config.label:String=Item Linking", "service.config.category:String=system",
                "service.pid:String=org.eclipse.smarthome.links" })
public class ThingLinkManager extends AbstractTypedEventSubscriber<ThingStatusInfoChangedEvent> {

    private static final String THREADPOOL_NAME = "thingLinkManager";

    private final Logger logger = LoggerFactory.getLogger(ThingLinkManager.class);

    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(THREADPOOL_NAME);

    private ThingRegistry thingRegistry;
    private ManagedThingProvider managedThingProvider;
    private ItemRegistry itemRegistry;
    private ItemChannelLinkRegistry itemChannelLinkRegistry;
    private ChannelTypeRegistry channelTypeRegistry;

    private boolean autoLinks = true;

    public ThingLinkManager() {
        super(ThingStatusInfoChangedEvent.TYPE);
    }

    @Activate
    public void activate(ComponentContext context) {
        modified(context);
        itemRegistry.addRegistryChangeListener(itemRegistryChangeListener);
        itemChannelLinkRegistry.addRegistryChangeListener(itemChannelLinkRegistryChangeListener);
        managedThingProvider.addProviderChangeListener(managedThingProviderListener);
    }

    @Modified
    protected void modified(ComponentContext context) {
        // check whether we want to enable the automatic link creation or not
        if (context != null) {
            Object value = context.getProperties().get("autoLinks");
            autoLinks = value == null || !value.toString().equals("false");
        }
    }

    @Deactivate
    public void deactivate() {
        itemRegistry.removeRegistryChangeListener(itemRegistryChangeListener);
        itemChannelLinkRegistry.removeRegistryChangeListener(itemChannelLinkRegistryChangeListener);
        managedThingProvider.removeProviderChangeListener(managedThingProviderListener);
    }

    @Reference
    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Reference
    protected void setItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }

    protected void unsetItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = null;
    }

    @Reference
    protected void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    protected void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    @Reference
    protected void setManagedThingProvider(ManagedThingProvider managedThingProvider) {
        this.managedThingProvider = managedThingProvider;
    }

    protected void unsetManagedThingProvider(ManagedThingProvider managedThingProvider) {
        this.managedThingProvider = null;
    }

    @Reference
    protected void setChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = channelTypeRegistry;
    }

    protected void unsetChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = null;
    }

    public boolean isAutoLinksEnabled() {
        return autoLinks;
    }

    private final RegistryChangeListener<Item> itemRegistryChangeListener = new RegistryChangeListener<Item>() {
        @Override
        public void added(Item element) {
            for (final ChannelUID channelUID : itemChannelLinkRegistry.getBoundChannels(element.getName())) {
                final Thing thing = thingRegistry.get(channelUID.getThingUID());
                if (thing != null) {
                    final Channel channel = thing.getChannel(channelUID.getId());
                    if (channel != null) {
                        ThingLinkManager.this.informHandlerAboutLinkedChannel(thing, channel);
                    }
                }
            }
        }

        @Override
        public void removed(Item element) {
            for (final ChannelUID channelUID : itemChannelLinkRegistry.getBoundChannels(element.getName())) {
                final Thing thing = thingRegistry.get(channelUID.getThingUID());
                if (thing != null) {
                    final Channel channel = thing.getChannel(channelUID.getId());
                    if (channel != null) {
                        ThingLinkManager.this.informHandlerAboutUnlinkedChannel(thing, channel);
                    }
                }
            }
        }

        @Override
        public void updated(Item oldElement, Item element) {
            if (!oldElement.equals(element)) {
                this.removed(oldElement);
                this.added(element);
            }
        }

    };

    private final RegistryChangeListener<ItemChannelLink> itemChannelLinkRegistryChangeListener = new RegistryChangeListener<ItemChannelLink>() {

        @Override
        public void added(ItemChannelLink itemChannelLink) {
            if (itemRegistry.get(itemChannelLink.getItemName()) == null) {
                // Don't inform about the link if the item does not exist.
                // The handler will be informed on item creation.
                return;
            }
            ChannelUID channelUID = itemChannelLink.getLinkedUID();
            Thing thing = thingRegistry.get(channelUID.getThingUID());
            if (thing != null) {
                Channel channel = thing.getChannel(channelUID.getId());
                if (channel != null) {
                    ThingLinkManager.this.informHandlerAboutLinkedChannel(thing, channel);
                }
            }
        }

        @Override
        public void removed(ItemChannelLink itemChannelLink) {
            /*
             * Don't check for item existence here.
             * If an item and its link are removed before the registry change listener methods are called,
             * a check for the item could prevent that the handler is informed about the unlink at all.
             */
            ChannelUID channelUID = itemChannelLink.getLinkedUID();
            Thing thing = thingRegistry.get(channelUID.getThingUID());
            if (thing != null) {
                Channel channel = thing.getChannel(channelUID.getId());
                if (channel != null) {
                    ThingLinkManager.this.informHandlerAboutUnlinkedChannel(thing, channel);
                }
            }
        }

        @Override
        public void updated(ItemChannelLink oldElement, ItemChannelLink element) {
            if (!oldElement.equals(element)) {
                this.removed(oldElement);
                this.added(element);
            }
        }

    };

    private final ProviderChangeListener<Thing> managedThingProviderListener = new ProviderChangeListener<Thing>() {

        @Override
        public void added(Provider<Thing> provider, Thing thing) {
            List<Channel> channels = thing.getChannels();
            for (Channel channel : channels) {
                createLinkIfNotAdvanced(channel);
            }
        }

        private void createLinkIfNotAdvanced(Channel channel) {
            if (autoLinks) {
                if (channel.getChannelTypeUID() != null) {
                    ChannelType type = channelTypeRegistry.getChannelType(channel.getChannelTypeUID());
                    if (type != null && type.isAdvanced()) {
                        return;
                    }
                }
                ItemChannelLink link = new ItemChannelLink(deriveItemName(channel.getUID()), channel.getUID());
                itemChannelLinkRegistry.add(link);
            }
        }

        @Override
        public void removed(Provider<Thing> provider, Thing thing) {
            List<Channel> channels = thing.getChannels();
            for (Channel channel : channels) {
                ItemChannelLink link = new ItemChannelLink(deriveItemName(channel.getUID()), channel.getUID());
                itemChannelLinkRegistry.remove(link.getUID());
            }
        }

        @Override
        public void updated(Provider<Thing> provider, Thing oldThing, Thing newThing) {
            for (Channel channel : oldThing.getChannels()) {
                if (newThing.getChannel(channel.getUID().getId()) == null) {
                    // this channel does not exist anymore, so remove outdated links
                    ItemChannelLink link = new ItemChannelLink(deriveItemName(channel.getUID()), channel.getUID());
                    itemChannelLinkRegistry.remove(link.getUID());
                }
            }
            for (Channel channel : newThing.getChannels()) {
                if (oldThing.getChannel(channel.getUID().getId()) == null) {
                    // this channel did not exist before, so add a link
                    createLinkIfNotAdvanced(channel);
                }
            }
        }

        private String deriveItemName(ChannelUID uid) {
            return uid.getAsString().replaceAll("[^a-zA-Z0-9_]", "_");
        }

    };

    private void informHandlerAboutLinkedChannel(Thing thing, Channel channel) {
        scheduler.submit(() -> {
            // Don't notify the thing if the thing isn't initialised
            if (ThingHandlerHelper.isHandlerInitialized(thing)) {
                ThingHandler handler = thing.getHandler();
                if (handler != null) {
                    try {
                        handler.channelLinked(channel.getUID());
                    } catch (Exception ex) {
                        logger.error("Exception occurred while informing handler: {}", ex.getMessage(), ex);
                    }
                } else {
                    logger.trace(
                            "Can not inform handler about linked channel, because no handler is assigned to the thing {}.",
                            thing.getUID());
                }
            }
        });
    }

    private void informHandlerAboutUnlinkedChannel(Thing thing, Channel channel) {
        scheduler.submit(() -> {
            // Don't notify the thing if the thing isn't initialised
            if (ThingHandlerHelper.isHandlerInitialized(thing)) {
                ThingHandler handler = thing.getHandler();
                if (handler != null) {
                    try {
                        handler.channelUnlinked(channel.getUID());
                    } catch (Exception ex) {
                        logger.error("Exception occurred while informing handler: {}", ex.getMessage(), ex);
                    }
                } else {
                    logger.trace(
                            "Can not inform handler about unlinked channel, because no handler is assigned to the thing {}.",
                            thing.getUID());
                }
            }
        });
    }

    @Override
    protected void receiveTypedEvent(ThingStatusInfoChangedEvent event) {
        // when a thing handler is successfully initialized (i.e. it goes from INITIALIZING to UNKNOWN, ONLINE or
        // OFFLINE), we need to make sure that channelLinked() is called for all existing links
        if (ThingStatus.INITIALIZING.equals(event.getOldStatusInfo().getStatus())) {
            if (ThingHandlerHelper.isHandlerInitialized(event.getStatusInfo().getStatus())) {
                Thing thing = thingRegistry.get(event.getThingUID());
                if (thing != null) {
                    for (Channel channel : thing.getChannels()) {
                        if (itemChannelLinkRegistry.getLinkedItemNames(channel.getUID()).size() > 0) {
                            informHandlerAboutLinkedChannel(thing, channel);
                        }
                    }
                }
            }
        }
    }

}
