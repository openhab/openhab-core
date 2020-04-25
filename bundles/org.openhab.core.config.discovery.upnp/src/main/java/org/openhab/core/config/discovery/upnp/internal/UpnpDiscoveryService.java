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
package org.openhab.core.config.discovery.upnp.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jupnp.UpnpService;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryListener;
import org.jupnp.transport.RouterException;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.config.discovery.upnp.UpnpDiscoveryParticipant;
import org.openhab.core.net.CidrAddress;
import org.openhab.core.net.NetworkAddressChangeListener;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link DiscoveryService} implementation, which can find UPnP devices in the network.
 * Support for further devices can be added by implementing and registering a {@link UpnpDiscoveryParticipant}.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Andre Fuechsel - Added call of removeOlderResults
 * @author Gary Tse - Add NetworkAddressChangeListener to handle interface changes
 * @author Tim Roberts - Added primary address change
 */
@Component(immediate = true, service = DiscoveryService.class, configurationPid = "discovery.upnp")
public class UpnpDiscoveryService extends AbstractDiscoveryService
        implements RegistryListener, NetworkAddressChangeListener {

    private final Logger logger = LoggerFactory.getLogger(UpnpDiscoveryService.class);

    private final Set<UpnpDiscoveryParticipant> participants = new CopyOnWriteArraySet<>();

    public UpnpDiscoveryService() {
        super(5);
    }

    private UpnpService upnpService;

    @Override
    protected void activate(Map<String, Object> configProperties) {
        super.activate(configProperties);
        startScan();
    }

    @Override
    @Modified
    protected void modified(Map<String, Object> configProperties) {
        super.modified(configProperties);
    }

    @Reference
    protected void setUpnpService(UpnpService upnpService) {
        this.upnpService = upnpService;
    }

    protected void unsetUpnpService(UpnpService upnpService) {
        this.upnpService = null;
    }

    @Reference
    protected void setNetworkAddressService(NetworkAddressService networkAddressService) {
        networkAddressService.addNetworkAddressChangeListener(this);
    }

    protected void unsetNetworkAddressService(NetworkAddressService networkAddressService) {
        networkAddressService.removeNetworkAddressChangeListener(this);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addUpnpDiscoveryParticipant(UpnpDiscoveryParticipant participant) {
        this.participants.add(participant);

        if (upnpService != null) {
            Collection<RemoteDevice> devices = upnpService.getRegistry().getRemoteDevices();
            for (RemoteDevice device : devices) {
                DiscoveryResult result = participant.createResult(device);
                if (result != null) {
                    thingDiscovered(result);
                }
            }
        }
    }

    protected void removeUpnpDiscoveryParticipant(UpnpDiscoveryParticipant participant) {
        this.participants.remove(participant);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        Set<ThingTypeUID> supportedThingTypes = new HashSet<>();
        for (UpnpDiscoveryParticipant participant : participants) {
            supportedThingTypes.addAll(participant.getSupportedThingTypeUIDs());
        }
        return supportedThingTypes;
    }

    @Override
    protected void startBackgroundDiscovery() {
        upnpService.getRegistry().addListener(this);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        upnpService.getRegistry().removeListener(this);
    }

    @Override
    protected void startScan() {
        for (RemoteDevice device : upnpService.getRegistry().getRemoteDevices()) {
            remoteDeviceAdded(upnpService.getRegistry(), device);
        }
        upnpService.getRegistry().addListener(this);
        upnpService.getControlPoint().search();
    }

    @Override
    protected synchronized void stopScan() {
        removeOlderResults(getTimestampOfLastScan());
        super.stopScan();
        if (!isBackgroundDiscoveryEnabled()) {
            upnpService.getRegistry().removeListener(this);
        }
    }

    @Override
    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
        for (UpnpDiscoveryParticipant participant : participants) {
            try {
                DiscoveryResult result = participant.createResult(device);
                if (result != null) {
                    thingDiscovered(result);
                }
            } catch (Exception e) {
                logger.error("Participant '{}' threw an exception", participant.getClass().getName(), e);
            }
        }
    }

    @Override
    public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
        for (UpnpDiscoveryParticipant participant : participants) {
            try {
                ThingUID thingUID = participant.getThingUID(device);
                if (thingUID != null) {
                    thingRemoved(thingUID);
                }
            } catch (Exception e) {
                logger.error("Participant '{}' threw an exception", participant.getClass().getName(), e);
            }
        }
    }

    @Override
    public void onChanged(final List<CidrAddress> added, final List<CidrAddress> removed) {
        scheduler.execute(() -> {
            if (!removed.isEmpty()) {
                upnpService.getRegistry().removeAllRemoteDevices();
            }

            try {
                upnpService.getRouter().disable();
                upnpService.getRouter().enable();

                startScan();
            } catch (RouterException e) {
                logger.error("Could not restart UPnP network components.", e);
            }
        });
    }

    @Override
    public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
    }

    @Override
    public void localDeviceAdded(Registry registry, LocalDevice device) {
    }

    @Override
    public void localDeviceRemoved(Registry registry, LocalDevice device) {
    }

    @Override
    public void beforeShutdown(Registry registry) {
    }

    @Override
    public void afterShutdown() {
    }

    @Override
    public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
    }

    @Override
    public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
    }
}
