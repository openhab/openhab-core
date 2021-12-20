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
package org.openhab.core.config.discovery.mdns.internal;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.io.transport.mdns.MDNSClient;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
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
@NonNullByDefault
@Component(immediate = true, service = DiscoveryService.class, configurationPid = "discovery.mdns")
public class MDNSDiscoveryService extends AbstractDiscoveryService implements ServiceListener {

    private static final Duration FOREGROUND_SCAN_TIMEOUT = Duration.ofMillis(200);
    private final Logger logger = LoggerFactory.getLogger(MDNSDiscoveryService.class);

    private final Set<MDNSDiscoveryParticipant> participants = new CopyOnWriteArraySet<>();

    private final MDNSClient mdnsClient;

    /*
     * Map of scheduled tasks to remove devices from the Inbox
     */
    private Map<String, ScheduledFuture<?>> deviceRemovalTasks = new ConcurrentHashMap<>();

    @Activate
    public MDNSDiscoveryService(final @Nullable Map<String, Object> configProperties,
            final @Reference MDNSClient mdnsClient, final @Reference TranslationProvider i18nProvider,
            final @Reference LocaleProvider localeProvider) {
        super(5);

        this.mdnsClient = mdnsClient;
        this.i18nProvider = i18nProvider;
        this.localeProvider = localeProvider;

        super.activate(configProperties);

        if (isBackgroundDiscoveryEnabled()) {
            for (MDNSDiscoveryParticipant participant : participants) {
                mdnsClient.addServiceListener(participant.getServiceType(), this);
            }
        }
    }

    @Deactivate
    @Override
    protected void deactivate() {
        super.deactivate();

        for (MDNSDiscoveryParticipant participant : participants) {
            mdnsClient.removeServiceListener(participant.getServiceType(), this);
        }
    }

    @Modified
    @Override
    protected void modified(@Nullable Map<String, Object> configProperties) {
        super.modified(configProperties);
    }

    @Override
    protected void startBackgroundDiscovery() {
        for (MDNSDiscoveryParticipant participant : participants) {
            mdnsClient.addServiceListener(participant.getServiceType(), this);
        }
        startScan(true);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        for (MDNSDiscoveryParticipant participant : participants) {
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
                    final DiscoveryResult resultNew = getLocalizedDiscoveryResult(result,
                            FrameworkUtil.getBundle(participant.getClass()));
                    thingDiscovered(resultNew);
                }
            }
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addMDNSDiscoveryParticipant(MDNSDiscoveryParticipant participant) {
        this.participants.add(participant);
        if (isBackgroundDiscoveryEnabled()) {
            mdnsClient.addServiceListener(participant.getServiceType(), this);
        }
    }

    protected void removeMDNSDiscoveryParticipant(MDNSDiscoveryParticipant participant) {
        this.participants.remove(participant);
        mdnsClient.removeServiceListener(participant.getServiceType(), this);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        Set<ThingTypeUID> supportedThingTypes = new HashSet<>();
        for (MDNSDiscoveryParticipant participant : participants) {
            supportedThingTypes.addAll(participant.getSupportedThingTypeUIDs());
        }
        return supportedThingTypes;
    }

    @Override
    public void serviceAdded(@NonNullByDefault({}) ServiceEvent serviceEvent) {
        considerService(serviceEvent);
    }

    @Override
    public void serviceRemoved(@NonNullByDefault({}) ServiceEvent serviceEvent) {
        for (MDNSDiscoveryParticipant participant : participants) {
            if (participant.getServiceType().equals(serviceEvent.getType())) {
                try {
                    ThingUID thingUID = participant.getThingUID(serviceEvent.getInfo());
                    if (thingUID != null) {
                        ServiceInfo service = serviceEvent.getInfo();
                        long gracePeriod = participant.getRemovalGracePeriodSeconds(service);
                        if (gracePeriod <= 0) {
                            thingRemoved(thingUID);
                        } else {
                            cancelRemovalTask(service);
                            deviceRemovalTasks.put(service.getQualifiedName(), scheduler.schedule(() -> {
                                thingRemoved(thingUID);
                                cancelRemovalTask(service);
                            }, gracePeriod, TimeUnit.SECONDS));
                        }
                    }
                } catch (Exception e) {
                    logger.error("Participant '{}' threw an exception", participant.getClass().getName(), e);
                }
            }
        }
    }

    @Override
    public void serviceResolved(@NonNullByDefault({}) ServiceEvent serviceEvent) {
        considerService(serviceEvent);
    }

    private void considerService(ServiceEvent serviceEvent) {
        if (isBackgroundDiscoveryEnabled()) {
            for (MDNSDiscoveryParticipant participant : participants) {
                if (participant.getServiceType().equals(serviceEvent.getType())) {
                    try {
                        DiscoveryResult result = participant.createResult(serviceEvent.getInfo());
                        if (result != null) {
                            final DiscoveryResult resultNew = getLocalizedDiscoveryResult(result,
                                    FrameworkUtil.getBundle(participant.getClass()));
                            final ServiceInfo service = serviceEvent.getInfo();
                            if (participant.getRemovalGracePeriodSeconds(service) > 0) {
                                cancelRemovalTask(service);
                            }
                            thingDiscovered(resultNew);
                        }
                    } catch (Exception e) {
                        logger.error("Participant '{}' threw an exception", participant.getClass().getName(), e);
                    }
                }
            }
        }
    }

    /*
     * If the device has been scheduled to be removed, cancel its respective removal task
     */
    private void cancelRemovalTask(ServiceInfo service) {
        ScheduledFuture<?> deviceRemovalTask = deviceRemovalTasks.remove(service.getQualifiedName());
        if (deviceRemovalTask != null) {
            deviceRemovalTask.cancel(false);
        }
    }
}
