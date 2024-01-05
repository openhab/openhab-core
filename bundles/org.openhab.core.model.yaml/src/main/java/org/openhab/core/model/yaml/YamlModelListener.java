/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface YamlModelListener<T extends YamlElement> {

    /**
     * Method called by the model repository when elements from a model are added.
     *
     * @param modelName the name of the model
     * @param elements the collection of added elements
     */
    void addedModel(String modelName, Collection<? extends YamlElement> elements);

    /**
     * Method called by the model repository when elements from a model are updated.
     *
     * @param modelName the name of the model
     * @param elements the collection of updated elements
     */
    void updatedModel(String modelName, Collection<? extends YamlElement> elements);

    /**
     * Method called by the model repository when elements from a model are removed.
     *
     * @param modelName the name of the model
     * @param elements the collection of removed elements
     */
    void removedModel(String modelName, Collection<? extends YamlElement> elements);

    /**
     * Get the root name of this model type which is also the name of the root folder
     * containing the user files for this model type.
     *
     * A path is unexpected. What is expected is for example "items" or "things".
     *
     * @return the model root name
     */
    String getRootName();

    /**
     * Get the DTO class to be used for a file providing objects for this model type.
     *
     * @return the DTO file class
     */
    Class<? extends AbstractYamlFile> getFileClass();

    /**
     * Get the DTO class to be used for each object of this model type.
     *
     * @return the DTO element class
     */
    Class<T> getElementClass();
}
