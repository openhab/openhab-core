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

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.WeakHashMap;
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
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.Service;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.model.types.ErrorCode;
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

    private final ScheduledExecutorService scheduler;

    private static final int DEFAULT_POLLING_INTERVAL = 60;
    private static final String POOL_NAME = "upnp-io";
    private static final long DEFAULT_CACHED_EVENT_DELAY = 200L;

    // Threadsafe
    private final UpnpService upnpService;

    // All access must be guarded by "this"
    final Map<UpnpIOParticipant, ParticipantData> participants = new WeakHashMap<>();

    // All access must be guarded by "events"
    private final Map<DeviceIdentity, CachedDeviceEvent> events = new HashMap<>();

    private final long cachedEventDelay;

    public class UpnpSubscriptionCallback extends SubscriptionCallback {

        private final WeakReference<UpnpIOParticipant> participant;

        /**
         * Creates a new subscription callback for the specified service with a requested duration of
         * {@link UserConstants#DEFAULT_SUBSCRIPTION_DURATION_SECONDS}.
         *
         * @param participant the {@link UpnpIOParticipant} that is the subscriber.
         * @param service the {@link Service} to subscribe to.
         */
        public UpnpSubscriptionCallback(UpnpIOParticipant participant, Service service) {
            super(service);
            this.participant = new WeakReference<UpnpIOParticipant>(participant);
        }

        /**
         * Creates a new subscription callback for the specified service with a requested duration of
         * the specified number of seconds. The UPnP standard states that it "Should be greater than or equal
         * to 1800 seconds (30 minutes)".
         *
         * @param participant the {@link UpnpIOParticipant} that is the subscriber.
         * @param service the {@link Service} to subscribe to.
         * @param requestedDurationSeconds the subscription duration to request.
         */
        public UpnpSubscriptionCallback(UpnpIOParticipant participant, Service service, int requestedDurationSeconds) {
            super(service, requestedDurationSeconds);
            this.participant = new WeakReference<UpnpIOParticipant>(participant);
        }

        @SuppressWarnings("null")
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

                UpnpSubscriptionCallback callback = null;
                UpnpIOParticipant participant = this.participant.get();
                if ((CancelReason.EXPIRED.equals(reason) || CancelReason.RENEWAL_FAILED.equals(reason))) {
                    final ControlPoint cp = upnpService.getControlPoint();
                    if (cp != null && participant != null) {
                        callback = new UpnpSubscriptionCallback(participant, service,
                                subscription.getActualDurationSeconds());
                        cp.execute(callback);
                    }
                }
                ParticipantData data = participant == null ? null : getData(participant);
                if (data != null) {
                    if (callback == null) {
                        data.removeCallback(getService().getServiceId());
                    } else {
                        data.addCallback((RemoteService) getService(), callback, false);
                    }
                }
            }
        }

        @SuppressWarnings({ "unused", "null" })
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

                UpnpIOParticipant participant = this.participant.get();
                if (participant != null) {
                    try {
                        participant.onServiceSubscribed(serviceId.getId(), true);
                    } catch (Exception e) {
                        logger.error("Participant threw an exception during onServiceSubscribed()", e);
                    }
                } else {
                    logger.warn("A '{}' GENA subscription was established for a non-existing participant",
                            serviceId.getId());
                }
            }
        }

        @SuppressWarnings({ "unchecked", "unused", "null" })
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

                UpnpIOParticipant participant = this.participant.get();
                if (participant != null) {
                    for (Entry<String, StateVariableValue> entry : values.entrySet()) {
                        Object value = entry.getValue().getValue();
                        if (value != null) {
                            try {
                                participant.onValueReceived(entry.getKey(), value.toString(), serviceId.getId());
                            } catch (Exception e) {
                                logger.error("Participant threw an exception onValueReceived()", e);
                            }
                        }
                    }
                } else {
                    logger.warn("A '{}' GENA event was received for a non-existing participant: {}", serviceId.getId(),
                            values);
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

        @SuppressWarnings({ "unused", "null" })
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

                UpnpIOParticipant participant = this.participant.get();
                if (participant != null) {
                    ParticipantData data = getData(participant);
                    if (data != null) {
                        data.removeCallback(serviceId);
                    }
                    try {
                        participant.onServiceSubscribed(serviceId.getId(), false);
                    } catch (Exception pe) {
                        logger.error("Participant threw an exception during onServiceSubscribed()", pe);
                    }
                } else {
                    logger.warn("A '{}' GENA subscription failed for a non-existing participant", serviceId.getId());
                }
            }
        }
    }

    @Activate
    public UpnpIOServiceImpl(final @Reference UpnpService upnpService) {
        this.upnpService = upnpService;
        this.cachedEventDelay = DEFAULT_CACHED_EVENT_DELAY;
        this.scheduler = ThreadPoolManager.getScheduledPool(POOL_NAME);
    }

    /**
     * Only to be used by tests.
     */
    UpnpIOServiceImpl(UpnpService upnpService, ScheduledExecutorService scheduler, long delay) {
        this.upnpService = upnpService;
        this.cachedEventDelay = delay;
        this.scheduler = scheduler;
    }

    @Activate
    public void activate() {
        logger.debug("Starting UPnP IO service...");
        upnpService.getRegistry().getRemoteDevices().forEach(device -> informParticipants(device, true, true));
        upnpService.getRegistry().addListener(this);
    }

    @Deactivate
    public void deactivate() {
        logger.debug("Stopping UPnP IO service...");
        upnpService.getRegistry().removeListener(this);
        synchronized (this) {
            for (ParticipantData data : participants.values()) {
                data.dispose();
            }
            participants.clear();
        }
    }

    @Nullable
    private RemoteDevice getDevice(UpnpIOParticipant participant) {
        String participantUdn = participant.getUDN();
        if ("undefined".equals(participantUdn)) {
            return null;
        }
        Registry registry = upnpService.getRegistry();
        return registry == null ? null : registry.getRemoteDevice(new UDN(participantUdn), true);
    }

    @Override
    public boolean addSubscription(UpnpIOParticipant participant, String serviceID) {
        return addSubscription(participant, serviceID, UserConstants.DEFAULT_SUBSCRIPTION_DURATION_SECONDS);
    }

    @Override
    public boolean addSubscription(UpnpIOParticipant participant, String serviceID, int requestedDurationSeconds) {
        RemoteDevice device = getDevice(participant);
        if (device instanceof RemoteDevice remoteDevice) {
            ServiceId sid = resolveServiceId(serviceID, device.getType().getNamespace());
            // First look for the service in the root device only, and only if not found,
            // look in the embedded devices.
            RemoteService service = enumerateServices(remoteDevice, true).stream()
                    .filter(s -> sid.equals(s.getServiceId())).findAny().orElse(enumerateServices(remoteDevice, false)
                            .stream().filter(s -> sid.equals(s.getServiceId())).findAny().orElse(null));
            if (service != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Setting up a GENA subscription for '{}' for particpant '{}'", serviceID,
                            participant.getUDN());
                }

                ParticipantData data;
                synchronized (this) {
                    registerParticipant(participant);
                    data = Objects
                            .requireNonNull(participants.computeIfAbsent(participant, d -> new ParticipantData()));
                }
                UpnpSubscriptionCallback callback = new UpnpSubscriptionCallback(participant, service,
                        requestedDurationSeconds);
                UpnpSubscriptionCallback oldCallback = data.addCallback(service, callback, true);
                if (oldCallback != null) {
                    logger.warn(
                            "Participant '{}' added a GENA subscription for '{}' when one already existed. Cancelling the old subscription.",
                            participant.getUDN(), serviceID);
                }
                upnpService.getControlPoint().execute(callback);
                return true;
            } else {
                logger.debug("Could not find service '{}' for device '{}'", serviceID, device.getIdentity().getUdn());
            }
        } else {
            logger.debug("Could not find an UPnP device for participant '{}'", participant.getUDN());
        }
        return false;
    }

    @Override
    public boolean addSubscription(UpnpIOParticipant participant, RemoteDevice device, String serviceId,
            @Nullable String namespace) {
        return addSubscription(participant, device, serviceId, namespace,
                UserConstants.DEFAULT_SUBSCRIPTION_DURATION_SECONDS);
    }

    @Override
    public boolean addSubscription(UpnpIOParticipant participant, RemoteDevice device, String serviceId,
            @Nullable String namespace, int requestedDurationSeconds) {
        RemoteService service = findService(device, namespace, serviceId);
        if (service != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Setting up a GENA subscription for '{}' for device '{}'", service.getServiceId().getId(),
                        device.getIdentity().getUdn().getIdentifierString());
            }

            ParticipantData data;
            synchronized (this) {
                registerParticipant(participant);
                data = Objects.requireNonNull(participants.computeIfAbsent(participant, d -> new ParticipantData()));
            }
            UpnpSubscriptionCallback callback = new UpnpSubscriptionCallback(participant, service,
                    requestedDurationSeconds);
            UpnpSubscriptionCallback oldCallback = data.addCallback(service, callback, true);
            if (oldCallback != null) {
                logger.warn(
                        "Participant '{}' added a GENA subscription for '{}' when one already existed. Cancelling the old subscription.",
                        participant.getUDN(), service.getServiceId().getId());
            }
            upnpService.getControlPoint().execute(callback);
            return true;
        } else {
            logger.debug("Could not find service '{}' for device '{}'", serviceId,
                    device.getIdentity().getUdn().getIdentifierString());
        }
        return false;
    }

    @Override
    public void addSubscription(UpnpIOParticipant participant, RemoteService service) {
        addSubscription(participant, service, UserConstants.DEFAULT_SUBSCRIPTION_DURATION_SECONDS);
    }

    @Override
    public void addSubscription(UpnpIOParticipant participant, RemoteService service, int requestedDurationSeconds) {
        if (logger.isTraceEnabled()) {
            logger.trace("Setting up a GENA subscription for '{}' for particpant '{}'", service.getServiceId().getId(),
                    participant.getUDN());
        }

        ParticipantData data;
        synchronized (this) {
            registerParticipant(participant);
            data = Objects.requireNonNull(participants.computeIfAbsent(participant, d -> new ParticipantData()));
        }
        UpnpSubscriptionCallback callback = new UpnpSubscriptionCallback(participant, service,
                requestedDurationSeconds);
        UpnpSubscriptionCallback oldCallback = data.addCallback(service, callback, true);
        if (oldCallback != null) {
            logger.warn(
                    "Participant '{}' added a GENA subscription for '{}' when one already existed. Cancelling the old subscription.",
                    participant.getUDN(), service.getServiceId().getId());
        }
        upnpService.getControlPoint().execute(callback);
    }

    @Override
    public boolean removeSubscription(UpnpIOParticipant participant, String serviceID) {
        ParticipantData data = getData(participant);
        if (data == null) {
            logger.debug("Participant '{}' is trying to remove GENA subscription for '{}', but isn't registered",
                    participant.getUDN(), serviceID);
            return false;
        }

        UpnpSubscriptionCallback callback;
        synchronized (data) {
            Service service = data.getCallbacks().keySet().stream()
                    .filter(s -> serviceID.equals(s.getServiceId().getId())).findAny().orElse(null);
            if (service == null) {
                logger.debug("Could not find and cancel GENA subscription for '{}' for participant '{}'", serviceID,
                        participant.getUDN());
                return false;
            }
            callback = data.removeCallback(service);
        }
        if (callback != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Removed GENA subscription for '{}' for particpant '{}'", serviceID, participant.getUDN());
            }
            return true;
        }
        return false;
    }

    public boolean removeSubscription(UpnpIOParticipant participant, ServiceId serviceId) {
        ParticipantData data;
        synchronized (this) {
            data = participants.get(participant);
        }
        if (data == null) {
            logger.debug("Participant '{}' is trying to remove GENA subscription for '{}', but isn't registered",
                    participant.getUDN(), serviceId.getId());
            return false;
        }

        UpnpSubscriptionCallback callback = data.removeCallback(serviceId);
        if (callback != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Removed GENA subscription for '{}' for particpant '{}'", serviceId.getId(),
                        participant.getUDN());
            }
            return true;
        } else {
            logger.debug("Could not find and cancel GENA subscription for '{}' for participant '{}'", serviceId.getId(),
                    participant.getUDN());
        }
        return false;
    }

    public boolean removeSubscription(UpnpIOParticipant participant, Service service) {
        ParticipantData data;
        synchronized (this) {
            data = participants.get(participant);
        }
        if (data == null) {
            logger.debug("Participant '{}' is trying to remove GENA subscription for '{}', but isn't registered",
                    participant.getUDN(), service.getServiceId().getId());
            return false;
        }

        UpnpSubscriptionCallback callback = data.removeCallback(service);
        if (callback != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Removed GENA subscription for '{}' for particpant '{}'", service.getServiceId().getId(),
                        participant.getUDN());
            }
            return true;
        } else {
            logger.debug("Could not find and cancel GENA subscription for '{}' for participant '{}'",
                    service.getServiceId().getId(), participant.getUDN());
        }
        return false;
    }

    @Override
    public Map<String, @Nullable String> invokeAction(UpnpIOParticipant participant, String serviceID, String actionID,
            @Nullable Map<String, String> inputs) {
        return invokeAction(participant, null, serviceID, actionID, inputs);
    }

    @Override
    public Map<String, @Nullable String> invokeAction(UpnpIOParticipant participant, @Nullable String namespace,
            String serviceID, String actionID, @Nullable Map<String, String> inputs) {

        registerParticipant(participant);
        RemoteDevice device = getDevice(participant);

        if (device != null) {
            try {
                return invokeAction(device, namespace, serviceID, actionID, inputs);
            } catch (ActionException e) {
                return Map.of();
            }

        } else {
            logger.debug("Could not find an UPnP device for participant '{}'", participant.getUDN());
            return Map.of();
        }
    }

    @Override
    public Map<String, @Nullable String> invokeAction(String deviceUdn, @Nullable String namespace, String serviceId,
            String actionName, @Nullable Map<String, String> inputs) throws ActionException {
        Registry registry = upnpService.getRegistry();
        RemoteDevice device = registry == null ? null : registry.getRemoteDevice(new UDN(deviceUdn), true);

        if (device != null) {
            Service service = findService(device, namespace, serviceId);
            if (service instanceof RemoteService remoteService) {
                return invokeAction(remoteService, actionName, inputs);
            } else {
                logger.debug("Could not find service '{}' of device '{}'", serviceId,
                        device.getIdentity().getUdn().getIdentifierString());
                throw new ActionException(-1, "Unknown service " + serviceId);
            }
        } else {
            logger.debug("Could not find UPnP device '{}'", deviceUdn);
            throw new ActionException(-1, "Unknown device " + deviceUdn);
        }
    }

    @Override
    public Map<String, @Nullable String> invokeAction(RemoteDevice device, @Nullable String namespace, String serviceId,
            String actionName, @Nullable Map<String, String> inputs) throws ActionException {
        Service service = findService(device, namespace, serviceId);
        if (service instanceof RemoteService remoteService) {
            return invokeAction(remoteService, actionName, inputs);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not find service '{}' of device '{}'", serviceId,
                        device.getIdentity().getUdn().getIdentifierString());
            }
            throw new ActionException(-1, "Unknown service " + serviceId);
        }
    }

    @Override
    public Map<String, @Nullable String> invokeAction(RemoteService service, String actionName,
            @Nullable Map<String, String> inputs) throws ActionException {
        Action<RemoteService> action = service.getAction(actionName);
        if (action != null) {
            return invokeAction(action, inputs);
        } else {
            if (logger.isDebugEnabled()) {
                RemoteDevice device = service.getDevice();
                logger.debug("Could not find action '{}' of service '{} of device '{}'", actionName,
                        service.getServiceId().getId(), device.getIdentity().getUdn().getIdentifierString());
            }
            throw new ActionException(ErrorCode.INVALID_ACTION);
        }
    }

    @Override
    public Map<String, @Nullable String> invokeAction(Action<RemoteService> action,
            @Nullable Map<String, String> inputs) throws ActionException {
        ActionInvocation<RemoteService> invocation = new ActionInvocation<RemoteService>(action);
        if (inputs != null) {
            for (Entry<String, String> entry : inputs.entrySet()) {
                invocation.setInput(entry.getKey(), entry.getValue());
            }
        }

        if (logger.isTraceEnabled()) {
            RemoteService service = action.getService();
            RemoteDevice device = service.getDevice();
            logger.trace("Invoking Action '{}' of service '{}' for device '{}'", action.getName(),
                    service.getServiceId().getId(), device.getIdentity().getUdn().getIdentifierString());
        }
        new ActionCallback.Default(invocation, upnpService.getControlPoint()).run();

        ActionException actionException = invocation.getFailure();
        if (actionException != null && actionException.getMessage() != null) {
            if (logger.isDebugEnabled()) {
                RemoteService service = action.getService();
                RemoteDevice device = service.getDevice();
                logger.debug("Invocation of action '{}' of service '{}' for device '{}' failed: {}", action.getName(),
                        service.getServiceId().getId(), device.getIdentity().getUdn().getIdentifierString(),
                        actionException.getMessage());
            }
            throw actionException;
        }

        Map<String, @Nullable String> resultMap = new HashMap<>();
        Map<String, ActionArgumentValue<RemoteService>> result = invocation.getOutputMap();
        if (result != null) {
            String variable;
            ActionArgumentValue newArgument;
            for (Entry<String, ActionArgumentValue<RemoteService>> entry : result.entrySet()) {
                variable = entry.getKey();
                try {
                    newArgument = entry.getValue();
                } catch (final Exception ex) {
                    logger.debug("An exception occurred when getting argument for variable '{}': {}", variable,
                            ex.getMessage());
                    continue;
                }
                try {
                    if (newArgument.getValue() != null) {
                        resultMap.put(variable, newArgument.getValue().toString());
                    }
                } catch (final Exception ex) {
                    logger.debug("An exception '{}' occurred processing ActionArgumentValue '{}' with value '{}'",
                            ex.getMessage(), newArgument.getArgument().getName(), newArgument.getValue());
                }
            }
        }
        return resultMap;
    }

    @Override
    public synchronized boolean isParticipantRegistered(UpnpIOParticipant participant) {
        return participants.containsKey(participant);
    }

    @Override
    public boolean isRegistered(UpnpIOParticipant participant) {
        return isDevicePresent(participant);
    }

    @Override
    public boolean isDevicePresent(UpnpIOParticipant participant) {
        return upnpService.getRegistry().getRemoteDevice(new UDN(participant.getUDN()), true) != null;
    }

    @Override
    public boolean registerParticipant(UpnpIOParticipant participant) {
        final ParticipantData data;
        synchronized (this) {
            if (participants.containsKey(participant)) {
                return false;
            }
            data = new ParticipantData();
            participants.put(participant, data);
        }
        scheduler.submit(() -> {
            setDeviceStatus(participant, data, isDevicePresent(participant), true);
        });
        return true;
    }

    @Override
    public void unregisterParticipant(UpnpIOParticipant participant) {
        ParticipantData data;
        synchronized (this) {
            data = participants.remove(participant);
        }
        if (data != null) {
            data.dispose();
        }
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
    private synchronized ParticipantData getData(UpnpIOParticipant participant) {
        return participants.get(participant);
    }

    @Nullable
    private RemoteService findService(RemoteDevice device, @Nullable String namespace, String serviceID) {
        RemoteService service;
        String ns = namespace == null ? device.getType().getNamespace() : namespace;
        if (UDAServiceId.DEFAULT_NAMESPACE.equals(ns) || UDAServiceId.BROKEN_DEFAULT_NAMESPACE.equals(ns)) {
            service = device.findService(new UDAServiceId(serviceID));
        } else {
            service = device.findService(new ServiceId(ns, serviceID));
        }
        return service;
    }

    private ServiceId resolveServiceId(String serviceId, String namespace) {
        return UDAServiceId.DEFAULT_NAMESPACE.equals(namespace)
                || UDAServiceId.BROKEN_DEFAULT_NAMESPACE.equals(namespace) ? new UDAServiceId(serviceId)
                        : new ServiceId(namespace, serviceId);
    }

    /**
     * Propagates a device status change to all participants.
     *
     * @param device the device that has changed its status.
     * @param status {@code true}, if device is reachable, {@code false} otherwise.
     * @param force if {@code true}, participants will be notified regardless of the previous status.
     */
    private void informParticipants(RemoteDevice device, boolean status, boolean force) {
        DeviceIdentity identity;
        if ((identity = device.getIdentity()) == null) {
            return;
        }
        String identifier = identity.getUdn().getIdentifierString();
        Map<UpnpIOParticipant, ParticipantData> snapshot;
        synchronized (this) {
            snapshot = Map.copyOf(participants);
        }
        UpnpIOParticipant participant;
        for (Entry<UpnpIOParticipant, ParticipantData> entry : snapshot.entrySet()) {
            participant = entry.getKey();
            if (participant.getUDN().equals(identifier)) {
                setDeviceStatus(participant, entry.getValue(), status, force);
            }
        }
    }

    private void setDeviceStatus(UpnpIOParticipant participant, ParticipantData data, boolean newStatus,
            boolean force) {
        boolean oldStatus = data.getAndSetAvailable(newStatus);
        if (force || oldStatus != newStatus) {
            logger.debug("Device '{}' reachability status changed to '{}'{}", participant.getUDN(), newStatus,
                    oldStatus == newStatus ? " (forced)" : "");
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
            ParticipantData data = getData(participant);
            if (data == null) {
                logger.debug(
                        "Participant '{}' scheduled for polling with Action '{}' of Service '{}', but isn't registered",
                        participant.getUDN(), actionID, serviceID);
                return;
            }
            try {
                Device device = getDevice(participant);
                String participantUdn = participant.getUDN();
                if (device instanceof RemoteDevice remoteDevice) {
                    ServiceId serviceId = resolveServiceId(serviceID, device.getType().getNamespace());

                    // First look for the service in the root device only, and only if not found,
                    // look in the embedded devices.
                    Service service = enumerateServices(remoteDevice, true).stream()
                            .filter(s -> serviceId.equals(s.getServiceId())).findAny()
                            .orElse(enumerateServices(remoteDevice, false).stream()
                                    .filter(s -> serviceId.equals(s.getServiceId())).findAny().orElse(null));
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
                                setDeviceStatus(participant, data, false, false);
                            } else {
                                // The UDN functions correctly
                                setDeviceStatus(participant, data, true, false);
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
        ParticipantData data = getData(participant);
        if (data == null) {
            // Should not happen
            logger.warn("Unable to add status listener for participant {}, participant isn't registered",
                    participant.getUDN());
            return;
        }
        data.setJob(scheduler.scheduleWithFixedDelay(new UPNPPollingRunnable(participant, serviceID, actionID), 0,
                pollingInterval, TimeUnit.SECONDS));
    }

    @Override
    public void removeStatusListener(UpnpIOParticipant participant) {
        ParticipantData data = getData(participant);
        if (data != null) {
            data.clearJob();
        }
    }

    @Override
    public void remoteDeviceAdded(@Nullable Registry registry, @Nullable RemoteDevice device) {
        DeviceIdentity identity;
        if (device != null && (identity = device.getIdentity()) != null) {
            synchronized (events) {
                CachedDeviceEvent event = events.get(identity);
                if (event == null || !event.updateEvent(device, true)) {
                    // Event didn't exist or canceling failed, create a new one
                    events.put(identity, new CachedDeviceEvent(device, true, cachedEventDelay));
                }
            }
        }
    }

    @Override
    public void remoteDeviceUpdated(@Nullable Registry registry, @Nullable RemoteDevice device) {
        DeviceIdentity identity;
        if (device != null && (identity = device.getIdentity()) != null) {
            synchronized (events) {
                CachedDeviceEvent event = events.get(identity);
                if (event == null || !event.updateEvent(device, true)) {
                    // Event didn't exist or canceling failed, create a new one
                    events.put(identity, new CachedDeviceEvent(device, true, cachedEventDelay));
                }
            }
        }
    }

    @Override
    public void remoteDeviceRemoved(@Nullable Registry registry, @Nullable RemoteDevice device) {
        DeviceIdentity identity;
        if (device != null && (identity = device.getIdentity()) != null) {
            synchronized (events) {
                CachedDeviceEvent event = events.get(identity);
                if (event == null || !event.updateEvent(device, false)) {
                    // Event didn't exist or canceling failed, create a new one
                    events.put(identity, new CachedDeviceEvent(device, false, cachedEventDelay));
                }
            }
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

    /**
     * Generates a {@link List} of {@link RemoteService}es provided by the specified {@link RemoteDevice},
     * and optionally, any embedded/child devices.
     *
     * @param device the {@link RemoteDevice} whose {@link Service}s to enumerate.
     * @param rootOnly {@code true} to only enumerate services from the device itself, {@code false} to also
     *            include services from its children.
     * @return The resulting {@link List} of {@link RemoteService}es.
     */
    public static List<RemoteService> enumerateServices(RemoteDevice device, boolean rootOnly) {
        List<RemoteService> result = new ArrayList<>();
        List<RemoteDevice> devices = rootOnly ? List.of(device) : enumerateAllDevices(device);
        RemoteService[] services;
        for (RemoteDevice d : devices) {
            services = d.getServices();
            if (services != null && services.length > 0) {
                result.addAll(Arrays.asList(services));
            }
        }
        return result;
    }

    /**
     * Generates a {@link List} of the specified {@link RemoteDevice} itself and its embedded/child devices.
     *
     * @param device the {@link RemoteDevice} whose device tree to enumerate.
     * @return The resulting {@link List} of {@link RemoteDevice}s.
     */
    public static List<RemoteDevice> enumerateAllDevices(RemoteDevice device) {
        List<RemoteDevice> result = new ArrayList<>();
        result.add(device);
        enumerateChildDevices(device, result);
        return result;
    }

    /**
     * Traverses and adds child/embedded devices to the provided {@link List} recursively.
     *
     * @param device the {@link RemoteDevice} whose descendants to add to {@code devices}.
     * @param devices the {@link List} to add the descendants to.
     */
    private static void enumerateChildDevices(RemoteDevice device, List<RemoteDevice> devices) {
        for (RemoteDevice child : device.getEmbeddedDevices()) {
            devices.add(child);
            enumerateChildDevices(child, devices);
        }
    }

    private class CachedDeviceEvent {

        private final DeviceIdentity identity;

        private final long delayMs;

        private boolean status;

        private RemoteDevice device;

        @Nullable
        private ScheduledFuture<?> task;

        public CachedDeviceEvent(RemoteDevice device, boolean status, long delayMs) {
            this.device = device;
            this.identity = device.getIdentity();
            this.status = status;
            this.delayMs = delayMs;
            scheduleRun();
        }

        public boolean updateEvent(RemoteDevice device, boolean status) {
            this.device = device;
            this.status = status;
            return scheduleRun();
        }

        @SuppressWarnings("PMD.CompareObjectsWithEquals")
        private synchronized boolean scheduleRun() {
            ScheduledFuture<?> task = this.task;
            if (task != null) {
                task.cancel(false);
            }
            boolean result = task == null || task.isCancelled();
            final RemoteDevice deviceRef = device;
            final boolean statusCopy = status;
            final CachedDeviceEvent instanceRef = this;
            this.task = scheduler.schedule(() -> {
                synchronized (events) {
                    CachedDeviceEvent event = events.get(identity);
                    if (event == instanceRef) {
                        events.remove(identity);
                    }
                }
                informParticipants(deviceRef, statusCopy, false);
            }, delayMs, TimeUnit.MILLISECONDS);
            return result;
        }
    }

    /**
     * A container for data held for each registered participant. Threadsafe.
     */
    public static class ParticipantData {

        // All access must be guarded by "this"
        @Nullable
        private ScheduledFuture<?> job;

        // All access must be guarded by "this"
        private boolean available;

        // All access must be guarded by "this"
        private final Map<RemoteService, UpnpSubscriptionCallback> callbacks = new HashMap<>();

        /**
         * @return The current polling job, if any.
         */
        @Nullable
        public synchronized ScheduledFuture<?> getJob() {
            return job;
        }

        /**
         * @return {@code true} if a polling job is registered.
         */
        public synchronized boolean hasJob() {
            return job != null;
        }

        /**
         * Sets the current polling job, and cancels the old one if one already exists.
         *
         * @param job the new polling job.
         */
        public void setJob(@Nullable ScheduledFuture<?> job) {
            ScheduledFuture<?> oldJob;
            synchronized (this) {
                oldJob = this.job;
                this.job = job;
            }
            if (oldJob != null && !oldJob.isDone()) {
                oldJob.cancel(true);
            }
        }

        /**
         * Sets the current polling job to {@code null} and cancels the old one if one exists.
         */
        public void clearJob() {
            setJob(null);
        }

        /**
         * @return {@code true} is the associated {@link RemoteDevice} is available, {@code false} otherwise.
         */
        public synchronized boolean isAvailable() {
            return available;
        }

        /**
         * Sets the availability status and returns the old one, within the same lock.
         *
         * @param status the new availability status.
         * @return the old availability status.
         */
        public synchronized boolean getAndSetAvailable(boolean status) {
            boolean result = available;
            available = status;
            return result;
        }

        /**
         * Sets the availability status.
         *
         * @param status the new availability status.
         */
        public synchronized void setAvailable(boolean status) {
            available = status;
        }

        /**
         * Checks if this {@link ParticipantData} instance holds as subscription callback that is registered
         * using the specified service ID.
         *
         * @param serviceId the service ID to check for.
         * @return {@code true} if a matching callback was found, {@code false} otherwise.
         */
        public synchronized boolean hasCallback(ServiceId serviceId) {
            return callbacks.keySet().stream().anyMatch(s -> serviceId.equals(s.getServiceId()));
        }

        /**
         * Checks if this {@link ParticipantData} instance holds as subscription callback that is registered
         * using the specified {@link Service}.
         *
         * @param service the {@link Service} to check for.
         * @return {@code true} if a matching callback was found, {@code false} otherwise.
         */
        public synchronized boolean hasCallback(Service service) {
            return callbacks.containsKey(service);
        }

        /**
         * Retrieve a {@link UpnpSubscriptionCallback} instance registered with the specified {@link ServiceId}.
         *
         * @param serviceId the {@link ServiceId} used for evaluation.
         * @return A matching {@link UpnpSubscriptionCallback} or {@code null}.
         */
        @Nullable
        public synchronized UpnpSubscriptionCallback getCallback(ServiceId serviceId) {
            return callbacks.entrySet().stream().filter(e -> serviceId.equals(e.getKey().getServiceId())).findAny()
                    .map(e -> e.getValue()).orElse(null);
        }

        /**
         * Retrieve the {@link UpnpSubscriptionCallback} instance registered with the specified {@link Service}.
         *
         * @param service the {@link Service} key.
         * @return The resulting {@link UpnpSubscriptionCallback} or {@code null}.
         */
        @Nullable
        public synchronized UpnpSubscriptionCallback getCallback(Service service) {
            return callbacks.get(service);
        }

        /**
         * @return A snapshot/copy of the {@link Map} of {@link RemoteService} and
         *         {@link UpnpSubscriptionCallback} pairs.
         */
        public synchronized Map<RemoteService, UpnpSubscriptionCallback> getCallbacks() {
            return Map.copyOf(callbacks);
        }

        /**
         * Add a {@link UpnpSubscriptionCallback} callback associated with the specified {@link RemoteService}
         * to the {@link Map} of callbacks. If a callback already exists for the specified {@link RemoteService},
         * the previously registered callback will be returned. If {@code doEnd} is {@code true}, its subscription
         * will also be cancelled before it is returned.
         *
         * @param service the {@link RemoteService} to use as the key.
         * @param callback The {@link UpnpSubscriptionCallback} to use as the value.
         * @param doEnd {@code true} to cancel the subscription of the old callback, if one is already registered for
         *            the key.
         * @return The previous {@link UpnpSubscriptionCallback} stored for the specified key, or {@code null}.
         */
        @Nullable
        public UpnpSubscriptionCallback addCallback(RemoteService service, UpnpSubscriptionCallback callback,
                boolean doEnd) {
            UpnpSubscriptionCallback result;
            synchronized (this) {
                result = callbacks.put(service, callback);
            }
            if (doEnd && result != null) {
                result.end();
            }
            return result;
        }

        /**
         * Remove a {@link UpnpSubscriptionCallback} associated with a service of the specified service ID from
         * the {@link Map} of callbacks, and return it. If a callback is found, its subscription is canceled
         * before it is returned.
         *
         * @param serviceId the {@link ServiceId} to match.
         * @return The removed {@link UpnpSubscriptionCallback} or {@code null}.
         */
        @Nullable
        public UpnpSubscriptionCallback removeCallback(ServiceId serviceId) {
            UpnpSubscriptionCallback result = null;
            Entry<RemoteService, UpnpSubscriptionCallback> entry;
            synchronized (this) {
                for (Iterator<Entry<RemoteService, UpnpSubscriptionCallback>> iterator = callbacks.entrySet()
                        .iterator(); iterator.hasNext();) {
                    entry = iterator.next();
                    if (serviceId.equals(entry.getKey().getServiceId())) {
                        result = entry.getValue();
                        iterator.remove();
                        break;
                    }
                }
            }
            if (result != null) {
                result.end();
            }
            return result;
        }

        /**
         * Remove a {@link UpnpSubscriptionCallback} associated with the specified {@link Service} from the
         * {@link Map} of callbacks, and return it. If a callback is found, its subscription is canceled before
         * it is returned.
         *
         * @param service the {@link Service} key for the entry to remove.
         * @return The removed {@link UpnpSubscriptionCallback} or {@code null}.
         */
        @Nullable
        public UpnpSubscriptionCallback removeCallback(Service service) {
            UpnpSubscriptionCallback result;
            synchronized (this) {
                result = callbacks.remove(service);
            }
            if (result != null) {
                result.end();
            }
            return result;
        }

        /**
         * Cancels all ongoing operations referenced by this {@link ParticipantData} instance. If a job is
         * registered, it is canceled, and so are any active subscriptions.
         */
        public void dispose() {
            ScheduledFuture<?> job;
            Map<RemoteService, UpnpSubscriptionCallback> callbacks;
            synchronized (this) {
                job = this.job;
                this.job = null;
                callbacks = this.callbacks;
                this.callbacks.clear();
                this.available = false;
            }
            if (job != null) {
                job.cancel(true);
            }
            callbacks.forEach((i, c) -> c.end());
        }
    }
}
