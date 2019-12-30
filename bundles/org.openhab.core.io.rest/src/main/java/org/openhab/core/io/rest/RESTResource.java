/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.io.rest;

/**
 * This is a marker interface for REST resource implementations
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Stefan Triller - Added default implementation for isSatisfied
 */
public interface RESTResource {

    /**
     * Method used to determine availability of a RESTResource
     *
     * @return true if this RESTResource is ready to process requests, false otherwise.
     */
    default boolean isSatisfied() {
        return true;
    }

}
