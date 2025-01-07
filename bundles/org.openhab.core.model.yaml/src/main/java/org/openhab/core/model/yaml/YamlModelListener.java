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

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link YamlModelListener} interface is responsible for managing a particular model type
 * with data processed from YAML configuration files.
 * <p />
 * Implementors are notified whenever a YAML model changed that contains elements of the given type. They MUST declare
 * at least {@link YamlModelListener} as service to automatically register them with the repository.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface YamlModelListener<T extends YamlElement> {

    /**
     * Method called by the model repository when elements from a model are added. Only added elements are contained in
     * the collection. In case the listener is added after a model was read, this method is also called with all
     * elements from that model.
     *
     * @param modelName the name of the model
     * @param elements the collection of added elements
     */
    void addedModel(String modelName, Collection<T> elements);

    /**
     * Method called by the model repository when elements from a model are updated. Only changed elements are contained
     * in the collection.
     *
     * @param modelName the name of the model
     * @param elements the collection of updated elements
     */
    void updatedModel(String modelName, Collection<T> elements);

    /**
     * Method called by the model repository when elements from a model are removed.
     *
     * @param modelName the name of the model
     * @param elements the collection of removed elements
     */
    void removedModel(String modelName, Collection<T> elements);

    /**
     * Get the DTO class to be used for each object of this model type. The DTO class MUST implement {@link YamlElement}
     * and fulfill all requirements defined for the interface.
     *
     * @return the DTO element class
     */
    Class<T> getElementClass();
}
