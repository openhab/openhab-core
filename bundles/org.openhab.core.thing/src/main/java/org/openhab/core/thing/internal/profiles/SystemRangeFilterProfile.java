/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.thing.profiles.SystemProfiles;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * This is the default implementation for a {@link SystemRangeFilterProfile}.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class SystemRangeFilterProfile implements StateProfile {

    static final String RANGE_PARAM = "range";
    static final String RANGE_ACTION_PARAM = "action";
    static final String RANGE_ACTION_ALLOW = "allow";
    static final String RANGE_ACTION_DISCARD = "discard";

    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "^(?<beginType>\\[|\\()\\s*(?<begin>[^\\]\\)]*?)\\s*\\.\\.\\s*(?<end>[^\\]\\)]*?)\\s*(?<endType>\\]|\\))$");

    private final Logger logger = LoggerFactory.getLogger(SystemRangeFilterProfile.class);

    private final ProfileCallback callback;

    private final List<Range> ranges;
    private final boolean inverted;

    public SystemRangeFilterProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;

        String rangeAction = ConfigParser.valueAsOrElse(context.getConfiguration().get(RANGE_ACTION_PARAM),
                String.class, RANGE_ACTION_ALLOW);

        this.inverted = switch (rangeAction) {
            case RANGE_ACTION_ALLOW -> false;
            case RANGE_ACTION_DISCARD -> true;
            default -> throw new IllegalArgumentException(
                    String.format("Invalid %s option: '%s'. Valid options are: '%s' or '%s'", linkUID(),
                            RANGE_ACTION_PARAM, rangeAction, RANGE_ACTION_ALLOW, RANGE_ACTION_DISCARD));
        };

        Object rangeConfig = context.getConfiguration().get(RANGE_PARAM);
        if (!(rangeConfig instanceof String rangeStr)) {
            throw new IllegalArgumentException(
                    String.format("%s: Invalid range parameter: '%s'", linkUID(), rangeConfig));
        }

        List<Range> ranges = new ArrayList<>();
        Arrays.stream(rangeStr.split(",")).map(String::trim).filter(s -> !s.isBlank()).forEach(rangeElement -> {
            Matcher rangeMatcher = RANGE_PATTERN.matcher(rangeElement);
            if (rangeMatcher.matches()) {
                Optional<Range> range = createRange(rangeElement, rangeMatcher.group("beginType"),
                        rangeMatcher.group("begin"), rangeMatcher.group("end"), rangeMatcher.group("endType"));
                range.ifPresent(r -> ranges.add(r));
            } else {
                throw new IllegalArgumentException(
                        String.format("%s: Invalid range syntax '%s'.", linkUID(), rangeElement));
            }
        });

        if (ranges.isEmpty()) {
            throw new IllegalArgumentException(
                    linkUID() + ": No valid range specifications found. Everything will be discarded.");
        }
        this.ranges = Collections.unmodifiableList(ranges);
    }

    private Optional<Range> createRange(String range, String beginType, String rangeBegin, String rangeEnd,
            String endType) {
        Optional<Comparator> beginComparator = Optional.empty();
        Optional<Comparator> endComparator = Optional.empty();
        Unit unit = Units.ONE;
        Optional<BigDecimal> beginValueCheck = Optional.empty();

        if (!rangeBegin.isBlank()) {
            try {
                QuantityType<?> value = QuantityType.valueOf(rangeBegin);
                final BigDecimal beginValue = value.toBigDecimal();
                beginComparator = Optional.of(switch (beginType) {
                    case "[" -> (input) -> input.compareTo(beginValue) >= 0;
                    case "(" -> (input) -> input.compareTo(beginValue) > 0;
                    default -> throw new IllegalStateException(String.format(
                            "Invalid begin type '%s' of the filter range '%s'. This is a bug.", beginType, range));
                });
                unit = value.getUnit();
                beginValueCheck = Optional.of(beginValue);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        String.format("%s: Invalid filter range begin: '%s'. %s", linkUID(), range, e.getMessage()));
            }
        }

        if (!rangeEnd.isBlank()) {
            try {
                @Nullable
                QuantityType<?> value = QuantityType.valueOf(rangeEnd);
                if (beginComparator.isPresent()) {
                    value = value.toInvertibleUnit(unit);
                }
                if (value != null) {
                    final BigDecimal endValue = value.toBigDecimal();
                    endComparator = Optional.of(switch (endType) {
                        case "]" -> (input) -> input.compareTo(endValue) <= 0;
                        case ")" -> (input) -> input.compareTo(endValue) < 0;
                        default -> throw new IllegalStateException(String.format(
                                "Invalid end type '%s' of the filter range '%s'. This is a bug.", endType, range));
                    });
                    if (beginComparator.map(begin -> !begin.check(endValue)).orElse(false)) {
                        throw new IllegalArgumentException("The end value is smaller than the begin limit.");
                    }
                    if (beginValueCheck.isPresent() && !endComparator.get().check(beginValueCheck.get())) {
                        throw new IllegalArgumentException("The begin value is bigger than the end limit.");
                    }
                } else {
                    throw new IllegalArgumentException("Begin and end have incompatible units.");
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        String.format("%s: Invalid filter range end: '%s'. %s", linkUID(), range, e.getMessage()));
            }
        }

        if (beginComparator.isEmpty() && endComparator.isEmpty()) {
            throw new IllegalArgumentException(String.format("%s: The range '%s' is empty.", linkUID(), range));
        }

        return Optional.of(new Range(range, beginComparator, endComparator, unit));
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return SystemProfiles.RANGE;
    }

    @Override
    public void onCommandFromItem(Command command) {
        callback.handleCommand(command);
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        // do nothing
    }

    private @Nullable QuantityType<?> toQuantity(final Type value) {
        if (value instanceof QuantityType quantity) {
            return quantity;
        } else if (value instanceof DecimalType decimal) {
            return new QuantityType(decimal, Units.ONE);
        }
        return null;
    }

    @Override
    public void onCommandFromHandler(Command command) {
        if (isAllowed(command)) {
            callback.sendCommand(command);
        } else {
            logger.debug("{}: Command '{}' discarded by filter profile.", linkUID(), command);
        }
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        if (isAllowed(state)) {
            callback.sendUpdate(state);
        } else {
            logger.debug("{}: State update '{}' discarded by filter profile.", linkUID(), state);
        }
    }

    private boolean isAllowed(Type value) {
        @Nullable
        QuantityType<?> quantityValue = toQuantity(value);
        if (quantityValue == null) {
            return false;
        }

        boolean withinRange = isWithinAnyRange(quantityValue);
        return inverted ? !withinRange : withinRange;
    }

    private boolean isWithinAnyRange(QuantityType<?> value) {
        return ranges.stream().anyMatch(range -> {
            @Nullable
            QuantityType<?> checkValue = value;
            if (!range.unit().equals(Units.ONE)) {
                checkValue = value.toInvertibleUnit(range.unit());
                if (checkValue == null) {
                    logger.warn("{}: Incompatible units between the incoming value '{}' and the range '{}'.", linkUID(),
                            value, range.range());
                    return false;
                }
            }
            return range.covers(checkValue.toBigDecimal());
        });
    }

    private String linkUID() {
        return callback.getItemChannelLink().getUID();
    }

    private interface Comparator {
        boolean check(BigDecimal value);
    }

    private record Range(String range, Optional<Comparator> begin, Optional<Comparator> end, Unit unit) {
        boolean covers(BigDecimal value) {
            return begin.map(c -> c.check(value)).orElse(true) && end.map(c -> c.check(value)).orElse(true);
        }
    }
}
