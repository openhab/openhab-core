/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.thing.internal;

import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.util.ThingHandlerHelper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ChannelLinkNotifier} notifies initialized thing handlers of channels being linked or unlinked.
 *
 * @author Łukasz Dywicki - Initial contribution
 * @author Wouter Born - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class ChannelLinkNotifier implements RegistryChangeListener<ItemChannelLink> {

    private final Logger logger = LoggerFactory.getLogger(ChannelLinkNotifier.class);

    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final ThingRegistry thingRegistry;

    @Activate
    public ChannelLinkNotifier(@Reference ItemChannelLinkRegistry itemChannelLinkRegistry,
            @Reference ThingRegistry thingRegistry) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.thingRegistry = thingRegistry;

        itemChannelLinkRegistry.addRegistryChangeListener(this);
        // registry does not dispatch notifications about existing links to listeners
        itemChannelLinkRegistry.stream().map(ItemChannelLink::getLinkedUID).distinct()
                .forEach(channelUID -> call(channelUID.getThingUID(), handler -> handler.channelLinked(channelUID),
                        "channelLinked"));
    }

    @Deactivate
    public void deactivate() {
        itemChannelLinkRegistry.removeRegistryChangeListener(this);
    }

    @Override
    public void added(ItemChannelLink element) {
        ChannelUID channelUID = element.getLinkedUID();
        ThingUID thingUID = channelUID.getThingUID();

        call(thingUID, handler -> handler.channelLinked(channelUID), "channelLinked");
    }

    @Override
    public void removed(ItemChannelLink element) {
        ChannelUID channelUID = element.getLinkedUID();
        ThingUID thingUID = channelUID.getThingUID();

        boolean channelLinked = itemChannelLinkRegistry.stream()
                .anyMatch(itemChannelLink -> channelUID.equals(itemChannelLink.getLinkedUID()));

        if (!channelLinked) {
            call(thingUID, handler -> handler.channelUnlinked(channelUID), "channelUnlinked");
        }
    }

    @Override
    public void updated(ItemChannelLink oldElement, ItemChannelLink element) {
        removed(oldElement);
        added(element);
    }

    private void call(ThingUID thingUID, Consumer<ThingHandler> consumer, String method) {
        Thing thing = thingRegistry.get(thingUID);
        if (thing != null) {
            ThingHandler handler = thing.getHandler();
            if (handler != null && ThingHandlerHelper.isHandlerInitialized(handler)) {
                consumer.accept(handler);
            } else {
                logger.debug("Skipping notification to thing {} handler '{}' method call, as it is not initialized",
                        thingUID, method);
            }
        }
    }
}
