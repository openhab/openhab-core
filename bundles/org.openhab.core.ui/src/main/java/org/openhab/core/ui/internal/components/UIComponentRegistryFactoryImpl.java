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
package org.openhab.core.ui.internal.components;

import java.util.HashMap;
import java.util.Map;

import org.openhab.core.storage.StorageService;
import org.openhab.core.ui.components.UIComponentRegistryFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Implementation for a {@link UIComponentRegistryFactory} using a {@link StorageService} and a
 * {@link UIComponentProvider}.
 *
 * @author Yannick Schaus - Initial contribution
 */
@Component(service = UIComponentRegistryFactory.class, immediate = true)
public class UIComponentRegistryFactoryImpl implements UIComponentRegistryFactory {
    Map<String, UIComponentRegistryImpl> registries = new HashMap<>();

    @Reference
    StorageService storageService;

    @Override
    public UIComponentRegistryImpl getRegistry(String namespace) {
        if (registries.containsKey(namespace)) {
            return registries.get(namespace);
        } else {
            UIComponentRegistryImpl registry = new UIComponentRegistryImpl(namespace, storageService);
            registries.put(namespace, registry);
            return registry;
        }
    }
}
