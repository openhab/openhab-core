/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.io.rest.core.persistence;

import java.io.Serial;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This exception is thrown by the {@link PersistenceResource} if an item could not be found in a persistence service.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class PersistenceItemNotFoundException extends Exception {

    public PersistenceItemNotFoundException(String name, String serviceId) {
        super("Item '" + name + "' could not be found in persistence service '" + serviceId + "'");
    }

    @Serial
    private static final long serialVersionUID = 360588429692588595L;
}
