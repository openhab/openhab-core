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
package org.openhab.core.types;

import java.util.SortedMap;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A complex type consists out of a sorted list of primitive constituents.
 * Each constituent can be referred to by a unique name.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface ComplexType extends Type {

    /**
     * Returns all constituents with their names as a sorted map
     *
     * @return all constituents with their names
     */
    public SortedMap<String, PrimitiveType> getConstituents();
}
