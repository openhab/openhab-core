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
package org.openhab.core.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BaseServiceBundleTracker} tracks a set of bundles (selected {@link #isRelevantBundle(Bundle)} and sets
 * the {@link #readyMarker} when all registered bundles are active. Methods can be overridden to add additional
 * conditions that must be fulfilled.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public abstract class BaseServiceBundleTracker extends BundleTracker<Bundle> implements ReadyService.ReadyTracker {
    private static final int STATE_MASK = Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE | Bundle.STARTING
            | Bundle.STOPPING | Bundle.UNINSTALLED;
    private final Logger logger = LoggerFactory.getLogger(BaseServiceBundleTracker.class);

    private final ReadyService readyService;
    private final ReadyMarker readyMarker;

    private final Map<String, Integer> bundles = new ConcurrentHashMap<>();
    private volatile boolean startLevelReached = false;

    // All access must be guarded by "this"
    private boolean ready = false;

    @Activate
    public BaseServiceBundleTracker(ReadyService readyService, ReadyMarker readyMarker, BundleContext bc) {
        super(bc, STATE_MASK, null);
        this.readyService = readyService;
        this.readyMarker = readyMarker;
        this.open();

        int minLevel = minimumStartLevel();
        if (minLevel >= 0) {
            readyService.registerTracker(this, new ReadyMarkerFilter()
                    .withType(StartLevelService.STARTLEVEL_MARKER_TYPE).withIdentifier(Integer.toString(minLevel)));
        } else {
            startLevelReached = true;
        }
    }

    @Deactivate
    public void deactivate() throws Exception {
        this.close();
        synchronized (this) {
            ready = false;
        }
    }

    /**
     * The minimum startlevel when this tracker might mark the {@code readyMarker} ready;
     *
     * @return The minimum startlevel or a negative value to disable.
     */
    protected abstract int minimumStartLevel();

    /**
     * Decide if a bundle should be tracked by this bundle tracker.
     *
     * @param bundle the bundle to evaluate.
     * @return {@code true} if the bundle should be tracked, {@code false} otherwise.
     */
    protected abstract boolean isRelevantBundle(Bundle bundle);

    @Override
    public Bundle addingBundle(@NonNullByDefault({}) Bundle bundle, @Nullable BundleEvent event) {
        String bsn = bundle.getSymbolicName();
        int state = bundle.getState();
        if (isRelevantBundle(bundle)) {
            logger.debug("Added {}: {} ", bsn, stateToString(state));
            bundles.put(bsn, state);
            handleChange();
        }

        return bundle;
    }

    @Override
    public void modifiedBundle(@NonNullByDefault({}) Bundle bundle, @Nullable BundleEvent event,
            @NonNullByDefault({}) Bundle object) {
        String bsn = bundle.getSymbolicName();
        int state = bundle.getState();
        if (isRelevantBundle(bundle)) {
            logger.debug("Modified {}: {}", bsn, stateToString(state));
            bundles.put(bsn, state);
            handleChange();
        }
    }

    @Override
    public void removedBundle(@NonNullByDefault({}) Bundle bundle, @Nullable BundleEvent event,
            @NonNullByDefault({}) Bundle object) {
        String bsn = bundle.getSymbolicName();
        if (isRelevantBundle(bundle)) {
            logger.debug("Removed {}", bsn);
            bundles.remove(bsn);
            handleChange();
        }
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        logger.debug("Readymarker '{}' added", readyMarker);
        startLevelReached = true;
        handleChange();
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        logger.debug("Readymarker '{}' removed", readyMarker);
        startLevelReached = false;
        handleChange();
    }

    protected void handleChange() {
        logger.trace("{} before change: ready: {}, startLevelReached: {}", getClass().getSimpleName(), ready,
                startLevelReached);

        boolean newReady = evaluateReady();
        synchronized (this) {
            if (ready != newReady) {
                ready = newReady;
                if (newReady) {
                    logger.debug("All conditions met, marking readymarker '{}' ready ({})", readyMarker, bundles);
                    readyService.markReady(readyMarker);
                } else {
                    logger.debug("All conditions not met, marking readymarker '{}' not ready ({})", readyMarker,
                            bundles);
                    readyService.unmarkReady(readyMarker);
                }
                logger.trace("{} after change: ready: {}, startLevelReached: {}", getClass().getSimpleName(), ready,
                        startLevelReached);
            }
        }
    }

    protected boolean evaluateReady() {
        return startLevelReached && allBundlesActive();
    }

    protected boolean allBundlesActive() {
        return bundles.values().stream().allMatch(i -> i == Bundle.ACTIVE);
    }

    protected String stateToString(int state) {
        return switch (state) {
            case Bundle.UNINSTALLED -> "UNINSTALLED";
            case Bundle.INSTALLED -> "INSTALLED";
            case Bundle.RESOLVED -> "RESOLVED";
            case Bundle.STARTING -> "STARTING";
            case Bundle.STOPPING -> "STOPPING";
            case Bundle.ACTIVE -> "ACTIVE";
            default -> "UNKNOWN";
        };
    }
}
