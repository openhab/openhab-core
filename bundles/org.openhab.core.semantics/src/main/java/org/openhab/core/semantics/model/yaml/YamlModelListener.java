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
package org.openhab.core.semantics.model.yaml;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link YamlModelListener} interface is responsible for managing a particular model
 * with data processed from YAML configuration files.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface YamlModelListener<T extends YamlElement> {

    void addedModel(String modelName, List<? extends YamlElement> elements);

    void updatedModel(String modelName, List<? extends YamlElement> elements);

    void removedModel(String modelName, List<? extends YamlElement> elements);

    String getRootName();

    Class<? extends YamlFile> getFileClass();

    Class<T> getElementClass();
}
