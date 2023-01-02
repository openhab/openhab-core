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
package org.openhab.core.common.registry;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AbstractManagedProvider} is an abstract implementation for the {@link ManagedProvider} interface and can be
 * used as base class for {@link ManagedProvider} implementations. It uses the {@link StorageService} to persist the
 * elements.
 *
 * <p>
 * It provides the possibility to transform the element into another java class, that can be persisted. This is needed,
 * if the original element class is not directly persistable. If the element type can be persisted directly the
 * {@link DefaultAbstractManagedProvider} can be used as base class.
 *
 * @author Dennis Nobel - Initial contribution
 *
 * @param <E>
 *            type of the element
 * @param <K>
 *            type of the element key
 * @param <PE>
 *            type of the persistable element
 */
@NonNullByDefault
public abstract class AbstractManagedProvider<@NonNull E extends Identifiable<K>, @NonNull K, @NonNull PE>
        extends AbstractProvider<E> implements ManagedProvider<E, K> {

    private volatile Storage<PE> storage;

    protected final Logger logger = LoggerFactory.getLogger(AbstractManagedProvider.class);

    public AbstractManagedProvider(final StorageService storageService) {
        storage = storageService.getStorage(getStorageName(), this.getClass().getClassLoader());
    }

    @Override
    public void add(E element) {
        String keyAsString = getKeyAsString(element);
        if (storage.get(keyAsString) != null) {
            throw new IllegalArgumentException(
                    "Cannot add element, because an element with same UID (" + keyAsString + ") already exists.");
        }

        storage.put(keyAsString, toPersistableElement(element));
        notifyListenersAboutAddedElement(element);
        logger.debug("Added new element {} to {}.", keyAsString, this.getClass().getSimpleName());
    }

    @Override
    public Collection<E> getAll() {
        return (Collection<E>) storage.getKeys().stream().map(this::getElement).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public @Nullable E get(K key) {
        return getElement(keyToString(key));
    }

    private @Nullable E getElement(String key) {
        @Nullable
        PE persistableElement = storage.get(key);
        return persistableElement != null ? toElement(key, persistableElement) : null;
    }

    @Override
    public @Nullable E remove(K key) {
        String keyAsString = keyToString(key);
        @Nullable
        PE persistableElement = storage.remove(keyAsString);
        if (persistableElement != null) {
            @Nullable
            E element = toElement(keyAsString, persistableElement);
            if (element != null) {
                notifyListenersAboutRemovedElement(element);
                logger.debug("Removed element {} from {}.", keyAsString, this.getClass().getSimpleName());
                return element;
            }
        }

        return null;
    }

    @Override
    public @Nullable E update(E element) {
        String key = getKeyAsString(element);
        if (storage.get(key) != null) {
            @Nullable
            PE persistableElement = storage.put(key, toPersistableElement(element));
            if (persistableElement != null) {
                @Nullable
                E oldElement = toElement(key, persistableElement);
                if (oldElement == null) {
                    oldElement = element;
                }
                notifyListenersAboutUpdatedElement(oldElement, element);
                logger.debug("Updated element {} in {}.", key, this.getClass().getSimpleName());
                return oldElement;
            }
        } else {
            logger.warn("Could not update element with key {} in {}, because it does not exists.", key,
                    this.getClass().getSimpleName());
        }

        return null;
    }

    private String getKeyAsString(E element) {
        return keyToString(element.getUID());
    }

    /**
     * Returns the name of storage, that is used to persist the elements.
     *
     * @return name of the storage
     */
    protected abstract String getStorageName();

    /**
     * Transforms the key into a string representation.
     *
     * @param key key
     * @return string representation of the key
     */
    protected abstract String keyToString(K key);

    /**
     * Converts the persistable element into the original element.
     *
     * @param key key
     * @param persistableElement persistable element
     * @return original element
     */
    protected abstract @Nullable E toElement(String key, PE persistableElement);

    /**
     * Converts the original element into an element that can be persisted.
     *
     * @param element original element
     * @return persistable element
     */
    protected abstract PE toPersistableElement(E element);
}
