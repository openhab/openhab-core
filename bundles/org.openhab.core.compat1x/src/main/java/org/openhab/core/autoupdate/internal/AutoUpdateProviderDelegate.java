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
package org.openhab.core.autoupdate.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.common.registry.RegistryChangeListener;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.Metadata;
import org.eclipse.smarthome.core.items.MetadataKey;
import org.eclipse.smarthome.core.items.MetadataProvider;
import org.openhab.core.autoupdate.AutoUpdateBindingProvider;
import org.openhab.core.binding.BindingChangeListener;
import org.openhab.core.binding.BindingProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

/**
 * This class serves as a mapping from the "old" org.openhab namespace to the new org.eclipse.smarthome
 * namespace for the auto update provider. It gathers all services that implement the old interface
 * and makes them available as single provider of the new interface.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@NonNullByDefault
@Component(service = MetadataProvider.class)
public class AutoUpdateProviderDelegate
        implements MetadataProvider, RegistryChangeListener<Item>, BindingChangeListener {

    private static final String AUTOUPDATE_KEY = "autoupdate";

    private Set<AutoUpdateBindingProvider> providers = new CopyOnWriteArraySet<>();
    private Set<ProviderChangeListener<Metadata>> listeners = new CopyOnWriteArraySet<>();
    private Set<String> itemUpdateVetos = new HashSet<>();
    private boolean started = false;

    private @NonNullByDefault({}) ItemRegistry itemRegistry;

    @Activate
    protected void activate() {
        refreshItemUpdateVetos();
        started = true;
        itemRegistry.addRegistryChangeListener(this);
        for (AutoUpdateBindingProvider provider : providers) {
            provider.addBindingChangeListener(this);
        }
    }

    @Deactivate
    protected void deactivate() {
        for (AutoUpdateBindingProvider provider : providers) {
            provider.removeBindingChangeListener(this);
        }
        itemRegistry.removeRegistryChangeListener(this);
        started = false;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE)
    public void addAutoUpdateBindingProvider(AutoUpdateBindingProvider provider) {
        providers.add(provider);
        if (started) {
            refreshItemUpdateVetos();
            provider.addBindingChangeListener(this);
        }
    }

    public void removeAutoUpdateBindingProvider(AutoUpdateBindingProvider provider) {
        providers.remove(provider);
        if (started) {
            refreshItemUpdateVetos();
            provider.removeBindingChangeListener(this);
        }
    }

    @Reference
    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<Metadata> listener) {
        this.listeners.add(listener);
    }

    @Override
    public Collection<Metadata> getAll() {
        Set<Metadata> metadataSet = new HashSet<>();
        for (Item item : itemRegistry.getAll()) {
            synchronized (itemUpdateVetos) {
                if (itemUpdateVetos.contains(item.getName())) {
                    Metadata metadata = getMetadata(item.getName());
                    metadataSet.add(metadata);
                }
            }
        }
        return metadataSet;
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<Metadata> listener) {
        this.listeners.remove(listener);
    }

    private void refreshItemUpdateVetos() {
        Set<String> newVetos = new HashSet<>();
        synchronized (itemUpdateVetos) {
            itemUpdateVetos.clear();
            for (Item item : itemRegistry.getAll()) {
                for (AutoUpdateBindingProvider provider : providers) {
                    Boolean autoUpdate = provider.autoUpdate(item.getName());
                    if (Boolean.FALSE.equals(autoUpdate)) {
                        newVetos.add(item.getName());
                    }
                }
            }

            // find the removed ones
            Set<String> removedVetos = new HashSet<>(itemUpdateVetos);
            removedVetos.removeAll(newVetos);
            for (String itemName : removedVetos) {
                if (itemUpdateVetos.contains(itemName)) {
                    Metadata md = getMetadata(itemName);
                    for (ProviderChangeListener<Metadata> listener : listeners) {
                        listener.removed(this, md);
                    }
                }
            }

            // find the added ones
            Set<String> addedVetos = new HashSet<>(newVetos);
            addedVetos.removeAll(itemUpdateVetos);
            for (String itemName : addedVetos) {
                notifyAboutAddedMetadata(itemName);
            }
            itemUpdateVetos = newVetos;
        }
    }

    private void notifyAboutAddedMetadata(String itemName) {
        if (itemUpdateVetos.contains(itemName)) {
            Metadata md = getMetadata(itemName);
            for (ProviderChangeListener<Metadata> listener : listeners) {
                listener.added(this, md);
            }
        }
    }

    private void notifyAboutRemovedMetadata(String itemName) {
        for (ProviderChangeListener<Metadata> listener : listeners) {
            listener.removed(this, getMetadata(itemName));
        }
    }

    private Metadata getMetadata(String itemName) {
        return new Metadata(new MetadataKey(AUTOUPDATE_KEY, itemName), "false", null);
    }

    @Override
    public void added(Item element) {
        String itemName = element.getName();
        refreshVetoForItem(itemName);
    }

    private void refreshVetoForItem(String itemName) {
        synchronized (itemUpdateVetos) {
            boolean removed = itemUpdateVetos.remove(itemName);
            for (AutoUpdateBindingProvider provider : providers) {
                Boolean autoUpdate = provider.autoUpdate(itemName);
                if (Boolean.FALSE.equals(autoUpdate)) {
                    itemUpdateVetos.add(itemName);
                    notifyAboutAddedMetadata(itemName);
                    return;
                }
            }
            if (removed) {
                notifyAboutRemovedMetadata(itemName);
            }
        }
    }

    @Override
    public void removed(Item element) {
        itemUpdateVetos.remove(element.getName());
    }

    @Override
    public void updated(Item oldElement, Item element) {
        refreshVetoForItem(element.getName());
    }

    @Override
    public void bindingChanged(@Nullable BindingProvider provider, @Nullable String itemName) {
        if (itemName != null) {
            refreshVetoForItem(itemName);
        }
    }

    @Override
    public void allBindingsChanged(@Nullable BindingProvider provider) {
        refreshItemUpdateVetos();
    }
}
