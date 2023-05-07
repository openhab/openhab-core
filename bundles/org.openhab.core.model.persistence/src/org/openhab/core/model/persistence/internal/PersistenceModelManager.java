/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractProvider;
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
import org.openhab.core.model.persistence.persistence.ThresholdFilter;
import org.openhab.core.model.persistence.persistence.TimeFilter;
import org.openhab.core.persistence.PersistenceItemConfiguration;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.config.PersistenceAllConfig;
import org.openhab.core.persistence.config.PersistenceConfig;
import org.openhab.core.persistence.config.PersistenceGroupConfig;
import org.openhab.core.persistence.config.PersistenceItemConfig;
import org.openhab.core.persistence.filter.PersistenceFilter;
import org.openhab.core.persistence.filter.PersistenceThresholdFilter;
import org.openhab.core.persistence.filter.PersistenceTimeFilter;
import org.openhab.core.persistence.registry.PersistenceServiceConfiguration;
import org.openhab.core.persistence.registry.PersistenceServiceConfigurationProvider;
import org.openhab.core.persistence.strategy.PersistenceCronStrategy;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * This class is the central part of the persistence management and delegation. It reads the persistence
 * models, schedules timers and manages the invocation of {@link PersistenceService}s upon events.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Move non-model logic to core.persistence
 * @author Jan N. Klug - Refactored to {@link PersistenceServiceConfigurationProvider}
 */
@Component(immediate = true, service = PersistenceServiceConfigurationProvider.class)
@NonNullByDefault
public class PersistenceModelManager extends AbstractProvider<PersistenceServiceConfiguration>
        implements ModelRepositoryChangeListener, PersistenceServiceConfigurationProvider {
    private final Map<String, PersistenceServiceConfiguration> configurations = new ConcurrentHashMap<>();
    private final ModelRepository modelRepository;

    @Activate
    public PersistenceModelManager(@Reference ModelRepository modelRepository) {
        this.modelRepository = modelRepository;

        modelRepository.addModelRepositoryChangeListener(this);
        modelRepository.getAllModelNamesOfType("persist")
                .forEach(modelName -> modelChanged(modelName, EventType.ADDED));
    }

    @Deactivate
    protected void deactivate() {
        modelRepository.removeModelRepositoryChangeListener(this);
        modelRepository.getAllModelNamesOfType("persist")
                .forEach(modelName -> modelChanged(modelName, EventType.REMOVED));
    }

    @Override
    public void modelChanged(String modelName, EventType type) {
        if (modelName.endsWith(".persist")) {
            String serviceName = serviceName(modelName);
            if (type == EventType.REMOVED) {
                PersistenceServiceConfiguration removed = configurations.remove(serviceName);
                notifyListenersAboutRemovedElement(removed);
            } else {
                final PersistenceModel model = (PersistenceModel) modelRepository.getModel(modelName);

                if (model != null) {
                    PersistenceServiceConfiguration newConfiguration = new PersistenceServiceConfiguration(serviceName,
                            mapConfigs(model.getConfigs()), mapStrategies(model.getDefaults()),
                            mapStrategies(model.getStrategies()), mapFilters(model.getFilters()));
                    PersistenceServiceConfiguration oldConfiguration = configurations.put(serviceName,
                            newConfiguration);
                    if (oldConfiguration == null) {
                        if (type != EventType.ADDED) {
                            logger.warn(
                                    "Model {} is inconsistent: An updated event was sent, but there is no old configuration. Adding it now.",
                                    modelName);
                        }
                        notifyListenersAboutAddedElement(newConfiguration);
                    } else {
                        if (type != EventType.MODIFIED) {
                            logger.warn(
                                    "Model {} is inconsistent: An added event was sent, but there is an old configuration. Replacing it now.",
                                    modelName);
                        }
                        notifyListenersAboutUpdatedElement(oldConfiguration, newConfiguration);
                    }
                } else {
                    logger.error(
                            "The model repository reported a {} event for model '{}' but the model could not be found in the repository. ",
                            type, modelName);
                }
            }
        }
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
            } else if (item instanceof GroupConfig groupConfig) {
                items.add(new PersistenceGroupConfig(groupConfig.getGroup()));
            } else if (item instanceof ItemConfig itemConfig) {
                items.add(new PersistenceItemConfig(itemConfig.getItem()));
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
        if (strategy instanceof CronStrategy cronStrategy) {
            return new PersistenceCronStrategy(strategy.getName(), cronStrategy.getCronExpression());
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
        if (filter.getDefinition() instanceof TimeFilter) {
            TimeFilter timeFilter = (TimeFilter) filter.getDefinition();
            return new PersistenceTimeFilter(filter.getName(), timeFilter.getValue(), timeFilter.getUnit());
        } else if (filter.getDefinition() instanceof ThresholdFilter) {
            ThresholdFilter thresholdFilter = (ThresholdFilter) filter.getDefinition();
            return new PersistenceThresholdFilter(filter.getName(), thresholdFilter.getValue(),
                    thresholdFilter.getUnit());
        }
        throw new IllegalArgumentException("Unknown filter type " + filter.getClass());
    }

    @Override
    public Collection<PersistenceServiceConfiguration> getAll() {
        return List.copyOf(configurations.values());
    }
}
