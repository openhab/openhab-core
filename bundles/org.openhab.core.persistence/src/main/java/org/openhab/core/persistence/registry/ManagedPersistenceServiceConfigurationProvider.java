/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.persistence.registry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.persistence.dto.PersistenceServiceConfigurationDTO;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ManagedPersistenceServiceConfigurationProvider} implements a
 * {@link PersistenceServiceConfigurationProvider} for managed configurations which are stored in a JSON database
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { PersistenceServiceConfigurationProvider.class,
        ManagedPersistenceServiceConfigurationProvider.class })
public class ManagedPersistenceServiceConfigurationProvider
        extends AbstractManagedProvider<PersistenceServiceConfiguration, String, PersistenceServiceConfigurationDTO>
        implements PersistenceServiceConfigurationProvider {
    private static final String STORAGE_NAME = "org.openhab.core.persistence.PersistenceServiceConfiguration";

    @Activate
    public ManagedPersistenceServiceConfigurationProvider(@Reference StorageService storageService) {
        super(storageService);
    }

    @Override
    protected String getStorageName() {
        return STORAGE_NAME;
    }

    @Override
    protected String keyToString(String key) {
        return key;
    }

    @Override
    protected @Nullable PersistenceServiceConfiguration toElement(String key,
            PersistenceServiceConfigurationDTO persistableElement) {
        return PersistenceServiceConfigurationDTOMapper.map(persistableElement);
    }

    @Override
    protected PersistenceServiceConfigurationDTO toPersistableElement(PersistenceServiceConfiguration element) {
        return PersistenceServiceConfigurationDTOMapper.map(element);
    }
}
