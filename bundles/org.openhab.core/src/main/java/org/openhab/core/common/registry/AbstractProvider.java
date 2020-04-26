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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AbstractProvider} can be used as base class for {@link Provider} implementations. It supports the registration
 * and notification of listeners.
 *
 * @author Dennis Nobel - Initial contribution
 *
 * @param <E>
 *            type of the provided elements
 */
@NonNullByDefault
public abstract class AbstractProvider<@NonNull E> implements Provider<E> {

    private enum EventType {
        ADDED,
        REMOVED,
        UPDATED;
    }

    protected final Logger logger = LoggerFactory.getLogger(AbstractProvider.class);
    protected List<ProviderChangeListener<E>> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addProviderChangeListener(ProviderChangeListener<E> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<E> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(@Nullable E oldElement, E element, EventType eventType) {
        for (ProviderChangeListener<E> listener : this.listeners) {
            try {
                switch (eventType) {
                    case ADDED:
                        listener.added(this, element);
                        break;
                    case REMOVED:
                        listener.removed(this, element);
                        break;
                    case UPDATED:
                        listener.updated(this, oldElement != null ? oldElement : element, element);
                        break;
                    default:
                        break;
                }
            } catch (Exception ex) {
                logger.error("Could not inform the listener '{}' about the '{}' event!: {}", listener, eventType.name(),
                        ex.getMessage(), ex);
            }
        }
    }

    private void notifyListeners(E element, EventType eventType) {
        notifyListeners(null, element, eventType);
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
}
