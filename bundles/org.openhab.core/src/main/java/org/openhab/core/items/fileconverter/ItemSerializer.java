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
package org.openhab.core.items.fileconverter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.ObjectSerializer;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;

/**
 * {@link ItemSerializer} is the interface to implement by any file generator for {@link Item} object.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface ItemSerializer extends ObjectSerializer<Item> {

    /**
     * Specify the {@link List} of {@link Item}s (including {@link Metadata} and channel links) to be serialized and
     * associate them with an identifier.
     *
     * @param id the identifier of the {@link Item} format generation.
     * @param items the {@link List} of {@link Item}s to serialize.
     * @param metadata the provided {@link Collection} of {@link Metadata} for the {@link Item}s (including channel
     *            links).
     * @param stateFormatters the (optional) {@link Map} of {@link Item} name and state formatter for each {@link Item}.
     * @param hideDefaultParameters {@code true} to hide the configuration parameters having a default value.
     */
    void setItemsToBeGenerated(String id, List<Item> items, Collection<Metadata> metadata,
            Map<String, String> stateFormatters, boolean hideDefaultParameters);
}
