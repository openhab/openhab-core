/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.transform.util;

import java.time.ZoneId;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationHelper;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.openhab.core.types.util.UnitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for getting the display state of an Item from its {@link StateDescription}.
 *
 * @author Florian Hotze - Initial contribution (extracted from SseItemStatesEventBuilder)
 */
@NonNullByDefault
public class ItemDisplayStateUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemDisplayStateUtil.class);

    private static final Pattern EXTRACT_TRANSFORM_FUNCTION_PATTERN = Pattern.compile("(.*?)\\((.*)\\):(.*)");

    /**
     * Transform an arbitrary value with a transformation service
     *
     * @param serviceName the name of the transformation service
     * @param function the name of the transformation function
     * @param value the value to transform
     * @return the transformed state, can be null if the transformation function returns null
     * @throws TransformationException when state formatting failed, the transformation service is unavailable, or the
     *             transformation failed
     */
    public static @Nullable String transform(String serviceName, String function, String value)
            throws TransformationException {
        TransformationService transformation = TransformationHelper.getTransformationService(serviceName);
        if (transformation != null) {
            try {
                return transformation.transform(function, value);
            } catch (RuntimeException e) {
                throw new TransformationException(
                        "Transformation service of type '" + serviceName + "' threw an exception: " + e.getMessage(),
                        e);
            }
        } else {
            throw new TransformationException("Transformation service of type '" + serviceName + "' is not available.");
        }
    }

    /**
     * Transform an Item state with a transformation service
     *
     * @param serviceName the name of the transformation service
     * @param function the name of the transformation function
     * @param format the format to apply to the state before applying the transformation function
     * @param state the state to transform
     * @return the transformed state, can be null if the transformation function returns null
     * @throws TransformationException when state formatting failed, the transformation service is unavailable, or the
     *             transformation failed
     */
    public static @Nullable String transform(String serviceName, String function, String format, State state)
            throws TransformationException {
        String effectiveFormat = state instanceof UnDefType ? "%s" : format;
        try {
            return transform(serviceName, function, state.format(effectiveFormat));
        } catch (IllegalArgumentException e) {
            throw new TransformationException("Cannot format state '" + state + "' to format '" + effectiveFormat + "'",
                    e);
        }
    }

    /**
     * Get the display state of an item.
     *
     * @param item the item
     * @param locale the locale
     * @param zoneId the timezone id
     * @return the display state
     */
    public static @Nullable String getDisplayState(Item item, @Nullable Locale locale, ZoneId zoneId) {
        return getDisplayState(item, item.getState(), locale, zoneId);
    }

    /**
     * Get the display state of an item for a given state.
     *
     * @param item the item
     * @param state the state
     * @param locale the locale
     * @param zoneId the timezone id
     * @return the display state
     */
    public static @Nullable String getDisplayState(Item item, State state, @Nullable Locale locale, ZoneId zoneId) {
        StateDescription stateDescription = item.getStateDescription(locale);
        if (stateDescription == null) {
            return state.toString();
        }
        return formatState(item.getName(), stateDescription.getPattern(), stateDescription.getOptions(), state, zoneId);
    }

    /**
     * Format a state with the provided pattern.
     *
     * @param pattern the pattern to format
     * @param state the state
     * @param zoneId the timezone id
     * @return the display state
     */
    public static @Nullable String formatState(String itemName, @Nullable String pattern, List<StateOption> options,
            State state, ZoneId zoneId) {
        String displayState = state.toString();

        // First check if the pattern is a transformation
        Matcher matcher;
        if (pattern != null && (matcher = EXTRACT_TRANSFORM_FUNCTION_PATTERN.matcher(pattern)).find()) {
            try {
                String type = matcher.group(1);
                String function = matcher.group(2);
                String format = matcher.group(3);

                displayState = transform(type, function, format, state);
                if (displayState == null) {
                    displayState = state.toString();
                }
            } catch (TransformationException e) {
                LOGGER.warn("Failed transforming the state '{}' on item '{}' with pattern '{}': {}", state, itemName,
                        pattern, e.getMessage());
            }
        }
        // If no transformation, NULL/UNDEF state is returned as "NULL"/"UNDEF" without considering anything else
        else if (!(state instanceof UnDefType)) {
            boolean optionMatched = false;
            if (!options.isEmpty()) {
                // Look for a state option with a value corresponding to the state
                for (StateOption option : options) {
                    String label = option.getLabel();
                    if (option.getValue().equals(state.toString()) && label != null) {
                        optionMatched = true;
                        try {
                            displayState = pattern == null ? label : String.format(pattern, label);
                        } catch (IllegalFormatException e) {
                            LOGGER.debug(
                                    "Unable to format option label '{}' of item {} using format pattern '{}': {}, displaying option label",
                                    label, itemName, pattern, e.getMessage());
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
                        displayState = dateTimeState.format(pattern, zoneId);
                    } else {
                        displayState = state.format(pattern);
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.debug(
                            "Unable to format value '{}' of item {} using format pattern '{}': {}, displaying raw state",
                            state, itemName, pattern, e.getMessage());
                    displayState = state.toString();
                }
            }
        }

        return displayState;
    }
}
