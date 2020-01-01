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
package org.openhab.core.config.core.status;

/**
 * The {@link ConfigStatusCallback} interface is a callback interface to propagate a new configuration status for an
 * entity.
 *
 * @author Thomas Höfer - Initial contribution
 */
public interface ConfigStatusCallback {

    /**
     * Based on the given {@link ConfigStatusSource} this operation propagates a new configuration status for an entity
     * after its configuration has been updated.
     *
     * @param configStatusSource the source of the configuration status
     */
    void configUpdated(ConfigStatusSource configStatusSource);

}
