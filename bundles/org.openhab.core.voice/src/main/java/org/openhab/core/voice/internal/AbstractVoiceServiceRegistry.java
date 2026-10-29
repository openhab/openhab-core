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
package org.openhab.core.voice.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.common.registry.Registry;
import org.openhab.core.common.registry.RegistryChangeListener;

/**
 * Abstract base class for voice service registries. Provides {@link Registry}-like access to dynamically registered
 * services.
 *
 * @author Florian Hotze - Initial contribution
 *
 * @param <E> the identifiable service type
 */
@NonNullByDefault
public abstract class AbstractVoiceServiceRegistry<E extends Identifiable<String>> implements Registry<E, String> {

    protected final Map<String, E> services = new ConcurrentHashMap<>();
    protected final Set<RegistryChangeListener<E>> listeners = new CopyOnWriteArraySet<>();

    protected void addService(E service) {
        services.put(service.getUID(), service);
        listeners.forEach(l -> l.added(service));
    }

    protected void removeService(E service) {
        services.remove(service.getUID());
        listeners.forEach(l -> l.removed(service));
    }

    @Override
    public @Nullable E get(String key) {
        return services.get(key);
    }

    @Override
    public Collection<E> getAll() {
        return List.copyOf(services.values());
    }

    @Override
    public Stream<E> stream() {
        return services.values().stream();
    }

    @Override
    public void addRegistryChangeListener(RegistryChangeListener<E> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeRegistryChangeListener(RegistryChangeListener<E> listener) {
        listeners.remove(listener);
    }

    @Override
    public E add(E element) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " does not support adding elements manually.");
    }

    @Override
    public @Nullable E update(E element) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " does not support updating elements manually.");
    }

    @Override
    public @Nullable E remove(String key) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " does not support removing elements manually.");
    }
}
