package org.openhab.core.io.transport.upnp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
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
 * Component service that can be used to keep mis-behaving UPnP devices stored in the registry
 * by sending a targeted M-SEARCH message for their UDN before their maxAge expires.
 *
 * @author Andrew Fiddian-Green - Initial Contribution
 */
@Component(immediate = true)
public class UpnpDeviceFinder implements RegistryListener {

    private static final String DEVICE_FINDER_THREADPOOL = "upnpDeviceFinder";
    private static final int SEARCH_SCHEDULE_SECONDS = 60;

    private final Logger logger = LoggerFactory.getLogger(UpnpDeviceFinder.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(DEVICE_FINDER_THREADPOOL);
    private final Map<UDN, Future<?>> subscriptions = new ConcurrentHashMap<>();

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
     */
    private void cancelSearch(UDN udn) {
        Future<?> task = subscriptions.get(udn);
        if (task != null) {
            task.cancel(false);
        }
    }

    /**
     * Schedule a search for the given device at a future time based on its maxAge.
     */
    private void scheduleSearch(RemoteDevice device) {
        UDN udn = device.getIdentity().getUdn();
        int delaySeconds = Math.max(SEARCH_SCHEDULE_SECONDS,
                device.getIdentity().getMaxAgeSeconds() - SEARCH_SCHEDULE_SECONDS);
        logger.debug("Scheduling search for {} in {} seconds", udn, delaySeconds);
        cancelSearch(udn);
        subscriptions.put(udn, scheduler.schedule(() -> {
            logger.debug("Executing search for {}", udn);
            upnpService.getControlPoint().search(new UDNHeader(udn));
        }, delaySeconds, TimeUnit.SECONDS));
    }

    /**
     * Add the given UPnP device UDN to the subscriptions list and execute an initial immediate search.
     */
    public void addUDN(UDN udn) {
        if (!subscriptions.containsKey(udn)) {
            logger.debug("Added subscription for {}", udn);
            subscriptions.put(udn, scheduler.submit(() -> upnpService.getControlPoint().search(new UDNHeader(udn))));
        }
    }

    /**
     * Remove the given UPnP device UDN from the subscriptions list and cancel any pending search.
     */
    public void removeUDN(UDN udn) {
        subscriptions.remove(udn);
        cancelSearch(udn);
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
    public void localDeviceAdded(Registry registry, LocalDevice device) {
    }

    @Override
    public void localDeviceRemoved(Registry registry, LocalDevice device) {
    }

    @Override
    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
        if (subscriptions.containsKey(device.getIdentity().getUdn())) {
            scheduleSearch(device);
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
    public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
        if (subscriptions.containsKey(device.getIdentity().getUdn())) {
            UDN udn = device.getIdentity().getUdn();
            logger.warn("Device {} removed unexpectedly from registry; Executing search", udn);
            cancelSearch(udn);
            subscriptions.put(udn, scheduler.submit(() -> upnpService.getControlPoint().search(new UDNHeader(udn))));
        }
    }

    @Override
    public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
        if (subscriptions.containsKey(device.getIdentity().getUdn())) {
            scheduleSearch(device);
        }
    }
}
