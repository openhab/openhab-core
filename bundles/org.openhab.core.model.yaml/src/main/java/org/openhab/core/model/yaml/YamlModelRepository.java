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
 * @author Laurent Garnier - Added methods refreshModelElements and generateSyntaxFromElements
 * @author Laurent Garnier - Added methods createTemporaryModel and removeTemporaryModel
 */
@NonNullByDefault
public interface YamlModelRepository {
    void addElementToModel(String modelName, YamlElement element);

    void removeElementFromModel(String modelName, YamlElement element);

    void updateElementInModel(String modelName, YamlElement element);

    /**
     * Triggers the refresh of a certain type of elements in a given model.
     *
     * @param modelName the model name
     * @param elementName the type of elements to refresh
     */
    void refreshModelElements(String modelName, String elementName);

    /**
     * Generate the YAML syntax from a provided list of elements.
     *
     * @param out the output stream to write the generated syntax to
     * @param elements the list of elements to includ
     */
    void generateSyntaxFromElements(OutputStream out, List<YamlElement> elements);

    /**
     * Creates a temporary model in the repository
     *
     * A temporary model is not attached to a file on disk.
     * A temporary model will be loaded without impacting any object registry.
     *
     * @param inputStream an input stream with the model's content
     * @param errors the list to be used to fill the errors
     * @param warnings the list to be used to fill the warnings
     * @return the created model name if it was successfully processed, null otherwise
     */
    @Nullable
    String createTemporaryModel(InputStream inputStream, List<String> errors, List<String> warnings);

    /**
     * Removes a temporary model from the repository
     *
     * @param modelName the name of the model to be removed
     */
    void removeTemporaryModel(String modelName);
}
