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
package org.eclipse.smarthome.config.discovery.mdns.internal;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.io.transport.mdns.MDNSClient;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link DiscoveryService} implementation, which can find mDNS services in the network. Support for further
 * devices can be added by implementing and registering a {@link MDNSDiscoveryParticipant}.
 *
 * @author Tobias Bräutigam - Initial contribution
 * @author Kai Kreuzer - Improved startup behavior and background discovery
 * @author Andre Fuechsel - make {@link #startScan()} asynchronous
 */
@Component(immediate = true, service = DiscoveryService.class, configurationPid = "discovery.mdns")
public class MDNSDiscoveryService extends AbstractDiscoveryService implements ServiceListener {

    private static final Duration FOREGROUND_SCAN_TIMEOUT = Duration.ofMillis(200);
    private final Logger logger = LoggerFactory.getLogger(MDNSDiscoveryService.class);

    @Deprecated
    private final Set<org.eclipse.smarthome.io.transport.mdns.discovery.MDNSDiscoveryParticipant> oldParticipants = new CopyOnWriteArraySet<>();

    private final Set<MDNSDiscoveryParticipant> participants = new CopyOnWriteArraySet<>();

    private MDNSClient mdnsClient;

    public MDNSDiscoveryService() {
        super(5);
    }

    @Reference
    public void setMDNSClient(MDNSClient mdnsClient) {
        this.mdnsClient = mdnsClient;
        if (isBackgroundDiscoveryEnabled()) {
            for (MDNSDiscoveryParticipant participant : participants) {
                mdnsClient.addServiceListener(participant.getServiceType(), this);
            }
            for (org.eclipse.smarthome.io.transport.mdns.discovery.MDNSDiscoveryParticipant participant : oldParticipants) {
                mdnsClient.addServiceListener(participant.getServiceType(), this);
            }
        }
    }

    public void unsetMDNSClient(MDNSClient mdnsClient) {
        for (MDNSDiscoveryParticipant participant : participants) {
            mdnsClient.removeServiceListener(participant.getServiceType(), this);
        }
        for (org.eclipse.smarthome.io.transport.mdns.discovery.MDNSDiscoveryParticipant participant : oldParticipants) {
            mdnsClient.removeServiceListener(participant.getServiceType(), this);
        }
        this.mdnsClient = null;
    }

    @Modified
    @Override
    protected void modified(@Nullable Map<@NonNull String, @Nullable Object> configProperties) {
        super.modified(configProperties);
    }

    @Override
    protected void startBackgroundDiscovery() {
        for (MDNSDiscoveryParticipant participant : participants) {
            mdnsClient.addServiceListener(participant.getServiceType(), this);
        }
        for (org.eclipse.smarthome.io.transport.mdns.discovery.MDNSDiscoveryParticipant participant : oldParticipants) {
            mdnsClient.addServiceListener(participant.getServiceType(), this);
        }
        startScan(true);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        for (MDNSDiscoveryParticipant participant : participants) {
            mdnsClient.removeServiceListener(participant.getServiceType(), this);
        }
        for (org.eclipse.smarthome.io.transport.mdns.discovery.MDNSDiscoveryParticipant participant : oldParticipants) {
            mdnsClient.removeServiceListener(participant.getServiceType(), this);
        }
    }

    @Override
    protected void startScan() {
        startScan(false);
    }

    @Override
    protected synchronized void stopScan() {
        removeOlderResults(getTimestampOfLastScan());
        super.stopScan();
    }

    private void startScan(boolean isBackground) {
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                scan(isBackground);
            }
        }, 0, TimeUnit.SECONDS);
    }

    /**
     * Scan has 2 different behaviours. background/ foreground. Background scans can
     * have much higher timeout. Foreground scans have only a short timeout as human
     * users may become impatient. The underlying reason is that the jmDNS
     * implementation {@code MDNSClient#list(String)} has a default timeout of 6
     * seconds when no ServiceInfo is found. When there are many participants,
     * waiting 6 seconds for each non-existent type is too long.
     *
     * @param isBackground true if it is background scan, false otherwise.
     */
    private void scan(boolean isBackground) {
        for (MDNSDiscoveryParticipant participant : participants) {
            long start = System.currentTimeMillis();
            ServiceInfo[] services;
            if (isBackground) {
                services = mdnsClient.list(participant.getServiceType());
            } else {
                services = mdnsClient.list(participant.getServiceType(), FOREGROUND_SCAN_TIMEOUT);
            }
            logger.debug("{} services found for {}; duration: {}ms", services.length, participant.getServiceType(),
                    System.currentTimeMillis() - start);
            for (ServiceInfo service : services) {
                DiscoveryResult result = participant.createResult(service);
                if (result != null) {
                    thingDiscovered(result);
                }
            }
        }
        for (org.eclipse.smarthome.io.transport.mdns.discovery.MDNSDiscoveryParticipant participant : oldParticipants) {
            long start = System.currentTimeMillis();
            ServiceInfo[] services;
            if (isBackground) {
                services = mdnsClient.list(participant.getServiceType());
            } else {
                services = mdnsClient.list(participant.getServiceType(), FOREGROUND_SCAN_TIMEOUT);
            }
            logger.debug("{} services found for {}; duration: {}ms", services.length, participant.getServiceType(),
                    System.currentTimeMillis() - start);
            for (ServiceInfo service : services) {
                DiscoveryResult result = participant.createResult(service);
                if (result != null) {
                    thingDiscovered(result);
                }
            }
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addMDNSDiscoveryParticipant(MDNSDiscoveryParticipant participant) {
        this.participants.add(participant);
        if (mdnsClient != null && isBackgroundDiscoveryEnabled()) {
            mdnsClient.addServiceListener(participant.getServiceType(), this);
        }
    }

    protected void removeMDNSDiscoveryParticipant(MDNSDiscoveryParticipant participant) {
        this.participants.remove(participant);
        if (mdnsClient != null) {
            mdnsClient.removeServiceListener(participant.getServiceType(), this);
        }
    }

    @Deprecated
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addMDNSDiscoveryParticipant_old(
            org.eclipse.smarthome.io.transport.mdns.discovery.MDNSDiscoveryParticipant participant) {
        this.oldParticipants.add(participant);
        if (mdnsClient != null && isBackgroundDiscoveryEnabled()) {
            mdnsClient.addServiceListener(participant.getServiceType(), this);
        }
    }

    @Deprecated
    protected void removeMDNSDiscoveryParticipant_old(
            org.eclipse.smarthome.io.transport.mdns.discovery.MDNSDiscoveryParticipant participant) {
        this.oldParticipants.remove(participant);
        if (mdnsClient != null) {
            mdnsClient.removeServiceListener(participant.getServiceType(), this);
        }
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        Set<ThingTypeUID> supportedThingTypes = new HashSet<>();
        for (MDNSDiscoveryParticipant participant : participants) {
            supportedThingTypes.addAll(participant.getSupportedThingTypeUIDs());
        }
        for (org.eclipse.smarthome.io.transport.mdns.discovery.MDNSDiscoveryParticipant participant : oldParticipants) {
            supportedThingTypes.addAll(participant.getSupportedThingTypeUIDs());
        }
        return supportedThingTypes;
    }

    @Override
    public void serviceAdded(ServiceEvent serviceEvent) {
        considerService(serviceEvent);
    }

    @Override
    public void serviceRemoved(ServiceEvent serviceEvent) {
        for (MDNSDiscoveryParticipant participant : participants) {
            if (participant.getServiceType().equals(serviceEvent.getType())) {
                try {
                    ThingUID thingUID = participant.getThingUID(serviceEvent.getInfo());
                    if (thingUID != null) {
                        thingRemoved(thingUID);
                    }
                } catch (Exception e) {
                    logger.error("Participant '{}' threw an exception", participant.getClass().getName(), e);
                }
            }
        }
        for (org.eclipse.smarthome.io.transport.mdns.discovery.MDNSDiscoveryParticipant participant : oldParticipants) {
            if (participant.getServiceType().equals(serviceEvent.getType())) {
                try {
                    ThingUID thingUID = participant.getThingUID(serviceEvent.getInfo());
                    if (thingUID != null) {
                        thingRemoved(thingUID);
                    }
                } catch (Exception e) {
                    logger.error("Participant '{}' threw an exception", participant.getClass().getName(), e);
                }
            }
        }
    }

    @Override
    public void serviceResolved(ServiceEvent serviceEvent) {
        considerService(serviceEvent);
    }

    private void considerService(ServiceEvent serviceEvent) {
        if (isBackgroundDiscoveryEnabled()) {
            for (MDNSDiscoveryParticipant participant : participants) {
                if (participant.getServiceType().equals(serviceEvent.getType())) {
                    try {
                        DiscoveryResult result = participant.createResult(serviceEvent.getInfo());
                        if (result != null) {
                            thingDiscovered(result);
                        }
                    } catch (Exception e) {
                        logger.error("Participant '{}' threw an exception", participant.getClass().getName(), e);
                    }
                }
            }
            for (org.eclipse.smarthome.io.transport.mdns.discovery.MDNSDiscoveryParticipant participant : oldParticipants) {
                if (participant.getServiceType().equals(serviceEvent.getType())) {
                    try {
                        DiscoveryResult result = participant.createResult(serviceEvent.getInfo());
                        if (result != null) {
                            thingDiscovered(result);
                        }
                    } catch (Exception e) {
                        logger.error("Participant '{}' threw an exception", participant.getClass().getName(), e);
                    }
                }
            }
        }
    }
}
