/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.io.transport.upnp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jupnp.UpnpService;
import org.jupnp.model.message.header.RootDeviceHeader;
import org.jupnp.model.message.header.UDNHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.types.UDN;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryListener;
import org.openhab.core.common.ThreadPoolManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UpnpDeviceFinder} is a component service that can be used to keep 'mis-behaving' UPnP devices
 * 'alive' in the registry by sending a targeted M-SEARCH 'ping' message for their UDN before their maxAge
 * expires.
 * <p>
 * Typically such a 'mis-behaving' device may fail to send regular NOTIFY alive messages, or fail to send
 * them in time. This component substitutes for such lack by sending a targeted M-SEARCH message 60 seconds
 * before the device's maxAge would normally expire.
 * <p>
 * For a component to use this service it must consume a final @Reference to this class in an @Activate method,
 * and call 'addUDN()' to start processing the given device, and respectively call 'removeUDN()') to stop it.
 *
 * @author Andrew Fiddian-Green - Initial Contribution
 */
@Component(immediate = true)
public class UpnpDeviceFinder implements RegistryListener {

    private static final String DEVICE_FINDER_THREADPOOL = "upnpDeviceFinder";

    private static final int SEARCH_SCHEDULE_SECONDS = 60;
    private static final int SEARCH_RETRY_MAX = 5;
    private static final int SEARCH_RETRY_INTERVAL = 5000;

    private final Logger logger = LoggerFactory.getLogger(UpnpDeviceFinder.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(DEVICE_FINDER_THREADPOOL);
    private final Map<UDN, ScheduledFuture<?>> subscriptions = new ConcurrentHashMap<>();

    private final UpnpService upnpService;

    @Activate
    public UpnpDeviceFinder(final @Reference UpnpService upnpService) {
        this.upnpService = upnpService;
    }

    @Activate
    public void activate() {
        upnpService.getRegistry().addListener(this);
        upnpService.getControlPoint().search();
        upnpService.getControlPoint().search(new RootDeviceHeader());
    }

    @Deactivate
    public void deactivate() {
        upnpService.getRegistry().removeListener(this);
        subscriptions.values().forEach(task -> task.cancel(false));
        subscriptions.clear();
    }

    /**
     * Cancel scheduled search (if any) for the given UDN.
     * May interrupt the executeSearch() method below.
     */
    private void cancelSearch(UDN udn) {
        ScheduledFuture<?> task = subscriptions.get(udn);
        if (task != null) {
            task.cancel(true);
        }
    }

    /**
     * Execute the search for SEARCH_RETRY_MAX attempts at SEARCH_RETRY_INTERVAL.
     * May be interrupted by the cancelSearch() method above.
     */
    private void executeSearch(UDN udn) {
        logger.debug("Executing search for {}", udn);
        for (int i = 0; i < SEARCH_RETRY_MAX; i++) {
            upnpService.getControlPoint().search(new UDNHeader(udn));
            try {
                Thread.sleep(SEARCH_RETRY_INTERVAL);
            } catch (InterruptedException cancelled) {
                return;
            }
        }
    }

    /**
     * Schedule a search for the given device after the given delay.
     */
    private void scheduleSearch(UDN udn, int delaySeconds) {
        cancelSearch(udn);
        logger.debug("Scheduling search for {} in {} seconds", udn, delaySeconds);
        subscriptions.put(udn, scheduler.schedule(() -> executeSearch(udn), delaySeconds, TimeUnit.SECONDS));
    }

    /**
     * Schedule a search for the given device at a future time based on its maxAge.
     */
    private void scheduleSearch(RemoteDevice device) {
        scheduleSearch(device.getIdentity().getUdn(),
                Math.max(SEARCH_SCHEDULE_SECONDS, device.getIdentity().getMaxAgeSeconds() - SEARCH_SCHEDULE_SECONDS));
    }

    /**
     * Add the given UPnP device UDN to the subscriptions list and execute an initial immediate search.
     */
    public void addUDN(UDN udn) {
        if (!subscriptions.containsKey(udn)) {
            logger.debug("Added subscription for {}", udn);
            scheduleSearch(udn, 0);
        }
    }

    /**
     * Remove the given UPnP device UDN from the subscriptions list and cancel any pending search.
     */
    public void removeUDN(UDN udn) {
        cancelSearch(udn);
        subscriptions.remove(udn);
        logger.debug("Removed subscription for {}", udn);
    }

    @Override
    public void afterShutdown() {
    }

    @Override
    public void beforeShutdown(Registry registry) {
        subscriptions.values().forEach(task -> task.cancel(false));
        subscriptions.clear();
    }

    @Override
    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
        remoteDeviceUpdated(registry, device);
    }

    @Override
    public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
        if (subscriptions.containsKey(device.getIdentity().getUdn())) {
            scheduleSearch(device);
        }
    }

    @Override
    public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
        if (subscriptions.containsKey(device.getIdentity().getUdn())) {
            UDN udn = device.getIdentity().getUdn();
            logger.warn("Device {} removed unexpectedly from registry", udn);
            scheduleSearch(udn, 0);
        }
    }

    @Override
    public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception e) {
        if (subscriptions.containsKey(device.getIdentity().getUdn())) {
            logger.warn("Discovery of {} failed with exception {}", device.getIdentity().getUdn(), e.getMessage());
        }
    }

    @Override
    public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
    }

    @Override
    public void localDeviceAdded(Registry registry, LocalDevice device) {
    }

    @Override
    public void localDeviceRemoved(Registry registry, LocalDevice device) {
    }
}
