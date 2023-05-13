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
package org.openhab.core.thing;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.CallItem;
import org.openhab.core.library.items.ColorItem;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.DateTimeItem;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.ImageItem;
import org.openhab.core.library.items.LocationItem;
import org.openhab.core.library.items.PlayerItem;
import org.openhab.core.library.items.RollershutterItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.NextPreviousType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.PointType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.RawType;
import org.openhab.core.library.types.RewindFastforwardType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;

/**
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class CommunicationManagerConversionTest {
    // TODO: remove test - only to show CommunicationManager is too complex

    private static final List<Class<? extends Item>> itemTypes = List.of(CallItem.class, ColorItem.class,
            ContactItem.class, DateTimeItem.class, DimmerItem.class, ImageItem.class, LocationItem.class,
            PlayerItem.class, RollershutterItem.class, StringItem.class);

    private static final List<Class<? extends Type>> types = List.of(DateTimeType.class, DecimalType.class,
            HSBType.class, IncreaseDecreaseType.class, NextPreviousType.class, OnOffType.class, OpenClosedType.class,
            PercentType.class, PlayPauseType.class, PointType.class, QuantityType.class, RawType.class,
            RewindFastforwardType.class, StringType.class, UpDownType.class, UnDefType.class);

    private static Stream<Arguments> arguments()
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<Arguments> arguments = new ArrayList<>();
        for (Class<? extends Item> itemType : itemTypes) {
            Item item = itemType.getDeclaredConstructor(String.class).newInstance("testItem");
            for (Class<? extends Type> type : types) {
                if (type.isEnum()) {
                    arguments.add(Arguments.of(item, type.getEnumConstants()[0]));
                } else if (type == RawType.class) {
                    arguments.add(Arguments.of(item, new RawType(new byte[] {}, "mimeType")));
                } else {
                    arguments.add(Arguments.of(item, type.getDeclaredConstructor().newInstance()));
                }
            }
        }
        return arguments.stream();
    }

    @Disabled
    @MethodSource("arguments")
    @ParameterizedTest
    public void testCommand(Item item, Type originalType) {
        Type returnType = null;

        List<Class<? extends Command>> acceptedTypes = item.getAcceptedCommandTypes();
        if (acceptedTypes.contains(originalType.getClass())) {
            returnType = originalType;
        } else {
            // Look for class hierarchy and convert appropriately
            for (Class<? extends Type> typeClass : acceptedTypes) {
                if (!typeClass.isEnum() && typeClass.isAssignableFrom(originalType.getClass()) //
                        && State.class.isAssignableFrom(typeClass) && originalType instanceof State state) {
                    returnType = state.as((Class<? extends State>) typeClass);
                }
            }
        }

        if (returnType != null && !returnType.getClass().equals(originalType.getClass())) {
            fail("CommunicationManager did a conversion for target item " + item.getType() + " from "
                    + originalType.getClass() + " to " + returnType.getClass());
        }
    }

    @MethodSource("arguments")
    @ParameterizedTest
    public void testState(Item item, Type originalType) {
        Type returnType = null;

        List<Class<? extends State>> acceptedTypes = item.getAcceptedDataTypes();
        if (acceptedTypes.contains(originalType.getClass())) {
            returnType = originalType;
        } else {
            // Look for class hierarchy and convert appropriately
            for (Class<? extends Type> typeClass : acceptedTypes) {
                if (!typeClass.isEnum() && typeClass.isAssignableFrom(originalType.getClass()) //
                        && State.class.isAssignableFrom(typeClass) && originalType instanceof State state) {
                    returnType = state.as((Class<? extends State>) typeClass);

                }
            }
        }

        if (returnType != null && !returnType.equals(originalType)) {
            fail("CommunicationManager did a conversion for target item " + item.getType() + " from "
                    + originalType.getClass() + " to " + returnType.getClass());
        }
    }
}
