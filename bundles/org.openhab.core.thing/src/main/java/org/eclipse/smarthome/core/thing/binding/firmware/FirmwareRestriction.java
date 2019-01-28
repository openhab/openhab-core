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
package org.eclipse.smarthome.core.thing.binding.firmware;

import java.util.function.Function;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.firmware.FirmwareProvider;

/**
 * A function for defining specific installation restrictions for a given {@link Firmware}.
 * <br>
 * <br>
 * <b>Example:</b> Consider a device where:
 * <ul>
 * <li>the firmware with version 5 must only be installed if the device currently
 * has firmware version 1 installed;
 * <li>the firmware with version 4 can only be installed if the device currently has firmware version 3 installed.
 * </ul>
 * 
 * In such case the restrictions function can be defined as follows in the {@link FirmwareProvider}:
 * 
 * <pre>
 * {
 *     &#64;code
 *     Firmware firmwareV5 = FirmwareBuilder.create(thingTypeUID, "5").withCustomRestrictions(
 *             // Hardware version A
 *             thing -> "1".equals(thing.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION))).build();
 * 
 *     Firmware firmwareV4 = FirmwareBuilder.create(thingTypeUID, "4").withCustomRestrictions(
 *             // Hardware version B
 *             thing -> "3".equals(thing.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION))).build();
 * }
 * </pre>
 * 
 * @author Dimitar Ivanov - Initial contribution
 *
 */
public interface FirmwareRestriction extends Function<Thing, Boolean> {
}
