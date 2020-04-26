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
package org.openhab.core.persistence.internal;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.PersistenceServiceRegistry;
import org.osgi.framework.Constants;
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
 * @author Kai Kreuzer - Initial contribution
 */
@Component(immediate = true, configurationPid = "org.openhab.persistence", property = {
        Constants.SERVICE_PID + "=org.openhab.persistence", ConfigurableService.SERVICE_PROPERTY_CATEGORY + "=system",
        ConfigurableService.SERVICE_PROPERTY_LABEL + "=Persistence",
        ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI + "=" + PersistenceServiceRegistryImpl.CONFIG_URI })
@NonNullByDefault
public class PersistenceServiceRegistryImpl implements ConfigOptionProvider, PersistenceServiceRegistry {

    // constants for the configuration properties
    protected static final String CONFIG_URI = "system:persistence";
    private static final String CONFIG_DEFAULT = "default";

    private final Map<String, PersistenceService> persistenceServices = new HashMap<>();
    private @Nullable String defaultServiceId;

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addPersistenceService(PersistenceService persistenceService) {
        persistenceServices.put(persistenceService.getId(), persistenceService);
    }

    public void removePersistenceService(PersistenceService persistenceService) {
        persistenceServices.remove(persistenceService.getId());
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        modified(config);
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        defaultServiceId = (String) config.get(CONFIG_DEFAULT);
    }

    @Override
    public @Nullable PersistenceService getDefault() {
        return get(getDefaultId());
    }

    @Override
    public @Nullable PersistenceService get(@Nullable String serviceId) {
        return (serviceId != null) ? persistenceServices.get(serviceId) : null;
    }

    @Override
    public @Nullable String getDefaultId() {
        if (defaultServiceId != null) {
            return defaultServiceId;
        } else {
            // if there is exactly one service available in the system, we assume that this should be used, if no
            // default is specifically configured.
            return (persistenceServices.size() == 1) ? persistenceServices.keySet().iterator().next() : null;
        }
    }

    @Override
    public Set<PersistenceService> getAll() {
        return new HashSet<>(persistenceServices.values());
    }

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable Locale locale) {
        if (CONFIG_URI.equals(uri.toString()) && CONFIG_DEFAULT.equals(param)) {
            Set<ParameterOption> options = new HashSet<>();
            for (PersistenceService service : getAll()) {
                options.add(new ParameterOption(service.getId(), service.getLabel(locale)));
            }
            return options;
        }
        return null;
    }
}
