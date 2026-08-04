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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.io.transport.mdns.MDNSClient;
import org.openhab.core.io.transport.mdns.MDNSService;
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
 * @author Ravi Nadahar - Refactor to be thread-safe and to implement {@link MDNSService}
 */
@Component(immediate = true, service = { MDNSClient.class, MDNSService.class })
public class MDNSClientImpl implements MDNSClient, MDNSService, NetworkAddressChangeListener {
    public static final String MDNS_POOL_NAME = "mDNS";

    private final Logger logger = LoggerFactory.getLogger(MDNSClientImpl.class);

    // All access must be guarded by "this"
    private final Map<InetAddress, JmDNS> jmdnsInstances = new LinkedHashMap<>();

    // All access must be guarded by "this"
    private final Set<ServiceDescription> activeServices = new LinkedHashSet<>();

    // All access must be guarded by "this"
    private final Set<ServiceListenerRegistration> listeners = new HashSet<>();

    // All access must be guarded by "this"
    private boolean deactivated;

    // All access must be guarded by "addressQueue"
    final Queue<QueueTaskHandler<@Nullable Void, InetAddress>> addressQueue = new LinkedList<>();

    // All access must be guarded by "serviceQueue"
    final Queue<QueueTaskHandler<@Nullable Void, ServiceDescription>> serviceQueue = new LinkedList<>();

    private final NetworkAddressService networkAddressService;

    private final ExecutorService executor = ThreadPoolManager.getPool(MDNS_POOL_NAME);

    @Activate
    public MDNSClientImpl(final @Reference NetworkAddressService networkAddressService) {
        this.networkAddressService = networkAddressService;
        networkAddressService.addNetworkAddressChangeListener(this);
        logger.debug("mDNS: Starting services");
        updateNetworkAddresses();
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
        logger.debug("mDNS: Stopping services");
        synchronized (this) {
            deactivated = true;
            activeServices.clear();
            for (Entry<InetAddress, JmDNS> entry : jmdnsInstances.entrySet()) {
                closeQuietly(entry.getValue());
                logger.debug("mDNS: Services for {} have been stopped ({})", entry.getKey().getHostAddress(),
                        entry.getValue().getName());
            }
            jmdnsInstances.clear();

        }
        logger.debug("mDNS: All services have been stopped");
    }

    @Override
    public synchronized void addServiceListener(String type, ServiceListener listener) {
        listeners.add(new ServiceListenerRegistration(type, listener));
        jmdnsInstances.values().forEach(jmdns -> jmdns.addServiceListener(type, listener));
    }

    @Override
    public synchronized void removeServiceListener(String type, ServiceListener listener) {
        listeners.remove(new ServiceListenerRegistration(type, listener));
        jmdnsInstances.values().forEach(jmdns -> jmdns.removeServiceListener(type, listener));
    }

    @Override
    public void registerService(ServiceDescription description) {
        submitToQueue(serviceQueue, description, TaskAction.REGISTER, new RegisterService(description));
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

        // Create a ServiceInfo object for this JmDNS instance
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
        submitToQueue(serviceQueue, description, TaskAction.UNREGISTER, new UnregisterService(description));
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
    public ServiceInfo[] list(String type) {
        List<Future<ServiceInfo[]>> futures;
        synchronized (this) {
            futures = jmdnsInstances.values().stream().map(inst -> executor.submit(() -> {
                return inst.list(type);
            })).toList();
        }

        ServiceInfo[] services = new ServiceInfo[0];
        for (Future<ServiceInfo[]> future : futures) {
            try {
                services = concatenate(services, future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("mDNS: Interrupted while gathering service info, aborting");
                return services;
            } catch (ExecutionException e) {
                logger.debug("mDNS: An error occurred while gathering service info, skipping network: {}",
                        e.getCause() instanceof Throwable cause ? cause.getMessage() : e.getMessage());
                logger.trace("", e);
            }
        }
        return services;
    }

    @Override
    public ServiceInfo[] list(String type, Duration timeout) {
        List<Future<ServiceInfo[]>> futures;
        synchronized (this) {
            futures = jmdnsInstances.values().stream().map(inst -> executor.submit(() -> {
                return inst.list(type, timeout.toMillis());
            })).toList();
        }

        ServiceInfo[] services = new ServiceInfo[0];
        for (Future<ServiceInfo[]> future : futures) {
            try {
                services = concatenate(services, future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("mDNS: Interrupted while gathering service info, aborting");
                return services;
            } catch (ExecutionException e) {
                logger.debug("mDNS: An error occurred while gathering service info, skipping network: {}",
                        e.getCause() instanceof Throwable cause ? cause.getMessage() : e.getMessage());
                logger.trace("", e);
            }
        }
        return services;
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

    @Override
    public void onChanged(List<CidrAddress> added, List<CidrAddress> removed) {
        logger.debug("mDNS: IP address change: added {}, removed {}", added, removed);
        updateNetworkAddresses();
    }

    private void updateNetworkAddresses() {
        Set<InetAddress> add = getAllInetAddresses();
        List<InetAddress> remove = new ArrayList<>();
        synchronized (this) {
            for (InetAddress addr : jmdnsInstances.keySet()) {
                if (!add.contains(addr)) {
                    // IP address no longer in use, unregister
                    remove.add(addr);
                } else {
                    // The IP was and still is in use, leave it alone
                    add.remove(addr);
                }
            }
        }

        for (InetAddress addr : remove) {
            submitToQueue(addressQueue, addr, TaskAction.UNREGISTER, new DisposeJmDNSTask(addr));
        }

        // Any remaining addresses at this point isn't registered, so let's register them
        for (InetAddress addr : add) {
            submitToQueue(addressQueue, addr, TaskAction.REGISTER, new CreateJmDNSTask(addr));
        }
    }

    <@Nullable E, I> void submitToQueue(Queue<QueueTaskHandler<E, I>> queue, I identifier, @Nullable TaskAction action,
            Callable<E> task) {
        QueueTaskHandler<E, I> taskHandler = new QueueTaskHandler<>(queue, identifier, action, task);

        synchronized (queue) {
            boolean canceled = false;
            boolean isInQueue = false;

            QueueTaskHandler<E, I> queuedTask;
            for (Iterator<QueueTaskHandler<E, I>> iterator = queue.iterator(); !canceled && iterator.hasNext();) {
                queuedTask = iterator.next();
                if (identifier.equals(queuedTask.getIdentifier())) {
                    if (queuedTask.isActive()) {
                        isInQueue = true;
                    } else if (!canceled && action != null && action.isOppositeOf(queuedTask.getAction())) {
                        iterator.remove();
                        canceled = true;
                    }
                }
            }

            if (canceled) {
                logger.debug("mDNS: {} canceled out opposite task for {}", action, identifier);
                return;
            }

            queue.offer(taskHandler);

            // If identifier not in the queue, execute directly
            if (!isInQueue) {
                taskHandler.active = true;
                executor.submit(taskHandler);
            }
        }
    }

    private record ServiceListenerRegistration(String type, ServiceListener listener) {
    }

    private class RegisterService implements Callable<Void> {

        private final @NonNull ServiceDescription serviceDescription;

        public RegisterService(@NonNull ServiceDescription serviceDescription) {
            this.serviceDescription = serviceDescription;
        }

        @Override
        public Void call() throws Exception {
            List<JmDNS> instances = null;
            synchronized (MDNSClientImpl.this) {
                if (!deactivated && activeServices.add(serviceDescription)) {
                    instances = List.copyOf(jmdnsInstances.values());
                }
            }
            if (instances != null) {
                for (JmDNS instance : instances) {
                    registerServiceInstance(instance, serviceDescription);
                }
            }
            return null;
        }
    }

    private class UnregisterService implements Callable<Void> {

        private final @NonNull ServiceDescription serviceDescription;

        public UnregisterService(@NonNull ServiceDescription serviceDescription) {
            this.serviceDescription = serviceDescription;
        }

        @Override
        public Void call() throws Exception {
            List<JmDNS> instances = null;
            synchronized (MDNSClientImpl.this) {
                if (activeServices.remove(serviceDescription)) {
                    instances = List.copyOf(jmdnsInstances.values());
                }
            }
            if (instances != null) {
                for (JmDNS instance : instances) {
                    unregisterServiceInstance(instance, serviceDescription);
                }
            }
            return null;
        }
    }

    private class CreateJmDNSTask implements Callable<Void> {

        private final @NonNull InetAddress address;

        public CreateJmDNSTask(@NonNull InetAddress address) {
            this.address = address;
        }

        @Override
        public Void call() throws Exception {
            try {
                logger.debug("mDNS: Starting services for IP address '{}'", address.getHostAddress());
                JmDNS jmdns = JmDNS.create(address, null);
                JmDNS oldJmdns;
                Set<ServiceDescription> services;
                boolean deactivated;
                synchronized (MDNSClientImpl.this) {
                    deactivated = MDNSClientImpl.this.deactivated;
                    if (deactivated) {
                        oldJmdns = jmdns;
                        services = Set.of();
                    } else {
                        oldJmdns = jmdnsInstances.put(address, jmdns);
                        services = Set.copyOf(activeServices);
                        for (ServiceListenerRegistration listener : listeners) {
                            jmdns.addServiceListener(listener.type, listener.listener);
                        }
                        for (ServiceDescription description : activeServices) {
                            registerServiceInstance(jmdns, description);
                        }
                    }
                }
                // Prevent multiple instances for an address from existing
                if (oldJmdns != null) {
                    for (ServiceDescription description : services) {
                        unregisterServiceInstance(oldJmdns, description);
                    }
                    closeQuietly(oldJmdns);
                }
                if (logger.isDebugEnabled()) {
                    if (deactivated) {
                        logger.debug("mDNS: Not starting services for {} because {} has been deactivated",
                                address.getHostAddress(), getClass().getSimpleName());
                    } else {
                        logger.debug("mDNS: Services have been started ({} for IP {})", jmdns.getName(),
                                address.getHostAddress());
                    }
                }
                return null;
            } catch (IOException e) {
                logger.debug("mDNS: JmDNS instantiation failed ({})!", address.getHostAddress());
                throw e;
            }
        }
    }

    private class DisposeJmDNSTask implements Callable<Void> {

        private final @NonNull InetAddress address;

        public DisposeJmDNSTask(@NonNull InetAddress address) {
            this.address = address;
        }

        @Override
        public Void call() throws Exception {
            JmDNS inst;
            synchronized (MDNSClientImpl.this) {
                inst = jmdnsInstances.remove(address);
                if (inst != null) {
                    logger.debug("mDNS: Stopping services for removed IP address '{}'", address.getHostAddress());
                    closeQuietly(inst);
                    logger.debug("mDNS: Services have been stopped ({} for IP {})", inst.getName(),
                            address.getHostAddress());
                } else {
                    logger.debug(
                            "mDNS: Trying to stop services for removed IP address '{}', but the instance wasn't found",
                            address.getHostAddress());
                }
            }

            return null;
        }
    }

    class QueueTaskHandler<@Nullable E, I> implements Callable<E> {

        private final Queue<QueueTaskHandler<E, I>> queue;
        private final Callable<E> task;
        private final I identifier;
        private final @Nullable TaskAction action;

        // All access must be guarded by "queue"
        private boolean active;

        public QueueTaskHandler(Queue<QueueTaskHandler<E, I>> queue, I identifier, @Nullable TaskAction action,
                Callable<E> task) {
            this.queue = queue;
            this.task = task;
            this.identifier = identifier;
            this.action = action;
        }

        public I getIdentifier() {
            return identifier;
        }

        public TaskAction getAction() {
            return action;
        }

        // All access must be guarded by "queue"
        public boolean isActive() {
            return active;
        }

        @Override
        public @Nullable E call() throws Exception {
            try {
                return task.call();
            } catch (Exception e) {
                logger.debug("mDNS: Queued task {} failed for identifier {}: {}", action, identifier, e.getMessage());
                logger.trace("", e);
                throw e;
            } finally {
                synchronized (queue) {
                    queue.remove(this);

                    QueueTaskHandler<E, I> nextTask = null;
                    QueueTaskHandler<E, I> queuedTask;
                    for (Iterator<QueueTaskHandler<E, I>> iterator = queue.iterator(); iterator.hasNext();) {
                        queuedTask = iterator.next();
                        if (queuedTask.identifier.equals(this.identifier)) {
                            nextTask = queuedTask;
                            break;
                        }
                    }

                    if (nextTask != null) {
                        nextTask.active = true;
                        executor.submit(nextTask);
                    }
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
            if (identifier != null) {
                builder.append("identifier=").append(identifier).append(", ");
            }
            if (action != null) {
                builder.append("action=").append(action).append(", ");
            }
            builder.append("active=").append(active).append("]");
            return builder.toString();
        }
    }

    static enum TaskAction {
        REGISTER,
        UNREGISTER;

        public boolean isOppositeOf(@Nullable TaskAction other) {
            return (this == REGISTER && other == UNREGISTER) || (this == UNREGISTER && other == REGISTER);
        }
    }
}
