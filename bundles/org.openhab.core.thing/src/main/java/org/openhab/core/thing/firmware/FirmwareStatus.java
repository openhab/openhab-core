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
package org.openhab.core.thing.firmware;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.firmware.Firmware;
import org.openhab.core.thing.binding.firmware.FirmwareUpdateHandler;

/**
 * The {@link FirmwareStatus} enumeration defines all possible statuses for the {@link Firmware} of a {@link Thing}
 * . The property {@link Thing#PROPERTY_FIRMWARE_VERSION} must be set for a thing in order that its firmware status can
 * be determined.
 *
 * @author Thomas Höfer - Initial contribution
 */
@NonNullByDefault
public enum FirmwareStatus {

    /**
     * The firmware status can not be determined and hence it is unknown. Either the
     * {@link Thing#PROPERTY_FIRMWARE_VERSION} is not set for the thing or there is no {@link FirmwareProvider} that
     * provides a firmware for the {@link ThingTypeUID} of the thing.
     */
    UNKNOWN,

    /** The firmware of the thing is up to date. */
    UP_TO_DATE,

    /**
     * There is a newer firmware of the thing available. However the thing is not in a state where its firmware can be
     * updated, i.e. the operation {@link FirmwareUpdateHandler#isUpdateExecutable()} returned false.
     */
    UPDATE_AVAILABLE,

    /** There is a newer firmware of the thing available and the firmware update for the thing can be executed. */
    UPDATE_EXECUTABLE;

}
