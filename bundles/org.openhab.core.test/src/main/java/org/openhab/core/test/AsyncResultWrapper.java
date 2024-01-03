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
package org.openhab.core.test;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Tanya Georgieva - Initial contribution
 */
@NonNullByDefault
public class AsyncResultWrapper<T> {
    private @Nullable T wrappedObject;
    private boolean isSet = false;

    public void set(T wrappedObject) {
        this.wrappedObject = wrappedObject;
        isSet = true;
    }

    public @Nullable T getWrappedObject() {
        return wrappedObject;
    }

    public boolean isSet() {
        return isSet;
    }

    public void reset() {
        wrappedObject = null;
        isSet = false;
    }
}
