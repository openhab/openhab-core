/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
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
     * Creates a discovery result for a UPnP device. Only the "root device" is discovered,
     * to find embedded/child devices, use {@link #enumerateAllDevices(RemoteDevice)}.
     *
     * @param device the UPnP device found on the network.
     * @return the resulting discovery result or {@code null}, if device is not
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

    /**
     * The JUPnP library strictly follows the UPnP specification insofar as if a device fails to send its next
     * 'ssdp:alive' notification within its declared 'maxAge' period, it is immediately considered to be gone. But
     * unfortunately some openHAB bindings handle devices that can sometimes be a bit late in sending their 'ssdp:alive'
     * notifications even though they have not really gone offline, which means that such devices are repeatedly removed
     * from, and (re)added to, the Inbox.
     *
     * To prevent this, a binding that implements a UpnpDiscoveryParticipant may OPTIONALLY implement this method to
     * specify an additional delay period (grace period) to wait before the device is removed from the Inbox.
     *
     * @param device the UPnP device on the network
     * @return the additional grace period delay in seconds before the device will be removed from the Inbox
     */
    default long getRemovalGracePeriodSeconds(RemoteDevice device) {
        return 0;
    }

    /**
     * Generates a {@link List} of {@link RemoteService}es offered by the specified {@link RemoteDevice} and
     * its embedded/child devices, if any.
     *
     * @param device the {@link RemoteDevice} whose services to enumerate.
     * @return The resulting {@link List} of {@link RemoteService}es.
     */
    static List<RemoteService> enumerateAllServices(RemoteDevice device) {
        List<RemoteService> result = new ArrayList<>();
        RemoteService[] services;
        for (RemoteDevice d : enumerateAllDevices(device)) {
            services = d.getServices();
            if (services != null && services.length > 0) {
                result.addAll(Arrays.asList(services));
            }
        }
        return result;
    }

    /**
     * Generates a {@link List} of the specified {@link RemoteDevice} itself and its embedded/child devices.
     *
     * @param device the {@link RemoteDevice} whose device tree to enumerate.
     * @return The resulting {@link List} of {@link RemoteDevice}s.
     */
    static List<RemoteDevice> enumerateAllDevices(RemoteDevice device) {
        List<RemoteDevice> result = new ArrayList<>();
        result.add(device);
        enumerateChildDevices(device, result);
        return result;
    }

    /**
     * Traverses and adds child/embedded devices to the provided {@link List} recursively.
     *
     * @param device the {@link RemoteDevice} whose descendants to add to {@code devices}.
     * @param devices the {@link List} to add the descendants to.
     */
    static void enumerateChildDevices(RemoteDevice device, List<RemoteDevice> devices) {
        for (RemoteDevice child : device.getEmbeddedDevices()) {
            devices.add(child);
            enumerateChildDevices(child, devices);
        }
    }
}
