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
package org.openhab.core.items;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link MetadataAwareItem} is an interface that can be implemented by {@link Item}s that need to be notified of
 * metadata changes.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface MetadataAwareItem {

    /**
     * Can be implemented by subclasses to be informed about added metadata
     *
     * @param metadata the added {@link Metadata} object for this {@link Item}
     */
    void addedMetadata(Metadata metadata);

    /**
     * Can be implemented by subclasses to be informed about updated metadata
     *
     * @param oldMetadata the old {@link Metadata} object for this {@link Item}
     * @param newMetadata the new {@link Metadata} object for this {@link Item}
     *
     */
    void updatedMetadata(Metadata oldMetadata, Metadata newMetadata);

    /**
     * Can be implemented by subclasses to be informed about removed metadata
     *
     * @param metadata the removed {@link Metadata} object for this {@link Item}
     */
    void removedMetadata(Metadata metadata);
}
