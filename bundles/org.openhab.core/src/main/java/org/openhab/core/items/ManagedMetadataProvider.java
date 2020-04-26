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
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.storage.StorageService;

/**
 * {@link ManagedMetadataProvider} is an OSGi service interface that allows to add or remove
 * metadata for items at runtime. Persistence of added metadata is handled by
 * a {@link StorageService}.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface ManagedMetadataProvider extends ManagedProvider<Metadata, MetadataKey>, MetadataProvider {

    /**
     * Removes all metadata of a given item
     *
     * @param itemname the name of the item for which the metadata is to be removed.
     */
    void removeItemMetadata(String name);
}
