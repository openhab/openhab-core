/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.test;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.RegistryChangeListener;

/**
 * The {@link CountingRegistryListener} is a helper class for counting registry changes
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class CountingRegistryListener<T> implements RegistryChangeListener<T> {
    private int addedCounter = 0;
    private int removedCounter = 0;
    private int updatedCounter = 0;

    @Override
    public void added(T element) {
        addedCounter++;
    }

    @Override
    public void removed(T element) {
        removedCounter++;
    }

    @Override
    public void updated(T oldElement, T element) {
        updatedCounter++;
    }

    public int getAddedCounter() {
        return addedCounter;
    }

    public int getRemovedCounter() {
        return removedCounter;
    }

    public int getUpdatedCounter() {
        return updatedCounter;
    }
}
