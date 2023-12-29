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
package org.openhab.core.config.core.internal.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.net.CidrAddress;
import org.openhab.core.net.NetUtil;
import org.osgi.service.component.annotations.Component;

/**
 * Provides a list of IPv4 addresses of the local machine and shows the user which interface belongs to which IP address
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
@Component
public class NetworkConfigOptionProvider implements ConfigOptionProvider {
    static final URI CONFIG_URI = URI.create("system:network");
    static final String PARAM_PRIMARY_ADDRESS = "primaryAddress";
    static final String PARAM_BROADCAST_ADDRESS = "broadcastAddress";
    static final String NETWORK_INTERFACE = "network-interface";

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        if (CONFIG_URI.equals(uri)) {
            switch (param) {
                case PARAM_PRIMARY_ADDRESS:
                    Stream<CidrAddress> ipv4Addresses = NetUtil.getAllInterfaceAddresses().stream()
                            .filter(a -> a.getAddress() instanceof Inet4Address);
                    return ipv4Addresses.map(a -> new ParameterOption(a.toString(), a.toString())).toList();
                case PARAM_BROADCAST_ADDRESS:
                    List<String> broadcastAddrList = new ArrayList<>(NetUtil.getAllBroadcastAddresses());
                    broadcastAddrList.add("255.255.255.255");
                    return broadcastAddrList.stream().distinct().map(a -> new ParameterOption(a, a)).toList();
                default:
                    return null;
            }
        } else if (NETWORK_INTERFACE.equals(context)) {
            try {
                List<ParameterOption> options = new ArrayList<>();
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                while (networkInterfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = networkInterfaces.nextElement();
                    options.add(new ParameterOption(networkInterface.getName(),
                            getNetworkInterfaceLabel(networkInterface)));
                }
                return options;
            } catch (SocketException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private String getNetworkInterfaceLabel(NetworkInterface networkInterface) {
        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        String hostName = null;
        while (inetAddresses.hasMoreElements()) {
            InetAddress inetAddress = inetAddresses.nextElement();
            if (inetAddress instanceof Inet4Address) {
                hostName = inetAddress.getHostName();
                break;
            } else if (hostName == null) {
                hostName = inetAddress.getHostName();
            }
        }
        return hostName == null ? networkInterface.getName()
                : String.format("%s (%s)", networkInterface.getName(), hostName);
    }
}
