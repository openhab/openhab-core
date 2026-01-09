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
package org.openhab.core.model.yaml;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link YamlModelRepository} defines methods to update elements in a YAML model.
 *
 * @author Jan N. Klug - Initial contribution
 * @author Laurent Garnier - Added methods addElementsToBeGenerated, generateFileFormat, createIsolatedModel and
 *         removeIsolatedModel
 */
@NonNullByDefault
public interface YamlModelRepository {
    void addElementToModel(String modelName, YamlElement element);

    void removeElementFromModel(String modelName, YamlElement element);

    void updateElementInModel(String modelName, YamlElement element);

    /**
     * Associate a list of elements to be generated to an identifier.
     *
     * @param id the identifier of the file format generation
     * @param elements the elements to be added
     */
    void addElementsToBeGenerated(String id, List<YamlElement> elements);

    /**
     * Generate the YAML file format for all elements that were associated to the provided identifier.
     *
     * @param id the identifier of the file format generation
     * @param out the output stream to write to
     */
    void generateFileFormat(String id, OutputStream out);

    /**
     * Creates an isolated model in the repository
     *
     * An isolated model is a temporary model loaded without impacting any object registry.
     *
     * @param inputStream an input stream with the model's content
     * @param errors the list to be used to fill the errors
     * @param warnings the list to be used to fill the warnings
     * @return the created model name if it was successfully processed, null otherwise
     */
    @Nullable
    String createIsolatedModel(InputStream inputStream, List<String> errors, List<String> warnings);

    /**
     * Removes an isolated model from the repository
     *
     * @param modelName the name of the model to be removed
     */
    void removeIsolatedModel(String modelName);
}
