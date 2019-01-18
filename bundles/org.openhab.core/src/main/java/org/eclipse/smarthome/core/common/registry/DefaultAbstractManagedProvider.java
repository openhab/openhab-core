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
public abstract class DefaultAbstractManagedProvider<E extends Identifiable<K>, K>
        extends AbstractManagedProvider<E, K, E> {

    @Override
    protected E toElement(String key, E element) {
        return element;
    }

    @Override
    protected E toPersistableElement(E element) {
        return element;
    }

}
