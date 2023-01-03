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
     * @param service the mDNS service found on the network
     * @return the according discovery result or <code>null</code>, if device is not
     *         supported by this participant
     */
    @Nullable
    DiscoveryResult createResult(ServiceInfo service);

    /**
     * Returns the thing UID for a mDNS service
     *
     * @param service the mDNS service on the network
     * @return a thing UID or <code>null</code>, if device is not supported
     *         by this participant
     */
    @Nullable
    ThingUID getThingUID(ServiceInfo service);

    /**
     * Some openHAB bindings handle devices that can sometimes be a bit late in updating their mDNS announcements, which
     * means that such devices are repeatedly removed from, and (re)added to, the Inbox.
     *
     * To prevent this, a binding that implements an MDNSDiscoveryParticipant may OPTIONALLY implement this method to
     * specify an additional delay period (grace period) to wait before the device is removed from the Inbox.
     *
     * @param serviceInfo the mDNS ServiceInfo describing the device on the network.
     * @return the additional grace period delay in seconds before the device will be removed from the Inbox.
     */
    default long getRemovalGracePeriodSeconds(ServiceInfo serviceInfo) {
        return 0;
    }
}
