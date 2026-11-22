/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.core.types;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.xtext.common.types.access.impl.TypeResourceServices;
import org.eclipse.xtext.common.types.access.impl.URIHelperConstants;
import org.eclipse.xtext.common.types.access.reflect.ReflectionTypeProvider;
import org.eclipse.xtext.common.types.access.reflect.ReflectionTypeProviderFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Type provider factory that can remove its type providers from the resource sets again.
 *
 * A type provider registers itself in the resource set and is reused by every later model in it. It holds the class
 * loader of the language bundle, so after a refresh of that bundle the models of the new bundle revision would keep
 * resolving types with the class loaders of the old one. {@link #dispose()} is called when the language is
 * unregistered.
 *
 * @author Thomas Leber - Initial contribution
 */
@Singleton
@NonNullByDefault
public class DisposableTypeProviderFactory extends ReflectionTypeProviderFactory {

    private final Map<ResourceSet, ReflectionTypeProvider> typeProviders = new WeakHashMap<>();

    @Inject
    public DisposableTypeProviderFactory(ClassLoader classLoader, TypeResourceServices services) {
        super(classLoader, services);
    }

    @Override
    @NonNullByDefault({})
    public ReflectionTypeProvider createTypeProvider(ResourceSet resourceSet) {
        ReflectionTypeProvider typeProvider = super.createTypeProvider(resourceSet);

        synchronized (typeProviders) {
            typeProviders.put(resourceSet, typeProvider);
        }

        return typeProvider;
    }

    /**
     * Removes the type providers created by this factory and the type resources loaded through them.
     */
    public void dispose() {
        Map<ResourceSet, ReflectionTypeProvider> providers;

        synchronized (typeProviders) {
            providers = new HashMap<>(typeProviders);
            typeProviders.clear();
        }

        providers.forEach((resourceSet, typeProvider) -> {
            synchronized (resourceSet) {
                Map<String, Object> factories = resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap();
                // only if the registered type provider is still the one of this factory
                if (factories.remove(URIHelperConstants.PROTOCOL, typeProvider)) {
                    resourceSet.getResources()
                            .removeIf(resource -> URIHelperConstants.PROTOCOL.equals(resource.getURI().scheme()));
                }
            }
        });
    }
}
