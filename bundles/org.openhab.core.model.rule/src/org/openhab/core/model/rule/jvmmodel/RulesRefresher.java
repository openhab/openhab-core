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
package org.openhab.core.model.rule.jvmmodel;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
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
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingRegistryChangeListener;
import org.openhab.core.thing.binding.ThingActions;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RulesRefresher} is responsible for reloading rules resources every time.
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Kai Kreuzer - added delayed execution
 * @author Maoliang Huang - refactor
 */
@Component(immediate = true, service = {})
@NonNullByDefault
public class RulesRefresher implements ReadyTracker {

    // delay in seconds before rule resources are refreshed after items or services have changed
    private static final long REFRESH_DELAY = 30;

    public static final String RULES_REFRESH_MARKER_TYPE = "rules";
    public static final String RULES_REFRESH = "refresh";

    private final Logger logger = LoggerFactory.getLogger(RulesRefresher.class);

    private @Nullable ScheduledFuture<?> job;
    private final ScheduledExecutorService scheduler = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("rulesRefresher"));
    private boolean started;
    private final ReadyMarker marker = new ReadyMarker("rules", RULES_REFRESH);

    private final ModelRepository modelRepository;
    private final ItemRegistry itemRegistry;
    private final ThingRegistry thingRegistry;
    private final ReadyService readyService;

    private final ItemRegistryChangeListener itemRegistryChangeListener = new ItemRegistryChangeListener() {
        @Override
        public void added(Item element) {
            logger.debug("Item \"{}\" added => rules are going to be refreshed", element.getName());
            scheduleRuleRefresh(REFRESH_DELAY);
        }

        @Override
        public void removed(Item element) {
            logger.debug("Item \"{}\" removed => rules are going to be refreshed", element.getName());
            scheduleRuleRefresh(REFRESH_DELAY);
        }

        @Override
        public void updated(Item oldElement, Item element) {
        }

        @Override
        public void allItemsChanged(Collection<String> oldItemNames) {
            logger.debug("All items changed => rules are going to be refreshed");
            scheduleRuleRefresh(REFRESH_DELAY);
        }
    };

    private final ThingRegistryChangeListener thingRegistryChangeListener = new ThingRegistryChangeListener() {
        @Override
        public void added(Thing element) {
            logger.debug("Thing \"{}\" added => rules are going to be refreshed", element.getUID().getAsString());
            scheduleRuleRefresh(REFRESH_DELAY);
        }

        @Override
        public void removed(Thing element) {
            logger.debug("Thing \"{}\" removed => rules are going to be refreshed", element.getUID().getAsString());
            scheduleRuleRefresh(REFRESH_DELAY);
        }

        @Override
        public void updated(Thing oldElement, Thing element) {
        }
    };

    @Activate
    public RulesRefresher(@Reference ModelRepository modelRepository, @Reference ItemRegistry itemRegistry,
            @Reference ThingRegistry thingRegistry, @Reference ReadyService readyService) {
        this.modelRepository = modelRepository;
        this.itemRegistry = itemRegistry;
        this.thingRegistry = thingRegistry;
        this.readyService = readyService;
    }

    @Activate
    protected void activate() {
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(StartLevelService.STARTLEVEL_MARKER_TYPE)
                .withIdentifier(Integer.toString(StartLevelService.STARTLEVEL_MODEL)));
    }

    @Deactivate
    protected void deactivate() {
        readyService.unregisterTracker(this);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addActionService(ActionService actionService) {
        if (started) {
            logger.debug("Script action added => rules are going to be refreshed");
            scheduleRuleRefresh(REFRESH_DELAY);
        }
    }

    protected void removeActionService(ActionService actionService) {
        if (started) {
            logger.debug("Script action removed => rules are going to be refreshed");
            scheduleRuleRefresh(REFRESH_DELAY);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addThingActions(ThingActions thingActions) {
        if (started) {
            logger.debug("Thing automation action added => rules are going to be refreshed");
            scheduleRuleRefresh(REFRESH_DELAY);
        }
    }

    protected void removeThingActions(ThingActions thingActions) {
        if (started) {
            logger.debug("Thing automation action removed => rules are going to be refreshed");
            scheduleRuleRefresh(REFRESH_DELAY);
        }
    }

    protected synchronized void scheduleRuleRefresh(long delay) {
        ScheduledFuture<?> localJob = job;
        if (localJob != null && !localJob.isDone()) {
            localJob.cancel(false);
        }
        job = scheduler.schedule(() -> {
            try {
                modelRepository.reloadAllModelsOfType("rules");
            } catch (Exception e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
            }
            if (!started) {
                started = true;
                readyService.markReady(marker);
            }
        }, delay, TimeUnit.SECONDS);
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        scheduleRuleRefresh(0);
        itemRegistry.addRegistryChangeListener(itemRegistryChangeListener);
        thingRegistry.addRegistryChangeListener(thingRegistryChangeListener);
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        itemRegistry.removeRegistryChangeListener(itemRegistryChangeListener);
        thingRegistry.removeRegistryChangeListener(thingRegistryChangeListener);
    }
}
