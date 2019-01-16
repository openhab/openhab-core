/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.core.persistence.internal;

import java.util.Locale;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.persistence.PersistenceService;
import org.openhab.core.compat1x.internal.ItemMapper;

/**
 * This class serves as a mapping from the "old" org.openhab namespace to the new org.eclipse.smarthome
 * namespace for the persistence service. It wraps an instance with the old interface
 * into a class with the new interface.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Chris Jackson - updated API to support getId/getLabel
 */
public class PersistenceServiceDelegate implements PersistenceService {

    protected org.openhab.core.persistence.PersistenceService service;

    public PersistenceServiceDelegate(org.openhab.core.persistence.PersistenceService service) {
        this.service = service;
    }

    @Override
    public String getId() {
        return service.getName();
    }

    @Override
    public String getLabel(Locale locale) {
        return service.getName();
    }

    @Override
    public void store(Item item) {
        org.openhab.core.items.Item ohItem = ItemMapper.mapToOpenHABItem(item);
        if (ohItem != null) {
            service.store(ohItem);
        }
    }

    @Override
    public void store(Item item, String alias) {
        org.openhab.core.items.Item ohItem = ItemMapper.mapToOpenHABItem(item);
        if (ohItem != null) {
            service.store(ohItem, alias);
        }
    }

}
