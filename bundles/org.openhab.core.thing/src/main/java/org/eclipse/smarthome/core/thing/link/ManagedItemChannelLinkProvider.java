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
package org.eclipse.smarthome.core.thing.link;

import java.util.Collection;

import org.eclipse.smarthome.core.common.registry.DefaultAbstractManagedProvider;
import org.eclipse.smarthome.core.storage.StorageService;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * {@link ManagedItemChannelLinkProvider} is responsible for managed {@link ItemChannelLink}s at runtime.
 *
 * @author Dennis Nobel - Initial contribution
 *
 */
@Component(immediate = true, service = { ItemChannelLinkProvider.class, ManagedItemChannelLinkProvider.class })
public class ManagedItemChannelLinkProvider extends DefaultAbstractManagedProvider<ItemChannelLink, String>
        implements ItemChannelLinkProvider {

    @Override
    protected String getStorageName() {
        return ItemChannelLink.class.getName();
    }

    @Override
    protected String keyToString(String key) {
        return key;
    }

    public void removeLinksForThing(ThingUID thingUID) {
        Collection<ItemChannelLink> itemChannelLinks = getAll();
        for (ItemChannelLink itemChannelLink : itemChannelLinks) {
            if (itemChannelLink.getLinkedUID().getThingUID().equals(thingUID)) {
                this.remove(itemChannelLink.getUID());
            }
        }
    }

    @Reference
    @Override
    protected void setStorageService(StorageService storageService) {
        super.setStorageService(storageService);
    }

    @Override
    protected void unsetStorageService(StorageService storageService) {
        super.unsetStorageService(storageService);
    }

}
