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
package org.openhab.core.io.rest.sse.internal;

import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.Unit;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.OutboundSseEvent.Builder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.sse.internal.dto.StateDTO;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.service.StartLevelService;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationHelper;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.openhab.core.types.util.UnitUtils;
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

    private static final Pattern EXTRACT_TRANSFORM_FUNCTION_PATTERN = Pattern.compile("(.*?)\\((.*)\\):(.*)");

    private final Logger logger = LoggerFactory.getLogger(SseItemStatesEventBuilder.class);

    private final ItemRegistry itemRegistry;
    private final LocaleService localeService;
    private final TimeZoneProvider timeZoneProvider;
    private final StartLevelService startLevelService;

    @Activate
    public SseItemStatesEventBuilder(final @Reference ItemRegistry itemRegistry,
            final @Reference LocaleService localeService, final @Reference TimeZoneProvider timeZoneProvider,
            final @Reference StartLevelService startLevelService) {
        this.itemRegistry = itemRegistry;
        this.localeService = localeService;
        this.timeZoneProvider = timeZoneProvider;
        this.startLevelService = startLevelService;
    }

    public @Nullable OutboundSseEvent buildEvent(Builder eventBuilder, Set<String> itemNames) {
        Map<String, StateDTO> payload = new HashMap<>(itemNames.size());
        for (String itemName : itemNames) {
            try {
                Item item = itemRegistry.getItem(itemName);
                StateDTO stateDto = new StateDTO();
                stateDto.state = item.getState().toString();
                stateDto.type = getStateType(item.getState());
                String displayState = getDisplayState(item, localeService.getLocale(null));
                // Only include the display state if it's different than the raw state
                if (stateDto.state != null && !stateDto.state.equals(displayState)) {
                    stateDto.displayState = displayState;
                }
                if (item.getState() instanceof DecimalType decimalState) {
                    stateDto.numericState = decimalState.floatValue();
                }
                if (item.getState() instanceof QuantityType quantityState) {
                    stateDto.numericState = quantityState.floatValue();
                    stateDto.unit = quantityState.getUnit().toString();
                }
                payload.put(itemName, stateDto);
            } catch (ItemNotFoundException e) {
                if (startLevelService.getStartLevel() >= StartLevelService.STARTLEVEL_MODEL) {
                    logger.warn("Attempting to send a state update of an item which doesn't exist: {}", itemName);
                }
            }
        }

        if (!payload.isEmpty()) {
            return eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE).data(payload).build();
        }

        return null;
    }

    protected @Nullable String getDisplayState(Item item, Locale locale) {
        StateDescription stateDescription = item.getStateDescription(locale);
        State state = item.getState();
        String displayState = state.toString();

        if (stateDescription != null) {
            String pattern = stateDescription.getPattern();
            // First check if the pattern is a transformation
            Matcher matcher;
            if (pattern != null && (matcher = EXTRACT_TRANSFORM_FUNCTION_PATTERN.matcher(pattern)).find()) {
                try {
                    String type = matcher.group(1);
                    String function = matcher.group(2);
                    String value = matcher.group(3);
                    TransformationService transformation = TransformationHelper.getTransformationService(type);
                    if (transformation != null) {
                        String format = state instanceof UnDefType ? "%s" : value;
                        try {
                            displayState = transformation.transform(function, state.format(format));
                            if (displayState == null) {
                                displayState = state.toString();
                            }
                        } catch (IllegalArgumentException e) {
                            throw new TransformationException(
                                    "Cannot format state '" + state + "' to format '" + format + "'", e);
                        } catch (RuntimeException e) {
                            throw new TransformationException("Transformation service of type '" + type
                                    + "' threw an exception: " + e.getMessage(), e);
                        }
                    } else {
                        throw new TransformationException(
                                "Transformation service of type '" + type + "' is not available.");
                    }
                } catch (TransformationException e) {
                    logger.warn("Failed transforming the state '{}' on item '{}' with pattern '{}': {}", state,
                            item.getName(), pattern, e.getMessage());
                }
            }
            // If no transformation, NULL/UNDEF state is returned as "NULL"/"UNDEF" without considering anything else
            else if (!(state instanceof UnDefType)) {
                boolean optionMatched = false;
                if (!stateDescription.getOptions().isEmpty()) {
                    // Look for a state option with a value corresponding to the state
                    for (StateOption option : stateDescription.getOptions()) {
                        String label = option.getLabel();
                        if (option.getValue().equals(state.toString()) && label != null) {
                            optionMatched = true;
                            try {
                                displayState = pattern == null ? label : String.format(pattern, label);
                            } catch (IllegalFormatException e) {
                                logger.debug(
                                        "Unable to format option label '{}' of item {} using format pattern '{}': {}, displaying option label",
                                        label, item.getName(), pattern, e.getMessage());
                                displayState = label;
                            }
                            break;
                        }
                    }
                }
                if (pattern != null && !optionMatched) {
                    // if it's not a transformation pattern and there is no matching state option,
                    // then it must be a format string
                    if (state instanceof QuantityType quantityState) {
                        // sanity convert current state to the item state description unit in case it was
                        // updated in the meantime. The item state is still in the "original" unit while the
                        // state description will display the new unit:
                        Unit<?> patternUnit = UnitUtils.parseUnit(pattern);
                        if (patternUnit != null && !quantityState.getUnit().equals(patternUnit)) {
                            quantityState = quantityState.toInvertibleUnit(patternUnit);
                        }

                        if (quantityState != null) {
                            state = quantityState;
                        }
                    }

                    // The following exception handling has been added to work around a Java bug with formatting
                    // numbers. See http://bugs.sun.com/view_bug.do?bug_id=6476425
                    // This also handles IllegalFormatConversionException, which is a subclass of
                    // IllegalArgument.
                    try {
                        if (state instanceof DateTimeType dateTimeState) {
                            displayState = dateTimeState.format(pattern, timeZoneProvider.getTimeZone());
                        } else {
                            displayState = state.format(pattern);
                        }
                    } catch (IllegalArgumentException e) {
                        logger.debug(
                                "Unable to format value '{}' of item {} using format pattern '{}': {}, displaying raw state",
                                state, item.getName(), pattern, e.getMessage());
                        displayState = state.toString();
                    }
                }
            }
        }

        return displayState;
    }

    // Taken from org.openhab.core.items.events.ItemEventFactory
    private static String getStateType(State state) {
        String stateClassName = state.getClass().getSimpleName();
        return stateClassName.substring(0, stateClassName.length() - "Type".length());
    }
}
