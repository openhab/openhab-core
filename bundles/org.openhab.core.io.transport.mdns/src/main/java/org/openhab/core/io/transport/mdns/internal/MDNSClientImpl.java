/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.common.ThreadPoolManager;
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
 * @author Ravi Nadahar - Refactor to be thread-safe
 */
@Component(immediate = true, service = MDNSClient.class)
public class MDNSClientImpl implements MDNSClient, NetworkAddressChangeListener {
    public static final String MDNS_POOL_NAME = "mDNS";

    private final Logger logger = LoggerFactory.getLogger(MDNSClientImpl.class);

    // All access must be guarded by "this"
    private final Map<InetAddress, JmDNS> jmdnsInstances = new LinkedHashMap<>();

    // All access must be guarded by "this"
    private final Set<ServiceDescription> activeServices = new LinkedHashSet<>();

    private final NetworkAddressService networkAddressService;

    private final ExecutorService executor = ThreadPoolManager.getPool(MDNS_POOL_NAME);

    @Activate
    public MDNSClientImpl(final @Reference NetworkAddressService networkAddressService) {
        this.networkAddressService = networkAddressService;
        networkAddressService.addNetworkAddressChangeListener(this);

        // Even though each JmDNS instance is created using the executor, testing shows that getAllInetAddresses()
        // itself can be slow, so there's no reason why the bundle activation should have to wait for that.
        executor.execute(() -> {
            for (InetAddress address : getAllInetAddresses()) {
                createJmDNSByAddress(address);
            }
        });
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
    public synchronized Set<JmDNS> getClientInstances() {
        return Set.copyOf(jmdnsInstances.values());
    }

    @Deactivate
    public void deactivate() {
        networkAddressService.removeNetworkAddressChangeListener(this);
        synchronized (this) {
            close();
            activeServices.clear();
        }
    }

    @Override
    public synchronized void addServiceListener(String type, ServiceListener listener) {
        jmdnsInstances.values().forEach(jmdns -> jmdns.addServiceListener(type, listener));
    }

    @Override
    public synchronized void removeServiceListener(String type, ServiceListener listener) {
        jmdnsInstances.values().forEach(jmdns -> jmdns.removeServiceListener(type, listener));
    }

    @Override
    public void registerService(ServiceDescription description) {
        executor.execute(() -> {
            List<JmDNS> instances = null;
            synchronized (MDNSClientImpl.this) {
                if (activeServices.add(description)) {
                    instances = List.copyOf(jmdnsInstances.values());
                }
            }
            if (instances != null) {
                for (JmDNS instance : instances) {
                    registerServiceInstance(instance, description);
                }
            }
        });
    }

    private void registerServiceInstance(JmDNS instance, ServiceDescription description) {
        if (logger.isDebugEnabled()) {
            try {
                logger.debug("mDNS: Registering new service {} at {}:{} ({})", description.serviceType,
                        instance.getInetAddress().getHostAddress(), description.servicePort, instance.getName());
            } catch (IOException e) {
                logger.warn("mDNS: Failed to acquire IP address while trying to register new service {} ({}): {}",
                        description.serviceType, instance.getName(), e.getMessage());
                logger.trace("", e);
            }
        }

        // Create one ServiceInfo object for the JmDNS instance
        ServiceInfo serviceInfo = ServiceInfo.create(description.serviceType, description.serviceName,
                description.servicePort, 0, 0, description.serviceProperties);
        try {
            instance.registerService(serviceInfo);
        } catch (IOException e) {
            logger.warn("mDNS: Failed to register service info for {} {} ({}): {}", description.serviceType,
                    description.serviceName, instance.getName(), e.getMessage());
            logger.trace("", e);
        }
    }

    @Override
    public void unregisterService(ServiceDescription description) {
        List<JmDNS> instances = null;
        synchronized (this) {
            if (activeServices.remove(description)) {
                instances = List.copyOf(jmdnsInstances.values());
            }
        }
        if (instances != null) {
            for (JmDNS instance : instances) {
                unregisterServiceInstance(instance, description);
            }
        }
    }

    private void unregisterServiceInstance(JmDNS instance, ServiceDescription description) {
        if (logger.isDebugEnabled()) {
            try {
                logger.debug("mDNS: Unregistering service {} at {}:{} ({})", description.serviceType,
                        instance.getInetAddress().getHostAddress(), description.servicePort, instance.getName());
            } catch (IOException e) {
                logger.debug("mDNS: Unregistering service {} ({})", description.serviceType, instance.getName());
            }
        }
        ServiceInfo serviceInfo = ServiceInfo.create(description.serviceType, description.serviceName,
                description.servicePort, 0, 0, description.serviceProperties);
        instance.unregisterService(serviceInfo);
    }

    @Override
    public synchronized void unregisterAllServices() {
        activeServices.clear();
        for (JmDNS instance : jmdnsInstances.values()) {
            instance.unregisterAllServices();
        }
    }

    @Override
    public ServiceInfo[] list(String type) {
        ServiceInfo[] services = new ServiceInfo[0];
        synchronized (this) {
            for (JmDNS instance : jmdnsInstances.values()) {
                services = concatenate(services, instance.list(type));
            }
        }
        return services;
    }

    @Override
    public ServiceInfo[] list(String type, Duration timeout) {
        ServiceInfo[] services = new ServiceInfo[0];
        synchronized (this) {
            for (JmDNS instance : jmdnsInstances.values()) {
                services = concatenate(services, instance.list(type, timeout.toMillis()));
            }
        }
        return services;
    }

    @Override
    public synchronized void close() {
        for (JmDNS jmdns : jmdnsInstances.values()) {
            closeQuietly(jmdns);
            logger.debug("mDNS: Services have been stopped ({})", jmdns.getName());
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
        executor.execute(() -> {
            try {
                JmDNS jmdns = JmDNS.create(address, null);
                JmDNS oldJmdns;
                Set<ServiceDescription> services;
                synchronized (MDNSClientImpl.this) {
                    oldJmdns = jmdnsInstances.put(address, jmdns);
                    services = Set.copyOf(activeServices);
                }
                // Prevent multiple instances for an address from existing
                if (oldJmdns != null) {
                    for (ServiceDescription description : services) {
                        unregisterServiceInstance(oldJmdns, description);
                    }
                    closeQuietly(oldJmdns);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("mDNS: Services has been started ({} for IP {})", jmdns.getName(),
                            address.getHostAddress());
                }
                for (ServiceDescription description : services) {
                    registerServiceInstance(jmdns, description);
                }

            } catch (IOException e) {
                logger.debug("mDNS: JmDNS instantiation failed ({})!", address.getHostAddress());
            }
        });
    }

    @Override
    public void onChanged(List<CidrAddress> added, List<CidrAddress> removed) {
        logger.debug("mDNS: IP address change: added {}, removed {}", added, removed);

        Set<InetAddress> filteredAddresses = getAllInetAddresses();

        synchronized (this) {
            Entry<InetAddress, JmDNS> entry;
            InetAddress addr;
            JmDNS inst;
            for (Iterator<@NonNull Entry<InetAddress, JmDNS>> iterator = jmdnsInstances.entrySet().iterator(); iterator
                    .hasNext();) {
                entry = iterator.next();
                addr = entry.getKey();
                inst = entry.getValue();
                if (!filteredAddresses.contains(addr)) {
                    // IP address no longer in use, unregister
                    logger.debug("mDNS: Stopping services for removed IP address '{}'", addr.getHostAddress());
                    for (ServiceDescription description : activeServices) {
                        unregisterServiceInstance(inst, description);
                    }
                    closeQuietly(inst);
                    logger.debug("mDNS: Services has been stopped ({} for IP {})", inst.getName(),
                            addr.getHostAddress());
                    iterator.remove();
                } else {
                    // The IP was and still is in use, leave it alone
                    filteredAddresses.remove(addr);
                }
            }
        }

        // Any remaining addresses in filteredAddresses at this point isn't registered, so let's register them
        for (InetAddress addr : filteredAddresses) {
            logger.debug("mDNS: Starting services for new IP address '{}'", addr.getHostAddress());
            createJmDNSByAddress(addr);
        }
    }
}
