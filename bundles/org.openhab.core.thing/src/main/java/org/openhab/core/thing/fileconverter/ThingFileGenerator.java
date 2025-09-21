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
package org.openhab.core.thing.fileconverter;

import java.io.OutputStream;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Thing;

/**
 * {@link ThingFileGenerator} is the interface to implement by any file generator for {@link Thing} object.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface ThingFileGenerator {

    /**
     * Returns the format of the file.
     *
     * @return the file format
     */
    String getFileFormatGenerator();

    /**
     * Define the list of things to be generated and associate them to an identifier.
     *
     * @param id the identifier of the file format generation
     * @param things the things
     * @param hideDefaultChannels true to hide the non extensible channels having a default configuration
     * @param hideDefaultParameters true to hide the configuration parameters having the default value
     */
    void setThingsToBeGenerated(String id, List<Thing> things, boolean hideDefaultChannels,
            boolean hideDefaultParameters);

    /**
     * Generate the file format for all data that were associated to the provided identifier.
     *
     * @param id the identifier of the file format generation
     * @param out the output stream to write to
     */
    void generateFileFormat(String id, OutputStream out);
}
