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
package org.openhab.core.io.rest.sse.internal;

import java.io.IOException;
import java.time.DateTimeException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.measure.Unit;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.server.ChunkedOutput;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationHelper;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.openhab.core.types.util.UnitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link SseBroadcaster} keeps track of the {@link SseStateEventOutput} listeners to state changes and tracks them
 * by their connectionId.
 *
 * @author Yannick Schaus - initial contribution
 */
public class ItemStatesSseBroadcaster extends SseBroadcaster {

    private final Logger logger = LoggerFactory.getLogger(ItemStatesSseBroadcaster.class);

    private Map<String, SseStateEventOutput> eventOutputs = new HashMap<>();

    private ItemRegistry itemRegistry;

    public ItemStatesSseBroadcaster(ItemRegistry itemRegistry) {
        super();
        this.itemRegistry = itemRegistry;
    }

    @Override
    public <OUT extends ChunkedOutput<OutboundEvent>> boolean add(OUT chunkedOutput) {
        if (chunkedOutput instanceof SseStateEventOutput) {
            SseStateEventOutput eventOutput = (SseStateEventOutput) chunkedOutput;
            OutboundEvent.Builder builder = new OutboundEvent.Builder();
            String connectionId = eventOutput.getConnectionId();
            try {
                eventOutputs.put(connectionId, eventOutput);
                eventOutput.writeDirect(builder.id("0").name("ready").data(connectionId).build());
            } catch (IOException e) {
                logger.error("Cannot write initial ready event to {}, discarding connection", connectionId);
                return false;
            }
        }

        return super.add(chunkedOutput);
    }

    @Override
    public <OUT extends ChunkedOutput<OutboundEvent>> boolean remove(OUT chunkedOutput) {
        eventOutputs.values().remove(chunkedOutput);
        return super.remove(chunkedOutput);
    }

    @Override
    public void onClose(ChunkedOutput<OutboundEvent> chunkedOutput) {
        remove(chunkedOutput);
    }

    @Override
    public void onException(ChunkedOutput<OutboundEvent> chunkedOutput, Exception exception) {
        remove(chunkedOutput);
    }

    /**
     * Updates the list of tracked items for a connection
     *
     * @param connectionId the connection id
     * @param newTrackedItems the list of items and their current state to send to the client
     */
    public void updateTrackedItems(String connectionId, Set<String> newTrackedItems) {
        SseStateEventOutput eventOutput = eventOutputs.get(connectionId);

        if (eventOutput == null) {
            throw new IllegalArgumentException("ConnectionId not found");
        }

        eventOutput.setTrackedItems(newTrackedItems);

        try {
            if (!eventOutput.isClosed()) {
                OutboundEvent event = buildStateEvent(newTrackedItems);
                if (event != null) {
                    eventOutput.writeDirect(event);
                }
            }
            if (eventOutput.isClosed()) {
                onClose(eventOutput);
            }
        } catch (IOException e) {
            onException(eventOutput, e);
        }
    }

    public OutboundEvent buildStateEvent(Set<String> itemNames) {
        Map<String, StateDTO> payload = new HashMap<>();
        for (String itemName : itemNames) {
            try {
                // Check that the item is tracked by at least one connection
                if (eventOutputs.values().stream().anyMatch(c -> c.getTrackedItems().contains(itemName))) {
                    Item item = itemRegistry.getItem(itemName);
                    StateDTO stateDto = new StateDTO();
                    stateDto.state = item.getState().toString();
                    String displayState = getDisplayState(item, Locale.getDefault());
                    // Only include the display state if it's different than the raw state
                    if (stateDto.state != null && !stateDto.state.equals(displayState)) {
                        stateDto.displayState = displayState;
                    }
                    payload.put(itemName, stateDto);
                }
            } catch (ItemNotFoundException e) {
                logger.warn("Attempting to send a state update of an item which doesn't exist: {}", itemName);
            }
        }

        if (!payload.isEmpty()) {
            OutboundEvent.Builder builder = new OutboundEvent.Builder();
            OutboundEvent event = builder.mediaType(MediaType.APPLICATION_JSON_TYPE).data(payload).build();
            return event;
        }

        return null;
    }

    protected String getDisplayState(Item item, Locale locale) {
        StateDescription stateDescription = item.getStateDescription(locale);
        State state = item.getState();
        String displayState = state.toString();

        if (!(state instanceof UnDefType)) {
            if (stateDescription != null) {
                if (!stateDescription.getOptions().isEmpty()) {
                    // Look for a state option with a label corresponding to the state
                    for (StateOption option : stateDescription.getOptions()) {
                        if (option.getValue().equals(state.toString()) && option.getLabel() != null) {
                            displayState = option.getLabel();
                            break;
                        }
                    }
                } else {
                    // If there's a pattern, first check if it's a transformation
                    String pattern = stateDescription.getPattern();
                    if (pattern != null) {
                        if (TransformationHelper.isTransform(pattern)) {
                            try {
                                displayState = TransformationHelper.transform(SseActivator.getContext(), pattern,
                                        state.toString());
                            } catch (NoClassDefFoundError ex) {
                                // TransformationHelper is optional dependency, so ignore if class not found
                                // return state as it is without transformation
                            } catch (TransformationException e) {
                                logger.warn("Failed transforming the state '{}' on item '{}' with pattern '{}': {}",
                                        state, item.getName(), pattern, e.getMessage());
                            }
                        } else {
                            // if it's not a transformation pattern, then it must be a format string

                            if (state instanceof QuantityType) {
                                QuantityType<?> quantityState = (QuantityType<?>) state;
                                // sanity convert current state to the item state description unit in case it was
                                // updated in the meantime. The item state is still in the "original" unit while the
                                // state description will display the new unit:
                                Unit<?> patternUnit = UnitUtils.parseUnit(pattern);
                                if (patternUnit != null && !quantityState.getUnit().equals(patternUnit)) {
                                    quantityState = quantityState.toUnit(patternUnit);
                                }

                                if (quantityState != null) {
                                    state = quantityState;
                                }
                            } else if (state instanceof DateTimeType) {
                                // Translate a DateTimeType state to the local time zone
                                try {
                                    state = ((DateTimeType) state).toLocaleZone();
                                } catch (DateTimeException e) {
                                }
                            }

                            // The following exception handling has been added to work around a Java bug with formatting
                            // numbers. See http://bugs.sun.com/view_bug.do?bug_id=6476425
                            // This also handles IllegalFormatConversionException, which is a subclass of
                            // IllegalArgument.
                            try {
                                displayState = state.format(pattern);
                            } catch (IllegalArgumentException e) {
                                logger.warn("Exception while formatting value '{}' of item {} with format '{}': {}",
                                        state, item.getName(), pattern, e.getMessage());
                                displayState = new String("Err");
                            }
                        }
                    }
                }
            }
        }

        return displayState;
    }
}
