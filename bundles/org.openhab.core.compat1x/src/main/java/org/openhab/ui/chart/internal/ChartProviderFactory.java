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
package org.openhab.ui.chart.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.openhab.ui.chart.ChartProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This class listens for services that implement the old chart provider service interface and registers
 * an according service for each under the new interface.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@Component(immediate = true)
public class ChartProviderFactory {

    private Map<String, ServiceRegistration<org.eclipse.smarthome.ui.chart.ChartProvider>> delegates = new HashMap<>();
    private BundleContext context;

    private Set<ChartProvider> chartProviders = new HashSet<>();

    @Activate
    public void activate(BundleContext context) {
        this.context = context;
        for (ChartProvider provider : chartProviders) {
            registerDelegateProvider(provider);
        }
    }

    @Deactivate
    public void deactivate() {
        for (ServiceRegistration<org.eclipse.smarthome.ui.chart.ChartProvider> serviceReg : delegates.values()) {
            serviceReg.unregister();
        }
        delegates.clear();
        this.context = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addChartProvider(ChartProvider provider) {
        if (context != null) {
            registerDelegateProvider(provider);
        } else {
            chartProviders.add(provider);
        }
    }

    public void removeChartProvider(ChartProvider provider) {
        if (context != null) {
            unregisterDelegateProvider(provider);
        }
    }

    private void registerDelegateProvider(ChartProvider chartProvider) {
        if (!delegates.containsKey(chartProvider.getName())) {
            ChartProviderDelegate service = new ChartProviderDelegate(chartProvider);
            Dictionary<String, Object> props = new Hashtable<>();
            ServiceRegistration<org.eclipse.smarthome.ui.chart.ChartProvider> serviceReg = context
                    .registerService(org.eclipse.smarthome.ui.chart.ChartProvider.class, service, props);
            delegates.put(chartProvider.getName(), serviceReg);
        }
    }

    private void unregisterDelegateProvider(ChartProvider chartProvider) {
        if (delegates.containsKey(chartProvider.getName())) {
            ServiceRegistration<org.eclipse.smarthome.ui.chart.ChartProvider> serviceReg = delegates
                    .get(chartProvider.getName());
            delegates.remove(chartProvider.getName());
            serviceReg.unregister();
        }
    }
}
