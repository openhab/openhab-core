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
package org.openhab.core.thing.binding.firmware;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link ProgressStep} enumeration defines the possible progress steps for a firmware update. The actual sequence
 * of the firmware update is defined by the operation {@link ProgressCallback#defineSequence(ProgressStep...)}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Chris Jackson - Add WAITING
 */
@NonNullByDefault
public enum ProgressStep {

    /**
     * The {@link FirmwareUpdateHandler} is going to download / read the firmware image by reading the input stream from
     * {@link Firmware#getBytes()}.
     */
    DOWNLOADING,

    /**
     * The {@link FirmwareUpdateHandler} is waiting for the device to initiate the transfer. For battery devices that
     * may wake up periodically, this may take some time. For mains devices this step may be very short or omitted.
     */
    WAITING,

    /** The {@link FirmwareUpdateHandler} is going to transfer the firmware to the actual device. */
    TRANSFERRING,

    /** The {@link FirmwareUpdateHandler} is going to trigger the firmware update for the actual device. */
    UPDATING,

    /** The {@link FirmwareUpdateHandler} is going to reboot the device. */
    REBOOTING;
}
