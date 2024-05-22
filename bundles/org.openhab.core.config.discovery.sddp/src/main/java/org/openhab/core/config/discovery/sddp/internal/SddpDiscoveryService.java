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
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.openhab.core.net.NetworkAddressService;
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

    private static final String IP_ADDRESS_MULTICAST = "239.255.255.250";
    private static final int PORT_MULTICAST = 1902;

    private static final int READ_BUFFER_SIZE = 1024;
    private static final Duration READ_TIMOUT = Duration.ofMillis(1000);
    private static final Duration SEARCH_LISTEN_DURATION = Duration.ofSeconds(5);
    private static final Duration CACHE_PURGE_INTERVAL = Duration.ofSeconds(60);

    private static final String SEARCH_RESPONSE_HEADER = "SDDP/1.0 200 OK";
    private static final String ALIVE_NOTIFICATION_HEADER = "NOTIFY ALIVE SDDP/1.0";

    private static final String SEARCH_REQUEST_BODY_FORMAT = """
            SEARCH * SDDP/1.0
            Host: "%s:%d"
            """;

    private final Logger logger = LoggerFactory.getLogger(SddpDiscoveryService.class);
    private final Set<SddpDevice> foundDevicesCache = ConcurrentHashMap.newKeySet();
    private final Set<SddpDiscoveryParticipant> participants = new CopyOnWriteArraySet<>();
    private final Set<SddpDeviceListener> finders = new CopyOnWriteArraySet<>();

    private final int portLocalSearch;
    private final String ipAddressLocal;
    private final String searchRequestBody;
    private final NetworkInterface networkInterface;
    private final InetSocketAddress addressLocalSearch;
    private final InetSocketAddress addressLocalMulticast;
    private final InetSocketAddress addressRemoteMulticast;

    private boolean closing = false;

    private @Nullable Future<?> listenMulticastTask = null;
    private @Nullable Future<?> listenUnicastTask = null;
    private @Nullable ScheduledFuture<?> purgeExpiredDevicesTask = null;

    @Activate
    public SddpDiscoveryService(final @Reference NetworkAddressService networkAddressService) throws IOException {
        super((int) SEARCH_LISTEN_DURATION.getSeconds());

        try (ServerSocket portFindTemporarySocket = new ServerSocket(0)) {
            portLocalSearch = portFindTemporarySocket.getLocalPort();
        }
        ipAddressLocal = Objects.requireNonNull(networkAddressService.getPrimaryIpv4HostAddress());
        searchRequestBody = String.format(SEARCH_REQUEST_BODY_FORMAT, ipAddressLocal, portLocalSearch);
        networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(ipAddressLocal));
        addressLocalSearch = new InetSocketAddress(ipAddressLocal, portLocalSearch);
        addressLocalMulticast = new InetSocketAddress(ipAddressLocal, PORT_MULTICAST);
        addressRemoteMulticast = new InetSocketAddress(IP_ADDRESS_MULTICAST, PORT_MULTICAST);

        if (isBackgroundDiscoveryEnabled()) {
            startBackgroundDiscovery();
        }

        purgeExpiredDevicesTask = scheduler.scheduleWithFixedDelay(() -> purgeExpiredDevices(),
                CACHE_PURGE_INTERVAL.getSeconds(), CACHE_PURGE_INTERVAL.getSeconds(), TimeUnit.SECONDS);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addSddpDeviceListener(SddpDeviceListener listener) {
        finders.add(listener);
        foundDevicesCache.stream().filter(d -> !d.isExpired()).forEach(d -> listener.deviceAdded(d));
        startScan();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addSddpDiscoveryParticipant(SddpDiscoveryParticipant participant) {
        participants.add(participant);
        foundDevicesCache.stream().filter(d -> !d.isExpired()).forEach(d -> {
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
                if (method.startsWith(ALIVE_NOTIFICATION_HEADER) || method.startsWith(SEARCH_RESPONSE_HEADER)) {
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
        foundDevicesCache.clear();
        participants.clear();
        finders.clear();
        cancelTask(listenUnicastTask);
        cancelTask(listenMulticastTask);
        cancelTask(purgeExpiredDevicesTask);
        listenUnicastTask = null;
        listenMulticastTask = null;
        purgeExpiredDevicesTask = null;
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        Set<ThingTypeUID> supportedThingTypes = new HashSet<>();
        participants.forEach(p -> supportedThingTypes.addAll(p.getSupportedThingTypeUIDs()));
        return supportedThingTypes;
    }

    /**
     * Continue to listen for incoming SDDP multicast messages until the thread is externally interrupted.
     */
    private void listenMulticast() {
        MulticastSocket readSocket = null;
        try {
            byte[] buffer;
            DatagramPacket packet;

            readSocket = new MulticastSocket(addressLocalMulticast);
            readSocket.joinGroup(addressRemoteMulticast, networkInterface);
            readSocket.setSoTimeout((int) READ_TIMOUT.toMillis());

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    buffer = new byte[READ_BUFFER_SIZE];
                    packet = new DatagramPacket(buffer, buffer.length);
                    readSocket.receive(packet);
                    processPacket(packet);
                } catch (SocketTimeoutException e) {
                    // receive will time out every 1 second so the thread won't block
                }
            }
        } catch (IOException e) {
            if (!closing) {
                logger.warn("listenMulticast() error {}", e.getMessage());
            }
        } finally {
            if (readSocket != null) {
                try {
                    readSocket.leaveGroup(addressRemoteMulticast, networkInterface);
                } catch (IOException e) {
                    if (!closing) {
                        logger.warn("listenMulticast() error {}", e.getMessage());
                    }
                }
                readSocket.close();
            }
        }
    }

    /**
     * Send a single outgoing SEARCH 'ping' and then continue to listen for incoming SDDP unicast responses until the
     * finish time elapses or the thread is externally interrupted.
     */
    private void listenUnicast() {
        DatagramSocket sendSocket = null;
        DatagramSocket readSocket = null;
        try {
            byte[] buffer;
            DatagramPacket packet;

            sendSocket = new DatagramSocket(addressLocalSearch);
            readSocket = new DatagramSocket(addressLocalSearch);

            sendSocket.setReuseAddress(true);
            readSocket.setReuseAddress(true);

            sendSocket.setSoTimeout((int) READ_TIMOUT.toMillis());
            readSocket.setSoTimeout((int) READ_TIMOUT.toMillis());

            buffer = searchRequestBody.getBytes(StandardCharsets.UTF_8);
            packet = new DatagramPacket(buffer, buffer.length, addressRemoteMulticast);

            sendSocket.send(packet);

            if (logger.isDebugEnabled()) {
                logger.debug("Packet sent to ipAddr:{}:{}, content:\r\n{}", packet.getAddress().getHostAddress(),
                        packet.getPort(), searchRequestBody);
            }

            final Instant finishTime = Instant.now().plus(SEARCH_LISTEN_DURATION);

            while (Instant.now().isBefore(finishTime) && !Thread.currentThread().isInterrupted()) {
                try {
                    buffer = new byte[READ_BUFFER_SIZE];
                    packet = new DatagramPacket(buffer, buffer.length);
                    readSocket.receive(packet);
                    processPacket(packet);
                } catch (SocketTimeoutException e) {
                    // receive will time out every 1 second so the thread won't block
                }
            }
        } catch (IOException e) {
            if (!closing) {
                logger.warn("listenUnicast() error {}", e.getMessage());
            }
        } finally {
            if (sendSocket != null) {
                sendSocket.close();
            }
            if (readSocket != null) {
                readSocket.close();
            }
        }
    }

    /**
     * Process the {@link DatagramPacket} content by trying to create an {@link SddpDevice} and eventually adding it to
     * the foundDevicesCache, and if so, then notifying all listeners.
     * 
     * @param packet a datagram packet that arrived over the network.
     */
    private synchronized void processPacket(DatagramPacket packet) {
        String content = new String(packet.getData(), StandardCharsets.UTF_8);
        if (logger.isDebugEnabled()) {
            logger.debug("Packet received from ipAddr:{}:{}, content:\r\n{}", packet.getAddress().getHostAddress(),
                    packet.getPort(), content);
        }
        Optional<SddpDevice> deviceOptional = createSddpDevice(content);
        if (deviceOptional.isPresent()) {
            SddpDevice device = deviceOptional.get();
            foundDevicesCache.remove(device); // force update fields that are not set-unique
            foundDevicesCache.add(device);
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
     * Purge expired devices and notify all listeners.
     */
    private synchronized void purgeExpiredDevices() {
        Set<SddpDevice> devices = new HashSet<>(foundDevicesCache);
        devices.stream().filter(d -> d.isExpired()).forEach(d -> {
            participants.forEach(p -> {
                ThingUID thingUID = p.getThingUID(d);
                if (thingUID != null) {
                    thingRemoved(thingUID);
                }
            });
            finders.forEach(f -> f.deviceRemoved(d));
        });
        foundDevicesCache.clear();
        foundDevicesCache.addAll(devices.stream().filter(d -> !d.isExpired()).collect(Collectors.toSet()));
    }

    protected void removeSddpDeviceListener(SddpDeviceListener listener) {
        finders.remove(listener);
    }

    protected void removeSddpDiscoveryParticipant(SddpDiscoveryParticipant participant) {
        participants.remove(participant);
    }

    @Override
    protected void startBackgroundDiscovery() {
        Future<?> listenMulticastTask = this.listenMulticastTask;
        if (listenMulticastTask == null || listenMulticastTask.isDone()) {
            this.listenMulticastTask = scheduler.submit(() -> listenMulticast());
        }
    }

    /**
     * Schedule to send one single SDDP SEARCH request, and listen for responses.
     */
    @Override
    protected void startScan() {
        Future<?> listenUnicastTask = this.listenUnicastTask;
        if (listenUnicastTask == null || listenUnicastTask.isDone()) {
            this.listenUnicastTask = scheduler.submit(() -> listenUnicast());
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        cancelTask(listenMulticastTask);
        listenMulticastTask = null;
    }
}
