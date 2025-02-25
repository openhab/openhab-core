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
package org.openhab.core.items.fileconverter;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

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
     * Generate the file format for a sorted list of items.
     *
     * @param out the output stream to write the generated syntax to
     * @param items the items
     * @param metadata the provided collection of metadata for these items (including channel links)
     * @param hideDefaultParameters true to hide the configuration parameters having the default value
     */
    void generateFileFormat(OutputStream out, List<Item> items, Collection<Metadata> metadata,
            boolean hideDefaultParameters);
}
