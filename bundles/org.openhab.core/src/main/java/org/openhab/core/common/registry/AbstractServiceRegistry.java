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
package org.openhab.core.common.registry;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link AbstractServiceRegistry} is an abstract implementation of the {@link Registry} interface, that can be used
 * as a base class for {@link Registry} implementations that track registered OSGi services and provide access to them.
 *
 * <p>
 * This {@link Registry} implementation provides access to registered OSGi services by their
 * {@link Identifiable#getUID}. It does NOT support adding, updating, or removing elements manually.
 * <p>
 * In comparison to {@link AbstractRegistry}, this implementation does not track {@link Provider}s and query them for
 * elements. Instead, elements are registered directly as OSGi services. Also, there is no {@link ManagedProvider} for
 * this {@link Registry} implementation to write to, so adding, updating, or removing elements through the
 * {@link Registry} methods is not possible. Use this class to implement {@link Registry}-like access to OSGi services
 * of a specific service type. Use {@link AbstractRegistry} when elements are to be provided by {@link Provider}s and
 * the {@link Registry} methods to add, update, and remove elements are required to work.
 *
 * @implNote Implementations must define OSGi bind and unbind methods for the services they track, and call
 *           {@link #addService(Identifiable)} and {@link #removeService(Identifiable)} when the service is bound or
 *           unbound.
 *
 * @author Florian Hotze - Initial contribution
 *
 * @param <S> type of the identifiable service
 * @param <K> type of the key
 */
@NonNullByDefault
public abstract class AbstractServiceRegistry<@NonNull S extends Identifiable<K>, @NonNull K>
        implements Registry<S, K> {

    protected final Map<K, S> services = new ConcurrentHashMap<>();
    protected final Set<RegistryChangeListener<S>> listeners = new CopyOnWriteArraySet<>();

    protected void addService(S service) {
        services.put(service.getUID(), service);
        listeners.forEach(l -> l.added(service));
    }

    protected void removeService(S service) {
        services.remove(service.getUID());
        listeners.forEach(l -> l.removed(service));
    }

    @Override
    public @Nullable S get(K key) {
        return services.get(key);
    }

    @Override
    public Collection<S> getAll() {
        return List.copyOf(services.values());
    }

    @Override
    public Stream<S> stream() {
        return services.values().stream();
    }

    @Override
    public void addRegistryChangeListener(RegistryChangeListener<S> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeRegistryChangeListener(RegistryChangeListener<S> listener) {
        listeners.remove(listener);
    }

    @Override
    public S add(S element) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " does not support adding elements manually.");
    }

    @Override
    public @Nullable S update(S element) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " does not support updating elements manually.");
    }

    @Override
    public @Nullable S remove(K key) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " does not support removing elements manually.");
    }
}
