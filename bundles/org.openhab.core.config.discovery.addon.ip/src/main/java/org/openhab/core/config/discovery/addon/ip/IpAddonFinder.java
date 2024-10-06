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
package org.openhab.core.config.discovery.addon.ip;

import static org.openhab.core.config.discovery.addon.AddonFinderConstants.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonMatchProperty;
import org.openhab.core.addon.AddonParameter;
import org.openhab.core.addon.AddonService;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.discovery.addon.AddonFinder;
import org.openhab.core.config.discovery.addon.BaseAddonFinder;
import org.openhab.core.net.CidrAddress;
import org.openhab.core.net.NetUtil;
import org.openhab.core.net.NetworkAddressChangeListener;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.util.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link IpAddonFinder} for finding suggested add-ons by sending IP packets to the
 * network and collecting responses.
 * 
 * This finder is intended to detect devices on the network which do not announce via UPnP
 * or mDNS. Some devices respond to queries to defined multicast addresses and ports and thus
 * can be detected by sending a single frame on the IP network.
 * <p>
 * Be aware of possible side effects of sending packets to unknown devices in the network!
 * This is why the IP finder is not intended for large scale network scanning, e.g. using
 * large port or IP ranges.
 * <p>
 * <strong>Configuration</strong>
 * <p>
 * The following parameters can be used to configure frames to be sent to the network:
 * <p>
 * <table border="1">
 * <tr>
 * <td><b>discovery-parameter</b></td>
 * <td><b>values</b></td>
 * <td><b>comment</b></td>
 * </tr>
 * <tr>
 * <td>{@code type}</td>
 * <td>ipMulticast</td>
 * <td>no other options implemented</td>
 * </tr>
 * <tr>
 * <td>{@code destIp}</td>
 * <td>destination IP address</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@code destPort}</td>
 * <td>destination port</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@code listenPort}</td>
 * <td>port to use for listening to responses (optional)</td>
 * <td>privileged ports ({@code <1024}) not allowed</td>
 * </tr>
 * <tr>
 * <td>{@code request}</td>
 * <td>description of request frame as hex bytes separated by spaces (e.g. 0x01 0x02 ...)</td>
 * <td>dynamic replacement of variables $srcIp, $srcPort and $uuid, no others implemented yet
 * </tr>
 * <tr>
 * <td>{@code requestPlain}</td>
 * <td>description of request frame as plaintext string</td>
 * <td>dynamic replacement of variables $srcIp, $srcPort and $uuid, no others implemented yet;
 * standard backslash sequences will be translated, and in addition to {@code \}, there are five
 * XML special characters which need to be escaped:
 *
 * <pre>{@code
 * & - &amp;
 * < - &lt;
 * > - &gt;
 * " - &quot;
 * ' - &apos;
 * }</pre>
 * </tr>
 * <tr>
 * <td>{@code timeoutMs}</td>
 * <td>timeout to wait for a answers</td>
 * <td></td>
 * </tr>
 * </table>
 * <p>
 * <table border="1">
 * <tr>
 * <td><b>dynamic replacement</b> (in {@code request*})</td>
 * <td><b>value</b></td>
 * </tr>
 * <tr>
 * <td>{@code $srcIp}</td>
 * <td>source IP address</td>
 * </tr>
 * <tr>
 * <td>{@code $srcPort}</td>
 * <td>source port</td>
 * </tr>
 * <td>{@code $uuid}</td>
 * <td>String returned by {@code java.util.UUID.randomUUID()}</td>
 * </tr>
 * </table>
 * <p>
 * Packets are sent out on every available network interface.
 * <p>
 * There is currently only one match-property defined: {@code response}.
 * It allows a regex match, but currently only ".*" is supported.
 * <p>
 * <strong>Limitations</strong>
 * <p>
 * The {@link IpAddonFinder} is still under active development.
 * There are limitations:
 * <ul>
 * <li>Currently every returned frame is considered as success, regex matching is not implemented.
 * <li>Frames are sent only on startup (or if an {@link org.openhab.core.addon.AddonInfoProvider}
 * calls {@link #setAddonCandidates(List)}), no background scanning.
 * <ul>
 *
 * @apiNote The {@link IpAddonFinder} is still under active development, it has initially
 *          been developed to detect KNX installations and will be extended. Configuration parameters
 *          and supported features may still change.
 *
 * @implNote On activation, a thread is spawned which handles the detection. Scan runs once,
 *           no continuous background scanning.
 *
 * @author Holger Friedrich - Initial contribution
 * @author Jacob Laursen - Added support for broadcast-based scanning
 * @author Andrew Fiddian-Green - Parallelization
 */
@NonNullByDefault
@Component(service = AddonFinder.class, name = IpAddonFinder.SERVICE_NAME)
public class IpAddonFinder extends BaseAddonFinder implements NetworkAddressChangeListener {

    public static final String SERVICE_TYPE = SERVICE_TYPE_IP;
    public static final String SERVICE_NAME = SERVICE_NAME_IP;

    private static final String TYPE_IP_BROADCAST = "ipBroadcast";
    private static final String TYPE_IP_MULTICAST = "ipMulticast";
    private static final String MATCH_PROPERTY_RESPONSE = "response";
    private static final String PARAMETER_DEST_IP = "destIp";
    private static final String PARAMETER_DEST_PORT = "destPort";
    private static final String PARAMETER_LISTEN_PORT = "listenPort";
    private static final String PARAMETER_REQUEST = "request";
    private static final String PARAMETER_REQUEST_PLAIN = "requestPlain";
    private static final String PARAMETER_SRC_IP = "srcIp";
    private static final String PARAMETER_SRC_PORT = "srcPort";
    private static final String PARAMETER_TIMEOUT_MS = "timeoutMs";
    private static final String REPLACEMENT_UUID = "uuid";

    private final Logger logger = LoggerFactory.getLogger(IpAddonFinder.class);
    private final NetworkAddressService networkAddressService;
    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);
    private final Set<AddonService> addonServices = new CopyOnWriteArraySet<>();
    private @Nullable Future<?> scanJob = null;
    private final List<CompletableFuture<?>> scanJobs = new CopyOnWriteArrayList<>();
    private final Set<AddonInfo> suggestions = new CopyOnWriteArraySet<>();

    @Activate
    public IpAddonFinder(final @Reference NetworkAddressService networkAddressService) {
        logger.trace("IpAddonFinder::IpAddonFinder");
        // start of scan will be triggered by setAddonCandidates to ensure addonCandidates are available
        this.networkAddressService = networkAddressService;
        this.networkAddressService.addNetworkAddressChangeListener(this);
    }

    @Deactivate
    public void deactivate() {
        logger.trace("IpAddonFinder::deactivate");
        networkAddressService.removeNetworkAddressChangeListener(this);
        stopScanJob();
    }

    @Override
    public void setAddonCandidates(List<AddonInfo> candidates) {
        logger.debug("IpAddonFinder::setAddonCandidates({})", candidates.size());
        super.setAddonCandidates(candidates);
        startScanJob(20);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addAddonService(AddonService featureService) {
        this.addonServices.add(featureService);
    }

    protected void removeAddonService(AddonService featureService) {
        this.addonServices.remove(featureService);
    }

    @Override
    public void onChanged(List<CidrAddress> added, List<CidrAddress> removed) {
        // Nothing to do
    }

    @Override
    public void onPrimaryAddressChanged(@Nullable String oldPrimaryAddress, @Nullable String newPrimaryAddress) {
        startScanJob(0);
    }

    private void startScanJob(long delayInSeconds) {
        // The setAddonCandidates() method is called for each info provider.
        // In order to do the scan only once, but on the full set of candidates, we have to delay the execution.
        // At the same time we must make sure that a scheduled scan is rescheduled - or (after more than our delay) is
        // executed once more.
        stopScanJob();
        logger.trace("Scheduling new IP scan");
        scanJob = scheduler.schedule(this::startScanJobs, delayInSeconds, TimeUnit.SECONDS);
    }

    // cancel a scheduled scan, including all subtasks;
    // cancelling will not always succeed if tasks are already running
    private void stopScanJob() {
        Future<?> tmpScanJob = scanJob;
        if (tmpScanJob != null) {
            if (!tmpScanJob.isDone()) {
                logger.trace("Trying to cancel IP scan");
                tmpScanJob.cancel(true);
            }
            scanJob = null;
        }
        // cancel all subtasks
        stopScanJobs();
    }

    // cancel subtasks for scanning specific add-ons;
    // cancelling will not always succeed if tasks are already running
    private synchronized void stopScanJobs() {
        scanJobs.stream().filter(j -> !j.isDone()).forEach(j -> j.cancel(true));
        scanJobs.clear();
    }

    private synchronized void startScanJobs() {
        logger.trace("IpAddonFinder::startScanJobs");
        for (AddonInfo candidate : addonCandidates) {
            for (AddonDiscoveryMethod method : candidate.getDiscoveryMethods().stream()
                    .filter(method -> SERVICE_TYPE.equals(method.getServiceType())).toList()) {

                logger.trace("Checking candidate: {}", candidate.getUID());

                // skip scanning if already installed
                if (isAddonInstalled(candidate.getUID())) {
                    logger.trace("Skipping {}, already installed", candidate.getUID());
                    continue;
                }

                Map<String, String> parameters = method.getParameters().stream()
                        .collect(Collectors.toMap(AddonParameter::getName, AddonParameter::getValue));
                Map<String, String> matchProperties = method.getMatchProperties().stream()
                        .collect(Collectors.toMap(AddonMatchProperty::getName, AddonMatchProperty::getRegex));

                // parse standard set of parameters
                String type = Objects.toString(parameters.get("type"), "");
                String request = Objects.requireNonNull(Objects.toString(parameters.get(PARAMETER_REQUEST), ""));
                String requestPlain = Objects
                        .requireNonNull(Objects.toString(parameters.get(PARAMETER_REQUEST_PLAIN), ""));
                // xor
                if (!("".equals(request) ^ "".equals(requestPlain))) {
                    logger.warn("{}: discovery-parameter '{}' or '{}' required", candidate.getUID(), PARAMETER_REQUEST,
                            PARAMETER_REQUEST_PLAIN);
                    continue;
                }
                String response = Objects
                        .requireNonNull(Objects.toString(matchProperties.get(MATCH_PROPERTY_RESPONSE), ""));
                int timeoutMs;
                try {
                    timeoutMs = Integer.parseInt(Objects.toString(parameters.get(PARAMETER_TIMEOUT_MS)));
                } catch (NumberFormatException e) {
                    logger.warn("{}: discovery-parameter '{}' cannot be parsed", candidate.getUID(),
                            PARAMETER_TIMEOUT_MS);
                    continue;
                }
                @Nullable
                InetAddress destIp;
                try {
                    destIp = InetAddress.getByName(parameters.get(PARAMETER_DEST_IP));
                } catch (UnknownHostException e) {
                    logger.warn("{}: discovery-parameter '{}' cannot be parsed", candidate.getUID(), PARAMETER_DEST_IP);
                    continue;
                }
                int destPort;
                try {
                    destPort = Integer.parseInt(Objects.toString(parameters.get(PARAMETER_DEST_PORT)));
                } catch (NumberFormatException e) {
                    logger.warn("{}: discovery-parameter '{}' cannot be parsed", candidate.getUID(),
                            PARAMETER_DEST_PORT);
                    continue;
                }
                int testListenPort = 0; // default, pick a non-privileged port
                if (parameters.get(PARAMETER_LISTEN_PORT) != null) {
                    try {
                        testListenPort = Integer.parseInt(Objects.toString(parameters.get(PARAMETER_LISTEN_PORT)));
                    } catch (NumberFormatException e) {
                        logger.warn("{}: discovery-parameter '{}' cannot be parsed", candidate.getUID(),
                                PARAMETER_LISTEN_PORT);
                        continue;
                    }
                    // do not allow privileged ports
                    if (testListenPort < 1024) {
                        logger.warn("{}: discovery-parameter '{}' not allowed, privileged port", candidate.getUID(),
                                PARAMETER_LISTEN_PORT);
                        continue;
                    }
                }
                int listenPort = testListenPort;

                // handle known types
                try {
                    switch (Objects.toString(type)) {
                        case TYPE_IP_BROADCAST:
                            scanBroadcast(candidate, request, requestPlain, response, timeoutMs, destPort);
                            break;
                        case TYPE_IP_MULTICAST:
                            scanMulticast(candidate, request, requestPlain, response, timeoutMs, listenPort, destIp,
                                    destPort);
                            break;
                        /*
                         * List<String> ipAddresses = NetUtil.getAllInterfaceAddresses().stream()
                         * .filter(a -> a.getAddress() instanceof Inet4Address)
                         * .map(a -> a.getAddress().getHostAddress()).toList();
                         * 
                         * for (String localIp : ipAddresses) {
                         * logger.trace("Scheduling scan for candidate:{}", candidate.getUID());
                         * scanJobs.add(
                         * CompletableFuture
                         * .runAsync(
                         * () -> doIpMulticastScan(candidate, type, request, requestPlain,
                         * response, timeoutMs, destIp, destPort, listenPort, localIp),
                         * scheduler));
                         * }
                         * break;
                         */
                        default:
                            logger.warn("{}: discovery-parameter type \"{}\" is unknown", candidate.getUID(), type);
                    }
                } catch (ParseException | NumberFormatException none) {
                    continue;
                }
            }
        }
    }

    private void doIpMulticastScan(AddonInfo candidate, String type, String request, String requestPlain,
            String response, int timeoutMs, @Nullable InetAddress destIp, int destPort, int listenPort,
            String localIp) {
        try (DatagramChannel channel = (DatagramChannel) DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true).bind(new InetSocketAddress(localIp, listenPort))
                .setOption(StandardSocketOptions.IP_MULTICAST_TTL, 64).configureBlocking(false);
                Selector selector = Selector.open()) {
            byte[] requestArray = "".equals(requestPlain)
                    ? buildRequestArray(channel.getLocalAddress(), Objects.toString(request))
                    : buildRequestArrayPlain(channel.getLocalAddress(), Objects.toString(requestPlain));
            if (logger.isTraceEnabled()) {
                InetSocketAddress sock = (InetSocketAddress) channel.getLocalAddress();
                String id = candidate.getUID();
                logger.trace("{}: probing {} -> {}:{}", id, localIp, destIp != null ? destIp.getHostAddress() : "",
                        destPort);
                if (!"".equals(requestPlain)) {
                    logger.trace("{}: \'{}\'", id, new String(requestArray));
                }
                logger.trace("{}: {}", id, HexFormat.of().withDelimiter(" ").formatHex(requestArray));
                logger.trace("{}: listening on {}:{} for {} ms", id, sock.getAddress().getHostAddress(), sock.getPort(),
                        timeoutMs);
            }

            channel.send(ByteBuffer.wrap(requestArray), new InetSocketAddress(destIp, destPort));

            // listen to responses
            ByteBuffer buffer = ByteBuffer.wrap(new byte[50]);
            channel.register(selector, SelectionKey.OP_READ);
            selector.select(timeoutMs);
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();

            switch (Objects.toString(response)) {
                case ".*":
                    if (it.hasNext()) {
                        final SocketAddress source = ((DatagramChannel) it.next().channel()).receive(buffer);
                        logger.debug("Received return frame from {}",
                                ((InetSocketAddress) source).getAddress().getHostAddress());
                        suggestions.add(candidate);
                        logger.debug("Suggested add-on found: {}", candidate.getUID());
                    } else {
                        logger.trace("{}: no response received on {}", candidate.getUID(), localIp);
                    }
                    break;
                default:
                    logger.warn("{}: match-property response '{}' is unknown", candidate.getUID(), type);
                    break; // end loop
            }
        } catch (IOException e) {
            logger.debug("{}: network error", candidate.getUID(), e);
        } catch (ParseException e) {
            logger.debug("{}: parsing error", candidate.getUID(), e);
        }
    }

    private void scanBroadcast(AddonInfo candidate, String request, String requestPlain, String response, int timeoutMs,
            int destPort) throws ParseException {
        if (request.isEmpty() && requestPlain.isEmpty()) {
            logger.warn("{}: match-property request and requestPlain \"{}\" is unknown", candidate.getUID(),
                    TYPE_IP_BROADCAST);
            return;
        }
        if (!request.isEmpty() && !requestPlain.isEmpty()) {
            logger.warn("{}: match-properties request and requestPlain \"{}\" are both present", candidate.getUID(),
                    TYPE_IP_BROADCAST);
            return;
        }
        if (response.isEmpty()) {
            logger.warn("{}: match-property response \"{}\" is unknown", candidate.getUID(), TYPE_IP_BROADCAST);
            return;
        }
        String broadcastAddress = networkAddressService.getConfiguredBroadcastAddress();
        logger.debug("Starting broadcast scan with address {}", broadcastAddress);

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(timeoutMs);
            byte[] sendBuffer = requestPlain.isEmpty() ? buildRequestArray(socket.getLocalSocketAddress(), request)
                    : buildRequestArrayPlain(socket.getLocalSocketAddress(), requestPlain);
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length,
                    InetAddress.getByName(broadcastAddress), destPort);
            socket.send(sendPacket);

            // wait for responses
            while (!Thread.currentThread().isInterrupted()) {
                byte[] discoverReceive = buildByteArray(response);
                byte[] receiveBuffer = new byte[discoverReceive.length];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                try {
                    socket.receive(receivePacket);
                } catch (SocketTimeoutException e) {
                    break; // leave the endless loop
                }

                byte[] data = receivePacket.getData();
                if (Arrays.equals(data, discoverReceive)) {
                    suggestions.add(candidate);
                    logger.debug("Suggested add-on found: {}", candidate.getUID());
                }
            }
        } catch (IOException e) {
            logger.debug("{}: network error", candidate.getUID(), e);
        }
    }

    private byte[] buildByteArray(String input) {
        ByteArrayOutputStream requestFrame = new ByteArrayOutputStream();
        StringTokenizer parts = new StringTokenizer(input);

        while (parts.hasMoreTokens()) {
            String token = parts.nextToken();
            int i = Integer.decode(token);
            requestFrame.write((byte) i);
        }
        return requestFrame.toByteArray();
    }

    private void scanMulticast(AddonInfo candidate, String request, String requestPlain, String response, int timeoutMs,
            int listenPort, @Nullable InetAddress destIp, int destPort) throws ParseException {
        List<String> ipAddresses = NetUtil.getAllInterfaceAddresses().stream()
                .filter(a -> a.getAddress() instanceof Inet4Address).map(a -> a.getAddress().getHostAddress()).toList();

        for (String localIp : ipAddresses) {
            try (DatagramChannel channel = (DatagramChannel) DatagramChannel.open(StandardProtocolFamily.INET)
                    .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                    .bind(new InetSocketAddress(localIp, listenPort))
                    .setOption(StandardSocketOptions.IP_MULTICAST_TTL, 64).configureBlocking(false);
                    Selector selector = Selector.open()) {
                byte[] requestArray = "".equals(requestPlain)
                        ? buildRequestArray(channel.getLocalAddress(), Objects.toString(request))
                        : buildRequestArrayPlain(channel.getLocalAddress(), Objects.toString(requestPlain));
                if (logger.isTraceEnabled()) {
                    InetSocketAddress sock = (InetSocketAddress) channel.getLocalAddress();
                    String id = candidate.getUID();
                    logger.trace("{}: probing {} -> {}:{}", id, localIp, destIp != null ? destIp.getHostAddress() : "",
                            destPort);
                    if (!"".equals(requestPlain)) {
                        logger.trace("{}: \'{}\'", id, new String(requestArray));
                    }
                    logger.trace("{}: {}", id, HexFormat.of().withDelimiter(" ").formatHex(requestArray));
                    logger.trace("{}: listening on {}:{} for {} ms", id, sock.getAddress().getHostAddress(),
                            sock.getPort(), timeoutMs);
                }

                channel.send(ByteBuffer.wrap(requestArray), new InetSocketAddress(destIp, destPort));

                // listen to responses
                ByteBuffer buffer = ByteBuffer.wrap(new byte[50]);
                channel.register(selector, SelectionKey.OP_READ);
                selector.select(timeoutMs);
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();

                switch (Objects.toString(response)) {
                    case ".*":
                        if (it.hasNext()) {
                            final SocketAddress source = ((DatagramChannel) it.next().channel()).receive(buffer);
                            logger.debug("Received return frame from {}",
                                    ((InetSocketAddress) source).getAddress().getHostAddress());
                            suggestions.add(candidate);
                            logger.debug("Suggested add-on found: {}", candidate.getUID());
                        } else {
                            logger.trace("{}: no response received on {}", candidate.getUID(), localIp);
                        }
                        break;
                    default:
                        logger.warn("{}: match-property response \"{}\" is unknown", candidate.getUID(),
                                TYPE_IP_MULTICAST);
                        break; // end loop
                }
            } catch (IOException e) {
                logger.debug("{}: network error", candidate.getUID(), e);
            }
        }
    }

    // build from plaintext string
    private byte[] buildRequestArrayPlain(SocketAddress address, String request)
            throws java.io.IOException, ParseException {
        InetSocketAddress sock = (InetSocketAddress) address;

        // replace first
        StringBuilder req = new StringBuilder(request);
        int p;
        while ((p = req.indexOf("$" + PARAMETER_SRC_IP)) != -1) {
            req.replace(p, p + PARAMETER_SRC_IP.length() + 1, sock.getAddress().getHostAddress());
        }
        while ((p = req.indexOf("$" + PARAMETER_SRC_PORT)) != -1) {
            req.replace(p, p + PARAMETER_SRC_PORT.length() + 1, "" + sock.getPort());
        }
        while ((p = req.indexOf("$" + REPLACEMENT_UUID)) != -1) {
            req.replace(p, p + REPLACEMENT_UUID.length() + 1, UUID.randomUUID().toString());
        }

        @Nullable
        String reqUnEscaped = StringUtils.unEscapeXml(req.toString());
        return reqUnEscaped != null ? reqUnEscaped.translateEscapes().getBytes() : new byte[0];
    }

    // build from hex string
    private byte[] buildRequestArray(SocketAddress address, String request) throws java.io.IOException, ParseException {
        InetSocketAddress sock = (InetSocketAddress) address;

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
                    case "$" + REPLACEMENT_UUID:
                        String uuid = UUID.randomUUID().toString();
                        requestFrame.write(uuid.getBytes());
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

    private boolean isAddonInstalled(String addonId) {
        for (AddonService addonService : addonServices) {
            Addon addon = addonService.getAddon(addonId, null);
            if (addon != null && addon.isInstalled()) {
                return true;
            }
        }
        return false;
    }
}
