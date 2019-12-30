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
package org.openhab.core.model.script.jvmmodel;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemRegistryChangeListener;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.script.engine.action.ActionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ScriptItemRefresher} is responsible for reloading script resources every time an item is added or removed.
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Kai Kreuzer - added delayed execution
 */
@Component(service = ScriptItemRefresher.class, immediate = true)
public class ScriptItemRefresher implements ItemRegistryChangeListener {

    private final Logger logger = LoggerFactory.getLogger(ScriptItemRefresher.class);

    // delay before rule resources are refreshed after items or services have changed
    private static final long REFRESH_DELAY = 2000;

    ModelRepository modelRepository;
    private ItemRegistry itemRegistry;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> job;

    @Reference
    public void setModelRepository(ModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    public void unsetModelRepository(ModelRepository modelRepository) {
        if (job != null && !job.isDone()) {
            job.cancel(false);
        }
        this.modelRepository = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
        this.itemRegistry.addRegistryChangeListener(this);
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry.removeRegistryChangeListener(this);
        this.itemRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addActionService(ActionService actionService) {
        scheduleScriptRefresh();
    }

    protected void removeActionService(ActionService actionService) {
        scheduleScriptRefresh();
    }

    @Override
    public void added(Item element) {
        scheduleScriptRefresh();
    }

    @Override
    public void removed(Item element) {
        scheduleScriptRefresh();
    }

    @Override
    public void updated(Item oldElement, Item element) {

    }

    @Override
    public void allItemsChanged(Collection<String> oldItemNames) {
        scheduleScriptRefresh();
    }

    private synchronized void scheduleScriptRefresh() {
        if (job != null && !job.isDone()) {
            job.cancel(false);
        }
        job = scheduler.schedule(runnable, REFRESH_DELAY, TimeUnit.MILLISECONDS);
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                modelRepository.reloadAllModelsOfType("script");
            } catch (Exception e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
            }
        }
    };
}
