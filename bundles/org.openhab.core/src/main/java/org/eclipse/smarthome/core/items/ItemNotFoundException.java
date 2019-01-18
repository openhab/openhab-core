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

/**
 * This exception is thrown by the {@link ItemRegistry} if an item could
 * not be found.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public class ItemNotFoundException extends ItemLookupException {

    public ItemNotFoundException(String name) {
        super("Item '" + name + "' could not be found in the item registry");
    }

    private static final long serialVersionUID = -3720784568250902711L;

}
