/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.config.discovery.upnp.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.config.discovery.upnp.UpnpDiscoveryParticipant;
import org.eclipse.smarthome.core.net.CidrAddress;
import org.eclipse.smarthome.core.net.NetworkAddressChangeListener;
import org.eclipse.smarthome.core.net.NetworkAddressService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.jupnp.UpnpService;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryListener;
import org.jupnp.transport.RouterException;
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
 *
 */
@Component(immediate = true, service = DiscoveryService.class, configurationPid = "discovery.upnp")
public class UpnpDiscoveryService extends AbstractDiscoveryService
        implements RegistryListener, NetworkAddressChangeListener {

    private final Logger logger = LoggerFactory.getLogger(UpnpDiscoveryService.class);

    @Deprecated
    private final Set<org.eclipse.smarthome.config.discovery.UpnpDiscoveryParticipant> oldParticipants = new CopyOnWriteArraySet<>();

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

    @Deprecated
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addUpnpDiscoveryParticipant_old(
            org.eclipse.smarthome.config.discovery.UpnpDiscoveryParticipant participant) {
        this.oldParticipants.add(participant);

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

    @Deprecated
    protected void removeUpnpDiscoveryParticipant_old(
            org.eclipse.smarthome.config.discovery.UpnpDiscoveryParticipant participant) {
        this.oldParticipants.remove(participant);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        Set<ThingTypeUID> supportedThingTypes = new HashSet<>();
        for (UpnpDiscoveryParticipant participant : participants) {
            supportedThingTypes.addAll(participant.getSupportedThingTypeUIDs());
        }
        for (org.eclipse.smarthome.config.discovery.UpnpDiscoveryParticipant participant : oldParticipants) {
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
        for (org.eclipse.smarthome.config.discovery.UpnpDiscoveryParticipant participant : oldParticipants) {
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
        for (org.eclipse.smarthome.config.discovery.UpnpDiscoveryParticipant participant : oldParticipants) {
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
                logger.error("Could not retstart UPnP network components.", e);
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
