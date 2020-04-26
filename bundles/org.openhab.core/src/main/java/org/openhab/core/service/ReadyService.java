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
package org.openhab.core.service;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Registry for {@link ReadyMarker}s.
 *
 * Services may use the {@link ReadyService} in order to denote they have completed loading/processing something.
 * <p>
 * Interested parties may register as a tracker for {@link ReadyMarker}s. Optionally they can provide a
 * {@link ReadyMarkerFilter} in order to restrict the {@link ReadyMarker}s they get notified for.
 * <p>
 * Alternatively, {@link #isReady(ReadyMarker)} can be used to check for any given {@link ReadyMarker}.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public interface ReadyService {

    /**
     * Register the given marker as being "ready".
     *
     * @param readyMarker
     */
    void markReady(ReadyMarker readyMarker);

    /**
     * Removes the given marker.
     *
     * @param readyMarker
     */
    void unmarkReady(ReadyMarker readyMarker);

    /**
     *
     * @param readyMarker
     * @return {@code true} if the given {@link ReadyMarker} is registered as being "ready".
     */
    boolean isReady(ReadyMarker readyMarker);

    /**
     * Adds the given tracker.
     *
     * It will be notified for all {@link ReadyMarker}s.
     *
     * @param readyTracker
     */
    void registerTracker(ReadyTracker readyTracker);

    /**
     * Adds the given tracker.
     *
     * It will be notified for a ReadyMarker changes related to those which match the given filter criteria.
     * <p>
     * The provided tracker will get notified about the addition of all existing readyMarkers right away.
     *
     * @param readyTracker
     * @param readyMarker
     */
    void registerTracker(ReadyTracker readyTracker, ReadyMarkerFilter filter);

    /**
     * Removes the given tracker.
     *
     * The provided tracker will get notified about the removal of all existing readyMarkers right away.
     *
     * @param readyTracker
     */
    void unregisterTracker(ReadyTracker readyTracker);

    /**
     * Tracker for changes related to {@link ReadyMarker} registrations.
     *
     * @author Simon Kaufmann - Initial contribution
     *
     */
    interface ReadyTracker {

        /**
         * Gets called when a new {@link ReadyMarker} was registered as being "ready".
         *
         * @param readyMarker
         */
        void onReadyMarkerAdded(ReadyMarker readyMarker);

        /**
         * Gets called when a {@link ReadyMarker} was unregistered.
         *
         * @param readyMarker
         */
        void onReadyMarkerRemoved(ReadyMarker readyMarker);
    }
}
