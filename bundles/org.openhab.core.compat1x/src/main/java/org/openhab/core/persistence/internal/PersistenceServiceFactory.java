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
package org.openhab.core.persistence.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.openhab.core.persistence.PersistenceService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This class listens for services that implement the old persistence service interface and registers
 * an according service for each under the new interface.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@Component(immediate = true)
public class PersistenceServiceFactory {

    private Map<String, ServiceRegistration<org.eclipse.smarthome.core.persistence.PersistenceService>> delegates = new HashMap<>();
    private BundleContext context;

    private Set<PersistenceService> persistenceServices = new HashSet<>();

    @Activate
    public void activate(BundleContext context) {
        this.context = context;
        for (PersistenceService service : persistenceServices) {
            registerDelegateService(service);
        }
    }

    @Deactivate
    public void deactivate() {
        for (ServiceRegistration<org.eclipse.smarthome.core.persistence.PersistenceService> serviceReg : delegates
                .values()) {
            serviceReg.unregister();
        }
        delegates.clear();
        this.context = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addPersistenceService(PersistenceService service) {
        if (context != null) {
            registerDelegateService(service);
        } else {
            persistenceServices.add(service);
        }
    }

    public void removePersistenceService(PersistenceService service) {
        if (context != null) {
            unregisterDelegateService(service);
        }
    }

    private void registerDelegateService(PersistenceService persistenceService) {
        if (!delegates.containsKey(persistenceService.getName())) {
            org.eclipse.smarthome.core.persistence.PersistenceService service = (persistenceService instanceof org.openhab.core.persistence.QueryablePersistenceService)
                    ? new QueryablePersistenceServiceDelegate(persistenceService)
                    : new PersistenceServiceDelegate(persistenceService);
            Dictionary<String, Object> props = new Hashtable<>();
            ServiceRegistration<org.eclipse.smarthome.core.persistence.PersistenceService> serviceReg = context
                    .registerService(org.eclipse.smarthome.core.persistence.PersistenceService.class, service, props);
            delegates.put(persistenceService.getName(), serviceReg);
        }
    }

    private void unregisterDelegateService(PersistenceService service) {
        if (delegates.containsKey(service.getName())) {
            ServiceRegistration<org.eclipse.smarthome.core.persistence.PersistenceService> serviceReg = delegates
                    .get(service.getName());
            delegates.remove(service.getName());
            serviceReg.unregister();
        }
    }
}
