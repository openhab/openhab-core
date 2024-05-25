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
import org.openhab.core.config.discovery.sddp.SddpDeviceParticipant;
import org.openhab.core.config.discovery.sddp.SddpDiscoveryParticipant;
import org.openhab.core.config.discovery.sddp.SddpDiscoveryServiceInterface;
import org.openhab.core.net.CidrAddress;
import org.openhab.core.net.NetworkAddressChangeListener;
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
 * Support for finders can be achieved by implementing and registering a {@link SddpDeviceParticipant}.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = DiscoveryService.class, configurationPid = "discovery.sddp")
public class SddpDiscoveryService extends AbstractDiscoveryService
        implements AutoCloseable, NetworkAddressChangeListener, SddpDiscoveryServiceInterface {

    private static final int SDDP_PORT = 1902;
    private static final String SDDP_IP_ADDRESS = "239.255.255.250";
    private static final InetSocketAddress SDDP_GROUP = new InetSocketAddress(SDDP_IP_ADDRESS, SDDP_PORT);

    private static final int READ_BUFFER_SIZE = 1024;
    private static final Duration SOCKET_TIMOUT = Duration.ofMillis(1000);
    private static final Duration SEARCH_LISTEN_DURATION = Duration.ofSeconds(5);
    private static final Duration CACHE_PURGE_INTERVAL = Duration.ofSeconds(300);

    private static final String SEARCH_RESPONSE_HEADER = "SDDP/1.0 200 OK";
    private static final String ALIVE_NOTIFICATION_HEADER = "NOTIFY ALIVE SDDP/1.0";

    private static final String SEARCH_REQUEST_BODY_FORMAT = """
            SEARCH * SDDP/1.0
            Host: "%s:%d"
            """;

    private final Logger logger = LoggerFactory.getLogger(SddpDiscoveryService.class);
    private final Set<SddpDevice> foundDevicesCache = ConcurrentHashMap.newKeySet();
    private final Set<SddpDiscoveryParticipant> discoveryParticipants = new CopyOnWriteArraySet<>();
    private final Set<SddpDeviceParticipant> deviceParticipants = new CopyOnWriteArraySet<>();

    private final NetworkAddressService networkAddressService;

    private boolean closing = false;

    private @Nullable Future<?> listenMulticastTask = null;
    private @Nullable Future<?> listenUnicastTask = null;
    private @Nullable ScheduledFuture<?> purgeExpiredDevicesTask = null;

    @Activate
    public SddpDiscoveryService(final @Reference NetworkAddressService networkAddressService) throws IOException {
        super((int) SEARCH_LISTEN_DURATION.getSeconds());

        this.networkAddressService = networkAddressService;

        if (isBackgroundDiscoveryEnabled()) {
            startBackgroundDiscovery();
        }

        purgeExpiredDevicesTask = scheduler.scheduleWithFixedDelay(() -> purgeExpiredDevices(),
                CACHE_PURGE_INTERVAL.getSeconds(), CACHE_PURGE_INTERVAL.getSeconds(), TimeUnit.SECONDS);

        logger.trace("SddpDiscoveryService() isBackgroundDiscoveryEnabled={}", isBackgroundDiscoveryEnabled());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addSddpDeviceParticipant(SddpDeviceParticipant participant) {
        logger.trace("addSddpDeviceParticipant()");
        deviceParticipants.add(participant);
        foundDevicesCache.stream().filter(d -> !d.isExpired()).forEach(d -> participant.deviceAdded(d));
        startScan();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addSddpDiscoveryParticipant(SddpDiscoveryParticipant participant) {
        logger.trace("addSddpDiscoveryParticipant()");
        discoveryParticipants.add(participant);
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
        logger.trace("cancelTask()");
        if (task != null) {
            task.cancel(true);
        }
    }

    @Override
    public void close() {
        logger.trace("close()");
        deactivate();
    }

    /**
     * Create an {@link SddpDevice) object from UDP packet data.
     */
    public Optional<SddpDevice> createSddpDevice(String data) {
        logger.trace("createSddpDevice()");
        if (!data.isBlank()) {
            List<String> lines = data.lines().toList();
            if (lines.size() > 1) {
                String statement = lines.get(0).strip();
                if (statement.startsWith(ALIVE_NOTIFICATION_HEADER) || statement.startsWith(SEARCH_RESPONSE_HEADER)) {
                    Map<String, String> headers = new HashMap<>();
                    for (int i = 1; i < lines.size(); i++) {
                        String[] header = lines.get(i).split(":", 2);
                        if (header.length > 1) {
                            headers.put(header[0].strip(), header[1].strip());
                        }
                    }
                    return Optional.of(new SddpDevice(headers));
                }
            }
        }
        return Optional.empty();
    }

    @Deactivate
    @Override
    protected void deactivate() {
        logger.trace("deactivate()");
        closing = true;
        foundDevicesCache.clear();
        discoveryParticipants.clear();
        deviceParticipants.clear();
        cancelTask(listenUnicastTask);
        cancelTask(listenMulticastTask);
        cancelTask(purgeExpiredDevicesTask);
        listenUnicastTask = null;
        listenMulticastTask = null;
        purgeExpiredDevicesTask = null;
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        logger.trace("getSupportedThingTypes()");
        Set<ThingTypeUID> supportedThingTypes = new HashSet<>();
        discoveryParticipants.forEach(p -> supportedThingTypes.addAll(p.getSupportedThingTypeUIDs()));
        return supportedThingTypes;
    }

    /**
     * Continue to listen for incoming SDDP multicast messages until the thread is externally interrupted.
     */
    private void listenMulticast() {
        MulticastSocket socket = null;
        NetworkInterface networkInterface = null;

        try {
            networkInterface = NetworkInterface
                    .getByInetAddress(InetAddress.getByName(networkAddressService.getPrimaryIpv4HostAddress()));
            networkInterface.getHardwareAddress();

            if (logger.isDebugEnabled()) {
                logger.debug("listenMulticast() starting on interface '{}'", networkInterface.getDisplayName());
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
                logger.warn("listenMulticast() error '{}'", e.getMessage());
            }
        } finally {
            if (socket != null && networkInterface != null) {
                try {
                    socket.leaveGroup(SDDP_GROUP, networkInterface);
                } catch (IOException e) {
                    if (!closing) {
                        logger.warn("listenMulticast() error '{}'", e.getMessage());
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
    private void listenUnicast() {
        // get a free port number
        int port;
        try (ServerSocket portFinder = new ServerSocket(0)) {
            port = portFinder.getLocalPort();
        } catch (IOException e) {
            logger.warn("listenUnicast() port finder error '{}'", e.getMessage());
            return;
        }

        try (DatagramSocket socket = new DatagramSocket(port)) {
            String ipAddress = networkAddressService.getPrimaryIpv4HostAddress();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(ipAddress));

            if (logger.isDebugEnabled()) {
                logger.debug("listenUnicast() starting on '{}:{}' on interface '{}'", ipAddress, port,
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
            packet = null;
            logger.debug("Packet sent to '{}:{}' content:\r\n{}", SDDP_IP_ADDRESS, SDDP_PORT, search);

            final Instant listenDoneTime = Instant.now().plus(SEARCH_LISTEN_DURATION);

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
                logger.warn("listenUnicast() error '{}'", e.getMessage());
            }
        }
    }

    /**
     * If the network interfaces change then cancel and recreate all pending tasks.
     */
    @Override
    public synchronized void onChanged(List<CidrAddress> added, List<CidrAddress> removed) {
        logger.trace("onChanged() i.e. network interfaces");
        Future<?> multicastTask = listenMulticastTask;
        if (multicastTask != null && !multicastTask.isDone()) {
            multicastTask.cancel(false);
            listenMulticastTask = scheduler.submit(() -> listenMulticast());
        }
        Future<?> unicastTask = listenUnicastTask;
        if (unicastTask != null && !unicastTask.isDone()) {
            unicastTask.cancel(false);
            listenUnicastTask = scheduler.submit(() -> listenUnicast());
        }
    }

    /**
     * Process the {@link DatagramPacket} content by trying to create an {@link SddpDevice} and eventually adding it to
     * the foundDevicesCache, and if so, then notifying all listeners.
     * 
     * @param packet a datagram packet that arrived over the network.
     */
    private synchronized void processPacket(DatagramPacket packet) {
        logger.trace("processPacket()");
        String content = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        if (logger.isDebugEnabled()) {
            logger.debug("Packet received from '{}:{}' content:\r\n{}", packet.getAddress().getHostAddress(),
                    packet.getPort(), content);
        }
        Optional<SddpDevice> deviceOptional = createSddpDevice(content);
        if (deviceOptional.isPresent()) {
            SddpDevice device = deviceOptional.get();
            foundDevicesCache.remove(device); // force update fields that are not set-unique
            foundDevicesCache.add(device);
            logger.debug("processPacket() foundDevices={}, deviceParticipants={}, discoveryParticipants={}",
                    foundDevicesCache.size(), deviceParticipants.size(), discoveryParticipants.size());
            discoveryParticipants.forEach(p -> {
                DiscoveryResult result = p.createResult(device);
                if (result != null) {
                    DiscoveryResult localizedResult = getLocalizedDiscoveryResult(result,
                            FrameworkUtil.getBundle(p.getClass()));
                    thingDiscovered(localizedResult);
                }
            });
            deviceParticipants.forEach(f -> f.deviceAdded(device));
        }
    }

    /**
     * Purge expired devices and notify all listeners.
     */
    private synchronized void purgeExpiredDevices() {
        logger.trace("purgeExpiredDevices()");
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

    protected void removeSddpDeviceParticipant(SddpDeviceParticipant participant) {
        logger.trace("removeSddpDeviceListener()");
        deviceParticipants.remove(participant);
    }

    protected void removeSddpDiscoveryParticipant(SddpDiscoveryParticipant participant) {
        logger.trace("removeSddpDiscoveryParticipant()");
        discoveryParticipants.remove(participant);
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.trace("startBackgroundDiscovery()");
        Future<?> task = listenMulticastTask;
        if (task == null || task.isDone()) {
            listenMulticastTask = scheduler.submit(() -> listenMulticast());
        }
    }

    /**
     * Schedule to send one single SDDP SEARCH request, and listen for responses.
     */
    @Override
    protected void startScan() {
        logger.trace("startScan()");
        Future<?> task = listenUnicastTask;
        if (task == null || task.isDone()) {
            listenUnicastTask = scheduler.submit(() -> listenUnicast());
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.trace("stopBackgroundDiscovery()");
        cancelTask(listenMulticastTask);
        listenMulticastTask = null;
    }
}
