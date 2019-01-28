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
package org.eclipse.smarthome.model.core;

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
