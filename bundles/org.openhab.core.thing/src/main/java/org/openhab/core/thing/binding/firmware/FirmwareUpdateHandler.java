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
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.firmware.FirmwareUpdateService;

/**
 * The {@link FirmwareUpdateHandler} can be implemented and registered as an OSGi service in order to update the
 * firmware for the physical device of a {@link Thing}. The {@link FirmwareUpdateService} tracks each firmware
 * update handler and starts the firmware update process by the operation
 * {@link FirmwareUpdateService#updateFirmware(org.openhab.core.thing.ThingUID, FirmwareUID, java.util.Locale)}
 * .
 *
 * @author Thomas Höfer - Initial contribution
 */
@NonNullByDefault
public interface FirmwareUpdateHandler {

    /**
     * Returns the {@link Thing} that is handled by this firmware update handler.
     *
     * @return the thing that is handled by this firmware update handler (not null)
     */
    Thing getThing();

    /**
     * Updates the firmware for the physical device of the thing that is handled by this firmware update handler.
     *
     * @param firmware the new firmware to be updated (not null)
     * @param progressCallback the progress callback to send progress information of the firmware update process (not
     *            null)
     */
    void updateFirmware(Firmware firmware, ProgressCallback progressCallback);

    /**
     * Cancels a previous started firmware update.
     */
    void cancel();

    /**
     * Returns true, if this firmware update handler is in a state in which the firmware update can be executed,
     * otherwise false (e.g. the thing is {@link ThingStatus#OFFLINE} or its status detail is already
     * {@link ThingStatusDetail#FIRMWARE_UPDATING.)
     *
     * @return true, if this firmware update handler is in a state in which the firmware update can be executed,
     *         otherwise false
     */
    boolean isUpdateExecutable();

}
