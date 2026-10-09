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
package org.openhab.core.io.transport.upnp;

import java.net.URL;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.Service;

/**
 * The {@link UpnpIOService} is an interface that described the
 * UPNP IO Service.
 *
 * @author Karel Goderis - Initial contribution
 * @author Kai Kreuzer - added descriptor url retrieval
 */
@NonNullByDefault
public interface UpnpIOService {

    /**
     * Invoke an UPnP Action using the device default namespace, serviceID, actionID and inputs.
     * <p>
     * <b>Note:</b> This method has been kept as-is for backwards compatibility, but has some shortcomings:
     * <ul>
     * <li>It implicitly registers the participant with the {@link UpnpIOService}, which might not be expected.</li>
     * <li>It doesn't throw an exception in case of an error, it just returns an empty map. There is therefore
     * no real way to tell an error from a genuine empty result.</li>
     * <li>It only allows invoking actions on services of the root device.
     * </ul>
     * To avoid these limitations, use one of the other overloaded versions.
     * <p>
     * 
     * @param participant the participant to invoke the action for
     * @param serviceID the UPNP service to invoke the action upon
     * @param actionID the Action to invoke
     * @param inputs a map of {variable,values} to parameterize the Action that will be invoked
     */
    Map<String, @Nullable String> invokeAction(UpnpIOParticipant participant, String serviceID, String actionID,
            @Nullable Map<String, String> inputs);

    /**
     * Invoke an UPnP Action using the specified namespace, serviceID, actionID and inputs.
     * <p>
     * <b>Note:</b> This method has been kept as-is for backwards compatibility, but has some shortcomings:
     * <ul>
     * <li>It implicitly registers the participant with the {@link UpnpIOService}, which might not be expected.</li>
     * <li>It doesn't throw an exception in case of an error, it just returns an empty map. There is therefore
     * no real way to tell an error from a genuine empty result.</li>
     * <li>It only allows invoking actions on services of the root device.
     * </ul>
     * To avoid these limitations, use one of the other overloaded versions.
     * <p>
     * 
     * @param participant the participant to invoke the action for
     * @param namespace the namespace of the service to invoke the action upon
     * @param serviceID the UPNP service to invoke the action upon
     * @param actionID the Action to invoke
     * @param inputs a map of {variable,values} to parameterize the Action that will be invoked
     */
    Map<String, String> invokeAction(UpnpIOParticipant participant, @Nullable String namespace, String serviceID,
            String actionID, @Nullable Map<String, String> inputs);

    /**
     * Invoke the action with the specified action name from the device with the specified UDN and
     * service ID, using the specified inputs.
     * <p>
     * <b>Note:</b> Embedded/child devices will not be searched, specify the UDN of the device that
     * actually offers the service you wish to invoke.
     *
     * @param deviceUdn the UDN of the device whose service to look up.
     * @param namespace the namespace to use, or {@code null} to use the device default namespace.
     * @param serviceId the ID of the service whose action to invoke.
     * @param actionName the name of the action to invoke.
     * @param inputs the {@link Map} of input arguments.
     * @return The resulting {@link Map} of results.
     * @throws ActionException If the invocation fails.
     */
    public Map<String, @Nullable String> invokeAction(String deviceUdn, @Nullable String namespace, String serviceId,
            String actionName, @Nullable Map<String, String> inputs) throws ActionException;

    /**
     * Invoke the action with the specified action name from the specified {@link RemoteDevice} and
     * service ID, using the specified inputs.
     * <p>
     * <b>Note:</b> Embedded/child devices will not be searched, specify the {@link RemoteDevice} that
     * actually offers the service you wish to invoke.
     *
     * @param device the {@link RemoteDevice} whose service to look up.
     * @param namespace the namespace to use, or {@code null} to use the device default namespace.
     * @param serviceId the ID of the service whose action to invoke.
     * @param actionName the name of the action to invoke.
     * @param inputs the {@link Map} of input arguments.
     * @return The resulting {@link Map} of results.
     * @throws ActionException If the invocation fails.
     */
    public Map<String, @Nullable String> invokeAction(RemoteDevice device, @Nullable String namespace, String serviceId,
            String actionName, @Nullable Map<String, String> inputs) throws ActionException;

    /**
     * Invoke the action with the specified action name from the specified {@link RemoteService} using
     * the specified inputs.
     *
     * @param service the {@link RemoteService} whose action to invoke.
     * @param actionName the name of the action to invoke.
     * @param inputs the {@link Map} of input arguments.
     * @return The resulting {@link Map} of results.
     * @throws ActionException If the invocation fails.
     */
    public Map<String, @Nullable String> invokeAction(RemoteService service, String actionName,
            @Nullable Map<String, String> inputs) throws ActionException;

    /**
     * Invoke the specified {@link Action} and inputs.
     *
     * @param action the {@link Action} to invoke.
     * @param inputs the {@link Map} of input arguments.
     * @return The resulting {@link Map} of results.
     * @throws ActionException If the invocation fails.
     */
    public Map<String, @Nullable String> invokeAction(Action<RemoteService> action,
            @Nullable Map<String, String> inputs) throws ActionException;

    /**
     * Create a GENA subscription for a {@link Service} with the specified ID and with a request for the
     * default subscription duration (30 minutes). Please note that this is just a request, the publisher might
     * grant a subscription with a different duration. If the service is found, this method will also register the
     * participant with the {@link UpnpIOService} if it's not already registered.
     * <p>
     * For more information about subscription duration, see {@link #addSubscription(UpnpIOParticipant, String, int)}.
     * <p>
     * The service will first be attempted resolved by looking through the services provided by the root device
     * the participant is tied to with its UDN. If a service with the specified ID is found, it will be subscribed
     * to. If not, services from embedded/child devices will be searched. If several embedded/child services provides
     * a service with the same ID, it's unpredictable which one will be subscribed to. Use one of the overloaded
     * methods to control which service to subscribe to if multiple with the same ID are available.
     *
     * @param participant the participant that will receive the subscription events.
     * @param serviceID the ID of UPnP service we want to subscribe to.
     * @return {@code true} if the subscription attempt succeeded.
     */
    boolean addSubscription(UpnpIOParticipant participant, String serviceID);

    /**
     * Create a GENA subscription for a {@link Service} with the specified ID with the specified subscription
     * duration. Please note that this is just a request, the publisher might grant a subscription with a
     * different duration. If the service is found, this method will also register the participant with the
     * {@link UpnpIOService} if it's not already registered.
     * <p>
     * The service will first be attempted resolved by looking through the services provided by the root device
     * the participant is tied to with its UDN. If a service with the specified ID is found, it will be subscribed
     * to. If not, services from embedded/child devices will be searched. If several embedded/child services provides
     * a service with the same ID, it's unpredictable which one will be subscribed to. Use one of the overloaded
     * methods to control which service to subscribe to if multiple with the same ID are available.
     * <p>
     * The subscription duration is <i>not</i> the duration the subscription will stay active. It will stay
     * active until it is cancelled. Instead, it's a Time To Live value for the subscription, within which the
     * subscription must be renewed, or it is automatically cancelled. jUPnP, which is used to manage this, will
     * automatically renew subscriptions in time. This value is thus only needed to control when the publisher
     * can consider a subscription cancelled in case OH stops responding, lose network connectivity, power etc.
     * A low value will mean that the publisher will cancel the subscription sooner under such circumstances,
     * but it will also lead to more network traffic to handle renewals.
     * <p>
     * The UPnP standard says the following on the subject:
     * <blockquote>
     * A duration should be chosen that matches assumptions about how frequently control points are removed from
     * the network; if control points are removed every few minutes, then the duration should be similarly short,
     * allowing a publisher to rapidly deprecate any expired subscribers; if control points are expected to be
     * semi-permanent, then the duration should be very long, minimizing the processing and traffic associated
     * with renewing subscriptions.
     * <p>
     * Should be greater than or equal to 1800 seconds (30 minutes).
     * </blockquote>
     *
     * @param participant the participant that will receive the subscription events.
     * @param serviceID the ID of UPnP service we want to subscribe to.
     * @param requestedDurationSeconds the requested duration of the subscription in seconds.
     * @return {@code true} if the subscription attempt succeeded.
     */
    boolean addSubscription(UpnpIOParticipant participant, String serviceID, int requestedDurationSeconds);

    /**
     * Create a GENA subscription for a {@link Service} with the specified ID offered by the specified
     * {@link RemoteDevice}, with a request for the default subscription duration (30 minutes). Please note that
     * this is just a request, the publisher might grant a subscription with a different duration. If the service
     * is found, this method will also register the participant with the {@link UpnpIOService} if it's not already
     * registered.
     * <p>
     * The service will be resolved by looking only at the services provided by the specified device. If the device
     * offers multiple versions of the same service, it is unpredictable which service will be subscribed to.
     * <p>
     * For more information about subscription duration, see
     * {@link #addSubscription(UpnpIOParticipant, RemoteDevice, String, int)}.
     *
     * @param participant the participant that will receive the subscription events.
     * @param device the {@link RemoteDevice} which provides the service to subscribe to.
     * @param serviceId the ID of UPnP service we want to subscribe to.
     * @param namespace the namespace to use, or {@code null} to use the device default namespace.
     * @return {@code true} if the subscription attempt succeeded.
     */
    public boolean addSubscription(UpnpIOParticipant participant, RemoteDevice device, String serviceId,
            @Nullable String namespace);

    /**
     * Create a GENA subscription for a {@link Service} with the specified ID offered by the specified
     * {@link RemoteDevice}, with the specified subscription duration. Please note that this is just a request,
     * the publisher might grant a subscription with a different duration. If the service is found, this method
     * will also register the participant with the {@link UpnpIOService} if it's not already registered.
     * <p>
     * The service will be resolved by looking only at the services provided by the specified device. If the device
     * offers multiple versions of the same service, it is unpredictable which service will be subscribed to.
     * <p>
     * The subscription duration is <i>not</i> the duration the subscription will stay active. It will stay
     * active until it is cancelled. Instead, it's a Time To Live value for the subscription, within which the
     * subscription must be renewed, or it is automatically cancelled. jUPnP, which is used to manage this, will
     * automatically renew subscriptions in time. This value is thus only needed to control when the publisher
     * can consider a subscription cancelled in case OH stops responding, lose network connectivity, power etc.
     * A low value will mean that the publisher will cancel the subscription sooner under such circumstances,
     * but it will also lead to more network traffic to handle renewals.
     * <p>
     * The UPnP standard says the following on the subject:
     * <blockquote>
     * A duration should be chosen that matches assumptions about how frequently control points are removed from
     * the network; if control points are removed every few minutes, then the duration should be similarly short,
     * allowing a publisher to rapidly deprecate any expired subscribers; if control points are expected to be
     * semi-permanent, then the duration should be very long, minimizing the processing and traffic associated
     * with renewing subscriptions.
     * <p>
     * Should be greater than or equal to 1800 seconds (30 minutes).
     * </blockquote>
     *
     * @param participant the participant that will receive the subscription events.
     * @param device the {@link RemoteDevice} which provides the service to subscribe to.
     * @param serviceId the ID of UPnP service we want to subscribe to.
     * @param namespace the namespace to use, or {@code null} to use the device default namespace.
     * @param requestedDurationSeconds the requested duration of the subscription in seconds.
     * @return {@code true} if the subscription attempt succeeded.
     */
    public boolean addSubscription(UpnpIOParticipant participant, RemoteDevice device, String serviceId,
            @Nullable String namespace, int requestedDurationSeconds);

    /**
     * Create a GENA subscription for the specified {@link RemoteService} with a request for the default
     * subscription duration (30 minutes). Please note that this is just a request, the publisher might
     * grant a subscription with a different duration. This method will register the participant with
     * the {@link UpnpIOService} if it's not already registered.
     * <p>
     * For more information about subscription duration, see
     * {@link #addSubscription(UpnpIOParticipant, RemoteService, int)}.
     *
     * @param participant the participant that will receive the subscription events.
     * @param service the {@link RemoteService} to subscribe to.
     */
    public void addSubscription(UpnpIOParticipant participant, RemoteService service);

    /**
     * Create a GENA subscription for the specified {@link RemoteService} with the specified subscription duration.
     * Please note that this is just a request, the publisher might grant a subscription with a different duration.
     * This method will register the participant with the {@link UpnpIOService} if it's not already registered.
     * <p>
     * The subscription duration is <i>not</i> the duration the subscription will stay active. It will stay
     * active until it is cancelled. Instead, it's a Time To Live value for the subscription, within which the
     * subscription must be renewed, or it is automatically cancelled. jUPnP, which is used to manage this, will
     * automatically renew subscriptions in time. This value is thus only needed to control when the publisher
     * can consider a subscription cancelled in case OH stops responding, lose network connectivity, power etc.
     * A low value will mean that the publisher will cancel the subscription sooner under such circumstances,
     * but it will also lead to more network traffic to handle renewals.
     * <p>
     * The UPnP standard says the following on the subject:
     * <blockquote>
     * A duration should be chosen that matches assumptions about how frequently control points are removed from
     * the network; if control points are removed every few minutes, then the duration should be similarly short,
     * allowing a publisher to rapidly deprecate any expired subscribers; if control points are expected to be
     * semi-permanent, then the duration should be very long, minimizing the processing and traffic associated
     * with renewing subscriptions.
     * <p>
     * Should be greater than or equal to 1800 seconds (30 minutes).
     * </blockquote>
     *
     * @param participant the participant that will receive the subscription events.
     * @param service the {@link RemoteService} to subscribe to.
     * @param requestedDurationSeconds the requested duration of the subscription in seconds.
     */
    public void addSubscription(UpnpIOParticipant participant, RemoteService service, int requestedDurationSeconds);

    /**
     * Unsubscribe from a GENA subscription.
     * <p>
     * Please note that if the participant is subscribing to several services with the same ID, this will just cancel
     * one of them. There's no way to control which one. For more control, use one of the overloaded versions of
     * this method.
     *
     * @param participant the participant that is the receiver of the subscription events.
     * @param serviceID the ID of the UPnP service we want to unsubscribe from.
     * @return {@code true} if a subscription was unsubscribed from.
     */
    boolean removeSubscription(UpnpIOParticipant participant, String serviceID);

    /**
     * Unsubscribe from a GENA subscription with the specified service ID from the specified {@link RemoteDevice}.
     * <p>
     * Please note that if the participant is subscribing to several services with the same ID from the same device,
     * this will just cancel one of them. There's no way to control which one. For more control, use
     * {@link #removeSubscription(UpnpIOParticipant, RemoteService)}.
     *
     * @param participant the participant that is the receiver of the subscription events.
     * @param device the {@link RemoteDevice} that provides the service to unsubscribe from.
     * @param serviceId the ID of the service to unsubscribe from.
     * @param namespace the namespace to use, or {@code null} to use the device default namespace.
     * @return {@code true} if a subscription was unsubscribed from.
     */
    public boolean removeSubscription(UpnpIOParticipant participant, RemoteDevice device, String serviceId,
            @Nullable String namespace);

    /**
     * Unsubscribe from a GENA subscription with the specified {@link RemoteService}.
     *
     * @param participant the participant that is the receiver of the subscription events.
     * @param service the {@link RemoteService} whose subscription to cancel.
     * @return {@code true} if a subscription was unsubscribed from.
     */
    public boolean removeSubscription(UpnpIOParticipant participant, RemoteService service);

    /**
     * Verify if the the specified participant is registered with {@link UpnpIOService} and will
     * receive device status updates.
     *
     * @param participant the participant whose registration to verify.
     * @return {@code true} if the participant is registered with the {@link UpnpIOService}.
     */
    boolean isParticipantRegistered(UpnpIOParticipant participant);

    /**
     * For historic reasons, this method performs the same check as {@link #isDevicePresent(UpnpIOParticipant)}.
     *
     * @deprecated Do not use because of its misleading name. Instead, use {@link #isDevicePresent(UpnpIOParticipant)}
     *             or {@link #isParticipantRegistered(UpnpIOParticipant)} depending on what you want to get an answer
     *             to.
     *
     * @param participant the participant whose "linked device" to check for presence.
     * @return {@code true} if the device is deemed to be connected, {@code false} if not.
     */
    @Deprecated(forRemoval = false)
    boolean isRegistered(UpnpIOParticipant participant);

    /**
     * Verify if the {@link RemoteDevice} that corresponds to the UDN reported by participant's
     * {@link UpnpIOParticipant#getUDN()} method, is currently found in jUPnP's registry - which means that
     * according to the rules of UPnP, it should currently be connected and reachable.
     *
     * @param participant the participant whose "linked device" to check for presence.
     * @return {@code true} if the device is deemed to be connected, {@code false} if not.
     */
    boolean isDevicePresent(UpnpIOParticipant participant);

    /**
     * Register a participant with {@link UpnpIOService}. Participants will automatically receive
     * presence status updates for their "linked device". The current status will be notified
     * promptly after registration.
     *
     * @param participant the participant whose participation we want to register
     * @return {@code true} if the participant was registered, {@code false} it the participant was
     *         already registered.
     */
    boolean registerParticipant(UpnpIOParticipant participant);

    /**
     * Unregister a participant with the UPNP IO Service
     *
     * @param participant the participant whose participation we want to unregister
     */
    void unregisterParticipant(UpnpIOParticipant participant);

    /**
     * Retrieves the descriptor url for the participant
     *
     * @param participant the participant whom's descriptor url is requested
     * @return the url of the descriptor as provided by the upnp device
     */
    @Nullable
    URL getDescriptorURL(UpnpIOParticipant participant);

    /**
     * Establish a polling mechanism to check the status of a specific UDN device. The polling mechanism
     * works by invoking the actionID on serviceID every interval. It is assumed that the actionID does
     * not take/have to take any {variable,value} input set.
     * <p>
     * <b>Note:</b> This should be avoided unless the device is unable to stay online according to the UPnP
     * specification. Polling is not needed for functioning devices, and will lead to increased load on both
     * ends and the network, without any benefit.
     *
     * @deprecated Should be avoided if at all possible. If using a broken device that relies on polling, the
     *             deprecation warning should be suppressed.
     *
     * @param participant the participant for whom we want to set up a polling
     * @param serviceID the service to use for polling
     * @param actionID the action to call
     * @param interval the interval in seconds
     */
    @Deprecated(forRemoval = false)
    void addStatusListener(UpnpIOParticipant participant, String serviceID, String actionID, int interval);

    /**
     * Stops the polling mechanism to check the status of a specific UDN device.
     *
     * @param participant the participant for whom we want to remove the polling
     */
    void removeStatusListener(UpnpIOParticipant participant);
}
