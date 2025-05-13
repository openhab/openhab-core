/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.io.dto;

import org.eclipse.jdt.annotation.NonNull;

/**
 * A DTO that can handle the deserialization from "tree form" itself, allowing more flexible, multi-step processing.
 *
 * @param <D> the DTO type.
 * @param <M> the mapper/helper type, e.g. {@code ObjectMapper}.
 * @param <N> the tree-node type, e.g. {@code JsonNode}.
 *
 * @author Ravi Nadahar - Initial contribution
 */
public interface ModularDTO<D, M, N> {

    /**
     * Deserializes the specified node into a DTO object.
     *
     * @param node the node to deserialize.
     * @param mapper the mapper/helper object to use for deserialization.
     * @return The resulting DTO instance.
     * @throws SerializationException If an error occurs during deserialization.
     */
    @NonNull
    D toDto(@NonNull N node, @NonNull M mapper) throws SerializationException;
}
