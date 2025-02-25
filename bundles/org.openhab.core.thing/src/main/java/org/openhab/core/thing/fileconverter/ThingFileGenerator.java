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
     * Generate the file format for a sorted list of things.
     *
     * @param out the output stream to write the generated syntax to
     * @param things the things
     * @param hideDefaultParameters true to hide the configuration parameters having the default value
     */
    void generateFileFormat(OutputStream out, List<Thing> things, boolean hideDefaultParameters);
}
