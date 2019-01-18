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
package org.eclipse.smarthome.core.internal.items;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.common.registry.AbstractManagedProvider;
import org.eclipse.smarthome.core.items.ManagedMetadataProvider;
import org.eclipse.smarthome.core.items.Metadata;
import org.eclipse.smarthome.core.items.MetadataKey;
import org.eclipse.smarthome.core.items.MetadataPredicates;
import org.eclipse.smarthome.core.items.MetadataProvider;
import org.eclipse.smarthome.core.storage.StorageService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ManagedMetadataProviderImpl} is an OSGi service, that allows to add or remove
 * metadata for items at runtime. Persistence of added metadata is handled by
 * a {@link StorageService}.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(immediate = true, service = { MetadataProvider.class, ManagedMetadataProvider.class })
public class ManagedMetadataProviderImpl extends AbstractManagedProvider<Metadata, MetadataKey, Metadata>
        implements ManagedMetadataProvider {

    private final Logger logger = LoggerFactory.getLogger(ManagedMetadataProviderImpl.class);

    @Override
    protected String getStorageName() {
        return Metadata.class.getName();
    }

    @Override
    protected @NonNull String keyToString(@NonNull MetadataKey key) {
        return key.toString();
    }

    @Override
    protected Metadata toElement(@NonNull String key, @NonNull Metadata persistableElement) {
        return persistableElement;
    }

    @Override
    protected Metadata toPersistableElement(Metadata element) {
        return element;
    }

    @Override
    @Reference
    protected void setStorageService(StorageService storageService) {
        super.setStorageService(storageService);
    }

    @Override
    protected void unsetStorageService(StorageService storageService) {
        super.unsetStorageService(storageService);
    }

    /**
     * Removes all metadata of a given item
     *
     * @param itemname the name of the item for which the metadata is to be removed.
     */
    @Override
    public void removeItemMetadata(@NonNull String name) {
        logger.debug("Removing all metadata for item {}", name);
        getAll().stream().filter(MetadataPredicates.ofItem(name)).map(Metadata::getUID).forEach(this::remove);
    }

}
