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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.common.registry.ManagedProvider;
import org.eclipse.smarthome.core.storage.StorageService;

/**
 * {@link ManagedMetadataProvider} is an OSGi service interface that allows to add or remove
 * metadata for items at runtime. Persistence of added metadata is handled by
 * a {@link StorageService}.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public interface ManagedMetadataProvider extends ManagedProvider<Metadata, MetadataKey>, MetadataProvider {

    /**
     * Removes all metadata of a given item
     *
     * @param itemname the name of the item for which the metadata is to be removed.
     */
    void removeItemMetadata(@NonNull String name);

}
