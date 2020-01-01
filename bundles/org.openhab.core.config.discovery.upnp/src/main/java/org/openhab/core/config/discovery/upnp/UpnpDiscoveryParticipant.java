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
package org.openhab.core.config.discovery.upnp;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.model.meta.RemoteDevice;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

/**
 * A {@link UpnpDiscoveryParticipant} that is registered as a service is picked up by the UpnpDiscoveryService
 * and can thus contribute {@link DiscoveryResult}s from
 * UPnP scans.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface UpnpDiscoveryParticipant {

    /**
     * According to the UPnP specification, the minimum MaxAge is 1800 seconds.
     */
    long MIN_MAX_AGE_SECS = 1800;

    /**
     * Defines the list of thing types that this participant can identify
     *
     * @return a set of thing type UIDs for which results can be created
     */
    Set<ThingTypeUID> getSupportedThingTypeUIDs();

    /**
     * Creates a discovery result for a upnp device
     *
     * @param device the upnp device found on the network
     * @return the according discovery result or <code>null</code>, if device is not
     *         supported by this participant
     */
    @Nullable
    DiscoveryResult createResult(RemoteDevice device);

    /**
     * Returns the thing UID for a upnp device
     *
     * @param device the upnp device on the network
     * @return a thing UID or <code>null</code>, if device is not supported
     *         by this participant
     */
    @Nullable
    ThingUID getThingUID(RemoteDevice device);
}
