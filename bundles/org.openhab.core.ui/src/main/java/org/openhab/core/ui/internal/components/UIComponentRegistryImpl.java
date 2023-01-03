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
package org.openhab.core.ui.internal.components;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.UIComponentProvider;
import org.openhab.core.ui.components.UIComponentRegistry;

/**
 * Implementation of a {@link UIComponentRegistry} using a {@link ManagedUIComponentProvider}.
 * It is instantiated by the {@link UIComponentRegistryFactoryImpl}.
 *
 * @author Yannick Schaus - Initial contribution
 * @author Łukasz Dywicki - Support for dynamic registration of providers
 */
@NonNullByDefault
public class UIComponentRegistryImpl extends AbstractRegistry<RootUIComponent, String, ManagedUIComponentProvider>
        implements UIComponentRegistry {

    /**
     * Constructs a UI component registry for the specified namespace.
     *
     * @param namespace UI components namespace of this registry
     */
    public UIComponentRegistryImpl(String namespace, @Nullable Set<UIComponentProvider> providers) {
        super(null);
        if (providers != null && !providers.isEmpty()) {
            for (Provider<RootUIComponent> provider : providers) {
                addProvider(provider);
            }
        }
    }

    @Override
    public void addProvider(Provider<RootUIComponent> provider) {
        super.addProvider(provider);
        if (getManagedProvider().isEmpty() && provider instanceof ManagedProvider) {
            setManagedProvider((ManagedProvider<RootUIComponent, String>) provider);
        }
    }

    @Override
    public void removeProvider(Provider<RootUIComponent> provider) {
        if (getManagedProvider().isPresent() && provider instanceof ManagedProvider) {
            unsetManagedProvider((ManagedProvider<RootUIComponent, String>) provider);
        }
        super.removeProvider(provider);
    }
}
