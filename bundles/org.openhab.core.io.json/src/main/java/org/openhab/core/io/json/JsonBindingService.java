/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.core.io.json;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

/**
 * JSON data binding service.
 *
 * @author Flavio Costa - Initial contribution.
 *
 * @param <T> Type to be serialized or deserialized.
 */
public interface JsonBindingService<T> {

    /**
     * Serialize an object to Json.
     *
     * @param object Object instance to be serialized.
     * @param genericType Generic type definition.
     * @param writer Writer where the Json output will be written to.
     */
    void toJson(T object, Type genericType, Writer writer);

    /**
     * Deserializes an object from Json.
     *
     * @param reader Reader that provides the serialized Json.
     * @param genericType Generic type definition.
     * @return Object instance that was deserialized.
     */
    T fromJson(Reader reader, Type genericType);

    /**
     * Defines whether the serialized output will be formatted or not.
     *
     * @param prettyPrinting True if formatted output is expected (optimized for human reading), false if the output
     *            Json should be streamlined.
     */
    void setFormattedOutput(boolean prettyPrinting);
}
