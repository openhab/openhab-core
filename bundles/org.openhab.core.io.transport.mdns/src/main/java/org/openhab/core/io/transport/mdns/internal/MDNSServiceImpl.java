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
package org.openhab.core.io.transport.mdns.internal;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.mdns.MDNSClient;
import org.openhab.core.io.transport.mdns.MDNSService;
import org.openhab.core.io.transport.mdns.ServiceDescription;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class starts the JmDNS and implements interface to register and
 * unregister services.
 *
 * @author Victor Belov - Initial contribution
 */
@Component(immediate = true)
public class MDNSServiceImpl implements MDNSService {
    private final Logger logger = LoggerFactory.getLogger(MDNSServiceImpl.class);

    private @Nullable MDNSClient mdnsClient;

    private final Set<ServiceDescription> servicesToRegisterQueue = new CopyOnWriteArraySet<>();

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setMDNSClient(MDNSClient client) {
        this.mdnsClient = client;
        // register queued services
        if (!servicesToRegisterQueue.isEmpty()) {
            Executors.newSingleThreadExecutor().execute(() -> {
                logger.debug("Registering {} queued services", servicesToRegisterQueue.size());
                for (ServiceDescription description : servicesToRegisterQueue) {
                    try {
                        MDNSClient localClient = mdnsClient;
                        if (localClient != null) {
                            localClient.registerService(description);
                        } else {
                            break;
                        }
                    } catch (IOException e) {
                        logger.error("{}", e.getMessage());
                    } catch (IllegalStateException e) {
                        logger.debug("Not registering service {}, because service is already deactivated!",
                                description.serviceType);
                    }
                }
                servicesToRegisterQueue.clear();
            });
        }
    }

    protected void unsetMDNSClient(MDNSClient mdnsClient) {
        this.mdnsClient = null;
        mdnsClient.unregisterAllServices();
    }

    @Override
    public void registerService(final ServiceDescription description) {
        MDNSClient localClient = mdnsClient;
        if (localClient == null) {
            // queue the service to register it as soon as the mDNS client is available
            servicesToRegisterQueue.add(description);
        } else {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    localClient.registerService(description);
                } catch (IOException e) {
                    logger.error("{}", e.getMessage());
                } catch (IllegalStateException e) {
                    logger.debug("Not registering service {}, because service is already deactivated!",
                            description.serviceType);
                }
            });
        }
    }

    @Override
    public void unregisterService(ServiceDescription description) {
        if (mdnsClient != null) {
            mdnsClient.unregisterService(description);
        }
    }

    /**
     * This method unregisters all services from Bonjour/MDNS
     */
    protected void unregisterAllServices() {
        if (mdnsClient != null) {
            mdnsClient.unregisterAllServices();
        }
    }

    @Deactivate
    public void deactivate() {
        unregisterAllServices();
        if (mdnsClient != null) {
            mdnsClient.close();
            logger.debug("mDNS service has been stopped");
        }
    }
}
