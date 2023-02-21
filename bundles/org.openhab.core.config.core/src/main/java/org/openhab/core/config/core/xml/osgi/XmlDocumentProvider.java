/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.config.core.xml.osgi;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.xml.internal.ConfigDescriptionReader;

/**
 * The {@link XmlDocumentProvider} is responsible managing any created objects
 * by a {@link ConfigDescriptionReader} for a certain module.
 * <p>
 * Each instance of this class is assigned to exactly one module.
 *
 * @author Michael Grammling - Initial contribution
 *
 * @param <T> the result type of the conversion
 */
@NonNullByDefault
public interface XmlDocumentProvider<@NonNull T> {

    /**
     * Adds a new result object from the XML processing for further processing.
     *
     * @param object the result object to be processed
     */
    void addingObject(T object);

    /**
     * Signals that all available result objects from the XML processing of the
     * certain module have been added.
     */
    void addingFinished();

    /**
     * Releases any added result objects from the XML processing.
     */
    void release();
}
