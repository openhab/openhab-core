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
package org.openhab.core.thing.internal.firmware;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Contains methods for NonNull parameters checks
 *
 * @author Dimitar Ivanov - Initial contribution
 */
class ParameterChecks {

    static void checkNotNull(@Nullable Object object, String argumentName) {
        if (object == null) {
            throw new IllegalArgumentException(argumentName + " must not be null.");
        }
    }

    static void checkNotNullOrEmpty(@Nullable String string, String argumentName) {
        if (string == null || string.isEmpty()) {
            throw new IllegalArgumentException(argumentName + " must not be null or empty.");
        }
    }
}
