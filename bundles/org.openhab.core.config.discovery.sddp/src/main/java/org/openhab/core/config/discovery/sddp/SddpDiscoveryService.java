/*
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
package org.openhab.core.config.discovery.sddp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.net.CidrAddress;
import org.openhab.core.net.NetworkAddressChangeListener;
import org.openhab.core.net.NetworkAddressService;
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
 * This is a {@link DiscoveryService} implementation, which can find SDDP devices in the network.
 * <p>
 * Simple Device Discovery Protocol (SDDP) is a simple multicast discovery protocol implemented
 * by many "smart home" devices to allow a controlling agent to easily discover and connect to
 * devices on a local subnet.
 * <p>
 * SDDP was created by Control4, and is quite similar to UPnP's standard Simple Service Discovery
 * Protocol (SSDP), and it serves a virtually identical purpose. SDDP is not a standard protocol
 * and it is not publicly documented.
 * <p>
 * Support for bindings can be achieved by implementing and registering a {@link SddpDiscoveryParticipant}.
 * Support for finders can be achieved by implementing and registering a {@link SddpDeviceParticipant}.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = DiscoveryService.class, property = "protocol=sddp", configurationPid = "discovery.sddp")
public class SddpDiscoveryService extends AbstractDiscoveryService
        implements AutoCloseable, NetworkAddressChangeListener {

    private static final int SDDP_PORT = 1902;
    private static final String SDDP_IP_ADDRESS = "239.255.255.250";
    private static final InetSocketAddress SDDP_GROUP = new InetSocketAddress(SDDP_IP_ADDRESS, SDDP_PORT);

    private static final int READ_BUFFER_SIZE = 1024;
    private static final Duration SOCKET_TIMOUT = Duration.ofMillis(1000);
    private static final Duration SEARCH_LISTEN_DURATION = Duration.ofSeconds(5);
    private static final Duration CACHE_PURGE_INTERVAL = Duration.ofSeconds(300);

    private static final String SEARCH_REQUEST_BODY_FORMAT = "SEARCH * SDDP/1.0\r\nHost: \"%s:%d\"\r\n";
    private static final String SEARCH_RESPONSE_HEADER = "SDDP/1.0 200 OK";

    private static final String NOTIFY_ALIVE_HEADER = "NOTIFY ALIVE SDDP/1.0";
    private static final String NOTIFY_IDENTIFY_HEADER = "NOTIFY IDENTIFY SDDP/1.0";
    private static final String NOTIFY_OFFLINE_HEADER = "NOTIFY OFFLINE SDDP/1.0";

    private final Logger logger = LoggerFactory.getLogger(SddpDiscoveryService.class);
    private final Set<SddpDevice> foundDevicesCache = ConcurrentHashMap.newKeySet();
    private final Set<SddpDiscoveryParticipant> discoveryParticipants = ConcurrentHashMap.newKeySet();
    private final Set<SddpDeviceParticipant> deviceParticipants = ConcurrentHashMap.newKeySet();

    private final NetworkAddressService networkAddressService;

    private boolean closing = false;

    private @Nullable Future<?> listenBackgroundMulticastTask = null;
    private @Nullable Future<?> listenActiveScanUnicastTask = null;
    private @Nullable ScheduledFuture<?> purgeExpiredDevicesTask = null;

    @Activate
    public SddpDiscoveryService(final @Nullable Map<String, Object> configProperties, //
            final @Reference NetworkAddressService networkAddressService, //
            final @Reference TranslationProvider i18nProvider, //
            final @Reference LocaleProvider localeProvider) {
        super((int) SEARCH_LISTEN_DURATION.getSeconds());

        this.networkAddressService = networkAddressService;
        this.i18nProvider = i18nProvider;
        this.localeProvider = localeProvider;

        super.activate(configProperties); // note: this starts listenBackgroundMulticastTask

        purgeExpiredDevicesTask = scheduler.scheduleWithFixedDelay(() -> purgeExpiredDevices(),
                CACHE_PURGE_INTERVAL.getSeconds(), CACHE_PURGE_INTERVAL.getSeconds(), TimeUnit.SECONDS);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addSddpDeviceParticipant(SddpDeviceParticipant participant) {
        deviceParticipants.add(participant);
        foundDevicesCache.stream().filter(d -> !d.isExpired()).forEach(d -> participant.deviceAdded(d));
        startScan();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addSddpDiscoveryParticipant(SddpDiscoveryParticipant participant) {
        discoveryParticipants.add(participant);
        foundDevicesCache.stream().filter(d -> !d.isExpired()).forEach(d -> {
            DiscoveryResult result = participant.createResult(d);
            if (result != null) {
                thingDiscovered(result, FrameworkUtil.getBundle(participant.getClass()));
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
     * Optionally create an {@link SddpDevice} object from UDP packet data if the data is good.
     */
    public Optional<SddpDevice> createSddpDevice(String data) {
        if (!data.isBlank()) {
            List<String> lines = data.lines().toList();
            if (lines.size() > 1) {
                String statement = lines.getFirst().strip();
                boolean offline = statement.startsWith(NOTIFY_OFFLINE_HEADER);
                if (offline || statement.startsWith(NOTIFY_ALIVE_HEADER) || statement.startsWith(NOTIFY_IDENTIFY_HEADER)
                        || statement.startsWith(SEARCH_RESPONSE_HEADER)) {
                    Map<String, String> headers = new HashMap<>();
                    for (int i = 1; i < lines.size(); i++) {
                        String[] header = lines.get(i).split(":", 2);
                        if (header.length > 1) {
                            headers.put(header[0].strip(), header[1].strip());
                        }
                    }
                    return Optional.of(new SddpDevice(headers, offline));
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
        discoveryParticipants.clear();
        deviceParticipants.clear();

        super.deactivate(); // note: this cancels and nulls listenBackgroundMulticastTask

        cancelTask(listenActiveScanUnicastTask);
        listenActiveScanUnicastTask = null;

        cancelTask(purgeExpiredDevicesTask);
        purgeExpiredDevicesTask = null;
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        Set<ThingTypeUID> supportedThingTypes = new HashSet<>();
        discoveryParticipants.forEach(p -> supportedThingTypes.addAll(p.getSupportedThingTypeUIDs()));
        return supportedThingTypes;
    }

    /**
     * Continue to listen for incoming SDDP multicast messages until the thread is externally interrupted.
     */
    private void listenBackGroundMulticast() {
        MulticastSocket socket = null;
        NetworkInterface networkInterface = null;

        try {
            networkInterface = NetworkInterface
                    .getByInetAddress(InetAddress.getByName(networkAddressService.getPrimaryIpv4HostAddress()));

            if (logger.isDebugEnabled()) {
                logger.debug("listenBackGroundMulticast() starting on interface '{}'",
                        networkInterface.getDisplayName());
            }

            socket = new MulticastSocket(SDDP_PORT);
            socket.joinGroup(SDDP_GROUP, networkInterface);
            socket.setSoTimeout((int) SOCKET_TIMOUT.toMillis());

            DatagramPacket packet = null;
            byte[] buffer = new byte[READ_BUFFER_SIZE];

            // loop listen for responses
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (packet == null) {
                        packet = new DatagramPacket(buffer, buffer.length);
                    }
                    socket.receive(packet);
                    processPacket(packet);
                    packet = null;
                } catch (SocketTimeoutException e) {
                    // socket.receive() will time out every 1 second so the thread won't block
                }
            }
        } catch (IOException e) {
            if (!closing) {
                logger.warn("listenBackGroundMulticast error '{}'", e.getMessage());
            }
        } finally {
            if (socket != null && networkInterface != null) {
                try {
                    socket.leaveGroup(SDDP_GROUP, networkInterface);
                } catch (IOException e) {
                    if (!closing) {
                        logger.warn("listenBackGroundMulticast() error '{}'", e.getMessage());
                    }
                }
                socket.close();
            }
        }
    }

    /**
     * Send a single outgoing SEARCH 'ping' and then continue to listen for incoming SDDP unicast responses until the
     * loop time elapses or the thread is externally interrupted.
     */
    private void listenActiveScanUnicast() {
        // get a free port number
        int port;
        try (ServerSocket portFinder = new ServerSocket(0)) {
            port = portFinder.getLocalPort();
        } catch (IOException e) {
            logger.warn("listenActiveScanUnicast() port finder error '{}'", e.getMessage());
            return;
        }

        try (DatagramSocket socket = new DatagramSocket(port)) {
            String ipAddress = networkAddressService.getPrimaryIpv4HostAddress();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(ipAddress));

            if (logger.isDebugEnabled()) {
                logger.debug("listenActiveScanUnicast() starting on '{}:{}' on interface '{}'", ipAddress, port,
                        networkInterface.getDisplayName());
            }

            socket.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
            socket.setSoTimeout((int) SOCKET_TIMOUT.toMillis());

            DatagramPacket packet;
            byte[] buffer;

            // send search request
            String search = String.format(SEARCH_REQUEST_BODY_FORMAT, ipAddress, port);
            buffer = search.getBytes(StandardCharsets.UTF_8);
            packet = new DatagramPacket(buffer, buffer.length, new InetSocketAddress(SDDP_IP_ADDRESS, SDDP_PORT));
            socket.send(packet);
            logger.trace("Packet sent to '{}:{}' content:\r\n{}", SDDP_IP_ADDRESS, SDDP_PORT, search);

            final Instant listenDoneTime = Instant.now().plus(SEARCH_LISTEN_DURATION);
            buffer = new byte[READ_BUFFER_SIZE];
            packet = null;

            // loop listen for responses
            while (Instant.now().isBefore(listenDoneTime) && !Thread.currentThread().isInterrupted()) {
                try {
                    if (packet == null) {
                        packet = new DatagramPacket(buffer, buffer.length);
                    }
                    socket.receive(packet);
                    processPacket(packet);
                    packet = null;
                } catch (SocketTimeoutException e) {
                    // receive will time out every 1 second so the thread won't block
                }
            }
        } catch (IOException e) {
            if (!closing) {
                logger.warn("listenActiveScanUnicast() error '{}'", e.getMessage());
            }
        }
    }

    @Modified
    @Override
    protected void modified(@Nullable Map<String, Object> configProperties) {
        super.modified(configProperties);
    }

    /**
     * If the network interfaces change then cancel and recreate all pending tasks.
     */
    @Override
    public synchronized void onChanged(List<CidrAddress> added, List<CidrAddress> removed) {
        Future<?> multicastTask = listenBackgroundMulticastTask;
        if (multicastTask != null && !multicastTask.isDone()) {
            multicastTask.cancel(true);
            listenBackgroundMulticastTask = scheduler.submit(() -> listenBackGroundMulticast());
        }
        Future<?> unicastTask = listenActiveScanUnicastTask;
        if (unicastTask != null && !unicastTask.isDone()) {
            unicastTask.cancel(true);
            listenActiveScanUnicastTask = scheduler.submit(() -> listenActiveScanUnicast());
        }
    }

    /**
     * Process the {@link DatagramPacket} content by trying to create an {@link SddpDevice} and eventually adding it to
     * the foundDevicesCache, and if so, then notifying all listeners.
     *
     * @param packet a datagram packet that arrived over the network.
     */
    private synchronized void processPacket(DatagramPacket packet) {
        String content = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        if (logger.isTraceEnabled()) {
            logger.trace("Packet received from '{}:{}' content:\r\n{}", packet.getAddress().getHostAddress(),
                    packet.getPort(), content);
        }
        Optional<SddpDevice> deviceOptional = createSddpDevice(content);
        if (deviceOptional.isPresent()) {
            SddpDevice device = deviceOptional.get();
            foundDevicesCache.remove(device);

            if (device.isExpired()) {
                // device created from a NOTIFY OFFLINE announcement
                discoveryParticipants.forEach(p -> {
                    DiscoveryResult discoveryResult = p.createResult(device);
                    if (discoveryResult != null) {
                        thingRemoved(discoveryResult.getThingUID());
                    }
                });
                deviceParticipants.forEach(f -> f.deviceRemoved(device));
            } else {
                // device created from a NOTIFY ALIVE announcement or SEARCH response
                foundDevicesCache.add(device);
                discoveryParticipants.forEach(p -> {
                    DiscoveryResult discoveryResult = p.createResult(device);
                    if (discoveryResult != null) {
                        thingDiscovered(discoveryResult, FrameworkUtil.getBundle(p.getClass()));
                    }
                });
                deviceParticipants.forEach(f -> f.deviceAdded(device));
            }

            if (logger.isDebugEnabled()) {
                logger.debug("processPacket() foundDevices={}, deviceParticipants={}, discoveryParticipants={}",
                        foundDevicesCache.size(), deviceParticipants.size(), discoveryParticipants.size());
            }
        }
    }

    /**
     * Purge expired devices and notify all listeners.
     */
    private synchronized void purgeExpiredDevices() {
        Set<SddpDevice> devices = new HashSet<>(foundDevicesCache);
        devices.stream().filter(d -> d.isExpired()).forEach(d -> {
            discoveryParticipants.forEach(p -> {
                ThingUID thingUID = p.getThingUID(d);
                if (thingUID != null) {
                    thingRemoved(thingUID);
                }
            });
            deviceParticipants.forEach(f -> f.deviceRemoved(d));
        });
        foundDevicesCache.clear();
        foundDevicesCache.addAll(devices.stream().filter(d -> !d.isExpired()).collect(Collectors.toSet()));
    }

    public void removeSddpDeviceParticipant(SddpDeviceParticipant participant) {
        deviceParticipants.remove(participant);
    }

    public void removeSddpDiscoveryParticipant(SddpDiscoveryParticipant participant) {
        discoveryParticipants.remove(participant);
    }

    @Override
    protected void startBackgroundDiscovery() {
        Future<?> task = listenBackgroundMulticastTask;
        if (task == null || task.isDone()) {
            listenBackgroundMulticastTask = scheduler.submit(() -> listenBackGroundMulticast());
        }
    }

    /**
     * Schedule to send one single SDDP SEARCH request, and listen for responses.
     */
    @Override
    protected void startScan() {
        Future<?> task = listenActiveScanUnicastTask;
        if (task == null || task.isDone()) {
            listenActiveScanUnicastTask = scheduler.submit(() -> listenActiveScanUnicast());
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        cancelTask(listenBackgroundMulticastTask);
        listenBackgroundMulticastTask = null;
    }
}
