/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.rest.mdns.internal;

import java.util.Hashtable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.net.HttpServiceUtil;
import org.eclipse.smarthome.io.rest.RESTService;
import org.eclipse.smarthome.io.transport.mdns.MDNSService;
import org.eclipse.smarthome.io.transport.mdns.ServiceDescription;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * This class announces the REST API through mDNS for clients to automatically
 * discover it.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Markus Rathgeb - Use HTTP service utility functions
 */
@Component(immediate = true, configurationPid = "org.eclipse.smarthome.mdns", property = {
        Constants.SERVICE_PID + "=org.eclipse.smarthome.mdns" //
})
@NonNullByDefault
public class MDNSAnnouncer {

    private int httpSSLPort;

    private int httpPort;

    @Reference
    private @NonNullByDefault({}) MDNSService mdnsService;

    @Reference
    private @NonNullByDefault({}) RESTService restService;

    private @NonNullByDefault({}) Config config;

    @ObjectClassDefinition(description = "%cors_description", name = "%cors_name")
    @interface Config {
        String mdnsName() default "smarthome";

        boolean enabled() default true;
    }

    @Activate
    public void activate(BundleContext bundleContext, Config config) {
        this.config = config;
        if (!config.enabled()) {
            return;
        }

        try {
            httpPort = HttpServiceUtil.getHttpServicePort(bundleContext);
            if (httpPort != -1) {
                mdnsService.registerService(getDefaultServiceDescription());
            }
        } catch (NumberFormatException e) {
        }
        try {
            httpSSLPort = HttpServiceUtil.getHttpServicePortSecure(bundleContext);
            if (httpSSLPort != -1) {
                mdnsService.registerService(getSSLServiceDescription());
            }
        } catch (NumberFormatException e) {
        }
    }

    @Deactivate
    public void deactivate() {
        mdnsService.unregisterService(getDefaultServiceDescription());
        mdnsService.unregisterService(getSSLServiceDescription());
    }

    private ServiceDescription getDefaultServiceDescription() {
        Hashtable<String, String> serviceProperties = new Hashtable<String, String>();
        serviceProperties.put("uri", restService.getRESTroot());
        return new ServiceDescription("_" + config.mdnsName() + "-server._tcp.local.", config.mdnsName(), httpPort,
                serviceProperties);
    }

    private ServiceDescription getSSLServiceDescription() {
        ServiceDescription description = getDefaultServiceDescription();
        description.serviceType = "_" + config.mdnsName() + "-server-ssl._tcp.local.";
        description.serviceName = "" + config.mdnsName() + "-ssl";
        description.servicePort = httpSSLPort;
        return description;
    }
}
