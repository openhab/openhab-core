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

import java.util.Hashtable;

/**
 * This is a simple data container to keep all details of a service description together.
 * 
 * @author Kai Kreuzer - Initial contribution
 */
public class ServiceDescription {

    public String serviceType;
    public String serviceName;
    public int servicePort;
    public Hashtable<String, String> serviceProperties;

    /**
     * Constructor for a {@link ServiceDescription}, which takes all details as parameters
     * 
     * @param serviceType String service type, like "_smarthome-server._tcp.local."
     * @param serviceName String service name, like "openHAB"
     * @param servicePort Int service port, like 8080
     * @param serviceProperties Hashtable service props, like url = "/rest"
     * @param serviceDescription String service description text, like "openHAB REST interface"
     */
    public ServiceDescription(String serviceType, String serviceName, int servicePort,
            Hashtable<String, String> serviceProperties) {
        this.serviceType = serviceType;
        this.serviceName = serviceName;
        this.servicePort = servicePort;
        this.serviceProperties = serviceProperties;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
        result = prime * result + servicePort;
        result = prime * result + ((serviceType == null) ? 0 : serviceType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ServiceDescription other = (ServiceDescription) obj;
        if (serviceName == null) {
            if (other.serviceName != null)
                return false;
        } else if (!serviceName.equals(other.serviceName))
            return false;
        if (servicePort != other.servicePort)
            return false;
        if (serviceType == null) {
            if (other.serviceType != null)
                return false;
        } else if (!serviceType.equals(other.serviceType))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ServiceDescription [serviceType=" + serviceType + ", serviceName=" + serviceName + ", servicePort="
                + servicePort + "]";
    }
}
