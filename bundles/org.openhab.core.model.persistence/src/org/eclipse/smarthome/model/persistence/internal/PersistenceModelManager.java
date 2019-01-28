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
package org.eclipse.smarthome.model.persistence.internal;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.smarthome.core.persistence.PersistenceManager;
import org.eclipse.smarthome.core.persistence.PersistenceService;
import org.eclipse.smarthome.core.persistence.PersistenceServiceConfiguration;
import org.eclipse.smarthome.core.persistence.SimpleFilter;
import org.eclipse.smarthome.core.persistence.SimpleItemConfiguration;
import org.eclipse.smarthome.core.persistence.config.SimpleAllConfig;
import org.eclipse.smarthome.core.persistence.config.SimpleConfig;
import org.eclipse.smarthome.core.persistence.config.SimpleGroupConfig;
import org.eclipse.smarthome.core.persistence.config.SimpleItemConfig;
import org.eclipse.smarthome.core.persistence.strategy.SimpleCronStrategy;
import org.eclipse.smarthome.core.persistence.strategy.SimpleStrategy;
import org.eclipse.smarthome.model.core.EventType;
import org.eclipse.smarthome.model.core.ModelRepository;
import org.eclipse.smarthome.model.core.ModelRepositoryChangeListener;
import org.eclipse.smarthome.model.persistence.persistence.AllConfig;
import org.eclipse.smarthome.model.persistence.persistence.CronStrategy;
import org.eclipse.smarthome.model.persistence.persistence.Filter;
import org.eclipse.smarthome.model.persistence.persistence.GroupConfig;
import org.eclipse.smarthome.model.persistence.persistence.ItemConfig;
import org.eclipse.smarthome.model.persistence.persistence.PersistenceConfiguration;
import org.eclipse.smarthome.model.persistence.persistence.PersistenceModel;
import org.eclipse.smarthome.model.persistence.persistence.Strategy;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This class is the central part of the persistence management and delegation. It reads the persistence
 * models, schedules timers and manages the invocation of {@link PersistenceService}s upon events.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Markus Rathgeb - Move non-model logic to core.persistence
 *
 */
@Component(immediate = true)
public class PersistenceModelManager implements ModelRepositoryChangeListener {

    private ModelRepository modelRepository;

    private PersistenceManager manager;

    public PersistenceModelManager() {
    }

    protected void activate() {
        modelRepository.addModelRepositoryChangeListener(this);
        for (String modelName : modelRepository.getAllModelNamesOfType("persist")) {
            addModel(modelName);
        }
    }

    protected void deactivate() {
        modelRepository.removeModelRepositoryChangeListener(this);
        for (String modelName : modelRepository.getAllModelNamesOfType("persist")) {
            removeModel(modelName);
        }
    }

    @Reference
    protected void setModelRepository(ModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    protected void unsetModelRepository(ModelRepository modelRepository) {
        this.modelRepository = null;
    }

    @Reference
    protected void setPersistenceManager(final PersistenceManager manager) {
        this.manager = manager;
    }

    protected void unsetPersistenceManager(final PersistenceManager manager) {
        this.manager = null;
    }

    @Override
    public void modelChanged(String modelName, EventType type) {
        if (modelName.endsWith(".persist")) {
            if (type == EventType.REMOVED || type == EventType.MODIFIED) {
                removeModel(modelName);
            }
            if (type == EventType.ADDED || type == EventType.MODIFIED) {
                addModel(modelName);
            }
        }
    }

    private void addModel(String modelName) {
        final PersistenceModel model = (PersistenceModel) modelRepository.getModel(modelName);
        if (model != null) {
            String serviceName = serviceName(modelName);
            manager.addConfig(serviceName, new PersistenceServiceConfiguration(mapConfigs(model.getConfigs()),
                    mapStrategies(model.getDefaults()), mapStrategies(model.getStrategies())));
        }
    }

    private void removeModel(String modelName) {
        String serviceName = serviceName(modelName);
        manager.removeConfig(serviceName);
    }

    private String serviceName(String modelName) {
        return modelName.substring(0, modelName.length() - ".persist".length());
    }

    private List<SimpleItemConfiguration> mapConfigs(List<PersistenceConfiguration> configs) {
        final List<SimpleItemConfiguration> lst = new LinkedList<>();
        for (final PersistenceConfiguration config : configs) {
            lst.add(mapConfig(config));
        }
        return lst;
    }

    private SimpleItemConfiguration mapConfig(PersistenceConfiguration config) {
        final List<SimpleConfig> items = new LinkedList<>();
        for (final EObject item : config.getItems()) {
            if (item instanceof AllConfig) {
                items.add(new SimpleAllConfig());
            } else if (item instanceof GroupConfig) {
                items.add(new SimpleGroupConfig(((GroupConfig) item).getGroup()));
            } else if (item instanceof ItemConfig) {
                items.add(new SimpleItemConfig(((ItemConfig) item).getItem()));
            }
        }
        return new SimpleItemConfiguration(items, config.getAlias(), mapStrategies(config.getStrategies()),
                mapFilters(config.getFilters()));
    }

    private List<SimpleStrategy> mapStrategies(List<Strategy> strategies) {
        final List<SimpleStrategy> lst = new LinkedList<>();
        for (final Strategy strategy : strategies) {
            lst.add(mapStrategy(strategy));
        }
        return lst;
    }

    private SimpleStrategy mapStrategy(Strategy strategy) {
        if (strategy instanceof CronStrategy) {
            return new SimpleCronStrategy(strategy.getName(), ((CronStrategy) strategy).getCronExpression());
        } else {
            return new SimpleStrategy(strategy.getName());
        }
    }

    private List<SimpleFilter> mapFilters(List<Filter> filters) {
        final List<SimpleFilter> lst = new LinkedList<>();
        for (final Filter filter : filters) {
            lst.add(mapFilter(filter));
        }
        return lst;
    }

    private SimpleFilter mapFilter(Filter filter) {
        return new SimpleFilter();
    }

}
