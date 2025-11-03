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

import org.jupnp.model.meta.Service;

/**
 * The {@link UpnpIOService} is an interface that described the
 * UPNP IO Service.
 *
 * @author Karel Goderis - Initial contribution
 * @author Kai Kreuzer - added descriptor url retrieval
 */
public interface UpnpIOService {

    /**
     * Invoke an UPNP Action using the device default namespace and serviceID
     *
     * @param participant the participant to invoke the action for
     * @param serviceID the UPNP service to invoke the action upon
     * @param actionID the Action to invoke
     * @param inputs a map of {variable,values} to parameterize the Action that will be invoked
     */
    Map<String, String> invokeAction(UpnpIOParticipant participant, String serviceID, String actionID,
            Map<String, String> inputs);

    /**
     * Invoke an UPNP Action using the specified namespace and serviceID
     *
     * @param participant the participant to invoke the action for
     * @param namespace the namespace of the service to invoke the action upon
     * @param serviceID the UPNP service to invoke the action upon
     * @param actionID the Action to invoke
     * @param inputs a map of {variable,values} to parameterize the Action that will be invoked
     */
    Map<String, String> invokeAction(UpnpIOParticipant participant, String namespace, String serviceID, String actionID,
            Map<String, String> inputs);

    /**
     * Create a GENA subscription for a {@link Service} with the specified ID and with a request for the
     * default subscription duration (30 minutes). Please not that this is just a request, the published might
     * grant a subscription with a different duration.
     * <p>
     * For more information about subscription duration, see {@link #addSubscription(UpnpIOParticipant, String, int)}.
     *
     * @param participant the participant to the subscription is for
     * @param serviceID the UPNP service we want to subscribe to
     */
    void addSubscription(UpnpIOParticipant participant, String serviceID);

    /**
     * Create a GENA subscription for a {@link Service} with the specified ID and with a request for the
     * specified subscription duration. Please not that this is just a request, the published might grant a
     * subscription with a different duration.
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
     * @param participant the participant to the subscription is for
     * @param serviceID the UPNP service we want to subscribe to
     * @param requestedDurationSeconds the request duration of the subscription in seconds.
     */
    void addSubscription(UpnpIOParticipant participant, String serviceID, int requestedDurationSeconds);

    /**
     * Unsubscribe from a GENA subscription
     *
     * @param participant the participant of the subscription
     * @param serviceID the UPNP service we want to unsubscribe from
     */
    void removeSubscription(UpnpIOParticipant participant, String serviceID);

    /**
     * Verify if the a participant is registered
     *
     * @param participant the participant whom's participation we want to verify
     * @return true of the participant is registered with the UpnpIOService
     */
    boolean isRegistered(UpnpIOParticipant participant);

    /**
     * Register a participant with the UPNP IO Service
     *
     * @param participant the participant whose participation we want to register
     */
    void registerParticipant(UpnpIOParticipant participant);

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
    URL getDescriptorURL(UpnpIOParticipant participant);

    /**
     * Establish a polling mechanism to check the status of a specific UDN device. The polling mechanism
     * works by invoking the actionID on serviceID every interval. It is assumed that the actionID does
     * not take/have to take any {variable,value} input set
     *
     * @param participant the participant for whom we want to set up a polling
     * @param serviceID the service to use for polling
     * @param actionID the action to call
     * @param interval the interval in seconds
     */
    void addStatusListener(UpnpIOParticipant participant, String serviceID, String actionID, int interval);

    /**
     * Stops the polling mechanism to check the status of a specific UDN device.
     *
     * @param participant the participant for whom we want to remove the polling
     */
    void removeStatusListener(UpnpIOParticipant participant);
}
