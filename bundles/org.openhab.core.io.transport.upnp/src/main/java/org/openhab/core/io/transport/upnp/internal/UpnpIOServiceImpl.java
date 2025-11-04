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
package org.openhab.core.io.transport.upnp.internal;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.UpnpService;
import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.model.UserConstants;
import org.jupnp.model.action.ActionArgumentValue;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.Service;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.UDAServiceId;
import org.jupnp.model.types.UDN;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryListener;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.io.transport.upnp.UpnpIOParticipant;
import org.openhab.core.io.transport.upnp.UpnpIOService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UpnpIOServiceImpl} is the implementation of the UpnpIOService
 * interface
 *
 * @author Karel Goderis - Initial contribution; added simple polling mechanism
 * @author Kai Kreuzer - added descriptor url retrieval
 * @author Markus Rathgeb - added NP checks in subscription ended callback
 * @author Andre Fuechsel - added methods to remove subscriptions
 * @author Ivan Iliev - made sure resubscribe is only done when subscription ended CancelReason was EXPIRED or
 *         RENEW_FAILED
 */
@SuppressWarnings("rawtypes")
@NonNullByDefault
@Component(immediate = true)
public class UpnpIOServiceImpl implements UpnpIOService, RegistryListener {

    private final Logger logger = LoggerFactory.getLogger(UpnpIOServiceImpl.class);

    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(POOL_NAME);

    private static final int DEFAULT_POLLING_INTERVAL = 60;
    private static final String POOL_NAME = "upnp-io";

    private final UpnpService upnpService;

    final Set<UpnpIOParticipant> participants = new CopyOnWriteArraySet<>();
    final Map<UpnpIOParticipant, ScheduledFuture> pollingJobs = new ConcurrentHashMap<>();
    final Map<UpnpIOParticipant, Boolean> currentStates = new ConcurrentHashMap<>();
    final Map<Service, UpnpSubscriptionCallback> subscriptionCallbacks = new ConcurrentHashMap<>();

    public class UpnpSubscriptionCallback extends SubscriptionCallback {

        /**
         * Creates a new subscription callback for the specified service with a requested duration of
         * {@link UserConstants#DEFAULT_SUBSCRIPTION_DURATION_SECONDS}.
         *
         * @param service the {@link Service} to subscribe to.
         */
        public UpnpSubscriptionCallback(Service service) {
            super(service);
        }

        /**
         * Creates a new subscription callback for the specified service with a requested duration of
         * the specified number of seconds. The UPnP standard states that it "Should be greater than or equal
         * to 1800 seconds (30 minutes)".
         *
         * @param service the {@link Service} to subscribe to.
         * @param requestedDurationSeconds the subscription duration to request.
         */
        public UpnpSubscriptionCallback(Service service, int requestedDurationSeconds) {
            super(service, requestedDurationSeconds);
        }

        @Override
        protected void ended(@Nullable GENASubscription subscription, @Nullable CancelReason reason,
                @Nullable UpnpResponse response) {
            Service service = subscription == null ? null : subscription.getService();
            if (subscription != null && service != null) {
                if (logger.isDebugEnabled()) {
                    ServiceId serviceId = service.getServiceId();
                    Device device = service.getDevice();
                    if (device != null) {
                        DeviceIdentity identity;
                        UDN deviceUdn = (identity = device.getIdentity()) == null ? null : identity.getUdn();
                        Device deviceRoot = device.getRoot();
                        if (!device.equals(deviceRoot)) {
                            UDN deviceRootUdn = deviceRoot == null || (identity = deviceRoot.getIdentity()) == null
                                    ? null
                                    : identity.getUdn();
                            logger.debug("A GENA subscription '{}' for device '{}' ('{}') was ended", serviceId.getId(),
                                    deviceUdn, deviceRootUdn);
                        } else {
                            logger.debug("A GENA subscription '{}' for device '{}' was ended", serviceId.getId(),
                                    deviceUdn);
                        }
                    }
                }

                if ((CancelReason.EXPIRED.equals(reason) || CancelReason.RENEWAL_FAILED.equals(reason))) {
                    final ControlPoint cp = upnpService.getControlPoint();
                    if (cp != null) {
                        final UpnpSubscriptionCallback callback = new UpnpSubscriptionCallback(service,
                                subscription.getActualDurationSeconds());
                        cp.execute(callback);
                    }
                }
            }
        }

        @Override
        protected void established(@Nullable GENASubscription subscription) {
            Service service = subscription == null ? null : subscription.getService();
            if (service != null) {
                Device device = service.getDevice();
                Device deviceRoot = device == null ? null : device.getRoot();
                ServiceId serviceId = service.getServiceId();

                if (logger.isDebugEnabled() && device != null) {
                    DeviceIdentity identity;
                    UDN deviceUdn = (identity = device.getIdentity()) == null ? null : identity.getUdn();
                    if (!device.equals(deviceRoot)) {
                        UDN deviceRootUdn = deviceRoot == null || (identity = deviceRoot.getIdentity()) == null ? null
                                : identity.getUdn();
                        logger.debug("A GENA subscription '{}' for device '{}' ('{}') was established",
                                serviceId.getId(), deviceUdn, deviceRootUdn);
                    } else {
                        logger.debug("A GENA subscription '{}' for device '{}' was established", serviceId.getId(),
                                deviceUdn);
                    }
                }

                for (UpnpIOParticipant participant : participants) {
                    if (Objects.equals(getDevice(participant), deviceRoot)) {
                        try {
                            participant.onServiceSubscribed(serviceId.getId(), true);
                        } catch (Exception e) {
                            logger.error("Participant threw an exception onServiceSubscribed", e);
                        }
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void eventReceived(@Nullable GENASubscription subscription) {
            Service service = subscription == null ? null : subscription.getService();
            if (subscription != null && service != null) {
                Map<String, StateVariableValue> values = subscription.getCurrentValues();
                Device device = service.getDevice();
                Device deviceRoot = device == null ? null : device.getRoot();
                ServiceId serviceId = service.getServiceId();

                if (logger.isTraceEnabled() && device != null) {
                    DeviceIdentity identity;
                    UDN deviceUdn = (identity = device.getIdentity()) == null ? null : identity.getUdn();
                    if (!device.equals(deviceRoot)) {
                        UDN deviceRootUdn = deviceRoot == null || (identity = deviceRoot.getIdentity()) == null ? null
                                : identity.getUdn();
                        logger.trace("A GENA subscription '{}' event for device '{}' ('{}') was received",
                                serviceId.getId(), deviceUdn, deviceRootUdn);
                    } else {
                        logger.trace("A GENA subscription '{}' event for device '{}' was received", serviceId.getId(),
                                deviceUdn);
                    }
                }

                for (UpnpIOParticipant participant : participants) {
                    if (Objects.equals(getDevice(participant), deviceRoot)) {
                        for (Entry<String, StateVariableValue> entry : values.entrySet()) {
                            Object value = entry.getValue().getValue();
                            if (value != null) {
                                try {
                                    participant.onValueReceived(entry.getKey(), value.toString(), serviceId.getId());
                                } catch (Exception e) {
                                    logger.error("Participant threw an exception onValueReceived", e);
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }

        @Override
        protected void eventsMissed(@Nullable GENASubscription subscription, int numberOfMissedEvents) {
            Service service;
            if (logger.isDebugEnabled() && subscription != null && (service = subscription.getService()) != null) {
                Device device = service.getDevice();
                Device deviceRoot = device == null ? null : device.getRoot();
                if (device != null) {
                    ServiceId serviceId = service.getServiceId();
                    DeviceIdentity identity;
                    UDN deviceUdn = (identity = device.getIdentity()) == null ? null : identity.getUdn();
                    if (!device.equals(deviceRoot)) {
                        UDN deviceRootUdn = deviceRoot == null || (identity = deviceRoot.getIdentity()) == null ? null
                                : identity.getUdn();
                        logger.debug("A GENA subscription '{}' for device '{}' ('{}') missed {} events",
                                serviceId.getId(), deviceUdn, deviceRootUdn, numberOfMissedEvents);
                    } else {
                        logger.debug("A GENA subscription '{}' for device '{}' missed {} events", serviceId.getId(),
                                deviceUdn, numberOfMissedEvents);
                    }
                }
            }
        }

        @Override
        protected void failed(@Nullable GENASubscription subscription, @Nullable UpnpResponse response,
                @Nullable Exception e, @Nullable String defaultMsg) {
            Service service = subscription == null ? null : subscription.getService();
            if (subscription != null && service != null) {
                Device device = service.getDevice();
                Device deviceRoot = device == null ? null : device.getRoot();
                ServiceId serviceId = service.getServiceId();

                if (logger.isDebugEnabled() && device != null) {
                    DeviceIdentity identity;
                    UDN deviceUdn = (identity = device.getIdentity()) == null ? null : identity.getUdn();
                    if (!device.equals(deviceRoot)) {
                        UDN deviceRootUdn = deviceRoot == null || (identity = deviceRoot.getIdentity()) == null ? null
                                : identity.getUdn();
                        logger.debug("A GENA subscription '{}' for device '{}' ('{}') failed: {}", serviceId.getId(),
                                deviceUdn, deviceRootUdn, defaultMsg);
                    } else {
                        logger.debug("A GENA subscription '{}' for device '{}' failed: {}", serviceId.getId(),
                                deviceUdn, defaultMsg);
                    }
                }

                for (UpnpIOParticipant participant : participants) {
                    if (Objects.equals(getDevice(participant), deviceRoot)) {
                        try {
                            participant.onServiceSubscribed(serviceId.getId(), false);
                        } catch (Exception e2) {
                            logger.error("Participant threw an exception onServiceSubscribed", e2);
                        }
                    }
                }
            }
        }
    }

    @Activate
    public UpnpIOServiceImpl(final @Reference UpnpService upnpService) {
        this.upnpService = upnpService;
    }

    @Activate
    public void activate() {
        logger.debug("Starting UPnP IO service...");
        upnpService.getRegistry().getRemoteDevices().forEach(device -> informParticipants(device, true));
        upnpService.getRegistry().addListener(this);
    }

    @Deactivate
    public void deactivate() {
        logger.debug("Stopping UPnP IO service...");
        upnpService.getRegistry().removeListener(this);
    }

    @Nullable
    private Device getDevice(UpnpIOParticipant participant) {
        String participantUdn = participant.getUDN();
        if ("undefined".equals(participantUdn)) {
            return null;
        }
        Registry registry = upnpService.getRegistry();
        return registry == null ? null : registry.getDevice(new UDN(participantUdn), true);
    }

    @Override
    public void addSubscription(UpnpIOParticipant participant, String serviceID) {
        addSubscription(participant, serviceID, UserConstants.DEFAULT_SUBSCRIPTION_DURATION_SECONDS);
    }

    @Override
    public void addSubscription(UpnpIOParticipant participant, String serviceID, int requestedDurationSeconds) {
        registerParticipant(participant);
        Device device = getDevice(participant);
        if (device != null) {
            Service subService = searchSubService(serviceID, device);
            if (subService != null) {
                logger.trace("Setting up an UPNP service subscription '{}' for particpant '{}'", serviceID,
                        participant.getUDN());

                UpnpSubscriptionCallback callback = new UpnpSubscriptionCallback(subService, requestedDurationSeconds);
                subscriptionCallbacks.put(subService, callback);
                upnpService.getControlPoint().execute(callback);
            } else {
                logger.trace("Could not find service '{}' for device '{}'", serviceID, device.getIdentity().getUdn());
            }
        } else {
            logger.trace("Could not find an upnp device for participant '{}'", participant.getUDN());
        }
    }

    @Nullable
    private Service searchSubService(String serviceID, Device device) {
        Service subService = findService(device, null, serviceID);
        if (subService == null) {
            // service not on the root device, we search the embedded devices as well
            Device[] embedded = device.getEmbeddedDevices();
            if (embedded != null) {
                for (Device aDevice : embedded) {
                    subService = findService(aDevice, null, serviceID);
                    if (subService != null) {
                        break;
                    }
                }
            }
        }
        return subService;
    }

    @Override
    public void removeSubscription(UpnpIOParticipant participant, String serviceID) {
        Device device = getDevice(participant);
        if (device != null) {
            Service subService = searchSubService(serviceID, device);
            if (subService != null) {
                logger.trace("Removing an UPNP service subscription '{}' for particpant '{}'", serviceID,
                        participant.getUDN());

                UpnpSubscriptionCallback callback = subscriptionCallbacks.remove(subService);
                if (callback != null) {
                    callback.end();
                }
            } else {
                logger.trace("Could not find service '{}' for device '{}'", serviceID, device.getIdentity().getUdn());
            }
        } else {
            logger.trace("Could not find an upnp device for participant '{}'", participant.getUDN());
        }
    }

    @Override
    public Map<String, @Nullable String> invokeAction(UpnpIOParticipant participant, String serviceID, String actionID,
            @Nullable Map<String, String> inputs) {
        return invokeAction(participant, null, serviceID, actionID, inputs);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, @Nullable String> invokeAction(UpnpIOParticipant participant, @Nullable String namespace,
            String serviceID, String actionID, @Nullable Map<String, String> inputs) {
        Map<String, @Nullable String> resultMap = new HashMap<>();

        registerParticipant(participant);
        Device device = getDevice(participant);

        if (device != null) {
            Service service = findService(device, namespace, serviceID);
            if (service != null) {
                Action action = service.getAction(actionID);
                if (action != null) {
                    ActionInvocation invocation = new ActionInvocation(action);
                    if (inputs != null) {
                        for (Entry<String, String> entry : inputs.entrySet()) {
                            invocation.setInput(entry.getKey(), entry.getValue());
                        }
                    }

                    logger.trace("Invoking Action '{}' of service '{}' for participant '{}'", actionID, serviceID,
                            participant.getUDN());
                    new ActionCallback.Default(invocation, upnpService.getControlPoint()).run();

                    ActionException anException = invocation.getFailure();
                    if (anException != null && anException.getMessage() != null) {
                        logger.debug("{}", anException.getMessage());
                    }

                    Map<String, ActionArgumentValue> result = invocation.getOutputMap();
                    if (result != null) {
                        for (Entry<String, ActionArgumentValue> entry : result.entrySet()) {
                            String variable = entry.getKey();
                            final ActionArgumentValue newArgument;
                            try {
                                newArgument = entry.getValue();
                            } catch (final Exception ex) {
                                logger.debug("An exception '{}' occurred, cannot get argument for variable '{}'",
                                        ex.getMessage(), variable);
                                continue;
                            }
                            try {
                                if (newArgument.getValue() != null) {
                                    resultMap.put(variable, newArgument.getValue().toString());
                                }
                            } catch (final Exception ex) {
                                logger.debug(
                                        "An exception '{}' occurred processing ActionArgumentValue '{}' with value '{}'",
                                        ex.getMessage(), newArgument.getArgument().getName(), newArgument.getValue());
                            }
                        }
                    }
                } else {
                    logger.debug("Could not find action '{}' for participant '{}'", actionID, participant.getUDN());
                }
            } else {
                logger.debug("Could not find service '{}' for participant '{}'", serviceID, participant.getUDN());
            }
        } else {
            logger.debug("Could not find an upnp device for participant '{}'", participant.getUDN());
        }
        return resultMap;
    }

    @Override
    public boolean isRegistered(UpnpIOParticipant participant) {
        return upnpService.getRegistry().getDevice(new UDN(participant.getUDN()), true) != null;
    }

    @Override
    public void registerParticipant(UpnpIOParticipant participant) {
        participants.add(participant);
    }

    @Override
    public void unregisterParticipant(UpnpIOParticipant participant) {
        stopPollingForParticipant(participant);
        pollingJobs.remove(participant);
        currentStates.remove(participant);
        participants.remove(participant);
    }

    @Override
    @Nullable
    public URL getDescriptorURL(UpnpIOParticipant participant) {
        RemoteDevice device = upnpService.getRegistry().getRemoteDevice(new UDN(participant.getUDN()), true);
        if (device != null) {
            return device.getIdentity().getDescriptorURL();
        } else {
            return null;
        }
    }

    @Nullable
    private Service findService(Device device, @Nullable String namespace, String serviceID) {
        Service service;
        String ns = namespace == null ? device.getType().getNamespace() : namespace;
        if (UDAServiceId.DEFAULT_NAMESPACE.equals(ns) || UDAServiceId.BROKEN_DEFAULT_NAMESPACE.equals(ns)) {
            service = device.findService(new UDAServiceId(serviceID));
        } else {
            service = device.findService(new ServiceId(ns, serviceID));
        }
        return service;
    }

    /**
     * Propagates a device status change to all participants
     *
     * @param device the device that has changed its status
     * @param status true, if device is reachable, false otherwise
     */
    private void informParticipants(RemoteDevice device, boolean status) {
        for (UpnpIOParticipant participant : participants) {
            if (participant.getUDN().equals(device.getIdentity().getUdn().getIdentifierString())) {
                setDeviceStatus(participant, status);
            }
        }
    }

    private void setDeviceStatus(UpnpIOParticipant participant, boolean newStatus) {
        if (!Objects.equals(currentStates.get(participant), newStatus)) {
            currentStates.put(participant, newStatus);
            logger.debug("Device '{}' reachability status changed to '{}'", participant.getUDN(), newStatus);
            participant.onStatusChanged(newStatus);
        }
    }

    private class UPNPPollingRunnable implements Runnable {

        private final UpnpIOParticipant participant;
        private final String serviceID;
        private final String actionID;

        public UPNPPollingRunnable(UpnpIOParticipant participant, String serviceID, String actionID) {
            this.participant = participant;
            this.serviceID = serviceID;
            this.actionID = actionID;
        }

        @Override
        public void run() {
            // It is assumed that during addStatusListener() a check is made whether the participant is correctly
            // registered
            try {
                Device device = getDevice(participant);
                String participantUdn = participant.getUDN();
                if (device != null) {
                    Service service = findService(device, null, serviceID);
                    if (service != null) {
                        Action action = service.getAction(actionID);
                        if (action != null) {
                            @SuppressWarnings("unchecked")
                            ActionInvocation invocation = new ActionInvocation(action);
                            logger.debug("Polling participant '{}' through Action '{}' of Service '{}' ",
                                    participantUdn, actionID, serviceID);
                            new ActionCallback.Default(invocation, upnpService.getControlPoint()).run();

                            ActionException anException = invocation.getFailure();
                            String message;
                            if (anException != null && (message = anException.getMessage()) != null
                                    && message.contains("Connection error or no response received")) {
                                // The UDN is not reachable anymore
                                setDeviceStatus(participant, false);
                            } else {
                                // The UDN functions correctly
                                setDeviceStatus(participant, true);
                            }
                        } else {
                            logger.debug("Could not find action '{}' for participant '{}'", actionID, participantUdn);
                        }
                    } else {
                        logger.debug("Could not find service '{}' for participant '{}'", serviceID, participantUdn);
                    }
                }
            } catch (Exception e) {
                logger.error("An exception occurred while polling an UPnP device: '{}'", e.getMessage(), e);
            }
        }
    }

    @Override
    public void addStatusListener(UpnpIOParticipant participant, String serviceID, String actionID, int interval) {
        registerParticipant(participant);

        int pollingInterval = interval == 0 ? DEFAULT_POLLING_INTERVAL : interval;

        // remove the previous polling job, if any
        stopPollingForParticipant(participant);

        currentStates.put(participant, true);

        Runnable pollingRunnable = new UPNPPollingRunnable(participant, serviceID, actionID);
        pollingJobs.put(participant,
                scheduler.scheduleWithFixedDelay(pollingRunnable, 0, pollingInterval, TimeUnit.SECONDS));
    }

    private void stopPollingForParticipant(UpnpIOParticipant participant) {
        if (pollingJobs.containsKey(participant)) {
            ScheduledFuture<?> pollingJob = pollingJobs.get(participant);
            if (pollingJob != null) {
                pollingJob.cancel(true);
            }
        }
    }

    @Override
    public void removeStatusListener(UpnpIOParticipant participant) {
        unregisterParticipant(participant);
    }

    @Override
    public void remoteDeviceAdded(@Nullable Registry registry, @Nullable RemoteDevice device) {
        if (device != null) {
            informParticipants(device, true);
        }
    }

    @Override
    public void remoteDeviceUpdated(@Nullable Registry registry, @Nullable RemoteDevice device) {
    }

    @Override
    public void remoteDeviceRemoved(@Nullable Registry registry, @Nullable RemoteDevice device) {
        if (device != null) {
            informParticipants(device, false);
        }
    }

    @Override
    public void remoteDeviceDiscoveryStarted(@Nullable Registry registry, @Nullable RemoteDevice device) {
    }

    @Override
    public void remoteDeviceDiscoveryFailed(@Nullable Registry registry, @Nullable RemoteDevice device,
            @Nullable Exception ex) {
    }

    @Override
    public void localDeviceAdded(@Nullable Registry registry, @Nullable LocalDevice device) {
    }

    @Override
    public void localDeviceRemoved(@Nullable Registry registry, @Nullable LocalDevice device) {
    }

    @Override
    public void beforeShutdown(@Nullable Registry registry) {
    }

    @Override
    public void afterShutdown() {
    }
}
