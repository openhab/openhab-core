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
package org.openhab.core.io.transport.upnp;

/**
 * The {@link UpnpIOParticipant} is an interface that needs to
 * be implemented by classes that wants to participate in
 * UPNP communication
 *
 * @author Karel Goderis - Initial contribution
 */
public interface UpnpIOParticipant {

    /** Get the UDN of the participant **/
    public String getUDN();

    /** Called when the UPNP IO service receives a {variable,value} tuple for the given UPNP service **/
    public void onValueReceived(String variable, String value, String service);

    /**
     * Called to notify if a GENA subscription succeeded or failed.
     *
     * @param service the UPnP service subscribed
     * @param succeeded true if the subscription succeeded; false if failed
     */
    public void onServiceSubscribed(String service, boolean succeeded);

    /**
     * Called when the UPNP IO service is unable to poll the UDN of the participant, given that
     * a addStatusListener is registered.
     *
     * @param status false, if the poll fails when the polling was previously successful; true if the poll succeeds
     *            when the polling was previously failing
     */
    public void onStatusChanged(boolean status);

}
