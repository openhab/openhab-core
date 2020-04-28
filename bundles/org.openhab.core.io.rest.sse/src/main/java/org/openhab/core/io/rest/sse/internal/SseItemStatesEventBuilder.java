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

import java.time.DateTimeException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.measure.Unit;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.OutboundSseEvent.Builder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.rest.sse.internal.dto.StateDTO;
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
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SseItemStatesEventBuilder} builds {@link OutboundSseEvent}s for connections that listen to item state
 * changes.
 *
 * @author Yannick Schaus - Initial contribution
 * @author Wouter Born - Rework SSE item state sinks for dropping Glassfish
 */
@Component(service = SseItemStatesEventBuilder.class)
@NonNullByDefault
public class SseItemStatesEventBuilder {

    private final Logger logger = LoggerFactory.getLogger(SseItemStatesEventBuilder.class);

    private final BundleContext bundleContext;
    private final ItemRegistry itemRegistry;

    @Activate
    public SseItemStatesEventBuilder(final BundleContext bundleContext, final @Reference ItemRegistry itemRegistry) {
        this.bundleContext = bundleContext;
        this.itemRegistry = itemRegistry;
    }

    public @Nullable OutboundSseEvent buildEvent(Builder eventBuilder, Set<String> itemNames) {
        Map<String, StateDTO> payload = new HashMap<>(itemNames.size());
        for (String itemName : itemNames) {
            try {
                Item item = itemRegistry.getItem(itemName);
                StateDTO stateDto = new StateDTO();
                stateDto.state = item.getState().toString();
                String displayState = getDisplayState(item, Locale.getDefault());
                // Only include the display state if it's different than the raw state
                if (stateDto.state != null && !stateDto.state.equals(displayState)) {
                    stateDto.displayState = displayState;
                }
                payload.put(itemName, stateDto);
            } catch (ItemNotFoundException e) {
                logger.warn("Attempting to send a state update of an item which doesn't exist: {}", itemName);
            }
        }

        if (!payload.isEmpty()) {
            return eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE).data(payload).build();
        }

        return null;
    }

    private @Nullable String getDisplayState(Item item, Locale locale) {
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
                                displayState = TransformationHelper.transform(bundleContext, pattern, state.toString());
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
