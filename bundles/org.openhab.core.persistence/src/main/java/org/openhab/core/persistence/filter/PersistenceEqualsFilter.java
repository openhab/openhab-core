/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;

/**
 * The {@link PersistenceEqualsFilter} is a filter that allows only specific values to pass
 * <p />
 * The filter returns {@code false} if the string representation of the item's state is not in the given list
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class PersistenceEqualsFilter extends PersistenceFilter {
    private final Collection<String> values;
    private final boolean inverted;

    public PersistenceEqualsFilter(String name, Collection<String> values, @Nullable Boolean inverted) {
        super(name);
        this.values = values;
        this.inverted = inverted != null && inverted;
    }

    public Collection<String> getValues() {
        return values;
    }

    public boolean getInverted() {
        return inverted;
    }

    @Override
    public boolean apply(Item item) {
        return values.contains(item.getState().toFullString()) != inverted;
    }

    @Override
    public void persisted(Item item) {
    }

    @Override
    public String toString() {
        return String.format("%s [name=%s, value=%s, inverted=]", getClass().getSimpleName(), getName(), values);
    }
}
