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
package org.openhab.core.persistence;

import java.io.Serial;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This exception is thrown if an item cannot be found in a persistence service.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class PersistenceItemNotFoundException extends Exception {

    @Serial
    private static final long serialVersionUID = 360588429692588595L;

    public PersistenceItemNotFoundException(String serviceId, String name) {
        this(serviceId, name, null);
    }

    public PersistenceItemNotFoundException(String serviceId, String name, @Nullable String alias) {
        super("Item '" + name + "' " + (alias != null ? "with alias '" + alias + "' " : "")
                + "could not be found in persistence service '" + serviceId + "'");
    }
}
