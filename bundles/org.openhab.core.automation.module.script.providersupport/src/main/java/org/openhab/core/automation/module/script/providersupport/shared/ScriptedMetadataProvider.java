/*
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
package org.openhab.core.automation.module.script.providersupport.shared;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.items.ManagedMetadataProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataPredicates;
import org.openhab.core.items.MetadataProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link org.openhab.core.items.MetadataProvider} keeps metadata provided by scripts during runtime.
 * This ensures that metadata is not kept on reboot, but has to be provided by the scripts again.
 *
 * @author Florian Hotze
 */
@NonNullByDefault
@Component(immediate = true, service = { ScriptedMetadataProvider.class, MetadataProvider.class })
public class ScriptedMetadataProvider extends AbstractProvider<Metadata> implements ManagedMetadataProvider {
    private final Logger logger = LoggerFactory.getLogger(ScriptedMetadataProvider.class);
    private final Map<MetadataKey, Metadata> metadataStorage = new HashMap<>();

    @Override
    public void removeItemMetadata(String name) {
        getAll().stream().filter(MetadataPredicates.ofItem(name)).map(Metadata::getUID).forEach(this::remove);
    }

    @Override
    public Collection<Metadata> getAll() {
        return metadataStorage.values();
    }

    @Override
    public @Nullable Metadata get(MetadataKey metadataKey) {
        return metadataStorage.get(metadataKey);
    }

    @Override
    public void add(Metadata metadata) {
        if (metadataStorage.containsKey(metadata.getUID())) {
            throw new IllegalArgumentException("Cannot add metadata, because metadata with the same UID ("
                    + metadata.getUID() + ") already exists");
        }
        metadataStorage.put(metadata.getUID(), metadata);

        notifyListenersAboutAddedElement(metadata);
    }

    @Override
    public @Nullable Metadata update(Metadata metadata) {
        Metadata oldMetadata = metadataStorage.get(metadata.getUID());
        if (oldMetadata != null) {
            metadataStorage.put(metadata.getUID(), metadata);
            notifyListenersAboutUpdatedElement(oldMetadata, metadata);
        } else {
            logger.warn("Could not update metadata with UID '{}', because it does not exist", metadata.getUID());
        }
        return oldMetadata;
    }

    @Override
    public @Nullable Metadata remove(MetadataKey metadataKey) {
        Metadata metadata = metadataStorage.remove(metadataKey);
        if (metadata != null) {
            notifyListenersAboutRemovedElement(metadata);
        }
        return metadata;
    }
}
