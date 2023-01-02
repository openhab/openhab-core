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
package org.openhab.core.automation.module.script;

import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link ScriptDependencyTracker} is an interface that script dependency trackers can implement to allow automatic
 * re-loading if scripts on dependency changes
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface ScriptDependencyTracker {

    /**
     * Get the tracker for a given script identifier
     *
     * @param scriptId the unique id of the script
     * @return a {@link Consumer<String>} that accepts a the path to a dependency
     */
    Consumer<String> getTracker(String scriptId);

    /**
     * Remove all tracking data for a given scipt identifier
     *
     * @param scriptId the uniwue id of the script
     */
    void removeTracking(String scriptId);

    /**
     * The {@link ScriptDependencyTracker.Listener} is an interface that needs to be implemented by listeners that want
     * to be notified about a dependency change
     */
    interface Listener {

        /**
         * Called by the dependency tracker when a registered dependency changes
         *
         * @param scriptId the identifier of the script whose dependency changed
         */
        void onDependencyChange(String scriptId);
    }
}
