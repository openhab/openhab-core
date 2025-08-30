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
package org.openhab.core.internal.items;

import java.util.ArrayList;
import java.util.List;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.items.Item;
import org.openhab.core.items.dto.GroupFunctionDTO;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.ArithmeticGroupFunction;
import org.openhab.core.library.types.DateTimeGroupFunction;
import org.openhab.core.library.types.QuantityTypeArithmeticGroupFunction;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.openhab.core.types.TypeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link GroupFunction}s according to the given parameters.
 *
 * @author Henning Treu - Initial contribution
 * @author Robert Michalak - LATEST and EARLIEST group functions
 */
@NonNullByDefault
public class GroupFunctionHelper {

    private final Logger logger = LoggerFactory.getLogger(GroupFunctionHelper.class);

    /**
     * Creates a {@link GroupFunction} according to the given parameters. In case dimension is given the resulting
     * arithmetic group function will take unit conversion into account.
     *
     * @param function the {@link GroupFunctionDTO} describing the group function.
     * @param baseItem an optional {@link Item} defining the dimension/unit for unit conversion.
     * @return a {@link GroupFunction} according to the given parameters.
     */
    public GroupFunction createGroupFunction(GroupFunctionDTO function, @Nullable Item baseItem) {
        if (baseItem instanceof NumberItem baseNumberItem && baseNumberItem.getDimension() != null) {
            return createDimensionGroupFunction(function, baseNumberItem);
        }
        return createDefaultGroupFunction(function, baseItem);
    }

    private List<State> parseStates(@Nullable Item baseItem, String @Nullable [] params) {
        if (params == null || baseItem == null) {
            return List.of();
        }

        List<State> states = new ArrayList<>();
        for (String param : params) {
            State state = TypeParser.parseState(baseItem.getAcceptedDataTypes(), param);
            if (state == null) {
                logger.warn("State '{}' is not valid for a group item with base type '{}'", param, baseItem.getType());
                states.clear();
                break;
            } else {
                states.add(state);
            }
        }
        return states;
    }

    private GroupFunction createDimensionGroupFunction(GroupFunctionDTO function, NumberItem baseItem) {
        final String functionName = function.name;
        Unit<?> baseItemUnit = baseItem.getUnit();
        if (baseItemUnit != null) {
            switch (functionName.toUpperCase()) {
                case GroupFunction.AVG:
                    return new QuantityTypeArithmeticGroupFunction.Avg(baseItemUnit);
                case GroupFunction.MEDIAN:
                    return new QuantityTypeArithmeticGroupFunction.Median(baseItemUnit);
                case GroupFunction.SUM:
                    return new QuantityTypeArithmeticGroupFunction.Sum(baseItemUnit);
                case GroupFunction.MIN:
                    return new QuantityTypeArithmeticGroupFunction.Min(baseItemUnit);
                case GroupFunction.MAX:
                    return new QuantityTypeArithmeticGroupFunction.Max(baseItemUnit);
                default:
            }
        }
        return createDefaultGroupFunction(function, baseItem);
    }

    private GroupFunction createDefaultGroupFunction(GroupFunctionDTO function, @Nullable Item baseItem) {
        final String functionName = function.name;
        final List<State> args;
        switch (functionName.toUpperCase()) {
            case GroupFunction.AND:
                args = parseStates(baseItem, function.params);
                if (args.size() == 2) {
                    return new ArithmeticGroupFunction.And(args.getFirst(), args.get(1));
                } else {
                    logger.error("Group function 'AND' requires two arguments. Using Equality instead.");
                }
                break;
            case GroupFunction.OR:
                args = parseStates(baseItem, function.params);
                if (args.size() == 2) {
                    return new ArithmeticGroupFunction.Or(args.getFirst(), args.get(1));
                } else {
                    logger.error("Group function 'OR' requires two arguments. Using Equality instead.");
                }
                break;
            case GroupFunction.NAND:
                args = parseStates(baseItem, function.params);
                if (args.size() == 2) {
                    return new ArithmeticGroupFunction.NAnd(args.getFirst(), args.get(1));
                } else {
                    logger.error("Group function 'NOT AND' requires two arguments. Using Equality instead.");
                }
                break;
            case GroupFunction.NOR:
                args = parseStates(baseItem, function.params);
                if (args.size() == 2) {
                    return new ArithmeticGroupFunction.NOr(args.getFirst(), args.get(1));
                } else {
                    logger.error("Group function 'NOT OR' requires two arguments. Using Equality instead.");
                }
                break;
            case GroupFunction.XOR:
                args = parseStates(baseItem, function.params);
                if (args.size() == 2) {
                    return new ArithmeticGroupFunction.Xor(args.getFirst(), args.get(1));
                } else {
                    logger.error("Group function 'XOR' requires two arguments. Using Equality instead.");
                }
                break;
            case GroupFunction.COUNT:
                if (function.params != null && function.params.length == 1) {
                    State countParam = new StringType(function.params[0]);
                    return new ArithmeticGroupFunction.Count(countParam);
                } else {
                    logger.error("Group function 'COUNT' requires one argument. Using Equality instead.");
                }
                break;
            case GroupFunction.AVG:
                return new ArithmeticGroupFunction.Avg();
            case GroupFunction.MEDIAN:
                return new ArithmeticGroupFunction.Median();
            case GroupFunction.SUM:
                return new ArithmeticGroupFunction.Sum();
            case GroupFunction.MIN:
                return new ArithmeticGroupFunction.Min();
            case GroupFunction.MAX:
                return new ArithmeticGroupFunction.Max();
            case GroupFunction.LATEST:
                return new DateTimeGroupFunction.Latest();
            case GroupFunction.EARLIEST:
                return new DateTimeGroupFunction.Earliest();
            case GroupFunction.EQUALITY:
                return new GroupFunction.Equality();
            default:
                logger.error("Unknown group function '{}'. Using Equality instead.", functionName);
        }
        return new GroupFunction.Equality();
    }
}
