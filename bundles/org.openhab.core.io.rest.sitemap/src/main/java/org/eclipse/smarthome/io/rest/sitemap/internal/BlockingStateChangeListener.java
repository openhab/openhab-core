/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.rest.sitemap.internal;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.StateChangeListener;
import org.eclipse.smarthome.core.types.State;

/**
 * This is a state change listener, which is merely used to determine, if a
 * state change has occurred on one of a list of items.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
class BlockingStateChangeListener implements StateChangeListener {

    private boolean changed = false;

    @Override
    public void stateChanged(Item item, State oldState, State newState) {
        changed = true;
    }

    /**
     * determines, whether a state change has occurred since its creation
     *
     * @return true, if a state has changed
     */
    public boolean hasChangeOccurred() {
        return changed;
    }

    @Override
    public void stateUpdated(Item item, State state) {
        // ignore if the state did not change
    }
}