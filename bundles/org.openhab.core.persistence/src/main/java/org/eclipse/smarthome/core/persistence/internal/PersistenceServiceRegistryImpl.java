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
package org.eclipse.smarthome.core.persistence.internal;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.core.ConfigOptionProvider;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.core.persistence.PersistenceService;
import org.eclipse.smarthome.core.persistence.PersistenceServiceRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This is a central service for accessing {@link PersistenceService}s. It is registered through DS and also provides
 * config options for the UI.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@Component(immediate = true, configurationPid = "org.eclipse.smarthome.persistence", property = {
        "service.pid:String=org.eclipse.smarthome.persistence",
        "service.config.description.uri:String=system:persistence", "service.config.label:String=Persistence",
        "service.config.category:String=system" })
public class PersistenceServiceRegistryImpl implements ConfigOptionProvider, PersistenceServiceRegistry {

    private final Map<String, PersistenceService> services = new HashMap<String, PersistenceService>();
    private String defaultServiceId = null;

    public PersistenceServiceRegistryImpl() {
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addPersistenceService(PersistenceService service) {
        services.put(service.getId(), service);
    }

    public void removePersistenceService(PersistenceService service) {
        services.remove(service.getId());
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        modified(config);
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        if (config != null) {
            defaultServiceId = (String) config.get("default");
        }
    }

    @Override
    public PersistenceService getDefault() {
        return get(getDefaultId());
    }

    @Override
    public PersistenceService get(String serviceId) {
        if (serviceId != null) {
            return services.get(serviceId);
        } else {
            return null;
        }
    }

    @Override
    public String getDefaultId() {
        if (defaultServiceId != null) {
            return defaultServiceId;
        } else {
            // if there is exactly one service available in the system, we assume that this should be used, if no
            // default is specifically configured.
            if (services.size() == 1) {
                return services.keySet().iterator().next();
            } else {
                return null;
            }
        }
    }

    @Override
    public Set<PersistenceService> getAll() {
        return new HashSet<>(services.values());
    }

    @Override
    public Collection<ParameterOption> getParameterOptions(URI uri, String param, Locale locale) {
        if (uri.toString().equals("system:persistence") && param.equals("default")) {
            Set<ParameterOption> options = new HashSet<>();
            for (PersistenceService service : getAll()) {
                options.add(new ParameterOption(service.getId(), service.getLabel(locale)));
            }
            return options;
        }
        return null;
    }

}
