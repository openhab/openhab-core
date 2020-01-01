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

import org.openhab.core.common.registry.Identifiable;

/**
 * A listener to be informed before entities are added respectively after they are removed.
 *
 * @author Simon Kaufmann - Initial contribution
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
