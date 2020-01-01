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
package org.openhab.core.model.script.engine.action;

/**
 * This interface must be implemented by services that want to contribute script actions.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public interface ActionService {

    /**
     * returns the FQCN of the action class.
     *
     * @return the FQCN of the action class
     */
    default String getActionClassName() {
        return getActionClass().getCanonicalName();
    }

    /**
     * Returns the action class itself
     *
     * @return the action class
     */
    Class<?> getActionClass();

}
