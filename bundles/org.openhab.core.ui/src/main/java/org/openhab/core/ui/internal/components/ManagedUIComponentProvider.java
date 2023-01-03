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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.UIComponentProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * A namespace-specific {@link ManagedProvider} for UI components.
 *
 * @author Yannick Schaus - Initial contribution
 * @author Jan N. Klug - Refactored to component factory
 */
@NonNullByDefault
@Component(factory = "org.openhab.core.ui.component.provider.factory")
public class ManagedUIComponentProvider extends AbstractProvider<RootUIComponent>
        implements ManagedProvider<RootUIComponent, String>, UIComponentProvider {

    private final String namespace;
    private final Storage<RootUIComponent> storage;

    @Activate
    public ManagedUIComponentProvider(@Reference StorageService storageService, Map<String, Object> config) {
        String namespace = ConfigParser.valueAs(config.get(CONFIG_NAMESPACE), String.class);
        if (namespace == null) {
            throw new IllegalStateException("'ui.namespace' must not be null in service configuration");
        }
        this.namespace = namespace;
        this.storage = storageService.getStorage("uicomponents_" + namespace.replace(':', '_'),
                this.getClass().getClassLoader());
    }

    @Override
    public Collection<RootUIComponent> getAll() {
        List<RootUIComponent> components = new ArrayList<>();
        for (RootUIComponent component : storage.getValues()) {
            if (component != null) {
                components.add(component);
            }
        }
        return components;
    }

    @Override
    public void add(RootUIComponent element) {
        if (element.getUID().isEmpty()) {
            throw new IllegalArgumentException("Invalid UID");
        }

        if (storage.get(element.getUID()) != null) {
            throw new IllegalArgumentException("Cannot add UI component to namespace " + namespace
                    + ", because a component with same UID (" + element.getUID() + ") already exists.");
        }
        storage.put(element.getUID(), element);
        notifyListenersAboutAddedElement(element);
    }

    @Override
    public @Nullable RootUIComponent remove(String key) {
        RootUIComponent element = storage.remove(key);
        if (element != null) {
            notifyListenersAboutRemovedElement(element);
            return element;
        }

        return null;
    }

    @Override
    public @Nullable RootUIComponent update(RootUIComponent element) {
        if (storage.get(element.getUID()) != null) {
            RootUIComponent oldElement = storage.put(element.getUID(), element);
            if (oldElement != null) {
                notifyListenersAboutUpdatedElement(oldElement, element);
                return oldElement;
            }
        } else {
            throw new IllegalArgumentException("Cannot update UI component " + element.getUID() + " in namespace "
                    + namespace + " because it doesn't exist.");
        }

        return null;
    }

    @Override
    public @Nullable RootUIComponent get(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Invalid UID");
        }
        return storage.get(key);
    }

    @Override
    public String getNamespace() {
        return namespace;
    }
}
