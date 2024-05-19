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
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.config.discovery.sddp.SddpDiscoveryParticipant;
import org.openhab.core.config.discovery.sddp.SddpInfo;
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
 * Support for further devices can be added by implementing and registering a {@link SddpDiscoveryParticipant}.
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

    private static final int LISTEN_DURATION_SECONDS = 5;

    private final InetAddress addressMulticast;
    private final InetAddress addressUnicast;

    private final Logger logger = LoggerFactory.getLogger(SddpDiscoveryService.class);
    private final Map<String, SddpInfo> foundDevices = new ConcurrentHashMap<>();
    private final Set<SddpDiscoveryParticipant> participants = new CopyOnWriteArraySet<>();

    private boolean listenMulticast = false;
    private boolean listenUnicast = false;

    private @Nullable Future<?> listenMulticastTask = null;
    private @Nullable Future<?> listenUnicastTask = null;
    private @Nullable ScheduledFuture<Boolean> listenUnicastCancelTask = null;

    @Activate
    public SddpDiscoveryService() throws SocketException, UnknownHostException {
        super(LISTEN_DURATION_SECONDS);
        addressMulticast = InetAddress.getByName(ADDRESS_MULTICAST);
        addressUnicast = InetAddress.getLocalHost();
        if (isBackgroundDiscoveryEnabled()) {
            startBackgroundDiscovery();
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addSddpDiscoveryParticipant(SddpDiscoveryParticipant participant) {
        participants.add(participant);
        foundDevices.values().forEach(device -> participant.createResult(device));
    }

    protected void removeSddpDiscoveryParticipant(SddpDiscoveryParticipant participant) {
        participants.remove(participant);
    }

    @Override
    public void close() {
        deactivate();
    }

    /**
     * Create an SddpInfo object from UDP packet data.
     */
    public Optional<SddpInfo> createSddpInfo(String data) {
        if (!data.isBlank()) {
            List<String> headers = data.lines().toList();
            if (headers.size() > 1) {
                String method = headers.get(0).strip();
                if (method.startsWith(ALIVE_NOTIFICATION) || method.startsWith(SEARCH_RESPONSE)) {
                    Map<String, String> headerMap = new HashMap<>();
                    for (int i = 1; i < headers.size(); i++) {
                        String[] header = headers.get(i).split(":", 2);
                        if (header.length > 1) {
                            headerMap.put(header[0].strip(), header[1].strip().replace("\"", ""));
                        }
                    }
                    return Optional.of(new SddpInfo(headerMap));
                }
            }
        }
        return Optional.empty();
    }

    @Deactivate
    @Override
    protected void deactivate() {
        listenMulticast = false;
        listenUnicast = false;
        foundDevices.clear();
        Future<?> listenMulticastTask = this.listenMulticastTask;
        if (listenMulticastTask != null) {
            listenMulticastTask.cancel(true);
            this.listenMulticastTask = null;
        }
        Future<?> listenUnicastTask = this.listenUnicastTask;
        if (listenUnicastTask != null) {
            listenUnicastTask.cancel(true);
            this.listenUnicastTask = null;
        }
        ScheduledFuture<Boolean> listenUnicastCancelTask = this.listenUnicastCancelTask;
        if (listenUnicastCancelTask != null) {
            listenUnicastCancelTask.cancel(true);
            this.listenUnicastCancelTask = null;
        }
        super.deactivate();
    }

    /**
     * Handle the DatagramPacket by trying to create a SddpInfo and eventually adding it to the foundDevices map.
     */
    private void handlePacket(DatagramPacket packet) {
        Optional<SddpInfo> sddpInfo = createSddpInfo(
                new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8));
        if (sddpInfo.isPresent()) {
            foundDevices.put(packet.getAddress().getHostAddress(), sddpInfo.get());
        }
    }

    /**
     * Listen for incoming SDDP multicast messages.
     */
    private void listenMulticast() {
        try (MulticastSocket socket = new MulticastSocket(PORT_MULTICAST)) {
            socket.joinGroup(new InetSocketAddress(addressMulticast, PORT_MULTICAST), NetworkInterface.getByIndex(0));
            while (listenMulticast) {
                byte[] buf = new byte[2048];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                handlePacket(packet);
            }
        } catch (IOException e) {
            logger.warn("SDDP multicast listen error {}", e.getMessage());
        }
    }

    /**
     * Listen for incoming SDDP unicast messages.
     */
    private void listenUnicast() {
        try (DatagramSocket socket = new DatagramSocket(PORT_UNICAST)) {
            while (listenUnicast) {
                byte[] buf = new byte[2048];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                handlePacket(packet);
            }
        } catch (IOException e) {
            logger.warn("SDDP unicast listen error {}", e.getMessage());
        }
    }

    @Override
    protected void startBackgroundDiscovery() {
        if (!listenMulticast) {
            listenMulticast = true;
            listenMulticastTask = scheduler.submit(() -> listenMulticast());
        }
    }

    /**
     * Send a single SDDP SEARCH broadcast.
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
            listenUnicastCancelTask = scheduler.schedule(() -> listenUnicast = false, LISTEN_DURATION_SECONDS,
                    TimeUnit.SECONDS);
        } catch (IOException e) {
            logger.warn("SDDP client error {}", e.getMessage());
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        listenMulticast = false;
    }
}
