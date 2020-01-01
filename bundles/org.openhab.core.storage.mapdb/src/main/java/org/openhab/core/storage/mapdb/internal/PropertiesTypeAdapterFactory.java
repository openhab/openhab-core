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
package org.openhab.core.storage.mapdb.internal;

import java.lang.reflect.Type;

import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

/**
 * TypeAdapterFactory responsible for returning a new instance of {@link PropertiesTypeAdapter} if the given type
 * matches Map&lt;String, Object&gt; or null otherwise.
 *
 * @author Ivan Iliev - Initial contribution
 */
public class PropertiesTypeAdapterFactory implements TypeAdapterFactory {

    @SuppressWarnings({ "unused", "unchecked" })
    @Override
    public <T> @Nullable TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Type type = typeToken.getType();

        Class<? super T> rawType = typeToken.getRawType();
        if (!PropertiesTypeAdapter.TOKEN.equals(typeToken)) {
            return null;
        }

        return (TypeAdapter<T>) new PropertiesTypeAdapter(gson);
    }

}
