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
package org.openhab.core.common.registry;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractRegistry} is an abstract implementation of the {@link Registry} interface, that can be used as
 * base class for {@link Registry} implementations.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Stefan Bußweiler - Migration to new event mechanism
 * @author Victor Toni - provide elements as {@link Stream}
 * @author Kai Kreuzer - switched to parameterized logging
 * @author Hilbrand Bouwkamp - Made protected fields private and added new methods to give access.
 * @author Markus Rathgeb - Use separate collections to improve performance
 *
 * @param <E> type of the element
 */
@NonNullByDefault
public abstract class AbstractRegistry<E extends Identifiable<K>, K, P extends Provider<E>>
        implements ProviderChangeListener<E>, Registry<E, K> {

    private enum EventType {
        ADDED,
        REMOVED,
        UPDATED;
    }

    private final Logger logger = LoggerFactory.getLogger(AbstractRegistry.class);

    private final @Nullable Class<P> providerClazz;
    private @Nullable ServiceTracker<P, P> providerTracker;

    private final ReentrantReadWriteLock elementLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock elementReadLock = elementLock.readLock();
    private final ReentrantReadWriteLock.WriteLock elementWriteLock = elementLock.writeLock();
    private final Map<Provider<E>, Collection<E>> providerToElements = new HashMap<>();
    private final Map<E, Provider<E>> elementToProvider = new HashMap<>();
    private final Map<K, E> identifierToElement = new HashMap<>();
    private final Set<E> elements = new HashSet<>();

    private final Collection<RegistryChangeListener<E>> listeners = new CopyOnWriteArraySet<>();

    private Optional<ManagedProvider<E, K>> managedProvider = Optional.empty();

    private @Nullable EventPublisher eventPublisher;

    /**
     * Constructor.
     *
     * @param providerClazz the class of the providers (see e.g. {@link AbstractRegistry#addProvider(Provider)}), null
     *            if no providers should be tracked automatically after activation
     */
    protected AbstractRegistry(final @Nullable Class<P> providerClazz) {
        this.providerClazz = providerClazz;
    }

    protected void activate(final BundleContext context) {
        /*
         * The handlers for 'add' and 'remove' the services implementing the provider class (cardinality is
         * multiple) rely on an active component.
         * To grant that the add and remove functions are called only for an active component, we use a provider
         * tracker.
         */
        if (providerClazz != null) {
            Class<P> providerClazz = this.providerClazz;
            providerTracker = new ProviderTracker(context, providerClazz);
            providerTracker.open();
        }
    }

    protected void deactivate() {
        if (providerTracker != null) {
            providerTracker.close();
            providerTracker = null;
        }
    }

    private final class ProviderTracker extends ServiceTracker<P, P> {

        private final BundleContext context;

        /**
         * Constructor.
         *
         * @param context the bundle context to lookup services
         * @param providerClazz the class that implementing services should be tracked
         */
        public ProviderTracker(final BundleContext context, final Class<P> providerClazz) {
            super(context, providerClazz.getName(), null);
            this.context = context;
        }

        @Override
        public P addingService(@Nullable ServiceReference<P> reference) {
            final P service = context.getService(reference);
            addProvider(service);
            return service;
        }

        @Override
        public void removedService(@Nullable ServiceReference<P> reference, P service) {
            removeProvider(service);
        }
    }

    @Override
    public void added(Provider<E> provider, E element) {
        elementWriteLock.lock();
        try {
            final Collection<E> providerElements = providerToElements.get(provider);
            if (providerElements == null) {
                logger.debug("Cannot add \"{}\" with key \"{}\". Provider \"{}\" unknown.",
                        element.getClass().getSimpleName(), element.getUID(), provider.getClass().getSimpleName());
                return;
            }
            if (!added(provider, element, providerElements)) {
                return;
            }
        } finally {
            elementWriteLock.unlock();
        }
        notifyListenersAboutAddedElement(element);
    }

    /**
     * Handle an element that has been added for a provider.
     *
     * <p>
     * This method must only be called if the write lock for elements has been locked!
     *
     * @param provider the provider that provides the element
     * @param element the element that has been added
     * @param providerElements the collection that holds the elements of the provider
     * @return indication if the element has been added
     */
    private boolean added(Provider<E> provider, E element, Collection<E> providerElements) {
        final K uid = element.getUID();
        if (identifierToElement.containsKey(uid)) {
            logger.debug(
                    "Cannot add \"{}\" with key \"{}\". It exists already from provider \"{}\"! Failed to add a second with the same UID from provider \"{}\"!",
                    element.getClass().getSimpleName(), uid,
                    elementToProvider.get(identifierToElement.get(uid)).getClass().getSimpleName(),
                    provider.getClass().getSimpleName());
            return false;
        }
        try {
            onAddElement(element);
        } catch (final RuntimeException ex) {
            logger.warn("Cannot add \"{}\" with key \"{}\": {}", element.getClass().getSimpleName(), uid,
                    ex.getMessage(), ex);
            return false;
        }
        identifierToElement.put(element.getUID(), element);
        elementToProvider.put(element, provider);
        providerElements.add(element);
        elements.add(element);
        return true;
    }

    @Override
    public void addRegistryChangeListener(RegistryChangeListener<E> listener) {
        listeners.add(listener);
    }

    @Override
    public Collection<@NonNull E> getAll() {
        elementReadLock.lock();
        try {
            return new HashSet<>(elements);
        } finally {
            elementReadLock.unlock();
        }
    }

    @Override
    public Stream<E> stream() {
        return getAll().stream();
    }

    @Override
    public void removed(Provider<E> provider, E element) {
        final E existingElement;
        elementWriteLock.lock();
        try {
            // The given "element" might not be the live instance but loaded from storage.
            // Use the identifier to operate on the "real" element.
            final K uid = element.getUID();
            existingElement = identifierToElement.get(uid);
            if (existingElement == null) {
                logger.debug("Cannot remove \"{}\" with key \"{}\" from provider \"{}\" because it does not exist!",
                        element.getClass().getSimpleName(), uid, provider.getClass().getSimpleName());
                return;
            }
            try {
                onRemoveElement(existingElement);
            } catch (final RuntimeException ex) {
                logger.warn("Cannot remove \"{}\" with key \"{}\": {}", element.getClass().getSimpleName(), uid,
                        ex.getMessage(), ex);
                return;
            }
            identifierToElement.remove(uid);
            elementToProvider.remove(existingElement);
            providerToElements.get(provider).remove(existingElement);
            elements.remove(existingElement);
        } finally {
            elementWriteLock.unlock();
        }
        notifyListenersAboutRemovedElement(existingElement);
    }

    @Override
    public void removeRegistryChangeListener(RegistryChangeListener<E> listener) {
        listeners.remove(listener);
    }

    @Override
    public void updated(Provider<E> provider, E oldElement, E element) {
        final K uidOld = oldElement.getUID();
        final K uid = element.getUID();
        if (!uidOld.equals(uid)) {
            logger.debug("Received update event for elements that UID differ (old: \"{}\", new: \"{}\"). Ignore event.",
                    uidOld, uid);
            return;
        }

        final E existingElement;
        elementWriteLock.lock();
        try {
            // The given "element" might not be the live instance but loaded from storage.
            // Use the identifier to operate on the "real" element.
            existingElement = identifierToElement.get(uid);
            if (existingElement == null) {
                logger.debug("Cannot update \"{}\" with key \"{}\" for provider \"{}\" because it does not exist!",
                        element.getClass().getSimpleName(), uid, provider.getClass().getSimpleName());
                return;
            }
            try {
                beforeUpdateElement(existingElement);
                onUpdateElement(oldElement, element);
            } catch (final RuntimeException ex) {
                logger.warn("Cannot update \"{}\" with key \"{}\": {}", element.getClass().getSimpleName(), uid,
                        ex.getMessage(), ex);
                return;
            }
            identifierToElement.put(uid, element);
            elementToProvider.remove(existingElement);
            elementToProvider.put(element, provider);
            final Collection<E> providerElements = providerToElements.get(provider);
            providerElements.remove(existingElement);
            providerElements.add(element);
            elements.remove(existingElement);
            elements.add(element);
        } finally {
            elementWriteLock.unlock();
        }
        notifyListenersAboutUpdatedElement(oldElement, element);
    }

    @Override
    public @Nullable E get(K key) {
        elementReadLock.lock();
        try {
            return identifierToElement.get(key);
        } finally {
            elementReadLock.unlock();
        }
    }

    /**
     * This method retrieves an Entry with the provider and the element for the key from the registry.
     *
     * @param key key of the element
     * @return provider and element entry or null if no element was found
     */
    protected @Nullable Entry<Provider<E>, E> getValueAndProvider(K key) {
        elementReadLock.lock();
        try {
            final E element = identifierToElement.get(key);
            if (element == null) {
                return null;
            }
            return new SimpleEntry<>(elementToProvider.get(element), element);
        } finally {
            elementReadLock.unlock();
        }
    }

    @Override
    public E add(E element) {
        managedProvider.orElseThrow(() -> new IllegalStateException("ManagedProvider is not available")).add(element);
        return element;
    }

    @Override
    public @Nullable E update(E element) {
        return managedProvider.orElseThrow(() -> new IllegalStateException("ManagedProvider is not available"))
                .update(element);
    }

    @Override
    public @Nullable E remove(K key) {
        return managedProvider.orElseThrow(() -> new IllegalStateException("ManagedProvider is not available"))
                .remove(key);
    }

    protected void notifyListeners(E element, EventType eventType) {
        for (RegistryChangeListener<E> listener : this.listeners) {
            try {
                switch (eventType) {
                    case ADDED:
                        listener.added(element);
                        break;
                    case REMOVED:
                        listener.removed(element);
                        break;
                    default:
                        break;
                }
            } catch (Throwable throwable) {
                logger.error("Cannot inform the listener \"{}\" about the \"{}\" event: {}", listener, eventType.name(),
                        throwable.getMessage(), throwable);
            }
        }
    }

    protected void notifyListeners(E oldElement, E element, EventType eventType) {
        for (RegistryChangeListener<E> listener : this.listeners) {
            try {
                switch (eventType) {
                    case UPDATED:
                        listener.updated(oldElement, element);
                        break;
                    default:
                        break;
                }
            } catch (Throwable throwable) {
                logger.error("Cannot inform the listener \"{}\" about the \"{}\" event: {}", listener, eventType.name(),
                        throwable.getMessage(), throwable);
            }
        }
    }

    protected void notifyListenersAboutAddedElement(E element) {
        notifyListeners(element, EventType.ADDED);
    }

    protected void notifyListenersAboutRemovedElement(E element) {
        notifyListeners(element, EventType.REMOVED);
    }

    protected void notifyListenersAboutUpdatedElement(E oldElement, E element) {
        notifyListeners(oldElement, element, EventType.UPDATED);
    }

    protected void addProvider(Provider<E> provider) {
        final Collection<E> elementsOfAddedProvider = provider.getAll();
        final Collection<E> elementsAdded = new HashSet<>(elementsOfAddedProvider.size());
        elementWriteLock.lock();
        try {
            if (providerToElements.get(provider) != null) {
                logger.warn("Cannot add provider \"{}\" because it already exists.",
                        provider.getClass().getSimpleName());
                return;
            }
            provider.addProviderChangeListener(this);
            final HashSet<E> providerElements = new HashSet<>();
            providerToElements.put(provider, providerElements);
            for (E element : elementsOfAddedProvider) {
                if (added(provider, element, providerElements)) {
                    elementsAdded.add(element);
                }
            }
        } finally {
            elementWriteLock.unlock();
        }
        elementsAdded.forEach(this::notifyListenersAboutAddedElement);
        logger.debug("Provider \"{}\" has been added.", provider.getClass().getName());
    }

    /**
     * This method retrieves the provider of an element from the registry.
     *
     * @param key key of the element
     * @return provider or null if no provider was found
     */
    protected @Nullable Provider<E> getProvider(K key) {
        elementReadLock.lock();
        try {
            final E element = identifierToElement.get(key);
            if (element == null) {
                return null;
            }
            return elementToProvider.get(element);
        } finally {
            elementReadLock.unlock();
        }
    }

    /**
     * This method retrieves the provider of an element from the registry.
     *
     * @param element the element
     * @return provider or null if no provider was found
     */
    public @Nullable Provider<E> getProvider(E element) {
        elementReadLock.lock();
        try {
            return elementToProvider.get(element);
        } finally {
            elementReadLock.unlock();
        }
    }

    /**
     * This method traverses over all elements of a provider in the registry and calls the consumer with each element.
     *
     * <p>
     * The traversal over the elements is done while holding a lock for the respective internal collections.
     * If you use this method, please ensure not execution time consuming stuff as it will block any other usage of that
     * collections.
     * You should also not call third party code that could e.g. access the registry itself again. This could lead to a
     * dead lock and hard finding bugs.
     * The {@link #getAll()} and {@link #stream()} method will operate on a copy and so no lock is hold.
     *
     * @param provider provider to traverse elements of
     * @param consumer function to call with element
     */
    protected void forEach(Provider<E> provider, Consumer<E> consumer) {
        elementReadLock.lock();
        try {
            final Collection<E> providerElements = providerToElements.get(provider);
            if (providerElements != null) {
                providerElements.forEach(consumer);
            }
        } finally {
            elementReadLock.unlock();
        }
    }

    /**
     * This method traverses over all elements in the registry and calls the consumer with each element.
     *
     * <p>
     * The traversal over the elements is done while holding a lock for the respective internal collections.
     * If you use this method, please ensure not execution time consuming stuff as it will block any other usage of that
     * collections.
     * You should also not call third party code that could e.g. access the registry itself again. This could lead to a
     * dead lock and hard finding bugs.
     * The {@link #getAll()} and {@link #stream()} method will operate on a copy and so no lock is hold.
     *
     * @param consumer function to call with element
     */
    protected void forEach(Consumer<E> consumer) {
        elementReadLock.lock();
        try {
            elements.forEach(consumer);
        } finally {
            elementReadLock.unlock();
        }
    }

    /**
     * This method traverses over all elements in the registry and calls the consumer with the provider of the
     * element as the first parameter and the element as the second argument.
     *
     * <p>
     * The traversal over the elements is done while holding a lock for the respective internal collections.
     * If you use this method, please ensure not execution time consuming stuff as it will block any other usage of that
     * collections.
     * You should also not call third party code that could e.g. access the registry itself again. This could lead to a
     * dead lock and hard finding bugs.
     * The {@link #getAll()} and {@link #stream()} method will operate on a copy and so no lock is hold.
     *
     * @param consumer function to call with the provider and element
     */
    protected void forEach(BiConsumer<Provider<E>, E> consumer) {
        elementReadLock.lock();
        try {
            for (final Entry<Provider<E>, Collection<E>> providerEntries : providerToElements.entrySet()) {
                final Provider<E> provider = providerEntries.getKey();
                providerEntries.getValue().forEach(element -> consumer.accept(provider, element));
            }
        } finally {
            elementReadLock.unlock();
        }
    }

    protected Optional<ManagedProvider<E, K>> getManagedProvider() {
        return managedProvider;
    }

    protected void setManagedProvider(ManagedProvider<E, K> provider) {
        managedProvider = Optional.ofNullable(provider);
    }

    protected void unsetManagedProvider(ManagedProvider<E, K> provider) {
        managedProvider = Optional.empty();
    }

    /**
     * This method is called before an element is added. The implementing class
     * can override this method to perform initialization logic or check the
     * validity of the element.
     *
     * <p>
     * To keep custom logic on the inheritance chain, you must call always the super implementation first.
     *
     * <p>
     * If the method throws an {@link IllegalArgumentException} the element will not be added.
     * <p>
     *
     * @param element element to be added
     * @throws IllegalArgumentException if the element is invalid and should not be added
     */
    protected void onAddElement(E element) throws IllegalArgumentException {
        // can be overridden by sub classes
    }

    /**
     * This method is called before an element is removed. The implementing
     * class can override this method to perform specific logic.
     *
     * <p>
     * To keep custom logic on the inheritance chain, you must call always the super implementation first.
     *
     * @param element element to be removed
     */
    protected void onRemoveElement(E element) {
        // can be overridden by sub classes
    }

    /**
     * This method is called before an element is updated. The implementing
     * class can override this method to perform specific logic.
     *
     * @param existingElement the previously existing element (as held in the element cache)
     */
    protected void beforeUpdateElement(E existingElement) {
        // can be overridden by sub classes
    }

    /**
     * This method is called before an element is updated. The implementing
     * class can override this method to perform specific logic or check the
     * validity of the updated element.
     *
     * <p>
     * To keep custom logic on the inheritance chain, you must call always the super implementation first.
     *
     * @param oldElement old element (before update, as given by the provider)
     * @param element updated element (after update)
     *            <p>
     *            If the method throws an {@link IllegalArgumentException} the element will not be updated.
     *            <p>
     * @throws IllegalArgumentException if the updated element is invalid and should not be updated
     */
    protected void onUpdateElement(E oldElement, E element) throws IllegalArgumentException {
        // can be overridden by sub classes
    }

    protected void removeProvider(Provider<E> provider) {
        final Collection<E> removedElements = new LinkedList<>();
        elementWriteLock.lock();
        try {
            final Collection<E> providerElements = providerToElements.remove(provider);
            if (providerElements == null) {
                logger.warn("Cannot remove provider \"{}\" because it is unknown.",
                        provider.getClass().getSimpleName());
                return;
            }
            for (final E element : providerElements) {
                try {
                    onRemoveElement(element);
                } catch (final RuntimeException ex) {
                    logger.warn(
                            "Removal of \"{}\" with key \"{}\" should be prevented but we need to remove the element as the provider \"{}\" is gone: {}",
                            element.getClass().getSimpleName(), element.getUID(), provider.getClass().getSimpleName(),
                            ex.getMessage(), ex);
                }
                removedElements.add(element);
                elements.remove(element);
                elementToProvider.remove(element);
                identifierToElement.remove(element.getUID());
            }
        } finally {
            elementWriteLock.unlock();
        }
        removedElements.forEach(this::notifyListenersAboutRemovedElement);
        provider.removeProviderChangeListener(this);
        logger.debug("Provider \"{}\" has been removed.", provider.getClass().getSimpleName());
    }

    protected @Nullable EventPublisher getEventPublisher() {
        return this.eventPublisher;
    }

    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    /**
     * This method can be used in a subclass in order to post events through the openHAB events bus. A common
     * use case is to notify event subscribers about an element which has been added/removed/updated to the registry.
     *
     * @param event the event
     */
    protected void postEvent(Event event) {
        if (eventPublisher != null) {
            try {
                eventPublisher.post(event);
            } catch (RuntimeException ex) {
                logger.error("Cannot post event of type \"{}\".", event.getType(), ex);
            }
        }
    }

}
