/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.transport.upnp;

import java.net.URL;
import java.util.Map;

/**
 * The {@link UpnpIOService} is an interface that described the
 * UPNP IO Service.
 *
 * @author Karel Goderis - Initial contribution
 * @author Kai Kreuzer - added descriptor url retrieval
 */
public interface UpnpIOService {

    /**
     * Invoke an UPNP Action
     * 
     * @param participant the participant to invoke the action for
     * @param serivceID the UPNP service to invoke the action upon
     * @param actionID the Action to invoke
     * @param inputs a map of {variable,values} to parameterize the Action that will be invoked
     */
    public Map<String, String> invokeAction(UpnpIOParticipant participant, String serviceID, String actionID,
            Map<String, String> inputs);

    /**
     * Subscribe to a GENA subscription
     * 
     * @param participant the participant to the subscription is for
     * @param serviceID the UPNP service we want to subscribe to
     * @param duration the duration of the subscription
     */
    public void addSubscription(UpnpIOParticipant participant, String serviceID, int duration);

    /**
     * Unsubscribe from a GENA subscription
     * 
     * @param participant the participant of the subscription
     * @param serviceID the UPNP service we want to unsubscribe from
     */
    public void removeSubscription(UpnpIOParticipant participant, String serviceID);

    /**
     * Verify if the a participant is registered
     * 
     * @param participant the participant whom's participation we want to verify
     * @return true of the participant is registered with the UpnpIOService
     */
    public boolean isRegistered(UpnpIOParticipant participant);

    /**
     * Register a participant with the UPNP IO Service
     * 
     * @param participant the participant whose participation we want to register
     */
    public void registerParticipant(UpnpIOParticipant participant);

    /**
     * Unregister a participant with the UPNP IO Service
     * 
     * @param participant the participant whose participation we want to unregister
     */
    public void unregisterParticipant(UpnpIOParticipant participant);

    /**
     * Retrieves the descriptor url for the participant
     * 
     * @param participant the participant whom's descriptor url is requested
     * @return the url of the descriptor as provided by the upnp device
     */
    public URL getDescriptorURL(UpnpIOParticipant participant);

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
    public void addStatusListener(UpnpIOParticipant participant, String serviceID, String actionID, int interval);

    /**
     * Stops the polling mechanism to check the status of a specific UDN device.
     * 
     * @param participant the participant for whom we want to remove the polling
     */
    public void removeStatusListener(UpnpIOParticipant participant);
}
