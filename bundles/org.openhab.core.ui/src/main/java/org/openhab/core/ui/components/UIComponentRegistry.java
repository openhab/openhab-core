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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Registry;
import org.openhab.core.storage.StorageService;
import org.openhab.core.ui.internal.components.UIComponentProvider;

/**
 * A namespace-specific {@link Registry} for UI components.
 * It is instantiated by the {@link UIComponentRegistryFactory}.
 *
 * @author Yannick Schaus - Initial contribution
 */
@NonNullByDefault
public class UIComponentRegistry extends AbstractRegistry<RootUIComponent, String, UIComponentProvider> {

    String namespace;
    StorageService storageService;

    /**
     * Constructs a UI component registry for the specified namespace.
     *
     * @param namespace UI components namespace of this registry
     * @param storageService supporting storage service
     */
    public UIComponentRegistry(String namespace, StorageService storageService) {
        super(null);
        this.namespace = namespace;
        this.storageService = storageService;
        UIComponentProvider provider = new UIComponentProvider(namespace, storageService);
        addProvider(provider);
        setManagedProvider(provider);
    }

}
