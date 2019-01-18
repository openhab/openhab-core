/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.common.registry;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.storage.Storage;
import org.eclipse.smarthome.core.storage.StorageService;
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
public abstract class AbstractManagedProvider<E extends Identifiable<K>, K, PE> extends AbstractProvider<E>
        implements ManagedProvider<E, K> {

    private StorageService storageService;
    private volatile Storage<PE> storage;

    protected final Logger logger = LoggerFactory.getLogger(AbstractManagedProvider.class);

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
        return storage.getKeys().stream().map(key -> {
            PE persistableElement = storage.get(key);
            if (persistableElement != null) {
                return toElement(key, persistableElement);
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public E get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Cannot get null element");
        }

        String keyAsString = keyToString(key);

        PE persistableElement = storage.get(keyAsString);
        if (persistableElement != null) {
            return toElement(keyAsString, persistableElement);
        } else {
            return null;
        }
    }

    @Override
    public E remove(K key) {
        String keyAsString = keyToString(key);
        PE persistableElement = storage.remove(keyAsString);
        if (persistableElement != null) {
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
    public E update(E element) {
        String key = getKeyAsString(element);
        if (storage.get(key) != null) {
            PE persistableElement = storage.put(key, toPersistableElement(element));
            if (persistableElement != null) {
                E oldElement = toElement(key, persistableElement);
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

    private @NonNull String getKeyAsString(@NonNull E element) {
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
    protected abstract @NonNull String keyToString(@NonNull K key);

    protected void setStorageService(final StorageService storageService) {
        if (this.storageService != storageService) {
            this.storageService = storageService;
            storage = storageService.getStorage(getStorageName(), this.getClass().getClassLoader());
        }
    }

    protected void unsetStorageService(final StorageService storageService) {
        if (this.storageService == storageService) {
            this.storageService = null;
            this.storage = null;
        }
    }

    /**
     * Converts the persistable element into the original element.
     *
     * @param key key
     * @param persistableElement persistable element
     * @return original element
     */
    protected abstract E toElement(@NonNull String key, @NonNull PE persistableElement);

    /**
     * Converts the original element into an element that can be persisted.
     *
     * @param element original element
     * @return persistable element
     */
    protected abstract PE toPersistableElement(E element);

}
