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
package org.openhab.core.config.discovery.mdns.internal;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jmdns.JmDNS;
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
 * @author Andrew Fiddian-Green - Improved service resolution and de-bouncing of multiple events
 */
@NonNullByDefault
@Component(immediate = true, service = DiscoveryService.class, configurationPid = "discovery.mdns")
public class MDNSDiscoveryService extends AbstractDiscoveryService implements ServiceListener {

    private static final Duration FOREGROUND_SCAN_TIMEOUT = Duration.ofMillis(200);
    private static final int CONSIDER_SERVICE_WINDOW_MSEC = 200;

    private final Logger logger = LoggerFactory.getLogger(MDNSDiscoveryService.class);
    private final Set<MDNSDiscoveryParticipant> participants = new CopyOnWriteArraySet<>();

    /*
     * Map of scheduled tasks: to remove devices from the Inbox, and to de-bounce the consideration
     * of multiple service events. Note: they must be protected by synchronization on 'this'.
     */
    private final Map<String, ScheduledFuture<?>> deviceRemovalTasks = new HashMap<>();
    private final Map<String, ScheduledFuture<?>> considerServiceTasks = new HashMap<>();

    private final MDNSClient mdnsClient;

    private volatile boolean deactivating = false;

    @Activate
    public MDNSDiscoveryService(final @Nullable Map<String, Object> configProperties, //
            final @Reference MDNSClient mdnsClient, //
            final @Reference TranslationProvider i18nProvider, //
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
        deactivating = true;
        super.deactivate();
        Map<String, ScheduledFuture<?>> removalTasks, considerTasks;
        synchronized (this) {
            removalTasks = Map.copyOf(deviceRemovalTasks);
            considerTasks = Map.copyOf(considerServiceTasks);
            deviceRemovalTasks.clear();
            considerServiceTasks.clear();
        }
        removalTasks.values().forEach(task -> task.cancel(false));
        considerTasks.values().forEach(task -> task.cancel(false));
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
        scheduler.schedule(() -> {
            scan(isBackground);
        }, 0, TimeUnit.SECONDS);
    }

    /**
     * Scan has 2 different behaviors. background/ foreground. Background scans can
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
            for (ServiceInfo serviceInfo : services) {
                createDiscoveryResult(participant, serviceInfo);
            }
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addMDNSDiscoveryParticipant(MDNSDiscoveryParticipant participant) {
        participants.add(participant);
        if (isBackgroundDiscoveryEnabled()) {
            mdnsClient.addServiceListener(participant.getServiceType(), this);
        }
    }

    protected void removeMDNSDiscoveryParticipant(MDNSDiscoveryParticipant participant) {
        participants.remove(participant);
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
    public void serviceAdded(@Nullable ServiceEvent serviceEvent) {
        if (deactivating || serviceEvent == null || !isBackgroundDiscoveryEnabled()) {
            return;
        }
        /*
         * When a service is added its ServiceInfo may be either resolved or unresolved. In the resolved case
         * we may directly consider the ServiceInfo for discovery here. But we also explicitly request the service
         * to be resolved so that future serviceResolved events may be called with the then resolved ServiceInfo
         * records. The considerService method applies a short delay to de-bounce multiple such events.
         */
        considerService(serviceEvent);
        /*
         * IMPORTANT: explicitly make asynchronous request service resolution to trigger possible further
         * serviceResolved() events with additional information
         */
        JmDNS jmDNS = serviceEvent.getDNS();
        if (jmDNS != null) {
            jmDNS.requestServiceInfo(serviceEvent.getType(), serviceEvent.getName(), true);
        }
    }

    @Override
    public void serviceRemoved(@Nullable ServiceEvent serviceEvent) {
        ServiceInfo serviceInfo;
        if (deactivating || serviceEvent == null || (serviceInfo = serviceEvent.getInfo()) == null) {
            return;
        }
        String serviceType = serviceEvent.getType();
        for (MDNSDiscoveryParticipant participant : participants) {
            if (participant.getServiceType().equals(serviceType)) {
                removeDiscoveryResult(participant, serviceInfo);
            }
        }
    }

    @Override
    public void serviceResolved(@Nullable ServiceEvent serviceEvent) {
        if (deactivating || serviceEvent == null || !isBackgroundDiscoveryEnabled()) {
            return;
        }
        /*
         * This method may be called several times as additional information such as the IP v4 and v6 addresses
         * and TXT attribute records are added. The considerService method applies a short delay to de-bounce
         * multiple such events.
         */
        considerService(serviceEvent);
    }

    /**
     * Schedules a task to consider the given ServiceEvent for creating a DiscoveryResult after a short delay. This is
     * needed to avoid creating multiple DiscoveryResults for the same service in case that multiple serviceAdded and
     * serviceResolved events are fired in quick succession during the resolution process of a new service.
     *
     * @param serviceEvent the ServiceEvent to consider.
     */
    private void considerService(ServiceEvent serviceEvent) {
        ServiceInfo serviceInfo;
        if ((serviceInfo = serviceEvent.getInfo()) == null) {
            return;
        }
        String lookupKey = serviceInfo.getKey();
        ScheduledFuture<?> oldTask;
        synchronized (this) {
            oldTask = considerServiceTasks.put(lookupKey,
                    scheduler.schedule(() -> considerServiceTask(serviceInfo, serviceEvent.getType(), lookupKey),
                            CONSIDER_SERVICE_WINDOW_MSEC, TimeUnit.MILLISECONDS));
        }
        if (oldTask != null) {
            oldTask.cancel(false);
        }
    }

    /**
     * Considers the given ServiceEvent for creating a DiscoveryResult. This method is called by the scheduled task
     * created in considerService() after the short delay has expired.
     * 
     * @param serviceInfo the mDNS ServiceInfo describing the device on the network.
     * @param serviceType the mDNS service type.
     * @param lookupKey the hash map lookup key.
     */
    private void considerServiceTask(ServiceInfo serviceInfo, String serviceType, String lookupKey) {
        if (deactivating) {
            return;
        }
        Future<?> task;
        synchronized (this) {
            task = considerServiceTasks.remove(lookupKey);
        }
        if (task != null) {
            task.cancel(false);
        }
        for (MDNSDiscoveryParticipant participant : participants) {
            if (participant.getServiceType().equals(serviceType)) {
                createDiscoveryResult(participant, serviceInfo);
            }
        }
    }

    private void createDiscoveryResult(MDNSDiscoveryParticipant participant, ServiceInfo serviceInfo) {
        try {
            DiscoveryResult result = participant.createResult(serviceInfo);
            if (result != null) {
                cancelRemovalTask(serviceInfo);
                thingDiscovered(result, FrameworkUtil.getBundle(participant.getClass()));
            }
        } catch (Exception e) {
            logger.error("Participant '{}' threw an exception", participant.getClass().getName(), e);
        }
    }

    private void removeDiscoveryResult(MDNSDiscoveryParticipant participant, ServiceInfo serviceInfo) {
        try {
            ThingUID thingUID = participant.getThingUID(serviceInfo);
            if (thingUID != null) {
                long gracePeriod = participant.getRemovalGracePeriodSeconds(serviceInfo);
                if (gracePeriod <= 0) {
                    thingRemoved(thingUID);
                } else {
                    cancelRemovalTask(serviceInfo);
                    scheduleRemovalTask(thingUID, serviceInfo, gracePeriod);
                }
            }
        } catch (Exception e) {
            logger.error("Participant '{}' threw an exception", participant.getClass().getName(), e);
        }
    }

    /**
     * If the device has been scheduled to be removed, cancel its respective removal task.
     *
     * @param serviceInfo the mDNS ServiceInfo describing the device on the network.
     */
    private void cancelRemovalTask(ServiceInfo serviceInfo) {
        ScheduledFuture<?> deviceRemovalTask;
        synchronized (this) {
            deviceRemovalTask = deviceRemovalTasks.remove(serviceInfo.getKey());
        }
        if (deviceRemovalTask != null) {
            deviceRemovalTask.cancel(false);
        }
    }

    /**
     * Schedule a task that will remove the device from the Inbox after the given grace period has expired.
     *
     * @param thingUID the UID of the Thing to be removed.
     * @param serviceInfo the mDNS ServiceInfo describing the device on the network.
     * @param gracePeriod the scheduled delay in seconds.
     */
    private void scheduleRemovalTask(ThingUID thingUID, ServiceInfo serviceInfo, long gracePeriod) {
        if (!deactivating) {
            synchronized (this) {
                deviceRemovalTasks.put(serviceInfo.getKey(), scheduler.schedule(() -> {
                    thingRemoved(thingUID);
                    cancelRemovalTask(serviceInfo);
                }, gracePeriod, TimeUnit.SECONDS));
            }
        }
    }
}
