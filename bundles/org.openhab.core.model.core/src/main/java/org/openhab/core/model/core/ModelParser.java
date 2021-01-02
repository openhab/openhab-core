/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.model.core;

/**
 * This interface has to be implemented by services that register an EMF model parser
 *
 * @author Kai Kreuzer - Initial contribution
 */
public interface ModelParser {

    /**
     * Returns the file extensions of the models this parser registers for.
     *
     * @return file extension of model files
     */
    String getExtension();
}
