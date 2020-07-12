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
package org.openhab.core.thing;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.dto.ThingDTO;
import org.openhab.core.thing.dto.ThingDTOMapper;
import org.openhab.core.thing.type.BridgeType;
import org.openhab.core.thing.type.ThingTypeRegistry;
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
public class ManagedThingProvider extends AbstractManagedProvider<Thing, ThingUID, ThingDTO> implements ThingProvider {

    private final ThingTypeRegistry thingTypeRegistry;

    @Activate
    public ManagedThingProvider(final @Reference StorageService storageService,
            final @Reference ThingTypeRegistry thingTypeRegistry) {
        super(storageService);
        this.thingTypeRegistry = thingTypeRegistry;
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
    protected @Nullable Thing toElement(String key, ThingDTO persistableElement) {
        ThingTypeUID thingTypeUID = new ThingTypeUID(persistableElement.thingTypeUID);
        boolean isBridge = thingTypeRegistry.getThingType(thingTypeUID) instanceof BridgeType;

        Thing thing = ThingDTOMapper.map(persistableElement, isBridge);

        return thing;
    }

    @Override
    protected ThingDTO toPersistableElement(Thing element) {
        return ThingDTOMapper.map(element);
    }
}
