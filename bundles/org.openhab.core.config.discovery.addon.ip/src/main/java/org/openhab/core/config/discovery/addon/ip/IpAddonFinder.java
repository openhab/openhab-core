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
import java.text.ParseException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.discovery.addon.AddonFinder;
import org.openhab.core.config.discovery.addon.BaseAddonFinder;
import org.openhab.core.net.NetUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link IpAddonFinder} for finding suggested add-ons by sending IP packets to the
 * network and collecting responses.
 * 
 * @implNote On activation, a thread is spawned which handles the detection. Scan runs once,
 *           no continuous background scanning.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
@Component(service = AddonFinder.class, name = IpAddonFinder.SERVICE_NAME)
public class IpAddonFinder extends BaseAddonFinder {

    public static final String SERVICE_TYPE = SERVICE_TYPE_IP;
    public static final String SERVICE_NAME = SERVICE_NAME_IP;

    private static final String TYPE_IP_MULTICAST = "ipMulticast";
    private static final String MATCH_PROPERTY_RESPONSE = "response";
    private static final String PARAMETER_DEST_IP = "destIp";
    private static final String PARAMETER_DEST_PORT = "destPort";
    private static final String PARAMETER_REQUEST = "request";
    private static final String PARAMETER_SRC_IP = "srcIp";
    private static final String PARAMETER_SRC_PORT = "srcPort";
    private static final String PARAMETER_TIMEOUT_MS = "timeoutMs";

    private final Logger logger = LoggerFactory.getLogger(IpAddonFinder.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(SERVICE_NAME);
    private @Nullable Future<?> scanJob = null;
    Set<AddonInfo> suggestions = new HashSet<>();

    public IpAddonFinder() {
        logger.trace("IpAddonFinder::IpAddonFinder");
        // start of scan will be triggered by setAddonCandidates to ensure addonCandidates are available
    }

    @Deactivate
    public void deactivate() {
        logger.trace("IpAddonFinder::deactivate");
        stopScan();
    }

    public void setAddonCandidates(List<AddonInfo> candidates) {
        logger.debug("IpAddonFinder::setAddonCandidates({})", candidates.size());
        super.setAddonCandidates(candidates);
        startScan();
    }

    synchronized void startScan() {
        if (scanJob == null) {
            scanJob = scheduler.schedule(this::scan, 1, TimeUnit.SECONDS);
        }
    }

    void stopScan() {
        Future<?> tmpScanJob = scanJob;
        if (tmpScanJob != null) {
            if (!tmpScanJob.isDone()) {
                logger.trace("Trying to cancel IP scan");
                tmpScanJob.cancel(true);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                }
            }
            scanJob = null;
        }
    }

    void scan() {
        logger.trace("IpAddonFinder::scan started");
        for (AddonInfo candidate : addonCandidates) {
            for (AddonDiscoveryMethod method : candidate.getDiscoveryMethods().stream()
                    .filter(method -> SERVICE_TYPE.equals(method.getServiceType())).toList()) {

                logger.trace("Checking candidate: {}", candidate.getUID());

                Map<String, String> parameters = method.getParameters().stream()
                        .collect(Collectors.toMap(property -> property.getName(), property -> property.getValue()));
                Map<String, String> matchProperties = method.getMatchProperties().stream()
                        .collect(Collectors.toMap(property -> property.getName(), property -> property.getRegex()));

                // parse standard set op parameters:
                String type = Objects.toString(parameters.get("type"), "");
                String request = Objects.toString(parameters.get(PARAMETER_REQUEST), "");
                String response = Objects.toString(matchProperties.get(MATCH_PROPERTY_RESPONSE), "");
                int timeoutMs = 0;
                try {
                    timeoutMs = Integer.parseInt(Objects.toString(parameters.get(PARAMETER_TIMEOUT_MS)));
                } catch (NumberFormatException e) {
                    logger.info("{}: discovery-parameter " + PARAMETER_TIMEOUT_MS + "cannot be parsed",
                            candidate.getUID());
                    continue;
                }
                @Nullable
                InetAddress destIp = null;
                try {
                    destIp = InetAddress.getByName(parameters.get(PARAMETER_DEST_IP));
                } catch (UnknownHostException e) {
                    logger.info("{}: discovery-parameter " + PARAMETER_DEST_IP + " cannot be parsed",
                            candidate.getUID());
                    continue;
                }
                int destPort = 0;
                try {
                    destPort = Integer.parseInt(Objects.toString(parameters.get(PARAMETER_DEST_PORT)));
                } catch (NumberFormatException e) {
                    logger.warn("{}: discovery-parameter '{}' cannot be parsed",
                            candidate.getUID(), PARAMETER_DEST_PORT);
                    continue;
                }

                //
                // handle known types
                //
                try {
                    switch (Objects.toString(type)) {
                        case TYPE_IP_MULTICAST:
                            List<String> ipAddresses = NetUtil.getAllInterfaceAddresses().stream()
                                    .filter(a -> a.getAddress() instanceof Inet4Address)
                                    .map(a -> a.getAddress().getHostAddress()).toList();

                            for (String localIp : ipAddresses) {
                                try {
                                    DatagramChannel channel = (DatagramChannel) DatagramChannel
                                            .open(StandardProtocolFamily.INET)
                                            .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                                            .bind(new InetSocketAddress(localIp, 0))
                                            .setOption(StandardSocketOptions.IP_MULTICAST_TTL, 64)
                                            .configureBlocking(false);

                                    byte[] requestArray = buildRequestArray(channel, Objects.toString(request));
                                    logger.trace("{}: {}", candidate.getUID(),
                                            HexFormat.of().withDelimiter(" ").formatHex(requestArray));

                                    channel.send(ByteBuffer.wrap(requestArray),
                                            new InetSocketAddress(destIp, destPort));

                                    // listen to responses
                                    Selector selector = Selector.open();
                                    ByteBuffer buffer = ByteBuffer.wrap(new byte[50]);
                                    channel.register(selector, SelectionKey.OP_READ);
                                    selector.select(timeoutMs);
                                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();

                                    switch (Objects.toString(response)) {
                                        case ".*":
                                            if (it.hasNext()) {
                                                final SocketAddress source = ((DatagramChannel) it.next().channel())
                                                        .receive(buffer);
                                                logger.debug("Received return frame from {}",
                                                        ((InetSocketAddress) source).getAddress().getHostAddress());
                                                suggestions.add(candidate);
                                                logger.debug("Suggested add-on found: {}", candidate.getUID());
                                            } else {
                                                logger.trace("{}: no response", candidate.getUID());
                                            }
                                            break;
                                        default:
                                            logger.warn("{}: match-property response \"{}\" is unknown",
                                                    candidate.getUID(), type);
                                            break; // end loop
                                    }

                                } catch (IOException e) {
                                    logger.debug("{}: network error", candidate.getUID(), e);
                                }
                            }
                            break;

                        default:
                            logger.warn("{}: discovery-parameter type \"{}\" is unknown", candidate.getUID(), type);
                    }
                } catch (ParseException | NumberFormatException none) {
                    continue;
                }
            }
        }
        logger.trace("IpAddonFinder::scan completed");
    }

    byte[] buildRequestArray(DatagramChannel channel, String request) throws java.io.IOException, ParseException {
        InetSocketAddress sock = (InetSocketAddress) channel.getLocalAddress();

        ByteArrayOutputStream requestFrame = new ByteArrayOutputStream();
        StringTokenizer parts = new StringTokenizer(request);

        while (parts.hasMoreTokens()) {
            String token = parts.nextToken();
            if (token.startsWith("$")) {
                switch (token) {
                    case "$" + PARAMETER_SRC_IP:
                        byte[] adr = sock.getAddress().getAddress();
                        requestFrame.write(adr);
                        break;
                    case "$" + PARAMETER_SRC_PORT:
                        int dPort = sock.getPort();
                        requestFrame.write((byte) ((dPort >> 8) & 0xff));
                        requestFrame.write((byte) (dPort & 0xff));
                        break;
                    default:
                        logger.warn("Unknown token in request frame \"{}\"", token);
                        throw new ParseException(token, 0);
                }
            } else {
                int i = Integer.decode(token);
                requestFrame.write((byte) i);
            }
        }
        return requestFrame.toByteArray();
    }

    @Override
    public Set<AddonInfo> getSuggestedAddons() {
        logger.trace("IpAddonFinder::getSuggestedAddons {}/{}", suggestions.size(), addonCandidates.size());
        return suggestions;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }
}
