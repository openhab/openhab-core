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
package org.openhab.core.types;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is a marker interface for all state types.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface State extends Type {

    /**
     * Convert this {@link State}'s value into another type
     *
     * @param target the desired {@link State} type
     * @return the {@link State}'s value in the given type's representation, or <code>null</code> if the conversion was
     *         not possible
     */
    default <T extends @Nullable State> @Nullable T as(@Nullable Class<T> target) {
        if (target != null && target.isInstance(this)) {
            return target.cast(this);
        } else {
            return null;
        }
    }
}
