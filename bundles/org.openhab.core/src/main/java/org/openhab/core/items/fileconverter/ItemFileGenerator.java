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

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;

/**
 * {@link ItemFileGenerator} is the interface to implement by any file generator for {@link Item} object.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface ItemFileGenerator {

    /**
     * Returns the format of the file.
     *
     * @return the file format
     */
    String getFileFormatGenerator();

    /**
     * Define the list of items (including metadata and channel links) to be generated and associate them
     * to an identifier.
     *
     * @param id the identifier of the file format generation
     * @param items the items
     * @param metadata the provided collection of metadata for these items (including channel links)
     * @param stateFormatters the optional state formatter for each item
     * @param hideDefaultParameters true to hide the configuration parameters having the default value
     */
    void setItemsToBeGenerated(String id, List<Item> items, Collection<Metadata> metadata,
            Map<String, String> stateFormatters, boolean hideDefaultParameters);

    /**
     * Generate the file format for all data that were associated to the provided identifier.
     *
     * @param id the identifier of the file format generation
     * @param out the output stream to write to
     */
    void generateFileFormat(String id, OutputStream out);
}
