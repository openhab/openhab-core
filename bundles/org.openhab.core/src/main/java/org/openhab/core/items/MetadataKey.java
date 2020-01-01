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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.AbstractUID;

/**
 * This class represents the key of a {@link Metadata} entity.
 * It is a simple combination of a namespace and an item name.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public final class MetadataKey extends AbstractUID {
    /**
     * Package protected default constructor to allow reflective instantiation.
     *
     * !!! DO NOT REMOVE - Gson needs it !!!
     */
    MetadataKey() {
        super("", "");
    }

    /**
     * Creates a new instance.
     *
     * @param namespace
     * @param itemName
     */
    public MetadataKey(String namespace, String itemName) {
        super(namespace, itemName);
    }

    /**
     * Provides the item name of this key
     *
     * @return the item name
     */
    public String getItemName() {
        return getSegment(1);
    }

    /**
     * Provides the namespace of this key
     *
     * @return the namespace
     */
    public String getNamespace() {
        return getSegment(0);
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 2;
    }
}
