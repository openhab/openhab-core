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
package org.openhab.core.semantics;

import java.util.Collection;
import java.util.Comparator;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.semantics.dto.SemanticTagDTO;
import org.openhab.core.semantics.dto.SemanticTagDTOMapper;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link ManagedSemanticTagProvider} is an OSGi service, that allows to add or remove
 * semantic tags at runtime by calling {@link ManagedSemanticTagProvider#add}
 * or {@link ManagedSemanticTagProvider#remove}.
 * An added semantic tag is automatically exposed to the {@link SemanticTagRegistry}.
 * Persistence of added semantic tags is handled by a {@link StorageService}.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { SemanticTagProvider.class, ManagedSemanticTagProvider.class })
public class ManagedSemanticTagProvider extends AbstractManagedProvider<SemanticTag, String, SemanticTagDTO>
        implements SemanticTagProvider {

    @Activate
    public ManagedSemanticTagProvider(final @Reference StorageService storageService) {
        super(storageService);
    }

    @Override
    protected String getStorageName() {
        return SemanticTag.class.getName();
    }

    @Override
    protected String keyToString(String key) {
        return key;
    }

    @Override
    public Collection<SemanticTag> getAll() {
        // Sort tags by uid to be sure that tag classes will be created in the right order
        return super.getAll().stream().sorted(Comparator.comparing(SemanticTag::getUID)).toList();
    }

    @Override
    protected @Nullable SemanticTag toElement(String uid, SemanticTagDTO persistedTag) {
        return SemanticTagDTOMapper.map(persistedTag);
    }

    @Override
    protected SemanticTagDTO toPersistableElement(SemanticTag tag) {
        return SemanticTagDTOMapper.map(tag);
    }
}
