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
package org.openhab.core.service;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.system.StartlevelEvent;
import org.openhab.core.events.system.SystemEventFactory;
import org.openhab.core.internal.common.WrappedScheduledExecutorService;
import org.openhab.core.service.ReadyService.ReadyTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service combines different {@link ReadyMarker}s into a new start level ready marker and thus
 * lets other services depend on those, without having to know about the single markers.
 * This brings an important decoupling, since the set of markers for a certain start level might
 * depend on the individual set up-
 * The start level service is therefore configurable, so that users have a chance to adapt the
 * conditions upon a certain start level is reached.
 *
 * Start levels are defined as values between 0 and 100. They carry the following semantics:
 *
 * 00 - OSGi framework has been started.
 * 10 - OSGi application start level has been reached, i.e. bundles are activated.
 * 20 - Model entities (items, things, links, persist config) have been loaded, both from db as well as files.
 * 30 - Item states have been restored from persistence service, where applicable.
 * 40 - Rules are loaded and parsed, both from db as well as dsl and script files.
 * 50 - Rule engine has executed all "system started" rules and is active.
 * 70 - User interface is up and running.
 * 80 - All things have been initialized.
 * 100 - Startup is fully complete.
 *
 * @author Kai Kreuzer - Initial contribution
 *
 */
@NonNullByDefault
@Component(immediate = true, service = StartLevelService.class, configurationPid = "org.openhab.startlevel", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class StartLevelService {

    public static final String STARTLEVEL_MARKER_TYPE = "startlevel";

    public static final int STARTLEVEL_OSGI = 10;
    public static final int STARTLEVEL_MODEL = 20;
    public static final int STARTLEVEL_STATES = 30;
    public static final int STARTLEVEL_RULES = 40;
    public static final int STARTLEVEL_RULEENGINE = 50;
    public static final int STARTLEVEL_UI = 70;
    public static final int STARTLEVEL_THINGS = 80;
    public static final int STARTLEVEL_COMPLETE = 100;

    private final Logger logger = LoggerFactory.getLogger(StartLevelService.class);

    private final BundleContext bundleContext;
    private final ReadyService readyService;
    private final EventPublisher eventPublisher;

    private final Set<ReadyMarker> markers = ConcurrentHashMap.newKeySet();
    private final Map<String, ReadyTracker> trackers = new ConcurrentHashMap<>();
    private final Map<Integer, ReadyMarker> slmarker = new ConcurrentHashMap<>();

    private @Nullable ScheduledFuture<?> job;
    private final ScheduledExecutorService scheduler = new WrappedScheduledExecutorService(1,
            new NamedThreadFactory("startlevel"));

    private int openHABStartLevel = 0;

    private Map<Integer, Set<ReadyMarker>> startlevels = Map.of();

    @Activate
    public StartLevelService(BundleContext bundleContext, @Reference ReadyService readyService,
            @Reference EventPublisher eventPublisher) {
        this.bundleContext = bundleContext;
        this.readyService = readyService;
        this.eventPublisher = eventPublisher;
    }

    @Activate
    protected void activate(Map<String, Object> configuration) {
        modified(configuration);

        job = scheduler.scheduleWithFixedDelay(() -> {
            handleOSGiStartlevel();

            if (openHABStartLevel >= 10) {
                for (Integer level : new TreeSet<>(startlevels.keySet())) {
                    if (openHABStartLevel >= level) {
                        continue;
                    } else {
                        boolean reached = isStartLevelReached(startlevels.get(level));
                        if (reached) {
                            setStartLevel(level);
                        } else {
                            return;
                        }
                    }
                }
                if (openHABStartLevel < 100) {
                    setStartLevel(100);
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Returns the current start level of openHAB
     *
     * @return the current start level
     */
    public int getStartLevel() {
        return openHABStartLevel;
    }

    private boolean isStartLevelReached(@Nullable Set<ReadyMarker> markerSet) {
        if (markerSet == null) {
            return true;
        }
        for (ReadyMarker m : markerSet) {
            if (!markers.contains(m)) {
                return false;
            }
        }
        return true;
    }

    private void handleOSGiStartlevel() {
        FrameworkStartLevel sl = this.bundleContext.getBundle(0).adapt(FrameworkStartLevel.class);
        int defaultStartLevel = sl.getInitialBundleStartLevel();
        int startLevel = sl.getStartLevel();
        if (startLevel >= defaultStartLevel && openHABStartLevel < 10) {
            setStartLevel(10);
        } else if (startLevel < defaultStartLevel && openHABStartLevel >= 10) {
            setStartLevel(0);
        }
    }

    @Modified
    protected void modified(Map<String, Object> configuration) {
        // clean up
        slmarker.clear();
        trackers.values().forEach(t -> readyService.unregisterTracker(t));
        trackers.clear();

        // set up trackers and markers
        startlevels = parseConfig(configuration);
        startlevels.keySet()
                .forEach(sl -> slmarker.put(sl, new ReadyMarker(STARTLEVEL_MARKER_TYPE, Integer.toString(sl))));
        slmarker.put(STARTLEVEL_COMPLETE,
                new ReadyMarker(STARTLEVEL_MARKER_TYPE, Integer.toString(STARTLEVEL_COMPLETE)));
        startlevels.values().stream().forEach(ms -> ms.forEach(e -> registerTracker(e)));
    }

    private void registerTracker(ReadyMarker e) {
        String type = e.getType();
        if (!trackers.containsKey(type)) {
            ReadyTracker tracker = new ReadyTracker() {
                @Override
                public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
                    markers.remove(readyMarker);
                }

                @Override
                public void onReadyMarkerAdded(ReadyMarker readyMarker) {
                    markers.add(readyMarker);
                }
            };
            readyService.registerTracker(tracker, new ReadyMarkerFilter().withType(type));
            trackers.put(type, tracker);
        }
    }

    private Map<Integer, Set<ReadyMarker>> parseConfig(Map<String, Object> configuration) {
        return configuration.entrySet().stream() //
                .filter(e -> hasIntegerKey(e)) //
                .map(e -> new AbstractMap.SimpleEntry<>(Integer.valueOf(e.getKey()), markerSet(e.getValue()))) //
                .sorted(Map.Entry.<Integer, Set<ReadyMarker>> comparingByKey().reversed()) //
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private Set<ReadyMarker> markerSet(Object value) {
        Set<ReadyMarker> markerSet = new HashSet<>();
        if (value instanceof String) {
            String[] segments = ((String) value).split(",");
            for (String segment : segments) {
                if (segment.contains(":")) {
                    String[] markerParts = segment.strip().split(":");
                    markerSet.add(new ReadyMarker(markerParts[0], markerParts[1]));
                } else {
                    logger.warn("Ignoring invalid configuration value '{}'", value);
                }
            }
        }
        return markerSet;
    }

    private boolean hasIntegerKey(Entry<String, Object> entry) {
        try {
            Integer.valueOf(entry.getKey());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Deactivate
    protected void deactivate() {
        slmarker.clear();
        trackers.values().forEach(t -> readyService.unregisterTracker(t));
        ScheduledFuture<?> job = this.job;
        if (job != null) {
            job.cancel(true);
        }
    }

    private void setStartLevel(int level) {
        ReadyMarker marker = slmarker.get(level);
        if (marker != null) {
            readyService.markReady(marker);
        }
        openHABStartLevel = level;
        scheduler.submit(() -> {
            StartlevelEvent startlevelEvent = SystemEventFactory.createStartlevelEvent(level);
            eventPublisher.post(startlevelEvent);
            logger.debug("Reached start level {}", level);
        });
    }
}
