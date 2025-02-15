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
package org.openhab.core.common.registry;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link RegistryChangedRunnableListener} can be added to {@link Registry} services, to execute a given
 * {@link Runnable} on all types of changes.
 *
 * @author Florian Hotze - Initial contribution
 *
 * @param <E> type of the element in the registry
 */
@NonNullByDefault
public class RegistryChangedRunnableListener<E> implements RegistryChangeListener<E> {
    final Runnable runnable;

    public RegistryChangedRunnableListener(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void added(E element) {
        runnable.run();
    }

    @Override
    public void removed(E element) {
        runnable.run();
    }

    @Override
    public void updated(E oldElement, E newElement) {
        runnable.run();
    }
}
