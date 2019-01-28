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
package org.eclipse.smarthome.core.items;

import org.eclipse.smarthome.core.common.registry.Identifiable;

/**
 * A listener to be informed before entities are added respectively after they are removed.
 *
 * @author Simon Kaufmann - initial contribution and API.
 */
public interface RegistryHook<E extends Identifiable<?>> {

    /**
     * Notifies the listener that an element is going to be added to the registry.
     *
     * @param element the element to be added
     */
    void beforeAdding(E element);

    /**
     * Notifies the listener that an element was removed from the registry.
     *
     * @param element the element that was removed
     */
    void afterRemoving(E element);

}
