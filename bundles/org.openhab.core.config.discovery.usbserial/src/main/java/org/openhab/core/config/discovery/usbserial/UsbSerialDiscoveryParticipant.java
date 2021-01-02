/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.config.discovery.usbserial;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.usbserial.internal.UsbSerialDiscoveryService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

/**
 * A {@link UsbSerialDiscoveryParticipant} that is registered as a component is picked up by the
 * {@link UsbSerialDiscoveryService} and can thus contribute {@link DiscoveryResult}s from
 * scans for USB devices with an associated serial port.
 *
 * @author Henning Sudbrock - Initial contribution
 */
@NonNullByDefault
public interface UsbSerialDiscoveryParticipant {

    /**
     * Defines the list of thing types that this participant can identify.
     *
     * @return a set of thing type UIDs for which results can be created
     */
    public Set<ThingTypeUID> getSupportedThingTypeUIDs();

    /**
     * Creates a discovery result for a USB device with corresponding serial port.
     *
     * @param deviceInformation information about the USB device and the corresponding serial port
     * @return the according discovery result or <code>null</code> if the device is not
     *         supported by this participant
     */
    public @Nullable DiscoveryResult createResult(UsbSerialDeviceInformation deviceInformation);

    /**
     * Returns the thing UID for a USB device with corresponding serial port.
     *
     * @param deviceInformation information about the USB device and the corresponding serial port
     * @return a thing UID or <code>null</code> if the device is not supported
     *         by this participant
     */
    public @Nullable ThingUID getThingUID(UsbSerialDeviceInformation deviceInformation);
}
