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
package org.openhab.core.binding.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.openhab.model.item.binding.BindingConfigReader;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This class listens for services that implement the old binding config reader interface and registers
 * an according service for each under the new interface.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@Component
public class BindingConfigReaderFactory {

    private Map<String, ServiceRegistration<org.eclipse.smarthome.model.item.BindingConfigReader>> delegates = new HashMap<>();
    private BundleContext context;

    private Set<BindingConfigReader> readers = new HashSet<>();

    @Activate
    public void activate(BundleContext context) {
        this.context = context;
        for (BindingConfigReader reader : readers) {
            registerDelegateService(reader);
        }
    }

    @Deactivate
    public void deactivate() {
        for (ServiceRegistration<org.eclipse.smarthome.model.item.BindingConfigReader> serviceReg : delegates
                .values()) {
            serviceReg.unregister();
        }
        delegates.clear();
        this.context = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addBindingConfigReader(BindingConfigReader reader) {
        if (context != null) {
            registerDelegateService(reader);
        } else {
            readers.add(reader);
        }
    }

    public void removeBindingConfigReader(BindingConfigReader reader) {
        if (context != null) {
            unregisterDelegateService(reader);
        }
    }

    private void registerDelegateService(BindingConfigReader reader) {
        if (!delegates.containsKey(reader.getBindingType())) {
            BindingConfigReaderDelegate service = new BindingConfigReaderDelegate(reader);
            Dictionary<String, Object> props = new Hashtable<>();
            ServiceRegistration<org.eclipse.smarthome.model.item.BindingConfigReader> serviceReg = context
                    .registerService(org.eclipse.smarthome.model.item.BindingConfigReader.class, service, props);
            delegates.put(reader.getBindingType(), serviceReg);
        }
    }

    private void unregisterDelegateService(BindingConfigReader reader) {
        if (delegates.containsKey(reader.getBindingType())) {
            ServiceRegistration<org.eclipse.smarthome.model.item.BindingConfigReader> serviceReg = delegates
                    .get(reader.getBindingType());
            delegates.remove(reader.getBindingType());
            serviceReg.unregister();
        }
    }
}
