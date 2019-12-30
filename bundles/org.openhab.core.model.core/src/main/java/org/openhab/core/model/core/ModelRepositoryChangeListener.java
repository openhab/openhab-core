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
package org.openhab.core.model.core;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface ModelRepositoryChangeListener {

    /**
     * Performs dispatch of all binding configs and
     * fires all {@link ItemsChangeListener}s if {@code modelName} ends with "items".
     */
    public void modelChanged(String modelName, EventType type);
}
