/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.io.transport.mdns.internal;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Duration;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.openhab.core.io.transport.mdns.MDNSClient;
import org.openhab.core.io.transport.mdns.ServiceDescription;
import org.openhab.core.net.CidrAddress;
import org.openhab.core.net.NetworkAddressChangeListener;
import org.openhab.core.net.NetworkAddressService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class starts the JmDNS and implements interface to register and unregister services.
 *
 * @author Victor Belov - Initial contribution
 * @author Gary Tse - Add NetworkAddressChangeListener to handle interface changes
 */
@Component(immediate = true, service = MDNSClient.class)
public class MDNSClientImpl implements MDNSClient, NetworkAddressChangeListener {
    private final Logger logger = LoggerFactory.getLogger(MDNSClientImpl.class);

    private final Map<InetAddress, JmDNS> jmdnsInstances = new ConcurrentHashMap<>();

    private final Set<ServiceDescription> activeServices = ConcurrentHashMap.newKeySet();

    private final NetworkAddressService networkAddressService;

    @Activate
    public MDNSClientImpl(final @Reference NetworkAddressService networkAddressService) {
        this.networkAddressService = networkAddressService;
    }

    private Set<InetAddress> getAllInetAddresses() {
        final Set<InetAddress> addresses = new HashSet<>();
        Enumeration<NetworkInterface> itInterfaces;
        try {
            itInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (final SocketException e) {
            return addresses;
        }
        while (itInterfaces.hasMoreElements()) {
            final NetworkInterface iface = itInterfaces.nextElement();
            try {
                if (!iface.isUp() || iface.isLoopback() || iface.isPointToPoint()) {
                    continue;
                }
            } catch (final SocketException ex) {
                continue;
            }

            InetAddress primaryIPv4HostAddress = null;

            if (networkAddressService.isUseOnlyOneAddress()
                    && networkAddressService.getPrimaryIpv4HostAddress() != null) {
                final Enumeration<InetAddress> itAddresses = iface.getInetAddresses();
                while (itAddresses.hasMoreElements()) {
                    final InetAddress address = itAddresses.nextElement();
                    if (address.getHostAddress().equals(networkAddressService.getPrimaryIpv4HostAddress())) {
                        primaryIPv4HostAddress = address;
                        break;
                    }
                }
            }

            final Enumeration<InetAddress> itAddresses = iface.getInetAddresses();
            boolean ipv4addressAdded = false;
            boolean ipv6addressAdded = false;
            while (itAddresses.hasMoreElements()) {
                final InetAddress address = itAddresses.nextElement();
                if (address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || (!networkAddressService.isUseIPv6() && address instanceof Inet6Address)) {
                    continue;
                }
                if (networkAddressService.isUseOnlyOneAddress()) {
                    // add only one address per interface and family
                    if (address instanceof Inet4Address) {
                        if (!ipv4addressAdded) {
                            if (primaryIPv4HostAddress != null) {
                                // use configured primary address instead of first one
                                addresses.add(primaryIPv4HostAddress);
                            } else {
                                addresses.add(address);
                            }
                            ipv4addressAdded = true;
                        }
                    } else if (address instanceof Inet6Address) {
                        if (!ipv6addressAdded) {
                            addresses.add(address);
                            ipv6addressAdded = true;
                        }
                    }
                } else {
                    addresses.add(address);
                }
            }
        }
        return addresses;
    }

    @Override
    public Set<JmDNS> getClientInstances() {
        return new HashSet<>(jmdnsInstances.values());
    }

    @Activate
    protected void activate() {
        networkAddressService.addNetworkAddressChangeListener(this);
        start();
    }

    private void start() {
        for (InetAddress address : getAllInetAddresses()) {
            createJmDNSByAddress(address);
        }
        for (ServiceDescription description : activeServices) {
            try {
                registerServiceInternal(description);
            } catch (IOException e) {
                logger.warn("Exception while registering service {}", description, e);
            }
        }
    }

    @Deactivate
    public void deactivate() {
        close();
        activeServices.clear();
        networkAddressService.removeNetworkAddressChangeListener(this);
    }

    @Override
    public void addServiceListener(String type, ServiceListener listener) {
        jmdnsInstances.values().forEach(jmdns -> jmdns.addServiceListener(type, listener));
    }

    @Override
    public void removeServiceListener(String type, ServiceListener listener) {
        jmdnsInstances.values().forEach(jmdns -> jmdns.removeServiceListener(type, listener));
    }

    @Override
    public void registerService(ServiceDescription description) throws IOException {
        activeServices.add(description);
        registerServiceInternal(description);
    }

    private void registerServiceInternal(ServiceDescription description) throws IOException {
        for (JmDNS instance : jmdnsInstances.values()) {
            logger.debug("Registering new service {} at {}:{} ({})", description.serviceType,
                    instance.getInetAddress().getHostAddress(), description.servicePort, instance.getName());
            // Create one ServiceInfo object for each JmDNS instance
            ServiceInfo serviceInfo = ServiceInfo.create(description.serviceType, description.serviceName,
                    description.servicePort, 0, 0, description.serviceProperties);
            instance.registerService(serviceInfo);
        }
    }

    @Override
    public void unregisterService(ServiceDescription description) {
        activeServices.remove(description);
        for (JmDNS instance : jmdnsInstances.values()) {
            try {
                logger.debug("Unregistering service {} at {}:{} ({})", description.serviceType,
                        instance.getInetAddress().getHostAddress(), description.servicePort, instance.getName());
            } catch (IOException e) {
                logger.debug("Unregistering service {} ({})", description.serviceType, instance.getName());
            }
            ServiceInfo serviceInfo = ServiceInfo.create(description.serviceType, description.serviceName,
                    description.servicePort, 0, 0, description.serviceProperties);
            instance.unregisterService(serviceInfo);
        }
    }

    @Override
    public void unregisterAllServices() {
        activeServices.clear();
        for (JmDNS instance : jmdnsInstances.values()) {
            instance.unregisterAllServices();
        }
    }

    @Override
    public ServiceInfo[] list(String type) {
        ServiceInfo[] services = new ServiceInfo[0];
        for (JmDNS instance : jmdnsInstances.values()) {
            services = concatenate(services, instance.list(type));
        }
        return services;
    }

    @Override
    public ServiceInfo[] list(String type, Duration timeout) {
        ServiceInfo[] services = new ServiceInfo[0];
        for (JmDNS instance : jmdnsInstances.values()) {
            services = concatenate(services, instance.list(type, timeout.toMillis()));
        }
        return services;
    }

    @Override
    public void close() {
        for (JmDNS jmdns : jmdnsInstances.values()) {
            closeQuietly(jmdns);
            logger.debug("mDNS service has been stopped ({})", jmdns.getName());
        }
        jmdnsInstances.clear();
    }

    private void closeQuietly(JmDNS jmdns) {
        try {
            jmdns.close();
        } catch (IOException e) {
        }
    }

    /**
     * Concatenate two arrays of ServiceInfo
     *
     * @param a: the first array
     * @param b: the second array
     * @return an array of ServiceInfo
     */
    private ServiceInfo[] concatenate(ServiceInfo[] a, ServiceInfo[] b) {
        int aLen = a.length;
        int bLen = b.length;

        ServiceInfo[] c = new ServiceInfo[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    private void createJmDNSByAddress(InetAddress address) {
        try {
            JmDNS jmdns = JmDNS.create(address, "JmDNS-" + address.toString());
            jmdnsInstances.put(address, jmdns);
            logger.debug("mDNS service has been started ({} for IP {})", jmdns.getName(), address.getHostAddress());
        } catch (IOException e) {
            logger.debug("JmDNS instantiation failed ({})!", address.getHostAddress());
        }
    }

    @Override
    public void onChanged(List<CidrAddress> added, List<CidrAddress> removed) {
        logger.debug("ip address change: added {}, removed {}", added, removed);
        close();
        start();
    }
}
