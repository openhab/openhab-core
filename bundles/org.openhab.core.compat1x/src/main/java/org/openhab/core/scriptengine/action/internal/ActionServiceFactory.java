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
package org.openhab.core.scriptengine.action.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.openhab.core.scriptengine.action.ActionService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This class listens for services that implement the old action service interface and registers
 * an according service for each under the new interface.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@Component
public class ActionServiceFactory {

    private Map<String, ServiceRegistration<org.eclipse.smarthome.model.script.engine.action.ActionService>> delegates = new HashMap<>();
    private BundleContext context;

    private Set<ActionService> actionServices = new HashSet<>();

    @Activate
    public void activate(BundleContext context) {
        this.context = context;
        for (ActionService service : actionServices) {
            registerDelegateService(service);
        }
    }

    @Deactivate
    public void deactivate() {
        for (ServiceRegistration<org.eclipse.smarthome.model.script.engine.action.ActionService> serviceReg : delegates
                .values()) {
            serviceReg.unregister();
        }
        delegates.clear();
        this.context = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addActionService(ActionService service) {
        if (context != null) {
            registerDelegateService(service);
        } else {
            actionServices.add(service);
        }
    }

    public void removeActionService(ActionService service) {
        if (context != null) {
            unregisterDelegateService(service);
        }
    }

    private void registerDelegateService(ActionService actionService) {
        if (!delegates.containsKey(actionService.getActionClassName())) {
            ActionServiceDelegate service = new ActionServiceDelegate(actionService);
            Dictionary<String, Object> props = new Hashtable<>();
            ServiceRegistration<org.eclipse.smarthome.model.script.engine.action.ActionService> serviceReg = context
                    .registerService(org.eclipse.smarthome.model.script.engine.action.ActionService.class, service,
                            props);
            delegates.put(actionService.getActionClassName(), serviceReg);
        }
    }

    private void unregisterDelegateService(ActionService service) {
        if (delegates.containsKey(service.getActionClassName())) {
            ServiceRegistration<org.eclipse.smarthome.model.script.engine.action.ActionService> serviceReg = delegates
                    .get(service.getActionClassName());
            delegates.remove(service.getActionClassName());
            serviceReg.unregister();
        }
    }
}
