/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.config.discovery.sddp.internal;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.config.discovery.sddp.SddpDevice;
import org.openhab.core.config.discovery.sddp.SddpDeviceListener;
import org.openhab.core.config.discovery.sddp.SddpDiscoveryParticipant;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link DiscoveryService} implementation, which can find SDDP devices in the network.
 * Support for bindings can be achieved by implementing and registering a {@link SddpDiscoveryParticipant}.
 * Support for finders can be achieved by implementing and registering a {@link SddpDeviceListener}.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = DiscoveryService.class, configurationPid = "discovery.sddp")
public class SddpDiscoveryService extends AbstractDiscoveryService implements AutoCloseable {

    private static final String SEARCH_REQUEST = "SEARCH * SDDP/1.0\r\nHost: \"%s:%d\"\r\n";
    private static final String SEARCH_RESPONSE = "SDDP/1.0 200 OK";
    private static final String ALIVE_NOTIFICATION = "NOTIFY ALIVE SDDP/1.0";

    private static final String ADDRESS_MULTICAST = "239.255.255.250";
    private static final int PORT_MULTICAST = 1902;
    private static final int PORT_UNICAST = 24378;

    private static final Duration READ_TIMOUT = Duration.ofMillis(1000);
    private static final Duration LISTEN_DURATION = Duration.ofSeconds(5);
    private static final Duration CACHE_CLEAN_INTERVAL = Duration.ofSeconds(60);

    private final InetAddress addressMulticast;
    private final InetAddress addressUnicast;

    private final Logger logger = LoggerFactory.getLogger(SddpDiscoveryService.class);
    private final Set<SddpDevice> foundDevices = ConcurrentHashMap.newKeySet();
    private final Set<SddpDiscoveryParticipant> participants = new CopyOnWriteArraySet<>();
    private final Set<SddpDeviceListener> finders = new CopyOnWriteArraySet<>();

    private boolean listenMulticast = false;
    private boolean listenUnicast = false;

    private boolean closing = false;

    private @Nullable Future<?> listenMulticastTask = null;
    private @Nullable Future<?> listenUnicastTask = null;
    private @Nullable ScheduledFuture<?> cancelListenUnicastTask = null;
    private @Nullable ScheduledFuture<?> removeExpiredDevicesTask = null;

    @Activate
    public SddpDiscoveryService() throws SocketException, UnknownHostException {
        super((int) LISTEN_DURATION.getSeconds());
        addressMulticast = InetAddress.getByName(ADDRESS_MULTICAST);
        addressUnicast = InetAddress.getLocalHost();
        if (isBackgroundDiscoveryEnabled()) {
            startBackgroundDiscovery();
        }
        removeExpiredDevicesTask = scheduler.scheduleWithFixedDelay(() -> removeExpiredDevices(),
                CACHE_CLEAN_INTERVAL.getSeconds(), CACHE_CLEAN_INTERVAL.getSeconds(), TimeUnit.SECONDS);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addSddpDeviceListener(SddpDeviceListener listener) {
        finders.add(listener);
        foundDevices.stream().filter(d -> !d.isExpired()).forEach(d -> listener.deviceAdded(d));
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addSddpDiscoveryParticipant(SddpDiscoveryParticipant participant) {
        participants.add(participant);
        foundDevices.stream().filter(d -> !d.isExpired()).forEach(d -> {
            DiscoveryResult result = participant.createResult(d);
            if (result != null) {
                DiscoveryResult localizedResult = getLocalizedDiscoveryResult(result,
                        FrameworkUtil.getBundle(participant.getClass()));
                thingDiscovered(localizedResult);
            }
        });
    }

    /**
     * Cancel the given task.
     */
    private void cancelTask(@Nullable Future<?> task) {
        if (task != null) {
            task.cancel(true);
        }
    }

    @Override
    public void close() {
        deactivate();
    }

    /**
     * Create an {@link SddpDevice) object from UDP packet data.
     */
    public Optional<SddpDevice> createSddpDevice(String data) {
        if (!data.isBlank()) {
            List<String> headers = data.lines().toList();
            if (headers.size() > 1) {
                String method = headers.get(0).strip();
                if (method.startsWith(ALIVE_NOTIFICATION) || method.startsWith(SEARCH_RESPONSE)) {
                    Map<String, String> headerMap = new HashMap<>();
                    for (int i = 1; i < headers.size(); i++) {
                        String[] header = headers.get(i).split(":", 2);
                        if (header.length > 1) {
                            headerMap.put(header[0].strip(), header[1].strip());
                        }
                    }
                    return Optional.of(new SddpDevice(headerMap));
                }
            }
        }
        return Optional.empty();
    }

    @Deactivate
    @Override
    protected void deactivate() {
        closing = true;
        listenMulticast = false;
        listenUnicast = false;
        foundDevices.clear();
        participants.clear();
        finders.clear();
        cancelTask(listenMulticastTask);
        cancelTask(listenUnicastTask);
        cancelTask(cancelListenUnicastTask);
        cancelTask(removeExpiredDevicesTask);
        listenMulticastTask = null;
        listenUnicastTask = null;
        cancelListenUnicastTask = null;
        removeExpiredDevicesTask = null;
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        Set<ThingTypeUID> supportedThingTypes = new HashSet<>();
        participants.forEach(p -> supportedThingTypes.addAll(p.getSupportedThingTypeUIDs()));
        return supportedThingTypes;
    }

    /**
     * Listen for incoming SDDP multicast messages.
     */
    private void listenMulticast() {
        try (MulticastSocket socket = new MulticastSocket(PORT_MULTICAST)) {
            socket.joinGroup(new InetSocketAddress(addressMulticast, PORT_MULTICAST), NetworkInterface.getByIndex(0));
            socket.setSoTimeout((int) READ_TIMOUT.toMillis());
            while (listenMulticast) {
                byte[] buf = new byte[2048];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packet);
                    processDatagramPacket(packet);
                } catch (SocketTimeoutException e) {
                    // simply avoid blocking the thread
                }
            }
        } catch (IOException e) {
            if (!closing) {
                logger.warn("SDDP multicast listen error {}", e.getMessage());
            }
        }
    }

    /**
     * Listen for incoming SDDP unicast messages.
     */
    private void listenUnicast() {
        try (DatagramSocket socket = new DatagramSocket(PORT_UNICAST)) {
            socket.setSoTimeout((int) READ_TIMOUT.toMillis());
            while (listenUnicast) {
                byte[] buf = new byte[2048];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packet);
                    processDatagramPacket(packet);
                } catch (SocketTimeoutException e) {
                    // simply avoid blocking the thread
                }
            }
        } catch (IOException e) {
            if (!closing) {
                logger.warn("SDDP unicast listen error {}", e.getMessage());
            }
        }
    }

    /**
     * Process the {@link DatagramPacket} by trying to create an {@link SddpDevice} and eventually adding it to the
     * foundDevices map, and notifying all listeners.
     */
    private synchronized void processDatagramPacket(DatagramPacket packet) {
        Optional<SddpDevice> deviceOptional = createSddpDevice(
                new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8));
        if (deviceOptional.isPresent()) {
            SddpDevice device = deviceOptional.get();
            foundDevices.remove(device); // force update fields that are not set-unique
            foundDevices.add(device);
            participants.forEach(p -> {
                DiscoveryResult result = p.createResult(device);
                if (result != null) {
                    DiscoveryResult localizedResult = getLocalizedDiscoveryResult(result,
                            FrameworkUtil.getBundle(p.getClass()));
                    thingDiscovered(localizedResult);
                }
            });
            finders.forEach(f -> f.deviceAdded(device));
        }
    }

    /**
     * Remove expired devices and notify all listeners.
     */
    private void removeExpiredDevices() {
        Set<SddpDevice> devices = new HashSet<>(foundDevices);
        devices.stream().filter(d -> d.isExpired()).forEach(d -> {
            participants.forEach(p -> {
                ThingUID thingUID = p.getThingUID(d);
                if (thingUID != null) {
                    thingRemoved(thingUID);
                }
            });
            finders.forEach(f -> f.deviceRemoved(d));
        });
        foundDevices.clear();
        foundDevices.addAll(devices.stream().filter(d -> !d.isExpired()).collect(Collectors.toSet()));
    }

    protected void removeSddpDeviceListener(SddpDeviceListener listener) {
        finders.remove(listener);
    }

    protected void removeSddpDiscoveryParticipant(SddpDiscoveryParticipant participant) {
        participants.remove(participant);
    }

    @Override
    protected void startBackgroundDiscovery() {
        if (!listenMulticast) {
            listenMulticast = true;
            listenMulticastTask = scheduler.submit(() -> listenMulticast());
        }
    }

    /**
     * Broadcast one single SDDP SEARCH request.
     */
    @Override
    protected void startScan() {
        listenUnicast = true;
        listenUnicastTask = scheduler.submit(() -> listenUnicast());
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            byte[] buf = String.format(SEARCH_REQUEST, addressUnicast.getHostAddress(), PORT_UNICAST)
                    .getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, addressMulticast, PORT_MULTICAST);
            socket.send(packet);
            cancelListenUnicastTask = scheduler.schedule(() -> listenUnicast = false, LISTEN_DURATION.getSeconds(),
                    TimeUnit.SECONDS);
        } catch (IOException e) {
            if (!closing) {
                logger.warn("SDDP client error {}", e.getMessage());
            }
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        listenMulticast = false;
        cancelTask(listenMulticastTask);
        listenMulticastTask = null;
    }
}
