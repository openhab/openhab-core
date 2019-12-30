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
package org.openhab.core.config.discovery.mdns;

import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.mdns.internal.MDNSDiscoveryService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

/**
 * A {@link MDNSDiscoveryParticipant} that is registered as a service is picked up by the {@link MDNSDiscoveryService}
 * and can thus contribute {@link DiscoveryResult}s from
 * mDNS scans.
 *
 * @author Tobias Bräutigam - Initial contribution
 */
@NonNullByDefault
public interface MDNSDiscoveryParticipant {

    /**
     * Defines the list of thing types that this participant can identify
     *
     * @return a set of thing type UIDs for which results can be created
     */
    Set<ThingTypeUID> getSupportedThingTypeUIDs();

    /**
     * Defines the mDNS service type this participant listens to
     *
     * @return a valid mDNS service type (see: http://www.dns-sd.org/ServiceTypes.html)
     */
    String getServiceType();

    /**
     * Creates a discovery result for a mDNS service
     *
     * @param device the mDNS service found on the network
     * @return the according discovery result or <code>null</code>, if device is not
     *         supported by this participant
     */
    @Nullable
    DiscoveryResult createResult(ServiceInfo service);

    /**
     * Returns the thing UID for a mDNS service
     *
     * @param device the mDNS service on the network
     * @return a thing UID or <code>null</code>, if device is not supported
     *         by this participant
     */
    @Nullable
    ThingUID getThingUID(ServiceInfo service);
}
