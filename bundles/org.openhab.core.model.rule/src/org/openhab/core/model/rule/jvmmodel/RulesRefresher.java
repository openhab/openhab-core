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
package org.openhab.core.model.rule.jvmmodel;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.system.StartlevelEvent;
import org.openhab.core.events.system.SystemEventFactory;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemRegistryChangeListener;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.script.engine.action.ActionService;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.ReadyService.ReadyTracker;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingRegistryChangeListener;
import org.openhab.core.thing.binding.ThingActions;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
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
    private static final long REFRESH_DELAY = 5;

    private static final String POOL_NAME = "automation";
    public static final String RULES_REFRESH = "rules_refresh";

    private final Logger logger = LoggerFactory.getLogger(RulesRefresher.class);

    private @Nullable ScheduledFuture<?> job;
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(POOL_NAME);
    private boolean started;
    private final ReadyMarker marker = new ReadyMarker("dsl", RULES_REFRESH);

    private final ModelRepository modelRepository;
    private final ItemRegistry itemRegistry;
    private final ThingRegistry thingRegistry;
    private final EventPublisher eventPublisher;
    private final ReadyService readyService;

    private final ItemRegistryChangeListener itemRegistryChangeListener = new ItemRegistryChangeListener() {
        @Override
        public void added(Item element) {
            scheduleRuleRefresh();
        }

        @Override
        public void removed(Item element) {
            scheduleRuleRefresh();
        }

        @Override
        public void updated(Item oldElement, Item element) {
        }

        @Override
        public void allItemsChanged(Collection<String> oldItemNames) {
            scheduleRuleRefresh();
        }
    };

    private final ThingRegistryChangeListener thingRegistryChangeListener = new ThingRegistryChangeListener() {
        @Override
        public void added(Thing element) {
            scheduleRuleRefresh();
        }

        @Override
        public void removed(Thing element) {
            scheduleRuleRefresh();
        }

        @Override
        public void updated(Thing oldElement, Thing element) {
        }
    };

    @Activate
    public RulesRefresher(@Reference ModelRepository modelRepository, @Reference ItemRegistry itemRegistry,
            @Reference ThingRegistry thingRegistry, @Reference EventPublisher eventPublisher,
            @Reference ReadyService readyService) {
        this.modelRepository = modelRepository;
        this.itemRegistry = itemRegistry;
        this.thingRegistry = thingRegistry;
        this.eventPublisher = eventPublisher;
        this.readyService = readyService;
    }

    @Activate
    protected void activate() {
        readyService.registerTracker(this, new ReadyMarkerFilter().withType("dsl").withIdentifier("rules"));
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addActionService(ActionService actionService) {
        if (started) {
            scheduleRuleRefresh();
        }
    }

    protected void removeActionService(ActionService actionService) {
        if (started) {
            scheduleRuleRefresh();
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addThingActions(ThingActions thingActions) {
        if (started) {
            scheduleRuleRefresh();
        }
    }

    protected void removeThingActions(ThingActions thingActions) {
        if (started) {
            scheduleRuleRefresh();
        }
    }

    protected synchronized void scheduleRuleRefresh() {
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
            readyService.markReady(marker);
        }, REFRESH_DELAY, TimeUnit.SECONDS);
        readyService.unmarkReady(marker);
    }

    private void setStartLevel() {
        if (!started) {
            started = true;
            // TODO: This is still a very dirty hack in the absence of a proper system start level management.
            scheduler.schedule(() -> {
                StartlevelEvent startlevelEvent = SystemEventFactory.createStartlevelEvent(20);
                eventPublisher.post(startlevelEvent);
            }, 15, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        itemRegistry.addRegistryChangeListener(itemRegistryChangeListener);
        thingRegistry.addRegistryChangeListener(thingRegistryChangeListener);
        setStartLevel();
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
    }
}
