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
package org.openhab.core.io.transport.mdns;

/**
 * This interface defines how to use JmDNS based service discovery
 * to register and unregister services on Bonjour/MDNS
 *
 * @author Victor Belov - Initial contribution
 * @author Kai Kreuzer - Initial contribution
 */
public interface MDNSService {

    /**
     * This method registers a service to be announced through Bonjour/MDNS
     *
     * @param serviceDescription the {@link ServiceDescription} instance with all details to identify the service
     */
    public void registerService(ServiceDescription description);

    /**
     * This method unregisters a service not to be announced through Bonjour/MDNS
     *
     * @param serviceDescription the {@link ServiceDescription} instance with all details to identify the service
     */
    public void unregisterService(ServiceDescription description);

}
