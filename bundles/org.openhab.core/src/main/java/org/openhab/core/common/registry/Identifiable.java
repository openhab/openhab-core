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
package org.openhab.core.common.registry;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface for classes that instances provide an identifier.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public interface Identifiable<T> {

    /**
     * Get the unique identifier.
     *
     * @return the unique identifier
     */
    T getUID();
}
