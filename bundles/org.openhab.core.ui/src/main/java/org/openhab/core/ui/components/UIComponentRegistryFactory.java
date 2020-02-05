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
package org.openhab.core.ui.components;

import java.util.HashMap;
import java.util.Map;

import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * A factory for {@link UIComponentRegistry} instances based on the namespace.
 *
 * @author Yannick Schaus - Initial contribution
 */
@Component(service = UIComponentRegistryFactory.class, immediate = true)
public class UIComponentRegistryFactory {
    StorageService storageService;
    Map<String, UIComponentRegistry> registries = new HashMap<>();

    @Reference
    protected void setStorageService(StorageService storageService) {
        this.storageService = storageService;
    }

    protected void unsetStorageService(StorageService storageService) {
        this.storageService = null;
    }

    /**
     * Gets the {@link UIComponentRegistry} for the specified namespace.
     *
     * @param namespace the namespace
     * @return a registry for UI elements in the namespace
     */
    public UIComponentRegistry getRegistry(String namespace) {
        if (registries.containsKey(namespace)) {
            return registries.get(namespace);
        } else {
            UIComponentRegistry registry = new UIComponentRegistry(namespace, storageService);
            registries.put(namespace, registry);
            return registry;
        }
    }
}
