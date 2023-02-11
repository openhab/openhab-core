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
package org.openhab.core.thing;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.dto.ThingDTOMapper;
import org.openhab.core.thing.internal.BridgeImpl;
import org.openhab.core.thing.internal.ThingStorageEntity;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link ManagedThingProvider} is an OSGi service, that allows to add or remove
 * things at runtime by calling {@link ManagedThingProvider#addThing(Thing)} or
 * {@link ManagedThingProvider#removeThing(Thing)}. An added thing is
 * automatically exposed to the {@link ThingRegistry}.
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Dennis Nobel - Integrated Storage
 * @author Michael Grammling - Added dynamic configuration update
 */
@NonNullByDefault
@Component(immediate = true, service = { ThingProvider.class, ManagedThingProvider.class })
public class ManagedThingProvider extends AbstractManagedProvider<Thing, ThingUID, ThingStorageEntity>
        implements ThingProvider {

    @Activate
    public ManagedThingProvider(final @Reference StorageService storageService) {
        super(storageService);
    }

    @Override
    protected String getStorageName() {
        return Thing.class.getName();
    }

    @Override
    protected String keyToString(ThingUID key) {
        return key.toString();
    }

    @Override
    protected @Nullable Thing toElement(String key, ThingStorageEntity persistableElement) {
        try {
            return ThingDTOMapper.map(persistableElement, persistableElement.isBridge);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create thing with UID '{}' from stored entity: {}", key, e.getMessage());
        }
        return null;
    }

    protected ThingStorageEntity toPersistableElement(Thing element) {
        return new ThingStorageEntity(ThingDTOMapper.map(element), element instanceof BridgeImpl);
    }
}
