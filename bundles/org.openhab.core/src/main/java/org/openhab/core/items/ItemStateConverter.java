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
package org.openhab.core.items;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;

/**
 * Convert a {@link State} to an {@link Item} accepted {@link State}.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public interface ItemStateConverter {

    /**
     * Convert the given {@link State} to a state which is acceptable for the given {@link Item}.
     *
     * @param state the {@link State} to be converted.
     * @param item the {@link Item} for which the given state will be converted.
     * @return the converted {@link State} according to an accepted State of the given Item. Will return the original
     *         state in case item was {@code null}.
     */
    State convertToAcceptedState(@Nullable State state, @Nullable Item item);
}
