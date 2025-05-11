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

import java.io.OutputStream;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link YamlModelRepository} defines methods to update elements in a YAML model.
 *
 * @author Jan N. Klug - Initial contribution
 * @author Laurent Garnier - Added method generateSyntaxFromElements
 */
@NonNullByDefault
public interface YamlModelRepository {
    void addElementToModel(String modelName, YamlElement element);

    void removeElementFromModel(String modelName, YamlElement element);

    void updateElementInModel(String modelName, YamlElement element);

    /**
     * Generate the YAML syntax from a provided list of elements.
     *
     * @param out the output stream to write the generated syntax to
     * @param elements the list of elements to includ
     */
    void generateSyntaxFromElements(OutputStream out, List<YamlElement> elements);
}
