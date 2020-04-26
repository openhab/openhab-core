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
package org.openhab.core.thing.internal.profiles;

import java.util.function.Function;

import org.openhab.core.common.SafeCaller;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemStateConverter;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.internal.CommunicationManager;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.util.ThingHandlerHelper;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ProfileCallback} implementation.
 *
 * @author Simon Kaufmann - Initial contribution
 */
public class ProfileCallbackImpl implements ProfileCallback {

    private final Logger logger = LoggerFactory.getLogger(ProfileCallbackImpl.class);

    private final EventPublisher eventPublisher;
    private final ItemChannelLink link;
    private final Function<ThingUID, Thing> thingProvider;
    private final Function<String, Item> itemProvider;
    private final SafeCaller safeCaller;
    private final ItemStateConverter itemStateConverter;

    public ProfileCallbackImpl(EventPublisher eventPublisher, SafeCaller safeCaller,
            ItemStateConverter itemStateConverter, ItemChannelLink link, Function<ThingUID, Thing> thingProvider,
            Function<String, Item> itemProvider) {
        this.eventPublisher = eventPublisher;
        this.safeCaller = safeCaller;
        this.itemStateConverter = itemStateConverter;
        this.link = link;
        this.thingProvider = thingProvider;
        this.itemProvider = itemProvider;
    }

    @Override
    public void handleCommand(Command command) {
        Thing thing = thingProvider.apply(link.getLinkedUID().getThingUID());
        if (thing != null) {
            final ThingHandler handler = thing.getHandler();
            if (handler != null) {
                if (ThingHandlerHelper.isHandlerInitialized(thing)) {
                    logger.debug("Delegating command '{}' for item '{}' to handler for channel '{}'", command,
                            link.getItemName(), link.getLinkedUID());
                    safeCaller.create(handler, ThingHandler.class)
                            .withTimeout(CommunicationManager.THINGHANDLER_EVENT_TIMEOUT).onTimeout(() -> {
                                logger.warn("Handler for thing '{}' takes more than {}ms for handling a command",
                                        handler.getThing().getUID(), CommunicationManager.THINGHANDLER_EVENT_TIMEOUT);
                            }).build().handleCommand(link.getLinkedUID(), command);
                } else {
                    logger.debug("Not delegating command '{}' for item '{}' to handler for channel '{}', "
                            + "because handler is not initialized (thing must be in status UNKNOWN, ONLINE or OFFLINE but was {}).",
                            command, link.getItemName(), link.getLinkedUID(), thing.getStatus());
                }
            } else {
                logger.warn("Cannot delegate command '{}' for item '{}' to handler for channel '{}', "
                        + "because no handler is assigned. Maybe the binding is not installed or not "
                        + "propertly initialized.", command, link.getItemName(), link.getLinkedUID());
            }
        } else {
            logger.warn(
                    "Cannot delegate command '{}' for item '{}' to handler for channel '{}', "
                            + "because no thing with the UID '{}' could be found.",
                    command, link.getItemName(), link.getLinkedUID(), link.getLinkedUID().getThingUID());
        }
    }

    @Override
    public void handleUpdate(State state) {
        Thing thing = thingProvider.apply(link.getLinkedUID().getThingUID());
        if (thing != null) {
            final ThingHandler handler = thing.getHandler();
            if (handler != null) {
                if (ThingHandlerHelper.isHandlerInitialized(thing)) {
                    logger.debug("Delegating update '{}' for item '{}' to handler for channel '{}'", state,
                            link.getItemName(), link.getLinkedUID());
                    safeCaller.create(handler, ThingHandler.class)
                            .withTimeout(CommunicationManager.THINGHANDLER_EVENT_TIMEOUT).onTimeout(() -> {
                                logger.warn("Handler for thing '{}' takes more than {}ms for handling an update",
                                        handler.getThing().getUID(), CommunicationManager.THINGHANDLER_EVENT_TIMEOUT);
                            }).build().handleUpdate(link.getLinkedUID(), state);
                } else {
                    logger.debug("Not delegating update '{}' for item '{}' to handler for channel '{}', "
                            + "because handler is not initialized (thing must be in status UNKNOWN, ONLINE or OFFLINE but was {}).",
                            state, link.getItemName(), link.getLinkedUID(), thing.getStatus());
                }
            } else {
                logger.warn("Cannot delegate update '{}' for item '{}' to handler for channel '{}', "
                        + "because no handler is assigned. Maybe the binding is not installed or not "
                        + "propertly initialized.", state, link.getItemName(), link.getLinkedUID());
            }
        } else {
            logger.warn(
                    "Cannot delegate update '{}' for item '{}' to handler for channel '{}', "
                            + "because no thing with the UID '{}' could be found.",
                    state, link.getItemName(), link.getLinkedUID(), link.getLinkedUID().getThingUID());
        }
    }

    @Override
    public void sendCommand(Command command) {
        eventPublisher
                .post(ItemEventFactory.createCommandEvent(link.getItemName(), command, link.getLinkedUID().toString()));
    }

    @Override
    public void sendUpdate(State state) {
        Item item = itemProvider.apply(link.getItemName());
        State acceptedState = itemStateConverter.convertToAcceptedState(state, item);
        eventPublisher.post(
                ItemEventFactory.createStateEvent(link.getItemName(), acceptedState, link.getLinkedUID().toString()));
    }
}
