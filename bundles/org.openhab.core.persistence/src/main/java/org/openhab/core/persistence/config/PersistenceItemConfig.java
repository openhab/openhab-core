/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.persistence.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This class represents the configuration that identify item(s) by name.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class PersistenceItemConfig extends PersistenceConfig {
    final String item;

    public PersistenceItemConfig(final String item) {
        this.item = item;
    }

    public String getItem() {
        return item;
    }

    @Override
    public String toString() {
        return String.format("%s [item=%s]", getClass().getSimpleName(), item);
    }
}
