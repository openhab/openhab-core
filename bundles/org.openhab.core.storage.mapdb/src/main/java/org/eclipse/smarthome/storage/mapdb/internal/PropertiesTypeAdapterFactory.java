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
package org.eclipse.smarthome.storage.mapdb.internal;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

/**
 * TypeAdapterFactory responsible for returning a new instance of {@link PropertiesTypeAdapter} if the given type
 * matches Map<String, Object>
 * or null otherwise.
 *
 * @author Ivan Iliev
 *
 */
public class PropertiesTypeAdapterFactory implements TypeAdapterFactory {

    @SuppressWarnings({ "unused", "unchecked" })
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Type type = typeToken.getType();

        Class<? super T> rawType = typeToken.getRawType();
        if (!PropertiesTypeAdapter.TOKEN.equals(typeToken)) {
            return null;
        }

        return (TypeAdapter<T>) new PropertiesTypeAdapter(gson);
    }

}
