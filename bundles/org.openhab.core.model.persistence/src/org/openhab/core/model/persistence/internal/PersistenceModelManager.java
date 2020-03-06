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
package org.openhab.core.model.persistence.internal;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.openhab.core.persistence.PersistenceManager;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.PersistenceServiceConfiguration;
import org.openhab.core.persistence.PersistenceFilter;
import org.openhab.core.persistence.PersistenceItemConfiguration;
import org.openhab.core.persistence.config.PersistenceAllConfig;
import org.openhab.core.persistence.config.PersistenceConfig;
import org.openhab.core.persistence.config.PersistenceGroupConfig;
import org.openhab.core.persistence.config.PersistenceItemConfig;
import org.openhab.core.persistence.strategy.PersistenceCronStrategy;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.openhab.core.model.core.EventType;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.core.ModelRepositoryChangeListener;
import org.openhab.core.model.persistence.persistence.AllConfig;
import org.openhab.core.model.persistence.persistence.CronStrategy;
import org.openhab.core.model.persistence.persistence.Filter;
import org.openhab.core.model.persistence.persistence.GroupConfig;
import org.openhab.core.model.persistence.persistence.ItemConfig;
import org.openhab.core.model.persistence.persistence.PersistenceConfiguration;
import org.openhab.core.model.persistence.persistence.PersistenceModel;
import org.openhab.core.model.persistence.persistence.Strategy;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This class is the central part of the persistence management and delegation. It reads the persistence
 * models, schedules timers and manages the invocation of {@link PersistenceService}s upon events.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Move non-model logic to core.persistence
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

    private List<PersistenceItemConfiguration> mapConfigs(List<PersistenceConfiguration> configs) {
        final List<PersistenceItemConfiguration> lst = new LinkedList<>();
        for (final PersistenceConfiguration config : configs) {
            lst.add(mapConfig(config));
        }
        return lst;
    }

    private PersistenceItemConfiguration mapConfig(PersistenceConfiguration config) {
        final List<PersistenceConfig> items = new LinkedList<>();
        for (final EObject item : config.getItems()) {
            if (item instanceof AllConfig) {
                items.add(new PersistenceAllConfig());
            } else if (item instanceof GroupConfig) {
                items.add(new PersistenceGroupConfig(((GroupConfig) item).getGroup()));
            } else if (item instanceof ItemConfig) {
                items.add(new PersistenceItemConfig(((ItemConfig) item).getItem()));
            }
        }
        return new PersistenceItemConfiguration(items, config.getAlias(), mapStrategies(config.getStrategies()),
                mapFilters(config.getFilters()));
    }

    private List<PersistenceStrategy> mapStrategies(List<Strategy> strategies) {
        final List<PersistenceStrategy> lst = new LinkedList<>();
        for (final Strategy strategy : strategies) {
            lst.add(mapStrategy(strategy));
        }
        return lst;
    }

    private PersistenceStrategy mapStrategy(Strategy strategy) {
        if (strategy instanceof CronStrategy) {
            return new PersistenceCronStrategy(strategy.getName(), ((CronStrategy) strategy).getCronExpression());
        } else {
            return new PersistenceStrategy(strategy.getName());
        }
    }

    private List<PersistenceFilter> mapFilters(List<Filter> filters) {
        final List<PersistenceFilter> lst = new LinkedList<>();
        for (final Filter filter : filters) {
            lst.add(mapFilter(filter));
        }
        return lst;
    }

    private PersistenceFilter mapFilter(Filter filter) {
        return new PersistenceFilter();
    }

}
