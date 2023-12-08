/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.config.discovery.addon.ip;

import static org.openhab.core.config.discovery.addon.AddonFinderConstants.SERVICE_NAME_IP;
import static org.openhab.core.config.discovery.addon.AddonFinderConstants.SERVICE_TYPE_IP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.config.discovery.addon.AddonFinder;
import org.openhab.core.config.discovery.addon.BaseAddonFinder;
import org.openhab.core.net.NetUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link IpAddonFinder} for finding suggested add-ons by sending IP packets to the
 * network and collecting responses.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
@Component(service = AddonFinder.class, name = IpAddonFinder.SERVICE_NAME)
public class IpAddonFinder extends BaseAddonFinder {

    public static final String SERVICE_TYPE = SERVICE_TYPE_IP;
    public static final String SERVICE_NAME = SERVICE_NAME_IP;

    private final Logger logger = LoggerFactory.getLogger(IpAddonFinder.class);

    @Activate
    public IpAddonFinder() {
        logger.warn("IpAddonFinder::IpAddonFinder");
    }

    Set<AddonInfo> scan() {
        Set<AddonInfo> result = new HashSet<>();
        for (AddonInfo candidate : addonCandidates) {
            for (AddonDiscoveryMethod method : candidate.getDiscoveryMethods().stream()
                    .filter(method -> SERVICE_TYPE.equals(method.getServiceType())).toList()) {

                Map<String, String> matchProperties = method.getMatchProperties().stream()
                        .collect(Collectors.toMap(property -> property.getName(), property -> property.getRegex()));

                String type = matchProperties.get("type");
                String request = matchProperties.get("request");
                int timeoutMs = Integer.parseInt(Objects.toString(matchProperties.get("timeout_ms")));
                @Nullable
                InetAddress destIp = null;
                try {
                    destIp = InetAddress.getByName(matchProperties.get("dest_ip"));
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block

                }
                int destPort = Integer.parseInt(Objects.toString(matchProperties.get("dest_port")));

                if ("ip_multicast".equals(type)) {

                    List<String> ipAddresses = NetUtil.getAllInterfaceAddresses().stream()
                            .filter(a -> a.getAddress() instanceof Inet4Address)
                            .map(a -> a.getAddress().getHostAddress()).toList();

                    for (String localIp : ipAddresses) {
                        try {

                            DatagramChannel channel = (DatagramChannel) DatagramChannel
                                    .open(StandardProtocolFamily.INET)
                                    .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                                    .bind(new InetSocketAddress(localIp, 0))
                                    .setOption(StandardSocketOptions.IP_MULTICAST_TTL, 64).configureBlocking(false);
                            InetSocketAddress sock = (InetSocketAddress) channel.getLocalAddress();

                            ByteArrayOutputStream requestFrame = new ByteArrayOutputStream();
                            StringTokenizer parts = new StringTokenizer(request);

                            while (parts.hasMoreTokens()) {
                                String token = parts.nextToken();
                                if (token.startsWith("$")) {
                                    switch (token) {
                                        case "$src_ip":
                                            byte[] adr = sock.getAddress().getAddress();
                                            requestFrame.write(adr);
                                            break;
                                        case "$src_port":
                                            int dPort = sock.getPort();
                                            requestFrame.write((byte) ((dPort >> 8) & 0xff));
                                            requestFrame.write((byte) (dPort & 0xff));
                                            break;
                                        default:
                                            logger.warn("unknown token");
                                    }
                                } else {
                                    int i = Integer.decode(token);
                                    requestFrame.write((byte) i);
                                }
                            }
                            logger.info("{}", HexFormat.of().withDelimiter(" ").formatHex(requestFrame.toByteArray()));

                            channel.send(ByteBuffer.wrap(requestFrame.toByteArray()),
                                    new InetSocketAddress(destIp, destPort));

                            // listen to responses
                            Selector selector = Selector.open();
                            ByteBuffer buffer = ByteBuffer.wrap(new byte[50]);
                            channel.register(selector, SelectionKey.OP_READ);
                            selector.select(timeoutMs);
                            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                            if (it.hasNext()) {
                                final SocketAddress source = ((DatagramChannel) it.next().channel()).receive(buffer);
                                logger.debug("Received return frame from {}",
                                        ((InetSocketAddress) source).getAddress().getHostAddress());
                                result.add(candidate);
                            } else {
                                logger.debug("no response");
                            }

                        } catch (IOException e) {
                            logger.trace("KNXnet/IP discovery failed on {}", localIp, e);
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Set<AddonInfo> getSuggestedAddons() {
        logger.trace("IpAddonFinder::getSuggestedAddons");
        Set<AddonInfo> result = new HashSet<>();

        result = scan();

        return result;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }
}
