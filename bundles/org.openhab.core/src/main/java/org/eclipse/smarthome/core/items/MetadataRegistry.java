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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.common.registry.Registry;

/**
 * The MetadataRegistry is the central place, where additional information about items is kept.
 *
 * Metadata can be supplied by {@link MetadataProvider}s, which can provision them from any source
 * they like and also dynamically remove or add data.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@NonNullByDefault
public interface MetadataRegistry extends Registry<Metadata, MetadataKey> {

    public static final String INTERNAL_NAMESPACE_PREFIX = "_";

    /**
     * Determines whether the given namespace is internal.
     *
     * @param namespace the metadata namespace to check
     * @return {@code true} if the given namespace is internal, {@code false} otherwise
     */
    boolean isInternalNamespace(String namespace);

    /**
     * Remove all metadata of a given item
     *
     * @param itemname the name of the item for which the metadata is to be removed.
     */
    void removeItemMetadata(String name);

}
