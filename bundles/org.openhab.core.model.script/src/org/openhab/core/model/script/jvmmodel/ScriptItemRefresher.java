/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemRegistryChangeListener;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.script.engine.action.ActionService;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.ReadyService.ReadyTracker;
import org.openhab.core.service.StartLevelService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
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
 * @author Laurent Garnier - delayed execution until start level 20 is reached
 */
@Component(service = {})
public class ScriptItemRefresher implements ItemRegistryChangeListener, ReadyTracker {

    // delay in seconds before script resources are refreshed after items or services have changed
    private static final long REFRESH_DELAY = 30;

    public static final String SCRIPTS_REFRESH_MARKER_TYPE = "scripts";
    public static final String SCRIPTS_REFRESH = "refresh";

    private final ReadyMarker MARKER = new ReadyMarker(SCRIPTS_REFRESH_MARKER_TYPE, SCRIPTS_REFRESH);

    private final Logger logger = LoggerFactory.getLogger(ScriptItemRefresher.class);

    private final ModelRepository modelRepository;
    private final ItemRegistry itemRegistry;
    private final ReadyService readyService;

    private final ScheduledExecutorService scheduler = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("scriptsRefresher"));

    private @Nullable ScheduledFuture<?> job;
    private boolean started;

    @Activate
    public ScriptItemRefresher(@Reference ModelRepository modelRepository, @Reference ItemRegistry itemRegistry,
            @Reference ReadyService readyService) {
        this.modelRepository = modelRepository;
        this.itemRegistry = itemRegistry;
        this.readyService = readyService;

        readyService.registerTracker(this, new ReadyMarkerFilter().withType(StartLevelService.STARTLEVEL_MARKER_TYPE)
                .withIdentifier(Integer.toString(StartLevelService.STARTLEVEL_MODEL)));
    }

    @Deactivate
    protected void deactivate() {
        ScheduledFuture<?> localJob = job;
        if (localJob != null && !localJob.isDone()) {
            localJob.cancel(false);
        }
        readyService.unregisterTracker(this);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addActionService(ActionService actionService) {
        if (started) {
            logger.debug("Script action added => scripts are going to be refreshed");
            scheduleScriptRefresh(REFRESH_DELAY);
        }
    }

    protected void removeActionService(ActionService actionService) {
        if (started) {
            logger.debug("Script action removed => scripts are going to be refreshed");
            scheduleScriptRefresh(REFRESH_DELAY);
        }
    }

    @Override
    public void added(Item element) {
        logger.debug("Item \"{}\" added => scripts are going to be refreshed", element.getName());
        scheduleScriptRefresh(REFRESH_DELAY);
    }

    @Override
    public void removed(Item element) {
        logger.debug("Item \"{}\" removed => scripts are going to be refreshed", element.getName());
        scheduleScriptRefresh(REFRESH_DELAY);
    }

    @Override
    public void updated(Item oldElement, Item element) {
    }

    @Override
    public void allItemsChanged(Collection<String> oldItemNames) {
        logger.debug("All items changed => scripts are going to be refreshed");
        scheduleScriptRefresh(REFRESH_DELAY);
    }

    private synchronized void scheduleScriptRefresh(long delay) {
        ScheduledFuture<?> localJob = job;
        if (localJob != null && !localJob.isDone()) {
            localJob.cancel(false);
        }
        job = scheduler.schedule(() -> {
            try {
                modelRepository.reloadAllModelsOfType("script");
            } catch (Exception e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
            }
            if (!started) {
                started = true;
                readyService.markReady(MARKER);
            }
        }, delay, TimeUnit.SECONDS);
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        scheduleScriptRefresh(0);
        itemRegistry.addRegistryChangeListener(this);
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        itemRegistry.removeRegistryChangeListener(this);
    }
}
