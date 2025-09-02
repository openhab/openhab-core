/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.sitemap.internal.registry;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.sitemap.Sitemap;
import org.openhab.core.sitemap.registry.SitemapProvider;
import org.openhab.core.sitemap.registry.SitemapRegistry;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link SitemapRegistryImpl} implements the {@link SitemapRegistry}
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
@Component(service = SitemapRegistry.class, immediate = true)
public class SitemapRegistryImpl extends AbstractRegistry<Sitemap, String, SitemapProvider> implements SitemapRegistry {

    public SitemapRegistryImpl() {
        super(null);
    }

    private final Set<RegistryChangeListener<Sitemap>> registryChangeListeners = new CopyOnWriteArraySet<>();

    @Override
    protected void notifyListenersAboutAddedElement(Sitemap element) {
        registryChangeListeners.forEach(listener -> listener.added(element));
        super.notifyListenersAboutAddedElement(element);
    }

    @Override
    protected void notifyListenersAboutRemovedElement(Sitemap element) {
        registryChangeListeners.forEach(listener -> listener.removed(element));
        super.notifyListenersAboutRemovedElement(element);
    }

    @Override
    protected void notifyListenersAboutUpdatedElement(Sitemap oldElement, Sitemap element) {
        registryChangeListeners.forEach(listener -> listener.updated(oldElement, element));
    }

    @Override
    public void addRegistryChangeListener(RegistryChangeListener<Sitemap> listener) {
        registryChangeListeners.add(listener);
    }

    @Override
    public void removeRegistryChangeListener(RegistryChangeListener<Sitemap> listener) {
        registryChangeListeners.remove(listener);
    }

    @Override
    public void addSitemapProvider(Provider<Sitemap> provider) {
        addProvider(provider);
    }

    @Override
    public void removeSitemapProvider(Provider<Sitemap> provider) {
        removeProvider(provider);
    }
}
