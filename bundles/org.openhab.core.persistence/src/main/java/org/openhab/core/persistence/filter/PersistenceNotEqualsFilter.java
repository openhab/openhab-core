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

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.items.Item;

/**
 * The {@link PersistenceNotEqualsFilter} is a filter blocks specific values
 * <p />
 * The filter returns {@code false} if the string representation of the item's state is in the given list
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class PersistenceNotEqualsFilter extends PersistenceFilter {
    private final Collection<String> values;

    public PersistenceNotEqualsFilter(String name, Collection<String> values) {
        super(name);
        this.values = values;
    }

    public Collection<String> getValues() {
        return values;
    }

    @Override
    public boolean apply(Item item) {
        return !values.contains(item.getState().toFullString());
    }

    @Override
    public void persisted(Item item) {
    }

    @Override
    public String toString() {
        return String.format("%s [name=%s, value=%s]", getClass().getSimpleName(), getName(), values);
    }
}
