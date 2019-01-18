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
package org.eclipse.smarthome.test;

/**
 * @author Tanya Georgieva - Refactor the groovy file to java
 */
public class AsyncResultWrapper<T> {
    private T wrappedObject;
    private boolean isSet = false;

    public void set(T wrappedObject) {
        this.wrappedObject = wrappedObject;
        isSet = true;
    }

    public T getWrappedObject() {
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
