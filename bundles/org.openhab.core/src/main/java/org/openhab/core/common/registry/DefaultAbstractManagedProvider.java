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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.StorageService;

/**
 * {@link DefaultAbstractManagedProvider} is a specific {@link AbstractManagedProvider} implementation, where the stored
 * element is
 * the same as the element of the provider. So no transformation is needed.
 * Therefore only two generic parameters are needed instead of three.
 *
 * @author Dennis Nobel - Initial contribution
 *
 * @param <E>
 *            type of the element
 * @param <K>
 *            type of the element key
 */
@NonNullByDefault
public abstract class DefaultAbstractManagedProvider<@NonNull E extends Identifiable<K>, @NonNull K>
        extends AbstractManagedProvider<E, K, E> {

    public DefaultAbstractManagedProvider(final StorageService storageService) {
        super(storageService);
    }

    @Override
    protected @Nullable E toElement(String key, E element) {
        return element;
    }

    @Override
    protected E toPersistableElement(E element) {
        return element;
    }
}
