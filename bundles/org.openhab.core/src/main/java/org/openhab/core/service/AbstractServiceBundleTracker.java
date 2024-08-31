/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractServiceBundleTracker} tracks a set of bundles (selected {@link #isRelevantBundle(Bundle)}
 * and sets the
 * {@link #readyMarker} when all registered bundles are active
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractServiceBundleTracker extends BundleTracker<Bundle> implements ReadyService.ReadyTracker {
    private static final int STATE_MASK = Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE | Bundle.STARTING
            | Bundle.STOPPING | Bundle.UNINSTALLED;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ReadyService readyService;
    private final ReadyMarker readyMarker;

    private final Map<String, Integer> bundles = new ConcurrentHashMap<>();
    private boolean startLevel = false;
    private boolean ready = false;

    @Activate
    public AbstractServiceBundleTracker(final @Reference ReadyService readyService, BundleContext bc,
            ReadyMarker readyMarker) {
        super(bc, STATE_MASK, null);
        this.readyService = readyService;
        this.readyMarker = readyMarker;
        this.open();

        readyService.registerTracker(this, new ReadyMarkerFilter().withType(StartLevelService.STARTLEVEL_MARKER_TYPE)
                .withIdentifier(Integer.toString(StartLevelService.STARTLEVEL_MODEL)));
    }

    @Deactivate
    public void deactivate() throws Exception {
        this.close();
        ready = false;
    }

    private boolean allBundlesActive() {
        return bundles.values().stream().allMatch(i -> i == Bundle.ACTIVE);
    }

    @Override
    public Bundle addingBundle(@NonNullByDefault({}) Bundle bundle, @Nullable BundleEvent event) {
        String bsn = bundle.getSymbolicName();
        int state = bundle.getState();
        if (isRelevantBundle(bundle)) {
            logger.debug("Added {}: {} ", bsn, stateToString(state));
            bundles.put(bsn, state);
            checkReady();
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
            checkReady();
        }
    }

    @Override
    public void removedBundle(@NonNullByDefault({}) Bundle bundle, @Nullable BundleEvent event,
            @NonNullByDefault({}) Bundle object) {
        String bsn = bundle.getSymbolicName();
        if (isRelevantBundle(bundle)) {
            logger.debug("Removed {}", bsn);
            bundles.remove(bsn);
            checkReady();
        }
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        logger.debug("Readymarker '{}' added", readyMarker);
        startLevel = true;
        checkReady();
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        logger.debug("Readymarker '{}' removed", readyMarker);
        startLevel = false;
        ready = false;
        readyService.unmarkReady(readyMarker);
    }

    private synchronized void checkReady() {
        boolean allBundlesActive = allBundlesActive();
        logger.trace("ready: {}, startlevel: {}, allActive: {}", ready, startLevel, allBundlesActive);

        if (!ready && startLevel && allBundlesActive) {
            logger.debug("Adding ready marker '{}': All bundles ready ({})", readyMarker, bundles);
            readyService.markReady(readyMarker);
            ready = true;
        } else if (ready && !allBundlesActive) {
            logger.debug("Removing ready marker '{}' : Not all bundles ready ({})", readyMarker, bundles);
            readyService.unmarkReady(readyMarker);
            ready = false;
        }
    }

    private String stateToString(int state) {
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

    /**
     * Decide if a bundle should be tracked by this bundle tracker
     *
     * @param bundle the bundle
     * @return {@code true} if the bundle should be considered, {@code false} otherwise
     */
    protected abstract boolean isRelevantBundle(Bundle bundle);
}
