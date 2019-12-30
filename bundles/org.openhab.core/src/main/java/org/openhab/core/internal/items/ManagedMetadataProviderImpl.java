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
package org.openhab.core.internal.items;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.items.ManagedMetadataProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataPredicates;
import org.openhab.core.items.MetadataProvider;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Activate;
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
@NonNullByDefault
public class ManagedMetadataProviderImpl extends AbstractManagedProvider<Metadata, MetadataKey, Metadata>
        implements ManagedMetadataProvider {

    private final Logger logger = LoggerFactory.getLogger(ManagedMetadataProviderImpl.class);

    @Activate
    public ManagedMetadataProviderImpl(final @Reference StorageService storageService) {
        super(storageService);
    }

    @Override
    protected String getStorageName() {
        return Metadata.class.getName();
    }

    @Override
    protected String keyToString(MetadataKey key) {
        return key.toString();
    }

    @Override
    protected Metadata toElement(String key, Metadata persistableElement) {
        return persistableElement;
    }

    @Override
    protected Metadata toPersistableElement(Metadata element) {
        return element;
    }

    /**
     * Removes all metadata of a given item
     *
     * @param itemname the name of the item for which the metadata is to be removed.
     */
    @Override
    public void removeItemMetadata(String name) {
        logger.debug("Removing all metadata for item {}", name);
        getAll().stream().filter(MetadataPredicates.ofItem(name)).map(Metadata::getUID).forEach(this::remove);
    }

}
