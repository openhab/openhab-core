/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.persistence.filter;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;

/**
 * The {@link PersistenceFilter} is the base class for implementing persistence filters.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public abstract class PersistenceFilter {
    private final String name;

    public PersistenceFilter(final String name) {
        this.name = name;
    }

    /**
     * Get the name of this filter
     *
     * @return a unique name
     */
    public String getName() {
        return name;
    }

    /**
     * Apply this filter to an item
     *
     * @param item the item to check
     * @return true if the filter allows persisting this value
     */
    public abstract boolean apply(Item item);

    /**
     * Notify filter that item was persisted
     *
     * @param item the persisted item
     */
    public abstract void persisted(Item item);

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        return result;
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PersistenceFilter)) {
            return false;
        }
        final PersistenceFilter other = (PersistenceFilter) obj;
        return Objects.equals(name, other.name);
    }

    @Override
    public String toString() {
        return String.format("%s [name=%s]", getClass().getSimpleName(), name);
    }
}
