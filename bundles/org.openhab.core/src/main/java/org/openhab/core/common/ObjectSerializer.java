/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.common;

import java.io.OutputStream;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A generic interface for serializers that serialize specific object types like Things, Items, Rules etc. into a
 * serialized representation that is written to an {@link OutputStream}.
 *
 * @param <T> The object type.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public interface ObjectSerializer<T> {

    /**
     * Get the name of the format.
     *
     * @return The format name.
     */
    String getGeneratedFormat();

    /**
     * Generate the format for all data that were associated to the provided identifier.
     *
     * @param id the identifier of the format generation.
     * @param out The {@link OutputStream} to write to.
     */
    void generateFormat(String id, OutputStream out);
}
