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
package org.eclipse.smarthome.storage.json.internal;

import com.google.gson.annotations.SerializedName;

/**
 * Internal data structure of the {@link JsonStorage}
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
public class StorageEntry {

    @SerializedName("class") // in order to stay backwards compatible
    private final String entityClassName;
    private final Object value;

    public StorageEntry(String entityClassName, Object value) {
        this.entityClassName = entityClassName;
        this.value = value;
    }

    public String getEntityClassName() {
        return entityClassName;
    }

    public Object getValue() {
        return value;
    }

}
