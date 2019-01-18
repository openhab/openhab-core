/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.thing;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;

/**
 * A {@link Bridge} is a {@link Thing} that connects other {@link Thing}s.
 *
 * @author Dennis Nobel - Initial contribution and API
 */
@NonNullByDefault
public interface Bridge extends Thing {

    /**
     * Returns the children of the bridge.
     *
     * @return children
     */
    List<Thing> getThings();

    /**
     * Gets the bridge handler.
     *
     * @return the handler which can be null for a Thing that is not initialized. Note that a Bridge is
     *         guaranteed to be initialized before its children. It is therefore safe to call getBridge().getHandler()
     *         for a subordinate Thing
     */
    @Override
    @Nullable
    BridgeHandler getHandler();
}
